package com.example.firebaseauth

import android.content.Context
import android.content.Intent
import com.example.firebaseauth.di.FirebaseAuthEntryPoint
import com.example.firebaseauth.model.User
import com.example.firebaseauth.repository.AuthRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthManager @Inject constructor(
    private val authRepository: AuthRepository
) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    companion object {
        @Volatile
        private var instance: FirebaseAuthManager? = null

        fun getInstance(context: Context): FirebaseAuthManager {
            return instance ?: synchronized(this) {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    FirebaseAuthEntryPoint::class.java
                )
                val manager = entryPoint.firebaseAuthManager()
                instance = manager
                manager
            }
        }
    }

    // Email & Password
    suspend fun signInWithEmailAndPassword(
        email: String,
        password: String
    ): FirebaseAuthResult<User> {
        return authRepository.signInWithEmailAndPassword(email, password)
    }

    suspend fun createUserWithEmailAndPassword(
        email: String,
        password: String
    ): FirebaseAuthResult<User> {
        return authRepository.createUserWithEmailAndPassword(email, password)
    }

    suspend fun sendPasswordResetEmail(email: String): FirebaseAuthResult<Unit> {
        return authRepository.sendPasswordResetEmail(email)
    }

    suspend fun sendEmailVerification(): FirebaseAuthResult<Unit> {
        return authRepository.sendEmailVerification()
    }

    // Google Sign-In
    fun getGoogleSignInIntent(): Intent {
        return authRepository.getGoogleSignInIntent()
    }

    suspend fun handleGoogleSignInResult(data: Intent?): FirebaseAuthResult<User> {
        return try {
            if (data == null) {
                return FirebaseAuthResult.Error(Exception("Không nhận được dữ liệu đăng nhập"))
            }
            
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            
            if (idToken != null) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                val firebaseUser = authResult.user
                
                if (firebaseUser != null) {
                    val user = User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email,
                        displayName = firebaseUser.displayName,
                        phoneNumber = firebaseUser.phoneNumber,
                        isEmailVerified = firebaseUser.isEmailVerified,
                        photoUrl = firebaseUser.photoUrl?.toString()
                    )
                    FirebaseAuthResult.Success(user)
                } else {
                    FirebaseAuthResult.Error(Exception("Không thể lấy thông tin người dùng"))
                }
            } else {
                FirebaseAuthResult.Error(Exception("Không thể lấy token xác thực"))
            }
        } catch (e: ApiException) {
            FirebaseAuthResult.Error(Exception("Lỗi đăng nhập Google: ${e.statusCode}"))
        } catch (e: Exception) {
            FirebaseAuthResult.Error(e)
        }
    }

    // Phone Authentication
    suspend fun sendPhoneVerificationCode(
        phoneNumber: String,
        onCodeSent: (String) -> Unit,
        onVerificationFailed: (Exception) -> Unit
    ) {
        authRepository.sendPhoneVerificationCode(
            phoneNumber = phoneNumber,
            onCodeSent = onCodeSent,
            onVerificationFailed = onVerificationFailed
        )
    }

    suspend fun verifyPhoneNumberWithCode(
        verificationId: String,
        code: String
    ): FirebaseAuthResult<User> {
        return authRepository.verifyPhoneNumberWithCode(verificationId, code)
    }

    // Common
    fun getCurrentUser(): User? {
        return authRepository.getCurrentUser()
    }

    suspend fun signOut() {
        authRepository.signOut()
    }

    fun isUserSignedIn(): Boolean {
        return authRepository.isUserSignedIn()
    }

    fun getAuthStateFlow(): Flow<User?> {
        return authRepository.getAuthStateFlow()
    }
}