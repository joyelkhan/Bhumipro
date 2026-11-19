package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditRoad
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.LandPlot
import com.example.data.model.LengthFormatter
import com.example.data.model.WorkspaceMode
import com.example.data.model.mouzaScalePresets
import com.example.domain.BengaliFormatters
import com.example.domain.GeometryUtils
import com.example.ui.components.AnnotationDialog
import com.example.ui.components.CalibrationDistanceDialog
import com.example.ui.components.CanvasSettingsSheet
import com.example.ui.components.ChooseSourceModal
import com.example.ui.components.DivisionModal
import com.example.ui.components.FloatingCanvasToolbar
import com.example.ui.components.GeorefOverlayPanel
import com.example.ui.components.InteractiveMapCanvas
import com.example.ui.components.InteractiveRoadControlPanel
import com.example.ui.components.LayersBottomSheet
import com.example.ui.components.MultiPlotSelectionSheet
import com.example.ui.components.PlotLabelStyleDialog
import com.example.ui.components.PlotMetadataDialog
import com.example.ui.components.PostDivisionBottomSheet
import com.example.ui.components.ProjectMetadataDialog
import com.example.ui.components.RenameDialog
import com.example.ui.components.SaveSurveyDialog
import com.example.ui.components.ScaleCalibrationModal
import com.example.ui.components.ShotokStandardModal
import com.example.ui.components.SnapshotsSheet
import com.example.ui.components.TextLineSizeModal
import com.example.ui.components.WorkspacePersistentBottomBar
import com.example.ui.theme.BhumiEmerald
import com.example.ui.theme.BhumiEmeraldContainer
import com.example.ui.theme.BhumiOrangeCta
import com.example.ui.theme.BhumiOrangeSoft
import com.example.ui.viewmodel.LandSurveyViewModel
import kotlin.math.hypot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapWorkspaceScreen(
    projectId: String,
    mode: WorkspaceMode,
    viewModel: LandSurveyViewModel,
    onBack: () -> Unit,
    onOpenPrintPreview: () -> Unit,
    onOpenExportCenter: () -> Unit
) {
    LaunchedEffect(projectId, mode) {
        viewModel.initWorkspace(projectId, mode)
    }

    val project by viewModel.uiState.collectAsStateWithLifecycle()
    val pdfBitmap by viewModel.pdfBitmap.collectAsStateWithLifecycle()
    val isPdfSource by viewModel.isPdfSource.collectAsStateWithLifecycle()
    val activeTool by viewModel.activeTool.collectAsStateWithLifecycle()
    val selectedVertexIndex by viewModel.selectedVertexIndex.collectAsStateWithLifecycle()
    val drawingPoints by viewModel.drawingPoints.collectAsStateWithLifecycle()
    val tapePoints by viewModel.tapeMeasurePoints.collectAsStateWithLifecycle()
    val cadOverlays by viewModel.cadOverlays.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()
    val showMinimap by viewModel.showMinimap.collectAsStateWithLifecycle()
    val snapEnabled by viewModel.snapEnabled.collectAsStateWithLifecycle()
    val rotationDeg by viewModel.canvasRotationDeg.collectAsStateWithLifecycle()
    val viewportCommand by viewModel.viewportCommand.collectAsStateWithLifecycle()
    val isCalibrating by viewModel.isCalibrating.collectAsStateWithLifecycle()
    val calibrationPoints by viewModel.calibrationPoints.collectAsStateWithLifecycle()
    val showCalDistanceDialog by viewModel.showCalibrationDistanceDialog.collectAsStateWithLifecycle()

    val showDivisionModal by viewModel.showDivisionModal.collectAsStateWithLifecycle()
    val divisionState by viewModel.divisionState.collectAsStateWithLifecycle()
    val showScaleModal by viewModel.showScaleModal.collectAsStateWithLifecycle()
    val showSourceModal by viewModel.showChooseSourceModal.collectAsStateWithLifecycle()
    val showShotokModal by viewModel.showShotokStandardModal.collectAsStateWithLifecycle()
    val showTextLineModal by viewModel.showTextLineSizeModal.collectAsStateWithLifecycle()
    val showMetadataDialog by viewModel.showProjectMetadataDialog.collectAsStateWithLifecycle()
    val showRenameDialog by viewModel.showRenameDialog.collectAsStateWithLifecycle()
    val annotationPlotId by viewModel.showAnnotationDialog.collectAsStateWithLifecycle()
    val showLayersSheet by viewModel.showLayersSheet.collectAsStateWithLifecycle()
    val showSnapshotsSheet by viewModel.showSnapshotsSheet.collectAsStateWithLifecycle()
    val snapshots by viewModel.observeProjectSnapshots().collectAsStateWithLifecycle(initialValue = emptyList())
    val statusMsg by viewModel.statusMessage.collectAsStateWithLifecycle()

    val georefOpacity by viewModel.georefOpacity.collectAsStateWithLifecycle()
    val controlPoints by viewModel.controlPoints.collectAsStateWithLifecycle()
    val georefQuality by viewModel.georefQuality.collectAsStateWithLifecycle()

    var showCadSheet by remember { mutableStateOf(false) }
    var showSelectionSheet by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showGeorefSheet by remember { mutableStateOf(false) }
    var showPostDivisionSheet by remember { mutableStateOf(false) }
    var topMoreExpanded by remember { mutableStateOf(false) }
    var editingPlotMetadata by remember { mutableStateOf<LandPlot?>(null) }
    var editingLabelStylePlot by remember { mutableStateOf<LandPlot?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusMsg?.id) {
        val m = statusMsg ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(m.text)
        viewModel.clearStatusMessage()
    }

    BackHandler(enabled = activeTool == "interactive_road" || activeTool == "draw" || activeTool == "pan" || activeTool == "measure") {
        when (activeTool) {
            "interactive_road" -> viewModel.exitInteractiveRoadMode()
            "draw" -> viewModel.cancelDrawingPolygon()
            else -> viewModel.setActiveTool("select")
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.importDocumentOrImageUri(uri.toString())
    }
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importDocumentOrImageUri(uri.toString())
    }
    val dxfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importCadUri(uri.toString())
    }

    val gridVisible = project.layers.firstOrNull { it.id == "layer_grid" }?.visible ?: false
    val scaleShortLabel = mouzaScalePresets.firstOrNull { it.id == project.scaleId }?.shortLabelBn ?: project.scale

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(tonalElevation = 3.dp, shadowElevation = 2.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.openRenameDialog() }
                        ) {
                            Text(
                                text = project.mouzaName.ifBlank { project.title },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${project.scale} • মোট ${BengaliFormatters.formatDecimal(project.totalLandAreaShotangsho, 2)} শতক",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { showSaveDialog = true }, modifier = Modifier.testTag("btn_save_project")) {
                            Icon(Icons.Default.Save, contentDescription = "সেভ করুন", tint = BhumiOrangeCta)
                        }
                        IconButton(onClick = { showSelectionSheet = true }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "প্লট নির্বাচন ও পরিদর্শক")
                        }
                        TextButton(
                            onClick = { viewModel.openTextLineSizeModal() },
                            modifier = Modifier.testTag("btn_top_text_line")
                        ) {
                            Text("Aa", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Box {
                            IconButton(onClick = { topMoreExpanded = true }, modifier = Modifier.testTag("btn_top_overflow")) {
                                Icon(Icons.Default.MoreVert, contentDescription = "ওয়ার্কস্পেস মেনু")
                            }
                            DropdownMenu(expanded = topMoreExpanded, onDismissRequest = { topMoreExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text("ক্যালিব্রেট (২-পয়েন্ট)") },
                                    leadingIcon = { Icon(Icons.Default.Straighten, contentDescription = null) },
                                    onClick = { topMoreExpanded = false; viewModel.startCalibrationMode() }
                                )
                                DropdownMenuItem(
                                    text = { Text("নাম পরিবর্তন") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = { topMoreExpanded = false; viewModel.openRenameDialog() }
                                )
                                DropdownMenuItem(
                                    text = { Text("সংরক্ষণ করুন") },
                                    leadingIcon = { Icon(Icons.Default.Save, contentDescription = null) },
                                    onClick = { topMoreExpanded = false; showSaveDialog = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("A4 প্রিন্ট") },
                                    leadingIcon = { Icon(Icons.Default.Print, contentDescription = null) },
                                    onClick = { topMoreExpanded = false; onOpenPrintPreview() }
                                )
                                DropdownMenuItem(
                                    text = { Text("স্কেল সেটিংস") },
                                    leadingIcon = { Icon(Icons.Default.FitScreen, contentDescription = null) },
                                    onClick = { topMoreExpanded = false; viewModel.openScaleModal() }
                                )
                                DropdownMenuItem(
                                    text = { Text("Aa লেখা ও লাইন সাইজ") },
                                    onClick = { topMoreExpanded = false; viewModel.openTextLineSizeModal() }
                                )
                                DropdownMenuItem(
                                    text = { Text("১ শতক সমান কত বর্গফুট (মান)") },
                                    leadingIcon = { Icon(Icons.Default.CropFree, contentDescription = null) },
                                    onClick = { topMoreExpanded = false; viewModel.openShotokStandardModal() }
                                )
                                DropdownMenuItem(
                                    text = { Text("অন্য ফাইল নিন") },
                                    leadingIcon = { Icon(Icons.Default.Map, contentDescription = null) },
                                    onClick = { topMoreExpanded = false; viewModel.openChooseSourceModal() }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("স্ন্যাপশট ও ইতিহাস") },
                                    leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                                    onClick = { topMoreExpanded = false; viewModel.openSnapshotsSheet() }
                                )
                                DropdownMenuItem(
                                    text = { Text("লেয়ার ও জিওরেফারেন্স") },
                                    leadingIcon = { Icon(Icons.Default.Layers, contentDescription = null) },
                                    onClick = { topMoreExpanded = false; viewModel.openLayersSheet() }
                                )
                            }
                        }
                    }

                    // Zoom preset strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(25, 50, 100, 150, 200, 400).forEach { pct ->
                            FilterChip(
                                selected = project.canvasSettings.zoomPresetPercent == pct,
                                onClick = { viewModel.requestZoomPercent(pct) },
                                label = { Text("${BengaliFormatters.toBengaliDigits(pct)}%") }
                            )
                        }
                        FilterChip(
                            selected = false,
                            onClick = { viewModel.openShotokStandardModal() },
                            label = { Text("১ শতক = ${BengaliFormatters.formatDecimal(project.sqFeetPerShotok, 1)}") }
                        )
                    }
                }
            }
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (activeTool == "interactive_road") {
                    InteractiveRoadControlPanel(
                        state = divisionState,
                        onSetEqualShares = { viewModel.setEqualSharesForAllParts() },
                        onAdjustParts = { viewModel.adjustDivisionPartsDelta(it) },
                        onAdjustRoadWidth = { viewModel.adjustRoadWidthDelta(it) },
                        onResetRoad = { viewModel.resetRoadToPreset() },
                        onCommitDivision = {
                            viewModel.commitDivision()
                            if (divisionState.divisionReport != null) {
                                showPostDivisionSheet = true
                            }
                        },
                        onClose = { viewModel.exitInteractiveRoadMode() }
                    )
                }

                WorkspacePersistentBottomBar(
                    plots = project.plots,
                    activePlotIndex = project.activePlotIndex,
                    selectedPlotIds = project.selectedPlotIds,
                    sqFeetPerShotok = project.sqFeetPerShotok,
                    activeTool = activeTool,
                    drawingPointCount = drawingPoints.size,
                    scaleShortLabel = scaleShortLabel,
                    snapEnabled = snapEnabled,
                    onToggleSnap = { viewModel.toggleSnap() },
                    onOpenScaleModal = { viewModel.openScaleModal() },
                    onOpenShotokModal = { viewModel.openShotokStandardModal() },
                    onDeleteActiveOrVertex = { viewModel.deleteActivePlotOrVertex() },
                    drawingPoints = drawingPoints,
                    feetPerCanvasUnit = project.feetPerCanvasUnit,
                    canUndo = canUndo,
                    canRedo = canRedo,
                    onUndo = { viewModel.undo() },
                    onRedo = { viewModel.redo() },
                    onZoomIn = { viewModel.requestZoomStep(true) },
                    onZoomOut = { viewModel.requestZoomStep(false) },
                    onTogglePanTool = { viewModel.setActiveTool(if (activeTool == "pan") "select" else "pan") },
                    onOpenTextLineSize = { viewModel.openTextLineSizeModal() },
                    onToggleActivePlotLock = {
                        project.activePlot?.let { viewModel.togglePlotLocked(it.id) }
                    },
                    onPlotPillClick = { plotId -> viewModel.togglePlotSelectionAndActivate(plotId) },
                    onPlotContextAction = { plot, action ->
                        viewModel.selectPlotById(plot.id)
                        when (action) {
                            "interactive_road" -> viewModel.enterInteractiveRoadMode()
                            "divide" -> viewModel.openDivisionModal()
                            "corner_measure" -> viewModel.setActiveTool(if (activeTool == "measure") "select" else "measure")
                            "metadata" -> editingPlotMetadata = plot
                            "annotation" -> viewModel.openAnnotationDialog(plot.id)
                            "lock" -> viewModel.togglePlotLocked(plot.id)
                            "highlight" -> viewModel.togglePlotHighlight(plot.id)
                            "road" -> viewModel.togglePlotAsRoad(plot.id)
                            "hide" -> viewModel.togglePlotHidden(plot.id)
                            "duplicate" -> viewModel.duplicateActivePlot()
                            "copy_geom" -> viewModel.copyActivePlotGeometry()
                            "label_style" -> editingLabelStylePlot = plot
                            "delete" -> viewModel.deleteActivePlotOrVertex()
                        }
                    },
                    onAddPlotOrFinishDraw = {
                        if (activeTool == "draw") {
                            if (drawingPoints.size >= 3) viewModel.closeDrawingPolygon()
                        } else {
                            viewModel.startDrawingNewPlot()
                        }
                    },
                    onCancelDraw = { viewModel.cancelDrawingPolygon() },
                    onSetTool = { tool -> viewModel.setActiveTool(tool) },
                    onOpenDivision = {
                        if (project.activePlot != null) {
                            viewModel.openDivisionModal()
                        }
                    },
                    onOpenPrint = onOpenPrintPreview,
                    onOpenExport = onOpenExportCenter,
                    onOpenMoreMenuAction = { action ->
                        when (action) {
                            "interactive_road" -> viewModel.enterInteractiveRoadMode()
                            "scale" -> viewModel.openScaleModal()
                            "source" -> viewModel.openChooseSourceModal()
                            "shotok" -> viewModel.openShotokStandardModal()
                            "text_line" -> viewModel.openTextLineSizeModal()
                            "cad_settings" -> showCadSheet = true
                            "selection" -> showSelectionSheet = true
                            "metadata" -> viewModel.openProjectMetadataDialog()
                            "snapshots" -> viewModel.openSnapshotsSheet()
                            "merge" -> viewModel.mergeActiveWithNextPlot()
                            "quick_plot" -> viewModel.addNewPlot()
                            "paste_geom" -> viewModel.pasteCopiedPlotGeometry()
                            "georef" -> showGeorefSheet = true
                            "save" -> viewModel.saveProjectToRoom()
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWideTablet = maxWidth >= 720.dp

            Row(modifier = Modifier.fillMaxSize()) {
                // Canvas center pane
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    InteractiveMapCanvas(
                        pdfBitmap = pdfBitmap,
                        plots = project.plots,
                        previewDividedPlots = divisionState.previewDividedPlots,
                        activePlotIndex = project.activePlotIndex,
                        selectedPlotIds = project.selectedPlotIds,
                        selectedVertexIndex = selectedVertexIndex,
                        activeTool = activeTool,
                        isCalibrating = isCalibrating,
                        calibrationPoints = calibrationPoints,
                        drawingPoints = drawingPoints,
                        tapeMeasurePoints = tapePoints,
                        cadOverlays = cadOverlays,
                        layers = project.layers,
                        canvasSettings = project.canvasSettings,
                        feetPerCanvasUnit = project.feetPerCanvasUnit,
                        sqFeetPerShotok = project.sqFeetPerShotok,
                        baseStrokeWidthPx = project.strokeWidthPx,
                        baseLabelFontSizeSp = project.labelFontSizeSp,
                        rotationDeg = rotationDeg,
                        showMinimap = showMinimap,
                        viewportCommand = viewportCommand,
                        manualSplitPoints = divisionState.manualSplitPoints,
                        roadWidthFeet = divisionState.roadWidthFeet,
                        useCustomRoadCenterline = divisionState.useCustomRoadCenterline,
                        onTapSelectOrDraw = { worldPt, spatial ->
                            if (activeTool == "draw") {
                                viewModel.addDrawingPoint(worldPt)
                            } else {
                                val hit = spatial.hitTest(worldPt, 16f)
                                if (hit != null) {
                                    viewModel.togglePlotSelectionAndActivate(hit.id)
                                }
                            }
                        },
                        onVertexSelected = { idx -> viewModel.selectVertex(idx) },
                        onVertexDragStart = { idx -> viewModel.beginVertexDrag(idx) },
                        onVertexDragMove = { pt -> viewModel.moveSelectedVertex(pt) },
                        onTapeTap = { pt -> viewModel.addTapeMeasurePoint(pt) },
                        onCalibrationTap = { pt -> viewModel.addCalibrationTapPoint(pt) },
                        onLongPressPlot = { plot ->
                            viewModel.selectPlotById(plot.id)
                            editingPlotMetadata = plot
                        },
                        onRoadNodeDragMove = { idx, pt -> viewModel.moveRoadNode(idx, pt) },
                        onRoadBendInsert = { idx, pt -> viewModel.insertRoadBend(idx, pt) },
                        onDeleteSelectedVertex = { viewModel.deleteSelectedVertex() },
                        onCloseDrawingPolygon = { viewModel.closeDrawingPolygon() }
                    )

                    FloatingCanvasToolbar(
                        canUndo = canUndo,
                        canRedo = canRedo,
                        showGrid = gridVisible,
                        showMinimap = showMinimap,
                        onUndo = { viewModel.undo() },
                        onRedo = { viewModel.redo() },
                        onZoomIn = { viewModel.requestZoomStep(true) },
                        onZoomOut = { viewModel.requestZoomStep(false) },
                        onFitScreen = { viewModel.requestFitScreen() },
                        onToggleGrid = { viewModel.toggleShowGrid() },
                        onToggleMinimap = { viewModel.toggleMinimap() },
                        onOpenLayers = { viewModel.openLayersSheet() },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 10.dp)
                    )

                    if (isCalibrating) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(12.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            tonalElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "ম্যাপে ২টি জানা বিন্দুতে ট্যাপ করুন (${BengaliFormatters.toBengaliDigits(calibrationPoints.size)}/২)",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Spacer(Modifier.width(8.dp))
                                TextButton(onClick = { viewModel.cancelCalibrationMode() }) { Text("বাতিল") }
                            }
                        }
                    }

                    if (selectedVertexIndex != null && project.activePlot != null) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp),
                            shape = RoundedCornerShape(24.dp),
                            tonalElevation = 6.dp,
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "শীর্ষবিন্দু ${BengaliFormatters.toBengaliDigits((selectedVertexIndex ?: 0) + 1)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                                IconButton(onClick = { viewModel.insertVertexAtEdgeMidpoint(selectedVertexIndex ?: 0) }) {
                                    Icon(Icons.Default.Add, contentDescription = "মাঝে বিন্দু যোগ")
                                }
                                IconButton(onClick = { viewModel.deleteSelectedVertex() }) {
                                    Icon(Icons.Default.Delete, contentDescription = "শীর্ষবিন্দু মুছুন", tint = MaterialTheme.colorScheme.error)
                                }
                                IconButton(onClick = { viewModel.selectVertex(null) }) {
                                    Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন")
                                }
                            }
                        }
                    }

                    if (activeTool == "draw") {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 10.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = BhumiOrangeSoft,
                            tonalElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "ম্যাপে ট্যাপ করে সীমানা আঁকুন (${BengaliFormatters.toBengaliDigits(drawingPoints.size)})",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (drawingPoints.isNotEmpty()) {
                                    TextButton(onClick = { viewModel.removeLastDrawingPoint() }) {
                                        Text("শেষটি মুছুন", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }

                // Tablet / Wide Screen 3rd Pane Plot Inspector (Mockup #17)
                if (isWideTablet) {
                    VerticalDivider()
                    TabletPlotInspectorDrawer(
                        activePlot = project.activePlot,
                        sqFeetPerShotok = project.sqFeetPerShotok,
                        feetPerCanvasUnit = project.feetPerCanvasUnit,
                        onAction = { action ->
                            val plot = project.activePlot ?: return@TabletPlotInspectorDrawer
                            when (action) {
                                "interactive_road" -> viewModel.enterInteractiveRoadMode()
                                "divide" -> viewModel.openDivisionModal()
                                "metadata" -> editingPlotMetadata = plot
                                "annotation" -> viewModel.openAnnotationDialog(plot.id)
                                "hide" -> viewModel.togglePlotHidden(plot.id)
                                "highlight" -> viewModel.togglePlotHighlight(plot.id)
                                "road" -> viewModel.togglePlotAsRoad(plot.id)
                                "delete" -> viewModel.deleteActivePlotOrVertex()
                            }
                        },
                        modifier = Modifier
                            .width(310.dp)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }

    // Modals & Bottom Sheets
    if (showSourceModal) {
        ChooseSourceModal(
            onPickImage = {
                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onPickPdfOrFile = {
                pdfPicker.launch(arrayOf("application/pdf", "image/*"))
            },
            onPickDxfCad = {
                dxfPicker.launch(arrayOf("*/*"))
            },
            onLoadSampleDxf = {
                viewModel.loadSampleDxfCadLayer()
                viewModel.closeChooseSourceModal()
            },
            onDismiss = { viewModel.closeChooseSourceModal() }
        )
    }

    if (showScaleModal) {
        ScaleCalibrationModal(
            currentScaleId = project.scaleId,
            currentScanDpi = project.scanDpi,
            onApplyPreset = { sId, dpi, customDpi, customFpi ->
                viewModel.saveScaleSettings(sId, dpi, customDpi, customFpi)
            },
            onStartTwoPointCalibration = { viewModel.startCalibrationMode() },
            onDismiss = { viewModel.closeScaleModal() },
            isPdfSource = isPdfSource
        )
    }

    if (showCalDistanceDialog && calibrationPoints.size == 2) {
        val px = hypot(
            (calibrationPoints[1].x - calibrationPoints[0].x).toDouble(),
            (calibrationPoints[1].y - calibrationPoints[0].y).toDouble()
        )
        CalibrationDistanceDialog(
            pixelDistance = px,
            onApply = { feet -> viewModel.applyCalibrationDistance(feet) },
            onCancel = { viewModel.cancelCalibrationMode() }
        )
    }

    if (showShotokModal) {
        ShotokStandardModal(
            currentFactor = project.sqFeetPerShotok,
            onApply = { factor, saveDefault -> viewModel.updateShotokStandard(factor, saveDefault) },
            onDismiss = { viewModel.closeShotokStandardModal() }
        )
    }

    if (showTextLineModal) {
        TextLineSizeModal(
            currentFontSizeSp = project.labelFontSizeSp,
            currentStrokeWidthPx = project.strokeWidthPx,
            onApply = { f, s -> viewModel.updateTextAndLineSize(f, s) },
            onDismiss = { viewModel.closeTextLineSizeModal() },
            canvasSettings = project.canvasSettings,
            onUpdateCanvasSettings = { viewModel.updateCanvasSettings(it) }
        )
    }

    if (showMetadataDialog) {
        ProjectMetadataDialog(
            project = project,
            onSave = { title, mouza, dist, upz, jl, khat, dag, owner, notes ->
                viewModel.updateProjectMetadata(title, mouza, dist, upz, jl, khat, dag, owner, notes = notes)
            },
            onDismiss = { viewModel.closeProjectMetadataDialog() }
        )
    }

    if (showRenameDialog) {
        RenameDialog(
            currentTitle = project.title,
            onRename = { viewModel.renameProject(it) },
            onDismiss = { viewModel.closeRenameDialog() }
        )
    }

    if (annotationPlotId != null) {
        val target = project.plots.firstOrNull { it.id == annotationPlotId }
        AnnotationDialog(
            initialText = target?.annotationText.orEmpty(),
            onSave = { txt -> viewModel.savePlotAnnotation(annotationPlotId!!, txt) },
            onDismiss = { viewModel.dismissAnnotationDialog() }
        )
    }

    editingPlotMetadata?.let { plot ->
        PlotMetadataDialog(
            plot = plot,
            onSave = { name, dag, owner, khatian, notes ->
                viewModel.selectPlotById(plot.id)
                viewModel.updateActivePlotMetadata(name, dag, owner, khatian, notes)
                editingPlotMetadata = null
            },
            onDismiss = { editingPlotMetadata = null }
        )
    }

    editingLabelStylePlot?.let { plot ->
        val freshPlot = project.plots.firstOrNull { it.id == plot.id } ?: plot
        PlotLabelStyleDialog(
            plot = freshPlot,
            onUpdate = { transform -> viewModel.setLabelStyle(freshPlot.id, transform) },
            onCopy = { viewModel.copyLabelStyle(freshPlot.id) },
            onPaste = { viewModel.pasteLabelStyle(freshPlot.id) },
            onReset = { viewModel.resetLabelStyle(freshPlot.id) },
            onDismiss = { editingLabelStylePlot = null }
        )
    }

    if (showLayersSheet) {
        LayersBottomSheet(
            layers = project.layers,
            onToggleVisibility = { viewModel.toggleLayerVisibility(it) },
            onToggleLocked = { viewModel.toggleLayerLocked(it) },
            onSetOpacity = { id, op -> viewModel.setLayerOpacity(id, op) },
            onMoveLayer = { id, up -> viewModel.moveLayer(id, up) },
            onShowAll = { viewModel.showAllLayers() },
            onHideAll = { viewModel.hideAllLayers() },
            onDismiss = { viewModel.closeLayersSheet() }
        )
    }

    if (showCadSheet) {
        CanvasSettingsSheet(
            settings = project.canvasSettings,
            onUpdate = { viewModel.updateCanvasSettings(it) },
            onReset = { viewModel.resetCanvasSettings() },
            onDismiss = { showCadSheet = false }
        )
    }

    if (showSelectionSheet) {
        MultiPlotSelectionSheet(
            selectedCount = project.selectedPlotIds.size,
            onSelectAll = { viewModel.selectAllPlots() },
            onDeselectAll = { viewModel.deselectAllPlots() },
            onInvert = { viewModel.invertPlotSelection() },
            onSelectAdjacent = { viewModel.selectAdjacentPlots() },
            onSelectByDag = { viewModel.selectPlotsByDagNumber(it) },
            onSelectByOwner = { viewModel.selectPlotsByOwner(it) },
            onSelectByAreaRange = { lo, hi -> viewModel.selectPlotsByAreaRange(lo, hi) },
            onDismiss = { showSelectionSheet = false }
        )
    }

    if (showSnapshotsSheet) {
        SnapshotsSheet(
            snapshots = snapshots,
            onCreateSnapshot = { viewModel.createNamedSnapshot(it) },
            onRestoreSnapshot = { viewModel.restoreSnapshot(it) },
            onDismiss = { viewModel.closeSnapshotsSheet() }
        )
    }

    if (showDivisionModal) {
        DivisionModal(
            state = divisionState,
            activePlotName = project.activePlot?.name ?: "প্লট",
            onSetType = { viewModel.setDivisionType(it) },
            onAdjustParts = { viewModel.adjustDivisionPartsDelta(it) },
            onUpdatePercentage = { idx, v -> viewModel.updateDivisionPercentage(idx, v) },
            onUpdateCustomShotok = { idx, v -> viewModel.updateCustomTargetShotok(idx, v) },
            onSelectRoad = { viewModel.setSelectedRoadOption(it) },
            onAdjustRoadWidth = { viewModel.adjustRoadWidthDelta(it) },
            onSweepAngleChange = { viewModel.setDivisionSweepAngle(it) },
            onPreview = { viewModel.previewDivision() },
            onCommit = {
                viewModel.commitDivision()
                if (divisionState.divisionReport != null) {
                    showPostDivisionSheet = true
                }
            },
            onDismiss = { viewModel.closeDivisionModal() },
            onStartInteractiveRoad = { viewModel.enterInteractiveRoadMode() }
        )
    }

    if (showPostDivisionSheet && divisionState.divisionReport != null) {
        PostDivisionBottomSheet(
            report = divisionState.divisionReport!!,
            onUndoDivision = {
                viewModel.undo()
                showPostDivisionSheet = false
            },
            onOpenPrint = {
                showPostDivisionSheet = false
                onOpenPrintPreview()
            },
            onDismiss = { showPostDivisionSheet = false }
        )
    }

    if (showSaveDialog) {
        SaveSurveyDialog(
            currentStatus = project.status,
            currentTitle = project.title,
            currentMouza = project.mouzaName,
            currentNotes = project.notes,
            totalAreaShotok = project.totalLandAreaShotangsho,
            plotCount = project.plots.size,
            totalPerimeterFeet = project.plots.sumOf { it.perimeterFeet },
            onSave = { st ->
                viewModel.saveProjectToRoom(st)
                showSaveDialog = false
            },
            onSaveWithMetadata = { st, title, mouza, notes ->
                viewModel.updateProjectMetadata(title = title, mouzaName = mouza, notes = notes)
                viewModel.saveProjectToRoom(st)
                showSaveDialog = false
            },
            onDismiss = { showSaveDialog = false }
        )
    }

    if (showGeorefSheet) {
        GeorefOverlayPanel(
            opacity = georefOpacity,
            controlPoints = controlPoints,
            quality = georefQuality,
            onOpacityChange = { viewModel.setGeorefOpacity(it) },
            onAddSamplePoint = { viewModel.addControlPoint(it) },
            onDismiss = { showGeorefSheet = false }
        )
    }
}

@Composable
private fun TabletPlotInspectorDrawer(
    activePlot: LandPlot?,
    sqFeetPerShotok: Double,
    feetPerCanvasUnit: Double,
    onAction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("প্লট পরিদর্শক", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (activePlot == null) {
            Text(
                "বিস্তারিত ও বিভাজন টুল দেখতে ম্যাপ থেকে একটি প্লট নির্বাচন করুন।",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }

        val liveShotok = activePlot.areaSqFt / sqFeetPerShotok.coerceAtLeast(1.0)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(BhumiEmeraldContainer)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = if (activePlot.dagNumber.isNotBlank()) "${activePlot.name} • দাগ ${activePlot.dagNumber}" else activePlot.name,
                fontWeight = FontWeight.Bold,
                color = BhumiEmerald
            )
            Text("${BengaliFormatters.formatDecimal(liveShotok, 2)} শতক (${BengaliFormatters.formatDecimal(activePlot.areaSqFt, 1)} বর্গফুট)", fontWeight = FontWeight.SemiBold)
            Text("পরিসীমা: ${BengaliFormatters.formatDecimal(activePlot.perimeterFeet, 1)} ফুট")
        }

        val actionItems = listOf(
            Triple("interactive_road", "রেখা টেনে ভাগ করুন", Icons.Default.EditRoad),
            Triple("divide", "ভাগ করুন / রাস্তা দিন", Icons.Default.ContentCut),
            Triple("metadata", "নাম ও দাগ সম্পাদনা", Icons.Default.Edit),
            Triple("hide", if (activePlot.isHidden) "মাপ দেখান" else "মাপ লুকান", if (activePlot.isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff),
            Triple("highlight", "হাইলাইট টগল", Icons.Default.CropFree),
            Triple("delete", "প্লট মুছুন", Icons.Default.Delete)
        )

        actionItems.forEach { (key, label, icon) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                    .clickable { onAction(key) }
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (key == "delete") MaterialTheme.colorScheme.error else if (key == "interactive_road") BhumiOrangeCta else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (key == "delete") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        HorizontalDivider()
        Text("বাহুর দৈর্ঘ্য সারণী", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        activePlot.points.indices.forEach { i ->
            val a = activePlot.points[i]
            val b = activePlot.points[(i + 1) % activePlot.points.size]
            val feet = GeometryUtils.edgeLengthUnits(a, b) * feetPerCanvasUnit
            val formatted = LengthFormatter.format(feet, com.example.data.model.MeasurementUnit.FEET_INCHES, 2, useBengali = true)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("বাহু ${BengaliFormatters.toBengaliDigits(i + 1)}", style = MaterialTheme.typography.bodyMedium)
                Text(formatted, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
