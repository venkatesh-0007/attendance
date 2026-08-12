# Attendance Tracker

![Android CI Workflow](https://img.shields.io/badge/Build-Passing-brightgreen?style=flat-square&logo=github)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-blue?style=flat-square&logo=kotlin)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%26%20M3-4285F4?style=flat-square&logo=android)
![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20Clean-orange?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)

**Attendance Tracker** is a modern, native Android application built with Kotlin, Jetpack Compose, Material 3, and Glance Home Screen Widgets. It helps university and college students seamlessly track course-wise attendance, simulate future leave scenarios, view interactive timetables, and monitor daily register statuses directly from their home screen.

---

## 🚀 Key Features

### 📱 Home Screen Widgets (Jetpack Glance)
- **4x2 Dashboard Widget**: Displays overall percentage, status badge (`SAFE` / `CRITICAL`), skip or attend period counts, today's period status timeline, and last sync timestamp.
- **2x2 Attendance Overview Widget**: Compact view with attendance stats, target threshold margin, and period status chips.
- **2x1 Minimal Badge Widget**: Clean badge displaying overall percentage and attendance status.
- **Interactive Tap Actions**: Tapping widget sections directly opens specific app screens (Dashboard, Attendance Grid, Subject Details).

### 📊 App Features & Analytics
- **Dashboard**: High-level attendance gauge, target warning alerts, class counters, and period timelines.
- **Subject Breakdown**: Course-wise attendance progress, search filtering, and sorting (alphabetical, attendance percentage).
- **Leave & Bunk Simulator**: Simulate marking future leaves or holidays to dynamically calculate projected attendance percentages and skip allowances.
- **Attendance Grid**: Full attendance register table featuring horizontal scroll and sticky subject headers.
- **Weekly Schedule & Faculty**: Interactive timetable with faculty details and contact info.
- **PDF Export**: Generate and share summary PDF reports.
- **Multi-Account Management**: Easily switch between saved student accounts with custom display names.

---

## 🛠️ Architecture & Tech Stack

The app adheres to modern Android Development (MAD) standards, using Clean Architecture with decoupled ViewModels, StateFlow UI states, and Dependency Injection.

- **Language**: Kotlin 2.0
- **UI Framework**: Jetpack Compose & Material Design 3
- **Navigation**: Navigation 3 (`androidx.navigation3`)
- **Widgets**: Jetpack Glance
- **Dependency Injection**: Dagger Hilt
- **Background Tasks**: WorkManager with custom Configuration Provider
- **Local Storage & Security**: EncryptedSharedPreferences (`androidx.security.crypto`)
- **Serialization**: Kotlinx Serialization

---

## 📂 Project Structure

```
app/src/main/java/com/attendance/app/
├── AttendanceApplication.kt    # Application entry point & Hilt setup
├── MainActivity.kt             # Main Activity & Compose Window entry
├── Navigation.kt               # Jetpack Navigation 3 router
├── NavigationKeys.kt           # Type-safe Navigation keys
├── data/
│   ├── api/                    # Retrofit network API interface
│   ├── local/                  # EncryptedSharedPreferences cache manager
│   ├── model/                  # Domain entities, DTO responses & summaries
│   └── repository/             # Attendance repository implementation
├── di/                         # Hilt Dependency Injection modules
├── theme/                      # Material 3 Color palette, Typography, & Theme
├── ui/
│   ├── attendance/             # Subject breakdown & Leave simulator (Screen, VM, Components)
│   ├── calendar/               # Attendance Calendar Screen
│   ├── dashboard/              # Home Dashboard (Screen, VM, Components)
│   ├── login/                  # Portal Login Screen & ViewModel
│   ├── settings/               # App Settings & Account Manager
│   └── timetable/              # Class schedule & Faculty info
├── util/                       # PDF Exporter & Notification Helper
├── widget/                     # Glance Widgets, Receivers, & Updater
└── worker/                     # WorkManager background sync & BootReceiver
```

---

## 🛠️ Building & Running

### Prerequisites
- **Android Studio Koala** or newer
- **Android SDK 36**
- **JDK 17**

### Build Commands

```bash
# Build Debug APK
./gradlew assembleDebug

# Clean & Build Debug APK
./gradlew clean assembleDebug

# Install on connected device or emulator
./gradlew installDebug
```

---

## 👨‍💻 Author & Creator

- **Developer**: **Venkatesh** ([@venkatesh-0007](https://github.com/venkatesh-0007))
- **Project**: Attendance Tracker (Android App & Home Screen Widgets)

---

## 📄 License
Distributed under the MIT License. See `LICENSE` for details.
