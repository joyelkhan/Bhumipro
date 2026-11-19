package com.example.domain

import com.example.data.model.BoundarySegment
import com.example.data.model.LandPlot
import com.example.data.model.PlotLabelStyle
import com.example.data.model.PointF
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

data class BoundingBox(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float
) {
    val width: Float get() = (maxX - minX).coerceAtLeast(0f)
    val height: Float get() = (maxY - minY).coerceAtLeast(0f)
    val centerX: Float get() = (minX + maxX) / 2f
    val centerY: Float get() = (minY + maxY) / 2f

    fun intersects(other: BoundingBox): Boolean =
        minX <= other.maxX && maxX >= other.minX && minY <= other.maxY && maxY >= other.minY
}

object GeometryUtils {

    fun signedAreaUnitsSq(points: List<PointF>): Double {
        if (points.size < 3) return 0.0
        var sum = 0.0
        for (i in points.indices) {
            val a = points[i]
            val b = points[(i + 1) % points.size]
            sum += (a.x.toDouble() * b.y.toDouble()) - (b.x.toDouble() * a.y.toDouble())
        }
        return sum * 0.5
    }

    fun areaUnitsSq(points: List<PointF>): Double = abs(signedAreaUnitsSq(points))

    fun sqFtToShotangsho(areaSqFt: Double, sqFeetPerShotok: Double = 435.6): Double {
        val factor = sqFeetPerShotok.coerceAtLeast(1.0)
        return (areaSqFt.coerceAtLeast(0.0)) / factor
    }

    fun edgeLengthUnits(a: PointF, b: PointF): Double =
        hypot((b.x - a.x).toDouble(), (b.y - a.y).toDouble())

    fun bearingDegrees(a: PointF, b: PointF): Double {
        val dx = (b.x - a.x).toDouble()
        val dy = (a.y - b.y).toDouble() // screen Y grows downward; North is -Y
        val deg = Math.toDegrees(atan2(dx, dy))
        return ((deg % 360.0) + 360.0) % 360.0
    }

    fun buildSegments(points: List<PointF>, feetPerCanvasUnit: Double): List<BoundarySegment> {
        if (points.size < 2) return emptyList()
        val scale = feetPerCanvasUnit.coerceAtLeast(0.0)
        return points.indices.map { i ->
            val a = points[i]
            val b = points[(i + 1) % points.size]
            val feet = edgeLengthUnits(a, b) * scale
            BoundarySegment(
                startLabel = BengaliFormatters.toBengaliDigits(i + 1),
                endLabel = BengaliFormatters.toBengaliDigits(((i + 1) % points.size) + 1),
                lengthFeet = feet,
                formattedLength = BengaliFormatters.formatFtInches(feet, useBengali = true),
                bearingDegrees = bearingDegrees(a, b)
            )
        }
    }

    fun boundingBox(points: List<PointF>): BoundingBox {
        if (points.isEmpty()) return BoundingBox(0f, 0f, 0f, 0f)
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        for (p in points) {
            if (p.x < minX) minX = p.x
            if (p.y < minY) minY = p.y
            if (p.x > maxX) maxX = p.x
            if (p.y > maxY) maxY = p.y
        }
        return BoundingBox(minX, minY, maxX, maxY)
    }

    fun centroid(points: List<PointF>): PointF {
        if (points.isEmpty()) return PointF(0f, 0f)
        if (points.size < 3) {
            return PointF(
                points.map { it.x }.average().toFloat(),
                points.map { it.y }.average().toFloat()
            )
        }
        var signedArea = 0.0
        var cx = 0.0
        var cy = 0.0
        for (i in points.indices) {
            val p0 = points[i]
            val p1 = points[(i + 1) % points.size]
            val cross = p0.x.toDouble() * p1.y.toDouble() - p1.x.toDouble() * p0.y.toDouble()
            signedArea += cross
            cx += (p0.x + p1.x) * cross
            cy += (p0.y + p1.y) * cross
        }
        signedArea *= 0.5
        if (abs(signedArea) < 1e-6) {
            val box = boundingBox(points)
            return PointF(box.centerX, box.centerY)
        }
        return PointF((cx / (6.0 * signedArea)).toFloat(), (cy / (6.0 * signedArea)).toFloat())
    }

    fun pointInPolygon(point: PointF, polygon: List<PointF>): Boolean {
        if (polygon.size < 3) return false
        var inside = false
        var j = polygon.size - 1
        for (i in polygon.indices) {
            val pi = polygon[i]
            val pj = polygon[j]
            val intersects = ((pi.y > point.y) != (pj.y > point.y)) &&
                (point.x < (pj.x - pi.x) * (point.y - pi.y) / ((pj.y - pi.y).takeIf { abs(it) > 1e-6f } ?: 1e-6f) + pi.x)
            if (intersects) inside = !inside
            j = i
        }
        return inside
    }

    fun distancePointToSegment(p: PointF, a: PointF, b: PointF): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val lenSq = dx * dx + dy * dy
        if (lenSq <= 1e-6f) return hypot((p.x - a.x).toDouble(), (p.y - a.y).toDouble()).toFloat()
        val t = (((p.x - a.x) * dx + (p.y - a.y) * dy) / lenSq).coerceIn(0f, 1f)
        val projX = a.x + t * dx
        val projY = a.y + t * dy
        return hypot((p.x - projX).toDouble(), (p.y - projY).toDouble()).toFloat()
    }

    fun minBoundingBoxDimensionsFeet(points: List<PointF>, feetPerCanvasUnit: Double): Pair<Double, Double> {
        if (points.size < 2) return 0.0 to 0.0
        var bestMinDim = Double.POSITIVE_INFINITY
        var bestPair = 0.0 to 0.0
        for (i in points.indices) {
            val a = points[i]
            val b = points[(i + 1) % points.size]
            val dx = (b.x - a.x).toDouble()
            val dy = (b.y - a.y).toDouble()
            val len = hypot(dx, dy)
            if (len < 1e-6) continue
            val ux = dx / len
            val uy = dy / len
            val vx = -uy
            val vy = ux
            var minU = Double.POSITIVE_INFINITY
            var maxU = Double.NEGATIVE_INFINITY
            var minV = Double.POSITIVE_INFINITY
            var maxV = Double.NEGATIVE_INFINITY
            for (p in points) {
                val projU = p.x * ux + p.y * uy
                val projV = p.x * vx + p.y * vy
                minU = min(minU, projU)
                maxU = max(maxU, projU)
                minV = min(minV, projV)
                maxV = max(maxV, projV)
            }
            val w = (maxU - minU) * feetPerCanvasUnit
            val h = (maxV - minV) * feetPerCanvasUnit
            val area = w * h
            if (area < bestMinDim) {
                bestMinDim = area
                bestPair = min(w, h) to max(w, h)
            }
        }
        return if (bestMinDim.isFinite()) bestPair else 0.0 to 0.0
    }

    fun buildPlot(
        id: String,
        name: String,
        colorHex: String,
        points: List<PointF>,
        feetPerCanvasUnit: Double,
        sqFeetPerShotok: Double,
        isRoad: Boolean = false,
        dagNumber: String = "",
        ownerName: String = "",
        labelStyle: PlotLabelStyle? = null
    ): LandPlot {
        val metrics = MeasurementEngine.computePlotMetrics(points, feetPerCanvasUnit, sqFeetPerShotok)
        val now = System.currentTimeMillis()
        return LandPlot(
            id = id,
            name = name,
            colorHex = colorHex,
            points = points,
            areaShotangsho = metrics.areaShotangsho,
            areaSqFt = metrics.areaSqFt,
            perimeterFeet = metrics.perimeterFeet,
            segments = metrics.segments,
            isRoad = isRoad,
            dagNumber = dagNumber,
            ownerName = ownerName,
            createdAt = now,
            updatedAt = now,
            labelStyle = labelStyle
        )
    }

    fun recalculatePlot(
        plot: LandPlot,
        feetPerCanvasUnit: Double,
        sqFeetPerShotok: Double
    ): LandPlot {
        val metrics = MeasurementEngine.computePlotMetrics(plot.points, feetPerCanvasUnit, sqFeetPerShotok)
        return plot.copy(
            areaShotangsho = metrics.areaShotangsho,
            areaSqFt = metrics.areaSqFt,
            perimeterFeet = metrics.perimeterFeet,
            segments = metrics.segments,
            updatedAt = System.currentTimeMillis()
        )
    }

    fun generatePlotId(): String {
        val hex = java.util.UUID.randomUUID().toString().replace("-", "").take(6)
        return "plot_$hex"
    }

    val PLOT_PALETTE = listOf("#2563EB", "#DC2626", "#16A34A", "#9333EA", "#EA580C")

    fun plotColorForIndex(index: Int): String =
        PLOT_PALETTE[index.coerceAtLeast(0) % PLOT_PALETTE.size]
}
