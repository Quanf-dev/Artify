package com.example.firebaseauth.repository

import android.content.Intent
import com.example.firebaseauth.FirebaseAuthResult
import com.example.firebaseauth.model.User
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.PhoneAuthCredential
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    // Email & Password
    suspend fun signInWithEmailAndPassword(email: String, password: String): FirebaseAuthResult<User>
    suspend fun createUserWithEmailAndPassword(email: String, password: String): FirebaseAuthResult<User>
    suspend fun sendPasswordResetEmail(email: String): FirebaseAuthResult<Unit>
    suspend fun sendEmailVerification(): FirebaseAuthResult<Unit>
    
    // Google Sign-In
    fun getGoogleSignInIntent(): Intent
    suspend fun signInWithGoogle(credential: AuthCredential): FirebaseAuthResult<User>
    
    // Phone Authentication
    suspend fun sendPhoneVerificationCode(
        phoneNumber: String,
        onCodeSent: (String) -> Unit,
        onVerificationFailed: (Exception) -> Unit
    )
    suspend fun verifyPhoneNumberWithCode(verificationId: String, code: String): FirebaseAuthResult<User>
    suspend fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential): FirebaseAuthResult<User>
    
    // Common
    fun getCurrentUser(): User?
    suspend fun signOut()
    fun isUserSignedIn(): Boolean
    fun getAuthStateFlow(): Flow<User?>
}