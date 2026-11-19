package com.example.domain.report

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.example.data.model.MapTextPosition
import com.example.data.model.MeasurementUnit
import com.example.data.model.PointF
import com.example.data.model.ReportSectionId
import com.example.data.model.ReportSectionLayout
import com.example.domain.BengaliFormatters
import com.example.domain.GeometryUtils
import com.example.data.model.LengthFormatter
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

data class RenderTelemetry(
    val polygonsDrawn: Int = 0,
    val dimensionLabelsDrawn: Int = 0,
    val areaTableRowsDrawn: Int = 0
)

private data class TemplatePalette(
    val headerAccent: Int,
    val tableHeader: Int,
    val boundaryDefault: Int,
    val fillDefault: Int,
    val dimensionColor: Int,
    val disclaimerColor: Int,
    val inspectorColor: Int
)

object BengaliSurveyReportRenderer {
    private const val INK = 0xFF1C1C1E.toInt()
    private const val INK_SECONDARY = 0xFF56575C.toInt()
    private const val INK_MUTED = 0xFF8F9096.toInt()
    private const val PAGE_WHITE = 0xFFFFFFFF.toInt()
    private const val REPORT_BLUE = 0xFF0F32B4.toInt()
    private const val TABLE_HEADER_BLUE = 0xFF0B2EB2.toInt()
    private const val BOUNDARY_GREEN = 0xFF1E6B3C.toInt()
    private const val PLOT_FILL_GREEN = 0xFFF0FAF2.toInt()
    private const val DIMENSION_RED = 0xFFC23A32.toInt()
    private const val DISCLAIMER_PINK = 0xFFE78CAB.toInt()
    private const val INSPECTOR_RED = 0xFFB23030.toInt()
    private const val RULER_GREY = 0xFFC9CDCC.toInt()
    private const val HATCH_GREY = 0xFFE4E4E6.toInt()
    private const val BOX_GREY = 0xFFB6B9B8.toInt()
    private const val ROW_ALT_GREY = 0xFFEFF1F0.toInt()

    private fun paletteFor(template: ReportTemplate): TemplatePalette = when (template) {
        ReportTemplate.STANDARD -> TemplatePalette(
            headerAccent = REPORT_BLUE,
            tableHeader = TABLE_HEADER_BLUE,
            boundaryDefault = BOUNDARY_GREEN,
            fillDefault = PLOT_FILL_GREEN,
            dimensionColor = DIMENSION_RED,
            disclaimerColor = DISCLAIMER_PINK,
            inspectorColor = INSPECTOR_RED
        )
        ReportTemplate.DIMENSION -> TemplatePalette(
            headerAccent = DIMENSION_RED,
            tableHeader = 0xFF991B1B.toInt(),
            boundaryDefault = 0xFF1E3A8A.toInt(),
            fillDefault = 0xFFEFF6FF.toInt(),
            dimensionColor = DIMENSION_RED,
            disclaimerColor = DISCLAIMER_PINK,
            inspectorColor = INSPECTOR_RED
        )
        ReportTemplate.ROYAL -> TemplatePalette(
            headerAccent = 0xFF0F172A.toInt(),
            tableHeader = 0xFF1E293B.toInt(),
            boundaryDefault = 0xFF92400E.toInt(),
            fillDefault = 0xFFFFFBEB.toInt(),
            dimensionColor = 0xFFB45309.toInt(),
            disclaimerColor = 0xFF9D174D.toInt(),
            inspectorColor = 0xFF991B1B.toInt()
        )
        ReportTemplate.LAND_MEASUREMENT -> TemplatePalette(
            headerAccent = 0xFF065F46.toInt(),
            tableHeader = 0xFF047857.toInt(),
            boundaryDefault = BOUNDARY_GREEN,
            fillDefault = 0xFFECFDF5.toInt(),
            dimensionColor = 0xFF133C2D.toInt(),
            disclaimerColor = 0xFFBE185D.toInt(),
            inspectorColor = INSPECTOR_RED
        )
        ReportTemplate.CUSTOM -> TemplatePalette(
            headerAccent = REPORT_BLUE,
            tableHeader = TABLE_HEADER_BLUE,
            boundaryDefault = BOUNDARY_GREEN,
            fillDefault = PLOT_FILL_GREEN,
            dimensionColor = DIMENSION_RED,
            disclaimerColor = DISCLAIMER_PINK,
            inspectorColor = INSPECTOR_RED
        )
    }

    fun renderPreviewBitmap(
        data: SurveyReportData,
        template: ReportTemplate,
        typeface: Typeface,
        qrBitmap: Bitmap? = null,
        logoBitmap: Bitmap? = null,
        signatureBitmap: Bitmap? = null,
        insetBitmap: Bitmap? = null,
        sections: ReportSectionLayout = ReportSectionLayout(),
        pageLayout: PageLayout = PageLayout(595f, 842f, 34f),
        overrideBounds: ReportBounds? = null
    ): Bitmap {
        val w = pageLayout.widthPt.toInt().coerceAtLeast(100)
        val h = pageLayout.heightPt.toInt().coerceAtLeast(100)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawReport(
            canvas = canvas,
            pageW = w.toFloat(),
            pageH = h.toFloat(),
            data = data,
            template = template,
            typeface = typeface,
            qrBitmap = qrBitmap,
            logoBitmap = logoBitmap,
            signatureBitmap = signatureBitmap,
            insetBitmap = insetBitmap,
            sections = sections,
            pageLayout = pageLayout,
            overrideBounds = overrideBounds
        )
        return bmp
    }

    fun renderThumbnail(
        data: SurveyReportData,
        template: ReportTemplate,
        typeface: Typeface,
        widthPx: Int = 220,
        heightPx: Int = 310
    ): Bitmap {
        val full = renderPreviewBitmap(data, template, typeface)
        val scaled = Bitmap.createScaledBitmap(full, widthPx.coerceAtLeast(60), heightPx.coerceAtLeast(80), true)
        if (scaled !== full) full.recycle()
        return scaled
    }

    fun drawReport(
        canvas: Canvas,
        pageW: Float,
        pageH: Float,
        data: SurveyReportData,
        template: ReportTemplate,
        typeface: Typeface,
        qrBitmap: Bitmap? = null,
        logoBitmap: Bitmap? = null,
        signatureBitmap: Bitmap? = null,
        insetBitmap: Bitmap? = null,
        sections: ReportSectionLayout = ReportSectionLayout(),
        pageLayout: PageLayout? = null,
        overrideBounds: ReportBounds? = null
    ): RenderTelemetry {
        val palette = paletteFor(template)
        val m = pageLayout?.marginPt ?: 14f
        val contentLeft = m
        val contentRight = (pageW - m).coerceAtLeast(contentLeft + 80f)
        val contentW = contentRight - contentLeft
        val contentTop = m
        val contentBottom = (pageH - m).coerceAtLeast(contentTop + 100f)

        canvas.drawColor(PAGE_WHITE)

        // Outer subtle frame
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = BOX_GREY
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }
        canvas.drawRect(contentLeft, contentTop, contentRight, contentBottom, framePaint)

        var cursorY = contentTop + 6f

        // 1. Header Section
        if (data.ticket.showHeader && sections.isVisible(ReportSectionId.HEADER)) {
            val headerH = 44f
            drawHeader(canvas, contentLeft + 8f, cursorY, contentW - 16f, headerH, data, palette, typeface, logoBitmap, qrBitmap, sections)
            cursorY += headerH + 4f
        }

        // 2. Title & Date + North Arrow Section
        if (sections.isVisible(ReportSectionId.TITLE)) {
            val titleH = 38f
            drawTitleAndCompass(canvas, contentLeft + 8f, cursorY, contentW - 16f, titleH, data, palette, typeface)
            cursorY += titleH + 4f
        }

        val footerH = if (data.ticket.showFooter && sections.isVisible(ReportSectionId.FOOTER)) 24f else 0f
        val infoH = if (data.ticket.showLegend || data.ticket.showAreaSummary || sections.isVisible(ReportSectionId.DISCLAIMER)) 128f else 0f
        val certifierH = if (sections.isVisible(ReportSectionId.CERTIFIER)) 52f else 0f
        val mapBottom = (contentBottom - footerH - infoH - certifierH - 10f).coerceAtLeast(cursorY + 120f)

        var polygonsDrawn = 0
        var dimLabelsDrawn = 0

        // 3. Map & Scale Section
        val mapRect = RectF(contentLeft + 8f, cursorY, contentRight - 8f, mapBottom)
        val bounds = overrideBounds ?: data.bounds
        val padFrac = if (data.pad.mapContentMode == "crop") 0.02f else 0.08f
        val transform = ReportMapTransform.compute(bounds, mapRect.left, mapRect.top, mapRect.width(), mapRect.height(), padFrac)

        if (sections.isVisible(ReportSectionId.MAP)) {
            if (data.isEmptySelection) {
                drawEmptySelectionPlaceholder(canvas, mapRect, palette, typeface)
            } else {
                val (pCount, dCount) = drawMapContent(
                    canvas = canvas,
                    mapRect = mapRect,
                    data = data,
                    transform = transform,
                    palette = palette,
                    typeface = typeface,
                    insetBitmap = insetBitmap,
                    sections = sections
                )
                polygonsDrawn = pCount
                dimLabelsDrawn = dCount
            }
        }

        cursorY = mapBottom + 4f

        // 4. Certifier & Scale Bar Section
        if (sections.isVisible(ReportSectionId.CERTIFIER)) {
            drawCertifierAndScaleRow(
                canvas = canvas,
                left = contentLeft + 8f,
                top = cursorY,
                width = contentW - 16f,
                height = certifierH,
                data = data,
                transform = transform,
                palette = palette,
                typeface = typeface,
                signatureBitmap = signatureBitmap,
                sections = sections
            )
            cursorY += certifierH + 4f
        }

        // 5. Info Columns (Disclaimer / Area Table / Legend)
        var areaRowsDrawn = 0
        if (infoH > 0f) {
            areaRowsDrawn = drawInfoColumns(
                canvas = canvas,
                left = contentLeft + 8f,
                top = cursorY,
                width = contentW - 16f,
                height = infoH,
                data = data,
                palette = palette,
                typeface = typeface,
                sections = sections
            )
        }

        // 6. Footer
        if (footerH > 0f) {
            drawFooter(canvas, contentLeft + 8f, contentBottom - footerH, contentW - 16f, footerH, data, typeface)
        }

        // Watermark overlay
        if (data.ticket.showWatermark && data.ticket.watermarkText.isNotBlank()) {
            drawWatermark(canvas, pageW, pageH, data.ticket.watermarkText, typeface)
        }

        return RenderTelemetry(
            polygonsDrawn = polygonsDrawn,
            dimensionLabelsDrawn = dimLabelsDrawn,
            areaTableRowsDrawn = areaRowsDrawn
        )
    }

    private fun drawHeader(
        canvas: Canvas,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        data: SurveyReportData,
        palette: TemplatePalette,
        typeface: Typeface,
        logoBitmap: Bitmap?,
        qrBitmap: Bitmap?,
        sections: ReportSectionLayout
    ) {
        val textStartX = if (sections.showLogo && logoBitmap != null) {
            canvas.drawBitmap(logoBitmap, null, RectF(left, top + 2f, left + 36f, top + 38f), null)
            left + 44f
        } else {
            left
        }

        val companyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            color = palette.headerAccent
            textSize = 11.5f
            isFakeBoldText = true
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            color = INK_SECONDARY
            textSize = 8.5f
        }
        canvas.drawText(data.ticket.companyName, textStartX, top + 14f, companyPaint)
        val mouzaLine = "মৌজাঃ ${data.mouzaName} • জে.এলঃ ${data.jlNumber} • স্কেলঃ ${data.scaleText} = ১ মাইল (৩৩০ ফিট)"
        canvas.drawText(mouzaLine, textStartX, top + 28f, subPaint)

        if (data.ticket.showQr && sections.showQrHeader && qrBitmap != null) {
            val qrSize = (height - 4f).coerceAtMost(38f)
            val qrRect = RectF(left + width - qrSize, top + 2f, left + width, top + 2f + qrSize)
            canvas.drawBitmap(qrBitmap, null, qrRect, null)
        }

        val divPaint = Paint().apply {
            color = palette.headerAccent
            strokeWidth = 1.2f
        }
        canvas.drawLine(left, top + height - 2f, left + width, top + height - 2f, divPaint)
    }

    private fun drawTitleAndCompass(
        canvas: Canvas,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        data: SurveyReportData,
        palette: TemplatePalette,
        typeface: Typeface
    ) {
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            color = palette.headerAccent
            textSize = 15f
            isFakeBoldText = true
        }
        canvas.drawText(data.ticket.title, left, top + 16f, titlePaint)

        if (data.ticket.showDate) {
            val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.typeface = typeface
                color = INK_SECONDARY
                textSize = 9f
            }
            canvas.drawText("তারিখঃ ${data.ticket.dateText}", left, top + 30f, datePaint)
        }

        if (data.ticket.showNorthArrow) {
            val scaleFactor = (data.pad.northArrowSizePercent.coerceIn(60, 160) / 100f)
            val radius = 12f * scaleFactor
            val cx = left + width - radius - 8f
            val cy = top + height * 0.5f
            drawNorthCompass(canvas, cx, cy, radius, palette.headerAccent, typeface)
        }
    }

    private fun drawNorthCompass(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        accentColor: Int,
        typeface: Typeface
    ) {
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.STROKE
            strokeWidth = 1.1f
        }
        canvas.drawCircle(cx, cy, radius, ringPaint)

        val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.FILL
        }
        val path = Path().apply {
            moveTo(cx, cy - radius * 0.72f)
            lineTo(cx - radius * 0.28f, cy + radius * 0.25f)
            lineTo(cx, cy + radius * 0.05f)
            lineTo(cx + radius * 0.28f, cy + radius * 0.25f)
            close()
        }
        canvas.drawPath(path, arrowPaint)

        val dirPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            color = INK
            textSize = (7.5f * (radius / 12f)).coerceIn(5.5f, 12f)
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        canvas.drawText("উ", cx, cy - radius - 2f, dirPaint)
        canvas.drawText("দ", cx, cy + radius + dirPaint.textSize, dirPaint)
        canvas.drawText("পূ", cx + radius + 6f, cy + 3f, dirPaint)
        canvas.drawText("প", cx - radius - 6f, cy + 3f, dirPaint)
    }

    private fun drawEmptySelectionPlaceholder(
        canvas: Canvas,
        mapRect: RectF,
        palette: TemplatePalette,
        typeface: Typeface
    ) {
        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ROW_ALT_GREY
            style = Paint.Style.FILL
        }
        canvas.drawRect(mapRect, boxPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = BOX_GREY
            style = Paint.Style.STROKE
            strokeWidth = 1f
            pathEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f)
        }
        canvas.drawRect(mapRect, borderPaint)

        val mainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            color = palette.headerAccent
            textSize = 13f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            color = INK_SECONDARY
            textSize = 9.5f
            textAlign = Paint.Align.CENTER
        }
        val cx = mapRect.centerX()
        val cy = mapRect.centerY()
        canvas.drawText("রিপোর্টের জন্য প্লট নির্বাচন করুন", cx, cy - 6f, mainPaint)
        canvas.drawText("ম্যাপে প্লটে ট্যাপ করে রিপোর্টে যুক্ত করুন", cx, cy + 12f, hintPaint)
    }

    private fun drawMapContent(
        canvas: Canvas,
        mapRect: RectF,
        data: SurveyReportData,
        transform: ReportMapTransform,
        palette: TemplatePalette,
        typeface: Typeface,
        insetBitmap: Bitmap?,
        sections: ReportSectionLayout
    ): Pair<Int, Int> {
        canvas.save()
        canvas.clipRect(mapRect)

        // Base map inset (filtered identically to canvas)
        if (sections.showInset && insetBitmap != null && !insetBitmap.isRecycled && data.pad.layerVisible("layer_base")) {
            val baseAlpha = (data.pad.mapOpacity * data.pad.layerOpacity("layer_base")).coerceIn(0f, 1f)
            val filtered = BitmapMapFilters.apply(
                source = insetBitmap,
                brightness = data.pad.brightness,
                contrast = data.pad.contrast,
                grayscale = data.pad.grayscale,
                opacity = baseAlpha
            ) ?: insetBitmap
            canvas.drawBitmap(filtered, null, mapRect, null)
            if (filtered !== insetBitmap) filtered.recycle()
        }

        // Survey Grid
        if (data.pad.showGrid && data.pad.layerVisible("layer_grid")) {
            val gridColor = parseColorSafe(data.pad.gridColorHex, RULER_GREY)
            val gridAlpha = scaledAlpha(0.35f, data.pad.layerOpacity("layer_grid"))
            val gridPaint = Paint().apply {
                color = gridColor
                alpha = gridAlpha
                strokeWidth = 0.6f
            }
            val stepPx = (data.pad.gridSpacingUnits * transform.scale).coerceIn(12f, 120f)
            var gx = mapRect.left
            while (gx <= mapRect.right) {
                canvas.drawLine(gx, mapRect.top, gx, mapRect.bottom, gridPaint)
                gx += stepPx
            }
            var gy = mapRect.top
            while (gy <= mapRect.bottom) {
                canvas.drawLine(mapRect.left, gy, mapRect.right, gy, gridPaint)
                gy += stepPx
            }
        }

        val drawOrderPlots = data.roadPlots + data.landPlots
        var polygonsDrawn = 0
        var dimLabelsDrawn = 0
        val placedLabelRects = mutableListOf<RectF>()

        for (plot in drawOrderPlots) {
            if (plot.points.size < 3) continue
            val layerId = if (plot.isRoad) "layer_roads" else "layer_plots"
            if (!data.pad.layerVisible(layerId)) continue
            val layerAlpha = data.pad.layerOpacity(layerId)

            val mapped = plot.points.map { transform.worldToCanvas(it) }
            val path = Path().apply {
                moveTo(mapped[0].x, mapped[0].y)
                for (i in 1 until mapped.size) lineTo(mapped[i].x, mapped[i].y)
                close()
            }

            // Fill
            val fillColor = if (plot.isRoad) HATCH_GREY else parseColorSafe(plot.colorHex, palette.fillDefault)
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = fillColor
                style = Paint.Style.FILL
                val baseFillAlpha = if (plot.isRoad) 0.45f else 0.14f * data.pad.plotFillOpacity
                alpha = scaledAlpha(baseFillAlpha, layerAlpha)
            }
            canvas.drawPath(path, fillPaint)

            // White CAD outline underneath colored stroke
            val baseStroke = 1.6f * data.pad.lineScale
            val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = PAGE_WHITE
                style = Paint.Style.STROKE
                strokeWidth = baseStroke * (1f + data.pad.outlineScale * 1.4f)
                strokeJoin = Paint.Join.ROUND
                alpha = scaledAlpha(0.95f, layerAlpha)
            }
            canvas.drawPath(path, outlinePaint)

            // Colored boundary stroke
            val strokeColor = data.pad.strokeColorHexOverride?.let { parseColorSafe(it, palette.boundaryDefault) }
                ?: parseColorSafe(plot.colorHex, palette.boundaryDefault)
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = strokeColor
                style = Paint.Style.STROKE
                strokeWidth = baseStroke
                strokeJoin = Paint.Join.ROUND
                alpha = scaledAlpha(1f, layerAlpha)
            }
            canvas.drawPath(path, strokePaint)
            polygonsDrawn++

            // Corner points & vertex numbers
            if (data.pad.showCornerPoints) {
                val radius = (3f * data.pad.pointScale).coerceIn(1.2f, 8f)
                val dotFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = PAGE_WHITE
                    style = Paint.Style.FILL
                }
                val dotStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = strokeColor
                    style = Paint.Style.STROKE
                    strokeWidth = 1.1f
                }
                val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.typeface = typeface
                    color = INK
                    textSize = 6.8f
                    isFakeBoldText = true
                }
                mapped.forEachIndexed { idx, pt ->
                    canvas.drawCircle(pt.x, pt.y, radius, dotFill)
                    canvas.drawCircle(pt.x, pt.y, radius, dotStroke)
                    if (data.pad.showVertexNumbers) {
                        canvas.drawText(BengaliFormatters.toBengaliDigits(idx + 1), pt.x + radius + 2f, pt.y - 2f, numPaint)
                    }
                }
            }

            // Dimension labels
            if (sections.showDimensionLabels && data.pad.layerVisible("layer_measure")) {
                val measureAlpha = data.pad.layerOpacity("layer_measure")
                val style = plot.labelStyle
                val textColor = style.colorHex?.let { parseColorSafe(it, palette.dimensionColor) }
                    ?: parseColorSafe(data.pad.measurementColorHex, palette.dimensionColor)
                val textSize = (7.5f * data.pad.fontScale * style.fontSizeScale).coerceIn(5.5f, 20f)
                val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.typeface = if (style.italic) Typeface.create(typeface, Typeface.ITALIC) else typeface
                    color = textColor
                    this.textSize = textSize
                    isFakeBoldText = style.bold
                    isUnderlineText = style.underline
                    letterSpacing = style.letterSpacingSp / textSize.coerceAtLeast(1f)
                    textAlign = Paint.Align.CENTER
                    alpha = scaledAlpha(style.opacity, measureAlpha)
                }

                val signed = GeometryUtils.signedAreaUnitsSq(mapped)
                val signMultiplier = if (signed >= 0) 1f else -1f
                val outwardSign = when {
                    !data.pad.showMeasurementsOutside -> -1f
                    data.pad.effectiveTextPosition() == MapTextPosition.BELOW -> -1f
                    data.pad.effectiveTextPosition() == MapTextPosition.INSIDE -> -0.35f
                    else -> 1f
                }

                val bbox = GeometryUtils.boundingBox(mapped)
                for (i in mapped.indices) {
                    val label = plot.dimensionLabels.getOrNull(i).orEmpty()
                    if (label.isBlank()) continue
                    val a = mapped[i]
                    val b = mapped[(i + 1) % mapped.size]
                    val dx = b.x - a.x
                    val dy = b.y - a.y
                    val len = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(1f)
                    val nx = (dy / len) * signMultiplier * outwardSign
                    val ny = (-dx / len) * signMultiplier * outwardSign
                    val offsetDist = 13f * data.pad.fontScale
                    var lx = (a.x + b.x) * 0.5f + nx * offsetDist
                    var ly = (a.y + b.y) * 0.5f + ny * offsetDist
                    if (style.manuallyPositioned) {
                        lx += style.offsetX * bbox.width * 0.25f
                        ly += style.offsetY * bbox.height * 0.25f
                    }

                    val textW = dimPaint.measureText(label)
                    val labelH = textSize + 2f
                    var candidate = RectF(lx - textW * 0.5f - 2f, ly - labelH * 0.5f, lx + textW * 0.5f + 2f, ly + labelH * 0.5f)
                    if (data.pad.labelCollisionAvoidance) {
                        var tries = 0
                        while (tries < 6 && placedLabelRects.any { RectF.intersects(it, candidate) }) {
                            val shift = (labelH + 1.5f) * (tries + 1)
                            lx += nx * shift
                            ly += ny * shift
                            candidate = RectF(lx - textW * 0.5f - 2f, ly - labelH * 0.5f, lx + textW * 0.5f + 2f, ly + labelH * 0.5f)
                            tries++
                        }
                    }
                    placedLabelRects.add(candidate)

                    if (style.backgroundHex != null) {
                        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = parseColorSafe(style.backgroundHex, PAGE_WHITE)
                            alpha = (style.backgroundOpacity * 255f).toInt().coerceIn(0, 255)
                        }
                        canvas.drawRoundRect(candidate, 3f, 3f, bgPaint)
                    }

                    canvas.save()
                    if (abs(style.rotationDeg) > 0.5f) {
                        canvas.rotate(style.rotationDeg, lx, ly)
                    }
                    canvas.drawText(label, lx, ly + textSize * 0.32f, dimPaint)
                    canvas.restore()
                    dimLabelsDrawn++
                }
            }

            // Central Plot Label
            if (!plot.isRoad && data.pad.layerVisible("layer_labels") && data.pad.showSelectedAreaLabel) {
                val style = plot.labelStyle
                val c = GeometryUtils.centroid(mapped)
                val bbox = GeometryUtils.boundingBox(mapped)
                val cx = if (style.manuallyPositioned) c.x + style.offsetX * bbox.width else c.x
                val cy = if (style.manuallyPositioned) c.y + style.offsetY * bbox.height else c.y
                val centerScale = (data.pad.fontScale * style.fontSizeScale).coerceIn(0.6f, 2.4f)
                val labelAlpha = scaledAlpha(style.opacity, data.pad.layerOpacity("layer_labels"))

                val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.typeface = if (style.italic) Typeface.create(typeface, Typeface.ITALIC) else typeface
                    color = style.colorHex?.let { parseColorSafe(it, INK) } ?: INK
                    textSize = 8.8f * centerScale
                    isFakeBoldText = style.bold
                    isUnderlineText = style.underline
                    textAlign = Paint.Align.CENTER
                    alpha = labelAlpha
                }
                val areaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.typeface = typeface
                    color = palette.boundaryDefault
                    textSize = 8.0f * centerScale
                    isFakeBoldText = true
                    textAlign = Paint.Align.CENTER
                    alpha = labelAlpha
                }

                val dagPart = if (plot.dagNumber.isNotBlank()) " (দাগ ${plot.dagNumber})" else ""
                val line1 = "${plot.name}$dagPart"
                canvas.save()
                if (abs(style.rotationDeg) > 0.5f) {
                    canvas.rotate(style.rotationDeg, cx, cy)
                }
                canvas.drawText(line1, cx, cy - 2f, namePaint)
                canvas.drawText(plot.areaLabel, cx, cy + areaPaint.textSize + 1f, areaPaint)
                canvas.restore()
            }
        }

        // Crosshair
        if (data.pad.showCrosshair) {
            val crossPaint = Paint().apply {
                color = INK_MUTED
                strokeWidth = 0.7f
                pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
            }
            canvas.drawLine(mapRect.centerX(), mapRect.top, mapRect.centerX(), mapRect.bottom, crossPaint)
            canvas.drawLine(mapRect.left, mapRect.centerY(), mapRect.right, mapRect.centerY(), crossPaint)
        }

        canvas.restore()
        return polygonsDrawn to dimLabelsDrawn
    }

    private fun drawCertifierAndScaleRow(
        canvas: Canvas,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        data: SurveyReportData,
        transform: ReportMapTransform,
        palette: TemplatePalette,
        typeface: Typeface,
        signatureBitmap: Bitmap?,
        sections: ReportSectionLayout
    ) {
        val linePaint = Paint().apply { color = BOX_GREY; strokeWidth = 0.7f }
        canvas.drawLine(left, top, left + width, top, linePaint)

        val infoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            color = INK
            textSize = 8.5f
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            color = palette.inspectorColor
            textSize = 8.5f
            isFakeBoldText = true
        }

        val surveyorLine = "বসতমূলকারীঃ ডিজিটাল সার্ভেয়ার মোঃ ${data.ticket.surveyorName}"
        canvas.drawText(surveyorLine, left, top + 14f, boldPaint)

        val feetPerInch = transform.feetPerPaperInch(data.feetPerCanvasUnit)
        val scaleLabel = "ক্ষেত্র ১ ইঞ্চি = ${BengaliFormatters.formatDecimal(feetPerInch, 1)} ফিট"
        val secondLine = buildString {
            append(scaleLabel)
            if (data.pad.showPerimeterLabel && data.totalPerimeterLabel.isNotBlank()) {
                append("   •   পরিধিঃ ")
                append(data.totalPerimeterLabel)
            }
        }
        canvas.drawText(secondLine, left, top + 28f, infoPaint)

        // Alternating scale bar (66pt)
        if (data.ticket.showScaleBar) {
            val barWidthPt = 66f
            val barY = top + 36f
            val segW = barWidthPt / 4f
            for (s in 0 until 4) {
                val segPaint = Paint().apply {
                    color = if (s % 2 == 0) INK else PAGE_WHITE
                    style = Paint.Style.FILL
                }
                val r = RectF(left + s * segW, barY, left + (s + 1) * segW, barY + 5f)
                canvas.drawRect(r, segPaint)
                canvas.drawRect(r, Paint().apply { color = INK; style = Paint.Style.STROKE; strokeWidth = 0.6f })
            }
            val barUnit = if (data.pad.scaleBarUnitId == "inherit") {
                data.pad.effectiveUnit()
            } else {
                MeasurementUnit.fromId(data.pad.scaleBarUnitId)
            }
            val barLengthFeet = feetPerInch * (barWidthPt / 72.0)
            val barText = LengthFormatter.format(barLengthFeet, barUnit, data.pad.decimalPrecision, useBengali = true)
            val barTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.typeface = typeface
                color = INK_SECONDARY
                textSize = 7.5f
            }
            canvas.drawText(barText, left + barWidthPt + 6f, barY + 5f, barTextPaint)
        }

        if (sections.showInspectorBox) {
            val boxW = 128f
            val boxRect = RectF(left + width - boxW, top + 4f, left + width, top + height - 4f)
            canvas.drawRect(boxRect, Paint().apply { color = BOX_GREY; style = Paint.Style.STROKE; strokeWidth = 0.7f })
            if (signatureBitmap != null && !signatureBitmap.isRecycled) {
                canvas.drawBitmap(signatureBitmap, null, RectF(boxRect.left + 6f, boxRect.top + 4f, boxRect.right - 6f, boxRect.bottom - 14f), null)
            }
            val sigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.typeface = typeface
                color = INK_SECONDARY
                textSize = 7.5f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("সার্ভেয়ার স্বাক্ষর", boxRect.centerX(), boxRect.bottom - 4f, sigPaint)
        }
    }

    private fun drawInfoColumns(
        canvas: Canvas,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        data: SurveyReportData,
        palette: TemplatePalette,
        typeface: Typeface,
        sections: ReportSectionLayout
    ): Int {
        val visibleCols = sections.orderedInfoColumns().filter { col ->
            when (col) {
                ReportSectionId.DISCLAIMER -> sections.isVisible(ReportSectionId.DISCLAIMER)
                ReportSectionId.AREA_TABLE -> sections.isVisible(ReportSectionId.AREA_TABLE) && data.ticket.showAreaSummary
                ReportSectionId.LEGEND -> sections.isVisible(ReportSectionId.LEGEND) && data.ticket.showLegend
                else -> false
            }
        }
        if (visibleCols.isEmpty()) return 0

        val gap = 6f
        val colW = (width - gap * (visibleCols.size - 1)) / visibleCols.size
        var areaRowsDrawn = 0

        visibleCols.forEachIndexed { idx, col ->
            val colLeft = left + idx * (colW + gap)
            val rect = RectF(colLeft, top, colLeft + colW, top + height)
            canvas.drawRect(rect, Paint().apply { color = BOX_GREY; style = Paint.Style.STROKE; strokeWidth = 0.7f })
            when (col) {
                ReportSectionId.DISCLAIMER -> drawDisclaimerBox(canvas, rect, data, palette, typeface)
                ReportSectionId.AREA_TABLE -> areaRowsDrawn = drawAreaTableBox(canvas, rect, data, palette, typeface, sections)
                ReportSectionId.LEGEND -> drawLegendBox(canvas, rect, palette, typeface)
                else -> Unit
            }
        }
        return areaRowsDrawn
    }

    private fun drawDisclaimerBox(
        canvas: Canvas,
        rect: RectF,
        data: SurveyReportData,
        palette: TemplatePalette,
        typeface: Typeface
    ) {
        val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            color = palette.disclaimerColor
            textSize = 8.5f
            isFakeBoldText = true
        }
        canvas.drawText("দাবিত্যাগ ও নোট", rect.left + 6f, rect.top + 14f, headPaint)

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            color = INK_SECONDARY
            textSize = 7.4f
        }
        val lines = wrapLines(data.ticket.disclaimerText, bodyPaint, rect.width() - 12f)
        var y = rect.top + 28f
        for (line in lines.take(7)) {
            canvas.drawText(line, rect.left + 6f, y, bodyPaint)
            y += 11f
        }
    }

    private fun drawAreaTableBox(
        canvas: Canvas,
        rect: RectF,
        data: SurveyReportData,
        palette: TemplatePalette,
        typeface: Typeface,
        sections: ReportSectionLayout
    ): Int {
        val headerRect = RectF(rect.left, rect.top, rect.right, rect.top + 18f)
        canvas.drawRect(headerRect, Paint().apply { color = palette.tableHeader; style = Paint.Style.FILL })
        val thPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            color = PAGE_WHITE
            textSize = 8f
            isFakeBoldText = true
        }
        canvas.drawText("প্লট / প্লটের পরিমান", rect.left + 6f, rect.top + 12.5f, thPaint)

        val rowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            color = INK
            textSize = 7.8f
        }
        val dimSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            color = INK_MUTED
            textSize = 6.8f
        }
        var y = rect.top + 30f
        var rowsDrawn = 0
        val shownPlots = data.landPlots.take(2)
        for (plot in shownPlots) {
            val dagSuffix = if (plot.dagNumber.isNotBlank()) " (${plot.dagNumber})" else ""
            canvas.drawText("${plot.name}$dagSuffix: ${plot.areaLabel}", rect.left + 6f, y, rowPaint)
            if (sections.showFrontageDepth && plot.frontageFeet > 0.0) {
                val unit = data.pad.effectiveUnit()
                val fStr = LengthFormatter.format(plot.frontageFeet, unit, data.pad.decimalPrecision, useBengali = true)
                val dStr = LengthFormatter.format(plot.depthFeet, unit, data.pad.decimalPrecision, useBengali = true)
                canvas.drawText("সম্মুখ: $fStr × গভীরতা: $dStr", rect.left + 6f, y + 10f, dimSubPaint)
                y += 24f
            } else {
                y += 16f
            }
            rowsDrawn++
        }
        if (data.landPlots.size > 2) {
            canvas.drawText("+ আরও ${BengaliFormatters.toBengaliDigits(data.landPlots.size - 2)} টি প্লট", rect.left + 6f, y, dimSubPaint)
            y += 12f
        }
        if (sections.showTotalRow && data.pad.showTotalAreaLabel) {
            val totalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.typeface = typeface
                color = palette.tableHeader
                textSize = 8.4f
                isFakeBoldText = true
            }
            canvas.drawText(data.totalAreaLabel, rect.left + 6f, rect.bottom - 8f, totalPaint)
        }
        return rowsDrawn
    }

    private fun drawLegendBox(
        canvas: Canvas,
        rect: RectF,
        palette: TemplatePalette,
        typeface: Typeface
    ) {
        val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            color = palette.headerAccent
            textSize = 8.5f
            isFakeBoldText = true
        }
        canvas.drawText("লিজেন্ড (সংকেত)", rect.left + 6f, rect.top + 14f, headPaint)

        val itemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            color = INK
            textSize = 7.8f
        }
        // Land line
        canvas.drawLine(
            rect.left + 8f, rect.top + 30f, rect.left + 28f, rect.top + 30f,
            Paint().apply { color = palette.boundaryDefault; strokeWidth = 2.2f }
        )
        canvas.drawText("জমির লাইন", rect.left + 34f, rect.top + 33f, itemPaint)

        // Dimension line
        canvas.drawLine(
            rect.left + 8f, rect.top + 48f, rect.left + 28f, rect.top + 48f,
            Paint().apply { color = palette.dimensionColor; strokeWidth = 1.6f }
        )
        canvas.drawText("ডাইমেনশন লাইন", rect.left + 34f, rect.top + 51f, itemPaint)

        // Road fill
        val roadRect = RectF(rect.left + 8f, rect.top + 61f, rect.left + 28f, rect.top + 71f)
        canvas.drawRect(roadRect, Paint().apply { color = HATCH_GREY; style = Paint.Style.FILL })
        canvas.drawText("রাস্তা / পথ", rect.left + 34f, rect.top + 69f, itemPaint)
    }

    private fun drawFooter(
        canvas: Canvas,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        data: SurveyReportData,
        typeface: Typeface
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            color = INK_MUTED
            textSize = 7.8f
        }
        canvas.drawText(data.ticket.footerLine, left, top + height - 6f, paint)
        val rightPaint = Paint(paint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText(data.ticket.website, left + width, top + height - 6f, rightPaint)
    }

    private fun drawWatermark(
        canvas: Canvas,
        pageW: Float,
        pageH: Float,
        text: String,
        typeface: Typeface
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            color = INK_MUTED
            alpha = 32
            textSize = 38f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.save()
        canvas.rotate(-32f, pageW * 0.5f, pageH * 0.5f)
        canvas.drawText(text, pageW * 0.5f, pageH * 0.5f, paint)
        canvas.restore()
    }

    fun wrapLines(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank() || maxWidth <= 10f) return emptyList()
        val words = text.split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = StringBuilder(candidate)
            } else {
                if (current.isNotEmpty()) lines.add(current.toString())
                current = StringBuilder(word)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines
    }

    private fun parseColorSafe(hex: String?, fallback: Int): Int {
        if (hex.isNullOrBlank()) return fallback
        return runCatching { Color.parseColor(hex) }.getOrDefault(fallback)
    }

    private fun scaledAlpha(baseFraction: Float, layerOpacity: Float): Int =
        (baseFraction.coerceIn(0f, 1f) * layerOpacity.coerceIn(0f, 1f) * 255f).toInt().coerceIn(0, 255)
}
