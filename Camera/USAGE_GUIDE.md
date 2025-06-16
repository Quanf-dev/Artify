# 📸 Hướng Dẫn Sử Dụng Camera Module

## 🚀 Các Cách Sử Dụng Camera Module

### 1. **Cách Đơn Giản Nhất - Sử dụng Extension Functions**

```kotlin
// Trong Activity
class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Mở camera thông thường
        findViewById<Button>(R.id.btnCamera).setOnClickListener {
            openCamera() // Extension function
        }
        
        // Mở camera với face detection
        findViewById<Button>(R.id.btnFaceCamera).setOnClickListener {
            openCameraWithFaceDetection()
        }
        
        // Mở camera với auto capture
        findViewById<Button>(R.id.btnAutoCapture).setOnClickListener {
            openCameraWithAutoCapture()
        }
    }
}
```

```kotlin
// Trong Fragment
class HomeFragment : Fragment() {
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.btnCamera.setOnClickListener {
            openCamera() // Extension function cho Fragment
        }
    }
}
```

### 2. **Sử Dụng Builder Pattern (Tùy Chỉnh Chi Tiết)**

```kotlin
// Mở camera với cấu hình tùy chỉnh
button.setOnClickListener {
    launchCamera {
        enableFaceDetection(true)
        setDefaultFilter(FilterType.SEPIA)
        setAspectRatio(AspectRatio.RATIO_16_9)
        autoCapture(false)
    }
}
```

```kotlin
// Hoặc tạo Intent custom
val intent = CameraLauncher.with(this)
    .enableFaceDetection(true)
    .setDefaultFilter(FilterType.CINEMATIC)
    .setAspectRatio(AspectRatio.RATIO_1_1)
    .build()
    
startActivity(intent)
```

### 3. **Sử Dụng Static Methods từ CameraActivity**

```kotlin
// Mở camera đơn giản
CameraActivity.openCamera(this)

// Mở camera với face detection
CameraActivity.openCameraWithFaceDetection(this)

// Mở camera với auto capture
CameraActivity.openCameraWithAutoCapture(this)

// Tạo Intent tùy chỉnh
val intent = CameraActivity.createIntent(
    context = this,
    autoCapture = false,
    enableFaceDetection = true,
    defaultFilter = "SEPIA"
)
startActivity(intent)
```

### 4. **Sử Dụng CameraLauncher Object**

```kotlin
// Các cách khác nhau sử dụng CameraLauncher
CameraLauncher.openCamera(this)
CameraLauncher.openCameraWithFaceDetection(this)
CameraLauncher.openCameraWithAutoCapture(this)

// Tạo Intent custom
val customIntent = CameraLauncher.createCustomCameraIntent(
    context = this,
    enableFaceDetection = true,
    defaultFilter = FilterType.BLACK_WHITE,
    aspectRatio = AspectRatio.RATIO_4_3,
    autoCapture = false
)
startActivity(customIntent)
```

## 🎯 Ví Dụ Sử Dụng Trong Các Trường Hợp Thực Tế

### **Trường Hợp 1: Social Media App**
```kotlin
// Button chụp ảnh profile với face detection
btnProfilePhoto.setOnClickListener {
    launchCamera {
        enableFaceDetection(true)
        setDefaultFilter(FilterType.WARM)
        setAspectRatio(AspectRatio.RATIO_1_1) // Square cho profile
    }
}

// Button chụp ảnh story với auto capture
btnStoryPhoto.setOnClickListener {
    launchCamera {
        enableFaceDetection(true)
        setAspectRatio(AspectRatio.RATIO_16_9)
        autoCapture(true) // Tự động chụp
    }
}
```

### **Trường Hợp 2: E-commerce App**
```kotlin
// Chụp ảnh sản phẩm không cần face detection
btnProductPhoto.setOnClickListener {
    launchCamera {
        enableFaceDetection(false)
        setDefaultFilter(FilterType.NONE)
        setAspectRatio(AspectRatio.RATIO_4_3)
    }
}
```

### **Trường Hợp 3: Document Scanner App**
```kotlin
// Chụp ảnh tài liệu với auto capture
btnScanDocument.setOnClickListener {
    launchCamera {
        enableFaceDetection(false)
        setDefaultFilter(FilterType.BLACK_WHITE)
        setAspectRatio(AspectRatio.RATIO_FULL)
        autoCapture(true)
    }
}
```

## 🔧 Tích Hợp Vào Ứng Dụng Chính

### **Bước 1: Thêm Dependency**
```kotlin
// Trong app/build.gradle.kts
dependencies {
    implementation(project(":Camera"))
}
```

### **Bước 2: Enable Hilt trong Application**
```kotlin
@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
```

### **Bước 3: Import Extensions (Tùy Chọn)**
```kotlin
// Thêm vào file Activity/Fragment
import com.example.camera.utils.openCamera
import com.example.camera.utils.openCameraWithFaceDetection
import com.example.camera.utils.launchCamera
```

## 📱 Gọi Từ Intent Actions (Ngoài App)

### **Từ Ứng Dụng Khác**
```kotlin
// Gọi camera module từ app khác
val intent = Intent().apply {
    action = "com.example.camera.OPEN_CAMERA"
    putExtra("enable_face_detection", true)
}
startActivity(intent)
```

### **Như Camera App Mặc Định**
```kotlin
// Sử dụng như camera app thông thường
val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
startActivity(intent)
```

## ⚙️ Cấu Hình Options

### **Các Filter Có Sẵn:**
- `FilterType.NONE` - Không filter
- `FilterType.SEPIA` - Màu nâu vintage  
- `FilterType.BLACK_WHITE` - Đen trắng
- `FilterType.CINEMATIC` - Phong cách điện ảnh
- `FilterType.VINTAGE` - Retro cổ điển
- `FilterType.COLD` - Tông màu lạnh
- `FilterType.WARM` - Tông màu ấm

### **Aspect Ratios:**
- `AspectRatio.RATIO_1_1` - Vuông (1:1)
- `AspectRatio.RATIO_4_3` - Chuẩn (4:3)  
- `AspectRatio.RATIO_16_9` - Rộng (16:9)
- `AspectRatio.RATIO_FULL` - Toàn màn hình

### **Intent Extras:**
- `EXTRA_AUTO_CAPTURE` - Tự động chụp ảnh
- `EXTRA_ENABLE_FACE_DETECTION` - Bật/tắt face detection
- `EXTRA_DEFAULT_FILTER` - Filter mặc định
- `EXTRA_ASPECT_RATIO` - Tỷ lệ khung hình

## 🎭 Face Masks Có Sẵn

- **Dog Ears** - Tai chó đáng yêu
- **Sunglasses** - Kính râm ngầu
- **Cat Face** - Mặt mèo với râu
- **Mustache** - Ria mép cổ điển

## 📸 Ví Dụ Hoàn Chỉnh

```kotlin
class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        setupCameraButtons()
    }
    
    private fun setupCameraButtons() {
        // Camera thông thường
        findViewById<Button>(R.id.btnNormalCamera).setOnClickListener {
            openCamera()
        }
        
        // Camera với face detection  
        findViewById<Button>(R.id.btnFaceCamera).setOnClickListener {
            openCameraWithFaceDetection()
        }
        
        // Camera tự động chụp
        findViewById<Button>(R.id.btnAutoCamera).setOnClickListener {
            openCameraWithAutoCapture()
        }
        
        // Camera tùy chỉnh cho profile
        findViewById<Button>(R.id.btnProfileCamera).setOnClickListener {
            launchCamera {
                enableFaceDetection(true)
                setDefaultFilter(FilterType.WARM)
                setAspectRatio(AspectRatio.RATIO_1_1)
            }
        }
        
        // Camera cho sản phẩm
        findViewById<Button>(R.id.btnProductCamera).setOnClickListener {
            launchCamera {
                enableFaceDetection(false)
                setDefaultFilter(FilterType.NONE)
                setAspectRatio(AspectRatio.RATIO_4_3)
            }
        }
        
        // Camera cho story/video
        findViewById<Button>(R.id.btnStoryCamera).setOnClickListener {
            launchCamera {
                enableFaceDetection(true)
                setDefaultFilter(FilterType.CINEMATIC)
                setAspectRatio(AspectRatio.RATIO_16_9)
            }
        }
    }
}
```

## 🛡️ Permissions

Camera module sẽ tự động xử lý permissions, nhưng bạn cần đảm bảo app chính có permissions trong `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="28" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

## 🎉 Kết Luận

Camera module cung cấp nhiều cách linh hoạt để tích hợp:

1. **Đơn giản**: Sử dụng extension functions
2. **Tùy chỉnh**: Sử dụng Builder pattern  
3. **Nâng cao**: Tạo Intent custom
4. **Từ ngoài**: Gọi qua Intent actions

Chọn cách phù hợp với nhu cầu của ứng dụng bạn! 📱✨ 