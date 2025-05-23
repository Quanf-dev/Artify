package com.example.firebaseauth

sealed class FirebaseAuthResult<out T> {
    data class Success<T>(val data: T) : FirebaseAuthResult<T>()
    data class Error(val exception: Exception) : FirebaseAuthResult<Nothing>()
    object Loading : FirebaseAuthResult<Nothing>()
    
    companion object {
        fun <T> success(data: T): FirebaseAuthResult<T> = Success(data)
        fun error(exception: Exception): FirebaseAuthResult<Nothing> = Error(exception)
        fun loading(): FirebaseAuthResult<Nothing> = Loading
    }
}