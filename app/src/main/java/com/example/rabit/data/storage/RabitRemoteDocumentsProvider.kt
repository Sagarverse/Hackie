package com.example.rabit.data.storage

import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Point
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import android.util.Log
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileNotFoundException

/**
 * RabitRemoteDocumentsProvider — Android Storage Access Framework (SAF) provider
 * that exposes remote device filesystems (via SSH or Helper) as a virtual storage volume
 * visible in Android's Files app and all third-party file managers.
 *
 * Document IDs are the remote file paths themselves (e.g. "/Users/john/Desktop/readme.txt").
 * The root document ID is "/" or the home directory path.
 */
class RabitRemoteDocumentsProvider : DocumentsProvider() {

    companion object {
        private const val TAG = "RabitDocProvider"
        private const val ROOT_ID = "hackie_remote"
        private const val ROOT_DOC_ID = "/"

        private val DEFAULT_ROOT_PROJECTION = arrayOf(
            Root.COLUMN_ROOT_ID,
            Root.COLUMN_MIME_TYPES,
            Root.COLUMN_FLAGS,
            Root.COLUMN_ICON,
            Root.COLUMN_TITLE,
            Root.COLUMN_SUMMARY,
            Root.COLUMN_DOCUMENT_ID
        )

        private val DEFAULT_DOCUMENT_PROJECTION = arrayOf(
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_LAST_MODIFIED,
            Document.COLUMN_FLAGS,
            Document.COLUMN_SIZE
        )
    }

    override fun onCreate(): Boolean {
        Log.i(TAG, "DocumentsProvider created")
        return true
    }

    // ---------- Roots ----------

    override fun queryRoots(projection: Array<out String>?): Cursor {
        val result = MatrixCursor(resolveRootProjection(projection))

        // Only show the root when mounted
        if (!RemoteStorageManager.isMounted) {
            return result
        }

        val row = result.newRow()
        row.add(Root.COLUMN_ROOT_ID, ROOT_ID)
        row.add(Root.COLUMN_MIME_TYPES, "*/*")
        row.add(
            Root.COLUMN_FLAGS,
            Root.FLAG_SUPPORTS_CREATE or
                Root.FLAG_SUPPORTS_SEARCH or
                Root.FLAG_SUPPORTS_IS_CHILD
        )
        row.add(Root.COLUMN_ICON, com.example.rabit.R.mipmap.ic_launcher)
        row.add(Root.COLUMN_TITLE, "Hackie Remote")

        val summary = when {
            RemoteStorageManager.sshHost.isNotBlank() -> "SSH: ${RemoteStorageManager.sshUser}@${RemoteStorageManager.sshHost}"
            RemoteStorageManager.helperBaseUrl.isNotBlank() -> "Helper: Connected"
            else -> "Remote Device"
        }
        row.add(Root.COLUMN_SUMMARY, summary)
        row.add(Root.COLUMN_DOCUMENT_ID, ROOT_DOC_ID)

        return result
    }

    // ---------- Document queries ----------

    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor {
        val result = MatrixCursor(resolveDocumentProjection(projection))

        if (documentId == ROOT_DOC_ID || documentId == "/") {
            val row = result.newRow()
            row.add(Document.COLUMN_DOCUMENT_ID, ROOT_DOC_ID)
            row.add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR)
            row.add(Document.COLUMN_DISPLAY_NAME, "Hackie Remote")
            row.add(Document.COLUMN_LAST_MODIFIED, System.currentTimeMillis())
            row.add(
                Document.COLUMN_FLAGS,
                Document.FLAG_DIR_SUPPORTS_CREATE
            )
            row.add(Document.COLUMN_SIZE, 0L)
            return result
        }

        val entry = RemoteStorageManager.statFile(documentId)
        if (entry != null) {
            addFileRow(result, entry)
        } else {
            // Best-effort: construct from path
            val name = documentId.substringAfterLast("/")
            val row = result.newRow()
            row.add(Document.COLUMN_DOCUMENT_ID, documentId)
            row.add(Document.COLUMN_MIME_TYPE, "application/octet-stream")
            row.add(Document.COLUMN_DISPLAY_NAME, name)
            row.add(Document.COLUMN_LAST_MODIFIED, System.currentTimeMillis())
            row.add(Document.COLUMN_FLAGS, Document.FLAG_SUPPORTS_DELETE or Document.FLAG_SUPPORTS_RENAME or Document.FLAG_SUPPORTS_WRITE)
            row.add(Document.COLUMN_SIZE, 0L)
        }

        return result
    }

    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val result = MatrixCursor(resolveDocumentProjection(projection))

        val path = if (parentDocumentId.isBlank()) "/" else parentDocumentId
        val files = RemoteStorageManager.listFiles(path)

        for (entry in files) {
            addFileRow(result, entry)
        }

        // Allow the cursor to be refreshed (important for file managers)
        val uri = DocumentsContract.buildChildDocumentsUri(
            "${context!!.packageName}.remote.documents",
            parentDocumentId
        )
        result.setNotificationUri(context!!.contentResolver, uri)

        return result
    }

    // ---------- File operations ----------

    @Throws(FileNotFoundException::class)
    override fun openDocument(
        documentId: String,
        mode: String,
        signal: CancellationSignal?
    ): ParcelFileDescriptor {
        val isWrite = mode.contains("w") || mode.contains("r") && mode.contains("w")

        val localFile = RemoteStorageManager.readFile(documentId)
            ?: throw FileNotFoundException("Cannot download remote file: $documentId")

        return if (isWrite) {
            // Return a writable FD; when closed, upload back to remote
            val handler = Handler(Looper.getMainLooper())
            ParcelFileDescriptor.open(
                localFile,
                ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_CREATE,
                handler
            ) { exception ->
                if (exception == null) {
                    // File was closed — sync changes back to remote
                    Thread {
                        try {
                            val success = RemoteStorageManager.writeFile(documentId, localFile)
                            Log.i(TAG, "Write-back ${if (success) "succeeded" else "FAILED"} for $documentId")
                        } catch (e: Exception) {
                            Log.e(TAG, "Write-back error for $documentId", e)
                        }
                    }.start()
                } else {
                    Log.w(TAG, "File descriptor error for $documentId", exception)
                }
            }
        } else {
            ParcelFileDescriptor.open(localFile, ParcelFileDescriptor.MODE_READ_ONLY)
        }
    }

    override fun createDocument(
        parentDocumentId: String,
        mimeType: String,
        displayName: String
    ): String {
        val isDir = mimeType == Document.MIME_TYPE_DIR
        val createdPath = RemoteStorageManager.createFile(parentDocumentId, displayName, isDir)
            ?: throw FileNotFoundException("Failed to create $displayName in $parentDocumentId")

        // Notify the parent that children changed
        notifyChildrenChanged(parentDocumentId)

        return createdPath
    }

    override fun deleteDocument(documentId: String) {
        val success = RemoteStorageManager.deleteFile(documentId)
        if (!success) {
            throw FileNotFoundException("Failed to delete: $documentId")
        }

        val parent = documentId.substringBeforeLast("/").ifEmpty { "/" }
        notifyChildrenChanged(parent)
    }

    override fun renameDocument(documentId: String, displayName: String): String {
        val newPath = RemoteStorageManager.renameFile(documentId, displayName)
            ?: throw FileNotFoundException("Failed to rename $documentId to $displayName")

        val parent = documentId.substringBeforeLast("/").ifEmpty { "/" }
        notifyChildrenChanged(parent)

        return newPath
    }

    override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean {
        return documentId.startsWith(parentDocumentId)
    }

    // ---------- Helpers ----------

    private fun addFileRow(cursor: MatrixCursor, entry: RemoteStorageManager.RemoteFileEntry) {
        val row = cursor.newRow()
        row.add(Document.COLUMN_DOCUMENT_ID, entry.path)
        row.add(Document.COLUMN_MIME_TYPE, entry.mimeType)
        row.add(Document.COLUMN_DISPLAY_NAME, entry.name)
        row.add(Document.COLUMN_LAST_MODIFIED, entry.lastModified)

        var flags = 0
        if (entry.isDirectory) {
            flags = flags or Document.FLAG_DIR_SUPPORTS_CREATE
        } else {
            flags = flags or Document.FLAG_SUPPORTS_WRITE
        }
        flags = flags or Document.FLAG_SUPPORTS_DELETE
        flags = flags or Document.FLAG_SUPPORTS_RENAME
        row.add(Document.COLUMN_FLAGS, flags)
        row.add(Document.COLUMN_SIZE, entry.size)
    }

    private fun notifyChildrenChanged(parentDocumentId: String) {
        val ctx = context ?: return
        val uri = DocumentsContract.buildChildDocumentsUri(
            "${ctx.packageName}.remote.documents",
            parentDocumentId
        )
        ctx.contentResolver.notifyChange(uri, null)
    }

    private fun resolveRootProjection(projection: Array<out String>?): Array<String> {
        return projection?.map { it }?.toTypedArray() ?: DEFAULT_ROOT_PROJECTION
    }

    private fun resolveDocumentProjection(projection: Array<out String>?): Array<String> {
        return projection?.map { it }?.toTypedArray() ?: DEFAULT_DOCUMENT_PROJECTION
    }
}
