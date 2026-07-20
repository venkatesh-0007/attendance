package com.example.attendance

import com.example.attendance.data.model.AttendanceResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AttendanceSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun testAttendanceResponseDeserialization() {
        val payload = """
            {
                "overall_attendance": 80.92,
                "attended_classes": 123,
                "held_classes": 152,
                "todays_attendance": "PPPPPPP",
                "subject_wise_attendance": [
                    {
                        "subject": "Mathematics IV",
                        "code": "MATH301",
                        "percentage": 85.2,
                        "attended": 30,
                        "held": 35
                    }
                ],
                "timetable": [
                    {
                        "day": "Monday",
                        "classes": [
                          {
                            "subject": "Mathematics IV",
                            "time": "09:00 AM - 10:00 AM",
                            "room": "L-201",
                            "faculty": "Dr. Sarah Connor"
                          }
                        ]
                    }
                ],
                "faculty_information": [
                    {
                        "name": "Dr. Sarah Connor",
                        "subject": "Mathematics IV",
                        "email": "sarah.connor@university.edu",
                        "phone": "+1-555-0199"
                      }
                ],
                "attendance_history": [
                    {
                        "subject": "Mathematics IV",
                        "date": "2026-07-15",
                        "status": "Present"
                    }
                ]
            }
        """.trimIndent()

        val response = json.decodeFromString<AttendanceResponse>(payload)

        assertNotNull(response)
        assertEquals(80.92, response.overall_attendance!!, 0.001)
        assertEquals(123, response.attended_classes)
        assertEquals(152, response.held_classes)
        assertEquals("PPPPPPP", response.todays_attendance)
        
        // Subject verification
        assertEquals(1, response.subject_wise_attendance?.size)
        val subject = response.subject_wise_attendance!![0]
        assertEquals("Mathematics IV", subject.subject)
        assertEquals("MATH301", subject.code)
        assertEquals(85.2, subject.percentage, 0.001)
        
        // Timetable verification
        assertEquals(1, response.timetable?.size)
        val timetableDay = response.timetable!![0]
        assertEquals("Monday", timetableDay.day)
        assertEquals(1, timetableDay.classes?.size)
        val timetableClass = timetableDay.classes!![0]
        assertEquals("Mathematics IV", timetableClass.subject)
        assertEquals("09:00 AM - 10:00 AM", timetableClass.time)
        assertEquals("L-201", timetableClass.room)
        assertEquals("Dr. Sarah Connor", timetableClass.faculty)

        // Faculty verification
        assertEquals(1, response.faculty_information?.size)
        val faculty = response.faculty_information!![0]
        assertEquals("Dr. Sarah Connor", faculty.name)
        assertEquals("sarah.connor@university.edu", faculty.email)

        // History verification
        assertEquals(1, response.attendance_history?.size)
        val history = response.attendance_history!![0]
        assertEquals("Mathematics IV", history.subject)
        assertEquals("2026-07-15", history.date)
        assertEquals("Present", history.status)
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
}
