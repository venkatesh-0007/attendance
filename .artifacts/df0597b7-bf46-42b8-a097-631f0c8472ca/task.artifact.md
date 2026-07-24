# Task: Feature Enhancement (Multi-Account, Theming, Advanced Widget)

## Phase 1: Data & Security
- [x] Update `SecurePreferences.kt` for multi-account storage and accent color
- [x] Update `AttendanceRepository.kt` to handle account switching and specific caching
- [x] Update `LoginViewModel.kt` to support adding new accounts without logging out active ones

## Phase 2: Theming Engine
- [x] Update `AttendanceTheme` in `Theme.kt` to support custom seed colors
- [x] Update `MainActivity.kt` to observe and apply theme changes

## Phase 3: UI Enhancements
- [x] Implement `AttendanceTableScreen.kt` for the full attendance grid
- [x] Update `DashboardScreen.kt` with user identity and navigation to Table
- [x] Overhaul `SettingsScreen.kt` with Account Manager and Theme Picker

## Phase 4: Widget Overhaul
- [x] Implement multi-user state in `AttendanceWidget.kt`
- [x] Implement "Today's Summary" (P/A count) logic
- [x] Update Widget UI for account identity and daily stats

## Phase 5: Verification
- [x] Verify build and run automated tests
- [x] Manual verification of multi-account switching
- [x] Manual verification of theme changes
- [x] Manual verification of widget today stats
