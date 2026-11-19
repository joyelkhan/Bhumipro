package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LandProDatabase
import com.example.data.model.PointF
import com.example.data.model.SurveyProject
import com.example.domain.GeometryUtils
import com.example.domain.report.SurveyReportDataMapper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RefactorSprintTest {

    @Test
    fun emptyPlotSelection_producesEmptyPlaceholderData_neverAutoSelects() {
        val plot = GeometryUtils.buildPlot(
            id = "plot_unselected",
            name = "প্লট ১",
            colorHex = "#2563EB",
            points = listOf(PointF(0f, 0f), PointF(50f, 0f), PointF(50f, 50f), PointF(0f, 50f)),
            feetPerCanvasUnit = 0.55,
            sqFeetPerShotok = 435.6
        )
        val project = SurveyProject(
            plots = listOf(plot),
            selectedPlotIds = emptySet(),
            feetPerCanvasUnit = 0.55
        )
        val reportData = SurveyReportDataMapper.build(project)
        assertTrue(reportData.isEmptySelection)
        assertTrue(reportData.plots.isEmpty())
    }

    @Test
    fun databaseMigrations_coverVersions2Through7_withAll9Daos() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val migrations = LandProDatabase.allMigrations()
        assertEquals(5, migrations.size)
        assertEquals(2, migrations[0].startVersion)
        assertEquals(7, migrations.last().endVersion)

        val db = LandProDatabase.createInMemory(context)
        runBlocking {
            assertEquals(0, db.draftProjectDao().count())
        }
        assertTrue(db.openHelper.readableDatabase.isOpen)
        db.close()
    }
}
