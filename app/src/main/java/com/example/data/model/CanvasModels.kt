package com.example.data.model

import com.example.domain.BengaliFormatters
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PlotLabelStyle(
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
    fun effectiveFontScale(): Float = fontSizeScale.coerceIn(0.4f, 4.0f)
    fun effectiveOpacity(): Float = opacity.coerceIn(0.05f, 1.0f)
    fun effectiveBgOpacity(): Float = backgroundOpacity.coerceIn(0.0f, 1.0f)
    fun effectiveRotationDeg(): Float = rotationDeg.coerceIn(-180f, 180f)
    fun effectiveOffsetX(): Float = offsetClamped(offsetX)
    fun effectiveOffsetY(): Float = offsetClamped(offsetY)

    fun isDefault(): Boolean =
        fontSizeScale == 1.0f &&
            bold &&
            !italic &&
            !underline &&
            colorHex == null &&
            backgroundHex == null &&
            backgroundOpacity == 0.88f &&
            opacity == 1.0f &&
            letterSpacingSp == 0f &&
            rotationDeg == 0f &&
            offsetX == 0f &&
            offsetY == 0f &&
            !manuallyPositioned &&
            !locked

    companion object {
        val DEFAULT = PlotLabelStyle()

        fun offsetClamped(v: Float): Float = v.coerceIn(-1.5f, 1.5f)
    }
}

enum class MeasurementUnit(
    val id: String,
    val bengaliName: String,
    val englishName: String,
    val suffixBn: String
) {
    FEET_INCHES("ft_in", "ফুট-ইঞ্চি", "Feet + Inches", ""),
    FEET("ft", "ফুট", "Feet", "′"),
    METERS("m", "মিটার", "Meters", " মি"),
    CENTIMETERS("cm", "সেন্টিমিটার", "Centimeters", " সেমি"),
    LINKS("link", "লিঙ্ক", "Links", " লিঙ্ক"),
    CHAINS("chain", "চেইন", "Chains", " চেইন"),
    HAT("hat", "হাত", "Hats", " হাত");

    companion object {
        fun fromId(id: String?): MeasurementUnit =
            entries.firstOrNull { it.id == id } ?: FEET_INCHES
    }
}

enum class MapTextPosition(val id: String, val labelBn: String) {
    ABOVE("above", "লাইনের উপরে (প্রস্তাবিত)"),
    BELOW("below", "লাইনের নিচে"),
    INSIDE("inside", "ভিতরে");

    companion object {
        fun fromId(id: String?): MapTextPosition =
            entries.firstOrNull { it.id == id } ?: ABOVE
    }
}

enum class ZoomTextMode(val id: String, val labelBn: String) {
    AUTO("auto", "অটো (প্রস্তাবিত)"),
    ALWAYS("always", "সর্বদা"),
    HIDDEN("hidden", "লুকান");

    companion object {
        fun fromId(id: String?): ZoomTextMode =
            entries.firstOrNull { it.id == id } ?: AUTO
    }
}

enum class AreaDisplayUnit(val id: String, val labelBn: String, val suffixBn: String) {
    SHOTOK("shotok", "শতক", "শতক"),
    SQ_FT("sq_ft", "বর্গফুট", "বর্গফুট"),
    ACRE("acre", "একর", "একর"),
    HECTARE("hectare", "হেক্টর", "হেক্টর"),
    SQ_M("sq_m", "বর্গমিটার", "বর্গমিটার");

    companion object {
        fun fromId(id: String?): AreaDisplayUnit =
            entries.firstOrNull { it.id == id } ?: SHOTOK
    }
}

@JsonClass(generateAdapter = true)
data class CanvasDisplaySettings(
    val measurementUnit: String = MeasurementUnit.FEET_INCHES.id,
    val decimalPrecision: Int = 2,
    val measurementColorHex: String = "#133C2D",
    val measurementStrokeScale: Float = 1.0f,
    val gridColorHex: String = "#133C2D",
    val gridSpacingUnits: Float = 40f,
    val showCrosshair: Boolean = false,
    val showCornerPoints: Boolean = true,
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val grayscale: Boolean = false,
    val mapOpacity: Float = 1f,
    val fontScale: Float = 1.0f,
    val lineScale: Float = 1.0f,
    val outlineScale: Float = 0.3f,
    val pointScale: Float = 1.0f,
    val showMeasurementsOutside: Boolean = true,
    val showSplitTotalInside: Boolean = false,
    val mapTextPosition: String = MapTextPosition.ABOVE.id,
    val zoomTextMode: String = ZoomTextMode.AUTO.id,
    val zoomPresetPercent: Int = 100
) {
    fun effectiveUnit(): MeasurementUnit = MeasurementUnit.fromId(measurementUnit)
    fun effectivePrecision(): Int = decimalPrecision.coerceIn(0, 3)
    fun effectiveTextPosition(): MapTextPosition = MapTextPosition.fromId(mapTextPosition)
    fun effectiveZoomTextMode(): ZoomTextMode = ZoomTextMode.fromId(zoomTextMode)
    fun effectiveFontScale(): Float = fontScale.coerceIn(0.4f, 3.0f)
    fun effectiveLineScale(): Float = lineScale.coerceIn(0.4f, 3.0f)
    fun effectiveOutlineScale(): Float = outlineScale.coerceIn(0.1f, 2.0f)
    fun effectivePointScale(): Float = pointScale.coerceIn(0.4f, 3.0f)
    fun effectiveMeasurementStrokeScale(): Float = measurementStrokeScale.coerceIn(0.4f, 3.0f)
    fun effectiveGridSpacing(): Float = gridSpacingUnits.coerceIn(8f, 160f)
    fun effectiveBrightness(): Float = brightness.coerceIn(-0.5f, 0.5f)
    fun effectiveContrast(): Float = contrast.coerceIn(-0.5f, 0.5f)
    fun effectiveMapOpacity(): Float = mapOpacity.coerceIn(0.05f, 1.0f)
    fun effectiveZoomPercent(): Int = zoomPresetPercent.coerceIn(15, 1600)

    fun presetIdOrNull(): String? = when {
        kotlin.math.abs(fontScale - 0.6f) < 0.02f &&
            kotlin.math.abs(lineScale - 0.8f) < 0.02f &&
            kotlin.math.abs(outlineScale - 0.3f) < 0.02f &&
            kotlin.math.abs(pointScale - 1.0f) < 0.02f -> "small"
        kotlin.math.abs(fontScale - 0.8f) < 0.02f &&
            kotlin.math.abs(lineScale - 1.0f) < 0.02f &&
            kotlin.math.abs(outlineScale - 0.3f) < 0.02f &&
            kotlin.math.abs(pointScale - 1.0f) < 0.02f -> "normal"
        kotlin.math.abs(fontScale - 1.2f) < 0.02f &&
            kotlin.math.abs(lineScale - 1.0f) < 0.02f &&
            kotlin.math.abs(outlineScale - 0.3f) < 0.02f &&
            kotlin.math.abs(pointScale - 1.0f) < 0.02f -> "medium"
        else -> null
    }

    fun withCadPreset(presetId: String): CanvasDisplaySettings = when (presetId) {
        "small" -> copy(fontScale = 0.6f, lineScale = 0.8f, outlineScale = 0.3f, pointScale = 1.0f)
        "normal" -> copy(fontScale = 0.8f, lineScale = 1.0f, outlineScale = 0.3f, pointScale = 1.0f)
        "medium" -> copy(fontScale = 1.2f, lineScale = 1.0f, outlineScale = 0.3f, pointScale = 1.0f)
        else -> this
    }
}

object UnitSanity {
    const val FEET_PER_METER = 3.280839895
    const val FEET_PER_LINK = 0.66
    const val FEET_PER_CHAIN = 66.0
    const val FEET_PER_HAT = 1.5
    const val LINKS_PER_CHAIN = 100.0
    const val SQ_FEET_PER_SQ_METER = 10.763910416709722
    const val SQ_FEET_PER_ACRE = 43560.0
    const val SQ_FEET_PER_HECTARE = 107639.10416709722
    const val DEFAULT_SQ_FEET_PER_SHOTOK = 435.6
}

object LengthFormatter {
    fun toUnitValue(feet: Double, unit: MeasurementUnit): Double {
        val safeFeet = if (feet.isFinite()) feet else 0.0
        return when (unit) {
            MeasurementUnit.FEET_INCHES -> safeFeet
            MeasurementUnit.FEET -> safeFeet
            MeasurementUnit.METERS -> safeFeet / UnitSanity.FEET_PER_METER
            MeasurementUnit.CENTIMETERS -> (safeFeet / UnitSanity.FEET_PER_METER) * 100.0
            MeasurementUnit.LINKS -> safeFeet / UnitSanity.FEET_PER_LINK
            MeasurementUnit.CHAINS -> safeFeet / UnitSanity.FEET_PER_CHAIN
            MeasurementUnit.HAT -> safeFeet / UnitSanity.FEET_PER_HAT
        }
    }

    fun format(
        feet: Double,
        unit: MeasurementUnit,
        precision: Int = 2,
        useBengali: Boolean = true
    ): String {
        val safePrecision = precision.coerceIn(0, 3)
        return when (unit) {
            MeasurementUnit.FEET_INCHES -> BengaliFormatters.formatFtInches(feet, useBengali)
            else -> {
                val converted = toUnitValue(feet, unit)
                val num = BengaliFormatters.formatDecimal(converted, safePrecision, useBengali)
                "$num${unit.suffixBn}"
            }
        }
    }

    fun formatAreaValue(
        areaSqFt: Double,
        areaUnit: AreaDisplayUnit,
        sqFeetPerShotok: Double = UnitSanity.DEFAULT_SQ_FEET_PER_SHOTOK,
        useBengali: Boolean = true
    ): String {
        val safeSqFt = if (areaSqFt.isFinite() && areaSqFt > 0.0) areaSqFt else 0.0
        val factor = sqFeetPerShotok.coerceAtLeast(1.0)
        return when (areaUnit) {
            AreaDisplayUnit.SHOTOK ->
                "${BengaliFormatters.formatDecimal(safeSqFt / factor, 2, useBengali)} শতক"
            AreaDisplayUnit.SQ_FT ->
                "${BengaliFormatters.formatDecimal(safeSqFt, 1, useBengali)} বর্গফুট"
            AreaDisplayUnit.ACRE ->
                "${BengaliFormatters.formatDecimal(safeSqFt / UnitSanity.SQ_FEET_PER_ACRE, 3, useBengali)} একর"
            AreaDisplayUnit.HECTARE ->
                "${BengaliFormatters.formatDecimal(safeSqFt / UnitSanity.SQ_FEET_PER_HECTARE, 3, useBengali)} হেক্টর"
            AreaDisplayUnit.SQ_M ->
                "${BengaliFormatters.formatDecimal(safeSqFt / UnitSanity.SQ_FEET_PER_SQ_METER, 2, useBengali)} বর্গমিটার"
        }
    }
}
