package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditRoad
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DivisionType
import com.example.data.model.LandPlot
import com.example.data.model.PointF
import com.example.domain.BengaliFormatters
import com.example.domain.GeometryUtils
import com.example.ui.theme.BhumiBlue
import com.example.ui.theme.BhumiEmerald
import com.example.ui.theme.BhumiEmeraldContainer
import com.example.ui.theme.BhumiOrangeCta
import com.example.ui.theme.BhumiOrangeSoft
import com.example.ui.viewmodel.DivisionUiState

@Composable
fun FloatingCanvasToolbar(
    canUndo: Boolean,
    canRedo: Boolean,
    showGrid: Boolean,
    showMinimap: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onFitScreen: () -> Unit,
    onToggleGrid: () -> Unit,
    onToggleMinimap: () -> Unit,
    onOpenLayers: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.testTag("btn_undo")) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "আনডু")
            }
            IconButton(onClick = onRedo, enabled = canRedo, modifier = Modifier.testTag("btn_redo")) {
                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "রিডু")
            }
            IconButton(onClick = onZoomIn) {
                Icon(Icons.Default.ZoomIn, contentDescription = "জুম ইন")
            }
            IconButton(onClick = onZoomOut) {
                Icon(Icons.Default.ZoomOut, contentDescription = "জুম আউট")
            }
            IconButton(onClick = onFitScreen) {
                Icon(Icons.Default.FitScreen, contentDescription = "ফিট স্ক্রিন")
            }
            IconButton(onClick = onToggleGrid) {
                Icon(
                    Icons.Default.GridOn,
                    contentDescription = "গ্রিড",
                    tint = if (showGrid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onToggleMinimap) {
                Icon(
                    Icons.Default.Map,
                    contentDescription = "মিনিম্যাপ",
                    tint = if (showMinimap) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onOpenLayers, modifier = Modifier.testTag("btn_layers")) {
                Icon(Icons.Default.Layers, contentDescription = "লেয়ারসমূহ")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorkspacePersistentBottomBar(
    plots: List<LandPlot>,
    activePlotIndex: Int,
    selectedPlotIds: Set<String>,
    sqFeetPerShotok: Double,
    activeTool: String,
    drawingPointCount: Int,
    onPlotPillClick: (String) -> Unit,
    onPlotContextAction: (LandPlot, String) -> Unit,
    onAddPlotOrFinishDraw: () -> Unit,
    onCancelDraw: () -> Unit,
    onSetTool: (String) -> Unit,
    onOpenDivision: () -> Unit,
    onOpenPrint: () -> Unit,
    onOpenExport: () -> Unit,
    onOpenMoreMenuAction: (String) -> Unit,
    scaleShortLabel: String = "১৬ ইঞ্চি",
    snapEnabled: Boolean = true,
    onToggleSnap: () -> Unit = {},
    onOpenScaleModal: () -> Unit = {},
    onOpenShotokModal: () -> Unit = {},
    onDeleteActiveOrVertex: () -> Unit = {},
    drawingPoints: List<PointF> = emptyList(),
    feetPerCanvasUnit: Double = 1.0,
    canUndo: Boolean = false,
    canRedo: Boolean = false,
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
    onZoomIn: () -> Unit = {},
    onZoomOut: () -> Unit = {},
    onTogglePanTool: () -> Unit = {},
    onOpenTextLineSize: () -> Unit = {},
    onToggleActivePlotLock: () -> Unit = {}
) {
    var moreExpanded by remember { mutableStateOf(false) }

    val activePlot = plots.getOrNull(activePlotIndex)
    val isActivePlotLocked = activePlot?.isLocked == true
    val totalShotok = plots.filter { !it.isRoad }.sumOf { it.areaSqFt } / sqFeetPerShotok.coerceAtLeast(1.0)
    val activeShotok = (activePlot?.areaSqFt ?: 0.0) / sqFeetPerShotok.coerceAtLeast(1.0)
    val activeSqFt = activePlot?.areaSqFt ?: 0.0
    val activePtCount = activePlot?.points?.size ?: 0

    val drawAreaSqFt = remember(drawingPoints, feetPerCanvasUnit) {
        if (drawingPoints.size >= 3) {
            GeometryUtils.areaUnitsSq(drawingPoints) * feetPerCanvasUnit * feetPerCanvasUnit
        } else 0.0
    }
    val drawShotok = drawAreaSqFt / sqFeetPerShotok.coerceAtLeast(1.0)

    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 6.dp, bottom = 8.dp)
        ) {
            // 1. Plot pills horizontal row (+ New Plot button at end matching Screenshots 9 & 10)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (plots.isEmpty()) {
                    Text(
                        text = "কোনো প্লট নেই — '+' ট্যাপ করে ম্যাপে প্লট আঁকুন",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                } else {
                    plots.forEachIndexed { idx, plot ->
                        var menuOpen by remember { mutableStateOf(false) }
                        val isActive = idx == activePlotIndex
                        val isSelected = plot.id in selectedPlotIds
                        val dotColor = runCatching { Color(android.graphics.Color.parseColor(plot.colorHex)) }
                            .getOrDefault(MaterialTheme.colorScheme.primary)
                        val liveShotok = plot.areaSqFt / sqFeetPerShotok.coerceAtLeast(1.0)

                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when {
                                            isActive -> BhumiOrangeSoft
                                            isSelected -> MaterialTheme.colorScheme.primaryContainer
                                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                        }
                                    )
                                    .border(
                                        width = if (isActive || isSelected) 1.5.dp else 1.dp,
                                        color = if (isActive) BhumiOrangeCta else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .combinedClickable(
                                        onClick = { onPlotPillClick(plot.id) },
                                        onLongClick = { menuOpen = true }
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "${plot.name} • ${BengaliFormatters.formatDecimal(liveShotok, 2)} শতক",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isActive || isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                if (plot.isLocked) {
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.Default.Lock, contentDescription = "লক", modifier = Modifier.size(12.dp))
                                }
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.MoreHoriz,
                                    contentDescription = "প্লট মেনু",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { menuOpen = true }
                                )
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("নাম/দাগ সম্পাদনা") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = { menuOpen = false; onPlotContextAction(plot, "metadata") }
                                )
                                DropdownMenuItem(
                                    text = { Text("ভাগ করুন / রাস্তা দিন") },
                                    leadingIcon = { Icon(Icons.Default.ContentCut, contentDescription = null) },
                                    onClick = { menuOpen = false; onPlotContextAction(plot, "divide") }
                                )
                                DropdownMenuItem(
                                    text = { Text("রেখা টেনে ভাগ করুন", fontWeight = FontWeight.SemiBold, color = BhumiOrangeCta) },
                                    leadingIcon = { Icon(Icons.Default.EditRoad, contentDescription = null, tint = BhumiOrangeCta) },
                                    onClick = { menuOpen = false; onPlotContextAction(plot, "interactive_road") }
                                )
                                DropdownMenuItem(
                                    text = { Text("কোণ থেকে কোণ মাপ") },
                                    leadingIcon = { Icon(Icons.Default.Straighten, contentDescription = null) },
                                    onClick = { menuOpen = false; onPlotContextAction(plot, "corner_measure") }
                                )
                                DropdownMenuItem(
                                    text = { Text("প্লট মুছুন", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = { menuOpen = false; onPlotContextAction(plot, "delete") }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(text = { Text("টীকা / নোট লিখুন") }, onClick = { menuOpen = false; onPlotContextAction(plot, "annotation") })
                                DropdownMenuItem(text = { Text(if (plot.isLocked) "আনলক করুন" else "লক করুন") }, onClick = { menuOpen = false; onPlotContextAction(plot, "lock") })
                                DropdownMenuItem(text = { Text("হাইলাইট টগল") }, onClick = { menuOpen = false; onPlotContextAction(plot, "highlight") })
                                DropdownMenuItem(text = { Text("রাস্তা হিসেবে চিহ্নিত") }, onClick = { menuOpen = false; onPlotContextAction(plot, "road") })
                                DropdownMenuItem(
                                    text = { Text(if (plot.isHidden) "ম্যাপে দেখান" else "ম্যাপে লুকান") },
                                    leadingIcon = { Icon(Icons.Default.VisibilityOff, contentDescription = null) },
                                    onClick = { menuOpen = false; onPlotContextAction(plot, "hide") }
                                )
                                DropdownMenuItem(text = { Text("ডুপ্লিকেট করুন") }, onClick = { menuOpen = false; onPlotContextAction(plot, "duplicate") })
                                DropdownMenuItem(text = { Text("জ্যামিতি কপি") }, onClick = { menuOpen = false; onPlotContextAction(plot, "copy_geom") })
                                DropdownMenuItem(text = { Text("লেবেল স্টাইল") }, onClick = { menuOpen = false; onPlotContextAction(plot, "label_style") })
                            }
                        }
                    }
                }
                // Quick Add Plot (+) Chip in Pill Bar
                FilledTonalIconButton(
                    onClick = {
                        if (activeTool == "draw") {
                            if (drawingPointCount >= 3) onAddPlotOrFinishDraw()
                        } else {
                            onAddPlotOrFinishDraw()
                        }
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("btn_add_plot_pill")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "নতুন প্লট", modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(4.dp))

            // Contextual Hint Strip (Screenshots 9 & 10)
            val hintText = when {
                activeTool == "draw" && drawingPointCount >= 3 ->
                    "১ম পয়েন্টে ট্যাপ করে বা 'সীমানা বন্ধ করুন' চেপে প্লট সম্পূর্ণ করুন"
                activeTool == "draw" ->
                    "ট্যাপে পয়েন্ট বসান (${BengaliFormatters.toBengaliDigits(drawingPointCount)}/৩) • ন্যূনতম ৩টি পয়েন্ট প্রয়োজন"
                activeTool == "measure" ->
                    "ম্যাপে ২টি বিন্দুতে ট্যাপ করে কোণ থেকে কোণ দূরত্ব মাপুন"
                activeTool == "pan" ->
                    "প্যান মোড চালু — ম্যাপ টেনে সরান ও জুম করুন"
                else ->
                    "পয়েন্টে ট্যাপে সিলেক্ট • প্লাস (+) চেপে নতুন প্লট আঁকুন"
            }
            Text(
                text = hintText,
                style = MaterialTheme.typography.labelSmall,
                color = if (activeTool == "draw") BhumiOrangeCta else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (activeTool == "draw") FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
            )

            // 2. 3-Card Compact Status Strip (Workflow 6 live draw metrics & closed plot metrics)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val leftPoints = if (activeTool == "draw") drawingPointCount else activePtCount
                // Card 1: Points & Scale
                Column(
                    modifier = Modifier
                        .weight(0.95f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        .clickable(onClick = onOpenScaleModal)
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "${BengaliFormatters.toBengaliDigits(leftPoints)} পয়েন্ট",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        scaleShortLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Card 2: Live Draw Status OR Green Active Plot Metrics
                if (activeTool == "draw") {
                    val canCloseDraw = drawingPointCount >= 3
                    Column(
                        modifier = Modifier
                            .weight(1.35f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (canCloseDraw) BhumiEmeraldContainer else BhumiOrangeSoft)
                            .border(
                                1.dp,
                                if (canCloseDraw) BhumiEmerald.copy(alpha = 0.5f) else BhumiOrangeCta.copy(alpha = 0.5f),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable(enabled = canCloseDraw, onClick = onAddPlotOrFinishDraw)
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            if (canCloseDraw) "${BengaliFormatters.formatDecimal(drawShotok, 2)} শতক" else "অঙ্কন চলছে",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (canCloseDraw) BhumiEmerald else BhumiOrangeCta,
                            maxLines = 1
                        )
                        Text(
                            if (canCloseDraw) "বন্ধ করুন • ${BengaliFormatters.formatDecimal(drawAreaSqFt, 0)} ফু²"
                            else "${BengaliFormatters.toBengaliDigits(drawingPointCount)}/৩ পয়েন্ট",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (canCloseDraw) BhumiEmerald else BhumiOrangeCta,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1.35f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BhumiEmeraldContainer)
                            .border(1.dp, BhumiEmerald.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "${BengaliFormatters.formatDecimal(activeShotok, 2)} শতক",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = BhumiEmerald,
                            maxLines = 1
                        )
                        Text(
                            "${BengaliFormatters.formatDecimal(activeSqFt, 1)} বর্গফুট • সক্রিয়",
                            style = MaterialTheme.typography.labelSmall,
                            color = BhumiEmerald,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Card 3: Total / Subtotal Project & Shotok Standard
                val displayTotalShotok = if (activeTool == "draw") totalShotok + drawShotok else totalShotok
                val rightSubPrefix = if (activeTool == "draw") "উপমোট" else "মোট"
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        .clickable(onClick = onOpenShotokModal)
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "${BengaliFormatters.formatDecimal(displayTotalShotok, 2)} শতক",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        "$rightSubPrefix (${BengaliFormatters.formatDecimal(sqFeetPerShotok, 1)} ফু²)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // 3. Draw Mode ("সীমানা বন্ধ করুন") OR Scrollable Workflow 6 Action Bar
            if (activeTool == "draw") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(onClick = onCancelDraw) {
                        Icon(Icons.Default.Close, contentDescription = "বাতিল", tint = MaterialTheme.colorScheme.error)
                    }
                    Button(
                        onClick = onAddPlotOrFinishDraw,
                        enabled = drawingPointCount >= 3,
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (drawingPointCount >= 3) BhumiEmerald else BhumiOrangeCta,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_add_or_finish_plot")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (drawingPointCount >= 3) {
                                "সীমানা বন্ধ করুন (${BengaliFormatters.toBengaliDigits(drawingPointCount)} পয়েন্ট)"
                            } else {
                                "কমপক্ষে ৩ পয়েন্ট (${BengaliFormatters.toBengaliDigits(drawingPointCount)}/৩)"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick = onAddPlotOrFinishDraw,
                        modifier = Modifier.testTag("btn_add_or_finish_plot")
                    ) {
                        Icon(Icons.Default.AddLocationAlt, contentDescription = "নতুন প্লট আঁকুন")
                    }

                    IconButton(onClick = onZoomOut) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "জুম আউট")
                    }

                    IconButton(onClick = onZoomIn) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "জুম ইন")
                    }

                    IconButton(onClick = onTogglePanTool) {
                        Icon(
                            Icons.Default.PanTool,
                            contentDescription = "প্যান মোড",
                            tint = if (activeTool == "pan") BhumiOrangeCta else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onOpenTextLineSize) {
                        Text(
                            "Aa",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = onUndo, enabled = canUndo) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "আনডু")
                    }

                    IconButton(onClick = onRedo, enabled = canRedo) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "রিডু")
                    }

                    IconButton(onClick = onDeleteActiveOrVertex) {
                        Icon(Icons.Default.Delete, contentDescription = "মুছুন", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    IconButton(onClick = onToggleActivePlotLock) {
                        Icon(
                            if (isActivePlotLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = if (isActivePlotLocked) "আনলক করুন" else "লক করুন",
                            tint = if (isActivePlotLocked) BhumiOrangeCta else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onToggleSnap) {
                        Icon(
                            Icons.Default.GpsFixed,
                            contentDescription = "স্ন্যাপ",
                            tint = if (snapEnabled) BhumiOrangeCta else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { onSetTool(if (activeTool == "measure") "select" else "measure") }) {
                        Icon(
                            Icons.Default.Straighten,
                            contentDescription = "টেপ পরিমাপ",
                            tint = if (activeTool == "measure") BhumiOrangeCta else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onOpenDivision, modifier = Modifier.testTag("btn_division")) {
                        Icon(
                            Icons.Default.ContentCut,
                            contentDescription = "জমি বিভাজন",
                            tint = if (activeTool == "interactive_road") BhumiOrangeCta else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onOpenPrint, modifier = Modifier.testTag("btn_print_preview")) {
                        BadgedBox(
                            badge = {
                                if (selectedPlotIds.isNotEmpty()) {
                                    Badge { Text(BengaliFormatters.toBengaliDigits(selectedPlotIds.size)) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Print, contentDescription = "প্রিন্ট প্রিভিউ")
                        }
                    }

                    IconButton(onClick = onOpenExport, modifier = Modifier.testTag("btn_export_center")) {
                        Icon(Icons.Default.Share, contentDescription = "রপ্তানি ও শেয়ার")
                    }

                    Box {
                        IconButton(onClick = { moreExpanded = true }, modifier = Modifier.testTag("btn_more_menu")) {
                            Icon(Icons.Default.MoreVert, contentDescription = "আরও অপশন")
                        }
                        DropdownMenu(expanded = moreExpanded, onDismissRequest = { moreExpanded = false }) {
                            DropdownMenuItem(text = { Text("রেখা টেনে রাস্তা বসান") }, onClick = { moreExpanded = false; onOpenMoreMenuAction("interactive_road") })
                            DropdownMenuItem(text = { Text("মৌজার স্কেল ও ক্যালিব্রেশন") }, onClick = { moreExpanded = false; onOpenMoreMenuAction("scale") })
                            DropdownMenuItem(text = { Text("ম্যাপ সোর্স নির্বাচন (ছবি/PDF/DXF)") }, onClick = { moreExpanded = false; onOpenMoreMenuAction("source") })
                            DropdownMenuItem(text = { Text("১ শতক মানদণ্ড (৪৩৫.৬)") }, onClick = { moreExpanded = false; onOpenMoreMenuAction("shotok") })
                            DropdownMenuItem(text = { Text("Aa লেখা ও লাইন সাইজ") }, onClick = { moreExpanded = false; onOpenMoreMenuAction("text_line") })
                            DropdownMenuItem(text = { Text("ক্যানভাস ও লেখার স্টাইল (CAD)") }, onClick = { moreExpanded = false; onOpenMoreMenuAction("cad_settings") })
                            DropdownMenuItem(text = { Text("বহু-প্লট নির্বাচন (৮ মোড)") }, onClick = { moreExpanded = false; onOpenMoreMenuAction("selection") })
                            DropdownMenuItem(text = { Text("প্রজেক্ট তথ্য ও মৌজা মেটাডেটা") }, onClick = { moreExpanded = false; onOpenMoreMenuAction("metadata") })
                            DropdownMenuItem(text = { Text("সংরক্ষিত স্ন্যাপশটসমূহ") }, onClick = { moreExpanded = false; onOpenMoreMenuAction("snapshots") })
                            DropdownMenuItem(text = { Text("পরবর্তী প্লটের সাথে মার্জ করুন") }, onClick = { moreExpanded = false; onOpenMoreMenuAction("merge") })
                            DropdownMenuItem(text = { Text("নতুন কুইক প্লট যোগ করুন") }, onClick = { moreExpanded = false; onOpenMoreMenuAction("quick_plot") })
                            DropdownMenuItem(text = { Text("কপি করা জ্যামিতি পেস্ট") }, onClick = { moreExpanded = false; onOpenMoreMenuAction("paste_geom") })
                            DropdownMenuItem(text = { Text("জিওরেফারেন্স নিয়ন্ত্রণ") }, onClick = { moreExpanded = false; onOpenMoreMenuAction("georef") })
                            DropdownMenuItem(text = { Text("প্রজেক্ট সেভ করুন") }, onClick = { moreExpanded = false; onOpenMoreMenuAction("save") })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveRoadControlPanel(
    state: DivisionUiState,
    onSetEqualShares: () -> Unit,
    onAdjustParts: (Int) -> Unit,
    onAdjustRoadWidth: (Float) -> Unit,
    onResetRoad: () -> Unit,
    onCommitDivision: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val partPalette = listOf(BhumiBlue, BhumiEmerald, Color(0xFF7C3AED), BhumiOrangeCta)
    val reportParts = state.divisionReport?.parts.orEmpty()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        tonalElevation = 8.dp,
        shadowElevation = 10.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Title + Orange Commit CTA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BhumiOrangeSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.EditRoad, contentDescription = null, tint = BhumiOrangeCta)
                    }
                    Column {
                        Text("বিন্দু টেনে রাস্তা বসান", fontWeight = FontWeight.Bold)
                        Text(
                            "রাস্তার ছোট বিন্দু টানলে নতুন বাঁক হয়",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Button(
                    onClick = onCommitDivision,
                    colors = ButtonDefaults.buttonColors(containerColor = BhumiOrangeCta),
                    modifier = Modifier.testTag("btn_interactive_road_commit")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("ভাগ করুন", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            // Horizontal Scrollable Colored Part Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val count = state.divisionPartsCount
                for (idx in 0 until count) {
                    val accent = partPalette[idx % partPalette.size]
                    val part = reportParts.getOrNull(idx)
                    val pctText = if (part != null) {
                        BengaliFormatters.formatDecimal(part.percentage, 0)
                    } else {
                        BengaliFormatters.formatDecimal(100.0 / count, 0)
                    }
                    val shotokText = if (part != null) {
                        BengaliFormatters.formatDecimal(part.areaShotok, 2)
                    } else "—"
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(accent.copy(alpha = 0.08f))
                            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(26.dp)
                                .background(accent, RoundedCornerShape(2.dp))
                        )
                        Column {
                            Text(
                                "ভাগ ${BengaliFormatters.toBengaliDigits(idx + 1)} $pctText%",
                                style = MaterialTheme.typography.labelSmall,
                                color = accent,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "$shotokText শতক",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Equal Shares Toggle Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(onClick = onSetEqualShares)
                        .padding(vertical = 4.dp, horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (state.divisionType == DivisionType.EQUAL) BhumiEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "সব ভাগ সমান",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (state.divisionType == DivisionType.EQUAL) BhumiEmerald else MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    "রাস্তা: ${BengaliFormatters.formatDecimal(state.roadWidthFeet.toDouble(), 0)} ফুট",
                    style = MaterialTheme.typography.labelLarge,
                    color = BhumiOrangeCta,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider()

            // Controls Bar: - 4 ভাগ + | Road Width -/+ | Reset | Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onAdjustParts(-1) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = "ভাগ কমান", modifier = Modifier.size(16.dp))
                    }
                    Text(
                        "${BengaliFormatters.toBengaliDigits(state.divisionPartsCount)} ভাগ",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    IconButton(onClick = { onAdjustParts(1) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "ভাগ বাড়ান", modifier = Modifier.size(16.dp))
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = "রাস্তার প্রস্থ", tint = BhumiOrangeCta, modifier = Modifier.size(18.dp))
                    IconButton(onClick = { onAdjustRoadWidth(-1f) }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = "প্রস্থ কমান", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { onAdjustRoadWidth(1f) }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "প্রস্থ বাড়ান", modifier = Modifier.size(16.dp))
                    }
                }

                IconButton(onClick = onResetRoad, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "রিসেট")
                }

                IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
