package com.subramanya.artha.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Persists uploaded insurance policy documents into app-private storage, UNMODIFIED.
 *
 * Unlike [ReceiptStore] (which re-encodes everything to a single JPEG), this copies the
 * raw bytes verbatim — so a multi-page PDF stays a real multi-page PDF the user can open
 * in an external viewer, not a 1-page image. Images (photos of a policy) are also copied
 * verbatim; no re-encode needed here.
 *
 * Docs live in their own `filesDir/policy_docs/` dir (separate from receipts) so the two
 * prune passes don't fight over one directory. The dir is exposed to FileProvider via
 * `@xml/file_paths` so [viewIntent] can vend a content:// uri for ACTION_VIEW.
 */
object PolicyDocStore {

    private const val DIR_NAME = "policy_docs"

    /** The app-private policy-docs directory (created on demand). */
    fun dir(context: Context): File = File(context.filesDir, DIR_NAME).apply { mkdirs() }

    private fun allFiles(context: Context): List<File> =
        dir(context).listFiles()?.filter { it.isFile }.orEmpty()

    /** True if [bytes] begins with the PDF magic marker `%PDF`. Pure — unit-testable. */
    fun isPdfBytes(bytes: ByteArray): Boolean =
        bytes.size >= 4 && bytes[0] == '%'.code.toByte() && bytes[1] == 'P'.code.toByte() &&
            bytes[2] == 'D'.code.toByte() && bytes[3] == 'F'.code.toByte()

    /**
     * Copies the document at [sourceUri] into `filesDir/policy_docs/` verbatim.
     * PDFs keep a `.pdf` extension, everything else falls back to `.jpg`. Returns the
     * stable absolute path to persist on the insurance, or null if the source can't be read.
     *
     * Picker URIs are one-time-use grants, so the bytes are read in a single pass.
     */
    suspend fun persist(context: Context, sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = context.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
                ?: return@runCatching null
            val mime = context.contentResolver.getType(sourceUri)
            val isPdf = mime == "application/pdf" || isPdfBytes(bytes)
            val ext = when {
                isPdf -> "pdf"
                mime == "image/png" -> "png"
                else -> "jpg"
            }
            val file = File(dir(context), "${UUID.randomUUID()}.$ext")
            file.writeBytes(bytes)
            file.absolutePath
        }.getOrNull()
    }

    /**
     * Deletes stored policy docs no insurance references anymore. Matching is by file name
     * (the stored path's last segment). Run at startup off the main thread.
     */
    suspend fun pruneOrphans(context: Context, referencedUris: Collection<String>) =
        withContext(Dispatchers.IO) {
            val keep = referencedUris
                .map { it.substringAfterLast('/') }
                .filterTo(HashSet()) { it.isNotBlank() }
            allFiles(context).forEach { file ->
                if (file.name !in keep) file.delete()
            }
        }

    /**
     * Builds an ACTION_VIEW intent that opens the stored doc in an external viewer via
     * FileProvider (files inside app-private storage can't be shared as raw file:// uris).
     * Returns null if the file is missing.
     */
    fun viewIntent(context: Context, storedUri: String): Intent? {
        val file = File(storedUri).takeIf { it.exists() } ?: return null
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val mime = if (file.extension.equals("pdf", ignoreCase = true)) "application/pdf" else "image/*"
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
