package com.example.attendance.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.attendance.data.model.AttendanceResponse
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    fun generateAndSharePdf(context: Context, attendance: AttendanceResponse) {
        val file = generatePdfFile(context, attendance) ?: return
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Attendance Report - ${attendance.student_name ?: "Student"}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share Attendance Report PDF")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    private fun generatePdfFile(context: Context, attendance: AttendanceResponse): File? {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size in points
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }
            val textPaint = Paint().apply { isAntiAlias = true }

            // 1. Header Banner
            paint.color = Color.parseColor("#3F51B5") // Indigo Primary
            canvas.drawRect(0f, 0f, 595f, 90f, paint)

            textPaint.color = Color.WHITE
            textPaint.textSize = 22f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("ATTENDANCE SUMMARY REPORT", 30f, 42f, textPaint)

            textPaint.textSize = 12f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val timestamp = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            canvas.drawText("Generated on: $timestamp", 30f, 68f, textPaint)

            // 2. Student Info Card
            paint.color = Color.parseColor("#F5F5F7")
            canvas.drawRoundRect(30f, 110f, 565f, 180f, 12f, 12f, paint)

            textPaint.color = Color.BLACK
            textPaint.textSize = 14f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Student Name: ${attendance.student_name ?: "N/A"}", 50f, 140f, textPaint)

            textPaint.textSize = 12f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.color = Color.DKGRAY
            canvas.drawText("Roll Number / ID: ${attendance.roll_number ?: "N/A"}", 50f, 162f, textPaint)

            // 3. Overall Statistics Section
            paint.color = Color.parseColor("#E8EAF6")
            canvas.drawRoundRect(30f, 195f, 565f, 265f, 12f, 12f, paint)

            textPaint.color = Color.parseColor("#1A237E")
            textPaint.textSize = 16f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Overall Attendance: ${attendance.total_info?.total_percentage ?: "0.0%"}", 50f, 230f, textPaint)

            textPaint.textSize = 12f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.color = Color.DKGRAY
            canvas.drawText("Total Attended: ${attendance.total_info?.total_attended ?: 0} / Total Held: ${attendance.total_info?.total_held ?: 0}", 50f, 250f, textPaint)

            // 4. Subject Breakdown Table Header
            var startY = 300f
            textPaint.color = Color.BLACK
            textPaint.textSize = 15f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Subject Breakdown", 30f, startY, textPaint)

            startY += 15f
            paint.color = Color.parseColor("#3F51B5")
            canvas.drawRect(30f, startY, 565f, startY + 28f, paint)

            textPaint.color = Color.WHITE
            textPaint.textSize = 11f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Subject Name", 40f, startY + 18f, textPaint)
            canvas.drawText("Attended", 290f, startY + 18f, textPaint)
            canvas.drawText("Held", 380f, startY + 18f, textPaint)
            canvas.drawText("Percentage", 470f, startY + 18f, textPaint)

            // Table Rows
            startY += 28f
            val subjects = attendance.subjectwise_summary ?: emptyList()
            var isAltRow = false

            subjects.forEach { sub ->
                if (startY > 780f) return@forEach // Fit single page

                paint.color = if (isAltRow) Color.parseColor("#F9F9FB") else Color.WHITE
                canvas.drawRect(30f, startY, 565f, startY + 24f, paint)

                textPaint.color = Color.parseColor("#212121")
                textPaint.textSize = 10f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

                val nameStr = if (sub.subject_name.length > 32) sub.subject_name.take(30) + ".." else sub.subject_name
                canvas.drawText(nameStr, 40f, startY + 16f, textPaint)
                canvas.drawText("${sub.attended}", 290f, startY + 16f, textPaint)
                canvas.drawText("${sub.held}", 380f, startY + 16f, textPaint)

                val pctColor = if (sub.percentageDouble >= 75.0) Color.parseColor("#2E7D32") else Color.parseColor("#C62828")
                textPaint.color = pctColor
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(sub.percentage, 470f, startY + 16f, textPaint)

                startY += 24f
                isAltRow = !isAltRow
            }

            // 5. Footer
            textPaint.color = Color.GRAY
            textPaint.textSize = 9f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas.drawText("Generated by Student Attendance Tracker App", 30f, 820f, textPaint)

            pdfDocument.finishPage(page)

            // Save PDF to cache directory
            val pdfDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val file = File(pdfDir, "Attendance_Report_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()

            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
