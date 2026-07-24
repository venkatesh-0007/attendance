package com.example.attendance

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Login : NavKey
@Serializable data object Dashboard : NavKey
@Serializable data object Attendance : NavKey
@Serializable data class SubjectDetails(val subjectName: String) : NavKey
@Serializable data object AttendanceTable : NavKey
@Serializable data object Timetable : NavKey
@Serializable data object Settings : NavKey
@Serializable data object Calendar : NavKey
