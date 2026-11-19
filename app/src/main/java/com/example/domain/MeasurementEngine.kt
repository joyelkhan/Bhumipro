package com.example.domain

import com.example.data.model.BoundarySegment
import com.example.data.model.PointF
import com.example.data.model.UnitSanity

data class PlotGeometryMetrics(
    val areaShotangsho: Double = 0.0,
    val areaSqFt: Double = 0.0,
    val areaSqMeters: Double = 0.0,
    val perimeterFeet: Double = 0.0,
    val frontageFeet: Double = 0.0,
    val depthFeet: Double = 0.0,
    val avgSideFeet: Double = 0.0,
    val isValidGeometry: Boolean = false,
    val isClockwise: Boolean = false,
    val segments: List<BoundarySegment> = emptyList()
)

object MeasurementEngine {

    fun computePlotMetrics(
        points: List<PointF>,
        feetPerCanvasUnit: Double,
        sqFeetPerShotok: Double = UnitSanity.DEFAULT_SQ_FEET_PER_SHOTOK
    ): PlotGeometryMetrics {
        if (points.size < 3) {
            return PlotGeometryMetrics(
                segments = GeometryUtils.buildSegments(points, feetPerCanvasUnit)
            )
        }
        val scale = feetPerCanvasUnit.coerceAtLeast(0.0)
        val factor = sqFeetPerShotok.coerceAtLeast(1.0)
        val signedUnitsSq = GeometryUtils.signedAreaUnitsSq(points)
        val rawSqFt = kotlin.math.abs(signedUnitsSq) * scale * scale
        val rawShotok = rawSqFt / factor
        val rawSqM = rawSqFt / UnitSanity.SQ_FEET_PER_SQ_METER

        val segments = GeometryUtils.buildSegments(points, scale)
        val perimeter = segments.sumOf { it.lengthFeet }
        val (frontage, depth) = GeometryUtils.minBoundingBoxDimensionsFeet(points, scale)
        val avgSide = if (segments.isNotEmpty()) perimeter / segments.size else 0.0
        val validation = JtsDivisionEngine.validatePolygon(points)

        return PlotGeometryMetrics(
            areaShotangsho = BengaliFormatters.roundToDecimals(rawShotok, 2),
            areaSqFt = BengaliFormatters.roundToDecimals(rawSqFt, 2),
            areaSqMeters = BengaliFormatters.roundToDecimals(rawSqM, 2),
            perimeterFeet = BengaliFormatters.roundToDecimals(perimeter, 2),
            frontageFeet = BengaliFormatters.roundToDecimals(frontage, 2),
            depthFeet = BengaliFormatters.roundToDecimals(depth, 2),
            avgSideFeet = BengaliFormatters.roundToDecimals(avgSide, 2),
            isValidGeometry = validation.isValid,
            isClockwise = signedUnitsSq > 0.0,
            segments = segments
        )
    }
}
