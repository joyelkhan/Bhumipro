package com.example

import com.example.data.model.PlotLabelStyle
import com.example.data.model.PointF
import com.example.domain.GeometryUtils
import com.example.ui.viewmodel.SurveySelectionHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceV14EngineTest {

    private val plotA = GeometryUtils.buildPlot(
        id = "pA",
        name = "প্লট ক",
        colorHex = "#2563EB",
        points = listOf(PointF(0f, 0f), PointF(100f, 0f), PointF(100f, 100f), PointF(0f, 100f)),
        feetPerCanvasUnit = 1.0,
        sqFeetPerShotok = 435.6,
        dagNumber = "১০১",
        ownerName = "রহিম উদ্দিন"
    )

    private val plotB = GeometryUtils.buildPlot(
        id = "pB",
        name = "প্লট খ",
        colorHex = "#16A34A",
        points = listOf(PointF(100f, 0f), PointF(200f, 0f), PointF(200f, 100f), PointF(100f, 100f)),
        feetPerCanvasUnit = 1.0,
        sqFeetPerShotok = 435.6,
        dagNumber = "১০২",
        ownerName = "করিম মিয়া"
    )

    private val plotFar = GeometryUtils.buildPlot(
        id = "pC",
        name = "প্লট গ",
        colorHex = "#EA580C",
        points = listOf(PointF(500f, 500f), PointF(550f, 500f), PointF(550f, 550f), PointF(500f, 550f)),
        feetPerCanvasUnit = 1.0,
        sqFeetPerShotok = 435.6,
        dagNumber = "১০৩",
        ownerName = "রহিম উদ্দিন"
    )

    @Test
    fun selectionHelper_rectangleAdjacentDagOwnerAndAreaRange() {
        val all = listOf(plotA, plotB, plotFar)
        assertEquals(setOf("pA", "pB"), SurveySelectionHelper.selectInRectangle(all, PointF(-10f, -10f), PointF(210f, 110f)))
        assertEquals(setOf("pA", "pB"), SurveySelectionHelper.selectAdjacent(all, plotA))
        assertEquals(setOf("pB"), SurveySelectionHelper.selectByDag(all, "১০২"))
        assertEquals(setOf("pA", "pC"), SurveySelectionHelper.selectByOwner(all, "রহিম"))
        assertEquals(setOf("pA", "pB"), SurveySelectionHelper.selectByAreaRange(all, 20.0, 25.0))
    }

    @Test
    fun plotLabelStyle_clampsFontScaleAndRotationBounds() {
        assertEquals(0.4f, PlotLabelStyle(fontSizeScale = 0.1f).effectiveFontScale(), 1e-4f)
        assertEquals(4.0f, PlotLabelStyle(fontSizeScale = 9.0f).effectiveFontScale(), 1e-4f)
        assertTrue(PlotLabelStyle(rotationDeg = 270f).effectiveRotationDeg() <= 180f)
        assertTrue(PlotLabelStyle(rotationDeg = -270f).effectiveRotationDeg() >= -180f)
    }
}
