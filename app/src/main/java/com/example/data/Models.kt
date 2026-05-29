package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "employees")
data class Employee(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val employeeCode: String,
    val name: String,
    val department: String,
    val email: String,
    val phone: String
)

@Entity(tableName = "attendance")
data class Attendance(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val employeeId: Int,
    val employeeName: String,
    val date: String, // format: "yyyy-MM-dd"
    val clockInTime: String? = null, // format: "HH:mm"
    val clockOutTime: String? = null, // format: "HH:mm"
    val earlyReason: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isInCorrectLocation: Boolean = true,
    val workingHours: Double = 0.0 // computed in hours
)

@Entity(tableName = "office_settings")
data class OfficeSetting(
    @PrimaryKey val id: Int = 1,
    val officeName: String = "Kantor Pusat Jakarta",
    val officeLat: Double = -6.200000,
    val officeLng: Double = 106.816666,
    val allowedRadiusMeters: Double = 500.0,
    val jamMasuk: String = "08:00", // HH:mm
    val jamPulang: String = "17:00"  // HH:mm
)
