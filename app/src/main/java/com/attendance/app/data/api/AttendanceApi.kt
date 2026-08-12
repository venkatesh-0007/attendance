package com.attendance.app.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface AttendanceApi {

    @GET("attendance")
    suspend fun getAttendance(
        @Query("student_id") studentId: String,
        @Query("password") password: String
    ): String
}
