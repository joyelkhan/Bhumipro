package com.example.domain

import com.example.data.model.LandPlot
import com.example.data.model.PointF
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.index.strtree.STRtree

class SpatialIndex(plots: List<LandPlot>) {
    private val tree = STRtree()
    private val indexedPlots: List<LandPlot> = plots.filter { !it.isHidden && it.points.size >= 3 }

    init {
        for (plot in indexedPlots) {
            val box = GeometryUtils.boundingBox(plot.points)
            val env = Envelope(box.minX.toDouble(), box.maxX.toDouble(), box.minY.toDouble(), box.maxY.toDouble())
            tree.insert(env, plot)
        }
        tree.build()
    }

    fun hitTest(point: PointF, toleranceUnits: Float = 6f): LandPlot? {
        val queryEnv = Envelope(
            (point.x - toleranceUnits).toDouble(),
            (point.x + toleranceUnits).toDouble(),
            (point.y - toleranceUnits).toDouble(),
            (point.y + toleranceUnits).toDouble()
        )
        @Suppress("UNCHECKED_CAST")
        val candidates = tree.query(queryEnv) as List<LandPlot>
        // Prefer topmost (last drawn) plot inside polygon
        for (plot in candidates.asReversed()) {
            if (GeometryUtils.pointInPolygon(point, plot.points)) {
                return plot
            }
        }
        // Fallback: near boundary edge
        for (plot in candidates.asReversed()) {
            for (i in plot.points.indices) {
                val a = plot.points[i]
                val b = plot.points[(i + 1) % plot.points.size]
                if (GeometryUtils.distancePointToSegment(point, a, b) <= toleranceUnits) {
                    return plot
                }
            }
        }
        return null
    }

    fun queryRectangle(a: PointF, b: PointF): List<LandPlot> {
        val loX = minOf(a.x, b.x)
        val hiX = maxOf(a.x, b.x)
        val loY = minOf(a.y, b.y)
        val hiY = maxOf(a.y, b.y)
        val queryEnv = Envelope(loX.toDouble(), hiX.toDouble(), loY.toDouble(), hiY.toDouble())
        val rectBox = BoundingBox(loX, loY, hiX, hiY)
        @Suppress("UNCHECKED_CAST")
        val candidates = tree.query(queryEnv) as List<LandPlot>
        return candidates.filter { plot ->
            val plotBox = GeometryUtils.boundingBox(plot.points)
            plotBox.intersects(rectBox)
        }
    }
}
