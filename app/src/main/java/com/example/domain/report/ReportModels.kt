package com.example.domain.report

import com.example.data.model.AreaDisplayUnit
import com.example.data.model.CanvasDisplaySettings
import com.example.data.model.GisLayerItem
import com.example.data.model.LandPlot
import com.example.data.model.LengthFormatter
import com.example.data.model.MeasurementUnit
import com.example.data.model.PointF
import com.example.data.model.PrintConfig
import com.example.data.model.SurveyProject
import com.example.domain.BengaliFormatters
import com.example.domain.GeometryUtils
import kotlin.math.max
import kotlin.math.min

enum class ReportTemplate(val id: String, val titleBn: String, val subtitleBn: String) {
    STANDARD("standard", "স্ট্যান্ডার্ড সার্ভে প্যাড", "ক্লাসিক নীল ও সবুজ রিপোর্ট"),
    DIMENSION("dimension", "ডিমেনশন ফোকাসড", "বিস্তারিত বাহু ও পরিমাপ"),
    ROYAL("royal", "রয়্যাল অফিশিয়াল", "গাঢ় নেভি ও সোনালী ফ্রেম"),
    LAND_MEASUREMENT("land_measurement", "ভূমি পরিমাপ চিঠা", "দাগ, খতিয়ান ও সীমানা টেবিল"),
    CUSTOM("custom", "কাস্টম প্যাড", "নিজস্ব কনফিগারেশন অনুযায়ী")
}

data class ReportTicket(
    val companyName: String = "নাসেরএক্স ল্যাবস (NaserX Labs)",
    val title: String = "ডিজিটাল ডিমার্কেশন",
    val footerLine: String = "ভূমি প্রো (BhumiPro) দিয়ে প্রস্তুত",
    val surveyorName: String = "Abu Naser Khan • NaserX Labs",
    val surveyorMobile: String = "",
    val address: String = "",
    val registrationNo: String = "",
    val districtField: String = "",
    val logoUri: String? = null,
    val designation: String = "সার্ভেয়ার ও ভূমি পরিমাপক",
    val qualifications: String = "",
    val contactLines: String = "",
    val surveyType: String = "ডিজিটাল ডিমার্কেশন",
    val upazilaField: String = "",
    val reportName: String = "",
    val slogan: String = "",
    val reportNote: String = "",
    val qrLink: String = "https://naserxlabs.bhumipro.app/verify",
    val website: String = "naserxlabs.com",
    val dateText: String = BengaliFormatters.bengaliToday(),
    val signatureUri: String? = null,
    val disclaimerText: String = "এই ডিজিটাল ডিমার্কেশন রিপোর্টটি কেবল মৌল বি.আর.এস নকশা অনুযায়ী প্রস্তুত — সরকারিভাবে সত্যায়িত নথি নয়।",
    val showHeader: Boolean = true,
    val showFooter: Boolean = true,
    val showNorthArrow: Boolean = true,
    val showWatermark: Boolean = false,
    val watermarkText: String = "BHUMIPRO SURVEY",
    val showQr: Boolean = true,
    val showBarcode: Boolean = true,
    val showLegend: Boolean = true,
    val showAreaSummary: Boolean = true,
    val showScaleBar: Boolean = true,
    val showDate: Boolean = true
)

data class ReportPlot(
    val id: String,
    val name: String,
    val dagNumber: String = "",
    val colorHex: String = "#2563EB",
    val points: List<PointF> = emptyList(),
    val areaSqFt: Double = 0.0,
    val areaShotok: Double = 0.0,
    val areaLabel: String = "",
    val perimeterFeet: Double = 0.0,
    val perimeterLabel: String = "",
    val dimensionLabels: List<String> = emptyList(),
    val isRoad: Boolean = false,
    val frontageFeet: Double = 0.0,
    val depthFeet: Double = 0.0,
    val labelStyle: ReportLabelStyle = ReportLabelStyle()
)

data class ReportBounds(
    val minX: Float = 0f,
    val minY: Float = 0f,
    val maxX: Float = 100f,
    val maxY: Float = 100f
) {
    val width: Float get() = (maxX - minX).coerceAtLeast(1f)
    val height: Float get() = (maxY - minY).coerceAtLeast(1f)
}

data class ReportMapTransform(
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float,
    val mapRectLeft: Float,
    val mapRectTop: Float,
    val mapRectWidth: Float,
    val mapRectHeight: Float
) {
    fun worldToCanvas(p: PointF): PointF =
        PointF(p.x * scale + offsetX, p.y * scale + offsetY)

    fun feetPerPaperInch(feetPerCanvasUnit: Double): Double {
        if (scale <= 1e-6f) return 0.0
        val canvasUnitsPerInch = 72.0 / scale.toDouble()
        return canvasUnitsPerInch * feetPerCanvasUnit
    }

    companion object {
        fun compute(
            bounds: ReportBounds,
            rectLeft: Float,
            rectTop: Float,
            rectWidth: Float,
            rectHeight: Float,
            paddingFraction: Float = 0.08f
        ): ReportMapTransform {
            val pad = paddingFraction.coerceIn(0.01f, 0.25f)
            val usableW = (rectWidth * (1f - pad * 2f)).coerceAtLeast(10f)
            val usableH = (rectHeight * (1f - pad * 2f)).coerceAtLeast(10f)
            val scale = min(usableW / bounds.width, usableH / bounds.height).coerceAtLeast(0.0001f)
            val contentW = bounds.width * scale
            val contentH = bounds.height * scale
            val offsetX = rectLeft + (rectWidth - contentW) * 0.5f - bounds.minX * scale
            val offsetY = rectTop + (rectHeight - contentH) * 0.5f - bounds.minY * scale
            return ReportMapTransform(
                scale = scale,
                offsetX = offsetX,
                offsetY = offsetY,
                mapRectLeft = rectLeft,
                mapRectTop = rectTop,
                mapRectWidth = rectWidth,
                mapRectHeight = rectHeight
            )
        }
    }
}

data class SurveyReportData(
    val projectId: String,
    val projectTitle: String,
    val mouzaName: String,
    val jlNumber: String,
    val district: String,
    val upazila: String,
    val scaleText: String,
    val feetPerCanvasUnit: Double,
    val isEmptySelection: Boolean,
    val plots: List<ReportPlot>,
    val landPlots: List<ReportPlot>,
    val roadPlots: List<ReportPlot>,
    val totalAreaSqFt: Double,
    val totalAreaLabel: String,
    val totalPerimeterLabel: String,
    val pad: SurveyPadFidelity,
    val ticket: ReportTicket,
    val bounds: ReportBounds
)

object SurveyReportDataMapper {

    fun formatAreaValue(
        areaSqFt: Double,
        areaUnit: AreaDisplayUnit,
        sqFeetPerShotok: Double
    ): String = LengthFormatter.formatAreaValue(
        areaSqFt = areaSqFt,
        areaUnit = areaUnit,
        sqFeetPerShotok = sqFeetPerShotok,
        useBengali = true
    )

    fun build(
        project: SurveyProject,
        ticket: ReportTicket = ReportTicket(),
        includePlotIds: Set<String> = project.selectedPlotIds,
        canvasSettings: CanvasDisplaySettings = project.canvasSettings,
        layers: List<GisLayerItem> = project.layers,
        printConfig: PrintConfig = PrintConfig()
    ): SurveyReportData {
        val pad = SurveyPadFidelity.fromCanvas(
            canvas = canvasSettings,
            layers = layers,
            sqFeetPerShotok = project.sqFeetPerShotok,
            printConfig = printConfig
        )
        val unit = pad.effectiveUnit()
        val precision = pad.decimalPrecision
        val areaUnit = pad.effectiveAreaUnit()
        val factor = pad.sqFeetPerShotok.coerceAtLeast(1.0)

        val explicitlySelected = project.plots.filter { it.id in includePlotIds && !it.isHidden }
        val filteredPlots: List<LandPlot> = if (explicitlySelected.isEmpty()) {
            emptyList()
        } else if (printConfig.mapContentMode == "selected") {
            val active = project.activePlot
            if (active != null && active.id in includePlotIds && !active.isHidden) {
                listOf(active)
            } else {
                listOf(explicitlySelected.first())
            }
        } else {
            explicitlySelected
        }

        val isEmpty = filteredPlots.isEmpty()

        val reportPlots = filteredPlots.map { plot ->
            val dimLabels = buildDimensionLabels(
                plot = plot,
                unit = unit,
                precision = precision,
                feetPerCanvasUnit = project.feetPerCanvasUnit
            )
            val liveShotok = plot.areaSqFt / factor
            val areaLabel = formatAreaValue(plot.areaSqFt, areaUnit, factor)
            val perimeterLabel = LengthFormatter.format(plot.perimeterFeet, unit, precision, useBengali = true)
            val (frontage, depth) = GeometryUtils.minBoundingBoxDimensionsFeet(plot.points, project.feetPerCanvasUnit)

            ReportPlot(
                id = plot.id,
                name = plot.name,
                dagNumber = plot.dagNumber,
                colorHex = plot.colorHex,
                points = plot.points,
                areaSqFt = plot.areaSqFt,
                areaShotok = liveShotok,
                areaLabel = areaLabel,
                perimeterFeet = plot.perimeterFeet,
                perimeterLabel = perimeterLabel,
                dimensionLabels = dimLabels,
                isRoad = plot.isRoad,
                frontageFeet = frontage,
                depthFeet = depth,
                labelStyle = ReportLabelStyle.from(plot.labelStyle)
            )
        }

        val landPlots = reportPlots.filter { !it.isRoad }
        val roadPlots = reportPlots.filter { it.isRoad }
        val totalSqFt = landPlots.sumOf { it.areaSqFt }
        val totalPerimeterFt = landPlots.sumOf { it.perimeterFeet }

        val totalAreaLabel = "মোটঃ ${formatAreaValue(totalSqFt, areaUnit, factor)}"
        val totalPerimeterLabel = LengthFormatter.format(totalPerimeterFt, unit, precision, useBengali = true)

        val allPoints = reportPlots.flatMap { it.points }
        val bounds = if (allPoints.isEmpty()) {
            ReportBounds(0f, 0f, 400f, 300f)
        } else {
            val box = GeometryUtils.boundingBox(allPoints)
            ReportBounds(box.minX, box.minY, box.maxX, box.maxY)
        }

        val effectiveTicket = ticket.copy(
            title = printConfig.headerTitle.ifBlank { ticket.title },
            surveyorName = printConfig.surveyorName.ifBlank { ticket.surveyorName },
            showHeader = printConfig.showHeaderAndInfo && ticket.showHeader,
            showFooter = printConfig.showFooterAndPage && ticket.showFooter,
            showNorthArrow = printConfig.showNorthArrow && ticket.showNorthArrow,
            showWatermark = printConfig.showWatermark || ticket.showWatermark,
            watermarkText = printConfig.watermarkText.ifBlank { ticket.watermarkText },
            showQr = printConfig.showQrCode && ticket.showQr,
            showBarcode = printConfig.showBarcode && ticket.showBarcode,
            showLegend = printConfig.showLegend && ticket.showLegend,
            showAreaSummary = printConfig.showAreaSummary && ticket.showAreaSummary,
            showScaleBar = printConfig.showScaleBar && ticket.showScaleBar,
            showDate = printConfig.showDate && ticket.showDate,
            signatureUri = printConfig.signatureUri ?: ticket.signatureUri
        )

        return SurveyReportData(
            projectId = project.id,
            projectTitle = project.title,
            mouzaName = printConfig.mouzaName.ifBlank { project.mouzaName },
            jlNumber = printConfig.jlNumber.ifBlank { project.jlNumber },
            district = project.district,
            upazila = project.upazila,
            scaleText = project.scale,
            feetPerCanvasUnit = project.feetPerCanvasUnit,
            isEmptySelection = isEmpty,
            plots = reportPlots,
            landPlots = landPlots,
            roadPlots = roadPlots,
            totalAreaSqFt = totalSqFt,
            totalAreaLabel = totalAreaLabel,
            totalPerimeterLabel = totalPerimeterLabel,
            pad = pad,
            ticket = effectiveTicket,
            bounds = bounds
        )
    }

    private fun buildDimensionLabels(
        plot: LandPlot,
        unit: MeasurementUnit,
        precision: Int,
        feetPerCanvasUnit: Double
    ): List<String> {
        if (plot.points.size < 2) return emptyList()
        if (unit == MeasurementUnit.FEET_INCHES && plot.segments.size == plot.points.size) {
            return plot.segments.map { seg ->
                if (seg.formattedLength.isNotBlank()) seg.formattedLength
                else LengthFormatter.format(seg.lengthFeet, unit, precision, useBengali = true)
            }
        }
        return plot.points.indices.map { i ->
            val a = plot.points[i]
            val b = plot.points[(i + 1) % plot.points.size]
            val feet = if (i < plot.segments.size && plot.segments[i].lengthFeet > 0.0) {
                plot.segments[i].lengthFeet
            } else {
                GeometryUtils.edgeLengthUnits(a, b) * feetPerCanvasUnit
            }
            LengthFormatter.format(feet, unit, precision, useBengali = true)
        }
    }
}
