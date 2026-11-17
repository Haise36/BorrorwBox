package com.example.borrowbox.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.borrowbox.utils.PasswordUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemeConfig {
    LIGHT, DARK, SYSTEM
}

class SettingsRepository(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme_config")
    private val notificationsKey = booleanPreferencesKey("notifications_enabled")
    private val reminderDaysKey = intPreferencesKey("reminder_days")
    private val appLockEnabledKey = booleanPreferencesKey("app_lock_enabled")
    private val biometricEnabledKey = booleanPreferencesKey("biometric_enabled")
    private val pinKey = stringPreferencesKey("app_pin")
    private val pinSaltKey = stringPreferencesKey("app_pin_salt")

    val themeConfig: Flow<ThemeConfig> = context.dataStore.data
        .map { preferences ->
            val themeName = preferences[themeKey] ?: ThemeConfig.SYSTEM.name
            ThemeConfig.valueOf(themeName)
        }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[notificationsKey] ?: true
        }

    val reminderDays: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[reminderDaysKey] ?: 1 // Default to 1 day before
        }

    val appLockEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[appLockEnabledKey] ?: false
        }

    val biometricEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[biometricEnabledKey] ?: false
        }

    val appPin: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[pinKey]
        }

    val pinSalt: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[pinSaltKey] ?: "default_pin_salt"
        }

    suspend fun setThemeConfig(config: ThemeConfig) {
        context.dataStore.edit { preferences ->
            preferences[themeKey] = config.name
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[notificationsKey] = enabled
        }
    }

    suspend fun setReminderDays(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[reminderDaysKey] = days
        }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[appLockEnabledKey] = enabled
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[biometricEnabledKey] = enabled
        }
    }

    suspend fun setAppPin(pin: String?) {
        context.dataStore.edit { preferences ->
            if (pin == null) {
                preferences.remove(pinKey)
                preferences.remove(pinSaltKey)
            } else {
                val salt = PasswordUtils.generateSalt()
                val hashedPin = PasswordUtils.hashPin(pin, salt)
                preferences[pinKey] = hashedPin
                preferences[pinSaltKey] = salt
            }
        }
    }
}
