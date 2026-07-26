# Attendance App

An Android application for tracking student attendance, featuring home screen widgets, subject breakdowns, and timetable schedule management.

---

## Features

### Home Screen Widgets
- **4x2 Widget**: Shows overall attendance percentage, status badge (SAFE / WARNING / CRITICAL), periods you can skip or need to attend, today's period timeline, and total attended/held classes.
- **2x2 Widget**: Compact view displaying percentage, status badge, skip/attend count, and period status chips.
- **2x1 Widget**: Minimal badge showing overall attendance percentage and class counts.
- **Widget Actions**: Tapping sections opens the Dashboard, Attendance Grid, or Subject Breakdown.

### App Features
- **Dashboard**: Overview of attendance percentage, today's class status, and quick menu options.
- **Subject Breakdown**: Course-wise attendance counts, search, and sorting.
- **Attendance Grid**: Full attendance register table with a sticky subject column for horizontal scrolling across dates.
- **Class Schedule**: Weekly timetable with faculty contact information.
- **Calendar & Export**: Monthly attendance history and PDF export option.

---

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose & Material 3
- **Widgets**: Jetpack Glance
- **Architecture**: MVVM with Repository Pattern
- **Dependency Injection**: Dagger Hilt
- **Local Storage**: EncryptedSharedPreferences
- **Background Tasks**: WorkManager

---

## Building and Running

### Prerequisites
- Android Studio Koala or newer
- Android SDK 34+
- JDK 17

### Build Commands

```bash
# Build Debug APK
./gradlew assembleDebug

# Install on connected device or emulator
./gradlew installDebug
```
