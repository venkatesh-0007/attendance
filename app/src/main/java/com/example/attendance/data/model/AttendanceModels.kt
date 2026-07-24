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

    fun getTodaySummary(todayDateStr: String): Pair<Int, Int> {
        val statusString = getTodayStatusString(todayDateStr)
        val presents = statusString.count { it == 'P' }
        val absents = statusString.count { it == 'A' }
        return presents to absents
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
