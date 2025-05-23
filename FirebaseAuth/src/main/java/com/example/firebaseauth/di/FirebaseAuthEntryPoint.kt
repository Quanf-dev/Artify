package com.example.firebaseauth.di

import com.example.firebaseauth.FirebaseAuthManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface FirebaseAuthEntryPoint {
    fun firebaseAuthManager(): FirebaseAuthManager
}