package com.example.attendance

import com.example.attendance.data.model.AttendanceResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.Calendar

class AttendanceSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun testAttendanceResponseDeserialization() {
        val payload = """
            {
                "roll_number": ":24L31A4461",
                "total_info": {
                    "total_attended": 137,
                    "total_held": 179,
                    "total_percentage": "76.54%",
                    "hours_can_skip": 3,
                    "additional_hours_needed": 0
                },
                "subjectwise_summary": [
                    {
                        "subject_name": "CNA \u0026 P",
                        "attended": 12,
                        "held": 16,
                        "percentage": "75.0%",
                        "hours_can_skip": 0,
                        "hours_needed": 0
                    }
                ],
                "attendance_table": {
                    "headers": ["Sl.No", "Subject"],
                    "rows": [["1", "CNA \u0026 P"]]
                }
            }
        """.trimIndent()

        val response = json.decodeFromString<AttendanceResponse>(payload)

        assertNotNull(response)
        assertEquals(":24L31A4461", response.roll_number)
        assertNotNull(response.total_info)
        assertEquals(137, response.total_info?.total_attended)
        assertEquals(179, response.total_info?.total_held)
        assertEquals("76.54%", response.total_info?.total_percentage)
        assertEquals(76.54, response.overallPercentage, 0.001)
        
        // Subject verification
        assertEquals(1, response.subjectwise_summary?.size)
        val subject = response.subjectwise_summary!![0]
        assertEquals("CNA \u0026 P", subject.subject_name)
        assertEquals("75.0%", subject.percentage)
        assertEquals(75.0, subject.percentageDouble, 0.001)
        
        // Table verification
        assertNotNull(response.attendance_table)
        assertEquals(2, response.attendance_table?.headers?.size)
        assertEquals(1, response.attendance_table?.rows?.size)
    }

    @Test
    fun testErrorResponseDeserialization() {
        val errorPayload = """
            {
                "error": "Attendance table not found"
            }
        """.trimIndent()

        val response = json.decodeFromString<AttendanceResponse>(errorPayload)
        assertNotNull(response)
        assertEquals("Attendance table not found", response.error)
    }

    @Test
    fun testAttendancePercentageParsing() {
        val payload = """
            {
                "total_info": {
                    "total_attended": 10,
                    "total_held": 20,
                    "total_percentage": "50.00%"
                }
            }
        """.trimIndent()
        val response = json.decodeFromString<AttendanceResponse>(payload)
        assertEquals(50.0, response.overallPercentage, 0.001)
    }

    @Test
    fun testTodaySummaryCalculation() {
        val payload = """
            {
                "attendance_table": {
                    "headers": ["Sl.No", "Subject", "23/07"],
                    "rows": [
                        ["1", "Math", "P"],
                        ["2", "Physics", "A"],
                        ["3", "Chemistry", "PP"]
                    ]
                }
            }
        """.trimIndent()
        val response = json.decodeFromString<AttendanceResponse>(payload)
        val summary = response.getTodaySummary("23/07")
        assertEquals(3, summary.first) // 1 in Math + 2 in Chemistry
        assertEquals(1, summary.second) // 1 in Physics
    }

    @Test
    fun testTodayColumnIndexWithDateHeaders() {
        val payload = """
            {
                "attendance_table": {
                    "headers": ["Sl.No", "Subject", "22/06", "24/07", "25/07", "27/07", "Atted/Held", "%"],
                    "rows": [
                        ["1", "Math", "P", "A", "H", "L", "17/19", "89.47%"]
                    ]
                }
            }
        """.trimIndent()
        val response = json.decodeFromString<AttendanceResponse>(payload)
        
        // When today date matches 27/07
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, Calendar.JULY)
        calendar.set(Calendar.DAY_OF_MONTH, 27)
        
        val index = response.getTodayColumnIndex("27/07")
        // It should match 27/07, which is index 5
        assertEquals(5, index)
        
        // If today matches 25/07
        val index2 = response.getTodayColumnIndex("25/07")
        assertEquals(4, index2)

        // Non-existent date: should return -1
        val fallbackIndex = response.getTodayColumnIndex("28/07")
        assertEquals(-1, fallbackIndex)
    }
}
