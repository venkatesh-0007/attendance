package com.example.attendance.data.model

import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.*

@Serializable
data class AttendanceResponse(
    val student_name: String? = null,
    val roll_number: String? = null,
    val total_info: TotalInfo? = null,
    val subjectwise_summary: List<SubjectSummary>? = null,
    val attendance_table: AttendanceTable? = null,
    // Keep these for backward compatibility or if they are in other parts of the app
    val timetable: List<TimetableDay>? = null,
    val faculty_information: List<FacultyInfo>? = null,
    val error: String? = null
) {
    val overallPercentage: Double
        get() = total_info?.total_percentage?.replace("%", "")?.toDoubleOrNull() ?: 0.0

    fun getTodaySummary(todayDateStr: String): Pair<Int, Int> {
        val table = attendance_table ?: return 0 to 0
        val headers = table.headers ?: return 0 to 0
        val rows = table.rows ?: return 0 to 0

        val columnIndex = headers.indexOfFirst { it.trim() == todayDateStr }
        if (columnIndex == -1) return 0 to 0

        var presents = 0
        var absents = 0
        rows.forEach { row ->
            if (columnIndex < row.size) {
                val status = row[columnIndex].trim().uppercase()
                presents += status.count { it == 'P' }
                absents += status.count { it == 'A' }
            }
        }
        return presents to absents
    }

    fun getTodayStatusString(todayDateStr: String): String {
        val table = attendance_table ?: return ""
        val headers = table.headers ?: return ""
        val rows = table.rows ?: return ""

        val columnIndex = headers.indexOfFirst { it.trim() == todayDateStr }
        if (columnIndex == -1) return ""

        val dayName = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
        val dayTimetable = timetable?.firstOrNull { it.day.equals(dayName, ignoreCase = true) } ?: return ""
        val classes = dayTimetable.classes.sortedBy { getStartMinutes(it.time) }

        val subjectOccurrenceCount = mutableMapOf<String, Int>()
        val result = StringBuilder()

        val calendar = Calendar.getInstance()
        val nowMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

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
                    if (isFutureClass(timetableClass.time, nowMinutes)) '-' else '?'
                }
            } else {
                if (isFutureClass(timetableClass.time, nowMinutes)) '-' else '?'
            }
            result.append(statusChar)
        }

        return result.toString()
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
        // Adding a small buffer (e.g. 5 mins) to consider a class "started"
        return getStartMinutes(timeString) > (nowMinutes + 5)
    }
}

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
