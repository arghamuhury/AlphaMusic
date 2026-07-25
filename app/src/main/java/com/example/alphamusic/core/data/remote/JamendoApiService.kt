package com.example.alphamusic.core.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface JamendoApiService {
    @GET("v3.0/tracks/")
    suspend fun getTracks(
        @Query("client_id") clientId: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 50,
        @Query("tags") tags: String? = null,
        @Query("search") search: String? = null,
        @Query("boost") boost: String = "popularity_total"
    ): JamendoResponse
}

@Serializable
data class JamendoResponse(
    val results: List<JamendoTrackDto>
)

@Serializable
data class JamendoTrackDto(
    val id: String,
    val name: String,
    val artist_name: String,
    val album_name: String? = null,
    val image: String,
    val audio: String,
    val duration: Long
)
