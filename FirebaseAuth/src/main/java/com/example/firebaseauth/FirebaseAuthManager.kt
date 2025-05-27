package com.example.firebaseauth

import android.app.Activity
import android.content.Intent
import com.example.firebaseauth.model.User
import com.example.firebaseauth.repository.AuthRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthManager @Inject constructor(
    private val authRepository: AuthRepository,
    private val facebookLoginManager: FacebookLoginManager
) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

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

    suspend fun sendEmailVerification(): FirebaseAuthResult<Unit> {
        return authRepository.sendEmailVerification()
    }

    // Password Reset
    suspend fun sendPasswordResetEmail(email: String): FirebaseAuthResult<Unit> {
        return authRepository.sendPasswordResetEmail(email)
    }

    suspend fun checkUsernameExists(username: String): FirebaseAuthResult<Boolean> {
        return authRepository.checkUsernameExists(username)
    }

    suspend fun saveUsername(uid: String, username: String): FirebaseAuthResult<Unit> {
        return authRepository.saveUsername(uid, username)
    }

    suspend fun getUser(uid: String): FirebaseAuthResult<User?> {
        return authRepository.getUser(uid)
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
                authRepository.signInWithGoogle(credential)
            } else {
                FirebaseAuthResult.Error(Exception("Không thể lấy token xác thực"))
            }
        } catch (e: ApiException) {
            FirebaseAuthResult.Error(Exception("Lỗi đăng nhập Google: ${e.statusCode}"))
        } catch (e: Exception) {
            FirebaseAuthResult.Error(e)
        }
    }

    // Facebook Sign-In
    suspend fun signInWithFacebook(accessToken: String): FirebaseAuthResult<User> {
        return try {
            val credential = com.google.firebase.auth.FacebookAuthProvider.getCredential(accessToken)
            authRepository.signInWithFacebook(credential)
        } catch (e: Exception) {
            FirebaseAuthResult.Error(e)
        }
    }

    fun loginWithFacebook(
        activity: Activity,
        onSuccess: (User) -> Unit,
        onError: (Exception) -> Unit,
        onCancel: () -> Unit
    ) {
        facebookLoginManager.loginWithFacebook(
            activity = activity,
            onSuccess = { accessToken ->
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                    when (val result = signInWithFacebook(accessToken.token)) {
                        is FirebaseAuthResult.Success -> onSuccess(result.data)
                        is FirebaseAuthResult.Error -> onError(result.exception)
                        is FirebaseAuthResult.Loading -> { /* Handle loading if needed */ }
                    }
                }
            },
            onError = onError,
            onCancel = onCancel
        )
    }

    fun handleFacebookActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        facebookLoginManager.handleActivityResult(requestCode, resultCode, data)
    }

    // Phone Authentication
    suspend fun sendPhoneVerificationCode(
        phoneNumber: String,
        activity: Activity,
        onCodeSent: (String) -> Unit,
        onVerificationFailed: (Exception) -> Unit
    ) {
        authRepository.sendPhoneVerificationCode(
            phoneNumber = phoneNumber,
            activity = activity,
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

    suspend fun reloadCurrentUser(): FirebaseAuthResult<Unit> {
        return authRepository.reloadCurrentUser()
    }

    suspend fun signOut() {
        authRepository.signOut()
    }

    fun isUserSignedIn(): Boolean {
        return authRepository.isUserSignedIn()
    }

}