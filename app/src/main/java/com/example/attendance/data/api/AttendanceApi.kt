package com.example.attendance.data.api

import com.example.attendance.data.model.AttendanceResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface AttendanceApi {

    @GET("attendance")
    suspend fun getAttendance(
        @Query("student_id") studentId: String,
        @Query("password") password: String
    ): AttendanceResponse
}
