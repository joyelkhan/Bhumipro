package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.model.CanvasDisplaySettings
import com.example.data.model.GisLayerItem
import com.example.data.model.LandPlot
import com.example.data.model.LengthFormatter
import com.example.data.model.MapTextPosition
import com.example.data.model.MeasurementUnit
import com.example.data.model.PlotLabelStyle
import com.example.data.model.PointF
import com.example.data.model.ZoomTextMode
import com.example.domain.BengaliFormatters
import com.example.domain.BoundingBox
import com.example.domain.CadOverlayEntity
import com.example.domain.GeometryUtils
import com.example.domain.JtsDivisionEngine
import com.example.domain.MapTransformMath
import com.example.domain.PdfAndPrintHelper
import com.example.domain.SpatialIndex
import com.example.ui.viewmodel.CanvasViewportCommand
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun InteractiveMapCanvas(
    pdfBitmap: Bitmap?,
    plots: List<LandPlot>,
    previewDividedPlots: List<LandPlot>,
    activePlotIndex: Int,
    selectedPlotIds: Set<String>,
    selectedVertexIndex: Int?,
    activeTool: String,
    isCalibrating: Boolean,
    calibrationPoints: List<PointF>,
    drawingPoints: List<PointF>,
    tapeMeasurePoints: List<PointF>,
    cadOverlays: List<CadOverlayEntity>,
    layers: List<GisLayerItem>,
    canvasSettings: CanvasDisplaySettings,
    feetPerCanvasUnit: Double,
    sqFeetPerShotok: Double,
    baseStrokeWidthPx: Float,
    baseLabelFontSizeSp: Float,
    rotationDeg: Float,
    showMinimap: Boolean,
    viewportCommand: CanvasViewportCommand?,
    onTapSelectOrDraw: (PointF, SpatialIndex) -> Unit,
    onVertexSelected: (Int?) -> Unit,
    onVertexDragStart: (Int) -> Unit,
    onVertexDragMove: (PointF) -> Unit,
    onTapeTap: (PointF) -> Unit,
    onCalibrationTap: (PointF) -> Unit,
    onLongPressPlot: (LandPlot) -> Unit,
    modifier: Modifier = Modifier,
    manualSplitPoints: List<PointF> = emptyList(),
    roadWidthFeet: Float = 6f,
    useCustomRoadCenterline: Boolean = false,
    onRoadNodeDragMove: (Int, PointF) -> Unit = { _, _ -> },
    onRoadBendInsert: (Int, PointF) -> Unit = { _, _ -> },
    onDeleteSelectedVertex: () -> Unit = {},
    onCloseDrawingPolygon: () -> Unit = {}
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val typeface = remember(context) { PdfAndPrintHelper.resolveBengaliTypeface(context) }

    val spatialIndex = remember(plots) { SpatialIndex(plots) }
    val layerMap = remember(layers) { layers.associateBy { it.id } }

    fun layerVisible(id: String): Boolean = layerMap[id]?.visible ?: true
    fun layerOpacity(id: String): Float = (layerMap[id]?.opacity ?: 1f).coerceIn(0.05f, 1f)

    val unit = canvasSettings.effectiveUnit()
    val precision = canvasSettings.effectivePrecision()
    val dimCache = remember(unit, precision) { HashMap<Long, String>() }
    fun cachedFormat(feet: Double): String {
        val key = (feet * 1000.0).toLong()
        return dimCache.getOrPut(key) {
            LengthFormatter.format(feet, unit, precision, useBengali = true)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF3F5F4))
            .testTag("interactive_map_canvas")
    ) {
        val viewW = with(LocalDensity.current) { maxWidth.toPx() }.coerceAtLeast(1f)
        val viewH = with(LocalDensity.current) { maxHeight.toPx() }.coerceAtLeast(1f)

        var viewport by remember {
            val worldW = (pdfBitmap?.width?.toFloat() ?: 1400f)
            val worldH = (pdfBitmap?.height?.toFloat() ?: 1000f)
            mutableStateOf(
                MapTransformMath.computeFitTransform(
                    BoundingBox(0f, 0f, worldW, worldH),
                    viewW,
                    viewH
                )
            )
        }

        LaunchedEffect(viewportCommand?.id) {
            val cmd = viewportCommand ?: return@LaunchedEffect
            when (cmd.type) {
                "fit_screen" -> {
                    val allPts = plots.flatMap { it.points }
                    val box = if (allPts.isNotEmpty()) {
                        GeometryUtils.boundingBox(allPts)
                    } else {
                        BoundingBox(
                            0f, 0f,
                            pdfBitmap?.width?.toFloat() ?: 1400f,
                            pdfBitmap?.height?.toFloat() ?: 1000f
                        )
                    }
                    viewport = MapTransformMath.computeFitTransform(box, viewW, viewH)
                }
                "fit_plot" -> {
                    val active = plots.getOrNull(activePlotIndex)
                    if (active != null && active.points.isNotEmpty()) {
                        viewport = MapTransformMath.computeFitTransform(
                            GeometryUtils.boundingBox(active.points),
                            viewW,
                            viewH,
                            paddingRatio = 0.14f
                        )
                    }
                }
                "zoom_percent" -> {
                    val targetScale = MapTransformMath.zoomPercentToScale(cmd.zoomPercent)
                    val factor = targetScale / viewport.zoom.coerceAtLeast(0.01f)
                    viewport = MapTransformMath.applyFocalZoom(viewport, factor, viewW * 0.5f, viewH * 0.5f)
                }
                "zoom_in" -> {
                    viewport = MapTransformMath.applyFocalZoom(viewport, 1.25f, viewW * 0.5f, viewH * 0.5f)
                }
                "zoom_out" -> {
                    viewport = MapTransformMath.applyFocalZoom(viewport, 0.8f, viewW * 0.5f, viewH * 0.5f)
                }
            }
        }

        val currentViewport = viewport.copy(rotationDeg = rotationDeg)
        val worldCenterX = (pdfBitmap?.width?.toFloat() ?: 1400f) * 0.5f
        val worldCenterY = (pdfBitmap?.height?.toFloat() ?: 1000f) * 0.5f

        var draggingVertex by remember { mutableStateOf<Int?>(null) }
        var draggingRoadNode by remember { mutableStateOf<Int?>(null) }
        var activeTouchWorld by remember { mutableStateOf<PointF?>(null) }

        val latestViewport by rememberUpdatedState(currentViewport)
        val latestActiveTool by rememberUpdatedState(activeTool)
        val latestPlots by rememberUpdatedState(plots)
        val latestActivePlotIndex by rememberUpdatedState(activePlotIndex)
        val latestSelectedVertex by rememberUpdatedState(selectedVertexIndex)
        val latestRoadPoints by rememberUpdatedState(manualSplitPoints)
        val latestDrawingPoints by rememberUpdatedState(drawingPoints)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Final)
                            val pressed = event.changes.firstOrNull { it.pressed }
                            if (pressed == null) {
                                draggingVertex = null
                                draggingRoadNode = null
                                activeTouchWorld = null
                            } else if (latestActiveTool == "draw") {
                                activeTouchWorld = MapTransformMath.screenToWorld(
                                    pressed.position.x,
                                    pressed.position.y,
                                    latestViewport,
                                    worldCenterX,
                                    worldCenterY
                                )
                            }
                        }
                    }
                }
                .pointerInput(isCalibrating) {
                    detectTapGestures(
                        onTap = { tapOffset ->
                            val vp = latestViewport
                            val world = MapTransformMath.screenToWorld(
                                tapOffset.x, tapOffset.y, vp, worldCenterX, worldCenterY
                            )
                            val tool = latestActiveTool
                            when {
                                isCalibrating -> onCalibrationTap(world)
                                tool == "pan" -> Unit
                                tool == "draw" -> {
                                    val pts = latestDrawingPoints
                                    if (pts.size >= 3) {
                                        val firstScreen = MapTransformMath.worldToScreen(
                                            pts.first().x, pts.first().y, vp, worldCenterX, worldCenterY
                                        )
                                        val distFirstPx = hypot(
                                            (firstScreen.x - tapOffset.x).toDouble(),
                                            (firstScreen.y - tapOffset.y).toDouble()
                                        )
                                        if (distFirstPx <= 24f * density) {
                                            onCloseDrawingPolygon()
                                            return@detectTapGestures
                                        }
                                    }
                                    onTapSelectOrDraw(world, spatialIndex)
                                }
                                tool == "measure" -> onTapeTap(world)
                                tool == "interactive_road" -> {
                                    // Check orange bend midpoint handles on tap too
                                    val roadPts = latestRoadPoints
                                    var inserted = false
                                    for (i in 0 until roadPts.size - 1) {
                                        val mid = PointF(
                                            (roadPts[i].x + roadPts[i + 1].x) * 0.5f,
                                            (roadPts[i].y + roadPts[i + 1].y) * 0.5f
                                        )
                                        val midScreen = MapTransformMath.worldToScreen(mid.x, mid.y, vp, worldCenterX, worldCenterY)
                                        if (hypot((midScreen.x - tapOffset.x).toDouble(), (midScreen.y - tapOffset.y).toDouble()) <= 26f * density) {
                                            onRoadBendInsert(i + 1, mid)
                                            inserted = true
                                            break
                                        }
                                    }
                                    if (!inserted && roadPts.size < 2) {
                                        onRoadBendInsert(roadPts.size, world)
                                    }
                                }
                                else -> {
                                    val active = latestPlots.getOrNull(latestActivePlotIndex)
                                    val hitTolWorld = (22f * density) / vp.zoom.coerceAtLeast(0.1f)
                                    val vertexHit = active?.points?.indexOfFirst { v ->
                                        hypot((v.x - world.x).toDouble(), (v.y - world.y).toDouble()) <= hitTolWorld
                                    } ?: -1
                                    if (vertexHit >= 0) {
                                        onVertexSelected(vertexHit)
                                    } else {
                                        onVertexSelected(null)
                                        onTapSelectOrDraw(world, spatialIndex)
                                    }
                                }
                            }
                        },
                        onDoubleTap = { tapOffset ->
                            viewport = MapTransformMath.applyFocalZoom(
                                viewport, 1.35f, tapOffset.x, tapOffset.y
                            )
                        },
                        onLongPress = { tapOffset ->
                            val vp = latestViewport
                            val world = MapTransformMath.screenToWorld(
                                tapOffset.x, tapOffset.y, vp, worldCenterX, worldCenterY
                            )
                            val hit = spatialIndex.hitTest(world, (16f * density) / vp.zoom.coerceAtLeast(0.1f))
                            if (hit != null) {
                                onLongPressPlot(hit)
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val vp = latestViewport
                        val tool = latestActiveTool
                        if (abs(zoom - 1f) > 0.005f) {
                            viewport = MapTransformMath.applyFocalZoom(viewport, zoom, centroid.x, centroid.y)
                            return@detectTransformGestures
                        }

                        if (tool == "interactive_road") {
                            val roadPts = latestRoadPoints
                            if (draggingRoadNode != null) {
                                val screenTarget = Offset(centroid.x + pan.x, centroid.y + pan.y)
                                val world = MapTransformMath.screenToWorld(
                                    screenTarget.x, screenTarget.y, vp, worldCenterX, worldCenterY
                                )
                                activeTouchWorld = world
                                onRoadNodeDragMove(draggingRoadNode!!, world)
                                return@detectTransformGestures
                            }
                            // Hit test white main road nodes
                            val hitNodeIdx = roadPts.indexOfFirst { rpt ->
                                val scr = MapTransformMath.worldToScreen(rpt.x, rpt.y, vp, worldCenterX, worldCenterY)
                                hypot((scr.x - centroid.x).toDouble(), (scr.y - centroid.y).toDouble()) < 30f * density
                            }
                            if (hitNodeIdx >= 0) {
                                draggingRoadNode = hitNodeIdx
                                val screenTarget = Offset(centroid.x + pan.x, centroid.y + pan.y)
                                val world = MapTransformMath.screenToWorld(screenTarget.x, screenTarget.y, vp, worldCenterX, worldCenterY)
                                activeTouchWorld = world
                                onRoadNodeDragMove(hitNodeIdx, world)
                                return@detectTransformGestures
                            }
                            // Hit test orange midpoint bend handles
                            for (i in 0 until roadPts.size - 1) {
                                val mid = PointF(
                                    (roadPts[i].x + roadPts[i + 1].x) * 0.5f,
                                    (roadPts[i].y + roadPts[i + 1].y) * 0.5f
                                )
                                val midScr = MapTransformMath.worldToScreen(mid.x, mid.y, vp, worldCenterX, worldCenterY)
                                if (hypot((midScr.x - centroid.x).toDouble(), (midScr.y - centroid.y).toDouble()) < 26f * density) {
                                    val newIdx = i + 1
                                    onRoadBendInsert(newIdx, mid)
                                    draggingRoadNode = newIdx
                                    activeTouchWorld = mid
                                    return@detectTransformGestures
                                }
                            }
                        }

                        if (draggingVertex != null) {
                            val screenTarget = Offset(centroid.x + pan.x, centroid.y + pan.y)
                            val world = MapTransformMath.screenToWorld(
                                screenTarget.x, screenTarget.y, vp, worldCenterX, worldCenterY
                            )
                            activeTouchWorld = world
                            onVertexDragMove(world)
                        } else {
                            val active = latestPlots.getOrNull(latestActivePlotIndex)
                            val selV = latestSelectedVertex
                            if (active != null && !active.isLocked && selV != null && selV in active.points.indices) {
                                val vScreen = MapTransformMath.worldToScreen(
                                    active.points[selV].x, active.points[selV].y, vp, worldCenterX, worldCenterY
                                )
                                val distPx = hypot((vScreen.x - centroid.x).toDouble(), (vScreen.y - centroid.y).toDouble())
                                if (distPx < 30f * density) {
                                    draggingVertex = selV
                                    onVertexDragStart(selV)
                                    val screenTarget = Offset(centroid.x + pan.x, centroid.y + pan.y)
                                    val world = MapTransformMath.screenToWorld(
                                        screenTarget.x, screenTarget.y, vp, worldCenterX, worldCenterY
                                    )
                                    activeTouchWorld = world
                                    onVertexDragMove(world)
                                    return@detectTransformGestures
                                }
                            }
                            viewport = viewport.copy(
                                panX = viewport.panX + pan.x,
                                panY = viewport.panY + pan.y
                            )
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawIntoCanvas { composeCanvas ->
                    val nc = composeCanvas.nativeCanvas
                    val state = currentViewport

                    nc.save()
                    nc.translate(state.panX, state.panY)
                    nc.scale(state.zoom, state.zoom)
                    if (abs(state.rotationDeg) > 0.01f) {
                        nc.rotate(state.rotationDeg, worldCenterX, worldCenterY)
                    }

                    // 1. Base map raster
                    if (pdfBitmap != null && !pdfBitmap.isRecycled && layerVisible("layer_base")) {
                        val b = canvasSettings.effectiveBrightness()
                        val c = canvasSettings.effectiveContrast()
                        val cm = ColorMatrix()
                        if (canvasSettings.grayscale) cm.setSaturation(0f)
                        val scale = (1f + c * 1.5f).coerceIn(0.25f, 2.5f)
                        val translate = (-0.5f * scale + 0.5f + b) * 255f
                        cm.postConcat(
                            ColorMatrix(
                                floatArrayOf(
                                    scale, 0f, 0f, 0f, translate,
                                    0f, scale, 0f, 0f, translate,
                                    0f, 0f, scale, 0f, translate,
                                    0f, 0f, 0f, 1f, 0f
                                )
                            )
                        )
                        val basePaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
                            colorFilter = ColorMatrixColorFilter(cm)
                            alpha = (canvasSettings.effectiveMapOpacity() * layerOpacity("layer_base") * 255f)
                                .toInt().coerceIn(0, 255)
                        }
                        nc.drawBitmap(pdfBitmap, 0f, 0f, basePaint)
                    }

                    // 2. Survey Grid
                    if (layerVisible("layer_grid")) {
                        val spacing = canvasSettings.effectiveGridSpacing()
                        val gridPaint = Paint().apply {
                            color = parseColor(canvasSettings.gridColorHex, android.graphics.Color.parseColor("#133C2D"))
                            alpha = (layerOpacity("layer_grid") * 255f).toInt().coerceIn(0, 255)
                            strokeWidth = (1.0f / state.zoom).coerceAtLeast(0.4f)
                        }
                        val maxX = (pdfBitmap?.width?.toFloat() ?: 1600f) + spacing
                        val maxY = (pdfBitmap?.height?.toFloat() ?: 1200f) + spacing
                        var gx = 0f
                        while (gx <= maxX) {
                            nc.drawLine(gx, 0f, gx, maxY, gridPaint)
                            gx += spacing
                        }
                        var gy = 0f
                        while (gy <= maxY) {
                            nc.drawLine(0f, gy, maxX, gy, gridPaint)
                            gy += spacing
                        }
                    }

                    // 3. CAD overlay
                    if (layerVisible("layer_cad") && cadOverlays.isNotEmpty()) {
                        val cadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.parseColor("#0EA5E9")
                            style = Paint.Style.STROKE
                            strokeWidth = (2.2f / state.zoom).coerceAtLeast(0.8f)
                            alpha = (layerOpacity("layer_cad") * 255f).toInt().coerceIn(0, 255)
                        }
                        for (entity in cadOverlays) {
                            if (entity.points.size < 2) continue
                            val path = Path().apply {
                                moveTo(entity.points[0].x, entity.points[0].y)
                                for (i in 1 until entity.points.size) lineTo(entity.points[i].x, entity.points[i].y)
                                if (entity.closed) close()
                            }
                            nc.drawPath(path, cadPaint)
                        }
                    }

                    val topLeftWorld = MapTransformMath.screenToWorld(0f, 0f, state, worldCenterX, worldCenterY)
                    val botRightWorld = MapTransformMath.screenToWorld(viewW, viewH, state, worldCenterX, worldCenterY)
                    val visibleBox = BoundingBox(
                        minOf(topLeftWorld.x, botRightWorld.x) - 100f,
                        minOf(topLeftWorld.y, botRightWorld.y) - 100f,
                        maxOf(topLeftWorld.x, botRightWorld.x) + 100f,
                        maxOf(topLeftWorld.y, botRightWorld.y) + 100f
                    )

                    val showMeasureLabels = when (canvasSettings.effectiveZoomTextMode()) {
                        ZoomTextMode.ALWAYS -> true
                        ZoomTextMode.HIDDEN -> false
                        ZoomTextMode.AUTO -> state.zoom >= 0.45f
                    }

                    // 4. Plots & Roads
                    plots.forEachIndexed { pIdx, plot ->
                        if (plot.isHidden || plot.points.size < 3) return@forEachIndexed
                        val layerId = if (plot.isRoad) "layer_roads" else "layer_plots"
                        if (!layerVisible(layerId)) return@forEachIndexed

                        val box = GeometryUtils.boundingBox(plot.points)
                        if (!box.intersects(visibleBox)) return@forEachIndexed

                        val layerAlpha = layerOpacity(layerId)
                        val isActive = pIdx == activePlotIndex
                        val isSelected = plot.id in selectedPlotIds
                        val plotColor = parseColor(plot.colorHex, android.graphics.Color.parseColor("#2563EB"))

                        val path = Path().apply {
                            moveTo(plot.points[0].x, plot.points[0].y)
                            for (i in 1 until plot.points.size) lineTo(plot.points[i].x, plot.points[i].y)
                            close()
                        }

                        val fillAlpha = when {
                            plot.isRoad -> 0.35f
                            isSelected -> 0.24f
                            isActive -> 0.18f
                            else -> 0.11f
                        }
                        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = plotColor
                            style = Paint.Style.FILL
                            alpha = (fillAlpha * layerAlpha * 255f).toInt().coerceIn(0, 255)
                        }
                        nc.drawPath(path, fillPaint)

                        val strokeWorld = (baseStrokeWidthPx * canvasSettings.effectiveLineScale()) / state.zoom
                        val whiteOutline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.WHITE
                            style = Paint.Style.STROKE
                            strokeWidth = strokeWorld * (1f + canvasSettings.effectiveOutlineScale() * 1.6f)
                            strokeJoin = Paint.Join.ROUND
                            alpha = (0.9f * layerAlpha * 255f).toInt().coerceIn(0, 255)
                        }
                        nc.drawPath(path, whiteOutline)

                        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = if (plot.isHighlighted) android.graphics.Color.parseColor("#F59E0B") else plotColor
                            style = Paint.Style.STROKE
                            strokeWidth = if (isActive || isSelected) strokeWorld * 1.25f else strokeWorld
                            strokeJoin = Paint.Join.ROUND
                            alpha = (layerAlpha * 255f).toInt().coerceIn(0, 255)
                        }
                        nc.drawPath(path, strokePaint)

                        // Corner points
                        if (canvasSettings.showCornerPoints) {
                            val rWorld = (3f * canvasSettings.effectivePointScale() * density) / state.zoom
                            val fillDot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = android.graphics.Color.WHITE
                                style = Paint.Style.FILL
                            }
                            val strokeDot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = plotColor
                                style = Paint.Style.STROKE
                                strokeWidth = (1.5f * density) / state.zoom
                            }
                            plot.points.forEachIndexed { vIdx, pt ->
                                val isSelVertex = isActive && selectedVertexIndex == vIdx
                                val radius = if (isSelVertex) rWorld * 1.7f else rWorld
                                nc.drawCircle(pt.x, pt.y, radius, fillDot)
                                nc.drawCircle(pt.x, pt.y, radius, strokeDot)
                                if (isSelVertex) {
                                    nc.drawCircle(
                                        pt.x, pt.y, radius * 1.35f,
                                        Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                            color = android.graphics.Color.parseColor("#F59E0B")
                                            style = Paint.Style.STROKE
                                            strokeWidth = (1.8f * density) / state.zoom
                                        }
                                    )
                                }
                            }
                        }

                        // Edge measurements
                        if (showMeasureLabels && layerVisible("layer_measure")) {
                            val measureAlpha = layerOpacity("layer_measure")
                            val style = plot.labelStyle ?: PlotLabelStyle.DEFAULT
                            val mColor = style.colorHex?.let { parseColor(it, plotColor) }
                                ?: parseColor(canvasSettings.measurementColorHex, android.graphics.Color.parseColor("#133C2D"))
                            val textSizeWorld = ((baseLabelFontSizeSp * canvasSettings.effectiveFontScale() * style.effectiveFontScale()) * density) / state.zoom
                            val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                this.typeface = if (style.italic) Typeface.create(typeface, Typeface.ITALIC) else typeface
                                color = mColor
                                textSize = textSizeWorld
                                isFakeBoldText = style.bold
                                isUnderlineText = style.underline
                                textAlign = Paint.Align.CENTER
                                alpha = (style.effectiveOpacity() * measureAlpha * 255f).toInt().coerceIn(0, 255)
                            }
                            val dimHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                this.typeface = if (style.italic) Typeface.create(typeface, Typeface.ITALIC) else typeface
                                color = android.graphics.Color.WHITE
                                this.style = Paint.Style.STROKE
                                strokeWidth = (3.4f * density) / state.zoom
                                strokeJoin = Paint.Join.ROUND
                                textSize = textSizeWorld
                                isFakeBoldText = style.bold
                                textAlign = Paint.Align.CENTER
                                alpha = (0.94f * style.effectiveOpacity() * measureAlpha * 255f).toInt().coerceIn(0, 255)
                            }
                            val signed = GeometryUtils.signedAreaUnitsSq(plot.points)
                            val signMul = if (signed >= 0) 1f else -1f
                            val posSign = when {
                                !canvasSettings.showMeasurementsOutside -> -1f
                                canvasSettings.effectiveTextPosition() == MapTextPosition.BELOW -> -1f
                                canvasSettings.effectiveTextPosition() == MapTextPosition.INSIDE -> -0.45f
                                else -> 1f
                            }
                            for (i in plot.points.indices) {
                                val a = plot.points[i]
                                val b = plot.points[(i + 1) % plot.points.size]
                                val edgeFeet = GeometryUtils.edgeLengthUnits(a, b) * feetPerCanvasUnit
                                val label = if (unit == MeasurementUnit.FEET_INCHES && i < plot.segments.size && plot.segments[i].formattedLength.isNotBlank()) {
                                    plot.segments[i].formattedLength
                                } else {
                                    cachedFormat(edgeFeet)
                                }
                                val dx = b.x - a.x
                                val dy = b.y - a.y
                                val len = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(1f)
                                val nx = (dy / len) * signMul * posSign
                                val ny = (-dx / len) * signMul * posSign
                                val offsetWorld = (14f * canvasSettings.effectiveFontScale() * density) / state.zoom
                                val lx = (a.x + b.x) * 0.5f + nx * offsetWorld
                                val ly = (a.y + b.y) * 0.5f + ny * offsetWorld
                                nc.drawText(label, lx, ly, dimHaloPaint)
                                nc.drawText(label, lx, ly, dimPaint)
                            }
                        }

                        // Central plot label (suppressed on active plot during live road preview so badges don't clash)
                        val suppressCenter = (activeTool == "interactive_road" || useCustomRoadCenterline) && isActive && previewDividedPlots.isNotEmpty()
                        if (!suppressCenter && layerVisible("layer_labels") && !plot.isRoad) {
                            val style = plot.labelStyle ?: PlotLabelStyle.DEFAULT
                            val c = GeometryUtils.centroid(plot.points)
                            val lx = if (style.manuallyPositioned) c.x + style.effectiveOffsetX() * box.width else c.x
                            val ly = if (style.manuallyPositioned) c.y + style.effectiveOffsetY() * box.height else c.y
                            val labelSizeWorld = ((baseLabelFontSizeSp * 1.05f * canvasSettings.effectiveFontScale() * style.effectiveFontScale()) * density) / state.zoom
                            val labelAlpha = (style.effectiveOpacity() * layerOpacity("layer_labels") * 255f).toInt().coerceIn(0, 255)
                            val titleHalo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                this.typeface = if (style.italic) Typeface.create(typeface, Typeface.ITALIC) else typeface
                                color = android.graphics.Color.WHITE
                                this.style = Paint.Style.STROKE
                                strokeWidth = (3.8f * density) / state.zoom
                                strokeJoin = Paint.Join.ROUND
                                textSize = labelSizeWorld
                                isFakeBoldText = style.bold
                                textAlign = Paint.Align.CENTER
                                alpha = (0.92f * labelAlpha).toInt().coerceIn(0, 255)
                            }
                            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                this.typeface = if (style.italic) Typeface.create(typeface, Typeface.ITALIC) else typeface
                                color = style.colorHex?.let { parseColor(it, android.graphics.Color.BLACK) } ?: android.graphics.Color.parseColor("#111827")
                                textSize = labelSizeWorld
                                isFakeBoldText = style.bold
                                isUnderlineText = style.underline
                                textAlign = Paint.Align.CENTER
                                alpha = labelAlpha
                            }
                            val areaHalo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                this.typeface = typeface
                                color = android.graphics.Color.WHITE
                                this.style = Paint.Style.STROKE
                                strokeWidth = (3.8f * density) / state.zoom
                                strokeJoin = Paint.Join.ROUND
                                textSize = labelSizeWorld * 0.92f
                                isFakeBoldText = true
                                textAlign = Paint.Align.CENTER
                                alpha = (0.92f * labelAlpha).toInt().coerceIn(0, 255)
                            }
                            val areaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                this.typeface = typeface
                                color = plotColor
                                textSize = labelSizeWorld * 0.92f
                                isFakeBoldText = true
                                textAlign = Paint.Align.CENTER
                                alpha = labelAlpha
                            }
                            val dagPart = if (plot.dagNumber.isNotBlank()) " (দাগ ${plot.dagNumber})" else ""
                            val line1 = "${plot.name}$dagPart"
                            val liveShotok = plot.areaSqFt / sqFeetPerShotok.coerceAtLeast(1.0)
                            val line2 = "${BengaliFormatters.formatDecimal(liveShotok, 2)} শতক"
                            nc.save()
                            if (abs(style.effectiveRotationDeg()) > 0.5f) {
                                nc.rotate(style.effectiveRotationDeg(), lx, ly)
                            }
                            nc.drawText(line1, lx, ly - labelSizeWorld * 0.2f, titleHalo)
                            nc.drawText(line1, lx, ly - labelSizeWorld * 0.2f, titlePaint)
                            nc.drawText(line2, lx, ly + labelSizeWorld * 0.95f, areaHalo)
                            nc.drawText(line2, lx, ly + labelSizeWorld * 0.95f, areaPaint)
                            nc.restore()
                        }

                        // Annotations
                        if (layerVisible("layer_notes") && plot.annotationText.isNotBlank()) {
                            val c = GeometryUtils.centroid(plot.points)
                            val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                this.typeface = typeface
                                color = android.graphics.Color.parseColor("#7C3AED")
                                textSize = (10f * density) / state.zoom
                                textAlign = Paint.Align.CENTER
                            }
                            nc.drawText("✎ ${plot.annotationText}", c.x, c.y + (26f * density) / state.zoom, notePaint)
                        }
                    }

                    // Division preview overlay + colored pill badges (Mockup #16)
                    if (previewDividedPlots.isNotEmpty()) {
                        val landPreviews = previewDividedPlots.filter { !it.isRoad }
                        val totalPreviewShotok = landPreviews.sumOf { it.areaShotangsho }.coerceAtLeast(0.001)
                        val partPalette = listOf("#2563EB", "#16A34A", "#7C3AED", "#F97316", "#0D9488", "#DC2626")

                        val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.WHITE
                            style = Paint.Style.STROKE
                            strokeWidth = (2.6f * density) / state.zoom
                            pathEffect = DashPathEffect(floatArrayOf(10f / state.zoom, 7f / state.zoom), 0f)
                        }
                        val dashInner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.parseColor("#1E293B")
                            style = Paint.Style.STROKE
                            strokeWidth = (1.6f * density) / state.zoom
                            pathEffect = DashPathEffect(floatArrayOf(10f / state.zoom, 7f / state.zoom), 0f)
                        }

                        var landIdx = 0
                        for (dp in previewDividedPlots) {
                            if (dp.points.size < 3) continue
                            val path = Path().apply {
                                moveTo(dp.points[0].x, dp.points[0].y)
                                for (i in 1 until dp.points.size) lineTo(dp.points[i].x, dp.points[i].y)
                                close()
                            }
                            if (dp.isRoad) {
                                nc.drawPath(
                                    path,
                                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                        color = android.graphics.Color.parseColor("#64748B")
                                        style = Paint.Style.FILL
                                        alpha = 185
                                    }
                                )
                                nc.drawPath(path, dashInner)
                            } else {
                                val pHex = partPalette[landIdx % partPalette.size]
                                val pCol = parseColor(pHex, android.graphics.Color.parseColor("#2563EB"))
                                nc.drawPath(
                                    path,
                                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                        color = pCol
                                        style = Paint.Style.FILL
                                        alpha = 68
                                    }
                                )
                                nc.drawPath(path, dashPaint)
                                nc.drawPath(path, dashInner)

                                // Floating colored pill inside parcel
                                val c = GeometryUtils.centroid(dp.points)
                                val pct = (dp.areaShotangsho / totalPreviewShotok) * 100.0
                                val textSz = (10f * density) / state.zoom
                                val titleStr = "ভাগ ${BengaliFormatters.toBengaliDigits(landIdx + 1)}"
                                val subStr = "${BengaliFormatters.formatDecimal(dp.areaShotangsho, 2)} শতক • ${BengaliFormatters.formatDecimal(pct, 0)}%"
                                val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    color = pCol
                                    style = Paint.Style.FILL
                                    alpha = 235
                                }
                                val txtP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    this.typeface = typeface
                                    color = android.graphics.Color.WHITE
                                    textSize = textSz
                                    isFakeBoldText = true
                                    textAlign = Paint.Align.CENTER
                                }
                                val pillW = maxOf(txtP.measureText(titleStr), txtP.measureText(subStr)) + textSz * 1.4f
                                val pillH = textSz * 2.9f
                                val rect = RectF(c.x - pillW * 0.5f, c.y - pillH * 0.5f, c.x + pillW * 0.5f, c.y + pillH * 0.5f)
                                nc.drawRoundRect(rect, textSz * 0.55f, textSz * 0.55f, pillPaint)
                                nc.drawRoundRect(
                                    rect,
                                    textSz * 0.55f,
                                    textSz * 0.55f,
                                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                        color = android.graphics.Color.WHITE
                                        style = Paint.Style.STROKE
                                        strokeWidth = (1.2f * density) / state.zoom
                                    }
                                )
                                nc.drawText(titleStr, c.x, c.y - textSz * 0.15f, txtP)
                                nc.drawText(subStr, c.x, c.y + textSz * 0.95f, txtP)
                                landIdx++
                            }
                        }
                    }

                    // Interactive Polyline Road Corridor & Nodes (Mockup #16)
                    if ((activeTool == "interactive_road" || useCustomRoadCenterline) && manualSplitPoints.size >= 2) {
                        val widthUnits = (roadWidthFeet / feetPerCanvasUnit.coerceAtLeast(1e-4)).toFloat().coerceAtLeast(2f)
                        val roadPath = Path().apply {
                            moveTo(manualSplitPoints[0].x, manualSplitPoints[0].y)
                            for (i in 1 until manualSplitPoints.size) {
                                lineTo(manualSplitPoints[i].x, manualSplitPoints[i].y)
                            }
                        }
                        // Corridor outer white outline
                        nc.drawPath(
                            roadPath,
                            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = android.graphics.Color.WHITE
                                style = Paint.Style.STROKE
                                strokeWidth = widthUnits + (2.5f * density) / state.zoom
                                strokeCap = Paint.Cap.BUTT
                                strokeJoin = Paint.Join.ROUND
                            }
                        )
                        // Slate asphalt body
                        nc.drawPath(
                            roadPath,
                            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = android.graphics.Color.parseColor("#64748B")
                                style = Paint.Style.STROKE
                                strokeWidth = widthUnits
                                strokeCap = Paint.Cap.BUTT
                                strokeJoin = Paint.Join.ROUND
                                alpha = 215
                            }
                        )
                        // Centerline white stripe
                        nc.drawPath(
                            roadPath,
                            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = android.graphics.Color.WHITE
                                style = Paint.Style.STROKE
                                strokeWidth = (1.8f * density) / state.zoom
                            }
                        )

                        // White circular main vertices + orange midpoint bend handles
                        val nodeR = (8.5f * density) / state.zoom
                        val bendR = (5.5f * density) / state.zoom
                        for (i in 0 until manualSplitPoints.size - 1) {
                            val mx = (manualSplitPoints[i].x + manualSplitPoints[i + 1].x) * 0.5f
                            val my = (manualSplitPoints[i].y + manualSplitPoints[i + 1].y) * 0.5f
                            nc.drawCircle(
                                mx, my, bendR,
                                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    color = android.graphics.Color.parseColor("#F97316")
                                    style = Paint.Style.FILL
                                }
                            )
                            nc.drawCircle(
                                mx, my, bendR,
                                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    color = android.graphics.Color.WHITE
                                    style = Paint.Style.STROKE
                                    strokeWidth = (1.6f * density) / state.zoom
                                }
                            )
                        }
                        manualSplitPoints.forEach { pt ->
                            nc.drawCircle(
                                pt.x, pt.y, nodeR,
                                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    color = android.graphics.Color.WHITE
                                    style = Paint.Style.FILL
                                }
                            )
                            nc.drawCircle(
                                pt.x, pt.y, nodeR,
                                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    color = android.graphics.Color.parseColor("#0F172A")
                                    style = Paint.Style.STROKE
                                    strokeWidth = (2.4f * density) / state.zoom
                                }
                            )
                            nc.drawCircle(
                                pt.x, pt.y, nodeR * 0.35f,
                                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    color = android.graphics.Color.parseColor("#0F172A")
                                    style = Paint.Style.FILL
                                }
                            )
                        }

                        // Road Dimension Badge ("রাস্তা ৬' ০" × ৭৯' ৯"")
                        val lenFt = JtsDivisionEngine.polylineLengthFeet(manualSplitPoints, feetPerCanvasUnit)
                        val roadDimLabel = "রাস্তা ${cachedFormat(roadWidthFeet.toDouble())} × ${cachedFormat(lenFt)}"
                        val lastPt = manualSplitPoints.last()
                        val badgeSz = (9.5f * density) / state.zoom
                        val badgeTxtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            this.typeface = typeface
                            color = android.graphics.Color.WHITE
                            textSize = badgeSz
                            isFakeBoldText = true
                            textAlign = Paint.Align.CENTER
                        }
                        val bw = badgeTxtPaint.measureText(roadDimLabel) + badgeSz * 1.4f
                        val bh = badgeSz * 1.9f
                        val bx = lastPt.x
                        val by = lastPt.y + (18f * density) / state.zoom
                        nc.drawRoundRect(
                            RectF(bx - bw * 0.5f, by - bh * 0.5f, bx + bw * 0.5f, by + bh * 0.5f),
                            badgeSz * 0.45f,
                            badgeSz * 0.45f,
                            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.parseColor("#334155") }
                        )
                        nc.drawText(roadDimLabel, bx, by + badgeSz * 0.35f, badgeTxtPaint)
                    }

                    // 11. In-progress drawing polygon (Workflow 6: vivid red lines + white halos + dashed closing edge)
                    if (drawingPoints.isNotEmpty()) {
                        val drawHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.WHITE
                            style = Paint.Style.STROKE
                            strokeWidth = (4.8f * density) / state.zoom
                            strokeCap = Paint.Cap.ROUND
                            alpha = 235
                        }
                        val drawPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.parseColor("#DC2626")
                            style = Paint.Style.STROKE
                            strokeWidth = (2.6f * density) / state.zoom
                            strokeCap = Paint.Cap.ROUND
                        }
                        val fillPreviewPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.parseColor("#DC2626")
                            style = Paint.Style.FILL
                            alpha = 34
                        }
                        val closingDashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.parseColor("#DC2626")
                            style = Paint.Style.STROKE
                            strokeWidth = (2.0f * density) / state.zoom
                            pathEffect = DashPathEffect(floatArrayOf(9f / state.zoom, 7f / state.zoom), 0f)
                            alpha = 200
                        }
                        val dotFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.WHITE
                            style = Paint.Style.FILL
                        }
                        val dotRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.parseColor("#DC2626")
                            style = Paint.Style.STROKE
                            strokeWidth = (2.2f * density) / state.zoom
                        }
                        val dotInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.parseColor("#DC2626")
                            style = Paint.Style.FILL
                        }

                        if (drawingPoints.size >= 3) {
                            val polyPath = Path().apply {
                                moveTo(drawingPoints[0].x, drawingPoints[0].y)
                                for (k in 1 until drawingPoints.size) {
                                    lineTo(drawingPoints[k].x, drawingPoints[k].y)
                                }
                                close()
                            }
                            nc.drawPath(polyPath, fillPreviewPaint)
                            val first = drawingPoints.first()
                            val last = drawingPoints.last()
                            nc.drawLine(last.x, last.y, first.x, first.y, closingDashPaint)
                        }

                        val tHalo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            this.typeface = typeface
                            color = android.graphics.Color.WHITE
                            style = Paint.Style.STROKE
                            strokeWidth = (3.4f * density) / state.zoom
                            strokeJoin = Paint.Join.ROUND
                            textSize = (11.5f * density) / state.zoom
                            isFakeBoldText = true
                            textAlign = Paint.Align.CENTER
                        }
                        val tPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            this.typeface = typeface
                            color = android.graphics.Color.parseColor("#B91C1C")
                            textSize = (11.5f * density) / state.zoom
                            isFakeBoldText = true
                            textAlign = Paint.Align.CENTER
                        }

                        for (i in 1 until drawingPoints.size) {
                            val prev = drawingPoints[i - 1]
                            val p = drawingPoints[i]
                            nc.drawLine(prev.x, prev.y, p.x, p.y, drawHaloPaint)
                            nc.drawLine(prev.x, prev.y, p.x, p.y, drawPaint)
                            val edgeFt = GeometryUtils.edgeLengthUnits(prev, p) * feetPerCanvasUnit
                            val txt = cachedFormat(edgeFt)
                            val lx = (prev.x + p.x) * 0.5f
                            val ly = (prev.y + p.y) * 0.5f - (8f * density) / state.zoom
                            nc.drawText(txt, lx, ly, tHalo)
                            nc.drawText(txt, lx, ly, tPaint)
                        }

                        val rWorld = (5.5f * density) / state.zoom
                        drawingPoints.forEachIndexed { idx, p ->
                            if (idx == 0 && drawingPoints.size >= 3) {
                                // Emerald outer target ring showing 1st point can be tapped to close
                                nc.drawCircle(
                                    p.x, p.y, rWorld * 1.65f,
                                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                        color = android.graphics.Color.parseColor("#16A34A")
                                        style = Paint.Style.STROKE
                                        strokeWidth = (2.2f * density) / state.zoom
                                    }
                                )
                            }
                            nc.drawCircle(p.x, p.y, rWorld, dotFillPaint)
                            nc.drawCircle(p.x, p.y, rWorld, dotRingPaint)
                            if (idx == drawingPoints.lastIndex) {
                                nc.drawCircle(p.x, p.y, rWorld * 0.48f, dotInnerPaint)
                            }
                        }
                    }

                    // 12. Tape measure & calibration markers
                    if (tapeMeasurePoints.isNotEmpty()) {
                        val tapePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.parseColor("#DC2626")
                            style = Paint.Style.STROKE
                            strokeWidth = (2.5f * density) / state.zoom
                        }
                        val r = (5f * density) / state.zoom
                        tapeMeasurePoints.forEach { pt -> nc.drawCircle(pt.x, pt.y, r, tapePaint) }
                        if (tapeMeasurePoints.size == 2) {
                            val a = tapeMeasurePoints[0]
                            val b = tapeMeasurePoints[1]
                            nc.drawLine(a.x, a.y, b.x, b.y, tapePaint)
                            val ft = GeometryUtils.edgeLengthUnits(a, b) * feetPerCanvasUnit
                            val label = cachedFormat(ft)
                            nc.drawText(
                                label,
                                (a.x + b.x) * 0.5f,
                                (a.y + b.y) * 0.5f - (10f * density) / state.zoom,
                                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    this.typeface = typeface
                                    color = android.graphics.Color.parseColor("#DC2626")
                                    textSize = (12f * density) / state.zoom
                                    isFakeBoldText = true
                                    textAlign = Paint.Align.CENTER
                                }
                            )
                        }
                    }

                    if (calibrationPoints.isNotEmpty()) {
                        val calPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.parseColor("#F59E0B")
                            style = Paint.Style.FILL
                        }
                        val r = (6f * density) / state.zoom
                        calibrationPoints.forEach { pt -> nc.drawCircle(pt.x, pt.y, r, calPaint) }
                        if (calibrationPoints.size == 2) {
                            nc.drawLine(
                                calibrationPoints[0].x, calibrationPoints[0].y,
                                calibrationPoints[1].x, calibrationPoints[1].y,
                                Paint(calPaint).apply { style = Paint.Style.STROKE; strokeWidth = (2.5f * density) / state.zoom }
                            )
                        }
                    }

                    nc.restore()

                    // Crosshair (screen center)
                    if (canvasSettings.showCrosshair) {
                        val chPaint = Paint().apply {
                            color = android.graphics.Color.parseColor("#DC2626")
                            alpha = 140
                            strokeWidth = 1.2f * density
                        }
                        val cx = viewW * 0.5f
                        val cy = viewH * 0.5f
                        nc.drawLine(cx - 18f * density, cy, cx + 18f * density, cy, chPaint)
                        nc.drawLine(cx, cy - 18f * density, cx, cy + 18f * density, chPaint)
                    }

                    // 14. Precision Loupe Magnifier Overlay (Mockup #20)
                    val loupeWorld = activeTouchWorld
                    if (loupeWorld != null && (draggingVertex != null || draggingRoadNode != null || activeTool == "draw")) {
                        val loupeRadiusPx = 56f * density
                        val loupeCenterX = viewW * 0.5f
                        val loupeCenterY = 74f * density
                        val magZoom = (state.zoom * 2.6f).coerceIn(1.2f, 12f)

                        val clipCircle = Path().apply {
                            addCircle(loupeCenterX, loupeCenterY, loupeRadiusPx, Path.Direction.CW)
                        }
                        // Background fill
                        nc.drawCircle(
                            loupeCenterX, loupeCenterY, loupeRadiusPx,
                            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
                        )
                        nc.save()
                        nc.clipPath(clipCircle)
                        nc.translate(loupeCenterX - loupeWorld.x * magZoom, loupeCenterY - loupeWorld.y * magZoom)
                        nc.scale(magZoom, magZoom)
                        if (pdfBitmap != null && !pdfBitmap.isRecycled) {
                            nc.drawBitmap(pdfBitmap, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))
                        }
                        plots.forEach { pl ->
                            if (pl.points.size < 3 || pl.isHidden) return@forEach
                            val pth = Path().apply {
                                moveTo(pl.points[0].x, pl.points[0].y)
                                for (i in 1 until pl.points.size) lineTo(pl.points[i].x, pl.points[i].y)
                                close()
                            }
                            nc.drawPath(
                                pth,
                                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    color = parseColor(pl.colorHex, android.graphics.Color.parseColor("#2563EB"))
                                    style = Paint.Style.STROKE
                                    strokeWidth = 2f / magZoom
                                }
                            )
                        }
                        nc.restore()

                        // Crosshair + border ring on loupe
                        val crossP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.parseColor("#EF4444")
                            strokeWidth = 1.5f * density
                        }
                        nc.drawLine(loupeCenterX - 14f * density, loupeCenterY, loupeCenterX + 14f * density, loupeCenterY, crossP)
                        nc.drawLine(loupeCenterX, loupeCenterY - 14f * density, loupeCenterX, loupeCenterY + 14f * density, crossP)
                        nc.drawCircle(
                            loupeCenterX, loupeCenterY, loupeRadiusPx,
                            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = android.graphics.Color.parseColor("#2563EB")
                                style = Paint.Style.STROKE
                                strokeWidth = 3f * density
                            }
                        )
                    }
                }
            }

            // Zoom badge
            val zoomPct = MapTransformMath.scaleToZoomPercent(currentViewport.zoom)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "জুম ${BengaliFormatters.toBengaliDigits(zoomPct)}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Quick Red (X) Vertex Delete Floating Pill on Selected Vertex (Mockup #20)
            val selV = selectedVertexIndex
            val activePlot = plots.getOrNull(activePlotIndex)
            if (selV != null && activePlot != null && !activePlot.isLocked && selV in activePlot.points.indices && draggingVertex == null) {
                val pt = activePlot.points[selV]
                val scr = MapTransformMath.worldToScreen(pt.x, pt.y, currentViewport, worldCenterX, worldCenterY)
                val xOff = (scr.x - 16f * density).roundToInt().coerceIn(8, (viewW - 96f * density).roundToInt().coerceAtLeast(8))
                val yOff = (scr.y - 44f * density).roundToInt().coerceIn(8, (viewH - 48f * density).roundToInt().coerceAtLeast(8))
                Row(
                    modifier = Modifier
                        .offset { IntOffset(xOff, yOff) }
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color(0xFFDC2626))
                        .clickable(onClick = onDeleteSelectedVertex)
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Close, contentDescription = "শীর্ষবিন্দু মুছুন", tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("মুছুন", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            // Minimap overlay
            if (showMinimap) {
                MinimapOverlay(
                    plots = plots,
                    worldW = pdfBitmap?.width?.toFloat() ?: 1400f,
                    worldH = pdfBitmap?.height?.toFloat() ?: 1000f,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(110.dp, 82.dp)
                )
            }
        }
    }
}

@Composable
private fun MinimapOverlay(
    plots: List<LandPlot>,
    worldW: Float,
    worldH: Float,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
    ) {
        val sx = size.width / worldW.coerceAtLeast(1f)
        val sy = size.height / worldH.coerceAtLeast(1f)
        val scale = minOf(sx, sy)
        drawIntoCanvas { canvas ->
            val nc = canvas.nativeCanvas
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#2563EB")
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
            }
            for (plot in plots) {
                if (plot.points.size < 3 || plot.isHidden) continue
                val path = Path().apply {
                    moveTo(plot.points[0].x * scale, plot.points[0].y * scale)
                    for (i in 1 until plot.points.size) {
                        lineTo(plot.points[i].x * scale, plot.points[i].y * scale)
                    }
                    close()
                }
                nc.drawPath(path, p)
            }
        }
    }
}

private fun parseColor(hex: String?, fallback: Int): Int {
    if (hex.isNullOrBlank()) return fallback
    return runCatching { android.graphics.Color.parseColor(hex) }.getOrDefault(fallback)
}
