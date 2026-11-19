package com.example.domain.report

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.example.data.model.AreaDisplayUnit
import com.example.data.model.CanvasDisplaySettings
import com.example.data.model.GisLayerItem
import com.example.data.model.MapTextPosition
import com.example.data.model.MeasurementUnit
import com.example.data.model.PaperSize
import com.example.data.model.PlotLabelStyle
import com.example.data.model.PrintConfig
import com.example.data.model.PrintOrientation
import com.example.domain.BengaliFormatters
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ReportLayerState(
    val id: String,
    val nameBengali: String,
    val visible: Boolean = true,
    val opacity: Float = 1.0f,
    val locked: Boolean = false,
    val zOrder: Int = 0
)

@JsonClass(generateAdapter = true)
data class ReportLabelStyle(
    val fontSizeScale: Float = 1.0f,
    val bold: Boolean = true,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val colorHex: String? = null,
    val backgroundHex: String? = null,
    val backgroundOpacity: Float = 0.88f,
    val opacity: Float = 1.0f,
    val letterSpacingSp: Float = 0f,
    val rotationDeg: Float = 0f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val manuallyPositioned: Boolean = false,
    val locked: Boolean = false
) {
    companion object {
        fun from(style: PlotLabelStyle?): ReportLabelStyle {
            val s = style ?: PlotLabelStyle.DEFAULT
            return ReportLabelStyle(
                fontSizeScale = s.effectiveFontScale(),
                bold = s.bold,
                italic = s.italic,
                underline = s.underline,
                colorHex = s.colorHex,
                backgroundHex = s.backgroundHex,
                backgroundOpacity = s.effectiveBgOpacity(),
                opacity = s.effectiveOpacity(),
                letterSpacingSp = s.letterSpacingSp,
                rotationDeg = s.effectiveRotationDeg(),
                offsetX = s.effectiveOffsetX(),
                offsetY = s.effectiveOffsetY(),
                manuallyPositioned = s.manuallyPositioned,
                locked = s.locked
            )
        }
    }
}

@JsonClass(generateAdapter = true)
data class SurveyPadFidelity(
    val unitId: String = MeasurementUnit.FEET_INCHES.id,
    val decimalPrecision: Int = 2,
    val sqFeetPerShotok: Double = 435.6,
    val areaUnitId: String = AreaDisplayUnit.SHOTOK.id,
    val measurementColorHex: String = "#133C2D",
    val measurementStrokeScale: Float = 1.0f,
    val fontScale: Float = 1.0f,
    val lineScale: Float = 1.0f,
    val outlineScale: Float = 0.3f,
    val pointScale: Float = 1.0f,
    val showMeasurementsOutside: Boolean = true,
    val showSplitTotalInside: Boolean = false,
    val mapTextPositionId: String = MapTextPosition.ABOVE.id,
    val zoomTextModeId: String = "auto",
    val showGrid: Boolean = false,
    val gridColorHex: String = "#133C2D",
    val gridSpacingUnits: Float = 40f,
    val showCrosshair: Boolean = false,
    val showCornerPoints: Boolean = true,
    val showVertexNumbers: Boolean = false,
    val plotFillOpacity: Float = 1.0f,
    val labelCollisionAvoidance: Boolean = true,
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val grayscale: Boolean = false,
    val mapOpacity: Float = 1.0f,
    val mapContentMode: String = "full",
    val showPerimeterLabel: Boolean = true,
    val showTotalAreaLabel: Boolean = true,
    val showSelectedAreaLabel: Boolean = true,
    val scaleBarUnitId: String = "inherit",
    val northArrowSizePercent: Int = 100,
    val strokeColorHexOverride: String? = null,
    val layers: List<ReportLayerState> = emptyList()
) {
    fun effectiveUnit(): MeasurementUnit = MeasurementUnit.fromId(unitId)
    fun effectiveAreaUnit(): AreaDisplayUnit = AreaDisplayUnit.fromId(areaUnitId)
    fun effectiveTextPosition(): MapTextPosition = MapTextPosition.fromId(mapTextPositionId)

    fun layerVisible(id: String): Boolean {
        val layer = layers.firstOrNull { it.id == id } ?: return true
        return layer.visible
    }

    fun layerOpacity(id: String): Float {
        val layer = layers.firstOrNull { it.id == id } ?: return 1.0f
        return layer.opacity.coerceIn(0.05f, 1.0f)
    }

    companion object {
        fun fromCanvas(
            canvas: CanvasDisplaySettings,
            layers: List<GisLayerItem>,
            sqFeetPerShotok: Double,
            showGridOverride: Boolean? = null,
            printConfig: PrintConfig = PrintConfig()
        ): SurveyPadFidelity {
            val gridLayer = layers.firstOrNull { it.id == "layer_grid" }
            val effectiveShowGrid = showGridOverride ?: (gridLayer?.visible ?: false)
            return SurveyPadFidelity(
                unitId = canvas.measurementUnit,
                decimalPrecision = canvas.effectivePrecision(),
                sqFeetPerShotok = sqFeetPerShotok.coerceIn(100.0, 2000.0),
                areaUnitId = printConfig.areaUnitId,
                measurementColorHex = canvas.measurementColorHex,
                measurementStrokeScale = canvas.effectiveMeasurementStrokeScale(),
                fontScale = canvas.effectiveFontScale(),
                lineScale = canvas.effectiveLineScale(),
                outlineScale = canvas.effectiveOutlineScale(),
                pointScale = canvas.effectivePointScale(),
                showMeasurementsOutside = canvas.showMeasurementsOutside,
                showSplitTotalInside = canvas.showSplitTotalInside,
                mapTextPositionId = canvas.mapTextPosition,
                zoomTextModeId = canvas.zoomTextMode,
                showGrid = effectiveShowGrid,
                gridColorHex = canvas.gridColorHex,
                gridSpacingUnits = canvas.effectiveGridSpacing(),
                showCrosshair = canvas.showCrosshair,
                showCornerPoints = canvas.showCornerPoints,
                showVertexNumbers = printConfig.showVertexNumbers,
                plotFillOpacity = printConfig.plotFillOpacity.coerceIn(0f, 1f),
                labelCollisionAvoidance = printConfig.labelCollisionAvoidance,
                brightness = canvas.effectiveBrightness(),
                contrast = canvas.effectiveContrast(),
                grayscale = canvas.grayscale || printConfig.grayscale,
                mapOpacity = canvas.effectiveMapOpacity(),
                mapContentMode = printConfig.mapContentMode,
                showPerimeterLabel = printConfig.showPerimeterLabel,
                showTotalAreaLabel = printConfig.showTotalAreaLabel,
                showSelectedAreaLabel = printConfig.showSelectedAreaLabel,
                scaleBarUnitId = printConfig.scaleBarUnitId,
                northArrowSizePercent = printConfig.northArrowSizePercent.coerceIn(60, 160),
                strokeColorHexOverride = printConfig.lineColorMode.hex,
                layers = layers.sortedBy { it.zOrder }.map {
                    ReportLayerState(
                        id = it.id,
                        nameBengali = it.nameBengali,
                        visible = it.visible,
                        opacity = it.opacity,
                        locked = it.locked,
                        zOrder = it.zOrder
                    )
                }
            )
        }
    }
}

data class PageLayout(
    val widthPt: Float,
    val heightPt: Float,
    val marginPt: Float
) {
    val contentWidth: Float get() = (widthPt - 2f * marginPt).coerceAtLeast(100f)
    val contentHeight: Float get() = (heightPt - 2f * marginPt).coerceAtLeast(100f)

    companion object {
        fun from(config: PrintConfig): PageLayout {
            val baseW: Float
            val baseH: Float
            if (config.paperSize == PaperSize.CUSTOM) {
                baseW = if (config.customPaperWidthPt >= 200f) config.customPaperWidthPt else PaperSize.A4.widthPt
                baseH = if (config.customPaperHeightPt >= 200f) config.customPaperHeightPt else PaperSize.A4.heightPt
            } else {
                baseW = config.paperSize.widthPt
                baseH = config.paperSize.heightPt
            }
            val (w, h) = when (config.orientation) {
                PrintOrientation.PORTRAIT -> if (baseW <= baseH) baseW to baseH else baseH to baseW
                PrintOrientation.LANDSCAPE -> if (baseW < baseH) baseH to baseW else baseW to baseH
            }
            val marginPt = config.marginMm.coerceIn(4f, 30f) * 72f / 25.4f
            return PageLayout(widthPt = w, heightPt = h, marginPt = marginPt)
        }
    }
}

object PrintPageMath {
    private val forbiddenRegex = Regex("[\\\\/:*?\"<>|\\s]+")
    private val multiUnderscore = Regex("_+")

    private fun sanitizePart(raw: String, fallback: String): String {
        val cleaned = raw.trim()
            .replace(forbiddenRegex, "_")
            .replace(multiUnderscore, "_")
            .trim('_')
        return if (cleaned.isBlank()) fallback else cleaned.take(48)
    }

    fun buildFileName(mapName: String, title: String, date: String): String {
        val p1 = sanitizePart(mapName, "Mouza_Map")
        val p2 = sanitizePart(title, "Survey")
        val p3 = sanitizePart(date, "Report")
        return "${p1}_${p2}_${p3}"
    }

    fun pageNumberLabel(pageIndex1Based: Int, totalPages: Int): String {
        if (totalPages <= 1) return ""
        val idx = pageIndex1Based.coerceIn(1, totalPages)
        return "পৃষ্ঠা ${BengaliFormatters.toBengaliDigits(idx)}/${BengaliFormatters.toBengaliDigits(totalPages)}"
    }
}

object BitmapMapFilters {
    fun apply(
        source: Bitmap?,
        brightness: Float,
        contrast: Float,
        grayscale: Boolean,
        opacity: Float
    ): Bitmap? {
        if (source == null || source.isRecycled) return source
        val b = brightness.coerceIn(-0.5f, 0.5f)
        val c = contrast.coerceIn(-0.5f, 0.5f)
        val a = opacity.coerceIn(0f, 1f)
        if (kotlin.math.abs(b) < 1e-4f && kotlin.math.abs(c) < 1e-4f && !grayscale && a >= 0.999f) {
            return source
        }

        val out = Bitmap.createBitmap(
            source.width.coerceAtLeast(1),
            source.height.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(out)
        val cm = ColorMatrix()
        if (grayscale) {
            cm.setSaturation(0f)
        }
        val scale = (1f + c * 1.5f).coerceIn(0.25f, 2.5f)
        val translate = (-0.5f * scale + 0.5f + b) * 255f
        val bcMatrix = ColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        cm.postConcat(bcMatrix)

        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(cm)
            alpha = (a * 255f).toInt().coerceIn(0, 255)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return out
    }
}
