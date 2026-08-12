package com.attendance.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Serializable
data class AttendanceResponse(
    @SerialName("student_name") val studentName: String? = null,
    @SerialName("roll_number") val rollNumber: String? = null,
    @SerialName("total_info") val totalInfo: TotalInfo? = null,
    @SerialName("subjectwise_summary") val subjectwiseSummary: List<SubjectSummary>? = null,
    @SerialName("attendance_table") val attendanceTable: AttendanceTable? = null,
    val timetable: List<TimetableDay>? = null,
    @SerialName("faculty_information") val facultyInformation: List<FacultyInfo>? = null,
    val error: String? = null
) {
    val overallPercentage: Double
        get() = totalInfo?.totalPercentage?.replace("%", "")?.toDoubleOrNull() ?: 0.0

    fun getCleanTable(): CleanTable? {
        val table = attendanceTable ?: return null
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

        val columnIndex = getTodayColumnIndex(todayDateStr)
        if (columnIndex == -1) return emptyList()

        val timeline = mutableListOf<AttendanceStatus>()

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

    fun toWidgetState(
        targetThreshold: Double = 75.0,
        lastUpdatedMillis: Long = System.currentTimeMillis(),
        customName: String? = null
    ): AttendanceWidgetState {
        val percentage = overallPercentage
        val status = when {
            percentage >= targetThreshold -> OverallAttendanceStatus.SAFE
            else -> OverallAttendanceStatus.CRITICAL
        }

        val attended = totalInfo?.totalAttended ?: 0
        val held = totalInfo?.totalHeld ?: 0

        val targetFactor = targetThreshold / 100.0
        val calculatedCanSkip = if (held > 0 && percentage >= targetThreshold) {
            val maxSkip = kotlin.math.floor((attended - targetFactor * held) / targetFactor).toInt()
            maxOf(0, maxSkip)
        } else 0
        val periodsCanSkip = if (targetThreshold == 75.0) (totalInfo?.hoursCanSkip ?: calculatedCanSkip) else calculatedCanSkip

        val calculatedNeedToAttend = if (held > 0 && percentage < targetThreshold) {
            val divisor = 1.0 - targetFactor
            val minAttend = if (divisor > 0.0) {
                kotlin.math.ceil((targetFactor * held - attended) / divisor).toInt()
            } else 0
            maxOf(0, minAttend)
        } else 0
        val periodsNeedToAttend = if (targetThreshold == 75.0) (totalInfo?.additionalHoursNeeded ?: calculatedNeedToAttend) else calculatedNeedToAttend

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

        val nameToUse = when {
            !customName.isNullOrBlank() -> customName
            !studentName.isNullOrBlank() -> studentName
            !rollNumber.isNullOrBlank() -> rollNumber
            else -> "Account"
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
            studentName = nameToUse,
            targetMargin = targetMargin
        )
    }

    fun calculateSimulation(
        leaveDates: Set<Long>,
        holidayDates: Set<Long>,
        targetThreshold: Double = 75.0
    ): SimulationResult {
        val curAttended = totalInfo?.totalAttended ?: 0
        val curHeld = totalInfo?.totalHeld ?: 0
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
