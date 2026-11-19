package com.example

import com.example.data.model.DivisionType
import com.example.data.model.LandPlot
import com.example.data.model.PointF
import com.example.domain.AnaGondaHelper
import com.example.domain.BangladeshiUnitConverter
import com.example.domain.BengaliFormatters
import com.example.domain.GeometryUtils
import com.example.domain.JtsDivisionEngine
import com.example.domain.LandAreaUnit
import com.example.domain.SpatialIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class LandProGisUnitTest {

    @Test
    fun geometryUtils_rectangleAreaAndPerimeter_exact() {
        val pts = listOf(PointF(0f, 0f), PointF(100f, 0f), PointF(100f, 50f), PointF(0f, 50f))
        val plot = GeometryUtils.buildPlot(
            id = "p1",
            name = "প্লট ১",
            colorHex = "#2563EB",
            points = pts,
            feetPerCanvasUnit = 1.0,
            sqFeetPerShotok = 435.6
        )
        assertEquals(5000.0, plot.areaSqFt, 1e-3)
        assertEquals(300.0, plot.perimeterFeet, 1e-3)
        assertEquals(11.48, plot.areaShotangsho, 1e-2)
    }

    @Test
    fun anaGondaHelper_computesFractionalShareCorrectly() {
        val res = AnaGondaHelper.calculateShare(totalShotok = 100.0, ana = 8, gonda = 0, kora = 0, kranti = 0, til = 0)
        assertEquals(0.5, res.decimalFraction, 1e-6)
        assertEquals(50.0, res.shareShotok, 1e-4)
    }

    @Test
    fun bangladeshiUnitConverter_shotokKathaBigha_consistent() {
        val map = BangladeshiUnitConverter.convertAll(100.0, LandAreaUnit.SHOTOK, 435.6)
        assertEquals(1.0, map[LandAreaUnit.ACRE] ?: 0.0, 1e-4)
        assertEquals(43560.0, map[LandAreaUnit.SQ_FT] ?: 0.0, 1e-1)
    }

    @Test
    fun jtsDivisionEngine_equalAndRoadDivision_conservesAreaWithinEpsilon() {
        val source = GeometryUtils.buildPlot(
            id = "src",
            name = "প্লট ১",
            colorHex = "#2563EB",
            points = listOf(PointF(0f, 0f), PointF(200f, 0f), PointF(200f, 100f), PointF(0f, 100f)),
            feetPerCanvasUnit = 1.0,
            sqFeetPerShotok = 435.6
        )

        val result = JtsDivisionEngine.dividePlot(
            source = source,
            divisionType = DivisionType.EQUAL,
            partsCount = 4,
            percentages = emptyList(),
            customTargetShotoks = emptyList(),
            roadOptionId = "middle_horizontal",
            roadWidthFeet = 10f,
            sweepAngleDeg = 0f,
            feetPerCanvasUnit = 1.0,
            sqFeetPerShotok = 435.6
        )

        val partsSum = result.plots.filter { !it.isRoad }.sumOf { it.areaSqFt }
        val roadSum = result.plots.filter { it.isRoad }.sumOf { it.areaSqFt }
        assertEquals(4, result.plots.count { !it.isRoad })
        assertTrue(roadSum > 0.0)
        assertTrue(abs((partsSum + roadSum) - source.areaSqFt) < 15.0)
    }

    @Test
    fun spatialIndex_hitTestFindsTargetPlot() {
        val p1 = GeometryUtils.buildPlot("a", "A", "#2563EB", listOf(PointF(0f, 0f), PointF(50f, 0f), PointF(50f, 50f), PointF(0f, 50f)), 1.0, 435.6)
        val p2 = GeometryUtils.buildPlot("b", "B", "#16A34A", listOf(PointF(200f, 200f), PointF(300f, 200f), PointF(300f, 300f), PointF(200f, 300f)), 1.0, 435.6)
        val index = SpatialIndex(listOf(p1, p2))
        assertEquals("a", index.hitTest(PointF(25f, 25f))?.id)
        assertEquals("b", index.hitTest(PointF(250f, 250f))?.id)
        assertEquals(null, index.hitTest(PointF(120f, 120f)))
    }

    @Test
    fun bengaliDigitsAndParser_handleBengaliAndEnglishNumbers() {
        assertEquals("১২৩.৪৫", BengaliFormatters.formatDecimal(123.45, 2))
        assertEquals(435.6, BengaliFormatters.parseDoubleFlexible("৪৩৫.৬") ?: 0.0, 1e-6)
        assertFalse(JtsDivisionEngine.validatePolygon(listOf(PointF(0f, 0f), PointF(10f, 10f))).isValid)
    }
}
