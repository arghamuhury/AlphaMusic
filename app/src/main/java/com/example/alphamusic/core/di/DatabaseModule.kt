package com.example.alphamusic.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.alphamusic.core.data.local.AlphaDatabase
import com.example.alphamusic.core.data.local.TrackDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AlphaDatabase {
        return Room.databaseBuilder(
            context,
            AlphaDatabase::class.java,
            "alpha_music.db"
        ).addMigrations(MIGRATION_3_4)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideTrackDao(database: AlphaDatabase): TrackDao {
        return database.trackDao
    }

    @Provides
    fun provideSearchQueryDao(database: AlphaDatabase): com.example.alphamusic.core.data.local.SearchQueryDao {
        return database.searchQueryDao
    }

    @Provides
    fun providePlaylistDao(database: AlphaDatabase): com.example.alphamusic.core.data.local.PlaylistDao {
        return database.playlistDao
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE tracks ADD COLUMN localUri TEXT")
        }
    }
}
