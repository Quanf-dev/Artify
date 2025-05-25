package com.example.firebaseauth

import android.app.Activity
import android.content.Intent
import androidx.fragment.app.Fragment
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FacebookLoginManager @Inject constructor() {
    
    private val callbackManager = CallbackManager.Factory.create()
    private val loginManager = LoginManager.getInstance()
    
    fun getCallbackManager(): CallbackManager = callbackManager
    
    fun loginWithFacebook(
        activity: Activity,
        onSuccess: (AccessToken) -> Unit,
        onError: (Exception) -> Unit,
        onCancel: () -> Unit
    ) {
        loginManager.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                onSuccess(result.accessToken)
            }
            
            override fun onCancel() {
                onCancel()
            }
            
            override fun onError(error: FacebookException) {
                onError(error)
            }
        })
        
        loginManager.logInWithReadPermissions(activity, listOf("public_profile"))
    }
    
  
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }
    
    fun logout() {
        loginManager.logOut()
    }

    fun getCurrentAccessToken(): AccessToken? {
        return AccessToken.getCurrentAccessToken()
    }

    // Method riêng để request email permission (chỉ dùng khi app đã được approve)
    fun requestEmailPermission(
        activity: Activity,
        onSuccess: (AccessToken) -> Unit,
        onError: (Exception) -> Unit,
        onCancel: () -> Unit
    ) {
        loginManager.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                onSuccess(result.accessToken)
            }

            override fun onCancel() {
                onCancel()
            }

            override fun onError(error: FacebookException) {
                onError(error)
            }
        })

        // Chỉ sử dụng khi app đã được Facebook approve cho email permission
        loginManager.logInWithReadPermissions(activity, listOf("email"))
    }
} 