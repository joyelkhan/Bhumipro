package com.example.domain

import com.example.data.model.LandPlot
import com.example.data.model.PointF

data class CadOverlayEntity(
    val layerName: String = "0",
    val points: List<PointF> = emptyList(),
    val closed: Boolean = false
)

object CadMapHelper {

    fun parseDxfToPolylines(dxfText: String): List<CadOverlayEntity> {
        val lines = dxfText.lines().map { it.trim() }
        val result = mutableListOf<CadOverlayEntity>()
        var i = 0
        while (i < lines.size - 1) {
            if (lines[i] == "0" && lines[i + 1] == "LWPOLYLINE") {
                var j = i + 2
                var layer = "0"
                var closed = false
                val xs = mutableListOf<Float>()
                val ys = mutableListOf<Float>()
                while (j < lines.size - 1) {
                    val code = lines[j]
                    val value = lines[j + 1]
                    if (code == "0") break
                    when (code) {
                        "8" -> layer = value
                        "70" -> closed = (value.toIntOrNull() ?: 0) and 1 != 0
                        "10" -> xs.add(value.toFloatOrNull() ?: 0f)
                        "20" -> ys.add(value.toFloatOrNull() ?: 0f)
                    }
                    j += 2
                }
                val pts = xs.zip(ys).map { (x, y) -> PointF(x, y) }
                if (pts.size >= 2) {
                    result.add(CadOverlayEntity(layer, pts, closed))
                }
                i = j
            } else if (lines[i] == "0" && lines[i + 1] == "LINE") {
                var j = i + 2
                var layer = "0"
                var x1 = 0f; var y1 = 0f; var x2 = 0f; var y2 = 0f
                while (j < lines.size - 1) {
                    val code = lines[j]
                    val value = lines[j + 1]
                    if (code == "0") break
                    when (code) {
                        "8" -> layer = value
                        "10" -> x1 = value.toFloatOrNull() ?: 0f
                        "20" -> y1 = value.toFloatOrNull() ?: 0f
                        "11" -> x2 = value.toFloatOrNull() ?: 0f
                        "21" -> y2 = value.toFloatOrNull() ?: 0f
                    }
                    j += 2
                }
                result.add(CadOverlayEntity(layer, listOf(PointF(x1, y1), PointF(x2, y2)), false))
                i = j
            } else {
                i++
            }
        }
        return result
    }

    fun convertEntitiesToPlots(
        entities: List<CadOverlayEntity>,
        feetPerCanvasUnit: Double,
        sqFeetPerShotok: Double
    ): List<LandPlot> {
        return entities
            .filter { it.points.size >= 3 }
            .mapIndexed { idx, entity ->
                GeometryUtils.buildPlot(
                    id = GeometryUtils.generatePlotId(),
                    name = "ক্যাড ${BengaliFormatters.toBengaliDigits(idx + 1)}",
                    colorHex = GeometryUtils.plotColorForIndex(idx),
                    points = entity.points,
                    feetPerCanvasUnit = feetPerCanvasUnit,
                    sqFeetPerShotok = sqFeetPerShotok
                ).copy(notes = "DXF স্তর: ${entity.layerName}")
            }
    }

    fun sampleDxfContent(): String = """
        0
        SECTION
        2
        ENTITIES
        0
        LWPOLYLINE
        8
        MOUZA_PARCEL
        70
        1
        10
        180.0
        20
        160.0
        10
        420.0
        20
        160.0
        10
        420.0
        20
        360.0
        10
        180.0
        20
        360.0
        0
        LWPOLYLINE
        8
        MOUZA_PARCEL
        70
        1
        10
        440.0
        20
        160.0
        10
        680.0
        20
        180.0
        10
        670.0
        20
        370.0
        10
        440.0
        20
        360.0
        0
        ENDSEC
        0
        EOF
    """.trimIndent()
}
