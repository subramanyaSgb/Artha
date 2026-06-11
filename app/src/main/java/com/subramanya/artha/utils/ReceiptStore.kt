package com.subramanya.artha.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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

    /**
     * Copies the image at [sourceUri] into app-private storage (downsampled JPEG).
     * Returns the stable `file://` URI string to persist on the transaction, or null
     * if the source can't be read.
     */
    suspend fun persist(context: Context, sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            // Pass 1: bounds only, to pick a power-of-two downsample factor.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(sourceUri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            } ?: return@runCatching null
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
            }
            // Pass 2: real decode (content streams aren't seekable, so reopen).
            val bitmap = resolver.openInputStream(sourceUri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return@runCatching null
            val dir = File(context.filesDir, DIR_NAME).apply { mkdirs() }
            val file = File(dir, "${UUID.randomUUID()}.jpg")
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
