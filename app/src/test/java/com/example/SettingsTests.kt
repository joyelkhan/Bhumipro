package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.DraftProjectEntity
import com.example.data.local.LandProDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.model.AppPreferences
import com.example.data.model.SettingsBundle
import com.example.data.model.SettingsCodec
import com.example.ui.settings.SettingsRegistry
import com.example.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettingsPersistenceTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun settingsDataStore_persistsAndResets() = runBlocking {
        val store = SettingsDataStore(app)
        store.updatePreferences {
            it.copy(
                language = "English",
                themeMode = "ডার্ক",
                appDefaultShotokSqFt = 432.0,
                defaultPrecision = 3,
                showQuickTips = false
            )
        }
        val snap = store.getSnapshot()
        assertEquals("English", snap.language)
        assertEquals("ডার্ক", snap.themeMode)
        assertEquals(432.0, snap.appDefaultShotokSqFt, 1e-6)
        assertEquals(3, snap.defaultPrecision)
        assertEquals(false, snap.showQuickTips)

        store.resetPreferences()
        val reset = store.getSnapshot()
        assertEquals("বাংলা", reset.language)
        assertEquals(435.6, reset.appDefaultShotokSqFt, 1e-6)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettingsResetRoomSafetyTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun resettingSettings_neverTouchesDraftProjectsTable() = runBlocking {
        val app: Application = ApplicationProvider.getApplicationContext()
        val db = LandProDatabase.createInMemory(app)
        db.draftProjectDao().upsert(
            DraftProjectEntity(
                id = "user_project_safe",
                title = "Safe Survey",
                mouzaName = "পশ্চিম রাউজান",
                scaleText = "১৬ ইঞ্চি",
                plotsJson = "[]",
                activePlotIndex = 0,
                totalAreaShotangsho = 12.5,
                updatedAt = 100L
            )
        )
        val vm = SettingsViewModel(app)
        vm.resetSettings()

        val stillThere = db.draftProjectDao().getById("user_project_safe")
        assertNotNull(stillThere)
        assertEquals("Safe Survey", stillThere!!.title)
        db.close()
    }
}

class SettingsRegistryTest {
    @Test
    fun registryContainsExpectedSectionsAndKeys() {
        assertTrue(SettingsRegistry.entries.isNotEmpty())
        assertNotNull(SettingsRegistry.findByKey("theme_mode"))
        assertNotNull(SettingsRegistry.findByKey("language"))
        assertNotNull(SettingsRegistry.findByKey("default_shotok"))
        assertTrue(SettingsRegistry.sectionKeys().contains("section_appearance"))
    }
}

class SettingsCodecTest {
    @Test
    fun codecRoundTripsValidBundle_andRejectsGarbage() {
        val bundle = SettingsBundle(
            version = 1,
            preferences = AppPreferences(
                language = "English",
                themeMode = "ডার্ক",
                appDefaultShotokSqFt = 430.0,
                defaultPrecision = 1
            ),
            exportedAt = 999L
        )
        val json = SettingsCodec.encode(bundle)
        val decoded = SettingsCodec.decode(json)
        assertNotNull(decoded)
        assertEquals("English", decoded!!.preferences.language)
        assertEquals(430.0, decoded.preferences.appDefaultShotokSqFt, 1e-6)
        assertNull(SettingsCodec.decode("{invalid-json"))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettingsViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun exportAndImportJson_roundTripsViaViewModel() {
        runBlocking {
            val app: Application = ApplicationProvider.getApplicationContext()
            val store = SettingsDataStore(app)
            val vm = SettingsViewModel(app)
            vm.updatePreferences { it.copy(themeMode = "সিস্টেম", appDefaultShotokSqFt = 432.0) }
            withTimeout(5000) {
                store.preferencesFlow.first { kotlin.math.abs(it.appDefaultShotokSqFt - 432.0) < 0.01 }
            }
            val exported = vm.exportSettingsJson()
            assertTrue(exported.contains("432.0"))
            vm.resetSettings()
            withTimeout(5000) {
                store.preferencesFlow.first { kotlin.math.abs(it.appDefaultShotokSqFt - 435.6) < 0.01 }
            }
            vm.importSettingsJson(exported)
            val snap = withTimeout(5000) {
                store.preferencesFlow.first { kotlin.math.abs(it.appDefaultShotokSqFt - 432.0) < 0.01 }
            }
            assertEquals(432.0, snap.appDefaultShotokSqFt, 1e-6)
        }
    }
}
