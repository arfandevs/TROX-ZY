package com.troxzy.xploit.di

import com.troxzy.xploit.data.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAiChatRepository(aiApiService: com.troxzy.xploit.data.remote.AiApiService): AiChatRepository {
        return AiChatRepository(aiApiService)
    }
}
