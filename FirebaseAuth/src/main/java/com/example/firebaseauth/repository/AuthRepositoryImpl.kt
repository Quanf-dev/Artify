package com.example.firebaseauth.repository

import android.app.Activity
import android.content.Intent
import com.example.firebaseauth.FirebaseAuthResult
import com.example.firebaseauth.model.User
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.FirebaseException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val googleSignInClient: GoogleSignInClient,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val USERNAMES_COLLECTION = "usernames"
        private const val USERNAME_FIELD = "username"
    }

    override suspend fun signInWithEmailAndPassword(
        email: String,
        password: String
    ): FirebaseAuthResult<User> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            result.user?.let {
                fetchUserWithUsername(it.uid)
            } ?: FirebaseAuthResult.Error(Exception("Đăng nhập thất bại"))
        } catch (e: Exception) {
            FirebaseAuthResult.Error(e)
        }
    }

    override suspend fun createUserWithEmailAndPassword(
        email: String,
        password: String
    ): FirebaseAuthResult<User> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let {
                FirebaseAuthResult.Success(it.toUser())
            } ?: FirebaseAuthResult.Error(Exception("Tạo tài khoản thất bại"))
        } catch (e: Exception) {
            FirebaseAuthResult.Error(e)
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): FirebaseAuthResult<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            FirebaseAuthResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseAuthResult.Error(e)
        }
    }

    override suspend fun sendEmailVerification(): FirebaseAuthResult<Unit> {
        return try {
            firebaseAuth.currentUser?.sendEmailVerification()?.await()
                ?: return FirebaseAuthResult.Error(Exception("Không có người dùng đăng nhập"))
            FirebaseAuthResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseAuthResult.Error(e)
        }
    }

    override fun getGoogleSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    override suspend fun signInWithGoogle(credential: AuthCredential): FirebaseAuthResult<User> {
        return try {
            val result = firebaseAuth.signInWithCredential(credential).await()
            result.user?.let {
                fetchUserWithUsername(it.uid)
            } ?: FirebaseAuthResult.Error(Exception("Đăng nhập Google thất bại"))
        } catch (e: Exception) {
            FirebaseAuthResult.Error(e)
        }
    }

    override suspend fun signInWithFacebook(credential: AuthCredential): FirebaseAuthResult<User> {
        return try {
            val result = firebaseAuth.signInWithCredential(credential).await()
            result.user?.let {
                fetchUserWithUsername(it.uid)
            } ?: FirebaseAuthResult.Error(java.lang.Exception("Đăng nhập Facebook thất bại"))
        } catch (e: Exception) {
            FirebaseAuthResult.Error(e)
        }
    }

    override suspend fun sendPhoneVerificationCode(
        phoneNumber: String,
        activity: Activity,
        onCodeSent: (String) -> Unit,
        onVerificationFailed: (Exception) -> Unit
    ) {
        try {
            val options = PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        // Tự động xác thực nếu có thể
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        onVerificationFailed(e)
                    }

                    override fun onCodeSent(
                        verificationId: String,
                        token: PhoneAuthProvider.ForceResendingToken
                    ) {
                        onCodeSent(verificationId)
                    }
                })
                .build()
            PhoneAuthProvider.verifyPhoneNumber(options)
        } catch (e: Exception) {
            onVerificationFailed(e)
        }
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
                fetchUserWithUsername(it.uid)
            } ?: FirebaseAuthResult.Error(Exception("Xác thực số điện thoại thất bại"))
        } catch (e: Exception) {
            FirebaseAuthResult.Error(e)
        }
    }

    override fun getCurrentUser(): User? {
        val firebaseUser = firebaseAuth.currentUser ?: return null
        // Note: This method is not suspend, so we can't fetch username from Firestore here
        // We need to use the synchronous approach and return basic user info
        return firebaseUser.toUser()
    }

    // Helper method to get user with username from Firestore (should be called from a coroutine)
    suspend fun getCurrentUserWithUsername(): User? {
        val firebaseUser = firebaseAuth.currentUser ?: return null
        return try {
            val userSnapshot = firestore.collection(USERS_COLLECTION).document(firebaseUser.uid).get().await()
            if (userSnapshot.exists()) {
                val username = userSnapshot.getString(USERNAME_FIELD)
                // Get photoUrl from Firestore if available, otherwise use Firebase Auth photoUrl
                val photoUrl = userSnapshot.getString("photoUrl") ?: firebaseUser.photoUrl?.toString()
                
                android.util.Log.d("AuthRepository", "Firestore photoUrl: ${userSnapshot.getString("photoUrl")}")
                android.util.Log.d("AuthRepository", "Firebase Auth photoUrl: ${firebaseUser.photoUrl?.toString()}")
                android.util.Log.d("AuthRepository", "Final photoUrl: $photoUrl")
                
                User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email,
                    displayName = firebaseUser.displayName,
                    phoneNumber = firebaseUser.phoneNumber,
                    photoUrl = photoUrl,
                    isEmailVerified = firebaseUser.isEmailVerified,
                    username = username
                )
            } else {
                firebaseUser.toUser()
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error fetching user data: ${e.message}")
            firebaseUser.toUser()
        }
    }

    override suspend fun reloadCurrentUser(): FirebaseAuthResult<Unit> {
        return try {
            firebaseAuth.currentUser?.reload()?.await()
                ?: return FirebaseAuthResult.Error(Exception("Không có người dùng đăng nhập"))
            FirebaseAuthResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseAuthResult.Error(e)
        }
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

    override suspend fun checkUsernameExists(username: String): FirebaseAuthResult<Boolean> {
        return try {
            val document = firestore.collection(USERNAMES_COLLECTION).document(username).get().await()
            FirebaseAuthResult.Success(document.exists())
        } catch (e: Exception) {
            FirebaseAuthResult.Error(e)
        }
    }

    override suspend fun saveUsername(uid: String, username: String): FirebaseAuthResult<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val userDocRef = firestore.collection(USERS_COLLECTION).document(uid)
                val usernameDocRef = firestore.collection(USERNAMES_COLLECTION).document(username)

                // Check if username is already taken in the transaction
                val usernameSnapshot = transaction.get(usernameDocRef)
                if (usernameSnapshot.exists()) {
                    throw FirebaseException("Username already taken.")
                }

                // Save username in users collection
                transaction.update(userDocRef, USERNAME_FIELD, username)
                // Save username in usernames collection for quick lookup
                transaction.set(usernameDocRef, mapOf("uid" to uid))
                null
            }.await()
            FirebaseAuthResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseAuthResult.Error(e)
        }
    }

    override suspend fun saveUsernamePhotoUrl(
        uid: String,
        username: String,
        photoUrl: String
    ): FirebaseAuthResult<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val userDocRef = firestore.collection(USERS_COLLECTION).document(uid)
                val usernameDocRef = firestore.collection(USERNAMES_COLLECTION).document(username)

                // Check if username is already taken in the transaction
                val usernameSnapshot = transaction.get(usernameDocRef)
                if (usernameSnapshot.exists() && usernameSnapshot.getString("uid") != uid) {
                    throw FirebaseException("Username '$username' already taken by another user.")
                }

                // Save username and photoUrl in users collection
                transaction.update(userDocRef, mapOf(USERNAME_FIELD to username, "photoUrl" to photoUrl))
                // Save username in usernames collection for quick lookup
                transaction.set(usernameDocRef, mapOf("uid" to uid))
                null
            }.await()
            FirebaseAuthResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseAuthResult.Error(e)
        }
    }

    override suspend fun getUser(uid: String): FirebaseAuthResult<User?> {
        return try {
            val documentSnapshot = firestore.collection(USERS_COLLECTION).document(uid).get().await()
            if (documentSnapshot.exists()) {
                val firebaseUser = firebaseAuth.currentUser
                val user = documentSnapshot.toObject(User::class.java)?.copy(
                    uid = firebaseUser?.uid ?: uid,
                    email = firebaseUser?.email,
                    displayName = firebaseUser?.displayName,
                    phoneNumber = firebaseUser?.phoneNumber,
                    photoUrl = firebaseUser?.photoUrl?.toString(),
                    isEmailVerified = firebaseUser?.isEmailVerified ?: false
                )
                FirebaseAuthResult.Success(user)
            } else {
                // If no user document in Firestore, create one from Auth data if user is signed in
                firebaseAuth.currentUser?.let {
                    val newUser = it.toUser()
                    firestore.collection(USERS_COLLECTION).document(it.uid).set(newUser).await()
                    FirebaseAuthResult.Success(newUser)
                } ?: FirebaseAuthResult.Success(null)
            }
        } catch (e: Exception) {
            FirebaseAuthResult.Error(e)
        }
    }

    private suspend fun fetchUserWithUsername(uid: String): FirebaseAuthResult<User> {
        return try {
            val userSnapshot = firestore.collection(USERS_COLLECTION).document(uid).get().await()
            val firebaseUser = firebaseAuth.currentUser

            if (firebaseUser == null) {
                return FirebaseAuthResult.Error(Exception("User not authenticated."))
            }

            if (userSnapshot.exists()) {
                val username = userSnapshot.getString(USERNAME_FIELD)
                // Get photoUrl from Firestore if available, otherwise use Firebase Auth photoUrl
                val photoUrl = userSnapshot.getString("photoUrl") ?: firebaseUser.photoUrl?.toString()
                
                android.util.Log.d("AuthRepository", "fetchUserWithUsername - Firestore photoUrl: ${userSnapshot.getString("photoUrl")}")
                android.util.Log.d("AuthRepository", "fetchUserWithUsername - Firebase Auth photoUrl: ${firebaseUser.photoUrl?.toString()}")
                android.util.Log.d("AuthRepository", "fetchUserWithUsername - Final photoUrl: $photoUrl")
                
                val user = User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email,
                    displayName = firebaseUser.displayName,
                    phoneNumber = firebaseUser.phoneNumber,
                    photoUrl = photoUrl,
                    isEmailVerified = firebaseUser.isEmailVerified,
                    username = username
                )
                FirebaseAuthResult.Success(user)
            } else {
                // User document doesn't exist in Firestore, create it without username yet
                val newUser = firebaseUser.toUser() // username will be null
                firestore.collection(USERS_COLLECTION).document(uid).set(newUser).await() // Save the basic user object
                FirebaseAuthResult.Success(newUser) // Return user without username, will prompt setup
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error in fetchUserWithUsername: ${e.message}")
            FirebaseAuthResult.Error(e)
        }
    }

    private fun FirebaseUser.toUser(username: String? = null): User {
        return User(
            uid = uid,
            email = email,
            displayName = displayName,
            phoneNumber = phoneNumber,
            photoUrl = photoUrl?.toString(),
            isEmailVerified = isEmailVerified,
            username = username
        )
    }
}