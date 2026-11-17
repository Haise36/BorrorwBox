package com.example.borrowbox.ui.screens

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.borrowbox.utils.PasswordUtils

@Composable
fun AppLockScreen(
    onUnlock: () -> Unit,
    biometricEnabled: Boolean,
    correctPin: String?,
    pinSalt: String = "default_pin_salt"
) {
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activity = context as? FragmentActivity

    val canUseBiometric = remember(context, biometricEnabled) {
        if (!biometricEnabled) false
        else {
            val manager = BiometricManager.from(context)
            val result = manager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            result == BiometricManager.BIOMETRIC_SUCCESS
        }
    }

    val triggerBiometric = remember(activity) {
        {
            if (activity != null) {
                val executor = ContextCompat.getMainExecutor(activity)
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("BorrowBox Biometric Unlock")
                    .setSubtitle("Use your fingerprint or face to unlock")
                    .setNegativeButtonText("Use PIN")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
                    .build()

                val biometricPrompt = BiometricPrompt(
                    activity,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            onUnlock()
                        }
                    }
                )
                biometricPrompt.authenticate(promptInfo)
            }
        }
    }

    LaunchedEffect(canUseBiometric) {
        if (canUseBiometric) {
            triggerBiometric()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock, 
            contentDescription = null, 
            modifier = Modifier.size(80.dp), 
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "BorrowBox Locked", 
            style = MaterialTheme.typography.headlineMedium, 
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Enter your PIN to continue", 
            style = MaterialTheme.typography.bodyMedium, 
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(Modifier.height(36.dp))
        
        // PIN Indicators
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(4) { index ->
                val filled = index < enteredPin.length
                Surface(
                    modifier = Modifier.size(20.dp),
                    shape = CircleShape,
                    color = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (isError) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error) else null
                ) {}
            }
        }
        
        if (isError) {
            Spacer(Modifier.height(16.dp))
            Text("Incorrect PIN, please try again", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        
        Spacer(Modifier.height(36.dp))
        
        // Keypad
        PinKeypad(
            onNumberClick = { num ->
                if (enteredPin.length < 4) {
                    enteredPin += num
                    isError = false
                    if (enteredPin.length == 4) {
                        val isValid = correctPin == null || PasswordUtils.verifyPin(enteredPin, correctPin, pinSalt)
                        if (isValid) {
                            onUnlock()
                        } else {
                            isError = true
                            enteredPin = ""
                        }
                    }
                }
            },
            onDeleteClick = {
                if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
            },
            onBiometricClick = if (canUseBiometric) triggerBiometric else null
        )
    }
}

@Composable
fun PinKeypad(
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onBiometricClick: (() -> Unit)? = null
) {
    val numbers = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "BIO", "0", "DEL")
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        for (i in 0 until 4) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                for (j in 0 until 3) {
                    val index = i * 3 + j
                    if (index < numbers.size) {
                        val key = numbers[index]
                        if (key == "DEL") {
                            IconButton(onClick = onDeleteClick, modifier = Modifier.size(64.dp)) {
                                Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Delete")
                            }
                        } else if (key == "BIO") {
                            if (onBiometricClick != null) {
                                IconButton(onClick = onBiometricClick, modifier = Modifier.size(64.dp)) {
                                    Icon(Icons.Default.Fingerprint, contentDescription = "Biometric", tint = MaterialTheme.colorScheme.primary)
                                }
                            } else {
                                Spacer(Modifier.size(64.dp))
                            }
                        } else if (key.isNotBlank()) {
                            FilledTonalButton(
                                onClick = { onNumberClick(key) },
                                modifier = Modifier.size(64.dp),
                                shape = CircleShape,
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(key, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Spacer(Modifier.size(64.dp))
                        }
                    }
                }
            }
        }
    }
}
