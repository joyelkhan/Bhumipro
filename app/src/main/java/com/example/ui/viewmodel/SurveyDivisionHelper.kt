package com.example.ui.viewmodel

import com.example.data.model.DivisionReport
import com.example.data.model.DivisionType
import com.example.data.model.LandPlot
import com.example.data.model.PointF
import com.example.domain.JtsDivisionEngine

data class DivisionUiState(
    val divisionType: DivisionType = DivisionType.EQUAL,
    val divisionPartsCount: Int = 4,
    val selectedRoadOption: String = "middle_horizontal",
    val roadWidthFeet: Float = 6f,
    val divisionPercentages: List<Double> = listOf(25.0, 25.0, 25.0, 25.0),
    val customTargetShotoks: List<Double> = listOf(2.0, 2.0, 2.0),
    val divisionSweepAngleDeg: Float = 0f,
    val useCustomRoadCenterline: Boolean = false,
    val manualSplitPoints: List<PointF> = emptyList(),
    val previewDividedPlots: List<LandPlot> = emptyList(),
    val divisionReport: DivisionReport? = null
)

object SurveyDivisionHelper {

    fun resizePercentages(partsCount: Int): List<Double> {
        val n = partsCount.coerceIn(2, 24)
        val base = 100.0 / n
        return List(n) { base }
    }

    fun resizeCustomShotoks(partsCount: Int, current: List<Double>): List<Double> {
        val needed = (partsCount.coerceIn(2, 24) - 1).coerceAtLeast(1)
        return List(needed) { idx -> current.getOrElse(idx) { 1.0 }.coerceAtLeast(0.1) }
    }

    fun computePreview(
        activePlot: LandPlot,
        state: DivisionUiState,
        feetPerUnit: Double,
        sqFeetPerShotok: Double
    ) = JtsDivisionEngine.dividePlot(
        source = activePlot,
        divisionType = state.divisionType,
        partsCount = state.divisionPartsCount,
        percentages = state.divisionPercentages,
        customTargetShotoks = state.customTargetShotoks,
        roadOptionId = state.selectedRoadOption,
        roadWidthFeet = state.roadWidthFeet,
        sweepAngleDeg = state.divisionSweepAngleDeg,
        feetPerCanvasUnit = feetPerUnit,
        sqFeetPerShotok = sqFeetPerShotok,
        customRoadPoints = if (state.useCustomRoadCenterline) state.manualSplitPoints else emptyList()
    )
}
