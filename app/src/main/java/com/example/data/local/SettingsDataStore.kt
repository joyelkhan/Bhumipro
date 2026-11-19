package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppPreferences
import com.example.data.model.MeasurementUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "bhumipro_settings_prefs")

class SettingsDataStore(private val context: Context) {
    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_SHOTOK = doublePreferencesKey("app_default_shotok")
        val DASHBOARD_TEMPLATE = stringPreferencesKey("dashboard_template")
        val DEFAULT_UNIT_ID = stringPreferencesKey("default_unit_id")
        val DEFAULT_PRECISION = intPreferencesKey("default_precision")
        val SHOW_QUICK_TIPS = booleanPreferencesKey("show_quick_tips")
        val SOUND_EFFECTS = booleanPreferencesKey("sound_effects")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val AUTO_SAVE_SNAPSHOTS = booleanPreferencesKey("auto_save_snapshots")
    }

    val preferencesFlow: Flow<AppPreferences> = context.settingsDataStore.data.map { prefs ->
        AppPreferences(
            language = prefs[Keys.LANGUAGE] ?: "বাংলা",
            themeMode = prefs[Keys.THEME_MODE] ?: "লাইট",
            appDefaultShotokSqFt = (prefs[Keys.DEFAULT_SHOTOK] ?: 435.6).coerceIn(100.0, 2000.0),
            dashboardTemplate = prefs[Keys.DASHBOARD_TEMPLATE] ?: "standard",
            defaultUnitId = prefs[Keys.DEFAULT_UNIT_ID] ?: MeasurementUnit.FEET_INCHES.id,
            defaultPrecision = (prefs[Keys.DEFAULT_PRECISION] ?: 2).coerceIn(0, 3),
            showQuickTips = prefs[Keys.SHOW_QUICK_TIPS] ?: true,
            soundEffects = prefs[Keys.SOUND_EFFECTS] ?: true,
            hapticFeedback = prefs[Keys.HAPTIC_FEEDBACK] ?: true,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            autoSaveSnapshots = prefs[Keys.AUTO_SAVE_SNAPSHOTS] ?: false
        )
    }

    suspend fun getSnapshot(): AppPreferences = preferencesFlow.first()

    suspend fun updatePreferences(transform: (AppPreferences) -> AppPreferences) {
        context.settingsDataStore.edit { prefs ->
            val current = AppPreferences(
                language = prefs[Keys.LANGUAGE] ?: "বাংলা",
                themeMode = prefs[Keys.THEME_MODE] ?: "লাইট",
                appDefaultShotokSqFt = (prefs[Keys.DEFAULT_SHOTOK] ?: 435.6).coerceIn(100.0, 2000.0),
                dashboardTemplate = prefs[Keys.DASHBOARD_TEMPLATE] ?: "standard",
                defaultUnitId = prefs[Keys.DEFAULT_UNIT_ID] ?: MeasurementUnit.FEET_INCHES.id,
                defaultPrecision = (prefs[Keys.DEFAULT_PRECISION] ?: 2).coerceIn(0, 3),
                showQuickTips = prefs[Keys.SHOW_QUICK_TIPS] ?: true,
                soundEffects = prefs[Keys.SOUND_EFFECTS] ?: true,
                hapticFeedback = prefs[Keys.HAPTIC_FEEDBACK] ?: true,
                notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
                autoSaveSnapshots = prefs[Keys.AUTO_SAVE_SNAPSHOTS] ?: false
            )
            val updated = transform(current)
            prefs[Keys.LANGUAGE] = updated.language
            prefs[Keys.THEME_MODE] = updated.themeMode
            prefs[Keys.DEFAULT_SHOTOK] = updated.appDefaultShotokSqFt.coerceIn(100.0, 2000.0)
            prefs[Keys.DASHBOARD_TEMPLATE] = updated.dashboardTemplate
            prefs[Keys.DEFAULT_UNIT_ID] = updated.defaultUnitId
            prefs[Keys.DEFAULT_PRECISION] = updated.defaultPrecision.coerceIn(0, 3)
            prefs[Keys.SHOW_QUICK_TIPS] = updated.showQuickTips
            prefs[Keys.SOUND_EFFECTS] = updated.soundEffects
            prefs[Keys.HAPTIC_FEEDBACK] = updated.hapticFeedback
            prefs[Keys.NOTIFICATIONS_ENABLED] = updated.notificationsEnabled
            prefs[Keys.AUTO_SAVE_SNAPSHOTS] = updated.autoSaveSnapshots
        }
    }

    suspend fun setAppDefaultShotok(sqFeetPerShotok: Double) {
        updatePreferences { it.copy(appDefaultShotokSqFt = sqFeetPerShotok.coerceIn(100.0, 2000.0)) }
    }

    suspend fun resetPreferences() {
        context.settingsDataStore.edit { it.clear() }
    }
}
