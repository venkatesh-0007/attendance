package com.attendance.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SimulationResult(
    val originalPercentage: Double = 0.0,
    val simulatedPercentage: Double = 0.0,
    val originalAttended: Int = 0,
    val simulatedAttended: Int = 0,
    val originalHeld: Int = 0,
    val simulatedHeld: Int = 0,
    val originalCanSkip: Int = 0,
    val simulatedCanSkip: Int = 0,
    val originalNeedToAttend: Int = 0,
    val simulatedNeedToAttend: Int = 0,
    val addedLeavesCount: Int = 0,
    val addedHolidaysCount: Int = 0
)

@Serializable
enum class AttendanceStatus {
    PRESENT,
    ABSENT,
    HOLIDAY,
    LEAVE,
    UPCOMING
}

@Serializable
enum class OverallAttendanceStatus {
    SAFE,
    WARNING,
    CRITICAL
}

@Serializable
data class AttendanceWidgetState(
    val attendancePercentage: Double = 0.0,
    val attendanceStatus: OverallAttendanceStatus = OverallAttendanceStatus.SAFE,
    val periodsCanSkip: Int = 0,
    val periodsNeedToAttend: Int = 0,
    val todayAttendanceTimeline: List<AttendanceStatus> = emptyList(),
    val attendedClasses: Int = 0,
    val heldClasses: Int = 0,
    val lastUpdated: String = "",
    val studentName: String = "",
    val targetMargin: String = ""
)

@Serializable
data class UserAccount(
    val studentId: String,
    val password: String,
    val studentName: String? = null,
    val customName: String? = null
) {
    val displayName: String
        get() = when {
            !customName.isNullOrBlank() -> customName
            !studentName.isNullOrBlank() -> studentName
            else -> "Account ($studentId)"
        }
}

@Serializable
data class TotalInfo(
    @SerialName("total_attended") val totalAttended: Int,
    @SerialName("total_held") val totalHeld: Int,
    @SerialName("total_percentage") val totalPercentage: String,
    @SerialName("hours_can_skip") val hoursCanSkip: Int? = null,
    @SerialName("additional_hours_needed") val additionalHoursNeeded: Int? = null
)

@Serializable
data class SubjectSummary(
    @SerialName("subject_name") val subjectName: String,
    val attended: Int,
    val held: Int,
    val percentage: String,
    @SerialName("hours_can_skip") val hoursCanSkip: Int? = null,
    @SerialName("hours_needed") val hoursNeeded: Int? = null
) {
    val percentageDouble: Double
        get() = percentage.replace("%", "").toDoubleOrNull() ?: 0.0
}

@Serializable
data class AttendanceTable(
    val headers: List<String>? = null,
    val rows: List<List<String>>? = null
)

@Serializable
data class TimetableDay(
    val day: String,
    val classes: List<TimetableClass>
)

@Serializable
data class TimetableClass(
    val subject: String,
    val time: String,
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
