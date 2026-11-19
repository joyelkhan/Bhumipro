package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.BengaliFormatters
import com.example.ui.theme.BhumiAmber
import com.example.ui.theme.BhumiEmerald
import com.example.ui.viewmodel.DashboardViewModel

private data class BdDivisionEntry(val id: String, val labelBn: String)

private val bangladeshDivisions = listOf(
    BdDivisionEntry("khl", "খুলনা বিভাগ"),
    BdDivisionEntry("ctg", "চট্টগ্রাম বিভাগ"),
    BdDivisionEntry("dhk", "ঢাকা বিভাগ"),
    BdDivisionEntry("bar", "বরিশাল বিভাগ"),
    BdDivisionEntry("mym", "ময়মনসিংহ বিভাগ"),
    BdDivisionEntry("rng", "রংপুর বিভাগ"),
    BdDivisionEntry("raj", "রাজশাহী বিভাগ"),
    BdDivisionEntry("syl", "সিলেট বিভাগ")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MouzaMapLibraryScreen(
    dashboardViewModel: DashboardViewModel,
    onOpenProjectInWorkspace: (String) -> Unit,
    onBack: () -> Unit
) {
    val items by dashboardViewModel.mouzaLibrary.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var favoritesOnly by remember { mutableStateOf(false) }
    var selectedDivisionId by remember { mutableStateOf<String?>(null) }
    var selectedDistrictName by remember { mutableStateOf<String?>(null) }
    var selectedUpazilaName by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = selectedDivisionId != null || selectedDistrictName != null || selectedUpazilaName != null) {
        when {
            selectedUpazilaName != null -> selectedUpazilaName = null
            selectedDistrictName != null -> selectedDistrictName = null
            selectedDivisionId != null -> selectedDivisionId = null
        }
    }

    val filtered = remember(items, query, favoritesOnly, selectedDivisionId, selectedDistrictName, selectedUpazilaName) {
        items.filter { rec ->
            val matchDiv = selectedDivisionId == null || rec.divisionId == selectedDivisionId
            val matchDist = selectedDistrictName == null || rec.districtNameBn == selectedDistrictName
            val matchUpz = selectedUpazilaName == null || rec.upazilaNameBn == selectedUpazilaName
            val matchesFav = !favoritesOnly || rec.isFavorite
            val matchesQuery = query.isBlank() ||
                rec.mouzaNameBn.contains(query, ignoreCase = true) ||
                rec.upazilaNameBn.contains(query, ignoreCase = true) ||
                rec.districtNameBn.contains(query, ignoreCase = true) ||
                rec.divisionNameBn.contains(query, ignoreCase = true) ||
                rec.jlNumber.contains(query, ignoreCase = true)
            matchDiv && matchDist && matchUpz && matchesFav && matchesQuery
        }
    }

    val isSearchingOrFav = query.isNotBlank() || favoritesOnly
    val browseLevel = when {
        isSearchingOrFav -> 3
        selectedDivisionId == null -> 0 // Divisions
        selectedDistrictName == null -> 1 // Districts
        selectedUpazilaName == null -> 2 // Upazilas
        else -> 3 // Mouzas
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        when {
                            selectedUpazilaName != null -> selectedUpazilaName = null
                            selectedDistrictName != null -> selectedDistrictName = null
                            selectedDivisionId != null -> selectedDivisionId = null
                            else -> onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                    }
                },
                title = {
                    val divTitle = bangladeshDivisions.firstOrNull { it.id == selectedDivisionId }?.labelBn
                    val title = selectedUpazilaName ?: selectedDistrictName ?: divTitle ?: "সকল মৌজা ম্যাপ"
                    Text(title, fontWeight = FontWeight.Bold)
                },
                actions = {
                    IconButton(onClick = { dashboardViewModel.refreshCatalog() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "রিফ্রেশ")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("ম্যাপের নাম খুঁজুন...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mouza_search_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val divLabel = bangladeshDivisions.firstOrNull { it.id == selectedDivisionId }?.labelBn
                val breadcrumb = when (browseLevel) {
                    0 -> "সমগ্র বাংলাদেশের মৌজা ম্যাপ"
                    1 -> "${divLabel.orEmpty()} • জেলা নির্বাচন করুন"
                    2 -> "${selectedDistrictName.orEmpty()} • উপজেলা নির্বাচন করুন"
                    else -> "মৌজা ম্যাপ তালিকা (${BengaliFormatters.toBengaliDigits(filtered.size)})"
                }
                Text(
                    text = breadcrumb,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FilterChip(
                    selected = favoritesOnly,
                    onClick = { favoritesOnly = !favoritesOnly },
                    label = { Text("প্রিয় তালিকা") }
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (browseLevel) {
                    0 -> {
                        items(bangladeshDivisions, key = { it.id }) { div ->
                            val count = items.count { it.divisionId == div.id }
                            FolderDrillRow(
                                title = div.labelBn,
                                subtitle = if (count > 0) "অফলাইন ম্যাপ: ${BengaliFormatters.toBengaliDigits(count)} টি" else null,
                                onClick = { selectedDivisionId = div.id },
                                modifier = Modifier.testTag("division_row_${div.id}")
                            )
                        }
                    }
                    1 -> {
                        val districts = filtered.map { it.districtNameBn }.distinct()
                        items(districts, key = { it }) { distName ->
                            val count = filtered.count { it.districtNameBn == distName }
                            FolderDrillRow(
                                title = "$distName জেলা",
                                subtitle = "ম্যাপ: ${BengaliFormatters.toBengaliDigits(count)} টি",
                                onClick = { selectedDistrictName = distName }
                            )
                        }
                    }
                    2 -> {
                        val upazilas = filtered.map { it.upazilaNameBn }.distinct()
                        items(upazilas, key = { it }) { upzName ->
                            val count = filtered.count { it.upazilaNameBn == upzName }
                            FolderDrillRow(
                                title = "$upzName উপজেলা",
                                subtitle = "মৌজা সিট: ${BengaliFormatters.toBengaliDigits(count)} টি",
                                onClick = { selectedUpazilaName = upzName }
                            )
                        }
                    }
                    else -> {
                        items(filtered, key = { it.mapKey }) { rec ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        dashboardViewModel.openMouzaInWorkspace(rec, onOpenProjectInWorkspace)
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(rec.mouzaNameBn, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(
                                            "${rec.districtNameBn} • ${rec.upazilaNameBn} • জে.এল ${rec.jlNumber} • ${rec.surveyType}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "সিট ${rec.sheetNumber} • অফলাইন প্রস্তুত",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BhumiEmerald
                                        )
                                    }
                                    IconButton(onClick = { dashboardViewModel.toggleMouzaFavorite(rec.mapKey, !rec.isFavorite) }) {
                                        Icon(
                                            if (rec.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "প্রিয়",
                                            tint = if (rec.isFavorite) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderDrillRow(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            tint = BhumiAmber,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
