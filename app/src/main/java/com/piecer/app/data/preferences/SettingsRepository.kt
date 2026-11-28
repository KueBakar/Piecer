package com.piecer.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val cycleStartDay: Int = 1,
    val isBalanceHidden: Boolean = false,
    val dailyReminderEnabled: Boolean = false,
    val dailyReminderHour: Int = 20,
    val dailyReminderMinute: Int = 0,
    val pinHash: String? = null,
    val pinSalt: String? = null,
    val biometricEnabled: Boolean = false,
    val blockScreenshots: Boolean = false,
    val reduceMotion: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val failedPinAttempts: Int = 0,
    val pinLockoutUntilMillis: Long = 0L,
    val piecesEnabled: Boolean = false,
    val autoUpdateEnabled: Boolean = false,
    val updateConsentShown: Boolean = false,
    val updateWifiOnly: Boolean = false,
    val lastUpdateCheckMillis: Long = 0L,
    val lastUpdateCheckStatus: String = "",
    val lastReminderFiredAtMillis: Long = 0L,
    val quickEntryLearningEnabled: Boolean = true,
    val quickEntryCorrections: Map<String, String> = emptyMap(),
    val lastUsedWalletId: Long? = null,
    val voiceEntryEnabled: Boolean = true,
    val voiceInfoShown: Boolean = false
) {
    val isPinEnabled: Boolean get() = !pinHash.isNullOrBlank() && !pinSalt.isNullOrBlank()
}

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "piecer_settings"
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CYCLE_START_DAY = intPreferencesKey("cycle_start_day")
        val BALANCE_HIDDEN = booleanPreferencesKey("balance_hidden")
        val DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        val DAILY_REMINDER_HOUR = intPreferencesKey("daily_reminder_hour")
        val DAILY_REMINDER_MINUTE = intPreferencesKey("daily_reminder_minute")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val BLOCK_SCREENSHOTS = booleanPreferencesKey("block_screenshots")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val FAILED_PIN_ATTEMPTS = intPreferencesKey("failed_pin_attempts")
        val PIN_LOCKOUT_UNTIL = longPreferencesKey("pin_lockout_until")
        val PIECES_ENABLED = booleanPreferencesKey("pieces_enabled")
        val AUTO_UPDATE_ENABLED = booleanPreferencesKey("auto_update_enabled")
        val UPDATE_CONSENT_SHOWN = booleanPreferencesKey("update_consent_shown")
        val UPDATE_WIFI_ONLY = booleanPreferencesKey("update_wifi_only")
        val LAST_UPDATE_CHECK_MILLIS = longPreferencesKey("last_update_check_millis")
        val LAST_UPDATE_CHECK_STATUS = stringPreferencesKey("last_update_check_status")
        val LAST_REMINDER_FIRED_AT_MILLIS = longPreferencesKey("last_reminder_fired_at_millis")
        val QUICK_ENTRY_LEARNING_ENABLED = booleanPreferencesKey("quick_entry_learning_enabled")
        val QUICK_ENTRY_CORRECTIONS = stringPreferencesKey("quick_entry_corrections")
        val LAST_USED_WALLET_ID = longPreferencesKey("last_used_wallet_id")
        val VOICE_ENTRY_ENABLED = booleanPreferencesKey("voice_entry_enabled")
        val VOICE_INFO_SHOWN = booleanPreferencesKey("voice_info_shown")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        val themeStr = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        val themeMode = runCatching { ThemeMode.valueOf(themeStr) }.getOrDefault(ThemeMode.SYSTEM)
        AppSettings(
            themeMode = themeMode,
            cycleStartDay = (prefs[Keys.CYCLE_START_DAY] ?: 1).coerceIn(1, 28),
            isBalanceHidden = prefs[Keys.BALANCE_HIDDEN] ?: false,
            dailyReminderEnabled = prefs[Keys.DAILY_REMINDER_ENABLED] ?: false,
            dailyReminderHour = (prefs[Keys.DAILY_REMINDER_HOUR] ?: 20).coerceIn(0, 23),
            dailyReminderMinute = (prefs[Keys.DAILY_REMINDER_MINUTE] ?: 0).coerceIn(0, 59),
            pinHash = prefs[Keys.PIN_HASH],
            pinSalt = prefs[Keys.PIN_SALT],
            biometricEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false,
            blockScreenshots = prefs[Keys.BLOCK_SCREENSHOTS] ?: false,
            reduceMotion = prefs[Keys.REDUCE_MOTION] ?: false,
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            failedPinAttempts = prefs[Keys.FAILED_PIN_ATTEMPTS] ?: 0,
            pinLockoutUntilMillis = prefs[Keys.PIN_LOCKOUT_UNTIL] ?: 0L,
            piecesEnabled = prefs[Keys.PIECES_ENABLED] ?: false,
            autoUpdateEnabled = prefs[Keys.AUTO_UPDATE_ENABLED] ?: false,
            updateConsentShown = prefs[Keys.UPDATE_CONSENT_SHOWN] ?: false,
            updateWifiOnly = prefs[Keys.UPDATE_WIFI_ONLY] ?: false,
            lastUpdateCheckMillis = prefs[Keys.LAST_UPDATE_CHECK_MILLIS] ?: 0L,
            lastUpdateCheckStatus = prefs[Keys.LAST_UPDATE_CHECK_STATUS] ?: "",
            lastReminderFiredAtMillis = prefs[Keys.LAST_REMINDER_FIRED_AT_MILLIS] ?: 0L,
            quickEntryLearningEnabled = prefs[Keys.QUICK_ENTRY_LEARNING_ENABLED] ?: true,
            quickEntryCorrections = decodeCorrections(prefs[Keys.QUICK_ENTRY_CORRECTIONS]),
            lastUsedWalletId = prefs[Keys.LAST_USED_WALLET_ID],
            voiceEntryEnabled = prefs[Keys.VOICE_ENTRY_ENABLED] ?: true,
            voiceInfoShown = prefs[Keys.VOICE_INFO_SHOWN] ?: false
        )
    }

    suspend fun getSettings(): AppSettings = settingsFlow.first()

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setCycleStartDay(day: Int) {
        context.settingsDataStore.edit { it[Keys.CYCLE_START_DAY] = day.coerceIn(1, 28) }
    }

    suspend fun setBalanceHidden(hidden: Boolean) {
        context.settingsDataStore.edit { it[Keys.BALANCE_HIDDEN] = hidden }
    }

    suspend fun setDailyReminder(enabled: Boolean, hour: Int, minute: Int) {
        context.settingsDataStore.edit {
            it[Keys.DAILY_REMINDER_ENABLED] = enabled
            it[Keys.DAILY_REMINDER_HOUR] = hour.coerceIn(0, 23)
            it[Keys.DAILY_REMINDER_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    suspend fun setPin(hash: String, salt: String) {
        context.settingsDataStore.edit {
            it[Keys.PIN_HASH] = hash
            it[Keys.PIN_SALT] = salt
            it[Keys.FAILED_PIN_ATTEMPTS] = 0
            it[Keys.PIN_LOCKOUT_UNTIL] = 0L
        }
    }

    suspend fun clearPin() {
        context.settingsDataStore.edit {
            it.remove(Keys.PIN_HASH)
            it.remove(Keys.PIN_SALT)
            it[Keys.BIOMETRIC_ENABLED] = false
            it[Keys.FAILED_PIN_ATTEMPTS] = 0
            it[Keys.PIN_LOCKOUT_UNTIL] = 0L
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setBlockScreenshots(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.BLOCK_SCREENSHOTS] = enabled }
    }

    suspend fun setReduceMotion(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.REDUCE_MOTION] = enabled }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.settingsDataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setPiecesEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.PIECES_ENABLED] = enabled }
    }

    suspend fun setAutoUpdateEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.AUTO_UPDATE_ENABLED] = enabled }
    }

    suspend fun setUpdateConsentAnswered(autoUpdateEnabled: Boolean) {
        context.settingsDataStore.edit {
            it[Keys.UPDATE_CONSENT_SHOWN] = true
            it[Keys.AUTO_UPDATE_ENABLED] = autoUpdateEnabled
        }
    }

    suspend fun setUpdateWifiOnly(wifiOnly: Boolean) {
        context.settingsDataStore.edit { it[Keys.UPDATE_WIFI_ONLY] = wifiOnly }
    }

    suspend fun recordUpdateCheck(timestampMillis: Long, status: String) {
        context.settingsDataStore.edit {
            it[Keys.LAST_UPDATE_CHECK_MILLIS] = timestampMillis
            it[Keys.LAST_UPDATE_CHECK_STATUS] = status
        }
    }

    suspend fun setLastReminderFiredAt(timestampMillis: Long) {
        context.settingsDataStore.edit {
            it[Keys.LAST_REMINDER_FIRED_AT_MILLIS] = timestampMillis
        }
    }

    suspend fun updatePinAttempts(attempts: Int, lockoutUntilMillis: Long) {
        context.settingsDataStore.edit {
            it[Keys.FAILED_PIN_ATTEMPTS] = attempts
            it[Keys.PIN_LOCKOUT_UNTIL] = lockoutUntilMillis
        }
    }

    suspend fun setLastUsedWalletId(walletId: Long) {
        context.settingsDataStore.edit { it[Keys.LAST_USED_WALLET_ID] = walletId }
    }

    suspend fun setQuickEntryLearningEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.QUICK_ENTRY_LEARNING_ENABLED] = enabled }
    }

    suspend fun setVoiceEntryEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.VOICE_ENTRY_ENABLED] = enabled }
    }

    suspend fun setVoiceInfoShown(shown: Boolean) {
        context.settingsDataStore.edit { it[Keys.VOICE_INFO_SHOWN] = shown }
    }

    suspend fun saveQuickEntryCorrection(keyword: String, categoryName: String) {
        val cleanKey = keyword.trim().lowercase()
        val cleanCat = categoryName.trim()
        if (cleanKey.isEmpty() || cleanCat.isEmpty()) return
        context.settingsDataStore.edit { prefs ->
            val current = decodeCorrections(prefs[Keys.QUICK_ENTRY_CORRECTIONS]).toMutableMap()
            current.remove(cleanKey)
            current[cleanKey] = cleanCat
            // Batasi maksimal 500 entri (buang yang paling lama)
            while (current.size > 500) {
                val oldestKey = current.keys.firstOrNull() ?: break
                current.remove(oldestKey)
            }
            prefs[Keys.QUICK_ENTRY_CORRECTIONS] = encodeCorrections(current)
        }
    }

    suspend fun clearQuickEntryCorrections() {
        context.settingsDataStore.edit { it.remove(Keys.QUICK_ENTRY_CORRECTIONS) }
    }

    private fun decodeCorrections(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            val obj = org.json.JSONObject(raw)
            val result = LinkedHashMap<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = obj.optString(k)
                if (k.isNotBlank() && v.isNotBlank()) {
                    result[k] = v
                }
            }
            result
        }.getOrDefault(emptyMap())
    }

    private fun encodeCorrections(map: Map<String, String>): String {
        val obj = org.json.JSONObject()
        for ((k, v) in map) {
            obj.put(k, v)
        }
        return obj.toString()
    }

    suspend fun applyBackupPreferences(
        themeMode: ThemeMode,
        cycleStartDay: Int,
        isBalanceHidden: Boolean,
        dailyReminderEnabled: Boolean,
        dailyReminderHour: Int,
        dailyReminderMinute: Int,
        blockScreenshots: Boolean,
        reduceMotion: Boolean,
        piecesEnabled: Boolean = false
    ) {
        context.settingsDataStore.edit {
            it[Keys.THEME_MODE] = themeMode.name
            it[Keys.CYCLE_START_DAY] = cycleStartDay.coerceIn(1, 28)
            it[Keys.BALANCE_HIDDEN] = isBalanceHidden
            it[Keys.DAILY_REMINDER_ENABLED] = dailyReminderEnabled
            it[Keys.DAILY_REMINDER_HOUR] = dailyReminderHour.coerceIn(0, 23)
            it[Keys.DAILY_REMINDER_MINUTE] = dailyReminderMinute.coerceIn(0, 59)
            it[Keys.BLOCK_SCREENSHOTS] = blockScreenshots
            it[Keys.REDUCE_MOTION] = reduceMotion
            it[Keys.PIECES_ENABLED] = piecesEnabled
        }
    }
}
