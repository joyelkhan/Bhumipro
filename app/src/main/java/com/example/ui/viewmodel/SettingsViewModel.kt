package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.SettingsDataStore
import com.example.data.model.AppPreferences
import com.example.data.model.SettingsBundle
import com.example.data.model.SettingsCodec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    application: Application,
    private val settingsStore: SettingsDataStore = SettingsDataStore(application)
) : AndroidViewModel(application) {

    val preferences: StateFlow<AppPreferences> = settingsStore.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppPreferences())

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun updatePreferences(transform: (AppPreferences) -> AppPreferences) {
        viewModelScope.launch {
            settingsStore.updatePreferences(transform)
        }
    }

    fun resetSettings() {
        viewModelScope.launch {
            settingsStore.resetPreferences()
            _statusMessage.value = "অ্যাপ সেটিংস ডিফল্টে রিসেট হয়েছে"
        }
    }

    suspend fun exportSettingsJson(): String {
        val prefs = settingsStore.getSnapshot()
        return SettingsCodec.encode(
            SettingsBundle(
                version = 1,
                preferences = prefs,
                exportedAt = System.currentTimeMillis()
            )
        )
    }

    fun importSettingsJson(json: String) {
        val parsed = SettingsCodec.decode(json)
        if (parsed == null) {
            _statusMessage.value = "অবৈধ সেটিংস JSON ফাইল"
            return
        }
        viewModelScope.launch {
            settingsStore.updatePreferences { parsed.preferences }
            _statusMessage.value = "সেটিংস ইমপোর্ট সম্পন্ন হয়েছে"
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(application) as T
        }
    }
}
