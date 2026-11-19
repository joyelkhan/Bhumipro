package com.example

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LandProDatabase
import com.example.data.local.PrintPresetEntity
import com.example.data.model.AreaDisplayUnit
import com.example.data.model.CanvasDisplaySettings
import com.example.data.model.GisLayerItem
import com.example.data.model.GisLayerType
import com.example.data.model.MapTextPosition
import com.example.data.model.MeasurementUnit
import com.example.data.model.PaperSize
import com.example.data.model.PlotLabelStyle
import com.example.data.model.PointF
import com.example.data.model.PrintConfig
import com.example.data.model.PrintLineColorMode
import com.example.data.model.PrintOrientation
import com.example.data.model.SurveyProject
import com.example.domain.GeometryUtils
import com.example.domain.report.BengaliSurveyReportRenderer
import com.example.domain.report.BitmapMapFilters
import com.example.domain.report.PageLayout
import com.example.domain.report.PrintPageMath
import com.example.domain.report.ReportLabelStyle
import com.example.domain.report.ReportPdfExporter
import com.example.domain.report.ReportTemplate
import com.example.domain.report.SurveyPadFidelity
import com.example.domain.report.SurveyReportDataMapper
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
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SurveyPadPrintFidelityTest {

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

    private fun sample100x79Plot(): com.example.data.model.LandPlot =
        GeometryUtils.buildPlot(
            id = "plot_fid",
            name = "প্লট ১",
            colorHex = "#2563EB",
            points = listOf(
                PointF(0f, 0f),
                PointF(100f, 0f),
                PointF(100f, 80f),
                PointF(0f, 80f)
            ),
            feetPerCanvasUnit = 0.55,
            sqFeetPerShotok = 435.6
        )

    @Test
    fun mapper_capturesEveryCanvasSettingIntoPad() {
        val canvas = CanvasDisplaySettings(
            measurementUnit = MeasurementUnit.METERS.id,
            decimalPrecision = 3,
            measurementColorHex = "#C23A32",
            measurementStrokeScale = 1.4f,
            fontScale = 1.25f,
            lineScale = 1.5f,
            outlineScale = 0.6f,
            pointScale = 1.3f,
            showMeasurementsOutside = false,
            showSplitTotalInside = true,
            mapTextPosition = MapTextPosition.INSIDE.id,
            zoomTextMode = "fixed",
            gridColorHex = "#334155",
            gridSpacingUnits = 50f,
            showCrosshair = true,
            showCornerPoints = false,
            brightness = -0.25f,
            contrast = 0.2f,
            grayscale = true,
            mapOpacity = 0.7f
        )
        val layers = listOf(
            GisLayerItem("layer_map_bg", GisLayerType.BASE_MAP, "ম্যাপ", visible = false, opacity = 0.5f, zOrder = 0),
            GisLayerItem("layer_grid", GisLayerType.GRID, "গ্রিড", visible = true, opacity = 0.8f, zOrder = 1)
        )
        val printCfg = PrintConfig(
            areaUnitId = AreaDisplayUnit.SQ_M.id,
            mapContentMode = "selected",
            showVertexNumbers = true,
            plotFillOpacity = 0.45f,
            labelCollisionAvoidance = false,
            lineColorMode = PrintLineColorMode.BLUE,
            showPerimeterLabel = false,
            showTotalAreaLabel = false,
            showSelectedAreaLabel = false,
            scaleBarUnitId = "meters",
            northArrowSizePercent = 130
        )

        val pad = SurveyPadFidelity.fromCanvas(
            canvas = canvas,
            layers = layers,
            sqFeetPerShotok = 432.0,
            printConfig = printCfg
        )

        assertEquals(MeasurementUnit.METERS.id, pad.unitId)
        assertEquals(3, pad.decimalPrecision)
        assertEquals(432.0, pad.sqFeetPerShotok, 1e-6)
        assertEquals(AreaDisplayUnit.SQ_M.id, pad.areaUnitId)
        assertEquals("#C23A32", pad.measurementColorHex)
        assertEquals(1.4f, pad.measurementStrokeScale, 1e-4f)
        assertEquals(1.25f, pad.fontScale, 1e-4f)
        assertEquals(1.5f, pad.lineScale, 1e-4f)
        assertEquals(0.6f, pad.outlineScale, 1e-4f)
        assertEquals(1.3f, pad.pointScale, 1e-4f)
        assertFalse(pad.showMeasurementsOutside)
        assertTrue(pad.showSplitTotalInside)
        assertEquals(MapTextPosition.INSIDE.id, pad.mapTextPositionId)
        assertEquals("fixed", pad.zoomTextModeId)
        assertTrue(pad.showGrid)
        assertTrue(pad.showCrosshair)
        assertFalse(pad.showCornerPoints)
        assertTrue(pad.showVertexNumbers)
        assertEquals(0.45f, pad.plotFillOpacity, 1e-4f)
        assertFalse(pad.labelCollisionAvoidance)
        assertEquals("#2563EB", pad.strokeColorHexOverride)
        assertEquals("selected", pad.mapContentMode)
        assertEquals(-0.25f, pad.brightness, 1e-4f)
        assertEquals(0.2f, pad.contrast, 1e-4f)
        assertTrue(pad.grayscale)
        assertEquals(0.7f, pad.mapOpacity, 1e-4f)
        assertFalse(pad.layerVisible("layer_map_bg"))
        assertEquals(0.5f, pad.layerOpacity("layer_map_bg"), 1e-4f)
    }

    @Test
    fun dimensionLabels_ftIns_keepsCanvasTextVerbatim() {
        val plot = sample100x79Plot()
        val project = SurveyProject(
            plots = listOf(plot),
            selectedPlotIds = setOf(plot.id),
            feetPerCanvasUnit = 0.55,
            canvasSettings = CanvasDisplaySettings(measurementUnit = MeasurementUnit.FEET_INCHES.id)
        )
        val data = SurveyReportDataMapper.build(project)
        assertEquals("৫৫' ০\"", data.plots.first().dimensionLabels.first())
    }

    @Test
    fun dimensionLabels_meters_liveRecomputeWithPrecision() {
        val plot = sample100x79Plot()
        val project = SurveyProject(
            plots = listOf(plot),
            selectedPlotIds = setOf(plot.id),
            feetPerCanvasUnit = 0.55,
            canvasSettings = CanvasDisplaySettings(
                measurementUnit = MeasurementUnit.METERS.id,
                decimalPrecision = 2
            )
        )
        val data = SurveyReportDataMapper.build(project)
        // 100 * 0.55 = 55 ft = 16.764 m -> ১৬.৭৬ মি
        assertEquals("১৬.৭৬ মি", data.plots.first().dimensionLabels.first())
    }

    @Test
    fun areaLabels_shotokFactor_recomputedLiveOnPaper() {
        val basePlot = GeometryUtils.buildPlot(
            id = "p4356",
            name = "প্লট ১",
            colorHex = "#2563EB",
            points = listOf(PointF(0f, 0f), PointF(100f, 0f), PointF(100f, 43.56f), PointF(0f, 43.56f)),
            feetPerCanvasUnit = 1.0,
            sqFeetPerShotok = 435.6
        )
        listOf(
            435.6 to "১০.০০ শতক",
            432.0 to "১০.০৮ শতক",
            430.0 to "১০.১৩ শতক"
        ).forEach { (factor, expected) ->
            val project = SurveyProject(
                plots = listOf(basePlot),
                selectedPlotIds = setOf(basePlot.id),
                feetPerCanvasUnit = 1.0,
                sqFeetPerShotok = factor
            )
            val data = SurveyReportDataMapper.build(project)
            assertEquals(expected, data.plots.first().areaLabel)
        }
    }

    @Test
    fun totalArea_perimeter_labelsFollowTogglesAndUnits() {
        val plot = sample100x79Plot()
        val project = SurveyProject(
            plots = listOf(plot),
            selectedPlotIds = setOf(plot.id),
            feetPerCanvasUnit = 0.55,
            canvasSettings = CanvasDisplaySettings(measurementUnit = MeasurementUnit.FEET.id, decimalPrecision = 1)
        )
        val data = SurveyReportDataMapper.build(
            project = project,
            printConfig = PrintConfig(showPerimeterLabel = false, showTotalAreaLabel = false)
        )
        assertFalse(data.pad.showPerimeterLabel)
        assertFalse(data.pad.showTotalAreaLabel)
        assertTrue(data.totalPerimeterLabel.startsWith("১৯৮"))
    }

    @Test
    fun perPlotLabelStyle_transfersToPaper() {
        val style = PlotLabelStyle(
            fontSizeScale = 1.6f,
            bold = false,
            italic = true,
            underline = true,
            colorHex = "#DC2626",
            backgroundHex = "#FEF08A",
            backgroundOpacity = 0.75f,
            opacity = 0.9f,
            letterSpacingSp = 1.5f,
            rotationDeg = 45f,
            offsetX = 12f,
            offsetY = -8f,
            manuallyPositioned = true,
            locked = true
        )
        val plot = sample100x79Plot().copy(labelStyle = style)
        val project = SurveyProject(
            plots = listOf(plot),
            selectedPlotIds = setOf(plot.id),
            feetPerCanvasUnit = 0.55
        )
        val data = SurveyReportDataMapper.build(project)
        val mapped = data.plots.first().labelStyle
        assertEquals(ReportLabelStyle.from(style), mapped)
        assertEquals(45f, mapped.rotationDeg, 1e-4f)
        assertTrue(mapped.italic)
        assertEquals("#DC2626", mapped.colorHex)
    }

    @Test
    fun pageLayout_resolvesPaperOrientationMargin() {
        val a4 = PageLayout.from(PrintConfig(paperSize = PaperSize.A4, orientation = PrintOrientation.PORTRAIT, marginMm = 12f))
        assertEquals(595f, a4.widthPt, 0.5f)
        assertEquals(842f, a4.heightPt, 0.5f)
        assertEquals(34f, a4.marginPt, 0.5f)

        val a3Land = PageLayout.from(PrintConfig(paperSize = PaperSize.A3, orientation = PrintOrientation.LANDSCAPE, marginMm = 12f))
        assertEquals(1191f, a3Land.widthPt, 0.5f)
        assertEquals(842f, a3Land.heightPt, 0.5f)

        val custom = PageLayout.from(PrintConfig(paperSize = PaperSize.CUSTOM, customPaperWidthPt = 800f, customPaperHeightPt = 1000f))
        assertEquals(800f, custom.widthPt, 0.5f)
        assertEquals(1000f, custom.heightPt, 0.5f)
    }

    @Test
    fun buildFileName_mapNameTitleDate_bengaliSafe() {
        val name = PrintPageMath.buildFileName("পশ্চিম/রাউজান", "ডিজিটাল জরিপ", "১৮/১১/২০২৬")
        assertTrue(name.contains("পশ্চিম_রাউজান"))
        assertFalse(name.contains("/"))

        val fallback = PrintPageMath.buildFileName("", "", "")
        assertEquals("Mouza_Map_Survey_Report", fallback)
    }

    @Test
    fun pageNumberLabel_bengaliDigits() {
        assertEquals("পৃষ্ঠা ২/৩", PrintPageMath.pageNumberLabel(2, 3))
        assertEquals("", PrintPageMath.pageNumberLabel(1, 1))
    }

    @Test
    fun renderPreviewBitmap_matchesPageLayoutGeometry() {
        val plot = sample100x79Plot()
        val project = SurveyProject(plots = listOf(plot), selectedPlotIds = setOf(plot.id), feetPerCanvasUnit = 0.55)
        val data = SurveyReportDataMapper.build(project)
        val layout = PageLayout.from(PrintConfig(paperSize = PaperSize.A3, orientation = PrintOrientation.LANDSCAPE))
        val bmp = BengaliSurveyReportRenderer.renderPreviewBitmap(
            data = data,
            template = ReportTemplate.STANDARD,
            typeface = Typeface.DEFAULT,
            pageLayout = layout
        )
        assertEquals(1191, bmp.width)
        assertEquals(842, bmp.height)
        bmp.recycle()
    }

    @Test
    fun drawReport_collisionAvoidance_andVertexNumbers_runClean() {
        val plot = sample100x79Plot()
        val project = SurveyProject(plots = listOf(plot), selectedPlotIds = setOf(plot.id), feetPerCanvasUnit = 0.55)
        val data = SurveyReportDataMapper.build(
            project = project,
            printConfig = PrintConfig(showVertexNumbers = true, labelCollisionAvoidance = true)
        )
        val bmp = Bitmap.createBitmap(595, 842, Bitmap.Config.ARGB_8888)
        val telemetry = BengaliSurveyReportRenderer.drawReport(
            canvas = Canvas(bmp),
            pageW = 595f,
            pageH = 842f,
            data = data,
            template = ReportTemplate.STANDARD,
            typeface = Typeface.DEFAULT
        )
        assertTrue(telemetry.dimensionLabelsDrawn >= 4)
        bmp.recycle()
    }

    @Test
    fun bitmapFilters_passthroughAndExtremesNoCrash() {
        val src = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(120, 180, 90))
        }
        val passthrough = BitmapMapFilters.apply(src, 0f, 0f, grayscale = false, opacity = 1f)
        assertSame(src, passthrough)

        val extreme = BitmapMapFilters.apply(src, -0.5f, 0.5f, grayscale = true, opacity = 0.25f)
        assertNotNull(extreme)
        assertNotSame(src, extreme)
        extreme?.recycle()
        src.recycle()
    }

    @Test
    fun printConfig_undoRedo_reset_presets_andExportEndToEnd() {
        runBlocking {
            val vm = LandSurveyViewModel(app, databaseOverride = db)
            vm.initWorkspace("draft_raujan_4")
            withTimeout(15_000) { vm.workspaceReady.first { it } }

            vm.addNewPlot()
            val addedId = vm.uiState.value.plots.first().id
            vm.togglePlotReportSelection(addedId)

            // Undo / Redo PrintConfig
            val initialMargin = vm.printConfig.value.marginMm
            vm.updatePrintConfigUndoable { it.copy(marginMm = 24f) }
            assertEquals(24f, vm.printConfig.value.marginMm, 1e-4f)
            assertTrue(vm.canUndoPrint.value)
            vm.undoPrintChange()
            assertEquals(initialMargin, vm.printConfig.value.marginMm, 1e-4f)
            assertTrue(vm.canRedoPrint.value)
            vm.redoPrintChange()
            assertEquals(24f, vm.printConfig.value.marginMm, 1e-4f)

            // Preset DAO roundtrip
            db.printPresetDao().upsert(PrintPresetEntity("pr_1", "মাঠ প্রিসেট", """{"marginMm":18.0}""", 1L, 1L))
            assertEquals(1, db.printPresetDao().getAll().size)
            vm.savePrintPreset("সার্ভে প্যাড স্ট্যান্ডার্ড")
            val presets = withTimeout(5000) {
                vm.printPresets.first { list -> list.any { it.name == "সার্ভে প্যাড স্ট্যান্ডার্ড" } }
            }
            val savedId = presets.first { it.name == "সার্ভে প্যাড স্ট্যান্ডার্ড" }.id
            vm.updatePrintConfigUndoable { it.copy(marginMm = 10f, showVertexNumbers = true, lineColorMode = PrintLineColorMode.BLUE) }
            vm.applyPrintPreset(savedId)
            withTimeout(5000) {
                vm.printConfig.first { kotlin.math.abs(it.marginMm - 24f) < 0.01f }
            }
            vm.deletePrintPreset(savedId)
            withTimeout(5000) {
                vm.printPresets.first { list -> list.none { it.id == savedId } }
            }

            // Reset Print Fidelity to Canvas
            vm.updatePrintConfigUndoable { it.copy(showVertexNumbers = true, lineColorMode = PrintLineColorMode.BLUE) }
            vm.resetPrintFidelityToCanvas()
            assertFalse(vm.printConfig.value.showVertexNumbers)
            assertEquals(PrintLineColorMode.PLOT_OWN, vm.printConfig.value.lineColorMode)

            // Multi-page progress & cancel
            val data = vm.buildSurveyReportData()
            val progressEvents = mutableListOf<Pair<Int, Int>>()
            val outMulti = File(app.cacheDir, "multi_fid.pdf")
            val multiRes = ReportPdfExporter.generateSurveyPadPdf(
                context = app,
                data = data,
                template = ReportTemplate.STANDARD,
                typeface = Typeface.DEFAULT,
                printConfig = PrintConfig(individualPlotPages = true),
                outFile = outMulti,
                onProgress = { p, t -> progressEvents.add(p to t) }
            )
            assertTrue(multiRes.isSuccess)
            assertTrue(progressEvents.isNotEmpty())
            assertEquals(2, progressEvents.first().second)
            outMulti.delete()

            val cancelOut = File(app.cacheDir, "cancelled_fid.pdf")
            val cancelFlag = AtomicBoolean(true)
            val cancelRes = ReportPdfExporter.generateSurveyPadPdf(
                context = app,
                data = data,
                template = ReportTemplate.STANDARD,
                typeface = Typeface.DEFAULT,
                printConfig = PrintConfig(individualPlotPages = true),
                outFile = cancelOut,
                isCancelled = cancelFlag
            )
            assertTrue(cancelRes.isFailure)
            assertEquals(ReportPdfExporter.CANCELLED_MESSAGE_BN, cancelRes.exceptionOrNull()?.message)
            assertFalse(cancelOut.exists())

            // End-to-end ViewModel export
            var exportedFile: File? = null
            vm.exportSurveyPadPdf(app) { file -> exportedFile = file }
            withTimeout(15_000) {
                vm.printJobState.first { !it.isExporting && it.lastExportMessageBn != null }
            }
            assertNotNull(exportedFile)
            assertTrue(exportedFile!!.exists())
            exportedFile?.delete()
        }
    }
}
