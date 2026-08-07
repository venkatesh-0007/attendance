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

data class CleanTable(
    val headers: List<String>,
    val rows: List<List<String>>
)

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

    fun getCleanTable(): CleanTable? {
        val table = attendance_table ?: return null
        val rawHeaders = table.headers ?: return null
        val rawRows = table.rows ?: return null
        if (rawHeaders.isEmpty() || rawRows.isEmpty()) return null

        val headers = if (rawHeaders.isNotEmpty() && rawHeaders[0].length > 100) {
            rawHeaders.drop(1)
        } else {
            rawHeaders
        }

        val rows = rawRows.filter { row ->
            row.isNotEmpty() && 
            row.getOrNull(0)?.trim()?.lowercase() != "sl.no" &&
            !row.getOrNull(0).isNullOrBlank() &&
            row.getOrNull(1)?.trim()?.lowercase() != "subject"
        }

        val percentIdx = headers.indexOfFirst { it.trim() == "%" }
        val finalHeaders = if (percentIdx != -1) headers.take(percentIdx + 1) else headers
        val finalRows = rows.map { row ->
            if (percentIdx != -1 && percentIdx < row.size) {
                row.take(percentIdx + 1)
            } else {
                row
            }
        }

        return CleanTable(finalHeaders, finalRows)
    }

    private fun isDateHeader(header: String): Boolean {
        val clean = header.trim().lowercase()
        if (clean.isEmpty()) return false

        val nonDateKeywords = listOf(
            "sl", "s.no", "no", "subject", "sub", "atted", "held", "att", "tot", "total",
            "%", "percent", "percentage", "cna", "ratio", "status", "action"
        )
        if (nonDateKeywords.any { clean.contains(it) }) return false

        val monthNames = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
        val hasDigits = clean.any { it.isDigit() }
        val hasMonth = monthNames.any { clean.contains(it) }

        return hasDigits || hasMonth
    }

    fun getTodayColumnIndex(todayDateStr: String = ""): Int {
        val cleanTable = getCleanTable() ?: return -1
        val headers = cleanTable.headers

        val todayCalendar = Calendar.getInstance()
        var targetDay = todayCalendar.get(Calendar.DAY_OF_MONTH)
        var targetMonth = todayCalendar.get(Calendar.MONTH) + 1

        if (todayDateStr.isNotEmpty()) {
            val parts = todayDateStr.split("/", "-", " ", ".").mapNotNull { it.toIntOrNull() }
            if (parts.size >= 2) {
                targetDay = parts[0]
                targetMonth = parts[1]
            }
        }

        // 1. Try matching day and month in valid date headers
        val matchedIndex = headers.indexOfFirst { header ->
            if (!isDateHeader(header)) return@indexOfFirst false

            val clean = header.trim().lowercase()
            if (todayDateStr.isNotEmpty()) {
                val cleanToday = todayDateStr.trim().lowercase().replace("-", "/")
                val cleanHeader = clean.replace("-", "/")
                if (cleanHeader.contains(cleanToday) || cleanToday.contains(cleanHeader)) {
                    return@indexOfFirst true
                }
            }

            // Month name matching (e.g., "29 Jul", "29-Jul")
            val monthNames = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
            val foundMonthIdx = monthNames.indexOfFirst { clean.contains(it) }
            if (foundMonthIdx != -1) {
                val hMonth = foundMonthIdx + 1
                val hDay = clean.filter { it.isDigit() }.toIntOrNull()
                if (hDay == targetDay && hMonth == targetMonth) {
                    return@indexOfFirst true
                }
            }

            val numberParts = clean.split("/", "-", " ", ".").mapNotNull { it.toIntOrNull() }
            if (numberParts.size >= 2) {
                val hDay = numberParts[0]
                val hMonth = numberParts[1]
                hDay == targetDay && hMonth == targetMonth
            } else false
        }

        return matchedIndex
    }

    fun getTodayAttendanceTimeline(todayDateStr: String = ""): List<AttendanceStatus> {
        val cleanTable = getCleanTable() ?: return emptyList()
        val rows = cleanTable.rows
        val headers = cleanTable.headers

        val columnIndex = getTodayColumnIndex(todayDateStr)
        if (columnIndex == -1) return emptyList()

        val timeline = mutableListOf<AttendanceStatus>()

        // 1. Collect all marked period statuses from attendance_table for today's column
        rows.forEach { row ->
            if (columnIndex < row.size) {
                val cellStatus = row[columnIndex].trim().uppercase()
                if (cellStatus.isNotEmpty() && cellStatus != "-" && cellStatus != "0") {
                    cellStatus.forEach { char ->
                        if (char == 'P' || char == 'A' || char == 'H' || char == 'L') {
                            timeline.add(mapCharToStatus(char))
                        }
                    }
                }
            }
        }

        // 2. Append pending periods if timetable defines total scheduled classes for today
        val dayName = SimpleDateFormat("EEEE", Locale.US).format(Date())
        val dayTimetable = timetable?.firstOrNull { it.day.equals(dayName, ignoreCase = true) }

        if (dayTimetable != null && dayTimetable.classes.isNotEmpty()) {
            val totalScheduled = dayTimetable.classes.size
            val currentMarkedCount = timeline.size

            if (totalScheduled > currentMarkedCount) {
                val pendingCount = totalScheduled - currentMarkedCount
                repeat(pendingCount) {
                    timeline.add(AttendanceStatus.UPCOMING)
                }
            }
        }

        return timeline
    }

    private fun mapCharToStatus(char: Char): AttendanceStatus {
        return when (char.uppercaseChar()) {
            'P' -> AttendanceStatus.PRESENT
            'A' -> AttendanceStatus.ABSENT
            'H' -> AttendanceStatus.HOLIDAY
            'L' -> AttendanceStatus.LEAVE
            else -> AttendanceStatus.UPCOMING
        }
    }

    fun getTodayCountSummary(todayDateStr: String = ""): TodayCountSummary {
        val timeline = getTodayAttendanceTimeline(todayDateStr)
        if (timeline.isEmpty()) return TodayCountSummary(0, 0, 0, 0)

        val presents = timeline.count { it == AttendanceStatus.PRESENT }
        val absents = timeline.count { it == AttendanceStatus.ABSENT }
        val pending = timeline.count { it == AttendanceStatus.UPCOMING }
        return TodayCountSummary(presents, absents, pending, timeline.size)
    }

    fun getTodaySummary(todayDateStr: String = ""): Pair<Int, Int> {
        val countSummary = getTodayCountSummary(todayDateStr)
        return countSummary.presents to countSummary.absents
    }

    fun getTodayStatusString(todayDateStr: String = ""): String {
        val timeline = getTodayAttendanceTimeline(todayDateStr)
        return timeline.joinToString("") { status ->
            when (status) {
                AttendanceStatus.PRESENT -> "P"
                AttendanceStatus.ABSENT -> "A"
                AttendanceStatus.HOLIDAY -> "H"
                AttendanceStatus.LEAVE -> "L"
                AttendanceStatus.UPCOMING -> "-"
            }
        }
    }

    private fun isSubjectMatch(cellText: String, targetSubject: String): Boolean {
        val cleanCell = cellText.lowercase().trim()
        val cleanTarget = targetSubject.lowercase().trim()
        if (cleanCell.isEmpty() || cleanTarget.isEmpty()) return false

        if (cleanCell.contains(cleanTarget) || cleanTarget.contains(cleanCell)) return true

        val cellWords = cleanCell.split(" ", "-", "_", "(", ")", ".").filter { it.length > 2 }
        val targetWords = cleanTarget.split(" ", "-", "_", "(", ")", ".").filter { it.length > 2 }

        return cellWords.any { word -> targetWords.contains(word) }
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



    fun toWidgetState(targetThreshold: Double = 75.0, lastUpdatedMillis: Long = System.currentTimeMillis()): AttendanceWidgetState {
        val percentage = overallPercentage
        val status = when {
            percentage >= targetThreshold -> OverallAttendanceStatus.SAFE
            else -> OverallAttendanceStatus.CRITICAL
        }

        val attended = total_info?.total_attended ?: 0
        val held = total_info?.total_held ?: 0

        val targetFactor = targetThreshold / 100.0
        val calculatedCanSkip = if (held > 0 && percentage >= targetThreshold) {
            val maxSkip = kotlin.math.floor((attended - targetFactor * held) / targetFactor).toInt()
            maxOf(0, maxSkip)
        } else 0
        val periodsCanSkip = if (targetThreshold == 75.0) (total_info?.hours_can_skip ?: calculatedCanSkip) else calculatedCanSkip

        val calculatedNeedToAttend = if (held > 0 && percentage < targetThreshold) {
            val divisor = 1.0 - targetFactor
            val minAttend = if (divisor > 0.0) {
                kotlin.math.ceil((targetFactor * held - attended) / divisor).toInt()
            } else 0
            maxOf(0, minAttend)
        } else 0
        val periodsNeedToAttend = if (targetThreshold == 75.0) (total_info?.additional_hours_needed ?: calculatedNeedToAttend) else calculatedNeedToAttend

        val todayDate = SimpleDateFormat("dd/MM", Locale.US).format(Date())
        val timeline = getTodayAttendanceTimeline(todayDate)

        val lastUpdatedStr = if (lastUpdatedMillis > 0) {
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(lastUpdatedMillis))
        } else {
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        }

        val diff = percentage - targetThreshold
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

    fun calculateSimulation(
        leaveDates: Set<Long>,
        holidayDates: Set<Long>,
        targetThreshold: Double = 75.0
    ): SimulationResult {
        val curAttended = total_info?.total_attended ?: 0
        val curHeld = total_info?.total_held ?: 0
        val curPct = overallPercentage

        var additionalLeaveHeld = 0

        leaveDates.forEach { millis ->
            val date = Date(millis)
            val dayOfWeek = SimpleDateFormat("EEEE", Locale.US).format(date)
            val dayTt = timetable?.firstOrNull { it.day.equals(dayOfWeek, ignoreCase = true) }
            val periodCount = dayTt?.classes?.size ?: 6
            additionalLeaveHeld += periodCount
        }

        val simAttended = curAttended
        val simHeld = curHeld + additionalLeaveHeld
        val simPct = if (simHeld > 0) (simAttended.toDouble() / simHeld.toDouble()) * 100.0 else curPct

        val targetFactor = targetThreshold / 100.0

        val origCanSkip = if (curHeld > 0 && curPct >= targetThreshold) {
            maxOf(0, kotlin.math.floor((curAttended - targetFactor * curHeld) / targetFactor).toInt())
        } else 0

        val simCanSkip = if (simHeld > 0 && simPct >= targetThreshold) {
            maxOf(0, kotlin.math.floor((simAttended - targetFactor * simHeld) / targetFactor).toInt())
        } else 0

        val origNeed = if (curHeld > 0 && curPct < targetThreshold) {
            val div = 1.0 - targetFactor
            if (div > 0) maxOf(0, kotlin.math.ceil((targetFactor * curHeld - curAttended) / div).toInt()) else 0
        } else 0

        val simNeed = if (simHeld > 0 && simPct < targetThreshold) {
            val div = 1.0 - targetFactor
            if (div > 0) maxOf(0, kotlin.math.ceil((targetFactor * simHeld - simAttended) / div).toInt()) else 0
        } else 0

        return SimulationResult(
            originalPercentage = curPct,
            simulatedPercentage = simPct,
            originalAttended = curAttended,
            simulatedAttended = simAttended,
            originalHeld = curHeld,
            simulatedHeld = simHeld,
            originalCanSkip = origCanSkip,
            simulatedCanSkip = simCanSkip,
            originalNeedToAttend = origNeed,
            simulatedNeedToAttend = simNeed,
            addedLeavesCount = leaveDates.size,
            addedHolidaysCount = holidayDates.size
        )
    }
}

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

