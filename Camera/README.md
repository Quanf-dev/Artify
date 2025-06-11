# Camera Module - Modern Android Camera App

## 🎯 Overview

This is a comprehensive camera module built with **Kotlin**, **CameraX**, **Firebase ML Kit**, and **Clean Architecture**. It provides a modern, feature-rich camera experience with real-time AR face masks, filters, and advanced camera controls.

## ✨ Features

### 📷 Core Camera Functionality
- **Live Camera Preview** - High-quality camera preview with CameraX
- **Dual Camera Support** - Seamless switching between front and back cameras
- **Smart Zoom Control** - Pinch-to-zoom and slider controls (1x-10x)
- **Brightness Adjustment** - Manual exposure control with slider
- **Multiple Flash Modes** - Auto, On, Off with visual indicators
- **Aspect Ratio Support** - 1:1, 4:3, 16:9, and Full screen ratios
- **Timer Modes** - 3s, 5s, 10s countdown with visual feedback

### 🎨 Real-time Filters
- **Sepia** - Vintage brown tone effect
- **Black & White** - Classic monochrome filter
- **Cinematic** - Movie-style color grading
- **Vintage** - Retro film look
- **Cold** - Blue-tinted cool effect
- **Warm** - Orange-tinted warm effect

### 🎭 AR Face Masks (Firebase ML Kit)
- **Real-time Face Detection** - Accurate face tracking with landmarks
- **Dynamic Face Masks**:
  - 🐶 **Dog Ears** - Cute puppy ears with positioning
  - 🕶️ **Sunglasses** - Cool aviator shades
  - 🐱 **Cat Face** - Whiskers and nose overlay
  - 👨 **Mustache** - Classic facial hair styles
- **Automatic Positioning** - Masks adapt to face rotation and movement
- **Multi-face Support** - Detects and applies masks to multiple faces

### 📸 Photo Management
- **High-Quality Capture** - Full resolution photos with metadata
- **Instant Preview** - Full-screen photo preview after capture
- **Quick Actions** - Retake, Save, Edit options
- **Gallery Access** - Browse previously captured photos
- **Custom Storage** - Organized in "CameraXApp" folder

## 🏗️ Architecture

### Clean Architecture Layers

```
📁 Camera Module
├── 🎯 Domain Layer
│   ├── 📄 Models (CameraSettings, FaceMask, DetectedFace)
│   ├── 📄 Repositories (Interfaces)
│   └── 📄 Use Cases (CapturePhoto, GetFaceMasks)
├── 💾 Data Layer
│   ├── 📄 Repository Implementations
│   ├── 📄 CameraX Integration
│   └── 📄 Firebase ML Kit Integration
├── 🎨 UI Layer
│   ├── 📄 MVVM Pattern
│   ├── 📄 ViewModels with StateFlow
│   ├── 📄 Fragments with ViewBinding
│   └── 📄 Custom Views (FaceMaskOverlay)
└── 🔧 DI Layer
    └── 📄 Hilt Dependency Injection
```

### Key Components

#### 🔄 State Management
- **CameraUiState** - Centralized UI state with StateFlow
- **CameraUiEvent** - Sealed class for user interactions
- **MVVM Pattern** - Clear separation of concerns

#### 🎭 Face Detection System
```kotlin
// Real-time face detection with ML Kit
FaceDetectionRepository -> DetectedFace -> FaceMaskOverlay
```

#### 📷 Camera Integration
```kotlin
// CameraX integration with lifecycle-aware components
CameraRepository -> ImageCapture -> PhotoCapture
```

## 🛠️ Tech Stack

### Core Technologies
- **Kotlin** - 100% Kotlin codebase
- **CameraX** - Modern camera API
- **Firebase ML Kit** - Face detection
- **Hilt** - Dependency injection
- **ViewBinding** - Type-safe view references

### Architecture Components
- **ViewModel** - UI-related data holder
- **StateFlow** - Reactive state management
- **LiveData** - Lifecycle-aware observables
- **Repository Pattern** - Data abstraction
- **Use Cases** - Business logic encapsulation

### UI & Design
- **Material 3** - Modern design components
- **ConstraintLayout** - Flexible UI layouts
- **RecyclerView** - Efficient list rendering
- **Custom Views** - Face mask overlay system

### Permissions & Storage
- **EasyPermissions** - Simplified permission handling
- **MediaStore** - Modern storage API
- **ExifInterface** - Photo metadata handling

## 📱 UI Components

### Main Camera Screen
- **PreviewView** - Live camera feed
- **Control Overlays** - Floating UI controls
- **Filter Selector** - Vertical filter list
- **Face Mask Selector** - AR mask options
- **Zoom/Brightness Sliders** - Vertical controls

### Custom Views
- **FaceMaskOverlayView** - Real-time AR mask rendering
- **Adaptive Controls** - Context-sensitive UI elements

## 🎯 Usage Example

```kotlin
// Initialize Camera Fragment
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val cameraFragment = CameraFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, cameraFragment)
            .commit()
    }
}

// Use Camera ViewModel
@HiltViewModel
class CameraViewModel @Inject constructor(
    private val capturePhotoUseCase: CapturePhotoUseCase,
    private val faceDetectionRepository: FaceDetectionRepository
) : ViewModel() {
    
    fun takePhoto() {
        viewModelScope.launch {
            capturePhotoUseCase(currentSettings)
                .collect { event ->
                    when (event) {
                        is CameraEvent.PhotoCaptured -> handleSuccess()
                        is CameraEvent.Error -> handleError(event.message)
                    }
                }
        }
    }
}
```

## 🔐 Permissions Required

```xml
<!-- Camera -->
<uses-permission android:name="android.permission.CAMERA" />

<!-- Storage (Android 10 and below) -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" 
    android:maxSdkVersion="28" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />

<!-- Camera Features -->
<uses-feature android:name="android.hardware.camera" android:required="true" />
```

## 🚀 Getting Started

1. **Add to your project**:
   ```kotlin
   implementation project(':Camera')
   ```

2. **Enable Hilt in your app**:
   ```kotlin
   @HiltAndroidApp
   class MyApplication : Application()
   ```

3. **Add Camera Fragment**:
   ```kotlin
   val cameraFragment = CameraFragment.newInstance()
   supportFragmentManager.beginTransaction()
       .replace(R.id.container, cameraFragment)
       .commit()
   ```

## 🎨 Customization

### Add Custom Filters
```kotlin
enum class FilterType {
    NONE, SEPIA, BLACK_WHITE, 
    YOUR_CUSTOM_FILTER  // Add here
}
```

### Add Custom Face Masks
```kotlin
val customMask = FaceMask(
    id = "custom_mask",
    name = "Custom Mask",
    type = MaskType.FULL_FACE,
    previewImage = R.drawable.custom_preview,
    overlayResources = listOf(/* mask overlays */)
)
```

## 🧪 Testing

- **Unit Tests** - Repository and ViewModel logic
- **UI Tests** - Camera interactions with Espresso
- **Integration Tests** - End-to-end camera flow

## 📚 Dependencies

See `build.gradle.kts` for complete dependency list including:
- CameraX libraries
- Firebase ML Kit
- Hilt DI
- Architecture Components
- UI Libraries

## 🤝 Contributing

1. Follow Android Kotlin coding guidelines
2. Use Clean Architecture principles  
3. Add unit tests for new features
4. Update documentation

## 📄 License

This project follows standard Android development practices and is designed for educational and production use.

---

**Built with ❤️ using Modern Android Development practices** 