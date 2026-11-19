package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "draft_projects")
data class DraftProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val mouzaName: String,
    val scaleText: String,
    val plotsJson: String = "[]",
    val activePlotIndex: Int = 0,
    val totalAreaShotangsho: Double = 0.0,
    val updatedAt: Long = 0L,
    // v3 columns
    val scaleId: String = "scale_16",
    val scanDpi: Int = 300,
    val feetPerCanvasUnit: Double = 0.55,
    val sqFeetPerShotok: Double = 435.6,
    val ownerName: String = "",
    val district: String = "চট্টগ্রাম",
    val upazila: String = "রাউজান",
    val jlNumber: String = "৩৫",
    val khatianNo: String = "",
    val dagNo: String = "",
    val surveyDate: String = "",
    val notes: String = "",
    val referenceCode: String = "",
    // v4 calibration provenance
    val calibrationRmsePx: Double = 0.0,
    val isCalibrated: Int = 0,
    val calibrationMethod: String = "PRESET_SCALE_DPI",
    val calibrationPixelDistance: Double = 0.0,
    val calibrationGroundFeet: Double = 0.0,
    val calibrationUpdatedAt: Long = 0L,
    val layersJson: String = "[]",
    val isFavorite: Int = 0,
    val isPinned: Int = 0,
    val isArchived: Int = 0,
    val isDeleted: Int = 0,
    val deletedAt: Long = 0L,
    val status: String = "সক্রিয়",
    // v5 columns
    val selectedPlotIdsJson: String = "",
    val canvasSettingsJson: String = ""
)

@Entity(tableName = "pinned_tools")
data class PinnedToolEntity(
    @PrimaryKey val id: String,
    val titleBn: String,
    val subtitleBn: String,
    val route: String,
    val iconKey: String,
    val accentHex: String,
    val containerHex: String,
    val sortOrder: Int = 0,
    val isPinned: Boolean = true
)

@Entity(
    tableName = "imported_assets",
    foreignKeys = [
        ForeignKey(
            entity = DraftProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"])]
)
data class ImportedAssetEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val assetType: String = "RASTER_MAP",
    val uriString: String,
    val fileName: String = "",
    val detectedDpi: Int = 0,
    val pageIndex: Int = 0,
    val createdAt: Long = 0L
)

@Entity(
    tableName = "project_snapshots",
    foreignKeys = [
        ForeignKey(
            entity = DraftProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId", "createdAt"])]
)
data class ProjectSnapshotEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val label: String,
    val plotsJson: String,
    val layersJson: String = "[]",
    val feetPerCanvasUnit: Double = 0.55,
    val sqFeetPerShotok: Double = 435.6,
    val isCalibrated: Int = 0,
    val createdAt: Long = 0L
)

@Entity(
    tableName = "boundary_documents",
    foreignKeys = [
        ForeignKey(
            entity = DraftProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"])]
)
data class BoundaryDocumentEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val title: String,
    val format: String,
    val filePath: String,
    val createdAt: Long = 0L
)

@Entity(tableName = "conversion_history")
data class ConversionHistoryEntity(
    @PrimaryKey val id: String,
    val sourceValue: String,
    val sourceUnit: String,
    val resultSummary: String,
    val shotokFactor: Double = 435.6,
    val createdAt: Long = 0L
)

@Entity(
    tableName = "export_records",
    indices = [Index(value = ["projectId", "createdAt"])]
)
data class ExportRecordEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val format: String,
    val fileName: String,
    val uriOrPath: String,
    val plotCount: Int = 0,
    val createdAt: Long = 0L
)

@Entity(
    tableName = "mouza_library_items",
    indices = [Index(value = ["divisionId", "districtId"])]
)
data class MouzaLibraryEntity(
    @PrimaryKey val mapKey: String,
    val divisionId: String,
    val divisionNameBn: String,
    val districtId: String,
    val districtNameBn: String,
    val upazilaNameBn: String,
    val mouzaNameBn: String,
    val jlNumber: String,
    val sheetNumber: String = "১",
    val surveyType: String = "R.S",
    val scaleId: String = "scale_16",
    val scanDpi: Int = 300,
    val isFavorite: Boolean = false,
    val isOfflineSaved: Boolean = true,
    val lastOpenedAt: Long = 0L
)

@Entity(tableName = "print_presets")
data class PrintPresetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val configJson: String,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
