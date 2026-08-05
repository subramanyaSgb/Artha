package com.subramanya.artha.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

/**
 * Rasterizes the first [maxPages] pages of a PDF (or decodes a single image) at [uri]
 * into base64 JPEG strings — the input PolicyDocParser.parse expects. Returns whatever
 * pages rendered successfully (empty list if nothing could be read).
 *
 * PDFs go through android.graphics.pdf.PdfRenderer (built-in, no dependency). If the uri
 * is an image (not a PDF), it's decoded directly to one base64 entry. Picker URIs are
 * one-time-use, so bytes are buffered up front. ponytail: first-3-pages cap — a "scan
 * more pages" control is a future enhancement, not needed until a real policy hides data
 * past page 3.
 *
 * Blocking (I/O + bitmap rasterization) — call on Dispatchers.IO.
 */
fun renderPolicyPagesToBase64(context: Context, uri: Uri, maxPages: Int = 3): List<String> {
    // The doc is already copied into app-private storage before we get here, so [uri] is an
    // absolute path / file:// (read via File), not a picker content:// grant. Fall back to the
    // ContentResolver only for a real content:// uri. Buffer once, then work from memory.
    val path = if (uri.scheme == null) uri.toString() else uri.path
    val localFile = path?.let(::File)?.takeIf { it.isAbsolute && it.exists() }
    val bytes = if (localFile != null) {
        localFile.readBytes()
    } else {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return emptyList()
    }

    // Image (not PDF) → decode straight to one base64 entry. Detect by magic bytes (works for a
    // File too) rather than only the resolver's mime.
    val isPdf = bytes.size >= 4 && bytes[0] == '%'.code.toByte() && bytes[1] == 'P'.code.toByte() &&
        bytes[2] == 'D'.code.toByte() && bytes[3] == 'F'.code.toByte()
    if (!isPdf) {
        return runCatching {
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return emptyList()
            listOf(bitmapToBase64(bitmap))
        }.getOrDefault(emptyList())
    }

    // PDF: PdfRenderer needs a seekable ParcelFileDescriptor, so spill the buffer to a temp file.
    val temp = File(context.cacheDir, "policy_${UUID.randomUUID()}.pdf")
    return try {
        temp.writeBytes(bytes)
        ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                val results = ArrayList<String>()
                for (i in 0 until minOf(maxPages, renderer.pageCount)) {
                    runCatching { renderPage(renderer, i) }.getOrNull()?.let { results.add(it) }
                }
                results
            }
        }
    } catch (e: Exception) {
        emptyList()
    } finally {
        temp.delete()
    }
}

/** Renders one PDF page to a white-backed base64 JPEG (~1500px on the longer side). */
private fun renderPage(renderer: PdfRenderer, index: Int): String {
    renderer.openPage(index).use { page ->
        // PdfRenderer's page width/height are in points at 72dpi; scale so the longer side ~1500px.
        val scale = TARGET_LONG_EDGE_PX.toFloat() / maxOf(page.width, page.height)
        val w = (page.width * scale).toInt().coerceAtLeast(1)
        val h = (page.height * scale).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        // PDF pages are transparent; without a white fill text renders on black and vision reads poorly.
        bitmap.eraseColor(Color.WHITE)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        return bitmapToBase64(bitmap)
    }
}

/**
 * JPEG 85, Base64 NO_WRAP — same output format as UpiReceiptParser. Rendered PDF pages are
 * already ~1500px, but the image-passthrough branch can hand us a huge photo, so cap the
 * longer side at [TARGET_LONG_EDGE_PX] to keep the base64 payload sane.
 *
 * Owns the recycle of [bitmap]: callers must not touch it after this returns.
 */
private fun bitmapToBase64(bitmap: Bitmap): String {
    val out = ByteArrayOutputStream()
    val longer = maxOf(bitmap.width, bitmap.height)
    val scaled = if (longer > TARGET_LONG_EDGE_PX) {
        val scale = TARGET_LONG_EDGE_PX.toFloat() / longer
        Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
    } else bitmap
    scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
    // Recycle the scaled copy only if downscaling actually allocated a new one, then the input.
    if (scaled !== bitmap) scaled.recycle()
    bitmap.recycle()
    return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
}

private const val TARGET_LONG_EDGE_PX = 1500
