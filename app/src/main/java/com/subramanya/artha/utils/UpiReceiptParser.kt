package com.subramanya.artha.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.subramanya.artha.utils.upi.PhonePeReceiptParser
import com.subramanya.artha.utils.upi.UpiParsedReceipt
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Runs on-device ML Kit OCR on a shared image URI, then dispatches to the
 * per-app parser that knows the receipt layout.
 *
 * Currently supports: PhonePe.
 * Future: GPay, Paytm, CRED — add a new parser object + dispatch entry here.
 */
class UpiReceiptParser {

    suspend fun parse(context: Context, uri: Uri): UpiParsedReceipt? {
        val text = runOcr(context, uri)
        return PhonePeReceiptParser.parse(text)
    }

    private suspend fun runOcr(context: Context, uri: Uri): String =
        suspendCancellableCoroutine { cont ->
            try {
                // Decode via ContentResolver — works for both FileProvider and MediaStore URIs.
                val bitmap = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)
                }
                if (bitmap == null) {
                    cont.resume("")
                    return@suspendCancellableCoroutine
                }
                val image = InputImage.fromBitmap(bitmap, 0)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                recognizer.process(image)
                    .addOnSuccessListener { result ->
                        recognizer.close()
                        cont.resume(result.text)
                    }
                    .addOnFailureListener { e ->
                        recognizer.close()
                        cont.resumeWithException(e)
                    }
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        }
}
