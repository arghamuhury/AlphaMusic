package com.example.alphamusic.core.di

import com.example.alphamusic.core.data.JioSaavnMusicSource
import com.example.alphamusic.core.data.MusicSource
import com.example.alphamusic.core.data.repository.MusicRepositoryImpl
import com.example.alphamusic.core.domain.MusicRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMusicSource(
        jioSaavnMusicSource: JioSaavnMusicSource
    ): MusicSource

    @Binds
    @Singleton
    abstract fun bindMusicRepository(
        musicRepositoryImpl: MusicRepositoryImpl
    ): MusicRepository
}
