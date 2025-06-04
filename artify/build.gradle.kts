plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
}

android {
    namespace = "com.example.artify"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.artify"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        dataBinding = true
    }

}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //BaseActity
    implementation ("androidx.core:core-ktx:1.12.0")


    implementation ("com.intuit.ssp:ssp-android:1.1.1")
    implementation ("com.intuit.sdp:sdp-android:1.1.1")

    // onboardingScreen slide + dot
    implementation ("androidx.viewpager2:viewpager2:1.0.0")
    implementation("com.tbuonomo:dotsindicator:5.1.0")

    // Firebase Auth Module
    implementation(project(":FirebaseAuth"))

    // Hilt
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-android-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-fragment:1.0.0")

    // ViewModel & LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.14.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // Facebook SDK
    implementation("com.facebook.android:facebook-login:16.2.0")

    implementation ("androidx.fragment:fragment-ktx:1.5.2")

    // BlurView
    implementation("com.github.Dimezis:BlurView:version-2.0.6")

    // Country Code Picker
    implementation ("com.hbb20:ccp:2.5.0")

    // OTP View
    implementation ("com.github.mukeshsolanki.android-otpview-pinview:otpview:3.1.0")

    //glide
    implementation ("com.github.bumptech.glide:glide:4.16.0")

    //shimmer
    implementation ("com.facebook.shimmer:shimmer:0.5.0")

    // Image Editor Library
    implementation(project(":ImageEditor"))

    //iconic
    implementation ("com.mikepenz:material-design-iconic-typeface:2.2.0.9-kotlin@aar")
    implementation ("com.mikepenz:iconics-core:5.3.4")
    implementation ("com.mikepenz:iconics-views:5.3.4")
    implementation ("androidx.appcompat:appcompat:1.6.1")

    // Color Picker (remove QuadFlask, add skydoves)
    // implementation("com.github.QuadFlask:colorpicker:0.0.15") // Remove this
    implementation("com.github.skydoves:colorpickerview:2.3.0") // Add this


    // drag delete crop rotate view
    implementation("com.github.miaoyongjun:StickerView:1.1")

    // verticalseekbar
    implementation ("com.h6ah4i.android.widget.verticalseekbar:verticalseekbar:1.0.0")

    // crop
    implementation ("com.github.yalantis:ucrop:2.2.10")

}