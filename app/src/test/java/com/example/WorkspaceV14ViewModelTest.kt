package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LandProDatabase
import com.example.data.model.PointF
import com.example.ui.viewmodel.LandSurveyViewModel
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WorkspaceV14ViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var app: Application
    private lateinit var db: LandProDatabase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()
        db = LandProDatabase.createInMemory(app)
    }

    @After
    fun teardown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun labelStyles_areUndoable_andCopyPasteWorks() = runBlocking {
        val vm = LandSurveyViewModel(app, databaseOverride = db)
        vm.initWorkspace("draft_raujan_4")
        withTimeout(15_000) { vm.workspaceReady.first { it } }

        vm.addNewPlot()
        vm.addNewPlot(listOf(PointF(400f, 120f), PointF(520f, 120f), PointF(520f, 240f), PointF(400f, 240f)))
        val plot1 = vm.uiState.value.plots[0].id
        val plot2 = vm.uiState.value.plots[1].id

        vm.setLabelStyle(plot1) { it.copy(italic = true, colorHex = "#DC2626", fontSizeScale = 1.5f) }
        assertTrue(vm.uiState.value.plots[0].labelStyle?.italic == true)
        assertTrue(vm.canUndo.value)

        vm.undo()
        assertFalse(vm.uiState.value.plots[0].labelStyle?.italic == true)

        vm.redo()
        assertTrue(vm.uiState.value.plots[0].labelStyle?.italic == true)

        vm.copyLabelStyle(plot1)
        vm.pasteLabelStyle(plot2)
        assertEquals("#DC2626", vm.uiState.value.plots[1].labelStyle?.colorHex)
    }

    @Test
    fun vertexDelete_pasteGeometry_andCanvasSettings_persistInRoomV5() = runBlocking {
        val vm = LandSurveyViewModel(app, databaseOverride = db)
        vm.initWorkspace("draft_raujan_4")
        withTimeout(15_000) { vm.workspaceReady.first { it } }

        vm.addNewPlot()
        val pId = vm.uiState.value.plots.first().id
        assertEquals(4, vm.uiState.value.plots.first().points.size)

        vm.insertVertexAtEdgeMidpoint(0)
        assertEquals(5, vm.uiState.value.plots.first().points.size)

        vm.selectVertex(1)
        vm.deleteSelectedVertex()
        assertEquals(4, vm.uiState.value.plots.first().points.size)

        vm.copyActivePlotGeometry()
        vm.pasteCopiedPlotGeometry()
        assertEquals(2, vm.uiState.value.plots.size)

        vm.selectAllPlots()
        assertEquals(2, vm.uiState.value.selectedPlotIds.size)

        vm.updateCanvasSettings { it.copy(fontScale = 1.75f, showCrosshair = true) }
        vm.saveProjectToRoom()

        val savedEntity = db.draftProjectDao().getById("draft_raujan_4")
        assertNotNull(savedEntity)
        assertTrue(savedEntity!!.canvasSettingsJson.contains("1.75"))
        assertTrue(savedEntity.selectedPlotIdsJson.contains(pId))
    }
}
