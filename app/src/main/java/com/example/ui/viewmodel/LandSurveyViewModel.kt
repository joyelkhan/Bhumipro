package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ExportRecordEntity
import com.example.data.local.ImportedAssetEntity
import com.example.data.local.LandProDatabase
import com.example.data.local.PrintConfigStore
import com.example.data.local.PrintPresetEntity
import com.example.data.local.ProjectSnapshotEntity
import com.example.data.local.SettingsDataStore
import com.example.data.model.AppPreferences
import com.example.data.model.CanvasDisplaySettings
import com.example.data.model.ControlPoint
import com.example.data.model.DivisionReport
import com.example.data.model.DivisionType
import com.example.data.model.ExportFormat
import com.example.data.model.GeorefQualityReport
import com.example.data.model.GisLayerItem
import com.example.data.model.LandPlot
import com.example.data.model.LengthFormatter
import com.example.data.model.PlotLabelStyle
import com.example.data.model.PointF
import com.example.data.model.PrintConfig
import com.example.data.model.PrintJobState
import com.example.data.model.PrintLayoutPreset
import com.example.data.model.PrintLineColorMode
import com.example.data.model.SCAN_DPI_AUTO
import com.example.data.model.SCAN_DPI_CUSTOM
import com.example.data.model.SurveyProject
import com.example.data.model.WorkspaceMode
import com.example.data.model.mouzaScalePresets
import com.example.data.repository.LandProRepository
import com.example.domain.BarcodeAndQrHelper
import com.example.domain.BengaliFormatters
import com.example.domain.CadMapHelper
import com.example.domain.ExportCenterHelper
import com.example.domain.GeometryUtils
import com.example.domain.GeoreferencingHelper
import com.example.domain.JtsDivisionEngine
import com.example.domain.MapTransformMath
import com.example.domain.PdfAndPrintHelper
import com.example.domain.TiledBitmapManager
import com.example.domain.report.BengaliSurveyReportRenderer
import com.example.domain.report.PageLayout
import com.example.domain.report.PrintPageMath
import com.example.domain.report.ReportPdfExporter
import com.example.domain.report.ReportTemplate
import com.example.domain.report.ReportTicket
import com.example.domain.report.SurveyReportData
import com.example.domain.report.SurveyReportDataMapper
import com.example.ui.common.UiMessage
import com.example.ui.common.UiMessageHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.hypot

data class UndoSnapshot(
    val plots: List<LandPlot>,
    val activePlotIndex: Int,
    val layers: List<GisLayerItem>
)

data class CanvasViewportCommand(
    val id: Long = System.nanoTime(),
    val type: String = "fit_screen",
    val zoomPercent: Int = 100
)

class LandSurveyViewModel(
    application: Application,
    private val databaseOverride: LandProDatabase? = null
) : AndroidViewModel(application) {

    private val db: LandProDatabase = databaseOverride ?: LandProDatabase.getInstance(application)
    private val repository = LandProRepository(db)
    private val settingsStore = SettingsDataStore(application)
    private val printConfigStore = PrintConfigStore(application)
    private val tiledBitmapManager = TiledBitmapManager()
    private val messageHost = UiMessageHost()

    private val _uiState = MutableStateFlow(SurveyProject())
    val uiState: StateFlow<SurveyProject> = _uiState.asStateFlow()

    val preferences: StateFlow<AppPreferences> = settingsStore.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppPreferences())

    private val _workspaceMode = MutableStateFlow(WorkspaceMode.MOUZA_SURVEY)
    val workspaceMode: StateFlow<WorkspaceMode> = _workspaceMode.asStateFlow()

    private val _workspaceReady = MutableStateFlow(false)
    val workspaceReady: StateFlow<Boolean> = _workspaceReady.asStateFlow()

    private val _activeTool = MutableStateFlow("select")
    val activeTool: StateFlow<String> = _activeTool.asStateFlow()

    private val _selectedVertexIndex = MutableStateFlow<Int?>(null)
    val selectedVertexIndex: StateFlow<Int?> = _selectedVertexIndex.asStateFlow()

    private val _drawingPoints = MutableStateFlow<List<PointF>>(emptyList())
    val drawingPoints: StateFlow<List<PointF>> = _drawingPoints.asStateFlow()

    private val _tapeMeasurePoints = MutableStateFlow<List<PointF>>(emptyList())
    val tapeMeasurePoints: StateFlow<List<PointF>> = _tapeMeasurePoints.asStateFlow()

    // Undo / Redo (workspace geometry)
    private val undoStack = ArrayDeque<UndoSnapshot>()
    private val redoStack = ArrayDeque<UndoSnapshot>()
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    // Dialog / sheet flags
    private val _showDivisionModal = MutableStateFlow(false)
    val showDivisionModal: StateFlow<Boolean> = _showDivisionModal.asStateFlow()
    private val _showScaleModal = MutableStateFlow(false)
    val showScaleModal: StateFlow<Boolean> = _showScaleModal.asStateFlow()
    private val _showChooseSourceModal = MutableStateFlow(false)
    val showChooseSourceModal: StateFlow<Boolean> = _showChooseSourceModal.asStateFlow()
    private val _showRenameDialog = MutableStateFlow(false)
    val showRenameDialog: StateFlow<Boolean> = _showRenameDialog.asStateFlow()
    private val _showProjectMetadataDialog = MutableStateFlow(false)
    val showProjectMetadataDialog: StateFlow<Boolean> = _showProjectMetadataDialog.asStateFlow()
    private val _showTextLineSizeModal = MutableStateFlow(false)
    val showTextLineSizeModal: StateFlow<Boolean> = _showTextLineSizeModal.asStateFlow()
    private val _showShotokStandardModal = MutableStateFlow(false)
    val showShotokStandardModal: StateFlow<Boolean> = _showShotokStandardModal.asStateFlow()
    private val _showAnnotationDialog = MutableStateFlow<String?>(null)
    val showAnnotationDialog: StateFlow<String?> = _showAnnotationDialog.asStateFlow()
    private val _showLayersSheet = MutableStateFlow(false)
    val showLayersSheet: StateFlow<Boolean> = _showLayersSheet.asStateFlow()
    private val _showSnapshotsSheet = MutableStateFlow(false)
    val showSnapshotsSheet: StateFlow<Boolean> = _showSnapshotsSheet.asStateFlow()

    // Calibration
    private val _isCalibrating = MutableStateFlow(false)
    val isCalibrating: StateFlow<Boolean> = _isCalibrating.asStateFlow()
    private val _calibrationPoints = MutableStateFlow<List<PointF>>(emptyList())
    val calibrationPoints: StateFlow<List<PointF>> = _calibrationPoints.asStateFlow()
    private val _showCalibrationDistanceDialog = MutableStateFlow(false)
    val showCalibrationDistanceDialog: StateFlow<Boolean> = _showCalibrationDistanceDialog.asStateFlow()
    private val _controlPoints = MutableStateFlow<List<ControlPoint>>(emptyList())
    val controlPoints: StateFlow<List<ControlPoint>> = _controlPoints.asStateFlow()
    private val _georefQuality = MutableStateFlow(GeorefQualityReport(0, 0.0, "পয়েন্ট নেই"))
    val georefQuality: StateFlow<GeorefQualityReport> = _georefQuality.asStateFlow()

    // Canvas view state & toggles
    private val _showMinimap = MutableStateFlow(false)
    val showMinimap: StateFlow<Boolean> = _showMinimap.asStateFlow()
    private val _snapEnabled = MutableStateFlow(true)
    val snapEnabled: StateFlow<Boolean> = _snapEnabled.asStateFlow()
    private val _gridSnapEnabled = MutableStateFlow(false)
    val gridSnapEnabled: StateFlow<Boolean> = _gridSnapEnabled.asStateFlow()
    private val _orthoSnapEnabled = MutableStateFlow(false)
    val orthoSnapEnabled: StateFlow<Boolean> = _orthoSnapEnabled.asStateFlow()
    private val _canvasRotationDeg = MutableStateFlow(0f)
    val canvasRotationDeg: StateFlow<Float> = _canvasRotationDeg.asStateFlow()
    private val _viewportCommand = MutableStateFlow<CanvasViewportCommand?>(null)
    val viewportCommand: StateFlow<CanvasViewportCommand?> = _viewportCommand.asStateFlow()

    // CAD / Georef / Comparative
    private val _cadOverlays = MutableStateFlow<List<com.example.domain.CadOverlayEntity>>(emptyList())
    val cadOverlays: StateFlow<List<com.example.domain.CadOverlayEntity>> = _cadOverlays.asStateFlow()
    private val _georefOpacity = MutableStateFlow(0.75f)
    val georefOpacity: StateFlow<Float> = _georefOpacity.asStateFlow()
    private val _pantographSwipeRatio = MutableStateFlow(0.5f)
    val pantographSwipeRatio: StateFlow<Float> = _pantographSwipeRatio.asStateFlow()
    private val _sideBySideComparison = MutableStateFlow(false)
    val sideBySideComparison: StateFlow<Boolean> = _sideBySideComparison.asStateFlow()
    private val _comparativeSummary = MutableStateFlow("")
    val comparativeSummary: StateFlow<String> = _comparativeSummary.asStateFlow()

    // Division
    private val _divisionState = MutableStateFlow(DivisionUiState())
    val divisionState: StateFlow<DivisionUiState> = _divisionState.asStateFlow()
    val divisionType: StateFlow<DivisionType> = MutableStateFlow(DivisionType.EQUAL).also { flow ->
        viewModelScope.launch { _divisionState.collect { flow.value = it.divisionType } }
    }
    val divisionPartsCount: StateFlow<Int> = MutableStateFlow(4).also { flow ->
        viewModelScope.launch { _divisionState.collect { flow.value = it.divisionPartsCount } }
    }
    val selectedRoadOption: StateFlow<String> = MutableStateFlow("middle_horizontal").also { flow ->
        viewModelScope.launch { _divisionState.collect { flow.value = it.selectedRoadOption } }
    }
    val roadWidthFeet: StateFlow<Float> = MutableStateFlow(6f).also { flow ->
        viewModelScope.launch { _divisionState.collect { flow.value = it.roadWidthFeet } }
    }
    val divisionPercentages: StateFlow<List<Double>> = MutableStateFlow(listOf(25.0, 25.0, 25.0, 25.0)).also { flow ->
        viewModelScope.launch { _divisionState.collect { flow.value = it.divisionPercentages } }
    }
    val customTargetShotoks: StateFlow<List<Double>> = MutableStateFlow(listOf(2.0, 2.0, 2.0)).also { flow ->
        viewModelScope.launch { _divisionState.collect { flow.value = it.customTargetShotoks } }
    }
    val previewDividedPlots: StateFlow<List<LandPlot>> = MutableStateFlow<List<LandPlot>>(emptyList()).also { flow ->
        viewModelScope.launch { _divisionState.collect { flow.value = it.previewDividedPlots } }
    }
    val divisionReport: StateFlow<DivisionReport?> = MutableStateFlow<DivisionReport?>(null).also { flow ->
        viewModelScope.launch { _divisionState.collect { flow.value = it.divisionReport } }
    }

    // Print state (v1.6)
    private val _printConfig = MutableStateFlow(PrintConfig())
    val printConfig: StateFlow<PrintConfig> = _printConfig.asStateFlow()
    private val printUndoStack = ArrayDeque<PrintConfig>()
    private val printRedoStack = ArrayDeque<PrintConfig>()
    private val _canUndoPrint = MutableStateFlow(false)
    val canUndoPrint: StateFlow<Boolean> = _canUndoPrint.asStateFlow()
    private val _canRedoPrint = MutableStateFlow(false)
    val canRedoPrint: StateFlow<Boolean> = _canRedoPrint.asStateFlow()

    private val _printJobState = MutableStateFlow(PrintJobState())
    val printJobState: StateFlow<PrintJobState> = _printJobState.asStateFlow()

    val printPresets: StateFlow<List<PrintPresetEntity>> = repository.observePrintPresets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _pdfBitmap = MutableStateFlow<Bitmap?>(null)
    val pdfBitmap: StateFlow<Bitmap?> = _pdfBitmap.asStateFlow()

    private val _isPdfSource = MutableStateFlow(true)
    val isPdfSource: StateFlow<Boolean> = _isPdfSource.asStateFlow()

    val statusMessage: StateFlow<UiMessage?> = messageHost.message

    private var preserveSourceOnNextInit = false
    private var copiedPlotGeometry: LandPlot? = null
    private var copiedLabelStyle: PlotLabelStyle? = null
    private var exportJob: Job? = null
    private var exportCancelFlag = AtomicBoolean(false)
    private var printJobObserverStarted = false

    init {
        observePrintJobState()
    }

    fun markPreserveSourceOnNextInit() {
        preserveSourceOnNextInit = true
    }

    fun initWorkspace(projectId: String, mode: WorkspaceMode = WorkspaceMode.MOUZA_SURVEY) {
        _workspaceMode.value = mode
        if (preserveSourceOnNextInit && _workspaceReady.value && _uiState.value.id == projectId) {
            preserveSourceOnNextInit = false
            return
        }
        preserveSourceOnNextInit = false
        _workspaceReady.value = false
        viewModelScope.launch {
            repository.ensureDefaultSeeded()
            var loaded = repository.getProjectById(projectId)
            if (loaded == null) {
                loaded = SurveyProject(
                    id = projectId,
                    title = "মৌজা জরিপ",
                    mouzaName = "৩৫-পশ্চিম রাউজান-৪",
                    isCalibrated = true,
                    updatedAt = System.currentTimeMillis()
                )
                repository.saveProject(loaded)
            }
            val effectivePrint = printConfigStore.loadEffective(loaded.id).copy(
                mouzaName = loaded.mouzaName,
                jlNumber = loaded.jlNumber
            )
            _uiState.value = loaded
            _printConfig.value = effectivePrint
            undoStack.clear()
            redoStack.clear()
            printUndoStack.clear()
            printRedoStack.clear()
            syncUndoFlags()
            syncPrintUndoFlags()

            // Load raster map or built-in sample Mouza sheet
            val latestAsset = repository.getLatestAssetForProject(loaded.id)
            _isPdfSource.value = if (latestAsset != null) {
                latestAsset.uriString.contains("pdf", ignoreCase = true)
            } else {
                true
            }
            val raster = withContext(Dispatchers.IO) {
                if (latestAsset != null) {
                    tiledBitmapManager.loadFromUri(getApplication(), latestAsset.uriString, latestAsset.pageIndex)?.bitmap
                } else null
            } ?: tiledBitmapManager.generateBuiltInSampleMouzaSheet()
            _pdfBitmap.value = raster
            _workspaceReady.value = true
        }
    }

    fun saveProjectToRoom(statusOverride: String? = null) {
        viewModelScope.launch {
            val cur = _uiState.value
            val toSave = if (statusOverride != null) cur.copy(status = statusOverride) else cur
            _uiState.value = toSave
            repository.saveProject(toSave.copy(updatedAt = System.currentTimeMillis()))
            messageHost.show("সংরক্ষিত হয়েছে")
        }
    }

    fun renameProject(newTitle: String) {
        val clean = newTitle.trim()
        if (clean.isEmpty()) return
        _uiState.value = _uiState.value.copy(title = clean, updatedAt = System.currentTimeMillis())
        saveProjectToRoom()
        _showRenameDialog.value = false
    }

    fun updateProjectMetadata(
        title: String = _uiState.value.title,
        mouzaName: String = _uiState.value.mouzaName,
        district: String = _uiState.value.district,
        upazila: String = _uiState.value.upazila,
        jlNumber: String = _uiState.value.jlNumber,
        khatianNo: String = _uiState.value.khatianNo,
        dagNo: String = _uiState.value.dagNo,
        ownerName: String = _uiState.value.ownerName,
        surveyDate: String = _uiState.value.surveyDate,
        notes: String = _uiState.value.notes,
        referenceCode: String = _uiState.value.referenceCode
    ) {
        _uiState.value = _uiState.value.copy(
            title = title,
            mouzaName = mouzaName,
            district = district,
            upazila = upazila,
            jlNumber = jlNumber,
            khatianNo = khatianNo,
            dagNo = dagNo,
            ownerName = ownerName,
            surveyDate = surveyDate,
            notes = notes,
            referenceCode = referenceCode,
            updatedAt = System.currentTimeMillis()
        )
        _printConfig.value = _printConfig.value.copy(mouzaName = mouzaName, jlNumber = jlNumber)
        _showProjectMetadataDialog.value = false
        saveProjectToRoom()
    }

    // Source & CAD import
    fun openChooseSourceModal() { _showChooseSourceModal.value = true }
    fun closeChooseSourceModal() { _showChooseSourceModal.value = false }

    fun importDocumentOrImageUri(uriString: String) {
        _showChooseSourceModal.value = false
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                tiledBitmapManager.loadFromUri(getApplication(), uriString, 0)
            }
            if (loaded != null) {
                _pdfBitmap.value = loaded.bitmap
                val mime = runCatching {
                    getApplication<Application>().contentResolver.getType(android.net.Uri.parse(uriString))
                }.getOrNull()
                _isPdfSource.value = mime?.contains("pdf", ignoreCase = true) == true ||
                    uriString.contains(".pdf", ignoreCase = true)
                val cur = _uiState.value
                val effectiveDpi = if (cur.scanDpi == SCAN_DPI_AUTO && loaded.detectedDpi in 50..2400) {
                    loaded.detectedDpi
                } else cur.scanDpi
                repository.saveImportedAsset(
                    ImportedAssetEntity(
                        id = "asset_${System.currentTimeMillis()}",
                        projectId = cur.id,
                        assetType = "RASTER_MAP",
                        uriString = uriString,
                        detectedDpi = loaded.detectedDpi,
                        createdAt = System.currentTimeMillis()
                    )
                )
                if (effectiveDpi != cur.scanDpi && effectiveDpi > 0) {
                    saveScaleSettings(cur.scaleId, effectiveDpi)
                }
                messageHost.show("মৌজা ম্যাপ লোড হয়েছে")
            } else {
                messageHost.show("ম্যাপ ফাইল পড়া যায়নি", isError = true)
            }
        }
    }

    fun selectPdfPage(pageIndex: Int) {
        viewModelScope.launch {
            val asset = repository.getLatestAssetForProject(_uiState.value.id) ?: return@launch
            val loaded = withContext(Dispatchers.IO) {
                tiledBitmapManager.loadFromUri(getApplication(), asset.uriString, pageIndex)
            }
            if (loaded != null) {
                _pdfBitmap.value = loaded.bitmap
                repository.saveImportedAsset(asset.copy(pageIndex = pageIndex))
            }
        }
    }

    fun importCadUri(uriString: String) {
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    getApplication<Application>().contentResolver.openInputStream(android.net.Uri.parse(uriString))
                        ?.bufferedReader()?.readText()
                }.getOrNull()
            }
            if (!text.isNullOrBlank()) {
                val entities = CadMapHelper.parseDxfToPolylines(text)
                _cadOverlays.value = entities
                messageHost.show("DXF ওভারলে লোড হয়েছে (${BengaliFormatters.toBengaliDigits(entities.size)}টি)")
            } else {
                messageHost.show("DXF ফাইল পড়া যায়নি", isError = true)
            }
        }
    }

    fun loadSampleDxfCadLayer() {
        val entities = CadMapHelper.parseDxfToPolylines(CadMapHelper.sampleDxfContent())
        _cadOverlays.value = entities
        messageHost.show("নমুনা DXF স্তর যুক্ত হয়েছে")
    }

    fun setGeorefOpacity(opacity: Float) {
        _georefOpacity.value = opacity.coerceIn(0.05f, 1.0f)
    }

    fun addControlPoint(cp: ControlPoint) {
        val next = _controlPoints.value + cp
        _controlPoints.value = next
        _georefQuality.value = GeoreferencingHelper.evaluateQuality(next)
    }

    fun updateControlPointOffset(id: String, offsetX: Float, offsetY: Float) {
        val next = _controlPoints.value.map {
            if (it.id == id) it.copy(offsetX = offsetX, offsetY = offsetY) else it
        }
        _controlPoints.value = next
        _georefQuality.value = GeoreferencingHelper.evaluateQuality(next)
    }

    // Scale Calibration
    fun openScaleModal() { _showScaleModal.value = true }
    fun closeScaleModal() { _showScaleModal.value = false }

    fun saveScaleSettings(
        scaleId: String,
        scanDpi: Int,
        customDpi: Int? = null,
        customFeetPerInch: Double? = null
    ) {
        val cur = _uiState.value
        val preset = mouzaScalePresets.firstOrNull { it.id == scaleId }
        val feetPerInch = customFeetPerInch?.takeIf { it > 0.0 } ?: preset?.feetPerInch ?: 330.0
        val effectiveDpi = when (scanDpi) {
            SCAN_DPI_CUSTOM -> (customDpi ?: 300).coerceIn(50, 2400)
            SCAN_DPI_AUTO -> 300
            else -> scanDpi.coerceIn(50, 2400)
        }
        val newFeetPerUnit = feetPerInch / effectiveDpi.toDouble()
        val scaleLabel = if (customFeetPerInch != null && customFeetPerInch > 0.0) {
            "কাস্টম ${BengaliFormatters.formatDecimal(customFeetPerInch, 1)} ফুট/ইঞ্চি"
        } else {
            preset?.shortLabelBn ?: cur.scale
        }
        pushUndoSnapshot()
        val recalculated = cur.plots.map {
            GeometryUtils.recalculatePlot(it, newFeetPerUnit, cur.sqFeetPerShotok)
        }
        _uiState.value = cur.copy(
            scaleId = scaleId,
            scale = scaleLabel,
            scanDpi = effectiveDpi,
            feetPerCanvasUnit = newFeetPerUnit,
            isCalibrated = true,
            calibrationMethod = "PRESET_SCALE_DPI",
            calibrationUpdatedAt = System.currentTimeMillis(),
            plots = recalculated,
            updatedAt = System.currentTimeMillis()
        )
        _showScaleModal.value = false
        messageHost.show("ক্যালিব্রেশন সফল: ${BengaliFormatters.toBengaliDigits(effectiveDpi)} DPI • প্রতি পিক্সেল ${BengaliFormatters.formatDecimal(newFeetPerUnit, 3)} ফুট")
    }

    fun startCalibrationMode() {
        _showScaleModal.value = false
        _isCalibrating.value = true
        _calibrationPoints.value = emptyList()
        _activeTool.value = "calibrate"
    }

    fun addCalibrationTapPoint(point: PointF) {
        val list = (_calibrationPoints.value + point).take(2)
        _calibrationPoints.value = list
        if (list.size == 2) {
            _showCalibrationDistanceDialog.value = true
        }
    }

    fun applyCalibrationDistance(knownFeet: Double) {
        val pts = _calibrationPoints.value
        if (pts.size < 2 || knownFeet <= 0.0) {
            cancelCalibrationMode()
            return
        }
        val pxDist = hypot((pts[1].x - pts[0].x).toDouble(), (pts[1].y - pts[0].y).toDouble()).coerceAtLeast(1.0)
        val newFeetPerUnit = knownFeet / pxDist
        val cur = _uiState.value
        pushUndoSnapshot()
        val recalculated = cur.plots.map {
            GeometryUtils.recalculatePlot(it, newFeetPerUnit, cur.sqFeetPerShotok)
        }
        _uiState.value = cur.copy(
            feetPerCanvasUnit = newFeetPerUnit,
            isCalibrated = true,
            calibrationMethod = "TWO_POINT",
            calibrationPixelDistance = pxDist,
            calibrationGroundFeet = knownFeet,
            calibrationRmsePx = 0.0,
            calibrationUpdatedAt = System.currentTimeMillis(),
            plots = recalculated,
            updatedAt = System.currentTimeMillis()
        )
        _isCalibrating.value = false
        _calibrationPoints.value = emptyList()
        _showCalibrationDistanceDialog.value = false
        _activeTool.value = "select"
        messageHost.show(
            "ক্যালিব্রেশন সফল: ${BengaliFormatters.formatDecimal(pxDist, 1)} পিক্সেল = " +
                "${BengaliFormatters.formatDecimal(knownFeet, 1)} ফুট • প্রতি পিক্সেল " +
                "${BengaliFormatters.formatDecimal(newFeetPerUnit, 3)} ফুট"
        )
    }

    fun cancelCalibrationMode() {
        _isCalibrating.value = false
        _calibrationPoints.value = emptyList()
        _showCalibrationDistanceDialog.value = false
        _activeTool.value = "select"
    }

    // শতক standard
    fun openShotokStandardModal() { _showShotokStandardModal.value = true }
    fun closeShotokStandardModal() { _showShotokStandardModal.value = false }

    fun updateShotokStandard(sqFeetPerShotok: Double, alsoSaveAsAppDefault: Boolean = false) {
        val factor = sqFeetPerShotok.coerceIn(100.0, 2000.0)
        val cur = _uiState.value
        pushUndoSnapshot()
        val recalculated = cur.plots.map {
            GeometryUtils.recalculatePlot(it, cur.feetPerCanvasUnit, factor)
        }
        _uiState.value = cur.copy(
            sqFeetPerShotok = factor,
            plots = recalculated,
            updatedAt = System.currentTimeMillis()
        )
        _showShotokStandardModal.value = false
        if (alsoSaveAsAppDefault) {
            viewModelScope.launch {
                settingsStore.setAppDefaultShotok(factor)
            }
            messageHost.show("১ শতক = ${BengaliFormatters.formatDecimal(factor, 1)} বর্গফুট প্রয়োগ ও ডিফল্ট হিসেবে সংরক্ষিত")
        } else {
            messageHost.show("১ শতক = ${BengaliFormatters.formatDecimal(factor, 1)} বর্গফুট প্রয়োগ হয়েছে")
        }
    }

    // Text/Line size & metadata modals
    fun openTextLineSizeModal() { _showTextLineSizeModal.value = true }
    fun closeTextLineSizeModal() { _showTextLineSizeModal.value = false }

    fun updateTextAndLineSize(labelFontSizeSp: Float, strokeWidthPx: Float) {
        _uiState.value = _uiState.value.copy(
            labelFontSizeSp = labelFontSizeSp.coerceIn(7f, 28f),
            strokeWidthPx = strokeWidthPx.coerceIn(1f, 12f),
            updatedAt = System.currentTimeMillis()
        )
        _showTextLineSizeModal.value = false
    }

    fun openRenameDialog() { _showRenameDialog.value = true }
    fun closeRenameDialog() { _showRenameDialog.value = false }
    fun openProjectMetadataDialog() { _showProjectMetadataDialog.value = true }
    fun closeProjectMetadataDialog() { _showProjectMetadataDialog.value = false }
    fun openAnnotationDialog(plotId: String) { _showAnnotationDialog.value = plotId }
    fun dismissAnnotationDialog() { _showAnnotationDialog.value = null }

    fun savePlotAnnotation(plotId: String, text: String) {
        pushUndoSnapshot()
        val cur = _uiState.value
        _uiState.value = cur.copy(
            plots = cur.plots.map { if (it.id == plotId) it.copy(annotationText = text) else it }
        )
        _showAnnotationDialog.value = null
    }

    // Layers sheet
    fun openLayersSheet() { _showLayersSheet.value = true }
    fun closeLayersSheet() { _showLayersSheet.value = false }

    fun toggleLayerVisibility(layerId: String) {
        val cur = _uiState.value
        _uiState.value = cur.copy(
            layers = cur.layers.map { if (it.id == layerId) it.copy(visible = !it.visible) else it }
        )
    }

    fun toggleLayerLocked(layerId: String) {
        val cur = _uiState.value
        _uiState.value = cur.copy(
            layers = cur.layers.map { if (it.id == layerId) it.copy(locked = !it.locked) else it }
        )
    }

    fun setLayerOpacity(layerId: String, opacity: Float) {
        val cur = _uiState.value
        _uiState.value = cur.copy(
            layers = cur.layers.map { if (it.id == layerId) it.copy(opacity = opacity.coerceIn(0.05f, 1f)) else it }
        )
    }

    fun renameLayer(layerId: String, name: String) {
        if (name.isBlank()) return
        val cur = _uiState.value
        _uiState.value = cur.copy(
            layers = cur.layers.map { if (it.id == layerId) it.copy(nameBengali = name.trim()) else it }
        )
    }

    fun moveLayer(layerId: String, up: Boolean) {
        val sorted = _uiState.value.layers.sortedBy { it.zOrder }.toMutableList()
        val idx = sorted.indexOfFirst { it.id == layerId }
        if (idx < 0) return
        val swapIdx = if (up) idx + 1 else idx - 1
        if (swapIdx !in sorted.indices) return
        val temp = sorted[idx]
        sorted[idx] = sorted[swapIdx]
        sorted[swapIdx] = temp
        val reindexed = sorted.mapIndexed { i, item -> item.copy(zOrder = i) }
        _uiState.value = _uiState.value.copy(layers = reindexed)
    }

    fun showAllLayers() {
        _uiState.value = _uiState.value.copy(layers = _uiState.value.layers.map { it.copy(visible = true) })
    }

    fun hideAllLayers() {
        _uiState.value = _uiState.value.copy(layers = _uiState.value.layers.map { it.copy(visible = false) })
    }

    // Snapshots
    fun openSnapshotsSheet() { _showSnapshotsSheet.value = true }
    fun closeSnapshotsSheet() { _showSnapshotsSheet.value = false }

    fun observeProjectSnapshots(): Flow<List<ProjectSnapshotEntity>> =
        repository.observeSnapshots(_uiState.value.id)

    fun createNamedSnapshot(label: String) {
        val cur = _uiState.value
        viewModelScope.launch {
            val clean = label.trim().ifEmpty { "স্ন্যাপশট ${BengaliFormatters.bengaliToday()}" }
            repository.createSnapshot(cur, clean)
            messageHost.show("স্ন্যাপশট সংরক্ষিত হয়েছে")
        }
    }

    fun restoreSnapshot(snapshotId: String) {
        viewModelScope.launch {
            val snap = repository.getSnapshotById(snapshotId) ?: return@launch
            pushUndoSnapshot()
            val plots = repository.decodePlots(snap.plotsJson)
            val layers = repository.decodeLayers(snap.layersJson)
            val cur = _uiState.value
            _uiState.value = cur.copy(
                plots = plots,
                layers = layers,
                feetPerCanvasUnit = snap.feetPerCanvasUnit,
                sqFeetPerShotok = snap.sqFeetPerShotok,
                isCalibrated = snap.isCalibrated != 0,
                activePlotIndex = 0,
                selectedPlotIds = cur.selectedPlotIds.filter { id -> plots.any { it.id == id } }.toSet(),
                updatedAt = System.currentTimeMillis()
            )
            _showSnapshotsSheet.value = false
            messageHost.show("স্ন্যাপশট পুনরুদ্ধার হয়েছে: ${snap.label}")
        }
    }

    // Drawing & Plot creation
    fun startDrawingNewPlot() {
        _drawingPoints.value = emptyList()
        _activeTool.value = "draw"
    }

    fun toggleDrawMode() {
        if (_activeTool.value == "draw") {
            _activeTool.value = "select"
            _drawingPoints.value = emptyList()
        } else {
            startDrawingNewPlot()
        }
    }

    fun addDrawingPoint(point: PointF) {
        val current = _drawingPoints.value
        val snapped = applySnapIfEnabled(point, current.lastOrNull())
        _drawingPoints.value = current + snapped
    }

    fun moveDrawingPoint(index: Int, point: PointF) {
        val list = _drawingPoints.value.toMutableList()
        if (index in list.indices) {
            list[index] = point
            _drawingPoints.value = list
        }
    }

    fun removeLastDrawingPoint() {
        val list = _drawingPoints.value
        if (list.isNotEmpty()) _drawingPoints.value = list.dropLast(1)
    }

    fun cancelDrawingPolygon() {
        _drawingPoints.value = emptyList()
        _activeTool.value = "select"
    }

    fun closeDrawingPolygon() {
        val pts = _drawingPoints.value
        if (pts.size < 3) {
            messageHost.show("প্লট তৈরি করতে কমপক্ষে ৩টি বিন্দু প্রয়োজন", isError = true)
            return
        }
        val cur = _uiState.value
        val nextNum = cur.plots.size + 1
        val name = "প্লট ${BengaliFormatters.toBengaliDigits(nextNum)}"
        val plot = GeometryUtils.buildPlot(
            id = GeometryUtils.generatePlotId(),
            name = name,
            colorHex = GeometryUtils.plotColorForIndex(nextNum - 1),
            points = pts,
            feetPerCanvasUnit = cur.feetPerCanvasUnit,
            sqFeetPerShotok = cur.sqFeetPerShotok
        )
        pushUndoSnapshot()
        val updatedPlots = cur.plots + plot
        _uiState.value = cur.copy(
            plots = updatedPlots,
            activePlotIndex = updatedPlots.lastIndex,
            updatedAt = System.currentTimeMillis()
        )
        _drawingPoints.value = emptyList()
        _activeTool.value = "select"
        messageHost.show("${plot.name} তৈরি হয়েছে (${BengaliFormatters.formatDecimal(plot.areaShotangsho, 2)} শতক)")
    }

    fun addNewPlot(
        points: List<PointF> = listOf(
            PointF(120f, 120f),
            PointF(320f, 120f),
            PointF(320f, 280f),
            PointF(120f, 280f)
        )
    ) {
        val cur = _uiState.value
        val nextNum = cur.plots.size + 1
        val plot = GeometryUtils.buildPlot(
            id = GeometryUtils.generatePlotId(),
            name = "প্লট ${BengaliFormatters.toBengaliDigits(nextNum)}",
            colorHex = GeometryUtils.plotColorForIndex(nextNum - 1),
            points = points,
            feetPerCanvasUnit = cur.feetPerCanvasUnit,
            sqFeetPerShotok = cur.sqFeetPerShotok
        )
        pushUndoSnapshot()
        val updated = cur.plots + plot
        _uiState.value = cur.copy(
            plots = updated,
            activePlotIndex = updated.lastIndex,
            updatedAt = System.currentTimeMillis()
        )
        messageHost.show("${plot.name} তৈরি হয়েছে (${BengaliFormatters.formatDecimal(plot.areaShotangsho, 2)} শতক)")
    }

    // Tool & Selection (8 modes)
    fun setActiveTool(tool: String) {
        _activeTool.value = tool
        if (tool != "draw") _drawingPoints.value = emptyList()
        if (tool != "measure") _tapeMeasurePoints.value = emptyList()
    }

    fun selectPlotById(plotId: String) {
        val cur = _uiState.value
        val idx = cur.plots.indexOfFirst { it.id == plotId }
        if (idx >= 0) {
            _uiState.value = cur.copy(activePlotIndex = idx)
            _selectedVertexIndex.value = null
        }
    }

    fun selectPlotByIndex(index: Int) {
        val cur = _uiState.value
        if (index in cur.plots.indices) {
            _uiState.value = cur.copy(activePlotIndex = index)
            _selectedVertexIndex.value = null
        }
    }

    fun togglePlotSelectionAndActivate(plotId: String) {
        val cur = _uiState.value
        val idx = cur.plots.indexOfFirst { it.id == plotId }
        if (idx < 0) return
        val nextSel = cur.selectedPlotIds.toMutableSet().apply {
            if (contains(plotId)) remove(plotId) else add(plotId)
        }
        _uiState.value = cur.copy(activePlotIndex = idx, selectedPlotIds = nextSel)
    }

    fun togglePlotReportSelection(plotId: String) {
        val cur = _uiState.value
        if (cur.plots.none { it.id == plotId }) return
        val nextSel = cur.selectedPlotIds.toMutableSet().apply {
            if (contains(plotId)) remove(plotId) else add(plotId)
        }
        _uiState.value = cur.copy(selectedPlotIds = nextSel)
    }

    fun selectPlotsInRectangle(a: PointF, b: PointF) {
        val cur = _uiState.value
        val ids = SurveySelectionHelper.selectInRectangle(cur.plots, a, b)
        _uiState.value = cur.copy(selectedPlotIds = ids)
    }

    fun selectAllPlots() {
        val cur = _uiState.value
        _uiState.value = cur.copy(selectedPlotIds = SurveySelectionHelper.selectAllVisible(cur.plots))
    }

    fun deselectAllPlots() {
        _uiState.value = _uiState.value.copy(selectedPlotIds = emptySet())
    }

    fun clearPlotSelection() = deselectAllPlots()

    fun invertPlotSelection() {
        val cur = _uiState.value
        _uiState.value = cur.copy(
            selectedPlotIds = SurveySelectionHelper.invertSelection(cur.plots, cur.selectedPlotIds)
        )
    }

    fun selectAdjacentPlots() {
        val cur = _uiState.value
        val ids = SurveySelectionHelper.selectAdjacent(cur.plots, cur.activePlot)
        _uiState.value = cur.copy(selectedPlotIds = ids)
    }

    fun selectPlotsByDagNumber(dag: String) {
        val cur = _uiState.value
        _uiState.value = cur.copy(selectedPlotIds = SurveySelectionHelper.selectByDag(cur.plots, dag))
    }

    fun selectPlotsByOwner(owner: String) {
        val cur = _uiState.value
        _uiState.value = cur.copy(selectedPlotIds = SurveySelectionHelper.selectByOwner(cur.plots, owner))
    }

    fun selectPlotsByAreaRange(minShotok: Double, maxShotok: Double) {
        val cur = _uiState.value
        _uiState.value = cur.copy(selectedPlotIds = SurveySelectionHelper.selectByAreaRange(cur.plots, minShotok, maxShotok))
    }

    // Active plot mutations
    fun updateActivePlotMetadata(
        name: String? = null,
        dagNumber: String? = null,
        ownerName: String? = null,
        khatianRef: String? = null,
        notes: String? = null,
        status: String? = null
    ) {
        val cur = _uiState.value
        val active = cur.activePlot ?: return
        pushUndoSnapshot()
        val updatedPlot = active.copy(
            name = name ?: active.name,
            dagNumber = dagNumber ?: active.dagNumber,
            ownerName = ownerName ?: active.ownerName,
            khatianRef = khatianRef ?: active.khatianRef,
            notes = notes ?: active.notes,
            status = status ?: active.status,
            updatedAt = System.currentTimeMillis()
        )
        _uiState.value = cur.copy(plots = cur.plots.map { if (it.id == active.id) updatedPlot else it })
    }

    fun togglePlotAsRoad(plotId: String = _uiState.value.activePlot?.id.orEmpty()) {
        val cur = _uiState.value
        if (plotId.isBlank()) return
        pushUndoSnapshot()
        _uiState.value = cur.copy(
            plots = cur.plots.map { if (it.id == plotId) it.copy(isRoad = !it.isRoad) else it }
        )
    }

    fun togglePlotHidden(plotId: String = _uiState.value.activePlot?.id.orEmpty()) {
        val cur = _uiState.value
        if (plotId.isBlank()) return
        pushUndoSnapshot()
        _uiState.value = cur.copy(
            plots = cur.plots.map { if (it.id == plotId) it.copy(isHidden = !it.isHidden) else it }
        )
    }

    fun togglePlotHighlight(plotId: String = _uiState.value.activePlot?.id.orEmpty()) {
        val cur = _uiState.value
        if (plotId.isBlank()) return
        _uiState.value = cur.copy(
            plots = cur.plots.map { if (it.id == plotId) it.copy(isHighlighted = !it.isHighlighted) else it }
        )
    }

    fun togglePlotLocked(plotId: String = _uiState.value.activePlot?.id.orEmpty()) {
        val cur = _uiState.value
        if (plotId.isBlank()) return
        _uiState.value = cur.copy(
            plots = cur.plots.map { if (it.id == plotId) it.copy(isLocked = !it.isLocked) else it }
        )
    }

    fun duplicateActivePlot() {
        val cur = _uiState.value
        val active = cur.activePlot ?: return
        pushUndoSnapshot()
        val copyPlot = active.copy(
            id = GeometryUtils.generatePlotId(),
            name = "${active.name} (কপি)",
            points = active.points.map { PointF(it.x + 18f, it.y + 18f) },
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val recalculated = GeometryUtils.recalculatePlot(copyPlot, cur.feetPerCanvasUnit, cur.sqFeetPerShotok)
        val nextList = cur.plots + recalculated
        _uiState.value = cur.copy(plots = nextList, activePlotIndex = nextList.lastIndex)
    }

    fun mergeActiveWithNextPlot() {
        val cur = _uiState.value
        if (cur.plots.size < 2) return
        val aIdx = cur.activePlotIndex.coerceIn(0, cur.plots.lastIndex)
        val bIdx = (aIdx + 1) % cur.plots.size
        val a = cur.plots[aIdx]
        val b = cur.plots[bIdx]
        val merged = JtsDivisionEngine.mergePolygons(a, b, cur.feetPerCanvasUnit, cur.sqFeetPerShotok)
        if (merged == null) {
            messageHost.show("প্লট মার্জ করা যায়নি", isError = true)
            return
        }
        pushUndoSnapshot()
        val remaining = cur.plots.filterIndexed { idx, _ -> idx != bIdx }
            .map { if (it.id == a.id) merged else it }
        val newIdx = remaining.indexOfFirst { it.id == merged.id }.coerceAtLeast(0)
        _uiState.value = cur.copy(
            plots = remaining,
            activePlotIndex = newIdx,
            selectedPlotIds = cur.selectedPlotIds - b.id,
            updatedAt = System.currentTimeMillis()
        )
        messageHost.show("প্লট মার্জ সম্পন্ন (${BengaliFormatters.formatDecimal(merged.areaShotangsho, 2)} শতক)")
    }

    fun copyActivePlotGeometry() {
        val active = _uiState.value.activePlot ?: return
        copiedPlotGeometry = active
        messageHost.show("প্লট জ্যামিতি কপি হয়েছে")
    }

    fun pasteCopiedPlotGeometry() {
        val src = copiedPlotGeometry ?: return
        val cur = _uiState.value
        pushUndoSnapshot()
        val offsetPts = src.points.map { PointF(it.x + 24f, it.y + 24f) }
        val pasted = GeometryUtils.buildPlot(
            id = GeometryUtils.generatePlotId(),
            name = "প্লট ${BengaliFormatters.toBengaliDigits(cur.plots.size + 1)} (পেস্ট)",
            colorHex = GeometryUtils.plotColorForIndex(cur.plots.size),
            points = offsetPts,
            feetPerCanvasUnit = cur.feetPerCanvasUnit,
            sqFeetPerShotok = cur.sqFeetPerShotok,
            labelStyle = src.labelStyle
        )
        val next = cur.plots + pasted
        _uiState.value = cur.copy(plots = next, activePlotIndex = next.lastIndex)
        messageHost.show("জ্যামিতি নতুন প্লট হিসেবে পেস্ট হয়েছে")
    }

    // Vertex operations
    fun selectVertex(index: Int?) {
        _selectedVertexIndex.value = index
    }

    fun beginVertexDrag(index: Int) {
        _selectedVertexIndex.value = index
        pushUndoSnapshot()
    }

    fun moveSelectedVertex(newPoint: PointF) {
        val cur = _uiState.value
        val active = cur.activePlot ?: return
        if (active.isLocked) return
        val vIdx = _selectedVertexIndex.value ?: return
        if (vIdx !in active.points.indices) return
        val updatedPts = active.points.toMutableList().apply { this[vIdx] = newPoint }
        val recalc = GeometryUtils.recalculatePlot(active.copy(points = updatedPts), cur.feetPerCanvasUnit, cur.sqFeetPerShotok)
        _uiState.value = cur.copy(plots = cur.plots.map { if (it.id == active.id) recalc else it })
    }

    fun nudgeSelectedVertex(dx: Float, dy: Float) {
        val cur = _uiState.value
        val active = cur.activePlot ?: return
        val vIdx = _selectedVertexIndex.value ?: return
        if (vIdx !in active.points.indices || active.isLocked) return
        pushUndoSnapshot()
        val old = active.points[vIdx]
        moveSelectedVertex(PointF(old.x + dx, old.y + dy))
    }

    fun addVertexToActivePlot(point: PointF? = null) {
        val cur = _uiState.value
        val active = cur.activePlot ?: return
        if (active.isLocked || active.points.isEmpty()) return
        pushUndoSnapshot()
        val newPt = point ?: run {
            val a = active.points.last()
            val b = active.points.first()
            PointF((a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f)
        }
        val recalc = GeometryUtils.recalculatePlot(
            active.copy(points = active.points + newPt),
            cur.feetPerCanvasUnit,
            cur.sqFeetPerShotok
        )
        _uiState.value = cur.copy(plots = cur.plots.map { if (it.id == active.id) recalc else it })
    }

    fun insertVertexAtEdgeMidpoint(edgeIndex: Int) {
        val cur = _uiState.value
        val active = cur.activePlot ?: return
        if (active.isLocked || edgeIndex !in active.points.indices) return
        pushUndoSnapshot()
        val a = active.points[edgeIndex]
        val b = active.points[(edgeIndex + 1) % active.points.size]
        val mid = PointF((a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f)
        val nextPts = active.points.toMutableList().apply { add(edgeIndex + 1, mid) }
        val recalc = GeometryUtils.recalculatePlot(active.copy(points = nextPts), cur.feetPerCanvasUnit, cur.sqFeetPerShotok)
        _uiState.value = cur.copy(plots = cur.plots.map { if (it.id == active.id) recalc else it })
        _selectedVertexIndex.value = edgeIndex + 1
    }

    fun deleteSelectedVertex() {
        val cur = _uiState.value
        val active = cur.activePlot ?: return
        val vIdx = _selectedVertexIndex.value ?: return
        if (active.isLocked || active.points.size <= 3 || vIdx !in active.points.indices) return
        pushUndoSnapshot()
        val nextPts = active.points.toMutableList().apply { removeAt(vIdx) }
        val recalc = GeometryUtils.recalculatePlot(active.copy(points = nextPts), cur.feetPerCanvasUnit, cur.sqFeetPerShotok)
        _uiState.value = cur.copy(plots = cur.plots.map { if (it.id == active.id) recalc else it })
        _selectedVertexIndex.value = null
        messageHost.show("শীর্ষবিন্দু মুছে ফেলা হয়েছে")
    }

    fun deleteActivePlotOrVertex() {
        if (_activeTool.value == "draw" && _drawingPoints.value.isNotEmpty()) {
            removeLastDrawingPoint()
            return
        }
        if (_selectedVertexIndex.value != null) {
            deleteSelectedVertex()
            return
        }
        val cur = _uiState.value
        val active = cur.activePlot ?: return
        if (active.isLocked) return
        pushUndoSnapshot()
        val remaining = cur.plots.filter { it.id != active.id }
        val nextIdx = (cur.activePlotIndex.coerceAtMost(remaining.lastIndex)).coerceAtLeast(0)
        _uiState.value = cur.copy(
            plots = remaining,
            activePlotIndex = nextIdx,
            selectedPlotIds = cur.selectedPlotIds - active.id
        )
    }

    fun reverseActivePlotVertices() {
        val cur = _uiState.value
        val active = cur.activePlot ?: return
        if (active.isLocked) return
        pushUndoSnapshot()
        val recalc = GeometryUtils.recalculatePlot(
            active.copy(points = active.points.reversed()),
            cur.feetPerCanvasUnit,
            cur.sqFeetPerShotok
        )
        _uiState.value = cur.copy(plots = cur.plots.map { if (it.id == active.id) recalc else it })
    }

    // Label styles (v1.4)
    fun setLabelStyle(plotId: String, transform: (PlotLabelStyle) -> PlotLabelStyle) {
        val cur = _uiState.value
        val (updated, changed) = SurveySelectionHelper.updatePlotLabelStyle(cur.plots, plotId, transform)
        if (!changed) return
        pushUndoSnapshot()
        _uiState.value = cur.copy(plots = updated, updatedAt = System.currentTimeMillis())
    }

    fun applyLabelStyleToSelection(transform: (PlotLabelStyle) -> PlotLabelStyle) {
        val cur = _uiState.value
        if (cur.selectedPlotIds.isEmpty()) return
        pushUndoSnapshot()
        _uiState.value = cur.copy(
            plots = SurveySelectionHelper.applyLabelStyleToSelection(cur.plots, cur.selectedPlotIds, transform),
            updatedAt = System.currentTimeMillis()
        )
    }

    fun copyLabelStyle(plotId: String = _uiState.value.activePlot?.id.orEmpty()) {
        val plot = _uiState.value.plots.firstOrNull { it.id == plotId } ?: return
        copiedLabelStyle = plot.labelStyle ?: PlotLabelStyle.DEFAULT
    }

    fun pasteLabelStyle(plotId: String = _uiState.value.activePlot?.id.orEmpty()) {
        val style = copiedLabelStyle ?: return
        setLabelStyle(plotId) { style }
    }

    fun resetLabelStyle(plotId: String = _uiState.value.activePlot?.id.orEmpty()) {
        val cur = _uiState.value
        pushUndoSnapshot()
        _uiState.value = cur.copy(
            plots = cur.plots.map { if (it.id == plotId) it.copy(labelStyle = null) else it }
        )
    }

    fun toggleLabelLocked(plotId: String = _uiState.value.activePlot?.id.orEmpty()) {
        val cur = _uiState.value
        pushUndoSnapshot()
        _uiState.value = cur.copy(
            plots = cur.plots.map {
                if (it.id == plotId) {
                    val s = it.labelStyle ?: PlotLabelStyle.DEFAULT
                    it.copy(labelStyle = s.copy(locked = !s.locked))
                } else it
            }
        )
    }

    fun applyCadFontScaleToSelection(scale: Float) {
        val clamped = scale.coerceIn(0.4f, 4.0f)
        applyLabelStyleToSelection { it.copy(fontSizeScale = clamped) }
    }

    fun beginLabelDrag() {
        pushUndoSnapshot()
    }

    fun dragLabel(plotId: String, deltaOffsetX: Float, deltaOffsetY: Float) {
        val cur = _uiState.value
        val updated = cur.plots.map { plot ->
            if (plot.id == plotId) {
                val s = plot.labelStyle ?: PlotLabelStyle.DEFAULT
                if (s.locked && !s.isDefault()) plot
                else plot.copy(
                    labelStyle = s.copy(
                        offsetX = PlotLabelStyle.offsetClamped(s.offsetX + deltaOffsetX),
                        offsetY = PlotLabelStyle.offsetClamped(s.offsetY + deltaOffsetY),
                        manuallyPositioned = true
                    )
                )
            } else plot
        }
        _uiState.value = cur.copy(plots = updated)
    }

    // Canvas display settings & viewport commands
    fun updateCanvasSettings(transform: (CanvasDisplaySettings) -> CanvasDisplaySettings) {
        val cur = _uiState.value
        _uiState.value = cur.copy(
            canvasSettings = transform(cur.canvasSettings),
            updatedAt = System.currentTimeMillis()
        )
    }

    fun resetCanvasSettings() {
        val cur = _uiState.value
        _uiState.value = cur.copy(
            canvasSettings = CanvasDisplaySettings(),
            updatedAt = System.currentTimeMillis()
        )
    }

    fun toggleShowGrid() = toggleLayerVisibility("layer_grid")
    fun toggleMinimap() { _showMinimap.value = !_showMinimap.value }
    fun toggleSnap() { _snapEnabled.value = !_snapEnabled.value }
    fun toggleGridSnap() { _gridSnapEnabled.value = !_gridSnapEnabled.value }
    fun toggleOrthoSnap() { _orthoSnapEnabled.value = !_orthoSnapEnabled.value }

    fun addTapeMeasurePoint(point: PointF) {
        val list = _tapeMeasurePoints.value
        _tapeMeasurePoints.value = if (list.size >= 2) listOf(point) else list + point
    }

    fun clearTapeMeasure() { _tapeMeasurePoints.value = emptyList() }
    fun requestFitScreen() { _viewportCommand.value = CanvasViewportCommand(type = "fit_screen") }
    fun requestFitPlot() { _viewportCommand.value = CanvasViewportCommand(type = "fit_plot") }
    fun triggerZoomToFit() { requestFitScreen() }

    fun requestZoomStep(zoomIn: Boolean) {
        _viewportCommand.value = CanvasViewportCommand(type = if (zoomIn) "zoom_in" else "zoom_out")
    }

    fun requestZoomPercent(percent: Int) {
        val clamped = percent.coerceIn(15, 1600)
        updateCanvasSettings { it.copy(zoomPresetPercent = clamped) }
        _viewportCommand.value = CanvasViewportCommand(type = "zoom_percent", zoomPercent = clamped)
    }

    fun adjustRotation(deltaDeg: Float) {
        _canvasRotationDeg.value = (_canvasRotationDeg.value + deltaDeg) % 360f
    }

    fun resetRotation() { _canvasRotationDeg.value = 0f }

    // Division workflow
    fun openDivisionModal() { _showDivisionModal.value = true }
    fun closeDivisionModal() { _showDivisionModal.value = false }

    fun enterInteractiveRoadMode() {
        val cur = _uiState.value
        val active = cur.activePlot
        if (active == null || active.points.size < 3) {
            messageHost.show("রাস্তা বসাতে প্রথমে একটি বৈধ প্লট নির্বাচন করুন", isError = true)
            return
        }
        val initialPts = _divisionState.value.manualSplitPoints.ifEmpty {
            JtsDivisionEngine.defaultRoadPolylineForPlot(active.points)
        }
        _showDivisionModal.value = false
        _activeTool.value = "interactive_road"
        _selectedVertexIndex.value = null
        _divisionState.value = _divisionState.value.copy(
            useCustomRoadCenterline = true,
            manualSplitPoints = initialPts
        )
        previewDivision()
    }

    fun exitInteractiveRoadMode() {
        _activeTool.value = "select"
        _divisionState.value = _divisionState.value.copy(
            useCustomRoadCenterline = false,
            previewDividedPlots = emptyList(),
            divisionReport = null
        )
    }

    fun setEqualSharesForAllParts() {
        val count = _divisionState.value.divisionPartsCount
        _divisionState.value = _divisionState.value.copy(
            divisionType = DivisionType.EQUAL,
            divisionPercentages = SurveyDivisionHelper.resizePercentages(count)
        )
        if (_activeTool.value == "interactive_road") previewDivision()
    }

    fun setDivisionType(type: DivisionType) {
        _divisionState.value = _divisionState.value.copy(divisionType = type)
        if (_activeTool.value == "interactive_road") previewDivision()
    }

    fun setDivisionPartsCount(count: Int) {
        val clamped = count.coerceIn(2, 24)
        val cur = _divisionState.value
        _divisionState.value = cur.copy(
            divisionPartsCount = clamped,
            divisionPercentages = SurveyDivisionHelper.resizePercentages(clamped),
            customTargetShotoks = SurveyDivisionHelper.resizeCustomShotoks(clamped, cur.customTargetShotoks)
        )
        if (_activeTool.value == "interactive_road") previewDivision()
    }

    fun adjustDivisionPartsDelta(delta: Int) =
        setDivisionPartsCount(_divisionState.value.divisionPartsCount + delta)

    fun updateDivisionPercentage(index: Int, value: Double) {
        val list = _divisionState.value.divisionPercentages.toMutableList()
        if (index in list.indices) {
            list[index] = value.coerceIn(0.0, 100.0)
            _divisionState.value = _divisionState.value.copy(divisionPercentages = list)
        }
    }

    fun updateCustomTargetShotok(index: Int, shotok: Double) {
        val list = _divisionState.value.customTargetShotoks.toMutableList()
        if (index in list.indices) {
            list[index] = shotok.coerceAtLeast(0.0)
            _divisionState.value = _divisionState.value.copy(customTargetShotoks = list)
        }
    }

    fun setSelectedRoadOption(optionId: String) {
        _divisionState.value = _divisionState.value.copy(selectedRoadOption = optionId)
    }

    fun setRoadWidthFeet(width: Float) {
        _divisionState.value = _divisionState.value.copy(roadWidthFeet = width.coerceIn(0f, 100f))
        if (_activeTool.value == "interactive_road") previewDivision()
    }

    fun adjustRoadWidthDelta(delta: Float) =
        setRoadWidthFeet(_divisionState.value.roadWidthFeet + delta)

    fun setDivisionSweepAngle(angleDeg: Float) {
        _divisionState.value = _divisionState.value.copy(divisionSweepAngleDeg = angleDeg.coerceIn(-180f, 180f))
    }

    fun addManualSplitPoint(point: PointF) {
        val cur = _divisionState.value
        _divisionState.value = cur.copy(
            useCustomRoadCenterline = true,
            manualSplitPoints = cur.manualSplitPoints + applySnapIfEnabled(point, cur.manualSplitPoints.lastOrNull())
        )
        if (_activeTool.value == "interactive_road") previewDivision()
    }

    fun insertRoadBend(index: Int, point: PointF) {
        val list = _divisionState.value.manualSplitPoints.toMutableList()
        val safeIdx = index.coerceIn(0, list.size)
        list.add(safeIdx, point)
        _divisionState.value = _divisionState.value.copy(manualSplitPoints = list)
        if (_activeTool.value == "interactive_road") previewDivision()
    }

    fun moveRoadNode(index: Int, point: PointF) {
        val list = _divisionState.value.manualSplitPoints.toMutableList()
        if (index in list.indices) {
            list[index] = point
            _divisionState.value = _divisionState.value.copy(manualSplitPoints = list)
            if (_activeTool.value == "interactive_road") previewDivision()
        }
    }

    fun resetRoadToPreset() {
        val active = _uiState.value.activePlot
        if (_activeTool.value == "interactive_road" && active != null && active.points.size >= 3) {
            _divisionState.value = _divisionState.value.copy(
                useCustomRoadCenterline = true,
                manualSplitPoints = JtsDivisionEngine.defaultRoadPolylineForPlot(active.points)
            )
            previewDivision()
        } else {
            _divisionState.value = _divisionState.value.copy(
                useCustomRoadCenterline = false,
                manualSplitPoints = emptyList()
            )
        }
    }

    fun previewDivision() {
        val cur = _uiState.value
        val active = cur.activePlot ?: return
        val res = SurveyDivisionHelper.computePreview(
            activePlot = active,
            state = _divisionState.value,
            feetPerUnit = cur.feetPerCanvasUnit,
            sqFeetPerShotok = cur.sqFeetPerShotok
        )
        _divisionState.value = _divisionState.value.copy(
            previewDividedPlots = res.plots,
            divisionReport = res.report
        )
        if (!res.report.warningMessage.isNullOrBlank()) {
            messageHost.show(res.report.warningMessage, isError = true)
        }
    }

    fun cancelDivisionPreview() {
        _divisionState.value = _divisionState.value.copy(
            previewDividedPlots = emptyList(),
            divisionReport = null
        )
    }

    fun commitDivision() {
        val cur = _uiState.value
        val active = cur.activePlot ?: return
        val divState = _divisionState.value
        val preview = divState.previewDividedPlots.ifEmpty {
            val res = SurveyDivisionHelper.computePreview(active, divState, cur.feetPerCanvasUnit, cur.sqFeetPerShotok)
            if (!res.report.warningMessage.isNullOrBlank()) {
                messageHost.show(res.report.warningMessage, isError = true)
                return
            }
            res.plots
        }
        if (preview.isEmpty()) return
        pushUndoSnapshot()
        val idx = cur.plots.indexOfFirst { it.id == active.id }.coerceAtLeast(0)
        val newPlots = cur.plots.toMutableList().apply {
            removeAt(idx)
            addAll(idx, preview)
        }
        _uiState.value = cur.copy(
            plots = newPlots,
            activePlotIndex = idx,
            selectedPlotIds = cur.selectedPlotIds - active.id,
            updatedAt = System.currentTimeMillis()
        )
        _divisionState.value = divState.copy(
            useCustomRoadCenterline = false,
            manualSplitPoints = emptyList(),
            previewDividedPlots = emptyList()
        )
        _activeTool.value = "select"
        _showDivisionModal.value = false
        messageHost.show("জমি বিভাজন সম্পন্ন (${BengaliFormatters.toBengaliDigits(preview.count { !it.isRoad })} অংশ)")
    }

    // Comparative pantograph
    fun setPathographSwipeRatio(ratio: Float) { _pantographSwipeRatio.value = ratio.coerceIn(0f, 1f) }
    fun toggleSideBySideComparison() { _sideBySideComparison.value = !_sideBySideComparison.value }
    fun updateComparativeSummary(summary: String) { _comparativeSummary.value = summary }

    // Undo / Redo
    private fun pushUndoSnapshot() {
        val cur = _uiState.value
        undoStack.addLast(UndoSnapshot(cur.plots, cur.activePlotIndex, cur.layers))
        if (undoStack.size > 50) undoStack.removeFirst()
        redoStack.clear()
        syncUndoFlags()
    }

    fun undo() {
        val prev = undoStack.removeLastOrNull() ?: return
        val cur = _uiState.value
        redoStack.addLast(UndoSnapshot(cur.plots, cur.activePlotIndex, cur.layers))
        _uiState.value = cur.copy(
            plots = prev.plots,
            activePlotIndex = prev.activePlotIndex.coerceIn(0, (prev.plots.size - 1).coerceAtLeast(0)),
            layers = prev.layers,
            selectedPlotIds = cur.selectedPlotIds.filter { id -> prev.plots.any { it.id == id } }.toSet()
        )
        _selectedVertexIndex.value = null
        syncUndoFlags()
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        val cur = _uiState.value
        undoStack.addLast(UndoSnapshot(cur.plots, cur.activePlotIndex, cur.layers))
        _uiState.value = cur.copy(
            plots = next.plots,
            activePlotIndex = next.activePlotIndex.coerceIn(0, (next.plots.size - 1).coerceAtLeast(0)),
            layers = next.layers
        )
        _selectedVertexIndex.value = null
        syncUndoFlags()
    }

    private fun syncUndoFlags() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    // Print config, undo/redo, presets, export (v1.6)
    fun updatePrintConfig(transform: (PrintConfig) -> PrintConfig) {
        _printConfig.value = transform(_printConfig.value)
    }

    fun updatePrintConfigUndoable(transform: (PrintConfig) -> PrintConfig) {
        val current = _printConfig.value
        val updated = transform(current)
        if (updated == current) return
        printUndoStack.addLast(current)
        if (printUndoStack.size > 50) printUndoStack.removeFirst()
        printRedoStack.clear()
        _printConfig.value = updated
        syncPrintUndoFlags()
    }

    fun undoPrintChange() {
        val prev = printUndoStack.removeLastOrNull() ?: return
        printRedoStack.addLast(_printConfig.value)
        _printConfig.value = prev
        syncPrintUndoFlags()
    }

    fun redoPrintChange() {
        val next = printRedoStack.removeLastOrNull() ?: return
        printUndoStack.addLast(_printConfig.value)
        _printConfig.value = next
        syncPrintUndoFlags()
    }

    private fun syncPrintUndoFlags() {
        _canUndoPrint.value = printUndoStack.isNotEmpty()
        _canRedoPrint.value = printRedoStack.isNotEmpty()
    }

    fun applyPrintLayoutPreset(preset: PrintLayoutPreset) {
        updatePrintConfigUndoable { cfg ->
            when (preset) {
                PrintLayoutPreset.STANDARD -> cfg.copy(
                    layoutPreset = preset, showHeaderAndInfo = true, showFooterAndPage = true,
                    showLegend = true, showAreaSummary = true, showWatermark = false
                )
                PrintLayoutPreset.DEMARCATION_CHITHA -> cfg.copy(
                    layoutPreset = preset, headerTitle = "ডিমার্কেশন চিঠা",
                    showHeaderAndInfo = true, showLegend = true, showAreaSummary = true, showQrCode = true
                )
                PrintLayoutPreset.CLEAN_SURVEY -> cfg.copy(
                    layoutPreset = preset, showWatermark = false, showLegend = false,
                    showBarcode = false, showHeaderAndInfo = true
                )
                PrintLayoutPreset.SHARE_PACK -> cfg.copy(
                    layoutPreset = preset, showWatermark = true, showQrCode = true, showBarcode = true
                )
            }
        }
    }

    fun persistPrintConfig(saveAsGlobalDefault: Boolean) {
        val cfg = _printConfig.value
        val projId = _uiState.value.id
        viewModelScope.launch {
            if (saveAsGlobalDefault) {
                printConfigStore.saveGlobal(cfg)
                messageHost.show("প্রিন্ট কনফিগ গ্লোবাল ডিফল্ট হিসেবে সেভ হয়েছে")
            } else {
                printConfigStore.saveForProject(projId, cfg)
                messageHost.show("প্রিন্ট কনফিগ এই প্রজেক্টের জন্য সেভ হয়েছে")
            }
        }
    }

    fun clearProjectPrintOverride() {
        val projId = _uiState.value.id
        viewModelScope.launch {
            printConfigStore.clearProjectOverride(projId)
            _printConfig.value = printConfigStore.loadEffective(projId)
            messageHost.show("প্রজেক্ট ওভাররাইড মুছে ফেলা হয়েছে — গ্লোবাল ডিফল্ট প্রযোজ্য")
        }
    }

    fun resetPrintConfig() {
        updatePrintConfigUndoable {
            PrintConfig(mouzaName = _uiState.value.mouzaName, jlNumber = _uiState.value.jlNumber)
        }
    }

    fun resetPrintFidelityToCanvas() {
        updatePrintConfigUndoable { cfg ->
            cfg.copy(
                mapContentMode = "full",
                showVertexNumbers = false,
                plotFillOpacity = 1f,
                labelCollisionAvoidance = true,
                showPerimeterLabel = true,
                showTotalAreaLabel = true,
                showSelectedAreaLabel = true,
                scaleBarUnitId = "inherit",
                northArrowSizePercent = 100,
                lineColorMode = PrintLineColorMode.PLOT_OWN
            )
        }
        messageHost.show("প্রিন্ট কাস্টমাইজ ক্যানভাস অবস্থায় রিসেট হয়েছে")
    }

    fun applyPrintStylesToCanvas() {
        val cfg = _printConfig.value
        updateCanvasSettings { canvas ->
            canvas.copy(
                grayscale = cfg.grayscale,
                mapOpacity = cfg.baseMapOpacity.coerceIn(0.05f, 1.0f),
                measurementColorHex = cfg.lineColorMode.hex ?: canvas.measurementColorHex
            )
        }
        messageHost.show("প্রিন্ট স্টাইল ক্যানভাসে প্রয়োগ হয়েছে")
    }

    fun savePrintPreset(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            repository.savePrintPreset(clean, _printConfig.value)
            messageHost.show("প্রিসেট সংরক্ষিত: $clean")
        }
    }

    fun applyPrintPreset(presetId: String) {
        viewModelScope.launch {
            val entity = repository.getPrintPresetById(presetId) ?: return@launch
            val decoded = repository.decodePrintConfig(entity.configJson) ?: return@launch
            val cur = _uiState.value
            updatePrintConfigUndoable {
                decoded.copy(mouzaName = cur.mouzaName, jlNumber = cur.jlNumber)
            }
            messageHost.show("প্রিসেট প্রয়োগ হয়েছে: ${entity.name}")
        }
    }

    fun deletePrintPreset(presetId: String) {
        viewModelScope.launch {
            repository.deletePrintPreset(presetId)
            messageHost.show("প্রিসেট মুছে ফেলা হয়েছে")
        }
    }

    fun buildSurveyReportData(ticket: ReportTicket = ReportTicket()): SurveyReportData {
        val project = _uiState.value
        return SurveyReportDataMapper.build(
            project = project,
            ticket = ticket,
            includePlotIds = project.selectedPlotIds,
            canvasSettings = project.canvasSettings,
            layers = project.layers,
            printConfig = _printConfig.value
        )
    }

    fun observePrintJobState() {
        if (printJobObserverStarted) return
        printJobObserverStarted = true
        viewModelScope.launch {
            combine(_uiState, _printConfig) { proj, _ ->
                val blocked = if (proj.feetPerCanvasUnit <= 0.0 && !proj.isCalibrated) {
                    "ম্যাপ ক্যালিব্রেট করা হয়নি — প্রিন্টের আগে স্কেল/DPI ক্যালিব্রেশন সম্পন্ন করুন"
                } else null
                val warning = if (blocked == null && proj.selectedPlotIds.isEmpty()) {
                    "কোনো প্লট নির্বাচন করা হয়নি — প্রিভিউতে নির্দেশক পাতা দেখাবে"
                } else null
                blocked to warning
            }.collect { (blocked, warning) ->
                _printJobState.value = _printJobState.value.copy(
                    isReady = blocked == null,
                    blockedReasonBn = blocked,
                    warningBn = warning
                )
            }
        }
    }

    fun exportSurveyPadPdf(
        context: Context,
        ticket: ReportTicket = ReportTicket(),
        template: ReportTemplate = ReportTemplate.STANDARD,
        onComplete: ((File?) -> Unit)? = null
    ) {
        val state = _printJobState.value
        if (!state.isReady) {
            messageHost.show(state.blockedReasonBn ?: "প্রিন্ট প্রস্তুত নয়", isError = true)
            onComplete?.invoke(null)
            return
        }
        val reportData = buildSurveyReportData(ticket)
        val cfg = _printConfig.value
        exportCancelFlag = AtomicBoolean(false)
        exportJob = viewModelScope.launch {
            _printJobState.value = _printJobState.value.copy(
                isExporting = true,
                exportProgressPage = 1,
                exportTotalPages = ReportPdfExporter.buildPagePlan(reportData, cfg.individualPlotPages).size,
                lastExportMessageBn = null
            )
            val result = withContext(Dispatchers.IO) {
                val typeface = PdfAndPrintHelper.resolveBengaliTypeface(context)
                val qr = if (reportData.ticket.showQr) BarcodeAndQrHelper.generateQrBitmap(reportData.ticket.qrLink) else null
                val sig = PdfAndPrintHelper.decodeUriToBitmap(context, reportData.ticket.signatureUri, 512)
                val logo = PdfAndPrintHelper.decodeUriToBitmap(context, reportData.ticket.logoUri, 512)
                val inset = _pdfBitmap.value
                ReportPdfExporter.saveSurveyPadPdfToDownloads(
                    context = context,
                    data = reportData,
                    template = template,
                    typeface = typeface,
                    qrBitmap = qr,
                    logoBitmap = logo,
                    signatureBitmap = sig,
                    insetBitmap = inset,
                    sections = cfg.sectionLayout,
                    printConfig = cfg,
                    isCancelled = exportCancelFlag,
                    onProgress = { page, total ->
                        _printJobState.value = _printJobState.value.copy(
                            exportProgressPage = page,
                            exportTotalPages = total
                        )
                    }
                )
            }
            result.onSuccess { (file, _) ->
                repository.addExportRecord(
                    ExportRecordEntity(
                        id = "exp_${System.currentTimeMillis()}",
                        projectId = _uiState.value.id,
                        format = "PDF",
                        fileName = file.name,
                        uriOrPath = file.absolutePath,
                        plotCount = reportData.plots.size,
                        createdAt = System.currentTimeMillis()
                    )
                )
                _printJobState.value = _printJobState.value.copy(
                    isExporting = false,
                    lastExportMessageBn = "সার্ভে প্যাড PDF ডাউনলোডে সেভ হয়েছে"
                )
                messageHost.show("সার্ভে প্যাড PDF ডাউনলোডে সেভ হয়েছে")
                onComplete?.invoke(file)
            }.onFailure { err ->
                val msg = ReportPdfExporter.mapExportError(err)
                _printJobState.value = _printJobState.value.copy(
                    isExporting = false,
                    lastExportMessageBn = msg
                )
                messageHost.show(msg, isError = true)
                onComplete?.invoke(null)
            }
        }
    }

    fun cancelPrintExport() {
        exportCancelFlag.set(true)
        exportJob?.cancel()
        _printJobState.value = _printJobState.value.copy(
            isExporting = false,
            lastExportMessageBn = ReportPdfExporter.CANCELLED_MESSAGE_BN
        )
        messageHost.show(ReportPdfExporter.CANCELLED_MESSAGE_BN)
    }

    fun clearPrintJobMessage() {
        _printJobState.value = _printJobState.value.copy(lastExportMessageBn = null)
    }

    fun exportAndSharePdf(context: Context, ticket: ReportTicket = ReportTicket(), template: ReportTemplate = ReportTemplate.STANDARD) {
        exportSurveyPadPdf(context, ticket, template) { file ->
            if (file != null) {
                PdfAndPrintHelper.shareFile(context, file, "application/pdf")
                    .onSuccess { messageHost.show("শেয়ার শিট খোলা হয়েছে") }
                    .onFailure { messageHost.show("শেয়ার করা যায়নি", isError = true) }
            }
        }
    }

    fun printSurveyA4(context: Context, ticket: ReportTicket = ReportTicket(), template: ReportTemplate = ReportTemplate.STANDARD) {
        exportSurveyPadPdf(context, ticket, template) { file ->
            if (file != null) {
                PdfAndPrintHelper.printPdfFile(context, file, _printConfig.value.headerTitle)
                    .onSuccess { messageHost.show("প্রিন্ট সার্ভিস চালু হয়েছে") }
                    .onFailure { messageHost.show("প্রিন্ট সার্ভিস চালু করা যায়নি", isError = true) }
            }
        }
    }

    fun exportProjectInFormat(context: Context, format: ExportFormat, shareAfter: Boolean = false) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                ExportCenterHelper.exportProject(context, _uiState.value, format) {
                    val data = buildSurveyReportData()
                    BengaliSurveyReportRenderer.renderPreviewBitmap(
                        data = data,
                        template = ReportTemplate.STANDARD,
                        typeface = PdfAndPrintHelper.resolveBengaliTypeface(context),
                        insetBitmap = _pdfBitmap.value,
                        pageLayout = PageLayout.from(_printConfig.value)
                    )
                }
            }
            result.onSuccess { file ->
                PdfAndPrintHelper.copyFileToDownloads(context, file, file.name, format.mimeType)
                if (shareAfter) {
                    PdfAndPrintHelper.shareFile(context, file, format.mimeType)
                }
                messageHost.show("${format.labelBn} সংরক্ষিত হয়েছে")
            }.onFailure {
                messageHost.show("সংরক্ষণ ব্যর্থ হয়েছে", isError = true)
            }
        }
    }

    fun clearStatusMessage() { messageHost.clear() }

    private fun applySnapIfEnabled(point: PointF, prev: PointF?): PointF {
        var p = point
        if (_gridSnapEnabled.value) {
            val spacing = _uiState.value.canvasSettings.effectiveGridSpacing()
            p = PointF(
                kotlin.math.round(p.x / spacing) * spacing,
                kotlin.math.round(p.y / spacing) * spacing
            )
        }
        if (_orthoSnapEnabled.value && prev != null) {
            val dx = kotlin.math.abs(p.x - prev.x)
            val dy = kotlin.math.abs(p.y - prev.y)
            p = if (dx >= dy) PointF(p.x, prev.y) else PointF(prev.x, p.y)
        }
        if (_snapEnabled.value) {
            for (plot in _uiState.value.plots) {
                for (v in plot.points) {
                    if (hypot((p.x - v.x).toDouble(), (p.y - v.y).toDouble()) < 10.0) {
                        return v
                    }
                }
            }
        }
        return p
    }

    class Factory(
        private val application: Application,
        private val databaseOverride: LandProDatabase? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LandSurveyViewModel(application, databaseOverride) as T
        }
    }
}
