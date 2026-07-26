package com.example.attendance.data.model

import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.*

@Serializable
data class TodayCountSummary(
    val presents: Int = 0,
    val absents: Int = 0,
    val pending: Int = 0,
    val totalClasses: Int = 0
) {
    fun toDisplayString(): String {
        if (totalClasses == 0) return "Today: No Classes"
        val parts = mutableListOf<String>()
        parts.add("$presents Present")
        parts.add("$absents Absent")
        if (pending > 0) {
            parts.add("($pending Pending)")
        }
        return "Today: ${parts.joinToString(", ")}"
    }
}

@Serializable
data class AttendanceResponse(
    val student_name: String? = null,
    val roll_number: String? = null,
    val total_info: TotalInfo? = null,
    val subjectwise_summary: List<SubjectSummary>? = null,
    val attendance_table: AttendanceTable? = null,
    val timetable: List<TimetableDay>? = null,
    val faculty_information: List<FacultyInfo>? = null,
    val error: String? = null
) {
    val overallPercentage: Double
        get() = total_info?.total_percentage?.replace("%", "")?.toDoubleOrNull() ?: 0.0

    fun getTodayColumnIndex(todayDateStr: String): Int {
        val table = attendance_table ?: return -1
        val headers = table.headers ?: return -1

        val parts = todayDateStr.split("/", "-")
        val targetDay = parts.getOrNull(0)?.toIntOrNull()
        val targetMonth = parts.getOrNull(1)?.toIntOrNull()

        return headers.indexOfFirst { header ->
            val clean = header.trim()
            if (clean == todayDateStr || clean.startsWith(todayDateStr) || clean.replace("-", "/").contains(todayDateStr)) {
                true
            } else if (targetDay != null && targetMonth != null) {
                val hParts = clean.split("/", "-")
                if (hParts.size >= 2) {
                    val hDay = hParts[0].toIntOrNull()
                    val hMonth = hParts[1].toIntOrNull()
                    hDay == targetDay && hMonth == targetMonth
                } else false
            } else false
        }
    }

    fun getTodayCountSummary(todayDateStr: String): TodayCountSummary {
        val statusString = getTodayStatusString(todayDateStr)
        if (statusString.isEmpty()) return TodayCountSummary(0, 0, 0, 0)

        val presents = statusString.count { it == 'P' }
        val absents = statusString.count { it == 'A' }
        val pending = statusString.count { it == '-' }
        return TodayCountSummary(presents, absents, pending, statusString.length)
    }

    fun getTodaySummary(todayDateStr: String): Pair<Int, Int> {
        val countSummary = getTodayCountSummary(todayDateStr)
        return countSummary.presents to countSummary.absents
    }

    fun getTodayStatusString(todayDateStr: String): String {
        val table = attendance_table ?: return ""
        val rows = table.rows ?: return ""
        val columnIndex = getTodayColumnIndex(todayDateStr)
        if (columnIndex == -1) return ""

        val dayName = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
        val dayTimetable = timetable?.firstOrNull { it.day.equals(dayName, ignoreCase = true) }

        val calendar = Calendar.getInstance()
        val nowMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

        if (dayTimetable != null && dayTimetable.classes.isNotEmpty()) {
            val classes = dayTimetable.classes.sortedBy { getStartMinutes(it.time) }
            val subjectOccurrenceCount = mutableMapOf<String, Int>()
            val result = StringBuilder()

            classes.forEach { timetableClass ->
                val subjectName = timetableClass.subject
                val row = rows.firstOrNull { it.size > 1 && it[1].contains(subjectName, ignoreCase = true) }

                val statusChar = if (row != null && columnIndex < row.size) {
                    val fullStatus = row[columnIndex].trim().uppercase()
                    val occurrence = subjectOccurrenceCount.getOrDefault(subjectName, 0)
                    subjectOccurrenceCount[subjectName] = occurrence + 1

                    if (occurrence < fullStatus.length) {
                        val char = fullStatus[occurrence]
                        if (char == 'A' && isFutureClass(timetableClass.time, nowMinutes)) {
                            '-'
                        } else {
                            char
                        }
                    } else {
                        if (isFutureClass(timetableClass.time, nowMinutes)) '-' else '-'
                    }
                } else {
                    if (isFutureClass(timetableClass.time, nowMinutes)) '-' else '-'
                }
                result.append(statusChar)
            }
            return result.toString()
        } else {
            val result = StringBuilder()
            rows.forEach { row ->
                if (columnIndex < row.size) {
                    val fullStatus = row[columnIndex].trim().uppercase()
                    fullStatus.forEach { char ->
                        if (char == 'P' || char == 'A') {
                            result.append(char)
                        }
                    }
                }
            }
            return result.toString()
        }
    }

    private fun getStartMinutes(timeString: String): Int {
        return try {
            val parts = timeString.split("-")
            if (parts.isEmpty()) return 0
            val startStr = parts[0].trim()
            val sdf = SimpleDateFormat("hh:mm a", Locale.US)
            val date = sdf.parse(startStr) ?: return 0
            val cal = Calendar.getInstance().apply { time = date }
            cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        } catch (e: Exception) {
            0
        }
    }

    private fun isFutureClass(timeString: String, nowMinutes: Int): Boolean {
        return getStartMinutes(timeString) > (nowMinutes + 5)
    }

    fun getTodayAttendanceTimeline(todayDateStr: String): List<AttendanceStatus> {
        val statusString = getTodayStatusString(todayDateStr)
        if (statusString.isEmpty()) return emptyList()

        return statusString.map { char ->
            when (char.uppercaseChar()) {
                'P' -> AttendanceStatus.PRESENT
                'A' -> AttendanceStatus.ABSENT
                'H' -> AttendanceStatus.HOLIDAY
                'L' -> AttendanceStatus.LEAVE
                else -> AttendanceStatus.UPCOMING
            }
        }
    }

    fun toWidgetState(lastUpdatedMillis: Long = System.currentTimeMillis()): AttendanceWidgetState {
        val percentage = overallPercentage
        val status = when {
            percentage >= 85.0 -> OverallAttendanceStatus.SAFE
            percentage >= 75.0 -> OverallAttendanceStatus.WARNING
            else -> OverallAttendanceStatus.CRITICAL
        }

        val attended = total_info?.total_attended ?: 0
        val held = total_info?.total_held ?: 0

        val calculatedCanSkip = if (held > 0 && percentage >= 75.0) {
            val maxSkip = kotlin.math.floor((attended - 0.75 * held) / 0.75).toInt()
            maxOf(0, maxSkip)
        } else 0
        val periodsCanSkip = total_info?.hours_can_skip ?: calculatedCanSkip

        val calculatedNeedToAttend = if (held > 0 && percentage < 75.0) {
            val minAttend = kotlin.math.ceil((0.75 * held - attended) / 0.25).toInt()
            maxOf(0, minAttend)
        } else 0
        val periodsNeedToAttend = total_info?.additional_hours_needed ?: calculatedNeedToAttend

        val todayDate = SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date())
        val timeline = getTodayAttendanceTimeline(todayDate)

        val lastUpdatedStr = if (lastUpdatedMillis > 0) {
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(lastUpdatedMillis))
        } else {
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        }

        val diff = percentage - 75.0
        val targetMargin = if (diff >= 0) {
            String.format(Locale.getDefault(), "+%.2f%% vs target", diff)
        } else {
            String.format(Locale.getDefault(), "%.2f%% vs target", diff)
        }

        return AttendanceWidgetState(
            attendancePercentage = percentage,
            attendanceStatus = status,
            periodsCanSkip = periodsCanSkip,
            periodsNeedToAttend = periodsNeedToAttend,
            todayAttendanceTimeline = timeline,
            attendedClasses = attended,
            heldClasses = held,
            lastUpdated = lastUpdatedStr,
            studentName = student_name ?: "",
            targetMargin = targetMargin
        )
    }
}

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
    val studentName: String? = null
)

@Serializable
data class TotalInfo(
    val total_attended: Int,
    val total_held: Int,
    val total_percentage: String,
    val hours_can_skip: Int? = null,
    val additional_hours_needed: Int? = null
)

@Serializable
data class SubjectSummary(
    val subject_name: String,
    val attended: Int,
    val held: Int,
    val percentage: String,
    val hours_can_skip: Int? = null,
    val hours_needed: Int? = null
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

