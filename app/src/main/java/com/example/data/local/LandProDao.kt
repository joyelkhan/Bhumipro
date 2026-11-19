package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftProjectDao {
    @Query("SELECT * FROM draft_projects WHERE isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun observeAll(): Flow<List<DraftProjectEntity>>

    @Query("SELECT * FROM draft_projects WHERE isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    suspend fun list(): List<DraftProjectEntity>

    @Query("SELECT * FROM draft_projects WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DraftProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DraftProjectEntity)

    @Query("UPDATE draft_projects SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun markDeleted(id: String, deletedAt: Long)

    @Query("DELETE FROM draft_projects WHERE id = :id")
    suspend fun purge(id: String)

    @Query("SELECT COUNT(*) FROM draft_projects")
    suspend fun count(): Int
}

@Dao
interface PinnedToolDao {
    @Query("SELECT * FROM pinned_tools ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<PinnedToolEntity>>

    @Query("SELECT * FROM pinned_tools ORDER BY sortOrder ASC")
    suspend fun list(): List<PinnedToolEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tools: List<PinnedToolEntity>)

    @Query("UPDATE pinned_tools SET isPinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Boolean)
}

@Dao
interface ImportedAssetDao {
    @Query("SELECT * FROM imported_assets WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun observeForProject(projectId: String): Flow<List<ImportedAssetEntity>>

    @Query("SELECT * FROM imported_assets WHERE projectId = :projectId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestForProject(projectId: String): ImportedAssetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(asset: ImportedAssetEntity)

    @Query("DELETE FROM imported_assets WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface ProjectSnapshotDao {
    @Query("SELECT * FROM project_snapshots WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun observeForProject(projectId: String): Flow<List<ProjectSnapshotEntity>>

    @Query("SELECT * FROM project_snapshots WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ProjectSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(snapshot: ProjectSnapshotEntity)

    @Query("DELETE FROM project_snapshots WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface BoundaryDocumentDao {
    @Query("SELECT * FROM boundary_documents WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun observeForProject(projectId: String): Flow<List<BoundaryDocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(doc: BoundaryDocumentEntity)
}

@Dao
interface ConversionHistoryDao {
    @Query("SELECT * FROM conversion_history ORDER BY createdAt DESC LIMIT 50")
    fun observeRecent(): Flow<List<ConversionHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: ConversionHistoryEntity)

    @Query("DELETE FROM conversion_history")
    suspend fun clearAll()
}

@Dao
interface ExportRecordDao {
    @Query("SELECT * FROM export_records WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun observeForProject(projectId: String): Flow<List<ExportRecordEntity>>

    @Query("SELECT * FROM export_records ORDER BY createdAt DESC LIMIT 50")
    fun observeAllRecent(): Flow<List<ExportRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: ExportRecordEntity)
}

@Dao
interface MouzaLibraryDao {
    @Query("SELECT * FROM mouza_library_items ORDER BY lastOpenedAt DESC, mouzaNameBn ASC")
    fun observeAll(): Flow<List<MouzaLibraryEntity>>

    @Query("SELECT * FROM mouza_library_items WHERE mapKey = :mapKey LIMIT 1")
    suspend fun getByKey(mapKey: String): MouzaLibraryEntity?

    @Query("SELECT * FROM mouza_library_items WHERE isFavorite = 1 ORDER BY mouzaNameBn ASC")
    fun observeFavorites(): Flow<List<MouzaLibraryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: MouzaLibraryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<MouzaLibraryEntity>)

    @Query("UPDATE mouza_library_items SET isFavorite = :favorite WHERE mapKey = :mapKey")
    suspend fun setFavorite(mapKey: String, favorite: Boolean)

    @Query("UPDATE mouza_library_items SET isOfflineSaved = :saved WHERE mapKey = :mapKey")
    suspend fun setOfflineSaved(mapKey: String, saved: Boolean)

    @Query("UPDATE mouza_library_items SET lastOpenedAt = :openedAt WHERE mapKey = :mapKey")
    suspend fun touchOpened(mapKey: String, openedAt: Long)

    @Query("SELECT COUNT(*) FROM mouza_library_items WHERE isOfflineSaved = 1")
    suspend fun offlineSavedCount(): Int
}

@Dao
interface PrintPresetDao {
    @Query("SELECT * FROM print_presets ORDER BY updatedAt DESC, name ASC")
    fun observeAll(): Flow<List<PrintPresetEntity>>

    @Query("SELECT * FROM print_presets ORDER BY updatedAt DESC, name ASC")
    suspend fun getAll(): List<PrintPresetEntity>

    @Query("SELECT * FROM print_presets WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PrintPresetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(preset: PrintPresetEntity)

    @Query("DELETE FROM print_presets WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM print_presets")
    suspend fun count(): Int
}
