package com.example.alphamusic.core.data

import android.util.Base64
import com.example.alphamusic.core.domain.models.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JioSaavnMusicSource @Inject constructor(
    private val client: OkHttpClient
) : MusicSource {

    private val DES_KEY = "38346591"

    override suspend fun searchTracks(query: String): Result<List<Track>> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://www.jiosaavn.com/api.php?__call=search.getResults&q=$encodedQuery&n=50&p=1&_format=json&_marker=0&api_version=4&ctx=web6dot0"
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()
                
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext Result.failure(Exception("Failed to fetch search results"))
            
            val jsonString = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response body"))
            val root = JSONObject(jsonString)
            val resultsArray = root.optJSONArray("results") ?: return@withContext Result.success(emptyList())

            val deferredTracks = (0 until resultsArray.length()).map { i ->
                async {
                    val obj = resultsArray.getJSONObject(i)
                    val id = obj.optString("id", "")
                    if (id.isEmpty()) return@async null

                    val title = obj.optString("title", obj.optString("song", "Unknown")).replace("&quot;", "\"").replace("&amp;", "&").replace("&#039;", "'")
                    val subtitle = obj.optString("subtitle", obj.optString("primary_artists", obj.optString("singers", "Unknown"))).replace("&quot;", "\"").replace("&amp;", "&").replace("&#039;", "'")
                    var image = obj.optString("image", "")
                    if (image.isNotEmpty()) {
                        image = image.replace("150x150", "500x500").replace("50x50", "500x500")
                    }

                    // Fetch Stream URL
                    val streamInfo = getStreamUrl(id) ?: return@async null
                    val streamUrl = streamInfo.first
                    val durationMs = streamInfo.second

                    Track(
                        id = id,
                        title = title,
                        artistName = subtitle.ifEmpty { "Unknown" },
                        albumName = obj.optString("album", "Unknown"),
                        coverUrl = image,
                        streamUrl = streamUrl,
                        durationMs = durationMs
                    )
                }
            }

            val tracks = deferredTracks.awaitAll().filterNotNull()
            Result.success(tracks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTrendingTracks(): Result<List<Track>> = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.jiosaavn.com/api.php?__call=webapi.get&token=I3kvhipIy73uCJW60TJk1Q__&type=playlist&p=1&n=50&includeMetaTags=0&ctx=web6dot0&api_version=4&_format=json&_marker=0"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext Result.failure(Exception("Failed to fetch trending"))

            val jsonString = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response body"))
            val root = JSONObject(jsonString)
            val newTrendingArray = root.optJSONArray("list") ?: return@withContext Result.success(emptyList())

            val deferredTracks = (0 until newTrendingArray.length()).map { i ->
                async {
                    val obj = newTrendingArray.getJSONObject(i)
                    val type = obj.optString("type", "")
                    if (type.isNotEmpty() && type != "song") return@async null

                    val id = obj.optString("id", "")
                    if (id.isEmpty()) return@async null

                    val title = obj.optString("title", "Unknown").replace("&quot;", "\"").replace("&amp;", "&").replace("&#039;", "'")
                    val subtitle = obj.optString("subtitle", "Unknown").replace("&quot;", "\"").replace("&amp;", "&").replace("&#039;", "'")
                    var image = obj.optString("image", "")
                    if (image.isNotEmpty()) {
                        image = image.replace("150x150", "500x500").replace("50x50", "500x500")
                    }

                    // Fetch Stream URL
                    val streamInfo = getStreamUrl(id) ?: return@async null
                    val streamUrl = streamInfo.first
                    val durationMs = streamInfo.second

                    Track(
                        id = id,
                        title = title,
                        artistName = subtitle.ifEmpty { "Unknown" },
                        albumName = "Trending",
                        coverUrl = image,
                        streamUrl = streamUrl,
                        durationMs = durationMs
                    )
                }
            }

            val tracks = deferredTracks.awaitAll().filterNotNull()
            Result.success(tracks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getStreamUrl(songId: String): Pair<String, Long>? {
        try {
            val url = "https://www.jiosaavn.com/api.php?__call=song.getDetails&pids=$songId&_format=json&_marker=0&ctx=android"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonString = response.body?.string() ?: return null
                val root = JSONObject(jsonString)
                val songObj = root.optJSONObject(songId)
                if (songObj != null) {
                    val encryptedUrl = songObj.optString("encrypted_media_url", "")
                    val durationSeconds = songObj.optLong("duration", 0L)
                    if (encryptedUrl.isNotEmpty()) {
                        val decrypted = decryptUrl(encryptedUrl)
                        if (decrypted != null) {
                            return Pair(decrypted, durationSeconds * 1000L)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun decryptUrl(encrypted: String): String? {
        try {
            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(DES_KEY.toByteArray(), "DES"))
            val decodedBytes = Base64.decode(encrypted.trim(), Base64.DEFAULT)
            val decryptedStr = String(cipher.doFinal(decodedBytes))
            
            return decryptedStr.replace("_96.mp4", "_320.mp4").replace("_160.mp4", "_320.mp4")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
