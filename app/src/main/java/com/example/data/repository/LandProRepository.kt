package com.example.data.repository

import com.example.data.local.BoundaryDocumentEntity
import com.example.data.local.ConversionHistoryEntity
import com.example.data.local.DraftProjectEntity
import com.example.data.local.ExportRecordEntity
import com.example.data.local.ImportedAssetEntity
import com.example.data.local.LandProDatabase
import com.example.data.local.MouzaLibraryEntity
import com.example.data.local.PinnedToolEntity
import com.example.data.local.PrintPresetEntity
import com.example.data.local.ProjectSnapshotEntity
import com.example.data.model.CanvasDisplaySettings
import com.example.data.model.GisLayerItem
import com.example.data.model.LandPlot
import com.example.data.model.MoshiProvider
import com.example.data.model.MouzaMapRecord
import com.example.data.model.PrintConfig
import com.example.data.model.SurveyProject
import com.example.data.model.defaultGisLayers
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LandProRepository(private val db: LandProDatabase) {

    private val plotListType = Types.newParameterizedType(List::class.java, LandPlot::class.java)
    private val plotListAdapter = MoshiProvider.moshi.adapter<List<LandPlot>>(plotListType)

    private val layerListType = Types.newParameterizedType(List::class.java, GisLayerItem::class.java)
    private val layerListAdapter = MoshiProvider.moshi.adapter<List<GisLayerItem>>(layerListType)

    private val stringListType = Types.newParameterizedType(List::class.java, String::class.java)
    private val stringListAdapter = MoshiProvider.moshi.adapter<List<String>>(stringListType)

    private val canvasSettingsAdapter = MoshiProvider.moshi.adapter(CanvasDisplaySettings::class.java)
    private val printConfigAdapter = MoshiProvider.moshi.adapter(PrintConfig::class.java)

    fun observeAllProjects(): Flow<List<SurveyProject>> =
        db.draftProjectDao().observeAll().map { list -> list.map(::toDomain) }

    suspend fun getAllProjects(): List<SurveyProject> =
        db.draftProjectDao().list().map(::toDomain)

    suspend fun getProjectById(id: String): SurveyProject? {
        val entity = db.draftProjectDao().getById(id) ?: return null
        return toDomain(entity)
    }

    suspend fun ensureDefaultSeeded() {
        LandProDatabase.seedDefaultsIfEmpty(db)
    }

    suspend fun saveProject(project: SurveyProject) {
        val now = if (project.updatedAt > 0L) project.updatedAt else System.currentTimeMillis()
        val updated = project.copy(updatedAt = now)
        db.draftProjectDao().upsert(toEntity(updated))
    }

    suspend fun deleteProjectSoft(id: String) {
        db.draftProjectDao().markDeleted(id, System.currentTimeMillis())
    }

    fun observePinnedTools(): Flow<List<PinnedToolEntity>> = db.pinnedToolDao().observeAll()

    suspend fun setToolPinned(id: String, pinned: Boolean) {
        db.pinnedToolDao().setPinned(id, pinned)
    }

    fun observeImportedAssets(projectId: String): Flow<List<ImportedAssetEntity>> =
        db.importedAssetDao().observeForProject(projectId)

    suspend fun getLatestAssetForProject(projectId: String): ImportedAssetEntity? =
        db.importedAssetDao().getLatestForProject(projectId)

    suspend fun saveImportedAsset(asset: ImportedAssetEntity) {
        db.importedAssetDao().upsert(asset)
    }

    fun observeSnapshots(projectId: String): Flow<List<ProjectSnapshotEntity>> =
        db.projectSnapshotDao().observeForProject(projectId)

    suspend fun createSnapshot(project: SurveyProject, label: String): ProjectSnapshotEntity {
        val snap = ProjectSnapshotEntity(
            id = "snap_${System.currentTimeMillis()}",
            projectId = project.id,
            label = label,
            plotsJson = encodePlots(project.plots),
            layersJson = encodeLayers(project.layers),
            feetPerCanvasUnit = project.feetPerCanvasUnit,
            sqFeetPerShotok = project.sqFeetPerShotok,
            isCalibrated = if (project.isCalibrated) 1 else 0,
            createdAt = System.currentTimeMillis()
        )
        db.projectSnapshotDao().upsert(snap)
        return snap
    }

    suspend fun getSnapshotById(snapshotId: String): ProjectSnapshotEntity? =
        db.projectSnapshotDao().getById(snapshotId)

    suspend fun deleteSnapshot(snapshotId: String) {
        db.projectSnapshotDao().deleteById(snapshotId)
    }

    suspend fun saveBoundaryDocument(doc: BoundaryDocumentEntity) {
        db.boundaryDocumentDao().upsert(doc)
    }

    fun observeConversionHistory(): Flow<List<ConversionHistoryEntity>> =
        db.conversionHistoryDao().observeRecent()

    suspend fun addConversionHistory(entry: ConversionHistoryEntity) {
        db.conversionHistoryDao().upsert(entry)
    }

    suspend fun clearConversionHistory() {
        db.conversionHistoryDao().clearAll()
    }

    fun observeExportRecords(projectId: String): Flow<List<ExportRecordEntity>> =
        db.exportRecordDao().observeForProject(projectId)

    fun observeAllRecentExports(): Flow<List<ExportRecordEntity>> =
        db.exportRecordDao().observeAllRecent()

    suspend fun addExportRecord(record: ExportRecordEntity) {
        db.exportRecordDao().upsert(record)
    }

    // Mouza Library
    fun observeMouzaLibrary(): Flow<List<MouzaMapRecord>> =
        db.mouzaLibraryDao().observeAll().map { items -> items.map(::toMouzaRecord) }

    suspend fun getMouzaByKey(key: String): MouzaMapRecord? =
        db.mouzaLibraryDao().getByKey(key)?.let(::toMouzaRecord)

    suspend fun toggleMouzaFavorite(key: String, favorite: Boolean) {
        db.mouzaLibraryDao().setFavorite(key, favorite)
    }

    suspend fun touchMouzaOpened(key: String) {
        db.mouzaLibraryDao().touchOpened(key, System.currentTimeMillis())
    }

    // Print Presets (v7)
    fun observePrintPresets(): Flow<List<PrintPresetEntity>> = db.printPresetDao().observeAll()

    suspend fun getAllPrintPresets(): List<PrintPresetEntity> = db.printPresetDao().getAll()

    suspend fun getPrintPresetById(id: String): PrintPresetEntity? = db.printPresetDao().getById(id)

    suspend fun savePrintPreset(name: String, config: PrintConfig, existingId: String? = null): PrintPresetEntity {
        val now = System.currentTimeMillis()
        val id = existingId ?: "preset_${now}"
        val entity = PrintPresetEntity(
            id = id,
            name = name.trim().ifEmpty { "কাস্টম প্রিসেট" },
            configJson = printConfigAdapter.toJson(config),
            createdAt = now,
            updatedAt = now
        )
        db.printPresetDao().upsert(entity)
        return entity
    }

    fun decodePrintConfig(json: String): PrintConfig? =
        runCatching { printConfigAdapter.fromJson(json) }.getOrNull()

    suspend fun deletePrintPreset(id: String) {
        db.printPresetDao().deleteById(id)
    }

    // Encoding / Decoding helpers
    fun encodePlots(plots: List<LandPlot>): String = plotListAdapter.toJson(plots)

    fun decodePlots(json: String): List<LandPlot> {
        if (json.isBlank()) return emptyList()
        return runCatching { plotListAdapter.fromJson(json) }.getOrNull().orEmpty()
    }

    fun encodeLayers(layers: List<GisLayerItem>): String = layerListAdapter.toJson(layers)

    fun decodeLayers(json: String): List<GisLayerItem> {
        if (json.isBlank() || json == "[]") return defaultGisLayers()
        val parsed = runCatching { layerListAdapter.fromJson(json) }.getOrNull()
        return if (parsed.isNullOrEmpty()) defaultGisLayers() else parsed
    }

    fun encodeSelectedPlotIds(ids: Set<String>): String = stringListAdapter.toJson(ids.toList())

    fun decodeSelectedPlotIds(json: String): Set<String> {
        if (json.isBlank()) return emptySet()
        return runCatching { stringListAdapter.fromJson(json) }.getOrNull()?.toSet().orEmpty()
    }

    fun encodeCanvasSettings(settings: CanvasDisplaySettings): String =
        canvasSettingsAdapter.toJson(settings)

    fun decodeCanvasSettings(json: String): CanvasDisplaySettings {
        if (json.isBlank()) return CanvasDisplaySettings()
        return runCatching { canvasSettingsAdapter.fromJson(json) }.getOrNull()
            ?: CanvasDisplaySettings()
    }

    fun toDomain(entity: DraftProjectEntity): SurveyProject {
        val plots = decodePlots(entity.plotsJson)
        val layers = decodeLayers(entity.layersJson)
        val selectedIds = decodeSelectedPlotIds(entity.selectedPlotIdsJson)
            .filter { id -> plots.any { it.id == id } }
            .toSet()
        val canvasSettings = decodeCanvasSettings(entity.canvasSettingsJson)
        return SurveyProject(
            id = entity.id,
            title = entity.title,
            mouzaName = entity.mouzaName,
            scale = entity.scaleText,
            scaleId = entity.scaleId,
            scanDpi = entity.scanDpi,
            sqFeetPerShotok = entity.sqFeetPerShotok.coerceIn(100.0, 2000.0),
            feetPerCanvasUnit = entity.feetPerCanvasUnit,
            plots = plots,
            activePlotIndex = entity.activePlotIndex.coerceIn(0, (plots.size - 1).coerceAtLeast(0)),
            selectedPlotIds = selectedIds,
            ownerName = entity.ownerName,
            district = entity.district,
            upazila = entity.upazila,
            jlNumber = entity.jlNumber,
            khatianNo = entity.khatianNo,
            dagNo = entity.dagNo,
            surveyDate = entity.surveyDate,
            notes = entity.notes,
            referenceCode = entity.referenceCode,
            calibrationRmsePx = entity.calibrationRmsePx,
            isCalibrated = entity.isCalibrated != 0,
            calibrationMethod = entity.calibrationMethod.ifBlank { "PRESET_SCALE_DPI" },
            calibrationPixelDistance = entity.calibrationPixelDistance,
            calibrationGroundFeet = entity.calibrationGroundFeet,
            calibrationUpdatedAt = entity.calibrationUpdatedAt,
            layers = layers,
            canvasSettings = canvasSettings,
            isFavorite = entity.isFavorite != 0,
            isPinned = entity.isPinned != 0,
            isArchived = entity.isArchived != 0,
            status = entity.status,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(project: SurveyProject): DraftProjectEntity = DraftProjectEntity(
        id = project.id,
        title = project.title,
        mouzaName = project.mouzaName,
        scaleText = project.scale,
        plotsJson = encodePlots(project.plots),
        activePlotIndex = project.activePlotIndex,
        totalAreaShotangsho = project.totalLandAreaShotangsho,
        updatedAt = project.updatedAt,
        scaleId = project.scaleId,
        scanDpi = project.scanDpi,
        feetPerCanvasUnit = project.feetPerCanvasUnit,
        sqFeetPerShotok = project.sqFeetPerShotok,
        ownerName = project.ownerName,
        district = project.district,
        upazila = project.upazila,
        jlNumber = project.jlNumber,
        khatianNo = project.khatianNo,
        dagNo = project.dagNo,
        surveyDate = project.surveyDate,
        notes = project.notes,
        referenceCode = project.referenceCode,
        calibrationRmsePx = project.calibrationRmsePx,
        isCalibrated = if (project.isCalibrated) 1 else 0,
        calibrationMethod = project.calibrationMethod,
        calibrationPixelDistance = project.calibrationPixelDistance,
        calibrationGroundFeet = project.calibrationGroundFeet,
        calibrationUpdatedAt = project.calibrationUpdatedAt,
        layersJson = encodeLayers(project.layers),
        isFavorite = if (project.isFavorite) 1 else 0,
        isPinned = if (project.isPinned) 1 else 0,
        isArchived = if (project.isArchived) 1 else 0,
        status = project.status,
        selectedPlotIdsJson = encodeSelectedPlotIds(project.selectedPlotIds),
        canvasSettingsJson = encodeCanvasSettings(project.canvasSettings)
    )

    private fun toMouzaRecord(e: MouzaLibraryEntity): MouzaMapRecord = MouzaMapRecord(
        mapKey = e.mapKey,
        divisionId = e.divisionId,
        divisionNameBn = e.divisionNameBn,
        districtId = e.districtId,
        districtNameBn = e.districtNameBn,
        upazilaNameBn = e.upazilaNameBn,
        mouzaNameBn = e.mouzaNameBn,
        jlNumber = e.jlNumber,
        sheetNumber = e.sheetNumber,
        surveyType = e.surveyType,
        scaleId = e.scaleId,
        scanDpi = e.scanDpi,
        isFavorite = e.isFavorite,
        isOfflineSaved = e.isOfflineSaved,
        lastOpenedAt = e.lastOpenedAt
    )
}
