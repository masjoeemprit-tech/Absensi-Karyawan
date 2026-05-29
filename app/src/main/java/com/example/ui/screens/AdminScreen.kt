package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.Employee
import com.example.data.OfficeSetting
import com.example.ui.AttendanceViewModel
import com.example.ui.export.DocumentExporter
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val employees by viewModel.employees.collectAsState()
    val attendanceLogs by viewModel.attendance.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val monthlyReports by viewModel.monthlyReports.collectAsState()

    val reportMonth by viewModel.reportMonth.collectAsState()
    val reportYear by viewModel.reportYear.collectAsState()

    var activeTab by remember { mutableStateOf(0) }
    val tabs = listOf("Kehadiran", "Pegawai DB", "Jam & Lokasi", "Laporan Bulanan")
    val tabIcons = listOf(
        Icons.Default.Ballot,
        Icons.Default.People,
        Icons.Default.Update,
        Icons.Default.Summarize
    )

    // Form inputs for managing employee
    var showEmployeeDialog by remember { mutableStateOf(false) }
    var editingEmployee by remember { mutableStateOf<Employee?>(null) }
    var empName by remember { mutableStateOf("") }
    var empCode by remember { mutableStateOf("") }
    var empDept by remember { mutableStateOf("") }
    var empEmail by remember { mutableStateOf("") }
    var empPhone by remember { mutableStateOf("") }

    // Form inputs for managing Office settings
    var showSettingDialog by remember { mutableStateOf(false) }
    var setOfficeName by remember { mutableStateOf("") }
    var setLat by remember { mutableStateOf("") }
    var setLng by remember { mutableStateOf("") }
    var setRadius by remember { mutableStateOf("") }
    var setJamMasuk by remember { mutableStateOf("") }
    var setJamPulang by remember { mutableStateOf("") }

    // Init office config form once loaded
    LaunchedEffect(settings) {
        settings?.let { s ->
            setOfficeName = s.officeName
            setLat = s.officeLat.toString()
            setLng = s.officeLng.toString()
            setRadius = s.allowedRadiusMeters.toString()
            setJamMasuk = s.jamMasuk
            setJamPulang = s.jamPulang
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin Area",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Column {
                            Text(
                                text = "Panel Admin Kepegawaian",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Kelola data & rekap absensi real-time",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        floatingActionButton = {
            if (activeTab == 1) {
                ExtendedFloatingActionButton(
                    text = { Text("Pegawai Baru") },
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = "Tambah") },
                    onClick = {
                        editingEmployee = null
                        empName = ""
                        empCode = "PEG00${employees.size + 1}"
                        empDept = "Operations"
                        empEmail = ""
                        empPhone = ""
                        showEmployeeDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.testTag("add_employee_fab")
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Horizontal scrollable Tab Layout
            ScrollableTabRow(
                selectedTabIndex = activeTab,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = activeTab == index,
                        onClick = { activeTab = index },
                        text = { Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(tabIcons[index], contentDescription = title, modifier = Modifier.size(20.dp)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tab contents
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                when (activeTab) {
                    0 -> AttendanceTab(attendanceLogs)
                    1 -> EmployeesTab(
                        employees = employees,
                        onEdit = { emp ->
                            editingEmployee = emp
                            empName = emp.name
                            empCode = emp.employeeCode
                            empDept = emp.department
                            empEmail = emp.email
                            empPhone = emp.phone
                            showEmployeeDialog = true
                        },
                        onDelete = { emp ->
                            viewModel.deleteEmployee(emp)
                            Toast.makeText(context, "${emp.name} berhasil dihapus", Toast.LENGTH_SHORT).show()
                        }
                    )
                    2 -> SettingsTab(
                        settings = settings ?: OfficeSetting(),
                        onEditClick = { showSettingDialog = true }
                    )
                    3 -> ReportsTab(
                        reports = monthlyReports,
                        employees = employees,
                        attendanceLogs = attendanceLogs,
                        reportMonth = reportMonth,
                        reportYear = reportYear,
                        onFilterChanged = { m, y -> viewModel.setReportFilter(m, y) }
                    )
                }
            }
        }

        // Employee CRUD Dialog
        if (showEmployeeDialog) {
            AlertDialog(
                onDismissRequest = { showEmployeeDialog = false },
                title = {
                    Text(
                        text = if (editingEmployee == null) "Tambah Pegawai Baru" else "Edit Data Pegawai",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = empName,
                            onValueChange = { empName = it },
                            label = { Text("Nama Lengkap") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("employee_name_input")
                        )
                        OutlinedTextField(
                            value = empCode,
                            onValueChange = { empCode = it },
                            label = { Text("NIK / Nomor Induk Karyawan") },
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = empDept,
                            onValueChange = { empDept = it },
                            label = { Text("Departemen / Divisi") },
                            leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = empEmail,
                            onValueChange = { empEmail = it },
                            label = { Text("Alamat Email") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = empPhone,
                            onValueChange = { empPhone = it },
                            label = { Text("Nomor Telepon") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (empName.isBlank() || empCode.isBlank()) {
                                Toast.makeText(context, "Nama dan NIK wajib diisi", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (editingEmployee == null) {
                                viewModel.addEmployee(empName, empCode, empDept, empEmail, empPhone)
                                Toast.makeText(context, "Berhasil menambahkan karyawan", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.updateEmployee(
                                    editingEmployee!!.id,
                                    empName,
                                    empCode,
                                    empDept,
                                    empEmail,
                                    empPhone
                                )
                                Toast.makeText(context, "Data karyawan diperbarui", Toast.LENGTH_SHORT).show()
                            }
                            showEmployeeDialog = false
                        },
                        modifier = Modifier.testTag("save_employee_button")
                    ) {
                        Text("Simpan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEmployeeDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }

        // Office Settings Configuration Dialog
        if (showSettingDialog) {
            AlertDialog(
                onDismissRequest = { showSettingDialog = false },
                title = { Text("Konfigurasi Jam & Lokasi Kerja", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        OutlinedTextField(
                            value = setOfficeName,
                            onValueChange = { setOfficeName = it },
                            label = { Text("Nama Lokasi / Kantor") },
                            leadingIcon = { Icon(Icons.Default.LocationCity, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = setLat,
                                onValueChange = { setLat = it },
                                label = { Text("Garis Lintang (Lat)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = setLng,
                                onValueChange = { setLng = it },
                                label = { Text("Garis Bujur (Lng)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = setRadius,
                            onValueChange = { setRadius = it },
                            label = { Text("Radius Maksimal Absen (meter)") },
                            leadingIcon = { Icon(Icons.Default.Straighten, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = setJamMasuk,
                                onValueChange = { setJamMasuk = it },
                                label = { Text("Jam Masuk (HH:mm)") },
                                leadingIcon = { Icon(Icons.Default.Login, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = setJamPulang,
                                onValueChange = { setJamPulang = it },
                                label = { Text("Jam Pulang (HH:mm)") },
                                leadingIcon = { Icon(Icons.Default.Logout, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val latVal = setLat.toDoubleOrNull() ?: -6.200
                            val lngVal = setLng.toDoubleOrNull() ?: 106.816
                            val radiusVal = setRadius.toDoubleOrNull() ?: 500.0
                            
                            if (setJamMasuk.split(":").size != 2 || setJamPulang.split(":").size != 2) {
                                Toast.makeText(context, "Format jam masuk/pulang tidak valid (harus HH:mm)", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            viewModel.updateOfficeSettings(
                                officeName = setOfficeName,
                                lat = latVal,
                                lng = lngVal,
                                radius = radiusVal,
                                jamMasuk = setJamMasuk,
                                jamPulang = setJamPulang
                            )
                            Toast.makeText(context, "Konfigurasi kantor diperbarui", Toast.LENGTH_SHORT).show()
                            showSettingDialog = false
                        }
                    ) {
                        Text("Simpan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSettingDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

// TAB 1: Attendance Daily real-time list
@Composable
fun AttendanceTab(logs: List<com.example.data.Attendance>) {
    if (logs.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.FactCheck,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Belum Ada Aktivitas Absen",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Log masuk dan pulang harian akan muncul di sini.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Aktivitas Terbaru", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                        Text("${logs.size} Logs", modifier = Modifier.padding(4.dp), color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 10.sp)
                    }
                }
            }
            items(logs) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Colored Circle Icon based on status
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (log.clockOutTime != null) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (log.clockOutTime != null) Icons.Default.CheckCircle else Icons.Default.Fingerprint,
                                tint = if (log.clockOutTime != null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                contentDescription = null
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Log labels
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = log.employeeName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = "Tanggal: ${log.date}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            
                            // Geofence alarm label
                            if (!log.isInCorrectLocation) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Absen Di Luar Radius Kantor!", color = MaterialTheme.colorScheme.error, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Early clock-out reason card
                            if (!log.earlyReason.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f))
                                        .padding(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Alasan Pulang Cepat: ${log.earlyReason}", color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 10.sp)
                                }
                            }
                        }

                        // Time markers (Right Aligned)
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Masuk: ${log.clockInTime ?: "-"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text(text = "Pulang: ${log.clockOutTime ?: "-"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                            if (log.workingHours > 0) {
                                Text(
                                    text = String.format(Locale.getDefault(), "%.1f Jam", log.workingHours),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// TAB 2: Employee List CRUD Tab
@Composable
fun EmployeesTab(
    employees: List<Employee>,
    onEdit: (Employee) -> Unit,
    onDelete: (Employee) -> Unit
) {
    if (employees.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Inbox, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Belum Ada Hubungan Pegawai", fontWeight = FontWeight.Bold)
            Text("Silakan klik '+' untuk mendaftarkan pegawai baru.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                Text(text = "Database Pegawai Terdaftar", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            items(employees) { emp ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = emp.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(text = "NIK: ${emp.employeeCode} | ${emp.department}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (emp.email.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = emp.email, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                        
                        // Edit & Delete actions
                        Row {
                            IconButton(onClick = { onEdit(emp) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { onDelete(emp) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

// TAB 3: Configuration of parameters
@Composable
fun SettingsTab(
    settings: OfficeSetting,
    onEditClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Konfigurasi Kantor",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))

                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Nama Lokasi :", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(settings.officeName, fontSize = 13.sp)
                }

                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Titik Koordinat (Garis) :", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("${settings.officeLat} , ${settings.officeLng}", fontSize = 13.sp)
                }

                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Batas Radius Lokasi :", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("${settings.allowedRadiusMeters} meter", fontSize = 13.sp)
                }

                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Jam Masuk Kerja :", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(settings.jamMasuk, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Jam Pulang Kerja :", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(settings.jamPulang, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                }
            }
        }

        Button(
            onClick = onEditClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sesuaikan Jam & Kordinat Geofence")
        }

        // Simulative Helper Tips
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Help, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Tips Geofencing Karyawan:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(
                        "Karyawan hanya dapat melakukan absen jika jarak koordinat HP mereka ke koordinat di atas kurang dari radius set (misal 500m). Anda dapat menguji fitur ini di halaman Karyawan dengan mengubah Slider Koordinat simulasi.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// TAB 4: Monthly statistics calculation report and PDF exporter
@Composable
fun ReportsTab(
    reports: List<com.example.ui.MonthlyEmployeeSummary>,
    employees: List<Employee>,
    attendanceLogs: List<com.example.data.Attendance>,
    reportMonth: Int,
    reportYear: Int,
    onFilterChanged: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    var isExporting by remember { mutableStateOf(false) }

    val monthNames = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    fun shareFile(file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Ekspor Laporan Bulanan"))
        Toast.makeText(context, "Laporan berhasil diekspor!", Toast.LENGTH_SHORT).show()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            // Month details and selectors
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Filter Rekapitulasi Laporan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Month controller buttons
                        Row(
                            modifier = Modifier
                                .weight(1.5f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = {
                                val prevM = if (reportMonth == 1) 12 else reportMonth - 1
                                val prevY = if (reportMonth == 1) reportYear - 1 else reportYear
                                onFilterChanged(prevM, prevY)
                            }) {
                                Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Sebelumnya", modifier = Modifier.size(16.dp))
                            }
                            Text(
                                "${monthNames[reportMonth - 1]} $reportYear",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = {
                                val nextM = if (reportMonth == 12) 1 else reportMonth + 1
                                val nextY = if (reportMonth == 12) reportYear + 1 else reportYear
                                onFilterChanged(nextM, nextY)
                            }) {
                                Icon(Icons.Default.ArrowForwardIos, contentDescription = "Berikutnya", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        item {
            // Document export triggers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val pdfFile = DocumentExporter.exportToPDF(
                            context = context,
                            attendances = attendanceLogs.filter { it.date.startsWith("%04d-%02d".format(reportYear, reportMonth)) },
                            employees = employees,
                            monthName = monthNames[reportMonth - 1],
                            year = reportYear.toString()
                        )
                        if (pdfFile != null) {
                            shareFile(pdfFile, "application/pdf")
                        } else {
                            Toast.makeText(context, "Gagal membuat PDF", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("export_pdf_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ekspor PDF", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        val csvFile = DocumentExporter.exportToCSV(
                            context = context,
                            attendances = attendanceLogs.filter { it.date.startsWith("%04d-%02d".format(reportYear, reportMonth)) },
                            employees = employees
                        )
                        if (csvFile != null) {
                            shareFile(csvFile, "text/csv")
                        } else {
                            Toast.makeText(context, "Gagal mengekspor Excel CSV", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("export_csv_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Output, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ekspor Excel", fontSize = 12.sp)
                }
            }
        }

        item {
            Text("Rekap Karyawan (Jam Kerja Bulanan)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 4.dp))
        }

        if (reports.isEmpty()) {
            item {
                Text("Belum ada pegawai atau kehadiran untuk periode ini.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(30.dp))
            }
        } else {
            items(reports) { record ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(record.employee.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("NIK: ${record.employee.employeeCode} | ${record.employee.department}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            // Total Automatic Calculation Rounded representation
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(vertical = 6.dp, horizontal = 12.dp)
                            ) {
                                Text(
                                    text = String.format(Locale.getDefault(), "%.1f j", record.totalHours),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 10.dp))

                        // Stats metrics details grid
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("Hadir", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                Text("${record.daysPresent} Hari", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("Selesai (In-Out)", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                Text("${record.daysCompleted} Hari", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("Pulang Cepat", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                Text("${record.earlyDeparturesCount} kali", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (record.earlyDeparturesCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }
    }
}
