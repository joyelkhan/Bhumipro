package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ConversionHistoryEntity
import com.example.data.local.ExportRecordEntity
import com.example.data.local.LandProDatabase
import com.example.data.local.MouzaLibraryEntity
import com.example.data.local.PinnedToolEntity
import com.example.data.model.MouzaMapRecord
import com.example.data.model.SurveyProject
import com.example.data.repository.LandProRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val db = LandProDatabase.getInstance(application)
    private val repository = LandProRepository(db)

    val projects: StateFlow<List<SurveyProject>> = repository.observeAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pinnedTools: StateFlow<List<PinnedToolEntity>> = repository.observePinnedTools()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mouzaLibrary: StateFlow<List<MouzaMapRecord>> = repository.observeMouzaLibrary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentExports: StateFlow<List<ExportRecordEntity>> = repository.observeAllRecentExports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val conversionHistory: StateFlow<List<ConversionHistoryEntity>> = repository.observeConversionHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.ensureDefaultSeeded()
        }
    }

    fun refreshCatalog() {
        viewModelScope.launch {
            repository.ensureDefaultSeeded()
        }
    }

    fun createNewProject(
        title: String,
        mouzaName: String,
        district: String = "চট্টগ্রাম",
        upazila: String = "রাউজান",
        jlNumber: String = "",
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            val id = "proj_${System.currentTimeMillis()}"
            val project = SurveyProject(
                id = id,
                title = title.ifBlank { "নতুন জরিপ প্রজেক্ট" },
                mouzaName = mouzaName.ifBlank { "নতুন মৌজা" },
                district = district,
                upazila = upazila,
                jlNumber = jlNumber,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveProject(project)
            onCreated(id)
        }
    }

    fun deleteProject(id: String) {
        viewModelScope.launch {
            repository.deleteProjectSoft(id)
        }
    }

    fun toggleMouzaFavorite(mapKey: String, favorite: Boolean) {
        viewModelScope.launch {
            repository.toggleMouzaFavorite(mapKey, favorite)
        }
    }

    fun openMouzaInWorkspace(record: MouzaMapRecord, onReady: (String) -> Unit) {
        viewModelScope.launch {
            repository.touchMouzaOpened(record.mapKey)
            val projectId = "mouza_${record.mapKey}"
            val existing = repository.getProjectById(projectId)
            if (existing == null) {
                repository.saveProject(
                    SurveyProject(
                        id = projectId,
                        title = "${record.mouzaNameBn} (সিট ${record.sheetNumber})",
                        mouzaName = record.mouzaNameBn,
                        district = record.districtNameBn,
                        upazila = record.upazilaNameBn,
                        jlNumber = record.jlNumber,
                        scaleId = record.scaleId,
                        scanDpi = record.scanDpi,
                        isCalibrated = true,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            onReady(projectId)
        }
    }

    fun toggleToolPinned(id: String, pinned: Boolean) {
        viewModelScope.launch {
            repository.setToolPinned(id, pinned)
        }
    }

    fun createProjectFromUri(
        uriString: String,
        titleHint: String = "আমদানিকৃত মৌজা ম্যাপ",
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            val id = "proj_${System.currentTimeMillis()}"
            val project = SurveyProject(
                id = id,
                title = titleHint,
                mouzaName = titleHint,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveProject(project)
            repository.saveImportedAsset(
                com.example.data.local.ImportedAssetEntity(
                    id = "asset_${System.currentTimeMillis()}",
                    projectId = id,
                    assetType = "RASTER_MAP",
                    uriString = uriString,
                    createdAt = System.currentTimeMillis()
                )
            )
            onCreated(id)
        }
    }
}
