package com.example.alphamusic.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        TrackEntity::class, 
        SearchQueryEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AlphaDatabase : RoomDatabase() {
    abstract val trackDao: TrackDao
    abstract val searchQueryDao: SearchQueryDao
    abstract val playlistDao: PlaylistDao
}
