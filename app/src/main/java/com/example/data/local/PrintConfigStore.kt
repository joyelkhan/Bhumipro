package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.MoshiProvider
import com.example.data.model.PrintConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.printConfigDataStore by preferencesDataStore(name = "print_config_prefs")

class PrintConfigStore(private val context: Context) {
    private val adapter = MoshiProvider.moshi.adapter(PrintConfig::class.java)
    private val globalKey = stringPreferencesKey("config_global")

    private fun projectKey(projectId: String): Preferences.Key<String> =
        stringPreferencesKey("config_project_$projectId")

    fun observeEffective(projectId: String): Flow<PrintConfig> =
        context.printConfigDataStore.data.map { prefs ->
            val projectJson = prefs[projectKey(projectId)]
            val globalJson = prefs[globalKey]
            decode(projectJson) ?: decode(globalJson) ?: PrintConfig()
        }

    suspend fun loadEffective(projectId: String): PrintConfig {
        val prefs = context.printConfigDataStore.data.first()
        val projectJson = prefs[projectKey(projectId)]
        val globalJson = prefs[globalKey]
        return decode(projectJson) ?: decode(globalJson) ?: PrintConfig()
    }

    suspend fun loadGlobal(): PrintConfig {
        val prefs = context.printConfigDataStore.data.first()
        return decode(prefs[globalKey]) ?: PrintConfig()
    }

    suspend fun saveGlobal(config: PrintConfig) {
        context.printConfigDataStore.edit { prefs ->
            prefs[globalKey] = adapter.toJson(config)
        }
    }

    suspend fun saveForProject(projectId: String, config: PrintConfig) {
        context.printConfigDataStore.edit { prefs ->
            prefs[projectKey(projectId)] = adapter.toJson(config)
        }
    }

    suspend fun clearProjectOverride(projectId: String) {
        context.printConfigDataStore.edit { prefs ->
            prefs.remove(projectKey(projectId))
        }
    }

    suspend fun hasProjectOverride(projectId: String): Boolean {
        val prefs = context.printConfigDataStore.data.first()
        return prefs.contains(projectKey(projectId))
    }

    private fun decode(json: String?): PrintConfig? {
        if (json.isNullOrBlank()) return null
        return runCatching { adapter.fromJson(json) }.getOrNull()
    }
}
