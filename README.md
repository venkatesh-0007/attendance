# Jetpack Compose Attendance Tracker

A production-quality, high-fidelity native Android application built with **Kotlin**, **Jetpack Compose (Material 3)**, and **Dagger Hilt** to fetch, store, and track student attendance securely.

The application implements a secure credentials cache, an offline-first repository pattern, background synchronizations with Jetpack WorkManager, real-time threshold notifications, and a Home Screen widget built with Jetpack Glance.

---

## ✨ Features

- **Secure Login**: Stores student credentials (ID and password) safely using `EncryptedSharedPreferences`. Features auto-login on subsequent app launches.
- **Home Dashboard**: Offers a visual representation of overall attendance using a progress gauge, period-by-period statuses for today's classes, pull-to-refresh sync, and quick actions.
- **Subject-Wise Metrics**: View detailed statistics (percentage and Attended/Held counts) for each course on clean Material 3 cards.
- **Detailed History Logs**: Click on any subject card to view a chronological log of presence/absence records.
- **Weekly Schedule & Timetable**: Access the weekly timetable class layout. It automatically highlights today's periods and includes an expandable directory of contact info for course faculty.
- **Preferences & Configuration**: 
  - Change themes instantly (System Default, Light, or Dark).
  - Reschedule background synchronization intervals (1hr, 3hr, 6hr, 12hr, 24hr).
  - Drag a threshold slider (50% - 95%) to configure when push alerts should warn you of low attendance.
- **Jetpack Glance Widget**: A 2x2 home screen widget showing overall attendance, attended/held ratios, and today's schedule at a glance, complete with a manual refresh button.

---

## 🛠️ Architecture & Technology Stack

The application conforms to standard Android architecture guidelines using an **MVVM (Model-View-ViewModel)** design:

```
               [ UI / Jetpack Compose ]
                          │
                          ▼
               [ ViewModels (Hilt) ]
                          │
                          ▼
             [ AttendanceRepository ]
             /                      \
            ▼                        ▼
[ AttendanceApi (Retrofit) ]   [ SecurePreferences ]
```

### Key Libraries and Tools

- **UI & Layout**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with [Material 3](https://developer.android.com/jetpack/compose/design/material) design system.
- **Navigation**: [Navigation 3 (Compose)](https://developer.android.com/guide/navigation/navigation-principles) for type-safe, backstack-based routing.
- **Dependency Injection**: [Dagger Hilt](https://developer.android.com/training/dependency-injection/hilt-android) and Hilt Work for dependency scoping.
- **Local Storage**: [Jetpack Security Crypto](https://developer.android.com/topic/security/data) (`EncryptedSharedPreferences`) for storing session credentials and offline-first JSON cache.
- **Networking**: [Retrofit 2](https://square.github.io/retrofit/) and OkHttp 3 with Kotlinx Serialization.
- **Background Work**: [Jetpack WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) for periodic background checks.
- **Widgets**: [Jetpack Glance](https://developer.android.com/jetpack/compose/glance) for dynamic AppWidgets.

---

## 🔌 API Integration

The application synchronizes data from the following REST endpoint:

- **Method**: `GET`
- **URL**: `https://register-api-green.vercel.app/attendance`
- **Query Parameters**:
  - `student_id`: Student register number
  - `password`: Portal password
- **Sample Payload**:
  ```json
  {
    "overall_attendance": 80.92,
    "attended_classes": 123,
    "held_classes": 152,
    "todays_attendance": "PPPPPPP",
    "subject_wise_attendance": [
      {
        "subject": "Mathematics IV",
        "code": "MATH301",
        "percentage": 85.2,
        "attended": 30,
        "held": 35
      }
    ],
    "timetable": [
      {
        "day": "Monday",
        "classes": [
          {
            "subject": "Mathematics IV",
            "time": "09:00 AM - 10:00 AM",
            "room": "L-201",
            "faculty": "Dr. Sarah Connor"
          }
        ]
      }
    ],
    "faculty_information": [
      {
        "name": "Dr. Sarah Connor",
        "subject": "Mathematics IV",
        "email": "sarah.connor@university.edu",
        "phone": "+1-555-0199"
      }
    ],
    "attendance_history": [
      {
        "subject": "Mathematics IV",
        "date": "2026-07-15",
        "status": "Present"
      }
    ]
  }
  ```

---

## 🚀 Getting Started

### Prerequisites

- Android Studio Koala / Ladybug or newer.
- Android SDK 34+.
- JDK 17 (pre-configured in build toolchain).

### Setup and Compilation

1. **Clone the Repository**
   ```bash
   git clone https://github.com/your-username/attendance-compose.git
   cd attendance-compose
   ```

2. **Clean and Compile Debug Build**
   ```bash
   ./gradlew clean assembleDebug
   ```

3. **Run Unit and Local Verification Checks**
   ```bash
   ./gradlew test
   ```

4. **Run on Device**
   Install on a connected device/emulator:
   ```bash
   ./gradlew installDebug
   ```
