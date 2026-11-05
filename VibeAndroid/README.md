# Vibe Android App 🌊☕

**Vibe** is a modern, offline-first Android application built in Kotlin (using XML Views with ViewBinding and Material 3) that helps users discover and save spaces matching their current mood and lifestyle.

---

## ✨ Key Features

1. **Bento Grid Dashboard & Mood Search**
   - Immersive home dashboard with mood cards (Quiet Focus, Social & Lively, Fast Wi-Fi, Creative, Focus).
   - Filter venues by mood, distance, opening hours, and amenities.
   - Dynamic sub-tag filter chips tailored to each selected mood.

2. **Offline-First Favorites Sync**
   - Full offline caching powered by **Room Database** and **WorkManager**.
   - Automatic background synchronization (Push & Pull) with the backend REST API (`https://vibe-api-iu1z.onrender.com/`).
   - Sync-pending status indicators (*"Waiting to sync"*) and offline copy fallback in venue details.

3. **Google Single Sign-On (SSO)**
   - Secure authentication via Android **Credential Manager API** and Firebase Auth.
   - One-tap *"Continue with Google"* login and registration flow.

4. **Push Notifications (FCM)**
   - **Firebase Cloud Messaging (FCM)** integration supporting rich push notifications and deep-linking directly to venue details.
   - Runtime permission handling for Android 13+ (`POST_NOTIFICATIONS`) and user preference toggles.

5. **Multi-Language Localisation (`zu` & `af`)**
   - Full support for English (`en`), isiZulu (`zu`), and Afrikaans (`af`) via Android App Locales (`AppLocale`).
   - Comprehensive translation test suites verifying placeholder parity and plural definitions.

6. **Material 3 Design & Custom Typography**
   - Built with Material 3 components, rounded corner cards, gradient overlays, rating pills, and global **Inter** typography.

---

## 🛠 Tech Stack & Architecture

- **Language:** Kotlin
- **UI:** XML Layouts, ViewBinding, Material 3, RecyclerView (`ListAdapter`), ConstraintLayout
- **Architecture:** MVVM (Model-View-ViewModel), StateFlow, Coroutines, Repository pattern
- **Local Storage:** Room Database (`vibe.db`)
- **Background Jobs:** WorkManager (`SyncWorker`)
- **Networking:** Retrofit, OkHttp, Gson, MockWebServer
- **Authentication & SSO:** Firebase Auth, Android Credential Manager (`GetGoogleIdOption`)
- **Notifications:** Firebase Cloud Messaging (FCM), NotificationCompat
- **Testing:** JUnit 4, Kotlinx Coroutines Test, MockWebServer (145+ unit tests passing)

---

## 🚀 Getting Started

1. Clone the repository.
2. Open the `VibeAndroid` project in **Android Studio**.
3. Ensure your `app/google-services.json` file is present.
4. Run `./gradlew testDebugUnitTest` to execute all unit tests.
5. Build and run the debug APK (`app/build/outputs/apk/debug/app-debug.apk`).
