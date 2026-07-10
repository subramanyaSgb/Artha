package com.subramanya.artha.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Persists receipt images into app-private storage and loads them off the main thread.
 *
 * Why copy at all: the photo picker returns a transient `content://` URI whose read
 * grant dies with the process (no persistable permission is offered for picker URIs),
 * so storing the raw URI made receipts invisible after a restart. Persisting copies
 * the image once into `filesDir/receipts/` and stores a stable `file://` URI instead.
 *
 * Images are downsampled to [MAX_DIMENSION] and re-encoded as JPEG so a 12 MP photo
 * doesn't balloon app storage or allocate a ~50 MB bitmap on display.
 */
object ReceiptStore {

    private const val DIR_NAME = "receipts"
    private const val MAX_DIMENSION = 1600
    private const val JPEG_QUALITY = 85

    /** The app-private receipts directory (created on demand). */
    fun dir(context: Context): File = File(context.filesDir, DIR_NAME).apply { mkdirs() }

    /** Every stored receipt image — what the full-backup archive packages. */
    fun allFiles(context: Context): List<File> = dir(context).listFiles()?.filter { it.isFile }.orEmpty()

    /**
     * Deletes stored receipts that no transaction references anymore — files orphaned
     * by a transaction delete or a receipt replacement. Matching is by file name
     * (the URI's last path segment). Run at startup off the main thread.
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
     * Copies the image at [sourceUri] into app-private storage (downsampled JPEG).
     * Returns the stable `file://` URI string to persist on the transaction, or null
     * if the source can't be read.
     *
     * Picker URIs (MediaStore one-time grants) can only be opened once before the grant
     * is consumed. We read the full bytes in a single stream, then work entirely from
     * the in-memory buffer for bounds, EXIF, and decode — no re-opening.
     */
    suspend fun persist(context: Context, sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            // Single read: buffer the whole image so we can inspect it multiple times.
            val bytes = resolver.openInputStream(sourceUri)?.use { it.readBytes() }
                ?: return@runCatching null

            // Pass 1: bounds only (no pixel allocation).
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

            // Pass 2: real decode with downsample factor.
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
            }
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                ?: return@runCatching null

            // Pass 3: EXIF rotation from the same buffer.
            val rotationDegrees = runCatching {
                ExifInterface(bytes.inputStream()).rotationDegrees
            }.getOrDefault(0)

            val bitmap = if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                    .also { if (it !== decoded) decoded.recycle() }
            } else {
                decoded
            }
            val file = File(dir(context), "${UUID.randomUUID()}.jpg")
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            bitmap.recycle()
            Uri.fromFile(file).toString()
        }.getOrNull()
    }

    /**
     * Decodes the receipt at [uri] (file:// or legacy content://) off the main thread.
     * Returns null when unreadable — e.g. a legacy content:// URI whose grant was
     * revoked; callers show their text fallback in that case.
     */
    suspend fun loadBitmap(context: Context, uri: String): ImageBitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val parsed = Uri.parse(uri)
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(parsed)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            } ?: return@runCatching null
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
            }
            resolver.openInputStream(parsed)
                ?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?.asImageBitmap()
        }.getOrNull()
    }

    private fun sampleSizeFor(width: Int, height: Int): Int {
        var sample = 1
        while (maxOf(width, height) / (sample * 2) >= MAX_DIMENSION) sample *= 2
        return sample
    }
}
