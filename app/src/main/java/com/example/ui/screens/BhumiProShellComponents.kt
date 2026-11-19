package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.MouzaMapRecord
import com.example.data.model.SurveyProject
import com.example.domain.BengaliFormatters
import com.example.ui.theme.BhumiAmber
import com.example.ui.theme.BhumiBlue
import com.example.ui.theme.BhumiBlueContainer
import com.example.ui.theme.BhumiEmerald
import com.example.ui.theme.BhumiEmeraldContainer
import com.example.ui.theme.BhumiOrangeCta
import com.example.ui.theme.BhumiOrangeSoft

enum class BhumiProBottomTab(
    val route: String,
    val labelBn: String,
    val labelEn: String,
    val icon: ImageVector
) {
    HOME("dashboard", "হোম", "Home", Icons.Default.Home),
    FILES("files_tab", "ম্যাপ হিস্টরি", "History", Icons.Default.Folder),
    BONTONNAMA("bontonnama_tab", "বন্টননামা", "Share", Icons.Default.AccountTree),
    FEED("feed_tab", "ফিড", "Feed", Icons.Default.DynamicFeed),
    SETTINGS("settings", "সেটিংস", "Settings", Icons.Default.Settings)
}

@Composable
fun BhumiProMainScaffold(
    selectedTab: BhumiProBottomTab,
    language: String = "বাংলা",
    onSelectTab: (BhumiProBottomTab) -> Unit,
    topBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (Modifier) -> Unit
) {
    val isEn = language == "English"
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        if (useRail) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Spacer(Modifier.height(12.dp))
                    BhumiProBottomTab.entries.forEach { tab ->
                        val selected = tab == selectedTab
                        NavigationRailItem(
                            selected = selected,
                            onClick = { onSelectTab(tab) },
                            icon = {
                                Icon(tab.icon, contentDescription = if (isEn) tab.labelEn else tab.labelBn)
                            },
                            label = {
                                Text(
                                    text = if (isEn) tab.labelEn else tab.labelBn,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = BhumiOrangeCta,
                                selectedTextColor = BhumiOrangeCta,
                                indicatorColor = BhumiOrangeSoft
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.route}")
                        )
                    }
                }
                Scaffold(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    topBar = topBar,
                    floatingActionButton = floatingActionButton
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        content(
                            Modifier
                                .fillMaxSize()
                                .widthIn(max = 900.dp)
                        )
                    }
                }
            }
        } else {
            Scaffold(
                topBar = topBar,
                floatingActionButton = floatingActionButton,
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        BhumiProBottomTab.entries.forEach { tab ->
                            val selected = tab == selectedTab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { onSelectTab(tab) },
                                icon = {
                                    Icon(tab.icon, contentDescription = if (isEn) tab.labelEn else tab.labelBn)
                                },
                                label = {
                                    Text(
                                        text = if (isEn) tab.labelEn else tab.labelBn,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = BhumiOrangeCta,
                                    selectedTextColor = BhumiOrangeCta,
                                    indicatorColor = BhumiOrangeSoft,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                content(
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeMapSourceBottomSheet(
    onOpenSavedPdfSheet: () -> Unit,
    onOpenMouzaLibrary: () -> Unit,
    onPickImageFromDevice: () -> Unit,
    onPickPdfFromDevice: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ম্যাপের উৎস বাছুন",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন")
                }
            }

            MapSourceSheetRow(
                icon = Icons.Default.Folder,
                iconTint = BhumiOrangeCta,
                iconBg = BhumiOrangeSoft,
                title = "সংরক্ষিত PDF বাছুন",
                subtitle = "অ্যাপে আগে থেকে রাখা মূল PDF আবার ব্যবহার করুন",
                onClick = {
                    onDismiss()
                    onOpenSavedPdfSheet()
                }
            )
            MapSourceSheetRow(
                icon = Icons.Default.LibraryBooks,
                iconTint = BhumiBlue,
                iconBg = BhumiBlueContainer,
                title = "মৌজা ম্যাপ",
                subtitle = "ক্যাটালগ থেকে ব্রাউজ ও ডাউনলোড (Pro)",
                onClick = {
                    onDismiss()
                    onOpenMouzaLibrary()
                }
            )
            MapSourceSheetRow(
                icon = Icons.Default.UploadFile,
                iconTint = BhumiBlue,
                iconBg = BhumiBlueContainer,
                title = "ডিভাইস থেকে বাছুন",
                subtitle = "এখান থেকে বাছা PDF স্বয়ংক্রিয়ভাবে লাইব্রেরিতে থাকবে",
                onClick = {
                    onDismiss()
                    onPickPdfFromDevice()
                }
            )
            MapSourceSheetRow(
                icon = Icons.Default.Image,
                iconTint = BhumiEmerald,
                iconBg = BhumiEmeraldContainer,
                title = "গ্যালারি থেকে ম্যাপ ছবি বাছুন",
                subtitle = "PNG / JPG স্ক্যান করা মৌজা ম্যাপ (ফটো পিকার)",
                onClick = {
                    onDismiss()
                    onPickImageFromDevice()
                }
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

private data class SavedPdfSheetItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val projectId: String? = null,
    val mouzaRecord: MouzaMapRecord? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedPdfLibraryBottomSheet(
    projects: List<SurveyProject>,
    mouzaLibrary: List<MouzaMapRecord>,
    onOpenProject: (String) -> Unit,
    onOpenMouzaRecord: (MouzaMapRecord) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    val builtInPdfAliases = remember {
        listOf(
            "17-DAKSHINSORTA-2_result_result" to "17-DAKSHINSORTA-2_result_result.pdf • ১৬ ইঞ্চি",
            "11-UTTARSORTA-1_result_result" to "11-UTTARSORTA-1_result_result.pdf • ১৬ ইঞ্চি",
            "35-PASCHIM RAUJAN-4_result_result" to "35-PASCHIM RAUJAN-4_result_result.pdf • ৩২ ইঞ্চি"
        )
    }

    val combinedItems = remember(projects, mouzaLibrary) {
        val list = mutableListOf<SavedPdfSheetItem>()
        projects.forEach { proj ->
            val fileLabel = if (proj.title.endsWith(".pdf", ignoreCase = true)) proj.title else "${proj.title}.pdf"
            list.add(
                SavedPdfSheetItem(
                    id = "proj_${proj.id}",
                    title = proj.title.removeSuffix(".pdf"),
                    subtitle = "$fileLabel • ${proj.mouzaName} • প্লট ${BengaliFormatters.toBengaliDigits(proj.plots.size)}",
                    projectId = proj.id
                )
            )
        }
        val existingTitles = list.map { it.title.lowercase() }.toSet()
        builtInPdfAliases.forEachIndexed { idx, (name, sub) ->
            if (name.lowercase() !in existingTitles) {
                val matchingMouza = mouzaLibrary.getOrNull(idx)
                list.add(
                    SavedPdfSheetItem(
                        id = "sample_pdf_$idx",
                        title = name,
                        subtitle = sub,
                        projectId = if (idx == 2) "draft_raujan_4" else null,
                        mouzaRecord = matchingMouza
                    )
                )
            }
        }
        mouzaLibrary.take(8).forEach { rec ->
            val fileTitle = "${rec.jlNumber}-${rec.mouzaNameBn}_sheet_${rec.sheetNumber}"
            list.add(
                SavedPdfSheetItem(
                    id = "mouza_pdf_${rec.mapKey}",
                    title = fileTitle,
                    subtitle = "${rec.divisionNameBn} • ${rec.districtNameBn} • ${rec.upazilaNameBn}",
                    mouzaRecord = rec
                )
            )
        }
        list
    }

    val filtered = remember(combinedItems, query) {
        if (query.isBlank()) combinedItems
        else combinedItems.filter {
            it.title.contains(query, ignoreCase = true) || it.subtitle.contains(query, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = "সংরক্ষিত PDF বাছুন",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন")
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("সংরক্ষিত ম্যাপ PDF খুঁজুন") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("saved_pdf_search_input")
            )

            filtered.take(8).forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
                        .clickable {
                            onDismiss()
                            when {
                                entry.projectId != null -> onOpenProject(entry.projectId)
                                entry.mouzaRecord != null -> onOpenMouzaRecord(entry.mouzaRecord)
                                else -> onOpenProject("draft_raujan_4")
                            }
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BhumiOrangeSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = BhumiOrangeCta)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = entry.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun MapSourceSheetRow(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
