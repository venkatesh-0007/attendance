package com.attendance.app.data.model

import kotlinx.serialization.Serializable

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
