package com.example.socialposts.di

import com.example.socialposts.repository.PostRepository
import com.example.socialposts.repository.PostRepositoryImpl
import com.google.firebase.storage.FirebaseStorage
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PostModule {

    @Binds
    @Singleton
    abstract fun bindPostRepository(
        postRepositoryImpl: PostRepositoryImpl
    ): PostRepository

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseStorage(): FirebaseStorage {
            return FirebaseStorage.getInstance()
        }
    }
} 