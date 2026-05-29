package com.example.ui.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.data.Attendance
import com.example.data.Employee
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object DocumentExporter {

    fun exportToCSV(
        context: Context,
        attendances: List<Attendance>,
        employees: List<Employee>
    ): File? {
        return try {
            val employeeMap = employees.associateBy { it.id }
            val csvBuilder = StringBuilder()
            
            // CSV Header
            csvBuilder.append("No;ID Pegawai;Nama Karyawan;Departemen;Tanggal;Jam Masuk;Jam Pulang;Total Jam Kerja;Alasan Pulang Awal\n")
            
            attendances.forEachIndexed { index, att ->
                val emp = employeeMap[att.employeeId]
                val code = emp?.employeeCode ?: "-"
                val name = att.employeeName
                val dept = emp?.department ?: "-"
                val date = att.date
                val cin = att.clockInTime ?: "-"
                val cout = att.clockOutTime ?: "-"
                val hrs = String.format(Locale.US, "%.2f", att.workingHours)
                val reason = att.earlyReason?.replace(";", ",") ?: ""
                
                csvBuilder.append("${index + 1};$code;$name;$dept;$date;$cin;$cout;$hrs;$reason\n")
            }

            val fileName = "Laporan_Absensi_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { out ->
                out.write(csvBuilder.toString().toByteArray())
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportToPDF(
        context: Context,
        attendances: List<Attendance>,
        employees: List<Employee>,
        monthName: String,
        year: String
    ): File? {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint().apply {
            color = Color.parseColor("#1B2A4A") // Deep slate blue
            textSize = 16f
            isFakeBoldText = true
        }
        val headerPaint = Paint().apply {
            color = Color.WHITE
            textSize = 10f
            isFakeBoldText = true
        }
        val cellPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9f
        }
        val gridPaint = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
        }

        // A4 Paper format (595 x 842 points)
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        try {
            var yPosition = 40f
            
            // Header Title
            canvas.drawText("LAPORAN BULANAN KEHADIRAN KARYAWAN", 30f, yPosition, titlePaint)
            yPosition += 20f
            
            val subtitlePaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 11f
            }
            canvas.drawText("Periode Laporan: $monthName $year", 30f, yPosition, subtitlePaint)
            yPosition += 15f
            
            val infoPaint = Paint().apply {
                color = Color.GRAY
                textSize = 8f
            }
            val formatter = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
            canvas.drawText("Diunduh Pada: ${formatter.format(Date())}", 30f, yPosition, infoPaint)
            yPosition += 30f

            // Table Header Background
            val headerBgPaint = Paint().apply {
                color = Color.parseColor("#294F8A") // Slate header accent
                style = Paint.Style.FILL
            }
            canvas.drawRect(30f, yPosition - 15f, 565f, yPosition + 10f, headerBgPaint)

            // Header labels
            val xNo = 35f
            val xNik = 65f
            val xNama = 135f
            val xTanggal = 270f
            val xMasuk = 330f
            val xPulang = 385f
            val xJam = 440f
            val xAlasan = 485f

            canvas.drawText("No", xNo, yPosition, headerPaint)
            canvas.drawText("NIK", xNik, yPosition, headerPaint)
            canvas.drawText("Nama", xNama, yPosition, headerPaint)
            canvas.drawText("Tanggal", xTanggal, yPosition, headerPaint)
            canvas.drawText("Masuk", xMasuk, yPosition, headerPaint)
            canvas.drawText("Pulang", xPulang, yPosition, headerPaint)
            canvas.drawText("Jam", xJam, yPosition, headerPaint)
            canvas.drawText("Alasan", xAlasan, yPosition, headerPaint)

            yPosition += 25f

            val employeeMap = employees.associateBy { it.id }

            attendances.forEachIndexed { idx, att ->
                if (yPosition > 800f) {
                    // Create a new page
                    pdfDocument.finishPage(page)
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    yPosition = 50f
                    
                    // Draw Header on new page
                    canvas.drawRect(30f, yPosition - 15f, 565f, yPosition + 10f, headerBgPaint)
                    canvas.drawText("No", xNo, yPosition, headerPaint)
                    canvas.drawText("NIK", xNik, yPosition, headerPaint)
                    canvas.drawText("Nama", xNama, yPosition, headerPaint)
                    canvas.drawText("Tanggal", xTanggal, yPosition, headerPaint)
                    canvas.drawText("Masuk", xMasuk, yPosition, headerPaint)
                    canvas.drawText("Pulang", xPulang, yPosition, headerPaint)
                    canvas.drawText("Jam", xJam, yPosition, headerPaint)
                    canvas.drawText("Alasan", xAlasan, yPosition, headerPaint)
                    
                    yPosition += 25f
                }

                val rowNum = (idx + 1).toString()
                val emp = employeeMap[att.employeeId]
                val nik = emp?.employeeCode ?: "-"
                val name = if (att.employeeName.length > 22) att.employeeName.take(20) + ".." else att.employeeName
                val date = att.date
                val masuk = att.clockInTime ?: "-"
                val pulang = att.clockOutTime ?: "-"
                val jam = String.format(Locale.US, "%.1f j", att.workingHours)
                val alasan = att.earlyReason ?: ""
                val shortAlasan = if (alasan.length > 14) alasan.take(12) + ".." else alasan

                // Row content
                canvas.drawText(rowNum, xNo, yPosition, cellPaint)
                canvas.drawText(nik, xNik, yPosition, cellPaint)
                canvas.drawText(name, xNama, yPosition, cellPaint)
                canvas.drawText(date, xTanggal, yPosition, cellPaint)
                canvas.drawText(masuk, xMasuk, yPosition, cellPaint)
                canvas.drawText(pulang, xPulang, yPosition, cellPaint)
                canvas.drawText(jam, xJam, yPosition, cellPaint)
                canvas.drawText(shortAlasan, xAlasan, yPosition, cellPaint)

                // Row Grid Line
                canvas.drawLine(30f, yPosition + 5f, 565f, yPosition + 5f, gridPaint)

                yPosition += 20f
            }

            pdfDocument.finishPage(page)
            val fileName = "Laporan_Absensi_Bulanan_${monthName}_${year}.pdf"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            pdfDocument.close()
        }
    }
}
