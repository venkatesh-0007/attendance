# Implementation Plan - Enhance Widget Today Summary

Improve the "Today's Summary" in the Glance widget to accurately reflect the chronological status of classes, distinguish "not yet completed" periods from "absent" ones, and ensure presents are clearly displayed.

## User Review Required

> [!IMPORTANT]
> I will implement a logic that cross-references the `timetable` with the `attendance_table`. This allows us to show periods in chronological order and identify future classes.

> [!NOTE]
> Future classes that are currently marked as 'A' (Absent) in the portal will be displayed as '-' (Pending) to avoid confusion, addressing the "shows absent for not completed periods" feedback.

## Proposed Changes

### Data Model Layer

#### [MODIFY] [AttendanceModels.kt](file:///Users/venkatesh/Documents/Projects/Attendance/app/src/main/java/com/example/attendance/data/model/AttendanceModels.kt)
- Add a new method `getTodayStatusString(todayDateStr: String): String` to `AttendanceResponse`.
- Implement logic to:
    - Get today's classes from `timetable`.
    - Match each class to its corresponding row in `attendance_table`.
    - Extract the status for the specific period.
    - Check if the class is in the future relative to the current time.
    - Map 'A' to '-' for future classes.
    - Combine into a single string (e.g., "PP-A--").
- Add helper methods `getStartMinutes` and `isFutureClass` (migrating logic from `TimetableScreen.kt`).

### UI Layer (Widget)

#### [MODIFY] [AttendanceWidget.kt](file:///Users/venkatesh/Documents/Projects/Attendance/app/src/main/java/com/example/attendance/widget/AttendanceWidget.kt)
- Update the `Today's summary` section to use the new `data.getTodayStatusString(today)`.
- Improve the display text to handle the new string format.

## Verification Plan

### Manual Verification
1. Open the app and ensure attendance data is synced.
2. Check the widget during the day:
    - Verify that completed periods show 'P' or 'A'.
    - Verify that upcoming periods show '-' instead of 'A' (even if the portal marks them as 'A' by default).
    - Verify that the sequence of characters matches the chronological order of the day's timetable.
3. Verify that the "Present" characters ('P') are visible as requested.
