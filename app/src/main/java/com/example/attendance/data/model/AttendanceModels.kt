package com.example.attendance.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AttendanceResponse(
    val overall_attendance: Double? = null,
    val attended_classes: Int? = null,
    val held_classes: Int? = null,
    val todays_attendance: String? = null,
    val subject_wise_attendance: List<SubjectAttendance>? = null,
    val timetable: List<TimetableDay>? = null,
    val faculty_information: List<FacultyInfo>? = null,
    val attendance_history: List<AttendanceHistoryItem>? = null,
    val error: String? = null
)

@Serializable
data class SubjectAttendance(
    val subject: String,
    val code: String? = null,
    val percentage: Double,
    val attended: Int,
    val held: Int,
    val history: List<AttendanceHistoryItem>? = null
)

@Serializable
data class TimetableDay(
    val day: String, // e.g. "Monday", "Tuesday", etc.
    val classes: List<TimetableClass>
)

@Serializable
data class TimetableClass(
    val subject: String,
    val time: String, // e.g. "09:00 AM - 10:00 AM"
    val room: String? = null,
    val faculty: String? = null
)

@Serializable
data class FacultyInfo(
    val name: String,
    val subject: String? = null,
    val email: String? = null,
    val phone: String? = null
)

@Serializable
data class AttendanceHistoryItem(
    val date: String, // e.g. "2026-07-19"
    val status: String, // "Present", "Absent", "P", "A"
    val subject: String? = null
)
