# Firebase Authentication Library

Thư viện đăng nhập Firebase cho Android, hỗ trợ các phương thức đăng nhập:
- Google
- Email và Password (bao gồm xác minh email)
- Số điện thoại

## Cài đặt

### Gradle

Thêm JitPack repository vào file build.gradle cấp project:

```groovy
allprojects {
    repositories {
        // ...
        maven { url 'https://jitpack.io' }
    }
}