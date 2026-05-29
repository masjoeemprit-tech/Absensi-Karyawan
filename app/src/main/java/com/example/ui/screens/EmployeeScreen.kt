package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Employee
import com.example.data.OfficeSetting
import com.example.ui.AttendanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val employees by viewModel.employees.collectAsState()
    val activeEmp by viewModel.activeEmployee.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val todayAttendance by viewModel.todayAttendanceState.collectAsState()

    val simLat by viewModel.simulatedLat.collectAsState()
    val simLng by viewModel.simulatedLng.collectAsState()
    val simHour by viewModel.simulatedHour.collectAsState()
    val simMinute by viewModel.simulatedMinute.collectAsState()

    var showEmployeeSelector by remember { mutableStateOf(false) }
    var clockOutReason by remember { mutableStateOf("") }
    
    // Auto-update simulated lat/long presets
    var selectedPresetIndex by remember { mutableStateOf(0) }
    val office = settings ?: OfficeSetting()
    
    // Quick presets to test geo-restriction
    val geoPresets = listOf(
        GeoPreset("Meja Kerja (Sangat Dekat)", office.officeLat, office.officeLng, "10 meter (Aktif)"),
        GeoPreset("Tempat Parkir Bawah", office.officeLat + 0.0005, office.officeLng + 0.0005, "75 meter (Aktif)"),
        GeoPreset("Halte Bus Depan Kantor", office.officeLat + 0.0035, office.officeLng + 0.0025, "480 meter (Batas Akhir)"),
        GeoPreset("Rumah / Kafe Jauh", office.officeLat + 0.025, office.officeLng + 0.035, "3.8 km (Terblokir)")
    )

    LaunchedEffect(selectedPresetIndex, settings) {
        val p = geoPresets.getOrElse(selectedPresetIndex) { geoPresets[0] }
        viewModel.updateSimulatedLocation(p.lat, p.lng)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Presensi Karyawan",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Lakukan absen masuk & pulang mandiri",
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
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Selector Profile
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = activeEmp?.name?.take(2)?.uppercase() ?: "??",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeEmp?.name ?: "Pilih Karyawan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = activeEmp?.let { "${it.department} (${it.employeeCode})" } ?: "Klik ganti untuk memilih nama",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { showEmployeeSelector = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Ganti", fontSize = 11.sp)
                        }
                    }
                }
            }

            // Section 2: Simulator Sandbox Control (Time and Location coordinates)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simulator Pengetesan (Waktu & GPS)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                        }

                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                        // A: Location presetes
                        Text("Simulasikan Posisi GPS Anda saat ini:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            geoPresets.forEachIndexed { index, preset ->
                                val selected = selectedPresetIndex == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .border(1.dp, if (selected) MaterialTheme.colorScheme.secondary else Color.Transparent, RoundedCornerShape(6.dp))
                                        .clickable { selectedPresetIndex = index }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = preset.name.substringBefore(" "),
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 12.sp,
                                        color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Distance metrics label
                        val dist = viewModel.getDistanceToOffice()
                        val allowed = office.allowedRadiusMeters
                        val inRange = viewModel.isWithinDistance()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (inRange) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (inRange) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                tint = if (inRange) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp),
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (inRange) "Lokasi Sah (Dalam Radius)" else "Lokasi Tidak Sah (Di Luar Radius)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (inRange) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Jarak Anda ke ${office.officeName}: %.1f meter (Maksimal %.0f meter)".format(dist, allowed),
                                    fontSize = 10.sp,
                                    color = if (inRange) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // B: Time preset adjustment (Slider)
                        Text(
                            text = "Simulasikan Jam HP Kehadiran: ${viewModel.getSimulatedTimeString()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                            Spacer(modifier = Modifier.width(8.dp))
                            Slider(
                                value = simHour.toFloat() + (simMinute.toFloat() / 60f),
                                onValueChange = {
                                    val hr = it.toInt()
                                    val min = ((it - hr) * 60).toInt()
                                    viewModel.updateSimulatedTime(hr, min)
                                },
                                valueRange = 0f..23.99f,
                                modifier = Modifier.weight(1f).testTag("time_simulation_slider")
                            )
                        }
                    }
                }
            }

            // Section 3: The Active Attendance Terminal UI
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "TERMINAL ABSENSI DIGITAL",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.outline
                        )

                        // Circular Simulated Time Watch FACE
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    )
                                )
                                .border(4.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = viewModel.getSimulatedTimeString(),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // Shift/Hours range display
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Jam Masuk", fontSize = 9.sp, color = Color.Gray)
                                    Text(office.jamMasuk, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Jam Pulang", fontSize = 9.sp, color = Color.Gray)
                                    Text(office.jamPulang, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.tertiary)
                                }
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                        // Core Button Clock Panel
                        if (todayAttendance == null) {
                            // Employee hasn't clocked in yet
                            Button(
                                onClick = {
                                    viewModel.performClockIn { success, message ->
                                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("clock_in_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Absen Masuk Sekarang", fontWeight = FontWeight.Bold)
                            }
                        } else if (todayAttendance?.clockOutTime == null) {
                            // Employee Clocked in, needs clock out
                            val early = viewModel.isClockOutEarly()
                            
                            if (early) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.NotificationImportant, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "Anda Pulang Sebelum Waktunya (${office.jamPulang})!",
                                                color = MaterialTheme.colorScheme.error,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Text(
                                            "Berdasarkan sistem, Anda pulang lebih awal dari jam pulang wajib. Anda wajib memasukkan alasan izin pulang awal untuk membuka tombol absen pulang.",
                                            fontSize = 10.sp,
                                            lineHeight = 12.sp
                                        )
                                        OutlinedTextField(
                                            value = clockOutReason,
                                            onValueChange = { clockOutReason = it },
                                            placeholder = { Text("Tulis misal: Sakit kepala, ke dokter, janji bank...", fontSize = 12.sp) },
                                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                            modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(4.dp)).testTag("early_reason_input"),
                                            singleLine = true
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Disable button if early clock-out is active but reason is empty
                            val buttonEnabled = !early || clockOutReason.isNotBlank()

                            Button(
                                onClick = {
                                    viewModel.performClockOut(
                                        reason = if (early) clockOutReason else null
                                    ) { success, message ->
                                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                        if (success) {
                                            clockOutReason = ""
                                        }
                                    }
                                },
                                enabled = buttonEnabled,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("clock_out_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.ExitToApp, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Absen Pulang Sekarang", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            // Finished attendance for today
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(28.dp))
                                    Text("Presensi Hari Ini Selesai!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                                    Text(
                                        "Masuk: ${todayAttendance?.clockInTime} | Pulang: ${todayAttendance?.clockOutTime}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "Total jam kerja dihitung: %.1f jam".format(todayAttendance?.workingHours ?: 0.0),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 4: History Logs of logged-in user
            item {
                Text("Log Kehadiran Anda (Bulan Ini)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
            }

            // Simple search lists in current user's attendance
            val userLogs = todayAttendance?.let { listOf(it) } ?: emptyList()
            // We can also let employee see any other logs they have
            // (Let's make a mini inline list filter of logs for activeEmp)
            val activeEmpLogs = activeEmp?.let { emp ->
                val targetPrefix = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
                // Grab all logs that match emp.id
                viewModel.attendance.value.filter { it.employeeId == emp.id && it.date.startsWith(targetPrefix) }
            } ?: emptyList()

            if (activeEmpLogs.isEmpty()) {
                item {
                    Text("Belum ada logs untuk bulan ini.", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(16.dp))
                }
            } else {
                items(activeEmpLogs) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(item.date, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                if (!item.earlyReason.isNullOrBlank()) {
                                    Text("Alasan: ${item.earlyReason}", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                                } else {
                                    Text("Absensi Lokasi Kantor Valid", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Masuk", fontSize = 9.sp, color = Color.Gray)
                                    Text(item.clockInTime ?: "-", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Pulang", fontSize = 9.sp, color = Color.Gray)
                                    Text(item.clockOutTime ?: "-", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Employee profile selection Dialog
        if (showEmployeeSelector) {
            AlertDialog(
                onDismissRequest = { showEmployeeSelector = false },
                title = { Text("Pilih Profil Pegawai", fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Simulasikan masuk sebagai akun berikut:", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
                        
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.heightIn(max = 240.dp)
                        ) {
                            items(employees) { e ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (activeEmp?.id == e.id) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                        .clickable {
                                            viewModel.setActiveEmployee(e)
                                            showEmployeeSelector = false
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (activeEmp?.id == e.id) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (activeEmp?.id == e.id) MaterialTheme.colorScheme.primary else Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(e.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("${e.department} | ${e.employeeCode}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }
    }
}

data class GeoPreset(
    val name: String,
    val lat: Double,
    val lng: Double,
    val info: String
)
