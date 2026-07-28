package com.example.alphamusic.core.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricsSource @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchLyrics(artistName: String, trackName: String): List<LyricLine>? =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://lrclib.net/api/get?artist_name=${artistName.encodeUrl()}&track_name=${trackName.encodeUrl()}")
                    .header("User-Agent", "AlphaMusic/1.0 (arghamuhury)")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext null

                val body = response.body?.string() ?: return@withContext null
                val lyricsResponse = json.decodeFromString<LyricsResponse>(body)

                if (lyricsResponse.instrumental == true) return@withContext emptyList()

                val synced = lyricsResponse.syncedLyrics
                if (synced != null) {
                    parseLrc(synced)
                } else {
                    lyricsResponse.plainLyrics?.let { plain ->
                        listOf(LyricLine(timestampMs = 0L, text = plain))
                    }
                }
            } catch (_: Exception) {
                null
            }
        }

    private fun parseLrc(lrc: String): List<LyricLine> {
        val lineRegex = Regex("""\[(\d+):(\d+\.\d+)](.*)""")
        return lrc.lines().mapNotNull { line ->
            lineRegex.find(line)?.let { match ->
                val minutes = match.groupValues[1].toIntOrNull() ?: return@let null
                val seconds = match.groupValues[2].toFloatOrNull() ?: return@let null
                val text = match.groupValues[3].trim()
                if (text.isBlank()) return@let null
                LyricLine(
                    timestampMs = (minutes * 60_000L + (seconds * 1000L).toLong()),
                    text = text
                )
            }
        }
    }

    private fun String.encodeUrl(): String {
        return java.net.URLEncoder.encode(this, "UTF-8")
    }
}
