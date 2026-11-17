package com.example.borrowbox.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.borrowbox.data.model.Borrowing
import com.example.borrowbox.data.model.UserRole
import com.example.borrowbox.data.repository.ThemeConfig
import com.example.borrowbox.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: MainViewModel,
    onNavigateToAdmin: () -> Unit = {}
) {
    val themeConfig by vm.themeConfig.collectAsState()
    val notificationsEnabled by vm.notificationsEnabled.collectAsState()
    val reminderDays by vm.reminderDays.collectAsState()
    val appLockEnabled by vm.appLockEnabled.collectAsState()
    val biometricEnabled by vm.biometricEnabled.collectAsState()
    val appPin by vm.appPin.collectAsState()
    val currentUser by vm.currentUser.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showTimingDialog by remember { mutableStateOf(false) }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showNotificationDeniedDialog by remember { mutableStateOf(false) }
    var showImportConfirmDialog by remember { mutableStateOf(false) }
    var pendingImportItems by remember { mutableStateOf<List<Borrowing>>(emptyList()) }
    var tempPin by remember { mutableStateOf("") }

    val isBiometricAvailable = remember(context) {
        val manager = BiometricManager.from(context)
        val result = manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        result == BiometricManager.BIOMETRIC_SUCCESS
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            vm.setNotifications(true)
        } else {
            showNotificationDeniedDialog = true
        }
    }

    val jsonFilePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val jsonText = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    if (jsonText != null && jsonText.isNotBlank()) {
                        val result = vm.parseJsonImport(jsonText)
                        if (result.isSuccess) {
                            pendingImportItems = result.getOrDefault(emptyList())
                            showImportConfirmDialog = true
                        }
                    }
                } catch (e: Exception) {
                    // Handled safely
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") }
            )
        }
    ) { p ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(p)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // User Profile Card
            if (currentUser != null) {
                SettingsSection("Account Profile")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (currentUser?.role == UserRole.ADMIN) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentUser?.fullName ?: "",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "@${currentUser?.username} • ${currentUser?.email?.ifBlank { "No email" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            color = if (currentUser?.role == UserRole.ADMIN) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = currentUser?.role?.name ?: "USER",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (currentUser?.role == UserRole.ADMIN) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                if (currentUser?.role == UserRole.ADMIN) {
                    SettingsItem(
                        title = "Admin Dashboard",
                        subtitle = "Manage users, monitor global system borrowings",
                        icon = Icons.Default.AdminPanelSettings,
                        onClick = onNavigateToAdmin
                    )
                }
            }

            SettingsSection("Appearance")

            SettingsItem(
                title = "Theme",
                subtitle = when (themeConfig) {
                    ThemeConfig.LIGHT -> "Light Mode"
                    ThemeConfig.DARK -> "Dark Mode"
                    ThemeConfig.SYSTEM -> "Follow System"
                },
                icon = Icons.Default.Palette,
                onClick = { showThemeDialog = true }
            )

            SettingsSection("Notifications")

            SettingsSwitchItem(
                title = "Enable Reminders",
                subtitle = "Notify when items are due",
                icon = Icons.Default.Notifications,
                checked = notificationsEnabled,
                onCheckedChange = { enabled ->
                    if (enabled && Build.VERSION.SDK_INT >= 33) {
                        val hasPerm = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPerm) {
                            vm.setNotifications(true)
                        } else {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    } else {
                        vm.setNotifications(enabled)
                    }
                }
            )

            if (notificationsEnabled) {
                SettingsItem(
                    title = "Reminder Timing",
                    subtitle = "Notify $reminderDays day(s) before due date",
                    icon = Icons.Default.Timer,
                    onClick = { showTimingDialog = true }
                )
            }

            SettingsSection("Security")

            SettingsSwitchItem(
                title = "App Lock",
                subtitle = if (appPin == null) "Set a PIN to enable lock" else "Require PIN to open the app",
                icon = Icons.Default.Lock,
                checked = appLockEnabled == true && appPin != null,
                onCheckedChange = { 
                    if (it && appPin == null) {
                        showPinSetupDialog = true
                    } else {
                        vm.setAppLock(it)
                    }
                }
            )

            if (isBiometricAvailable && appLockEnabled == true && appPin != null) {
                SettingsSwitchItem(
                    title = "Use Biometric Unlock",
                    subtitle = "Unlock with fingerprint or face recognition",
                    icon = Icons.Default.Fingerprint,
                    checked = biometricEnabled,
                    onCheckedChange = { vm.setBiometric(it) }
                )
            }

            if (appLockEnabled == true && appPin != null) {
                SettingsItem(
                    title = "Change PIN",
                    subtitle = "Update your 4-digit security code",
                    icon = Icons.Default.Password,
                    onClick = { 
                        tempPin = ""
                        showPinSetupDialog = true 
                    }
                )
            }

            SettingsSection("Data Backup & Restore")

            SettingsItem(
                title = "Export Data (CSV File)",
                subtitle = "Share a spreadsheet file of all items",
                icon = Icons.Default.FileDownload,
                onClick = {
                    vm.exportDataFile(context, isJson = false)
                }
            )

            SettingsItem(
                title = "Export Data (JSON File)",
                subtitle = "Export full backup file format",
                icon = Icons.Default.Code,
                onClick = {
                    vm.exportDataFile(context, isJson = true)
                }
            )

            SettingsItem(
                title = "Import Data (JSON File)",
                subtitle = "Restore borrowing records from JSON file",
                icon = Icons.Default.FileUpload,
                onClick = {
                    jsonFilePickerLauncher.launch("*/*")
                }
            )

            SettingsSection("Account Session")

            SettingsItem(
                title = "Sign Out",
                subtitle = "Log out from your current account",
                icon = Icons.AutoMirrored.Filled.Logout,
                onClick = { showLogoutDialog = true }
            )

            SettingsSection("About")

            SettingsItem(
                title = "BorrowBox v1.0",
                subtitle = "Track what you lend to others. Keep it simple, keep it purple.",
                icon = Icons.Default.Info,
                onClick = {}
            )

            Spacer(Modifier.height(40.dp))
        }
    }

    if (showImportConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showImportConfirmDialog = false },
            title = { Text("Confirm Data Import") },
            text = { Text("Found ${pendingImportItems.size} record(s) in JSON file. Do you want to import them into your BorrowBox?") },
            confirmButton = {
                Button(onClick = {
                    vm.importBorrowings(pendingImportItems)
                    showImportConfirmDialog = false
                    pendingImportItems = emptyList()
                }) {
                    Text("Import Records")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showImportConfirmDialog = false 
                    pendingImportItems = emptyList()
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showNotificationDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationDeniedDialog = false },
            title = { Text("Notification Permission Required") },
            text = { Text("Notifications are required to send borrowing reminders. Please grant permission in app settings.") },
            confirmButton = {
                Button(onClick = {
                    showNotificationDeniedDialog = false
                    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                    } else {
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    }
                    context.startActivity(intent)
                }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationDeniedDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Confirm Sign Out") },
            text = { Text("Are you sure you want to log out of BorrowBox?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        vm.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Select Theme") },
            text = {
                Column {
                    ThemeOption("Light Mode", ThemeConfig.LIGHT, themeConfig) { vm.setTheme(it); showThemeDialog = false }
                    ThemeOption("Dark Mode", ThemeConfig.DARK, themeConfig) { vm.setTheme(it); showThemeDialog = false }
                    ThemeOption("Follow System", ThemeConfig.SYSTEM, themeConfig) { vm.setTheme(it); showThemeDialog = false }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Close") }
            }
        )
    }

    if (showTimingDialog) {
        AlertDialog(
            onDismissRequest = { showTimingDialog = false },
            title = { Text("Reminder Timing") },
            text = {
                Column {
                    listOf(1, 2, 3, 5, 7).forEach { days ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (days == reminderDays),
                                    onClick = { vm.setReminderTiming(days); showTimingDialog = false }
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = (days == reminderDays), onClick = { vm.setReminderTiming(days); showTimingDialog = false })
                            Spacer(Modifier.width(12.dp))
                            Text("$days day(s) before", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimingDialog = false }) { Text("Close") }
            }
        )
    }

    if (showPinSetupDialog) {
        AlertDialog(
            onDismissRequest = { 
                showPinSetupDialog = false
                tempPin = ""
            },
            title = { Text(if (appPin == null) "Set App PIN" else "Change App PIN") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Enter a 4-digit PIN", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(4) { index ->
                            val digit = tempPin.getOrNull(index)
                            Surface(
                                modifier = Modifier.size(16.dp),
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = if (digit != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ) {}
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    PinKeypadSmall(
                        onNumberClick = { if (tempPin.length < 4) tempPin += it },
                        onDeleteClick = { if (tempPin.isNotEmpty()) tempPin = tempPin.dropLast(1) }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = tempPin.length == 4,
                    onClick = {
                        vm.setPin(tempPin)
                        vm.setAppLock(true)
                        showPinSetupDialog = false
                        tempPin = ""
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showPinSetupDialog = false
                    tempPin = ""
                }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun PinKeypadSmall(onNumberClick: (String) -> Unit, onDeleteClick: () -> Unit) {
    val numbers = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "DEL")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in 0 until 4) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                for (j in 0 until 3) {
                    val index = i * 3 + j
                    val key = numbers[index]
                    if (key == "DEL") {
                        IconButton(onClick = onDeleteClick, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.AutoMirrored.Filled.Backspace, null, modifier = Modifier.size(20.dp))
                        }
                    } else if (key.isNotBlank()) {
                        OutlinedButton(
                            onClick = { onNumberClick(key) },
                            modifier = Modifier.size(48.dp),
                            shape = androidx.compose.foundation.shape.CircleShape,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(key, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Spacer(Modifier.size(48.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

@Composable
fun SettingsItem(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(20.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun SettingsSwitchItem(title: String, subtitle: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun ThemeOption(label: String, config: ThemeConfig, current: ThemeConfig, onSelect: (ThemeConfig) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = (config == current),
                onClick = { onSelect(config) }
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = (config == current), onClick = { onSelect(config) })
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.isBlank()
