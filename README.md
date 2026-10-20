# 🎵 Melo (v0.0.1)

> A modern, ad-free Android music app with real-time synchronized lyrics, offline downloads, audio recognition, and lossless streaming built using Kotlin and Jetpack Compose.

---

## 🛠️ Tech Stack Overview

* **Language**: [Kotlin](https://kotlinlang.org/)
* **UI Framework**: [Jetpack Compose](https://developer.android.com/compose) (Material 3 Design)
* **Audio Playback**: [AndroidX Media3 (ExoPlayer)](https://developer.android.com/guide/topics/media/media3)
* **Dependency Injection**: [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
* **Local Storage & Cache**: [Room Database](https://developer.android.com/training/data-storage/room) & [Preferences DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
* **Networking**: [Ktor](https://ktor.io/) & [Retrofit](https://square.github.io/retrofit/)

---

## 🧱 Module Architecture Guide

For new developers joining the codebase, the repository is split into modular subprojects:

```
Melo/
├── app/                  # Main Android App (Compose Screens, Navigation, ViewModels, Playback)
├── innertube/            # YouTube Music API & Stream Extractor Pipeline
├── shazamkit/            # Audio Recognition Engine (Song Identifier / VibraFP)
├── lrclib/               # LRCLIB Lyrics Provider Engine
├── kugou/                # KuGou Lyrics Provider Engine
├── betterlyrics/         # BetterLyrics Provider Engine
├── paxsenixlyrics/       # Paxsenix Lyrics Provider Engine
├── canvas/               # Canvas Artwork & Animations
├── applecanvas/          # Apple Music Style Canvas Visuals
└── artistvideo/          # Music Video Playback Components
```

---

## 📂 Key Source Directories in `app/`

| Directory | Description |
| :--- | :--- |
| `app/.../ui/screens` | Jetpack Compose UI screens (`HomeScreen.kt`, `ExploreScreen.kt`, `LibraryScreen.kt`, `AboutScreen.kt`) |
| `app/.../ui/player` | Mini Player & Full-Screen Player sheet (`Player.kt`, `Lyrics.kt`) |
| `app/.../viewmodels` | State management ViewModels (`HomeViewModel.kt`, `PlayerViewModel.kt`) |
| `app/.../db` | Room Database entities, DAOs, and migrations |
| `app/.../playback` | ExoPlayer audio session and background player service |

---

## ⚙️ How to Build & Run Locally

### 1. Prerequisites
* **JDK 21** installed (`JAVA_HOME` pointing to JDK 21).
* **Android SDK** (Compile SDK: `36`, Min SDK: `26`, NDK: `27.0.12077973`).

### 2. Configure `local.properties`
Create a `local.properties` file in the root folder:

```properties
sdk.dir=C:\\Users\\YOUR_USERNAME\\AppData\\Local\\Android\\sdk
```

### 3. Compile the APK
Open PowerShell in the project directory and run:

```powershell
# Compile Universal FOSS Debug APK
.\gradlew.bat assembleUniversalFossDebug
```

📁 **Generated APK Output Location**:
`app/build/outputs/apk/universalFoss/debug/app-universal-foss-debug.apk`

For advanced setup details and CI options, see the [Developer Setup Guide](docs/SETUP.md).

---

## 📖 Documentation

* [**Project Documentation Index**](docs/README.md)
* [**Feature Matrix**](docs/FEATURES.md)
* [**Wordmark & Vector Geometry Spec**](docs/MELO_WORDMARK_SPEC.md)
* [**AppBar Architecture & Motion Specs**](docs/appbar.md)
* [**Release Information**](docs/RELEASE_INFO.md)
* [**Codebase Static Analysis**](docs/MELO_STATIC_ANALYSIS.md)

---

## 📜 License
Licensed under [GPL-3.0](LICENSE).
