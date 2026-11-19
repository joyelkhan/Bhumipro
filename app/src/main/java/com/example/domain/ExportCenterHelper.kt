package com.example.domain

import android.content.Context
import android.graphics.Bitmap
import com.example.data.model.ExportFormat
import com.example.data.model.LandPlot
import com.example.data.model.SurveyProject
import com.example.domain.report.BengaliSurveyReportRenderer
import com.example.domain.report.PageLayout
import com.example.domain.report.ReportTemplate
import com.example.domain.report.ReportTicket
import com.example.domain.report.SurveyReportDataMapper
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object ExportCenterHelper {

    fun exportProject(
        context: Context,
        project: SurveyProject,
        format: ExportFormat,
        previewBitmapProvider: () -> Bitmap? = { null }
    ): Result<File> = runCatching {
        val safeBase = buildSafeExportBaseName(project)
        val outDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(outDir, "$safeBase.${format.extension}")

        when (format) {
            ExportFormat.PNG -> {
                val bmp = previewBitmapProvider()
                    ?: renderFallbackPreview(context, project)
                FileOutputStream(file).use { out ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            }
            ExportFormat.JPEG -> {
                val bmp = previewBitmapProvider()
                    ?: renderFallbackPreview(context, project)
                FileOutputStream(file).use { out ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
                }
            }
            ExportFormat.SVG -> {
                file.writeText(buildSvg(project), Charsets.UTF_8)
            }
            ExportFormat.GEOJSON -> {
                file.writeText(buildGeoJson(project), Charsets.UTF_8)
            }
            ExportFormat.KML -> {
                file.writeText(buildKml(project), Charsets.UTF_8)
            }
            ExportFormat.CSV -> {
                file.writeText(buildCsv(project), Charsets.UTF_8)
            }
            ExportFormat.WKT -> {
                file.writeText(buildWkt(project), Charsets.UTF_8)
            }
            ExportFormat.DXF -> {
                file.writeText(buildDxf(project), Charsets.UTF_8)
            }
            ExportFormat.PDF -> {
                val bmp = previewBitmapProvider() ?: renderFallbackPreview(context, project)
                val baos = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG, 90, baos)
                file.writeBytes(baos.toByteArray())
            }
        }
        file
    }

    private fun renderFallbackPreview(context: Context, project: SurveyProject): Bitmap {
        val reportData = SurveyReportDataMapper.build(
            project = project,
            ticket = ReportTicket(),
            includePlotIds = project.selectedPlotIds,
            canvasSettings = project.canvasSettings,
            layers = project.layers
        )
        return BengaliSurveyReportRenderer.renderPreviewBitmap(
            data = reportData,
            template = ReportTemplate.STANDARD,
            typeface = PdfAndPrintHelper.resolveBengaliTypeface(context),
            pageLayout = PageLayout(595f, 842f, 34f)
        )
    }

    private fun buildSafeExportBaseName(project: SurveyProject): String {
        val raw = project.mouzaName.ifBlank { project.title }.ifBlank { "BhumiPro_Survey" }
        return raw.replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_").take(40)
    }

    fun buildCsv(project: SurveyProject): String {
        val sb = StringBuilder()
        sb.appendLine("PlotId,PlotName,DagNo,Owner,IsRoad,AreaShotok,AreaSqFt,PerimeterFt")
        for (p in project.plots) {
            val shotok = p.areaSqFt / project.sqFeetPerShotok.coerceAtLeast(1.0)
            sb.appendLine(
                "${p.id},\"${p.name}\",\"${p.dagNumber}\",\"${p.ownerName}\",${p.isRoad}," +
                    "${String.format(Locale.US, "%.2f", shotok)}," +
                    "${String.format(Locale.US, "%.2f", p.areaSqFt)}," +
                    String.format(Locale.US, "%.2f", p.perimeterFeet)
            )
        }
        return sb.toString()
    }

    fun buildWkt(project: SurveyProject): String {
        return project.plots.joinToString("\n") { p ->
            if (p.points.size < 3) "" else {
                val ring = (p.points + p.points.first()).joinToString(", ") { pt ->
                    "${String.format(Locale.US, "%.2f", pt.x)} ${String.format(Locale.US, "%.2f", pt.y)}"
                }
                "POLYGON (($ring))"
            }
        }
    }

    fun buildSvg(project: SurveyProject): String {
        val allPts = project.plots.flatMap { it.points }
        val box = GeometryUtils.boundingBox(allPts)
        val minX = box.minX - 20f
        val minY = box.minY - 20f
        val w = (box.width + 40f).coerceAtLeast(200f)
        val h = (box.height + 40f).coerceAtLeast(200f)
        val polygons = project.plots.filter { !it.isHidden && it.points.size >= 3 }.joinToString("\n") { p ->
            val pts = p.points.joinToString(" ") { "${it.x},${it.y}" }
            """  <polygon points="$pts" fill="${p.colorHex}22" stroke="${p.colorHex}" stroke-width="2" />"""
        }
        return """<svg xmlns="http://www.w3.org/2000/svg" viewBox="$minX $minY $w $h">
$polygons
</svg>"""
    }

    fun buildGeoJson(project: SurveyProject): String {
        val features = project.plots.filter { it.points.size >= 3 }.joinToString(",\n") { p ->
            val coords = (p.points + p.points.first()).joinToString(", ") { "[${it.x}, ${it.y}]" }
            """    {
      "type": "Feature",
      "properties": { "id": "${p.id}", "name": "${p.name}", "dag": "${p.dagNumber}", "areaSqFt": ${p.areaSqFt} },
      "geometry": { "type": "Polygon", "coordinates": [[$coords]] }
    }"""
        }
        return """{
  "type": "FeatureCollection",
  "features": [
$features
  ]
}"""
    }

    fun buildKml(project: SurveyProject): String {
        val placemarks = project.plots.filter { it.points.size >= 3 }.joinToString("\n") { p ->
            val coords = (p.points + p.points.first()).joinToString(" ") { "${it.x},${it.y},0" }
            """  <Placemark><name>${p.name}</name><Polygon><outerBoundaryIs><LinearRing><coordinates>$coords</coordinates></LinearRing></outerBoundaryIs></Polygon></Placemark>"""
        }
        return """<?xml version="1.0" encoding="UTF-8"?>
<kml xmlns="http://www.opengis.net/kml/2.2">
<Document>
<name>${project.mouzaName}</name>
$placemarks
</Document>
</kml>"""
    }

    fun buildDxf(project: SurveyProject): String {
        val sb = StringBuilder()
        sb.appendLine("0\nSECTION\n2\nENTITIES")
        for (p in project.plots.filter { it.points.size >= 3 }) {
            sb.appendLine("0\nLWPOLYLINE\n8\n${if (p.isRoad) "ROAD" else "PLOT"}\n70\n1")
            for (pt in p.points) {
                sb.appendLine("10\n${pt.x}\n20\n${pt.y}")
            }
        }
        sb.appendLine("0\nENDSEC\n0\nEOF")
        return sb.toString()
    }
}
