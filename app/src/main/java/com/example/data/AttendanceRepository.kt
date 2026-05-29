package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlin.math.*

class AttendanceRepository(private val db: AppDatabase) {
    private val employeeDao = db.employeeDao()
    private val attendanceDao = db.attendanceDao()
    private val officeSettingDao = db.officeSettingDao()

    val allEmployees: Flow<List<Employee>> = employeeDao.getAllEmployees()
    val allAttendance: Flow<List<Attendance>> = attendanceDao.getAllAttendance()
    val officeSettings: Flow<OfficeSetting?> = officeSettingDao.getSettings()

    fun getAttendanceForDate(date: String): Flow<List<Attendance>> {
        return attendanceDao.getAttendanceForDate(date)
    }

    fun getAttendanceForEmployee(employeeId: Int): Flow<List<Attendance>> {
        return attendanceDao.getAttendanceForEmployee(employeeId)
    }

    suspend fun getAttendanceForEmployeeAndDate(employeeId: Int, date: String): Attendance? {
        return attendanceDao.getAttendanceForEmployeeAndDate(employeeId, date)
    }

    suspend fun insertEmployee(employee: Employee): Long {
        return employeeDao.insertEmployee(employee)
    }

    suspend fun updateEmployee(employee: Employee) {
        employeeDao.updateEmployee(employee)
    }

    suspend fun deleteEmployee(employee: Employee) {
        employeeDao.deleteEmployee(employee)
        // Optionally delete their attendance logs or keep them
    }

    suspend fun saveOfficeSettings(setting: OfficeSetting) {
        officeSettingDao.insertOrUpdate(setting)
    }

    suspend fun getSettingsDirect(): OfficeSetting {
        return officeSettingDao.getSettingsDirect() ?: OfficeSetting().also {
            officeSettingDao.insertOrUpdate(it)
        }
    }

    // Helper to calculate distance using Haversine formula
    fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371e3 // Earth radius in meters
        val phi1 = lat1 * PI / 180
        val phi2 = lat2 * PI / 180
        val deltaPhi = (lat2 - lat1) * PI / 180
        val deltaLambda = (lon2 - lon1) * PI / 180

        val a = sin(deltaPhi / 2).pow(2) +
                cos(phi1) * cos(phi2) *
                sin(deltaLambda / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return r * c
    }

    suspend fun clockIn(
        employeeId: Int,
        employeeName: String,
        date: String,
        time: String,
        lat: Double,
        lng: Double
    ): Boolean {
        val settings = getSettingsDirect()
        val distance = calculateDistanceMeters(lat, lng, settings.officeLat, settings.officeLng)
        val inRange = distance <= settings.allowedRadiusMeters

        val existing = attendanceDao.getAttendanceForEmployeeAndDate(employeeId, date)
        if (existing == null) {
            val attendance = Attendance(
                employeeId = employeeId,
                employeeName = employeeName,
                date = date,
                clockInTime = time,
                latitude = lat,
                longitude = lng,
                isInCorrectLocation = inRange,
                workingHours = 0.0
            )
            attendanceDao.insertAttendance(attendance)
            return true
        } else {
            // Already clocked in for today
            return false
        }
    }

    suspend fun clockOut(
        employeeId: Int,
        date: String,
        time: String,
        reason: String?
    ): Boolean {
        val existing = attendanceDao.getAttendanceForEmployeeAndDate(employeeId, date) ?: return false
        
        val startHoursAndMinutes = existing.clockInTime?.split(":")?.mapNotNull { it.toIntOrNull() } ?: listOf(8, 0)
        val endHoursAndMinutes = time.split(":").mapNotNull { it.toIntOrNull() } ?: listOf(17, 0)
        
        val startVal = startHoursAndMinutes[0] + startHoursAndMinutes[1] / 60.0
        val endVal = endHoursAndMinutes[0] + endHoursAndMinutes[1] / 60.0
        val totalHours = max(0.0, endVal - startVal)

        val updated = existing.copy(
            clockOutTime = time,
            earlyReason = reason,
            workingHours = totalHours
        )
        attendanceDao.insertAttendance(updated)
        return true
    }

    suspend fun seedMockData() {
        // Only seed if empty
        val list = employeeDao.getAllEmployeesList()
        if (list.isEmpty()) {
            val emp1 = Employee(name = "Budi Santoso", employeeCode = "PEG001", department = "IT Support", email = "budi@kantor.com", phone = "08123456781")
            val emp2 = Employee(name = "Sari Wijaya", employeeCode = "PEG002", department = "Human Resources", email = "sari@kantor.com", phone = "08123456782")
            val emp3 = Employee(name = "Ronaldo Siregar", employeeCode = "PEG003", department = "Finance", email = "ronaldo@kantor.com", phone = "08123456783")
            val emp4 = Employee(name = "Dewi Lesmana", employeeCode = "PEG004", department = "Marketing", email = "dewi@kantor.com", phone = "08123456784")

            val e1 = employeeDao.insertEmployee(emp1).toInt()
            val e2 = employeeDao.insertEmployee(emp2).toInt()
            val e3 = employeeDao.insertEmployee(emp3).toInt()
            val e4 = employeeDao.insertEmployee(emp4).toInt()

            officeSettingDao.insertOrUpdate(OfficeSetting())

            // Create some sample history for May 23, 2026
            val att1 = Attendance(
                employeeId = e1, employeeName = "Budi Santoso", date = "2026-05-23",
                clockInTime = "07:54", clockOutTime = "17:02", workingHours = 9.13, isInCorrectLocation = true
            )
            val att2 = Attendance(
                employeeId = e2, employeeName = "Sari Wijaya", date = "2026-05-23",
                clockInTime = "08:15", clockOutTime = "17:10", workingHours = 8.91, isInCorrectLocation = true
            )
            val att3 = Attendance(
                employeeId = e3, employeeName = "Ronaldo Siregar", date = "2026-05-23",
                clockInTime = "07:45", clockOutTime = "16:15", workingHours = 8.50, isInCorrectLocation = true,
                earlyReason = "Ada keperluan mendadak ke bank"
            )

            attendanceDao.insertAttendance(att1)
            attendanceDao.insertAttendance(att2)
            attendanceDao.insertAttendance(att3)
        }
    }
}
