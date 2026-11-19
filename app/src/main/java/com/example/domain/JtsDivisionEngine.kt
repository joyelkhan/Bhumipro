package com.example.domain

import com.example.data.model.DivisionPartSummary
import com.example.data.model.DivisionReport
import com.example.data.model.DivisionType
import com.example.data.model.LandPlot
import com.example.data.model.PointF
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.util.AffineTransformation
import org.locationtech.jts.operation.buffer.BufferOp
import org.locationtech.jts.operation.buffer.BufferParameters
import org.locationtech.jts.operation.valid.IsValidOp
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

data class PolygonValidationResult(
    val isValid: Boolean,
    val errorMessageBn: String? = null
)

data class DivisionPreviewResult(
    val plots: List<LandPlot>,
    val report: DivisionReport
)

object JtsDivisionEngine {
    private val gf = GeometryFactory()

    fun toJtsPolygon(points: List<PointF>): Polygon? {
        if (points.size < 3) return null
        val coords = ArrayList<Coordinate>(points.size + 1)
        for (p in points) {
            coords.add(Coordinate(p.x.toDouble(), p.y.toDouble()))
        }
        val first = points.first()
        val last = points.last()
        if (abs(first.x - last.x) > 1e-4f || abs(first.y - last.y) > 1e-4f) {
            coords.add(Coordinate(first.x.toDouble(), first.y.toDouble()))
        }
        return runCatching {
            val ring: LinearRing = gf.createLinearRing(coords.toTypedArray())
            gf.createPolygon(ring)
        }.getOrNull()
    }

    fun fromJtsGeometry(geom: Geometry): List<List<PointF>> {
        val result = mutableListOf<List<PointF>>()
        for (i in 0 until geom.numGeometries) {
            val g = geom.getGeometryN(i)
            if (g is Polygon && !g.isEmpty) {
                val ring = g.exteriorRing.coordinates
                if (ring.size >= 4) {
                    val pts = ring.dropLast(1).map { PointF(it.x.toFloat(), it.y.toFloat()) }
                    if (pts.size >= 3) result.add(pts)
                }
            }
        }
        return result
    }

    fun validatePolygon(points: List<PointF>): PolygonValidationResult {
        if (points.size < 3) {
            return PolygonValidationResult(false, "কমপক্ষে ৩টি শীর্ষবিন্দু প্রয়োজন")
        }
        val area = GeometryUtils.areaUnitsSq(points)
        if (area < 1.0) {
            return PolygonValidationResult(false, "প্লটের ক্ষেত্রফল খুবই ছোট")
        }
        val poly = toJtsPolygon(points)
            ?: return PolygonValidationResult(false, "জ্যামিতি তৈরি করা যায়নি")
        val op = IsValidOp(poly)
        if (!op.isValid) {
            return PolygonValidationResult(false, "প্লটের সীমানা পরস্পরকে ছেদ করেছে")
        }
        return PolygonValidationResult(true, null)
    }

    fun polygonsTouchOrAdjacent(a: List<PointF>, b: List<PointF>, toleranceUnits: Double = 6.0): Boolean {
        val pa = toJtsPolygon(a) ?: return false
        val pb = toJtsPolygon(b) ?: return false
        return runCatching {
            pa.touches(pb) || pa.distance(pb) <= toleranceUnits
        }.getOrDefault(false)
    }

    fun mergePolygons(a: LandPlot, b: LandPlot, feetPerUnit: Double, sqFeetPerShotok: Double): LandPlot? {
        val pa = toJtsPolygon(a.points) ?: return null
        val pb = toJtsPolygon(b.points) ?: return null
        val unionGeom = runCatching { pa.union(pb) }.getOrNull() ?: return null
        val rings = fromJtsGeometry(unionGeom)
        val mergedPoints = rings.maxByOrNull { GeometryUtils.areaUnitsSq(it) } ?: return null
        return GeometryUtils.buildPlot(
            id = a.id,
            name = a.name,
            colorHex = a.colorHex,
            points = mergedPoints,
            feetPerCanvasUnit = feetPerUnit,
            sqFeetPerShotok = sqFeetPerShotok,
            dagNumber = listOf(a.dagNumber, b.dagNumber).filter { it.isNotBlank() }.distinct().joinToString(", "),
            ownerName = a.ownerName.ifBlank { b.ownerName },
            labelStyle = a.labelStyle
        )
    }

    fun dividePlot(
        source: LandPlot,
        divisionType: DivisionType,
        partsCount: Int,
        percentages: List<Double>,
        customTargetShotoks: List<Double>,
        roadOptionId: String,
        roadWidthFeet: Float,
        sweepAngleDeg: Float,
        feetPerCanvasUnit: Double,
        sqFeetPerShotok: Double,
        customRoadPoints: List<PointF> = emptyList()
    ): DivisionPreviewResult {
        val safeScale = feetPerCanvasUnit.coerceAtLeast(0.001)
        val safeFactor = sqFeetPerShotok.coerceAtLeast(1.0)
        val poly = toJtsPolygon(source.points)
        if (poly == null || !IsValidOp(poly).isValid) {
            return DivisionPreviewResult(
                plots = emptyList(),
                report = DivisionReport(warningMessage = "বৈধ পলিগন নয় — বিভাজন সম্ভব নয়")
            )
        }

        val count = partsCount.coerceIn(2, 24)
        val weights: List<Double> = when (divisionType) {
            DivisionType.EQUAL -> List(count) { 1.0 / count }
            DivisionType.PERCENTAGE -> {
                val slice = percentages.take(count)
                val sum = slice.sum()
                if (slice.size != count || abs(sum - 100.0) > 0.05) {
                    return DivisionPreviewResult(
                        emptyList(),
                        DivisionReport(warningMessage = "শতাংশের যোগফল ১০০% হতে হবে (বর্তমান ${BengaliFormatters.formatDecimal(sum, 2)}%)")
                    )
                }
                slice.map { (it / 100.0).coerceAtLeast(0.0001) }
            }
            DivisionType.CUSTOM_AREA -> {
                val targets = customTargetShotoks.take(count - 1).map { it.coerceAtLeast(0.0) }
                if (targets.any { it <= 0.0 }) {
                    return DivisionPreviewResult(
                        emptyList(),
                        DivisionReport(warningMessage = "প্রতিটি নির্ধারিত অংশের শতক ০-এর বেশি হতে হবে")
                    )
                }
                val totalTargetSqFt = targets.sum() * safeFactor
                val origSqFt = poly.area * safeScale * safeScale
                val estRoadSqFt = estimateRoadAreaUnitsSq(poly, roadOptionId, (roadWidthFeet / safeScale), customRoadPoints) * safeScale * safeScale
                val netSqFt = (origSqFt - estRoadSqFt).coerceAtLeast(1.0)
                if (totalTargetSqFt >= netSqFt * 0.999) {
                    return DivisionPreviewResult(
                        emptyList(),
                        DivisionReport(warningMessage = "নির্ধারিত ক্ষেত্রফল অবশিষ্ট নেট জমির চেয়ে বেশি")
                    )
                }
                val residualSqFt = (netSqFt - totalTargetSqFt).coerceAtLeast(0.01)
                val allSqFt = targets.map { it * safeFactor } + residualSqFt
                val sumSqFt = allSqFt.sum()
                allSqFt.map { it / sumSqFt }
            }
        }

        val roadWidthUnits = (roadWidthFeet.toDouble() / safeScale).coerceAtLeast(0.0)
        val roadGeoms = if (customRoadPoints.size >= 2) {
            buildPolylineRoadCorridor(customRoadPoints, roadWidthUnits)
        } else {
            buildRoadStrips(poly, roadOptionId, roadWidthUnits)
        }
        var netGeom: Geometry = poly
        val actualRoads = mutableListOf<Geometry>()
        for (strip in roadGeoms) {
            val clippedRoad = runCatching { poly.intersection(strip) }.getOrNull()
            if (clippedRoad != null && !clippedRoad.isEmpty && clippedRoad.area > 1e-3) {
                actualRoads.add(clippedRoad)
                netGeom = runCatching { netGeom.difference(clippedRoad) }.getOrDefault(netGeom)
            }
        }

        val verticalSplit = when {
            customRoadPoints.size >= 2 -> {
                val dx = abs(customRoadPoints.last().x - customRoadPoints.first().x)
                val dy = abs(customRoadPoints.last().y - customRoadPoints.first().y)
                dx >= dy
            }
            else -> when (roadOptionId) {
                "none_vertical", "middle_horizontal", "side_top", "side_bottom", "t_bottom", "t_top", "both_sides_transverse" -> true
                "none_horizontal", "middle_vertical", "side_left", "side_right", "t_left", "t_right", "both_sides_longitudinal" -> false
                else -> true
            }
        }
        val angleDeg = if (sweepAngleDeg != 0f) sweepAngleDeg.toDouble() else if (verticalSplit) 0.0 else 90.0

        val splitGeoms = splitGeometryAcrossComponents(netGeom, weights, angleDeg)
        val resultPlots = mutableListOf<LandPlot>()
        val summaries = mutableListOf<DivisionPartSummary>()

        val origSqFt = poly.area * safeScale * safeScale
        val totalRoadSqFt = actualRoads.sumOf { it.area } * safeScale * safeScale
        val netSqFt = (origSqFt - totalRoadSqFt).coerceAtLeast(0.0)

        var partCounter = 0
        splitGeoms.forEach { geom ->
            val rings = fromJtsGeometry(geom)
            rings.forEach { ring ->
                partCounter += 1
                val partName = "${source.name}.${BengaliFormatters.toBengaliDigits(partCounter)}"
                val plot = GeometryUtils.buildPlot(
                    id = GeometryUtils.generatePlotId(),
                    name = partName,
                    colorHex = GeometryUtils.plotColorForIndex(partCounter - 1),
                    points = ring,
                    feetPerCanvasUnit = safeScale,
                    sqFeetPerShotok = safeFactor,
                    isRoad = false,
                    dagNumber = source.dagNumber,
                    ownerName = source.ownerName
                )
                resultPlots.add(plot)
                val pct = if (netSqFt > 0) (plot.areaSqFt / netSqFt) * 100.0 else 0.0
                summaries.add(
                    DivisionPartSummary(
                        name = partName,
                        areaSqFt = plot.areaSqFt,
                        areaShotok = plot.areaShotangsho,
                        percentage = BengaliFormatters.roundToDecimals(pct, 2)
                    )
                )
            }
        }

        actualRoads.forEachIndexed { idx, rGeom ->
            fromJtsGeometry(rGeom).forEach { ring ->
                val roadPlot = GeometryUtils.buildPlot(
                    id = GeometryUtils.generatePlotId(),
                    name = "রাস্তা ${BengaliFormatters.toBengaliDigits(idx + 1)}",
                    colorHex = "#64748B",
                    points = ring,
                    feetPerCanvasUnit = safeScale,
                    sqFeetPerShotok = safeFactor,
                    isRoad = true
                )
                resultPlots.add(roadPlot)
            }
        }

        val report = DivisionReport(
            originalAreaSqFt = BengaliFormatters.roundToDecimals(origSqFt, 2),
            originalAreaShotok = BengaliFormatters.roundToDecimals(origSqFt / safeFactor, 2),
            roadAreaSqFt = BengaliFormatters.roundToDecimals(totalRoadSqFt, 2),
            roadAreaShotok = BengaliFormatters.roundToDecimals(totalRoadSqFt / safeFactor, 2),
            netAreaSqFt = BengaliFormatters.roundToDecimals(netSqFt, 2),
            netAreaShotok = BengaliFormatters.roundToDecimals(netSqFt / safeFactor, 2),
            parts = summaries
        )
        return DivisionPreviewResult(resultPlots, report)
    }

    fun polylineLengthFeet(points: List<PointF>, feetPerCanvasUnit: Double): Double {
        if (points.size < 2) return 0.0
        var sum = 0.0
        for (i in 0 until points.size - 1) {
            sum += hypot((points[i + 1].x - points[i].x).toDouble(), (points[i + 1].y - points[i].y).toDouble())
        }
        return sum * feetPerCanvasUnit
    }

    fun defaultRoadPolylineForPlot(points: List<PointF>): List<PointF> {
        if (points.size < 3) return emptyList()
        val box = GeometryUtils.boundingBox(points)
        val cx = (box.minX + box.maxX) * 0.5f
        val cy = (box.minY + box.maxY) * 0.5f
        val pad = max(box.width, box.height) * 0.08f
        return if (box.width >= box.height) {
            listOf(
                PointF(box.minX - pad, cy),
                PointF(cx, cy),
                PointF(box.maxX + pad, cy)
            )
        } else {
            listOf(
                PointF(cx, box.minY - pad),
                PointF(cx, cy),
                PointF(cx, box.maxY + pad)
            )
        }
    }

    private fun buildPolylineRoadCorridor(points: List<PointF>, widthUnits: Double): List<Geometry> {
        if (points.size < 2 || widthUnits <= 0.01) return emptyList()
        val coords = points.map { Coordinate(it.x.toDouble(), it.y.toDouble()) }.toTypedArray()
        val line = runCatching { gf.createLineString(coords) }.getOrNull() ?: return emptyList()
        val params = BufferParameters().apply {
            endCapStyle = BufferParameters.CAP_FLAT
            joinStyle = BufferParameters.JOIN_ROUND
        }
        val buffered = runCatching { BufferOp.bufferOp(line, widthUnits * 0.5, params) }.getOrNull()
        return if (buffered != null && !buffered.isEmpty) listOf(buffered) else emptyList()
    }

    private fun estimateRoadAreaUnitsSq(
        poly: Polygon,
        roadOptionId: String,
        roadWidthUnits: Double,
        customRoadPoints: List<PointF> = emptyList()
    ): Double {
        if (roadWidthUnits <= 0.0) return 0.0
        val strips = if (customRoadPoints.size >= 2) {
            buildPolylineRoadCorridor(customRoadPoints, roadWidthUnits)
        } else {
            buildRoadStrips(poly, roadOptionId, roadWidthUnits)
        }
        return strips.sumOf { strip ->
            runCatching { poly.intersection(strip).area }.getOrDefault(0.0)
        }
    }

    private fun buildRoadStrips(poly: Polygon, roadOptionId: String, w: Double): List<Geometry> {
        if (w <= 0.01 || roadOptionId == "none_vertical" || roadOptionId == "none_horizontal") return emptyList()
        val env = poly.envelopeInternal
        val minX = env.minX - 20.0
        val maxX = env.maxX + 20.0
        val minY = env.minY - 20.0
        val maxY = env.maxY + 20.0
        val midX = (env.minX + env.maxX) * 0.5
        val midY = (env.minY + env.maxY) * 0.5
        val hw = w * 0.5

        fun hBand(y0: Double, y1: Double): Polygon = rectPoly(minX, y0, maxX, y1)
        fun vBand(x0: Double, x1: Double): Polygon = rectPoly(x0, minY, x1, maxY)

        return when (roadOptionId) {
            "middle_horizontal" -> listOf(hBand(midY - hw, midY + hw))
            "middle_vertical" -> listOf(vBand(midX - hw, midX + hw))
            "cross" -> listOf(
                runCatching { hBand(midY - hw, midY + hw).union(vBand(midX - hw, midX + hw)) }.getOrDefault(hBand(midY - hw, midY + hw))
            )
            "side_top" -> listOf(hBand(env.minY, env.minY + w))
            "side_bottom" -> listOf(hBand(env.maxY - w, env.maxY))
            "side_left" -> listOf(vBand(env.minX, env.minX + w))
            "side_right" -> listOf(vBand(env.maxX - w, env.maxX))
            "t_bottom" -> listOf(
                runCatching { hBand(env.maxY - w, env.maxY).union(vBand(midX - hw, midX + hw)) }.getOrDefault(hBand(env.maxY - w, env.maxY))
            )
            "t_top" -> listOf(
                runCatching { hBand(env.minY, env.minY + w).union(vBand(midX - hw, midX + hw)) }.getOrDefault(hBand(env.minY, env.minY + w))
            )
            "t_left" -> listOf(
                runCatching { vBand(env.minX, env.minX + w).union(hBand(midY - hw, midY + hw)) }.getOrDefault(vBand(env.minX, env.minX + w))
            )
            "t_right" -> listOf(
                runCatching { vBand(env.maxX - w, env.maxX).union(hBand(midY - hw, midY + hw)) }.getOrDefault(vBand(env.maxX - w, env.maxX))
            )
            "l_bottom_left" -> listOf(
                runCatching { hBand(env.maxY - w, env.maxY).union(vBand(env.minX, env.minX + w)) }.getOrDefault(hBand(env.maxY - w, env.maxY))
            )
            "l_bottom_right" -> listOf(
                runCatching { hBand(env.maxY - w, env.maxY).union(vBand(env.maxX - w, env.maxX)) }.getOrDefault(hBand(env.maxY - w, env.maxY))
            )
            "l_top_left" -> listOf(
                runCatching { hBand(env.minY, env.minY + w).union(vBand(env.minX, env.minX + w)) }.getOrDefault(hBand(env.minY, env.minY + w))
            )
            "l_top_right" -> listOf(
                runCatching { hBand(env.minY, env.minY + w).union(vBand(env.maxX - w, env.maxX)) }.getOrDefault(hBand(env.minY, env.minY + w))
            )
            "both_sides_longitudinal" -> listOf(vBand(env.minX, env.minX + w), vBand(env.maxX - w, env.maxX))
            "both_sides_transverse" -> listOf(hBand(env.minY, env.minY + w), hBand(env.maxY - w, env.maxY))
            else -> emptyList()
        }
    }

    private fun rectPoly(x0: Double, y0: Double, x1: Double, y1: Double): Polygon {
        val loX = min(x0, x1)
        val hiX = max(x0, x1)
        val loY = min(y0, y1)
        val hiY = max(y0, y1)
        return gf.createPolygon(
            arrayOf(
                Coordinate(loX, loY),
                Coordinate(hiX, loY),
                Coordinate(hiX, hiY),
                Coordinate(loX, hiY),
                Coordinate(loX, loY)
            )
        )
    }

    private fun splitGeometryAcrossComponents(
        geom: Geometry,
        weights: List<Double>,
        angleDeg: Double
    ): List<Geometry> {
        val polys = (0 until geom.numGeometries)
            .map { geom.getGeometryN(it) }
            .filterIsInstance<Polygon>()
            .filter { !it.isEmpty && it.area > 1e-3 }
            .sortedWith(compareBy({ it.centroid.y }, { it.centroid.x }))

        if (polys.size <= 1 || weights.size < polys.size) {
            return splitGeometryByWeights(geom, weights, angleDeg)
        }

        val totalArea = polys.sumOf { it.area }.coerceAtLeast(1e-9)
        val totalWeight = weights.sum().coerceAtLeast(1e-9)
        val normalizedWeights = weights.map { it / totalWeight }

        // Assign contiguous slices of normalizedWeights to each polygon proportional to area
        val result = mutableListOf<Geometry>()
        var wCursor = 0
        polys.forEachIndexed { pIdx, poly ->
            if (pIdx == polys.lastIndex) {
                val slice = normalizedWeights.subList(wCursor, normalizedWeights.size)
                if (slice.isNotEmpty()) {
                    result.addAll(splitGeometryByWeights(poly, slice, angleDeg))
                } else {
                    result.add(poly)
                }
            } else {
                val remainingPolys = polys.size - 1 - pIdx
                val maxEnd = normalizedWeights.size - remainingPolys
                val targetShare = poly.area / totalArea
                var acc = 0.0
                var end = (wCursor + 1).coerceAtMost(maxEnd)
                while (end < maxEnd && acc + normalizedWeights[end - 1] <= targetShare + 1e-6) {
                    acc += normalizedWeights[end - 1]
                    end++
                }
                val slice = normalizedWeights.subList(wCursor, end)
                wCursor = end
                result.addAll(splitGeometryByWeights(poly, slice, angleDeg))
            }
        }
        return result
    }

    private fun splitGeometryByWeights(
        geom: Geometry,
        weights: List<Double>,
        angleDeg: Double
    ): List<Geometry> {
        if (weights.size <= 1 || geom.isEmpty) return listOf(geom)
        val centroid = geom.centroid
        val rad = Math.toRadians(angleDeg)
        val rotateTo = AffineTransformation.rotationInstance(-rad, centroid.x, centroid.y)
        val rotateBack = AffineTransformation.rotationInstance(rad, centroid.x, centroid.y)
        val working = rotateTo.transform(geom)

        val totalWeight = weights.sum().coerceAtLeast(1e-9)
        var remaining = working
        val pieces = mutableListOf<Geometry>()
        var remainingWeight = totalWeight

        for (i in 0 until weights.size - 1) {
            val w = weights[i]
            val targetFraction = (w / remainingWeight).coerceIn(0.001, 0.999)
            val targetArea = remaining.area * targetFraction
            val env = remaining.envelopeInternal
            var lo = env.minX
            var hi = env.maxX
            var bestCut = (lo + hi) * 0.5
            repeat(32) {
                val mid = (lo + hi) * 0.5
                val leftHalf = rectPoly(env.minX - 10.0, env.minY - 10.0, mid, env.maxY + 10.0)
                val leftGeom = runCatching { remaining.intersection(leftHalf) }.getOrNull()
                val leftArea = leftGeom?.area ?: 0.0
                if (leftArea < targetArea) {
                    lo = mid
                } else {
                    hi = mid
                }
                bestCut = mid
            }
            val cutBox = rectPoly(env.minX - 10.0, env.minY - 10.0, bestCut, env.maxY + 10.0)
            val leftPart = runCatching { remaining.intersection(cutBox) }.getOrNull()
            val rightPart = runCatching { remaining.difference(cutBox) }.getOrNull()
            if (leftPart != null && !leftPart.isEmpty) {
                pieces.add(rotateBack.transform(leftPart))
            }
            if (rightPart != null && !rightPart.isEmpty) {
                remaining = rightPart
            }
            remainingWeight = (remainingWeight - w).coerceAtLeast(1e-6)
        }
        if (!remaining.isEmpty) {
            pieces.add(rotateBack.transform(remaining))
        }
        return pieces
    }
}
