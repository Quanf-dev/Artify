package com.example.artify

import android.app.Application
import com.facebook.appevents.AppEventsLogger
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ArtifyApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        

        AppEventsLogger.activateApp(this)
    }
    

}