package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.AttendanceViewModel

@Composable
fun MainAppNavigation(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf(Screen.Employee) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("main_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = currentScreen == Screen.Employee,
                    onClick = { currentScreen = Screen.Employee },
                    icon = { Icon(Icons.Default.Fingerprint, contentDescription = "Terminal Karyawan") },
                    label = { Text("Karyawan") },
                    modifier = Modifier.testTag("nav_to_employee")
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.Admin,
                    onClick = { currentScreen = Screen.Admin },
                    icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Portal Admin") },
                    label = { Text("Portal Admin") },
                    modifier = Modifier.testTag("nav_to_admin")
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.Employee -> {
                    EmployeeScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                }
                Screen.Admin -> {
                    AdminScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

enum class Screen {
    Employee,
    Admin
}
