package com.example.alphamusic.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_searches")
data class SearchQueryEntity(
    @PrimaryKey val query: String,
    val timestamp: Long
)
