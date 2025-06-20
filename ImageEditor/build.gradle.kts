plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.imageeditor"
    compileSdk = 35

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Color Picker (remove QuadFlask, add skydoves)
    // implementation("com.github.QuadFlask:colorpicker:0.0.15") // Remove this
    implementation("com.github.skydoves:colorpickerview:2.3.0") // Add this

    // drag delete crop rotate view
    implementation("com.github.miaoyongjun:StickerView:1.1")
    
    // GP-Image for image filtering
    implementation("com.github.wasabeef:glide-transformations:4.3.0")
    implementation("jp.wasabeef:picasso-transformations:2.4.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("jp.co.cyberagent.android:gpuimage:2.1.0") // Main image filtering library
    implementation("com.squareup.picasso:picasso:2.8")
    
    // Frame template support
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.github.bumptech.glide:recyclerview-integration:4.16.0")

    implementation("com.github.miaoyongjun:StickerView:1.1")

}