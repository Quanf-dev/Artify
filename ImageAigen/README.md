# Firebase Gemini AI Image Generation & Editing

This module implements Google's Gemini AI for image generation and editing in the Artify Android app.

## Features

1. **Image Generation**: Generate images from text prompts using Gemini AI
2. **Image Editing**: Edit existing images with text instructions
3. **Save Images**: Save generated or edited images to the device gallery

## Setup Instructions

### 1. Google AI API Key

1. Obtain a Gemini API key from [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Add your API key to the AndroidManifest.xml:
   ```xml
   <meta-data
       android:name="com.google.ai.gemini.api.key"
       android:value="YOUR_API_KEY_HERE" />
   ```

### 2. Firebase Project Setup (Optional for Firebase Integration)

1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. Add your Android app to the Firebase project
   - Use package name: `com.example.artify`
   - Download the `google-services.json` file and place it in the app directory

## Usage

1. Launch the app
2. Tap "Open Gemini AI" button in the main screen
3. Use the "Generate" tab to create images from text prompts
4. Use the "Edit" tab to modify existing images with AI

## Permissions

The app requires the following permissions:

- `INTERNET`: To communicate with Gemini AI services
- `READ_EXTERNAL_STORAGE`: To select images for editing
- `READ_MEDIA_IMAGES`: For Android 13+ to select images
- `WRITE_EXTERNAL_STORAGE`: To save images to the gallery (for Android < 10)

## Technical Implementation

- Uses Google AI Gemini client SDK for image generation and editing
- Implements MVVM architecture with Repository pattern
- Uses ViewPager2 with TabLayout for UI
- Supports image saving to gallery compatible with Android 10+ scoped storage 