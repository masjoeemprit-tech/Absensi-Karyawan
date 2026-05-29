package com.example.ui

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repository = AttendanceRepository(db)

    // Raw Flows
    val employees = repository.allEmployees.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val attendance = repository.allAttendance.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val settings = repository.officeSettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OfficeSetting()
    )

    // Simulation states (allowing interactive testing inside the Stream Emulator)
    private val _simulatedLat = MutableStateFlow(-6.200000)
    val simulatedLat = _simulatedLat.asStateFlow()

    private val _simulatedLng = MutableStateFlow(106.816666)
    val simulatedLng = _simulatedLng.asStateFlow()

    private val _simulatedHour = MutableStateFlow(8) // Default 08 AM
    val simulatedHour = _simulatedHour.asStateFlow()

    private val _simulatedMinute = MutableStateFlow(0) // Default 00
    val simulatedMinute = _simulatedMinute.asStateFlow()

    // Active session employee (the employee currently clocking in or out)
    private val _activeEmployee = MutableStateFlow<Employee?>(null)
    val activeEmployee = _activeEmployee.asStateFlow()

    // Monthly Report Filters
    private val _reportMonth = MutableStateFlow(5) // May (1-indexed, i.e., 5)
    val reportMonth = _reportMonth.asStateFlow()

    private val _reportYear = MutableStateFlow(2026)
    val reportYear = _reportYear.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedMockData()
            // Set first employee as active by default for employee mode convenience
            employees.filter { it.isNotEmpty() }.collect { list ->
                if (_activeEmployee.value == null) {
                    _activeEmployee.value = list.firstOrNull()
                }
            }
        }
    }

    fun updateSimulatedLocation(lat: Double, lng: Double) {
        _simulatedLat.value = lat
        _simulatedLng.value = lng
    }

    fun updateSimulatedTime(hour: Int, minute: Int) {
        _simulatedHour.value = hour
        _simulatedMinute.value = minute
    }

    fun setActiveEmployee(employee: Employee) {
        _activeEmployee.value = employee
    }

    fun setReportFilter(month: Int, year: Int) {
        _reportMonth.value = month
        _reportYear.value = year
    }

    // CRUD - Employee
    fun addEmployee(name: String, code: String, dept: String, email: String, phone: String) {
        viewModelScope.launch {
            repository.insertEmployee(Employee(
                name = name,
                employeeCode = code,
                department = dept,
                email = email,
                phone = phone
            ))
        }
    }

    fun updateEmployee(id: Int, name: String, code: String, dept: String, email: String, phone: String) {
        viewModelScope.launch {
            repository.updateEmployee(Employee(
                id = id,
                name = name,
                employeeCode = code,
                department = dept,
                email = email,
                phone = phone
            ))
        }
    }

    fun deleteEmployee(employee: Employee) {
        viewModelScope.launch {
            repository.deleteEmployee(employee)
            // If the deleted employee was active, switch active employee
            if (_activeEmployee.value?.id == employee.id) {
                _activeEmployee.value = employees.value.firstOrNull { it.id != employee.id }
            }
        }
    }

    // Config - Office settings
    fun updateOfficeSettings(
        officeName: String,
        lat: Double,
        lng: Double,
        radius: Double,
        jamMasuk: String,
        jamPulang: String
    ) {
        viewModelScope.launch {
            repository.saveOfficeSettings(OfficeSetting(
                id = 1,
                officeName = officeName,
                officeLat = lat,
                officeLng = lng,
                allowedRadiusMeters = radius,
                jamMasuk = jamMasuk,
                jamPulang = jamPulang
            ))
        }
    }

    // Geofencing verification helper
    fun isWithinDistance(): Boolean {
        val st = settings.value ?: OfficeSetting()
        val dist = repository.calculateDistanceMeters(
            _simulatedLat.value, _simulatedLng.value,
            st.officeLat, st.officeLng
        )
        return dist <= st.allowedRadiusMeters
    }

    fun getDistanceToOffice(): Double {
        val st = settings.value ?: OfficeSetting()
        return repository.calculateDistanceMeters(
            _simulatedLat.value, _simulatedLng.value,
            st.officeLat, st.officeLng
        )
    }

    // Current formatted simulated time string (HH:mm)
    fun getSimulatedTimeString(): String {
        return String.format(Locale.getDefault(), "%02d:%02d", _simulatedHour.value, _simulatedMinute.value)
    }

    // Attendance Operations
    fun performClockIn(onResult: (Boolean, String) -> Unit) {
        val emp = _activeEmployee.value
        if (emp == null) {
            onResult(false, "Silakan pilih karyawan terlebih dahulu.")
            return
        }

        if (!isWithinDistance()) {
            val dist = getDistanceToOffice()
            onResult(false, "Gagal Absen! Jarak Anda %.1f meter (Maksimal %.0f meter).".format(dist, settings.value?.allowedRadiusMeters ?: 500f))
            return
        }

        viewModelScope.launch {
            val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val timeString = getSimulatedTimeString()
            val success = repository.clockIn(
                employeeId = emp.id,
                employeeName = emp.name,
                date = todayDate,
                time = timeString,
                lat = _simulatedLat.value,
                lng = _simulatedLng.value
            )
            if (success) {
                onResult(true, "Absen Masuk Berhasil Jam $timeString")
            } else {
                onResult(false, "Anda sudah melakukan absen masuk hari ini.")
            }
        }
    }

    // Verify if early clock-out reason is required
    fun isClockOutEarly(): Boolean {
        val st = settings.value ?: OfficeSetting()
        val currentSimHours = _simulatedHour.value + _simulatedMinute.value / 60.0
        val pulangParts = st.jamPulang.split(":").mapNotNull { it.toIntOrNull() }
        val pulangHours = if (pulangParts.size >= 2) pulangParts[0] + pulangParts[1] / 60.0 else 17.0
        return currentSimHours < pulangHours
    }

    fun performClockOut(reason: String?, onResult: (Boolean, String) -> Unit) {
        val emp = _activeEmployee.value
        if (emp == null) {
            onResult(false, "Silakan pilih karyawan terlebih dahulu.")
            return
        }

        if (isClockOutEarly() && reason.isNullOrBlank()) {
            onResult(false, "Anda pulang awal, harus menyertakan alasan.")
            return
        }

        viewModelScope.launch {
            val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val timeString = getSimulatedTimeString()
            
            // Check if clocked in first
            val existing = repository.getAttendanceForEmployeeAndDate(emp.id, todayDate)
            if (existing == null) {
                onResult(false, "Gagal: Anda belum melakukan absen MASUK hari ini.")
                return@launch
            }
            if (existing.clockOutTime != null) {
                onResult(false, "Gagal: Anda sudah melakukan absen PULANG hari ini.")
                return@launch
            }

            val success = repository.clockOut(
                employeeId = emp.id,
                date = todayDate,
                time = timeString,
                reason = if (isClockOutEarly()) reason else null
            )
            if (success) {
                onResult(true, "Absen Pulang Berhasil Jam $timeString")
            } else {
                onResult(false, "Gagal melakukan absen pulang.")
            }
        }
    }

    // Fetch live status for current active employee for today
    val todayAttendanceState = combine(
        activeEmployee.filterNotNull(),
        attendance
    ) { activeEmp, attList ->
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        attList.firstOrNull { it.employeeId == activeEmp.id && it.date == todayStr }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Aggregate calculations: monthly hours count per employee
    val monthlyReports = combine(
        employees,
        attendance,
        _reportMonth,
        _reportYear
    ) { empList, attList, month, year ->
        val targetPrefix = String.format(Locale.getDefault(), "%04d-%02d", year, month)
        empList.map { emp ->
            val empAtt = attList.filter { it.employeeId == emp.id && it.date.startsWith(targetPrefix) }
            val totalHours = empAtt.sumOf { it.workingHours }
            val completedDays = empAtt.count { it.clockOutTime != null }
            val earlyDepartures = empAtt.count { !it.earlyReason.isNullOrBlank() }
            
            MonthlyEmployeeSummary(
                employee = emp,
                totalHours = totalHours,
                daysPresent = empAtt.size,
                daysCompleted = completedDays,
                earlyDeparturesCount = earlyDepartures
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
}

data class MonthlyEmployeeSummary(
    val employee: Employee,
    val totalHours: Double,
    val daysPresent: Int,
    val daysCompleted: Int,
    val earlyDeparturesCount: Int
)
