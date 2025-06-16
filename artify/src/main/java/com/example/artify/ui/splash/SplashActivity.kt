package com.example.artify.ui.splash

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.artify.R
import com.example.artify.ui.home.HomeActivity
import com.example.artify.ui.login.LoginActivity
import com.example.artify.ui.onboard.OnboardingActivity
import com.example.artify.ui.profile.SetupUsernameActivity
import com.example.artify.ui.verification.EmailVerificationActivity
import com.example.firebaseauth.FirebaseAuthManager
import com.example.firebaseauth.FirebaseAuthResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    @Inject
    lateinit var authManager: FirebaseAuthManager
    
    private val viewModel: SplashViewModel by viewModels()
    
    private lateinit var sharedPreferences: SharedPreferences
    private val PREFS_NAME = "ArtifyPrefs"
    private val KEY_USER_LOGGED_IN = "user_logged_in"
    private val KEY_USER_ID = "user_id"
    private val KEY_USERNAME = "username"
    private val KEY_EMAIL_VERIFIED = "email_verified"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)
        
        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Log SharedPreferences values at startup
        logSharedPreferencesValues()

        // Delay for 2 seconds to show splash screen
        Handler(Looper.getMainLooper()).postDelayed({
            if (isInternetAvailable()) {
                checkAuthState()
            } else {
                showNoInternetDialog()
            }
        }, 2000)
    }
    
    private fun logSharedPreferencesValues() {
        val isLoggedIn = sharedPreferences.getBoolean(KEY_USER_LOGGED_IN, false)
        val userId = sharedPreferences.getString(KEY_USER_ID, null)
        val username = sharedPreferences.getString(KEY_USERNAME, null)

        // Check if Firebase authentication is active
        val firebaseUser = authManager.getCurrentUser()
        
        // Also check the default SharedPreferences
        val defaultPrefs = getSharedPreferences("artify_preferences", Context.MODE_PRIVATE)
        val defaultIsLoggedIn = defaultPrefs.getBoolean(KEY_USER_LOGGED_IN, false)
        val defaultUserId = defaultPrefs.getString(KEY_USER_ID, null)
        
        // Check app_prefs
        val appPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val appPrefsIsLoggedIn = appPrefs.getBoolean(KEY_USER_LOGGED_IN, false)
        val appPrefsUserId = appPrefs.getString(KEY_USER_ID, null)
        
        Log.d("SplashActivity", "SharedPreferences values at startup:")
        Log.d("SplashActivity", "- artify_preferences: isLoggedIn=$defaultIsLoggedIn, userId=$defaultUserId")
        Log.d("SplashActivity", "- app_prefs: isLoggedIn=$appPrefsIsLoggedIn, userId=$appPrefsUserId")
        Log.d("SplashActivity", "- Firebase isUserSignedIn: ${authManager.isUserSignedIn()}")
        Log.d("SplashActivity", "- Firebase currentUser: ${firebaseUser?.uid}, email: ${firebaseUser?.email}")
    }
    
    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                   capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            return networkInfo != null && networkInfo.isConnected
        }
    }
    
    private fun showNoInternetDialog() {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.please_check_your_internet_connection_and_try_again))
            .setPositiveButton(getString(R.string.retry)) { _, _ ->
                if (isInternetAvailable()) {
                    checkAuthState()
                } else {
                    Toast.makeText(this, getString(R.string.still_no_internet_connection), Toast.LENGTH_SHORT).show()
                    showNoInternetDialog()
                }
            }
            .setNegativeButton(getString(R.string.exit)) { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun checkAuthState() {
        // First check if we have saved login state
        val isLoggedIn = sharedPreferences.getBoolean(KEY_USER_LOGGED_IN, false)
        val userId = sharedPreferences.getString(KEY_USER_ID, null)
        val isLoggedInViewModel = viewModel.isUserLoggedIn()
        
        Log.d("SplashActivity", "Checking auth state:")
        Log.d("SplashActivity", "- SharedPreferences isLoggedIn: $isLoggedIn")
        Log.d("SplashActivity", "- SharedPreferences userId: $userId")
        Log.d("SplashActivity", "- ViewModel isLoggedIn: $isLoggedInViewModel")
        Log.d("SplashActivity", "- Firebase isUserSignedIn: ${authManager.isUserSignedIn()}")
        
        // If we have saved login state in either SharedPreferences or ViewModel
        if ((isLoggedIn && userId != null) || isLoggedInViewModel) {
            Log.d("SplashActivity", "User is logged in according to SharedPreferences or ViewModel, proceeding with logged in user")
            proceedWithLoggedInUser()
        } else if (authManager.isUserSignedIn()) {
            // Firebase says user is signed in but SharedPreferences doesn't match
            Log.d("SplashActivity", "User is signed in Firebase but not in SharedPreferences")
            proceedWithLoggedInUser()
        } else {
            // User is not signed in according to both sources
            Log.d("SplashActivity", "User is not logged in")
            viewModel.setUserLoggedIn(false)
            checkOnboardingAndNavigate()
        }
    }
    
    private fun proceedWithLoggedInUser() {
        lifecycleScope.launch {
            showLoading()
            try {
                // Try to reload user data but don't fail if it doesn't work
                try {
                    authManager.reloadCurrentUser()
                } catch (e: Exception) {
                    Log.w("SplashActivity", "Failed to reload user: ${e.message}")
                    // Continue anyway
                }
                
                // Get latest user data
                val currentUser = authManager.getCurrentUserWithUsername()
                
                if (currentUser != null) {
                    Log.d("SplashActivity", "Current user: ${currentUser.uid}, username: ${currentUser.username}, email verified: ${currentUser.isEmailVerified}")
                    
                    // Update saved preferences with latest data
                    saveUserToPreferences(currentUser)
                    

                    // Email is verified, check if username is set
                    if (currentUser.username.isNullOrEmpty()) {
                        Log.d("SplashActivity", "Username is empty, directing to profile setup")
                        // Username not set, direct to profile setup
                        startActivity(Intent(this@SplashActivity, SetupUsernameActivity::class.java))
                        finish()
                        return@launch
                    }
                    
                    // User is fully authenticated and profile is complete
                    Log.d("SplashActivity", "User is fully authenticated, going to HomeActivity")
                    viewModel.setUserLoggedIn(true)
                    startActivity(Intent(this@SplashActivity, HomeActivity::class.java))
                    finish()
                } else {
                    // User no longer exists or session expired
                    Log.d("SplashActivity", "User is null after fetching")
                    clearUserPreferences()
                    viewModel.setUserLoggedIn(false)
                    checkOnboardingAndNavigate()
                }
            } catch (e: Exception) {
                Log.e("SplashActivity", "Exception during auth check: ${e.message}")
                // Error occurred, but let's check if we have user data in SharedPreferences before clearing
                val userId = sharedPreferences.getString(KEY_USER_ID, null)
                val username = sharedPreferences.getString(KEY_USERNAME, null)
                
                if (userId != null && username != null && authManager.isUserSignedIn()) {
                    // We have enough data to proceed to HomeActivity
                    Log.d("SplashActivity", "Using SharedPreferences data to proceed despite error")
                    viewModel.setUserLoggedIn(true)
                    startActivity(Intent(this@SplashActivity, HomeActivity::class.java))
                    finish()
                } else {
                    // Clear preferences and go to login
                    clearUserPreferences()
                    viewModel.setUserLoggedIn(false)
                    checkOnboardingAndNavigate()
                }
            } finally {
                hideLoading()
            }
        }
    }
    
    private fun showLoading() {
        // Implement loading indicator if needed
    }
    
    private fun hideLoading() {
        // Hide loading indicator if needed
    }
    
    private fun saveUserToPreferences(user: com.example.firebaseauth.model.User) {
        sharedPreferences.edit().apply {
            putBoolean(KEY_USER_LOGGED_IN, true)
            putString(KEY_USER_ID, user.uid)
            putString(KEY_USERNAME, user.username)
            putBoolean(KEY_EMAIL_VERIFIED, user.isEmailVerified)
            apply()
        }
        // Also update the SplashViewModel
        viewModel.setUserLoggedIn(true)
        
        Log.d("SplashActivity", "Saved user to preferences: ${user.uid}, ${user.username}, ${user.isEmailVerified}")
        logSharedPreferencesValues() // Log values after saving
    }
    
    private fun clearUserPreferences() {
        sharedPreferences.edit().apply {
            putBoolean(KEY_USER_LOGGED_IN, false)
            remove(KEY_USER_ID)
            remove(KEY_USERNAME)
            remove(KEY_EMAIL_VERIFIED)
            apply()
        }
        // Also update the SplashViewModel
        viewModel.setUserLoggedIn(false)
        
        Log.d("SplashActivity", "Cleared user preferences")
        logSharedPreferencesValues() // Log values after clearing
    }
    
    private fun checkOnboardingAndNavigate() {
        // Check if user has seen onboarding
        if (viewModel.hasSeenOnboarding()) {
            // User has seen onboarding, go to login
            Log.d("SplashActivity", "User has seen onboarding, going to login")
            startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
        } else {
            // First time user, show onboarding
            Log.d("SplashActivity", "First time user, showing onboarding")
            startActivity(Intent(this@SplashActivity, OnboardingActivity::class.java))
        }
        finish() // Close splash activity
    }
}
