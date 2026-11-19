package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LandProDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.model.AppPreferences
import com.example.data.model.DivisionType
import com.example.data.model.LandPlot
import com.example.data.model.PointF
import com.example.data.model.SettingsBundle
import com.example.data.model.SettingsCodec
import com.example.domain.JtsDivisionEngine
import com.example.domain.UnknownSideCalculator
import com.example.ui.viewmodel.PrintTicketViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
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
class BhumiProV20UpgradeTest {
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
    fun unknownSideCalculator_heronAndQuadAndMissingSide() {
        val tri = UnknownSideCalculator.calculateTriangleHeron(30.0, 40.0, 50.0, 435.6)
        assertNull(tri.errorBn)
        assertEquals(600.0, tri.areaSqFt, 0.01)

        val badTri = UnknownSideCalculator.calculateTriangleHeron(10.0, 20.0, 100.0, 435.6)
        assertNotNull(badTri.errorBn)

        val quad = UnknownSideCalculator.calculateQuadrilateral(100.0, 100.0, 50.0, 50.0, 111.8034, 435.6)
        assertNull(quad.errorBn)
        assertEquals(5000.0, quad.exactAreaSqFt, 0.5)
        assertEquals(5000.0 / 435.6, quad.exactAreaShotok, 0.01)

        val missing = UnknownSideCalculator.solveMissingSide(
            targetShotok = 5.0,
            knownSideFeet = 50.0,
            oppositeParallelSideFeet = null,
            sqFeetPerShotok = 435.6
        )
        assertNull(missing.errorBn)
        assertEquals((5.0 * 435.6) / 50.0, missing.requiredLengthFeet, 0.01)
    }

    @Test
    fun bontonnamaCalculator_conservesTotalAreaAcrossWeightedShares() {
        val shares = UnknownSideCalculator.calculateBontonnamaShares(
            totalShotok = 12.0,
            shareLabelsAndWeights = listOf("অংশীদার ১" to 2.0, "অংশীদার ২" to 1.0, "অংশীদার ৩" to 1.0),
            sqFeetPerShotok = 435.6
        )
        assertEquals(3, shares.size)
        assertEquals(50.0, shares[0].normalizedPercent, 0.01)
        assertEquals(6.0, shares[0].areaShotok, 0.01)
        assertEquals(12.0, shares.sumOf { it.areaShotok }, 0.01)
    }

    @Test
    fun jtsDivisionEngine_interactivePolylineRoad_conservesAreaAndSplitsParts() {
        val rectPlot = LandPlot(
            id = "plot_poly_road",
            name = "প্লট ১",
            points = listOf(
                PointF(0f, 0f),
                PointF(200f, 0f),
                PointF(200f, 150f),
                PointF(0f, 150f)
            ),
            areaSqFt = 30000.0,
            areaShotangsho = 30000.0 / 435.6
        )
        val customRoad = listOf(
            PointF(-10f, 75f),
            PointF(100f, 65f),
            PointF(210f, 85f)
        )
        val result = JtsDivisionEngine.dividePlot(
            source = rectPlot,
            divisionType = DivisionType.EQUAL,
            partsCount = 4,
            percentages = listOf(25.0, 25.0, 25.0, 25.0),
            customTargetShotoks = emptyList(),
            roadOptionId = "middle_horizontal",
            roadWidthFeet = 6f,
            sweepAngleDeg = 0f,
            feetPerCanvasUnit = 1.0,
            sqFeetPerShotok = 435.6,
            customRoadPoints = customRoad
        )
        assertNull(result.report.warningMessage)
        assertTrue(result.report.roadAreaShotok > 0.0)
        assertEquals(4, result.report.parts.size)
        val totalOut = result.report.roadAreaShotok + result.report.parts.sumOf { it.areaShotok }
        assertEquals(result.report.originalAreaShotok, totalOut, 0.5)
    }

    @Test
    fun mouzaCatalog_coversAllEightBangladeshDivisions_andPersistsInRoom() = runBlocking {
        val catalog = LandProDatabase.defaultMouzaCatalog()
        val divisions = catalog.map { it.divisionNameBn }.toSet()
        val expected = setOf("খুলনা", "চট্টগ্রাম", "ঢাকা", "বরিশাল", "ময়মনসিংহ", "রংপুর", "রাজশাহী", "সিলেট")
        assertTrue(divisions.containsAll(expected))

        val app: Application = ApplicationProvider.getApplicationContext()
        val db = LandProDatabase.createInMemory(app)
        db.mouzaLibraryDao().upsertAll(catalog)
        val stored = db.mouzaLibraryDao().observeAll().first()
        assertEquals(catalog.size, stored.size)
        db.close()
    }

    @Test
    fun extendedPreferencesAndPrintTicket_roundTrip() = runBlocking {
        val prefs = AppPreferences(soundEffects = false, notificationsEnabled = false)
        val encoded = SettingsCodec.encode(SettingsBundle(1, prefs, 123L))
        val decoded = SettingsCodec.decode(encoded)
        assertNotNull(decoded)
        assertEquals(false, decoded!!.preferences.soundEffects)
        assertEquals(false, decoded.preferences.notificationsEnabled)

        val app: Application = ApplicationProvider.getApplicationContext()
        val store = SettingsDataStore(app)
        store.updatePreferences { it.copy(soundEffects = false, notificationsEnabled = true) }
        val snap = store.getSnapshot()
        assertEquals(false, snap.soundEffects)
        assertEquals(true, snap.notificationsEnabled)

        val ticketVm = PrintTicketViewModel(app)
        ticketVm.updateTicket {
            it.copy(
                address = "রাউজান, চট্টগ্রাম",
                registrationNo = "REG-2026-NX",
                districtField = "চট্টগ্রাম",
                qualifications = "বিএসসি সার্ভে ইঞ্জিনিয়ার",
                slogan = "নিখুঁত ডিজিটাল জরিপ",
                reportNote = "সরেজমিন তদন্ত সাপেক্ষে প্রস্তুত"
            )
        }
        ticketVm.saveTicket()
        withTimeout(5000) {
            ticketVm.uiState.first { it.statusMessage != null }
        }
        val persistedVm = PrintTicketViewModel(app)
        val loadedState = withTimeout(5000) {
            persistedVm.uiState.first { it.ticket.registrationNo == "REG-2026-NX" }
        }
        assertEquals("রাউজান, চট্টগ্রাম", loadedState.ticket.address)
        assertEquals("চট্টগ্রাম", loadedState.ticket.districtField)
        assertEquals("নিখুঁত ডিজিটাল জরিপ", loadedState.ticket.slogan)
    }

    @Test
    fun workspaceWorkflow6_drawPointDeleteZoomStepAndPdfSourceFlag() = runBlocking {
        val app: Application = ApplicationProvider.getApplicationContext()
        val db = LandProDatabase.createInMemory(app)
        val vm = com.example.ui.viewmodel.LandSurveyViewModel(app, db)
        vm.initWorkspace("draft_raujan_4")
        withTimeout(5000) {
            vm.workspaceReady.first { it }
        }
        assertTrue(vm.isPdfSource.value)

        vm.requestZoomStep(true)
        assertEquals("zoom_in", vm.viewportCommand.value?.type)
        vm.requestZoomStep(false)
        assertEquals("zoom_out", vm.viewportCommand.value?.type)

        val initialPlotCount = vm.uiState.value.plots.size
        vm.startDrawingNewPlot()
        vm.addDrawingPoint(PointF(10f, 10f))
        vm.addDrawingPoint(PointF(110f, 10f))
        vm.addDrawingPoint(PointF(110f, 110f))
        vm.addDrawingPoint(PointF(55f, 55f))
        assertEquals(4, vm.drawingPoints.value.size)

        // Delete button in draw mode deletes last drawing point instead of existing active plot
        vm.deleteActivePlotOrVertex()
        assertEquals(3, vm.drawingPoints.value.size)
        assertEquals(initialPlotCount, vm.uiState.value.plots.size)

        vm.closeDrawingPolygon()
        assertEquals(initialPlotCount + 1, vm.uiState.value.plots.size)
        assertEquals("select", vm.activeTool.value)
        assertTrue(vm.drawingPoints.value.isEmpty())
        db.close()
    }
}
