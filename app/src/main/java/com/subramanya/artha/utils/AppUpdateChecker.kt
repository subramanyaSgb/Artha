package com.subramanya.artha.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.subramanya.artha.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionName: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val apkSizeBytes: Long,
)

/**
 * Checks GitHub Releases for a newer APK and downloads it.
 *
 * Release convention: tag must be "v{versionName}" (e.g. "v0.2.0") and the
 * release must have one asset whose name ends in ".apk".
 *
 * Publish flow: build a release APK, create a GitHub Release tagged vX.Y.Z,
 * attach the APK asset. The app picks it up on next launch.
 */
class AppUpdateChecker(private val context: Context) {

    private val apiUrl = "https://api.github.com/repos/subramanyaSgb/Artha/releases/latest"

    /** Returns [UpdateInfo] if a newer release is found, null otherwise. */
    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/vnd.github+json")
        }
        try {
            if (conn.responseCode != 200) return@withContext null
            val body = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(body)

            val tagName = json.optString("tag_name").removePrefix("v")
            if (tagName.isBlank()) return@withContext null

            // Only show update if GitHub version is strictly newer than installed
            if (!isNewer(tagName, BuildConfig.VERSION_NAME)) return@withContext null

            val releaseNotes = json.optString("body").take(400).trim()
            val assets = json.optJSONArray("assets") ?: return@withContext null

            var downloadUrl = ""
            var sizeBytes = 0L
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.optString("name").endsWith(".apk")) {
                    downloadUrl = asset.optString("browser_download_url")
                    sizeBytes = asset.optLong("size")
                    break
                }
            }
            if (downloadUrl.isBlank()) return@withContext null

            UpdateInfo(
                versionName = tagName,
                releaseNotes = releaseNotes,
                downloadUrl = downloadUrl,
                apkSizeBytes = sizeBytes,
            )
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Downloads the APK to the app cache dir, calling [onProgress] with 0f–1f.
     * Returns the local [File] on success, null on failure.
     */
    suspend fun downloadApk(
        downloadUrl: String,
        onProgress: (Float) -> Unit,
    ): File? = withContext(Dispatchers.IO) {
        val outFile = File(context.cacheDir, "artha-update.apk")
        try {
            val conn = (URL(downloadUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 60_000
            }
            // Follow redirects manually (GitHub release assets redirect to S3)
            var finalConn = conn
            if (conn.responseCode in 301..302) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                finalConn = (URL(location).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 60_000
                }
            }
            val total = finalConn.contentLengthLong.takeIf { it > 0 } ?: -1L
            var downloaded = 0L
            finalConn.inputStream.use { input ->
                outFile.outputStream().use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (total > 0) onProgress(downloaded.toFloat() / total)
                    }
                }
            }
            finalConn.disconnect()
            outFile
        } catch (_: Exception) {
            outFile.delete()
            null
        }
    }

    /** Launches the system package installer for the given APK file. */
    fun triggerInstall(apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /** Semver comparison: returns true if [candidate] > [current]. */
    private fun isNewer(candidate: String, current: String): Boolean {
        val c = candidate.split(".").mapNotNull { it.toIntOrNull() }
        val r = current.split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(c.size, r.size)) {
            val cv = c.getOrElse(i) { 0 }
            val rv = r.getOrElse(i) { 0 }
            if (cv != rv) return cv > rv
        }
        return false
    }
}
