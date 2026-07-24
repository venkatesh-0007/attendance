# Feature Enhancement: Multi-Account Support, Theming, and Advanced Widget

This plan covers adding multiple user support, customizable accent colors, a full attendance history table, and an improved home screen widget.

## User Review Required

> [!IMPORTANT]
> **Multi-Account Support**: Switching accounts will reload the entire dashboard data. I will implement an "Account Manager" in the Settings screen.
> **Theming**: The accent color choice will override the dynamic color on Android 12+ if a specific color is selected.

## Proposed Changes

### 1. Data Layer & Preferences

#### [MODIFY] [SecurePreferences.kt](file:///Users/venkatesh/Documents/Projects/Attendance/app/src/main/java/com/example/attendance/data/local/SecurePreferences.kt)
- Add `accountsJson` to store a list of student IDs and passwords.
- Add `activeStudentId` to track the current session.
- Add `accentColorHex` to store the user's preferred theme color.
- Update `hasCredentials` and other getters to return data for the *active* student.

#### [MODIFY] [AttendanceRepository.kt](file:///Users/venkatesh/Documents/Projects/Attendance/app/src/main/java/com/example/attendance/data/repository/AttendanceRepository.kt)
- Update cache logic to use student-specific cache keys (e.g., `attendance_cache_STUDENTID`).
- Ensure `logout` only removes the active account or provides an option to "Log out all".

### 2. Theming Engine

#### [MODIFY] [Theme.kt](file:///Users/venkatesh/Documents/Projects/Attendance/app/src/main/java/com/example/attendance/theme/Theme.kt)
- Update `AttendanceTheme` to accept an optional `accentColor` (Color).
- Use `ColorScheme.fromSeed` or a custom mapping if an accent color is provided.

#### [MODIFY] [MainActivity.kt](file:///Users/venkatesh/Documents/Projects/Attendance/app/src/main/java/com/example/attendance/MainActivity.kt)
- Observe the `accentColor` preference and pass it down to `AttendanceTheme`.

### 3. UI Enhancements

#### [NEW] [AttendanceTableScreen.kt](file:///Users/venkatesh/Documents/Projects/Attendance/app/src/main/java/com/example/attendance/ui/attendance/AttendanceTableScreen.kt)
- A new screen that renders the `attendance_table` in a scrollable grid format.

#### [MODIFY] [SettingsScreen.kt](file:///Users/venkatesh/Documents/Projects/Attendance/app/src/main/java/com/example/attendance/ui/settings/SettingsScreen.kt)
- Add "Manage Accounts" section:
    - List of saved accounts with "Switch" and "Remove" options.
    - "Add Account" button (navigates to Login).
- Add "Theme Customization" section:
    - Color palette for accent color selection.

#### [MODIFY] [DashboardScreen.kt](file:///Users/venkatesh/Documents/Projects/Attendance/app/src/main/java/com/example/attendance/ui/dashboard/DashboardScreen.kt)
- Add a "View Full Table" button in the summary card.
- Show the current student name/ID in the header if multiple accounts exist.

### 4. Widget Enhancements

#### [MODIFY] [AttendanceWidget.kt](file:///Users/venkatesh/Documents/Projects/Attendance/app/src/main/java/com/example/attendance/widget/AttendanceWidget.kt)
- **Multi-User Widgets**:
    - Store `studentId` in the widget's `GlanceState`.
    - Update `provideGlance` to fetch data for the specific `studentId` associated with that widget instance.
    - If no `studentId` is bound (new widget), default to the active user or show an "Associate Account" button.
- **Today's Summary**:
    - Implement logic to find "Today's Column" in `attendance_table`.
    - Calculate total 'P' (Presents) and 'A' (Absents) for today across all subjects.
    - Display "Today: [P] Presents, [A] Absents" in the widget footer.

## Verification Plan

### Automated Tests
- Update `AttendanceSerializationTest.kt` to verify that `attendance_table` parses correctly.
- Add unit tests for the "Today's Column" logic in the widget.

### Manual Verification
1. **Theming**: Go to Settings, pick a color, and verify the app's primary elements change color.
2. **Multi-Account**: Add a second student ID, switch between them, and verify the dashboard data updates.
3. **Table**: Open the "Full Table" and verify the grid matches the portal data.
4. **Widget**: Verify the "Today" count matches the current day's classes.
