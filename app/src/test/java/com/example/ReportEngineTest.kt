package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Typeface
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CanvasDisplaySettings
import com.example.data.model.MeasurementUnit
import com.example.data.model.PlotLabelStyle
import com.example.data.model.PointF
import com.example.data.model.PrintConfig
import com.example.data.model.SurveyProject
import com.example.domain.BengaliFormatters
import com.example.domain.GeometryUtils
import com.example.domain.report.BengaliSurveyReportRenderer
import com.example.domain.report.PageLayout
import com.example.domain.report.RenderTelemetry
import com.example.domain.report.ReportBounds
import com.example.domain.report.ReportMapTransform
import com.example.domain.report.ReportPdfExporter
import com.example.domain.report.ReportTemplate
import com.example.domain.report.ReportTicket
import com.example.domain.report.SurveyReportDataMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReportEngineTest {

    private fun samplePlot(id: String = "plot_1") = GeometryUtils.buildPlot(
        id = id,
        name = "প্লট ১",
        colorHex = "#2563EB",
        points = listOf(
            PointF(0f, 0f),
            PointF(100f, 0f),
            PointF(100f, 80f),
            PointF(0f, 80f)
        ),
        feetPerCanvasUnit = 0.55,
        sqFeetPerShotok = 435.6,
        dagNumber = "১০১"
    )

    @Test
    fun feetInchesReferenceFormat_matchesSpec() {
        val formatted = BengaliFormatters.formatFtInches(101.92)
        assertEquals("১০১' ১১\"", formatted)
    }

    @Test
    fun reportMapTransform_fitsBoundsInsideRect() {
        val bounds = ReportBounds(10f, 20f, 210f, 120f)
        val tf = ReportMapTransform.compute(bounds, 50f, 100f, 400f, 300f)
        val p1 = tf.worldToCanvas(PointF(10f, 20f))
        val p2 = tf.worldToCanvas(PointF(210f, 120f))
        assertTrue(p1.x >= 50f && p1.y >= 100f)
        assertTrue(p2.x <= 450f && p2.y <= 400f)
    }

    @Test
    fun allFiveTemplates_render595x842_andHaveInvariantTelemetry() {
        val plot = samplePlot()
        val project = SurveyProject(
            id = "p_test",
            title = "টেস্ট",
            mouzaName = "পশ্চিম রাউজান",
            plots = listOf(plot),
            selectedPlotIds = setOf(plot.id),
            feetPerCanvasUnit = 0.55,
            sqFeetPerShotok = 435.6
        )
        val data = SurveyReportDataMapper.build(project, ReportTicket())
        val telemetries = mutableListOf<RenderTelemetry>()

        ReportTemplate.entries.forEach { template ->
            val bmp = BengaliSurveyReportRenderer.renderPreviewBitmap(
                data = data,
                template = template,
                typeface = Typeface.DEFAULT,
                pageLayout = PageLayout(595f, 842f, 34f)
            )
            assertEquals(595, bmp.width)
            assertEquals(842, bmp.height)
            bmp.recycle()

            val scratch = Bitmap.createBitmap(595, 842, Bitmap.Config.ARGB_8888)
            val telemetry = BengaliSurveyReportRenderer.drawReport(
                canvas = android.graphics.Canvas(scratch),
                pageW = 595f,
                pageH = 842f,
                data = data,
                template = template,
                typeface = Typeface.DEFAULT
            )
            telemetries.add(telemetry)
            scratch.recycle()
        }

        val first = telemetries.first()
        assertEquals(1, first.polygonsDrawn)
        assertEquals(4, first.dimensionLabelsDrawn)
        assertTrue(telemetries.all {
            it.polygonsDrawn == first.polygonsDrawn &&
                it.dimensionLabelsDrawn == first.dimensionLabelsDrawn &&
                it.areaTableRowsDrawn == first.areaTableRowsDrawn
        })
    }

    @Test
    fun dimensionLabels_useSameConversionAsMapCanvas() {
        val plot = samplePlot()
        val project = SurveyProject(
            id = "p_dim",
            plots = listOf(plot),
            selectedPlotIds = setOf(plot.id),
            feetPerCanvasUnit = 0.55,
            sqFeetPerShotok = 435.6,
            canvasSettings = CanvasDisplaySettings(measurementUnit = MeasurementUnit.FEET_INCHES.id)
        )
        val data = SurveyReportDataMapper.build(project)
        val expectedCanvasTexts = plot.segments.map { it.formattedLength }
        assertEquals(expectedCanvasTexts, data.plots.first().dimensionLabels)
    }

    @Test
    fun pdfExport_writesValidPdfHeader_andBengaliDateMatchesRegex() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val today = BengaliFormatters.bengaliToday()
        assertTrue(Regex("[০-৯]{2}/[০-৯]{2}/[০-৯]{4}").matches(today))

        val plot = samplePlot()
        val project = SurveyProject(
            id = "p_pdf",
            plots = listOf(plot),
            selectedPlotIds = setOf(plot.id),
            feetPerCanvasUnit = 0.55,
            sqFeetPerShotok = 435.6
        )
        val data = SurveyReportDataMapper.build(project)
        val out = File(context.cacheDir, "report_engine_test.pdf")
        if (out.exists()) out.delete()

        val result = ReportPdfExporter.generateSurveyPadPdf(
            context = context,
            data = data,
            template = ReportTemplate.STANDARD,
            typeface = Typeface.DEFAULT,
            printConfig = PrintConfig(),
            outFile = out
        )
        assertTrue(result.isSuccess)
        assertTrue(out.exists())
        val bytes = out.readBytes()
        assertTrue(bytes.size > 100)
        assertEquals("%PDF-", String(bytes.copyOfRange(0, 5)))
        out.delete()
    }
}
