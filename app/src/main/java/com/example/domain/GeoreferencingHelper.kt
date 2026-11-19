package com.example.domain

import com.example.data.model.ControlPoint
import com.example.data.model.GeorefQualityReport
import kotlin.math.hypot
import kotlin.math.sqrt

object GeoreferencingHelper {
    fun evaluateQuality(points: List<ControlPoint>): GeorefQualityReport {
        if (points.isEmpty()) {
            return GeorefQualityReport(0, 0.0, "পয়েন্ট নেই")
        }
        val sqSum = points.sumOf { cp ->
            val dx = (cp.targetPoint.x + cp.offsetX - cp.sourcePoint.x).toDouble()
            val dy = (cp.targetPoint.y + cp.offsetY - cp.sourcePoint.y).toDouble()
            val dist = hypot(dx, dy)
            dist * dist
        }
        val rmse = sqrt(sqSum / points.size)
        val label = when {
            points.size < 2 -> "অপর্যাপ্ত পয়েন্ট"
            rmse <= 2.5 -> "উৎকৃষ্ট (${BengaliFormatters.formatDecimal(rmse, 2)} px)"
            rmse <= 6.0 -> "ভালো (${BengaliFormatters.formatDecimal(rmse, 2)} px)"
            else -> "সামঞ্জস্য প্রয়োজন (${BengaliFormatters.formatDecimal(rmse, 2)} px)"
        }
        return GeorefQualityReport(
            pointCount = points.size,
            rmsePx = BengaliFormatters.roundToDecimals(rmse, 3),
            qualityLabelBn = label
        )
    }
}
