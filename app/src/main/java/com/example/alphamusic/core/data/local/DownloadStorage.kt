package com.example.alphamusic.core.data.local

import android.content.Context
import android.net.Uri
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.alphamusic.core.data.AppSettings
import com.example.alphamusic.core.domain.models.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

@Singleton
class DownloadStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val appSettings: AppSettings
) {
    private val downloadsDir: File
        get() = File(context.filesDir, DOWNLOADS_DIR).apply { mkdirs() }

    private val cacheDirs: List<File>
        get() = listOfNotNull(context.cacheDir, context.externalCacheDir)

    suspend fun downloadTrack(
        track: Track,
        onProgress: ((Float) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(track.streamUrl.isNotBlank()) { "Track stream URL is empty" }
            check(!appSettings.downloadWifiOnly || isWifiConnected()) {
                "Downloads are restricted to Wi-Fi in Settings"
            }

            val targetFile = File(downloadsDir, track.safeDownloadFileName())
            val tempFile = File(downloadsDir, "${targetFile.name}.tmp")

            if (targetFile.exists() && targetFile.length() > 0L) {
                return@runCatching Uri.fromFile(targetFile).toString()
            }

            val request = Request.Builder()
                .url(track.streamUrl)
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Download failed with HTTP ${response.code}" }
                val body = response.body ?: error("Download response body is empty")
                val contentLength = body.contentLength()

                tempFile.outputStream().use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalBytesRead = 0L
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            if (onProgress != null) {
                                if (contentLength > 0L) {
                                    onProgress(totalBytesRead.toFloat() / contentLength.toFloat())
                                } else {
                                    onProgress(-1f)
                                }
                            }
                        }
                    }
                }
            }

            check(tempFile.length() > 0L) { "Downloaded file is empty" }
            if (targetFile.exists()) targetFile.delete()
            check(tempFile.renameTo(targetFile)) { "Unable to save downloaded file" }
            Uri.fromFile(targetFile).toString()
        }.onFailure {
            tempFileFor(track).delete()
        }
    }

    suspend fun removeTrack(track: Track): Boolean = withContext(Dispatchers.IO) {
        fileFor(track).deleteIfExists() or tempFileFor(track).deleteIfExists()
    }

    suspend fun removeAllDownloads(): Int = withContext(Dispatchers.IO) {
        downloadsDir.listFiles()?.count { it.deleteIfExists() } ?: 0
    }

    suspend fun clearCache(): Long = withContext(Dispatchers.IO) {
        cacheDirs.sumOf { it.deleteChildren() }
    }

    suspend fun downloadsSizeBytes(): Long = withContext(Dispatchers.IO) {
        downloadsDir.sizeBytes()
    }

    suspend fun cacheSizeBytes(): Long = withContext(Dispatchers.IO) {
        cacheDirs.sumOf { it.sizeBytes() }
    }

    fun hasDownloadedFile(localUri: String?): Boolean {
        if (localUri.isNullOrBlank()) return false
        val file = runCatching { File(Uri.parse(localUri).path.orEmpty()) }.getOrNull() ?: return false
        return file.exists() && file.length() > 0L
    }

    private fun fileFor(track: Track): File = File(downloadsDir, track.safeDownloadFileName())
    private fun tempFileFor(track: Track): File = File(downloadsDir, "${track.safeDownloadFileName()}.tmp")

    private fun isWifiConnected(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun Track.safeDownloadFileName(): String {
        val safeId = id.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { title.hashCode().toString() }
        return "$safeId.audio"
    }

    private fun File.deleteIfExists(): Boolean = !exists() || deleteRecursively()

    private fun File.deleteChildren(): Long {
        if (!exists()) return 0L
        return listFiles()?.sumOf { child ->
            val size = child.sizeBytes()
            if (child.deleteRecursively()) size else 0L
        } ?: 0L
    }

    private fun File.sizeBytes(): Long {
        if (!exists()) return 0L
        if (isFile) return length()
        return listFiles()?.sumOf { it.sizeBytes() } ?: 0L
    }

    private companion object {
        const val DOWNLOADS_DIR = "offline_tracks"
    }
}
