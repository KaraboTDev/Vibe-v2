<div align="center">

# Vibe Android App

**Discover spaces that match your mood. Save favorites and keep them available offline.**

[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Material Design 3](https://img.shields.io/badge/Material_Design_3-6750A4?style=for-the-badge&logo=materialdesign&logoColor=white)](https://m3.material.io/)

[![Download APK](https://img.shields.io/badge/Download-Android_APK-2E7D32?style=for-the-badge&logo=android&logoColor=white)](https://github.com/KaraboTDev/Vibe-v2/releases/tag/v1)
[![Watch demo](https://img.shields.io/badge/Watch-Video_Demo-FF0000?style=for-the-badge&logo=youtube&logoColor=white)](https://youtube.com/shorts/XG5egFVZpa0?si=Bgoh0r0eQhwv6-VD)

</div>

---

## About

Vibe is an offline-first Android app built with Kotlin. It helps users discover and save venues based on their mood, preferences, and practical needs.

## Features

| Feature | What it offers |
| --- | --- |
| Mood-based discovery | Browse by moods such as Quiet Focus, Social & Lively, Fast Wi-Fi, Creative, and Focus. Filter by distance, opening hours, amenities, and mood-specific tags. |
| Offline favorites | Room caches favorites locally. WorkManager synchronizes changes with the REST API when connectivity is available, with pending-sync status and offline venue details. |
| Google sign-in | One-tap sign-in and registration using Android Credential Manager and Firebase Authentication. |
| Push notifications | Firebase Cloud Messaging notifications can deep-link to venue details. Includes Android 13+ notification permission handling and notification preferences. |
| Localized interface | English, isiZulu (`zu`), and Afrikaans (`af`) support through Android App Locales, with translation tests. |
| Material 3 interface | XML layouts, ViewBinding, rounded cards, gradient overlays, rating pills, and Inter typography. |

## Project document

[Download the Vibe project document (Word)](part3%20document%28VibeApp%29.docx)

## Technology

- **Language:** Kotlin
- **UI:** XML, ViewBinding, Material 3, RecyclerView, ConstraintLayout
- **Architecture:** MVVM, StateFlow, Coroutines, Repository pattern
- **Local persistence:** Room (`vibe.db`)
- **Background synchronization:** WorkManager (`SyncWorker`)
- **Networking:** Retrofit, OkHttp, Gson
- **Authentication:** Firebase Auth and Android Credential Manager
- **Notifications:** Firebase Cloud Messaging and NotificationCompat
- **Testing:** JUnit 4, Kotlinx Coroutines Test, MockWebServer; 145+ unit tests

## Getting started

1. Clone this repository and open the `VibeAndroid` project in Android Studio.
2. Add your Firebase configuration file at `VibeAndroid/app/google-services.json`.
3. From the `VibeAndroid` directory, run the unit tests:

   ```bash
   ./gradlew testDebugUnitTest
   ```

4. Build and run the debug app from Android Studio, or use Gradle to build the debug APK.

The debug APK is generated at `VibeAndroid/app/build/outputs/apk/debug/app-debug.apk`.

## Backend

The app connects to the Vibe REST API at [vibe-api-iu1z.onrender.com](https://vibe-api-iu1z.onrender.com/).
