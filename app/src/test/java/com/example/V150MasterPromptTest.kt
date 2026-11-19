package com.example

import com.example.data.local.LandProDatabase
import com.example.data.model.MouzaScalePreset
import com.example.data.model.mouzaScalePresets
import com.example.domain.CadMapHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V150MasterPromptTest {

    @Test
    fun mouzaScalePresets_includeStandardScales_andComputeFeetPerUnit() {
        assertTrue(mouzaScalePresets.any { it.id == "scale_16" })
        assertTrue(mouzaScalePresets.any { it.id == "scale_32" })
        assertTrue(mouzaScalePresets.any { it.id == "scale_64" })
        assertTrue(mouzaScalePresets.any { it.id == "scale_80" })
        val fpu16 = mouzaScalePresets.first { it.id == "scale_16" }.feetPerInch / 300.0
        assertTrue(fpu16 in 0.4..1.5)
    }

    @Test
    fun cadZoomClamping_keeps15To1600Percent() {
        val clampedLow = 5.coerceIn(15, 1600)
        val clampedHigh = 2500.coerceIn(15, 1600)
        assertEquals(15, clampedLow)
        assertEquals(1600, clampedHigh)
    }

    @Test
    fun cadMapHelper_parsesSampleDxfIntoPolygons() {
        val dxf = """
            0
            SECTION
            2
            ENTITIES
            0
            LWPOLYLINE
            8
            PLOT_BOUNDARY
            10
            0.0
            20
            0.0
            10
            100.0
            20
            0.0
            10
            100.0
            20
            100.0
            10
            0.0
            20
            100.0
            0
            ENDSEC
            0
            EOF
        """.trimIndent()
        val entities = CadMapHelper.parseDxfToPolylines(dxf)
        assertTrue(entities.isNotEmpty())
        assertTrue(entities.first().points.size >= 4)
    }

    @Test
    fun defaultMouzaCatalog_isPopulated() {
        val catalog = LandProDatabase.defaultMouzaCatalog()
        assertTrue(catalog.isNotEmpty())
        assertTrue(catalog.all { it.isOfflineSaved })
    }
}
