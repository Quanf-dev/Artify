package com.example.firebaseauth.repository

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.example.firebaseauth.FirebaseAuthResult
import com.example.firebaseauth.model.User
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val context: Context,
    private val googleSignInClient: GoogleSignInClient
) : AuthRepository {

    override suspend fun signInWithEmailAndPassword(
        email: String,
        password: String
    ): FirebaseAuthResult<User> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            result.user?.let {
                FirebaseAuthResult.success(it.toUser())
            } ?: FirebaseAuthResult.error(Exception("Đăng nhập thất bại"))
        } catch (e: Exception) {
            FirebaseAuthResult.error(e)
        }
    }

    override suspend fun createUserWithEmailAndPassword(
        email: String,
        password: String
    ): FirebaseAuthResult<User> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let {
                FirebaseAuthResult.success(it.toUser())
            } ?: FirebaseAuthResult.error(Exception("Tạo tài khoản thất bại"))
        } catch (e: Exception) {
            FirebaseAuthResult.error(e)
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): FirebaseAuthResult<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            FirebaseAuthResult.success(Unit)
        } catch (e: Exception) {
            FirebaseAuthResult.error(e)
        }
    }

    override suspend fun sendEmailVerification(): FirebaseAuthResult<Unit> {
        return try {
            firebaseAuth.currentUser?.sendEmailVerification()?.await()
                ?: return FirebaseAuthResult.error(Exception("Không có người dùng đăng nhập"))
            FirebaseAuthResult.success(Unit)
        } catch (e: Exception) {
            FirebaseAuthResult.error(e)
        }
    }

    override fun getGoogleSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    override suspend fun signInWithGoogle(credential: AuthCredential): FirebaseAuthResult<User> {
        return try {
            val result = firebaseAuth.signInWithCredential(credential).await()
            result.user?.let {
                FirebaseAuthResult.success(it.toUser())
            } ?: FirebaseAuthResult.error(Exception("Đăng nhập Google thất bại"))
        } catch (e: Exception) {
            FirebaseAuthResult.error(e)
        }
    }

    override suspend fun sendPhoneVerificationCode(
        phoneNumber: String,
        onCodeSent: (String) -> Unit,
        onVerificationFailed: (Exception) -> Unit
    ) {
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(context as Activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // Tự động xác thực nếu có thể
                }

                override fun onVerificationFailed(p0: FirebaseException) {
                    TODO("Not yet implemented")
                }

//                override fun onVerificationFailed(e: Exception) {
//                    onVerificationFailed(e)
//
//                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    onCodeSent(verificationId)
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    override suspend fun verifyPhoneNumberWithCode(
        verificationId: String,
        code: String
    ): FirebaseAuthResult<User> {
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        return signInWithPhoneAuthCredential(credential)
    }

    override suspend fun signInWithPhoneAuthCredential(
        credential: PhoneAuthCredential
    ): FirebaseAuthResult<User> {
        return try {
            val result = firebaseAuth.signInWithCredential(credential).await()
            result.user?.let {
                FirebaseAuthResult.success(it.toUser())
            } ?: FirebaseAuthResult.error(Exception("Xác thực số điện thoại thất bại"))
        } catch (e: Exception) {
            FirebaseAuthResult.error(e)
        }
    }

    override fun getCurrentUser(): User? {
        return firebaseAuth.currentUser?.toUser()
    }

    override suspend fun signOut() {
        googleSignInClient.signOut().await()
        firebaseAuth.signOut()
    }

    override fun isUserSignedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    override fun getAuthStateFlow(): Flow<User?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.toUser())
        }
        firebaseAuth.addAuthStateListener(authStateListener)
        awaitClose {
            firebaseAuth.removeAuthStateListener(authStateListener)
        }
    }

    private fun FirebaseUser.toUser(): User {
        return User(
            uid = uid,
            email = email,
            displayName = displayName,
            phoneNumber = phoneNumber,
            photoUrl = photoUrl?.toString(),
            isEmailVerified = isEmailVerified
        )
    }
}