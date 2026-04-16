package com.example.rabit.data.storage

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Properties

/**
 * RemoteStorageManager — Singleton that mediates between the SAF DocumentsProvider
 * and actual SSH / Helper HTTP backends.
 *
 * Tries SSH (SFTP) first; falls back to Helper HTTP API when SSH is unavailable.
 * All operations are blocking and designed to be called from IO threads.
 */
object RemoteStorageManager {

    private const val TAG = "RemoteStorageManager"
    private const val CACHE_DIR_NAME = "remote_mount_cache"
    private const val CONNECT_TIMEOUT_MS = 8_000
    private const val READ_TIMEOUT_MS = 10_000

    // --- Connection state ---
    @Volatile var isMounted: Boolean = false
        private set

    @Volatile private var sshSession: Session? = null
    @Volatile private var sftpChannel: ChannelSftp? = null

    // Credentials (populated from SharedPreferences when mount() is called)
    @Volatile var sshHost: String = ""
    @Volatile var sshPort: Int = 22
    @Volatile var sshUser: String = ""
    @Volatile var sshPassword: String = ""
    @Volatile var helperBaseUrl: String = ""
    @Volatile var helperPin: String = ""

    private var cacheDir: File? = null

    // ---------- lifecycle ----------

    fun mount(context: Context) {
        val prefs = context.getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE)
        sshHost = prefs.getString("ssh_host", "") ?: ""
        sshPort = prefs.getInt("ssh_port", 22)
        sshUser = prefs.getString("ssh_user", "") ?: ""
        sshPassword = prefs.getString("ssh_password", "") ?: ""
        helperBaseUrl = prefs.getString("helper_base_url", "") ?: ""
        helperPin = prefs.getString("helper_pin", "") ?: ""

        cacheDir = File(context.cacheDir, CACHE_DIR_NAME).also { it.mkdirs() }

        // Try establishing SSH/SFTP; ok if it fails — we'll retry lazily
        ensureSshConnected()
        isMounted = true
        Log.i(TAG, "Remote storage mounted (ssh=${sshHost.isNotBlank()}, helper=${helperBaseUrl.isNotBlank()})")
    }

    fun unmount() {
        isMounted = false
        runCatching { sftpChannel?.disconnect() }
        runCatching { sshSession?.disconnect() }
        sftpChannel = null
        sshSession = null
        Log.i(TAG, "Remote storage unmounted")
    }

    val isConnected: Boolean
        get() = isSshReady() || helperBaseUrl.isNotBlank()

    // ---------- SSH helpers ----------

    private fun isSshReady(): Boolean {
        val session = sshSession ?: return false
        val channel = sftpChannel
        return session.isConnected && channel != null && channel.isConnected
    }

    private fun ensureSshConnected(): Boolean {
        if (isSshReady()) return true
        if (sshHost.isBlank() || sshUser.isBlank()) return false

        return try {
            runCatching { sftpChannel?.disconnect() }
            runCatching { sshSession?.disconnect() }

            val jsch = JSch()
            val session = jsch.getSession(sshUser, sshHost, sshPort).apply {
                setPassword(sshPassword)
                setConfig(Properties().apply { put("StrictHostKeyChecking", "no") })
                connect(CONNECT_TIMEOUT_MS)
            }
            val channel = session.openChannel("sftp") as ChannelSftp
            channel.connect(CONNECT_TIMEOUT_MS)

            sshSession = session
            sftpChannel = channel
            Log.i(TAG, "SFTP connected to $sshHost")
            true
        } catch (e: Exception) {
            Log.w(TAG, "SFTP connect failed: ${e.message}")
            false
        }
    }

    // Execute an SSH command and get the output (for operations SFTP doesn't cover)
    private fun execSsh(command: String): String {
        val session = sshSession ?: throw IllegalStateException("SSH not connected")
        if (!session.isConnected) throw IllegalStateException("SSH session disconnected")

        val channel = session.openChannel("exec") as ChannelExec
        channel.setCommand(command)
        channel.inputStream = null
        val input = channel.inputStream
        channel.connect(CONNECT_TIMEOUT_MS)
        val result = input.bufferedReader().readText()
        channel.disconnect()
        return result
    }

    // ---------- File entry data ----------

    data class RemoteFileEntry(
        val name: String,
        val path: String,
        val isDirectory: Boolean,
        val size: Long,
        val lastModified: Long,
        val mimeType: String
    )

    // ---------- Core operations ----------

    /**
     * List files in a remote directory.
     */
    fun listFiles(path: String): List<RemoteFileEntry> {
        // Try SSH/SFTP first
        if (ensureSshConnected()) {
            return listFilesSftp(path)
        }
        // Fallback to Helper HTTP
        if (helperBaseUrl.isNotBlank()) {
            return listFilesHelper(path)
        }
        return emptyList()
    }

    private fun listFilesSftp(path: String): List<RemoteFileEntry> {
        val channel = sftpChannel ?: return emptyList()
        return try {
            @Suppress("UNCHECKED_CAST")
            val entries = channel.ls(path) as? java.util.Vector<ChannelSftp.LsEntry> ?: return emptyList()
            entries.mapNotNull { entry ->
                val name = entry.filename
                if (name == "." || name == "..") return@mapNotNull null
                val attrs = entry.attrs
                val fullPath = if (path.endsWith("/")) "$path$name" else "$path/$name"
                RemoteFileEntry(
                    name = name,
                    path = fullPath,
                    isDirectory = attrs.isDir,
                    size = attrs.size,
                    lastModified = attrs.mTime.toLong() * 1000L,
                    mimeType = if (attrs.isDir) "vnd.android.document/directory" else guessMimeType(name)
                )
            }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        } catch (e: Exception) {
            Log.w(TAG, "SFTP ls failed for $path: ${e.message}")
            emptyList()
        }
    }

    private fun listFilesHelper(path: String): List<RemoteFileEntry> {
        return try {
            val encodedPath = URLEncoder.encode(path, "UTF-8")
            val conn = (URL("$helperBaseUrl/files?path=$encodedPath").openConnection() as HttpURLConnection).apply {
                if (helperPin.isNotBlank()) setRequestProperty("X-Auth-PIN", helperPin)
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val items = json.optJSONArray("items") ?: JSONArray()
            val files = mutableListOf<RemoteFileEntry>()
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val name = item.optString("name", "Unknown")
                val isDir = item.optBoolean("isDir", false)
                val itemPath = item.optString("path", "$path/$name")
                files += RemoteFileEntry(
                    name = name,
                    path = itemPath,
                    isDirectory = isDir,
                    size = item.optLong("size", 0L),
                    lastModified = item.optLong("modified", System.currentTimeMillis()),
                    mimeType = if (isDir) "vnd.android.document/directory" else guessMimeType(name)
                )
            }
            files.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        } catch (e: Exception) {
            Log.w(TAG, "Helper list failed for $path: ${e.message}")
            emptyList()
        }
    }

    /**
     * Get metadata for a single file.
     */
    fun statFile(remotePath: String): RemoteFileEntry? {
        if (ensureSshConnected()) {
            return statFileSftp(remotePath)
        }
        // Helper doesn't have a dedicated stat endpoint; extract from parent listing
        val parent = remotePath.substringBeforeLast("/").ifEmpty { "/" }
        val name = remotePath.substringAfterLast("/")
        return listFilesHelper(parent).firstOrNull { it.name == name }
    }

    private fun statFileSftp(remotePath: String): RemoteFileEntry? {
        val channel = sftpChannel ?: return null
        return try {
            val attrs = channel.stat(remotePath)
            val name = remotePath.substringAfterLast("/")
            RemoteFileEntry(
                name = name,
                path = remotePath,
                isDirectory = attrs.isDir,
                size = attrs.size,
                lastModified = attrs.mTime.toLong() * 1000L,
                mimeType = if (attrs.isDir) "vnd.android.document/directory" else guessMimeType(name)
            )
        } catch (e: Exception) {
            Log.w(TAG, "SFTP stat failed for $remotePath: ${e.message}")
            null
        }
    }

    /**
     * Read file contents into a local cache file. Returns the cache File.
     */
    fun readFile(remotePath: String): File? {
        val cacheFile = getCacheFile(remotePath)

        // Try SFTP
        if (ensureSshConnected()) {
            return readFileSftp(remotePath, cacheFile)
        }
        // Fallback to Helper
        if (helperBaseUrl.isNotBlank()) {
            return readFileHelper(remotePath, cacheFile)
        }
        return null
    }

    private fun readFileSftp(remotePath: String, cacheFile: File): File? {
        val channel = sftpChannel ?: return null
        return try {
            cacheFile.parentFile?.mkdirs()
            FileOutputStream(cacheFile).use { fos ->
                channel.get(remotePath, fos)
            }
            cacheFile
        } catch (e: Exception) {
            Log.w(TAG, "SFTP get failed for $remotePath: ${e.message}")
            null
        }
    }

    private fun readFileHelper(remotePath: String, cacheFile: File): File? {
        return try {
            val encodedPath = URLEncoder.encode(remotePath, "UTF-8")
            val conn = (URL("$helperBaseUrl/file/download?path=$encodedPath").openConnection() as HttpURLConnection).apply {
                if (helperPin.isNotBlank()) setRequestProperty("X-Auth-PIN", helperPin)
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = 30_000
            }
            cacheFile.parentFile?.mkdirs()
            conn.inputStream.use { input ->
                FileOutputStream(cacheFile).use { fos ->
                    input.copyTo(fos)
                }
            }
            cacheFile
        } catch (e: Exception) {
            Log.w(TAG, "Helper download failed for $remotePath: ${e.message}")
            null
        }
    }

    /**
     * Write bytes from a local file back to the remote.
     */
    fun writeFile(remotePath: String, localFile: File): Boolean {
        if (ensureSshConnected()) {
            return writeFileSftp(remotePath, localFile)
        }
        if (helperBaseUrl.isNotBlank()) {
            return writeFileHelper(remotePath, localFile)
        }
        return false
    }

    private fun writeFileSftp(remotePath: String, localFile: File): Boolean {
        val channel = sftpChannel ?: return false
        return try {
            FileInputStream(localFile).use { fis ->
                channel.put(fis, remotePath, ChannelSftp.OVERWRITE)
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "SFTP put failed for $remotePath: ${e.message}")
            false
        }
    }

    private fun writeFileHelper(remotePath: String, localFile: File): Boolean {
        return try {
            val encodedPath = URLEncoder.encode(remotePath, "UTF-8")
            val boundary = "----HackieBoundary${System.currentTimeMillis()}"
            val conn = (URL("$helperBaseUrl/file/upload?path=$encodedPath").openConnection() as HttpURLConnection).apply {
                if (helperPin.isNotBlank()) setRequestProperty("X-Auth-PIN", helperPin)
                requestMethod = "POST"
                doOutput = true
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = 30_000
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            }
            conn.outputStream.use { os ->
                val header = "--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"${localFile.name}\"\r\nContent-Type: application/octet-stream\r\n\r\n"
                os.write(header.toByteArray())
                FileInputStream(localFile).use { it.copyTo(os) }
                os.write("\r\n--$boundary--\r\n".toByteArray())
            }
            conn.responseCode in 200..299
        } catch (e: Exception) {
            Log.w(TAG, "Helper upload failed for $remotePath: ${e.message}")
            false
        }
    }

    /**
     * Delete a remote file or directory.
     */
    fun deleteFile(remotePath: String): Boolean {
        if (ensureSshConnected()) {
            return deleteFileSftp(remotePath)
        }
        if (helperBaseUrl.isNotBlank()) {
            return deleteFileHelper(remotePath)
        }
        return false
    }

    private fun deleteFileSftp(remotePath: String): Boolean {
        val channel = sftpChannel ?: return false
        return try {
            val attrs = channel.stat(remotePath)
            if (attrs.isDir) {
                channel.rmdir(remotePath)
            } else {
                channel.rm(remotePath)
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "SFTP delete failed for $remotePath: ${e.message}")
            false
        }
    }

    private fun deleteFileHelper(remotePath: String): Boolean {
        return try {
            val encodedPath = URLEncoder.encode(remotePath, "UTF-8")
            val conn = (URL("$helperBaseUrl/file/delete?path=$encodedPath").openConnection() as HttpURLConnection).apply {
                if (helperPin.isNotBlank()) setRequestProperty("X-Auth-PIN", helperPin)
                requestMethod = "DELETE"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
            }
            conn.responseCode in 200..299
        } catch (e: Exception) {
            Log.w(TAG, "Helper delete failed for $remotePath: ${e.message}")
            false
        }
    }

    /**
     * Create a file or directory on the remote.
     */
    fun createFile(parentPath: String, name: String, isDirectory: Boolean): String? {
        val fullPath = if (parentPath.endsWith("/")) "$parentPath$name" else "$parentPath/$name"

        if (ensureSshConnected()) {
            return createFileSftp(fullPath, isDirectory)
        }
        if (helperBaseUrl.isNotBlank()) {
            return createFileHelper(parentPath, name, isDirectory)
        }
        return null
    }

    private fun createFileSftp(fullPath: String, isDirectory: Boolean): String? {
        val channel = sftpChannel ?: return null
        return try {
            if (isDirectory) {
                channel.mkdir(fullPath)
            } else {
                // Create empty file
                channel.put(ByteArrayInputStream(ByteArray(0)), fullPath)
            }
            fullPath
        } catch (e: Exception) {
            Log.w(TAG, "SFTP create failed for $fullPath: ${e.message}")
            null
        }
    }

    private fun createFileHelper(parentPath: String, name: String, isDirectory: Boolean): String? {
        return try {
            val json = JSONObject().apply {
                put("path", parentPath)
                put("name", name)
                put("isDir", isDirectory)
            }
            val conn = (URL("$helperBaseUrl/file/create").openConnection() as HttpURLConnection).apply {
                if (helperPin.isNotBlank()) setRequestProperty("X-Auth-PIN", helperPin)
                requestMethod = "POST"
                doOutput = true
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Content-Type", "application/json")
            }
            conn.outputStream.use { it.write(json.toString().toByteArray()) }
            if (conn.responseCode in 200..299) {
                if (parentPath.endsWith("/")) "$parentPath$name" else "$parentPath/$name"
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Helper create failed: ${e.message}")
            null
        }
    }

    /**
     * Rename a file or directory on the remote.
     */
    fun renameFile(oldPath: String, newName: String): String? {
        val parent = oldPath.substringBeforeLast("/").ifEmpty { "/" }
        val newPath = if (parent.endsWith("/")) "$parent$newName" else "$parent/$newName"

        if (ensureSshConnected()) {
            return renameFileSftp(oldPath, newPath)
        }
        if (helperBaseUrl.isNotBlank()) {
            return renameFileHelper(oldPath, newPath)
        }
        return null
    }

    private fun renameFileSftp(oldPath: String, newPath: String): String? {
        val channel = sftpChannel ?: return null
        return try {
            channel.rename(oldPath, newPath)
            newPath
        } catch (e: Exception) {
            Log.w(TAG, "SFTP rename failed: ${e.message}")
            null
        }
    }

    private fun renameFileHelper(oldPath: String, newPath: String): String? {
        return try {
            val json = JSONObject().apply {
                put("oldPath", oldPath)
                put("newPath", newPath)
            }
            val conn = (URL("$helperBaseUrl/file/rename").openConnection() as HttpURLConnection).apply {
                if (helperPin.isNotBlank()) setRequestProperty("X-Auth-PIN", helperPin)
                requestMethod = "POST"
                doOutput = true
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Content-Type", "application/json")
            }
            conn.outputStream.use { it.write(json.toString().toByteArray()) }
            if (conn.responseCode in 200..299) newPath else null
        } catch (e: Exception) {
            Log.w(TAG, "Helper rename failed: ${e.message}")
            null
        }
    }

    // ---------- Cache helpers ----------

    private fun getCacheFile(remotePath: String): File {
        // Create a safe local path from the remote path
        val safeName = remotePath.replace(Regex("[^a-zA-Z0-9._/\\-]"), "_").trimStart('/')
        return File(cacheDir ?: File("/tmp"), safeName)
    }

    fun clearCache() {
        cacheDir?.deleteRecursively()
        cacheDir?.mkdirs()
    }

    // ---------- MIME type resolver ----------

    private fun guessMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast(".", "").lowercase()
        return when (ext) {
            "txt", "log", "md", "csv" -> "text/plain"
            "html", "htm" -> "text/html"
            "css" -> "text/css"
            "js" -> "application/javascript"
            "json" -> "application/json"
            "xml" -> "text/xml"
            "pdf" -> "application/pdf"
            "zip" -> "application/zip"
            "gz", "tar" -> "application/gzip"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "svg" -> "image/svg+xml"
            "bmp" -> "image/bmp"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "ogg" -> "audio/ogg"
            "flac" -> "audio/flac"
            "aac" -> "audio/aac"
            "mp4", "m4v" -> "video/mp4"
            "avi" -> "video/x-msvideo"
            "mkv" -> "video/x-matroska"
            "mov" -> "video/quicktime"
            "doc", "docx" -> "application/msword"
            "xls", "xlsx" -> "application/vnd.ms-excel"
            "ppt", "pptx" -> "application/vnd.ms-powerpoint"
            "py" -> "text/x-python"
            "java", "kt", "kts" -> "text/x-java-source"
            "c", "cpp", "h" -> "text/x-c"
            "sh", "bash" -> "application/x-sh"
            "swift" -> "text/x-swift"
            "dart" -> "text/plain"
            "yaml", "yml" -> "text/yaml"
            "toml" -> "text/plain"
            "rs" -> "text/x-rust"
            "go" -> "text/x-go"
            "rb" -> "text/x-ruby"
            "dmg" -> "application/x-apple-diskimage"
            "apk" -> "application/vnd.android.package-archive"
            "ipa" -> "application/octet-stream"
            else -> "application/octet-stream"
        }
    }
}
