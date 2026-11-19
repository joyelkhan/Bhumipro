package com.example.ui.viewmodel

import com.example.data.model.LandPlot
import com.example.data.model.PlotLabelStyle
import com.example.data.model.PointF
import com.example.domain.GeometryUtils
import com.example.domain.JtsDivisionEngine
import com.example.domain.SpatialIndex

object SurveySelectionHelper {

    fun selectInRectangle(plots: List<LandPlot>, a: PointF, b: PointF): Set<String> {
        val index = SpatialIndex(plots)
        return index.queryRectangle(a, b).map { it.id }.toSet()
    }

    fun selectAllVisible(plots: List<LandPlot>): Set<String> =
        plots.filter { !it.isHidden }.map { it.id }.toSet()

    fun invertSelection(plots: List<LandPlot>, current: Set<String>): Set<String> =
        plots.filter { !it.isHidden && it.id !in current }.map { it.id }.toSet()

    fun selectAdjacent(plots: List<LandPlot>, activePlot: LandPlot?): Set<String> {
        if (activePlot == null) return emptySet()
        val result = mutableSetOf(activePlot.id)
        for (candidate in plots) {
            if (candidate.id != activePlot.id && !candidate.isHidden) {
                if (JtsDivisionEngine.polygonsTouchOrAdjacent(activePlot.points, candidate.points)) {
                    result.add(candidate.id)
                }
            }
        }
        return result
    }

    fun selectByDag(plots: List<LandPlot>, dag: String): Set<String> {
        val needle = dag.trim()
        if (needle.isBlank()) return emptySet()
        return plots.filter {
            !it.isHidden && it.dagNumber.trim().equals(needle, ignoreCase = true)
        }.map { it.id }.toSet()
    }

    fun selectByOwner(plots: List<LandPlot>, owner: String): Set<String> {
        val needle = owner.trim()
        if (needle.isBlank()) return emptySet()
        return plots.filter {
            !it.isHidden && it.ownerName.contains(needle, ignoreCase = true)
        }.map { it.id }.toSet()
    }

    fun selectByAreaRange(plots: List<LandPlot>, minShotok: Double, maxShotok: Double): Set<String> {
        val lo = minOf(minShotok, maxShotok)
        val hi = maxOf(minShotok, maxShotok)
        return plots.filter {
            !it.isHidden && !it.isRoad && it.areaShotangsho in lo..hi
        }.map { it.id }.toSet()
    }

    fun updatePlotLabelStyle(
        plots: List<LandPlot>,
        targetPlotId: String,
        transform: (PlotLabelStyle) -> PlotLabelStyle
    ): Pair<List<LandPlot>, Boolean> {
        var changed = false
        val updated = plots.map { plot ->
            if (plot.id == targetPlotId) {
                val cur = plot.labelStyle ?: PlotLabelStyle.DEFAULT
                if (cur.locked && !cur.isDefault()) {
                    plot
                } else {
                    val next = transform(cur)
                    changed = true
                    plot.copy(labelStyle = next, updatedAt = System.currentTimeMillis())
                }
            } else {
                plot
            }
        }
        return updated to changed
    }

    fun applyLabelStyleToSelection(
        plots: List<LandPlot>,
        selectedIds: Set<String>,
        transform: (PlotLabelStyle) -> PlotLabelStyle
    ): List<LandPlot> {
        if (selectedIds.isEmpty()) return plots
        return plots.map { plot ->
            if (plot.id in selectedIds) {
                val cur = plot.labelStyle ?: PlotLabelStyle.DEFAULT
                if (cur.locked && !cur.isDefault()) plot
                else plot.copy(labelStyle = transform(cur), updatedAt = System.currentTimeMillis())
            } else {
                plot
            }
        }
    }

    fun movePlotPoints(plot: LandPlot, dx: Float, dy: Float, feetPerUnit: Double, sqFeetPerShotok: Double): LandPlot {
        if (plot.isLocked) return plot
        val moved = plot.points.map { PointF(it.x + dx, it.y + dy) }
        return GeometryUtils.recalculatePlot(plot.copy(points = moved), feetPerUnit, sqFeetPerShotok)
    }
}
