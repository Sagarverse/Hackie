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
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope

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

    @Volatile var adbIp: String = ""
    @Volatile var adbPort: Int = 5555
    @Volatile var adbClient: com.example.rabit.data.adb.RabitAdbClient? = null

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
        adbIp = prefs.getString("adb_ip", "") ?: ""
        adbPort = prefs.getInt("adb_port", 5555)

        cacheDir = File(context.cacheDir, CACHE_DIR_NAME).also { it.mkdirs() }

        if (adbIp.isNotBlank()) {
            val crypto = com.example.rabit.data.adb.RabitAdbCrypto.getCrypto(context)
            adbClient = com.example.rabit.data.adb.RabitAdbClient(crypto)
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                runCatching { adbClient?.connectWifi(adbIp, adbPort) }
            }
        }

        // Try establishing SSH/SFTP; ok if it fails — we'll retry lazily
        ensureSshConnected()
        isMounted = true
        Log.i(TAG, "Remote storage mounted (ssh=${sshHost.isNotBlank()}, helper=${helperBaseUrl.isNotBlank()})")
    }

    fun unmount() {
        isMounted = false
        runCatching { sftpChannel?.disconnect() }
        runCatching { sshSession?.disconnect() }
        runCatching { adbClient?.disconnect() }
        sftpChannel = null
        sshSession = null
        adbClient = null
        Log.i(TAG, "Remote storage unmounted")
    }

    val isConnected: Boolean
        get() = isSshReady() || helperBaseUrl.isNotBlank() || (adbClient != null && adbIp.isNotBlank())

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
        // Try ADB first
        if (adbClient != null) {
            val list = runCatching { kotlinx.coroutines.runBlocking { listFilesAdb(path) } }.getOrNull()
            if (list != null && list.isNotEmpty()) return list
        }
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
        if (adbClient != null) {
            val entry = runCatching { kotlinx.coroutines.runBlocking { statFileAdb(remotePath) } }.getOrNull()
            if (entry != null) return entry
        }
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

        // Try ADB
        if (adbClient != null) {
            val f = runCatching { readFileAdb(remotePath, cacheFile) }.getOrNull()
            if (f != null) return f
        }
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
        if (adbClient != null) {
            val success = runCatching { writeFileAdb(remotePath, localFile) }.getOrNull()
            if (success == true) return true
        }
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
        if (adbClient != null) {
            val success = runCatching { deleteFileAdb(remotePath) }.getOrNull()
            if (success == true) return true
        }
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

        if (adbClient != null) {
            val success = runCatching { createFileAdb(fullPath, isDirectory) }.getOrNull()
            if (success == true) return fullPath
        }
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

        if (adbClient != null) {
            val success = runCatching { renameFileAdb(oldPath, newPath) }.getOrNull()
            if (success == true) return newPath
        }
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

    // ---------- ADB helpers ----------

    private suspend fun listFilesAdb(path: String): List<RemoteFileEntry> {
        val client = adbClient ?: return emptyList()
        val safePath = if (path.isBlank() || path == "/") "/sdcard" else path.replace("'", "'\\''")
        
        val cmd = "for f in '$safePath'/.* '$safePath'/*; do stat -c '%A|%s|%Y|%n' \"\$f\" 2>/dev/null; done"
        val out = client.executeCommand(cmd)
        
        val entries = mutableListOf<RemoteFileEntry>()
        val lines = out.trim().split("\n")
        
        for (line in lines) {
            val p = line.split("|")
            if (p.size >= 4) {
                val perms = p[0]
                val size = p[1].toLongOrNull() ?: 0L
                val time = (p[2].toLongOrNull() ?: 0L) * 1000L
                val fullPath = line.substring(line.indexOf('|', line.indexOf('|', line.indexOf('|') + 1) + 1) + 1).trim()
                val name = fullPath.substringAfterLast("/")
                
                if (name == "." || name == "..") continue
                if (name == "*") continue // unresolved glob
                
                val isDir = perms.startsWith("d")
                entries.add(
                    RemoteFileEntry(
                        name = name,
                        path = "$safePath/$name".replace("//", "/"),
                        isDirectory = isDir,
                        size = size,
                        lastModified = time,
                        mimeType = if (isDir) "vnd.android.document/directory" else guessMimeType(name)
                    )
                )
            }
        }
        return entries.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    private suspend fun statFileAdb(remotePath: String): RemoteFileEntry? {
        val client = adbClient ?: return null
        val safePath = remotePath.replace("'", "'\\''")
        val cmd = "stat -c '%A|%s|%Y|%n' '$safePath' 2>/dev/null"
        val out = client.executeCommand(cmd)
        val p = out.trim().split("|")
        if (p.size >= 4) {
            val perms = p[0]
            val size = p[1].toLongOrNull() ?: 0L
            val time = (p[2].toLongOrNull() ?: 0L) * 1000L
            val name = remotePath.substringAfterLast("/")
            val isDir = perms.startsWith("d")
            return RemoteFileEntry(
                name = name,
                path = remotePath,
                isDirectory = isDir,
                size = size,
                lastModified = time,
                mimeType = if (isDir) "vnd.android.document/directory" else guessMimeType(name)
            )
        }
        return null
    }

    private fun readFileAdb(remotePath: String, cacheFile: File): File? {
        val client = adbClient ?: return null
        return try {
            val safePath = remotePath.replace("'", "'\\''")
            val bytes = kotlinx.coroutines.runBlocking { client.downloadFileBytes(safePath) }
            cacheFile.parentFile?.mkdirs()
            FileOutputStream(cacheFile).use { it.write(bytes) }
            cacheFile
        } catch (e: Exception) {
            Log.e(TAG, "ADB readFile failed", e)
            null
        }
    }

    private fun writeFileAdb(remotePath: String, localFile: File): Boolean {
        val client = adbClient ?: return false
        return try {
            val safePath = remotePath.replace("'", "'\\''")
            val bytes = FileInputStream(localFile).readBytes()
            kotlinx.coroutines.runBlocking { client.uploadFileBytes(safePath, bytes) }
            true
        } catch (e: Exception) {
            Log.e(TAG, "ADB writeFile failed", e)
            false
        }
    }

    private fun deleteFileAdb(remotePath: String): Boolean {
        val client = adbClient ?: return false
        val safePath = remotePath.replace("'", "'\\''")
        val cmd = "rm -rf '$safePath'"
        kotlinx.coroutines.runBlocking { client.executeCommand(cmd) }
        return true
    }

    private fun createFileAdb(fullPath: String, isDirectory: Boolean): Boolean {
        val client = adbClient ?: return false
        val safePath = fullPath.replace("'", "'\\''")
        val cmd = if (isDirectory) "mkdir -p '$safePath'" else "touch '$safePath'"
        val out = kotlinx.coroutines.runBlocking { client.executeCommand(cmd) }
        return !out.contains("Permission denied")
    }

    private fun renameFileAdb(oldPath: String, newPath: String): Boolean {
        val client = adbClient ?: return false
        val safeOld = oldPath.replace("'", "'\\''")
        val safeNew = newPath.replace("'", "'\\''")
        val cmd = "mv '$safeOld' '$safeNew'"
        val out = kotlinx.coroutines.runBlocking { client.executeCommand(cmd) }
        return !out.contains("Permission denied") && !out.contains("No such file")
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
