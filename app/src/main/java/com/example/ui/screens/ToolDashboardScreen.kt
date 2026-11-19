package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.WorkspaceMode
import com.example.domain.BengaliFormatters
import com.example.ui.theme.BhumiBlue
import com.example.ui.theme.BhumiBlueContainer
import com.example.ui.theme.BhumiEmerald
import com.example.ui.theme.BhumiEmeraldContainer
import com.example.ui.theme.BhumiEmeraldLight
import com.example.ui.theme.BhumiOrangeCta
import com.example.ui.theme.BhumiOrangeSoft
import com.example.ui.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolDashboardScreen(
    viewModel: DashboardViewModel,
    language: String = "বাংলা",
    onSelectTab: (BhumiProBottomTab) -> Unit = {},
    onOpenWorkspace: (String) -> Unit,
    onOpenWorkspaceWithMode: (String, WorkspaceMode) -> Unit = { id, _ -> onOpenWorkspace(id) },
    onOpenRoute: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val mouzaLibrary by viewModel.mouzaLibrary.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showMapSourceSheet by remember { mutableStateOf(false) }
    var showSavedPdfSheet by remember { mutableStateOf(false) }
    var showFormulasDialog by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            viewModel.createProjectFromUri(uri.toString(), "ছবি মৌজা জরিপ") { newId ->
                onOpenWorkspace(newId)
            }
        }
    }
    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.createProjectFromUri(uri.toString(), "PDF মৌজা জরিপ") { newId ->
                onOpenWorkspace(newId)
            }
        }
    }

    val firstProjectId = projects.firstOrNull()?.id ?: "draft_raujan_4"

    BhumiProMainScaffold(
        selectedTab = BhumiProBottomTab.HOME,
        language = language,
        onSelectTab = onSelectTab,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("BhumiPro • ভূমিপ্রো", fontWeight = FontWeight.Bold)
                        Text(
                            "NaserX Labs • মৌজা ম্যাপে জমির পরিমাণ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onOpenRoute("compass") }) {
                        Icon(Icons.Default.Explore, contentDescription = "ডিজিটাল কম্পাস")
                    }
                    IconButton(onClick = { onSelectTab(BhumiProBottomTab.FEED) }) {
                        Icon(Icons.Default.Notifications, contentDescription = "নোটিফিকেশন ও ফিড")
                    }
                    IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("btn_open_settings")) {
                        Icon(Icons.Default.Settings, contentDescription = "সেটিংস")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = BhumiOrangeCta,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_new_project")
            ) {
                Icon(Icons.Default.Add, contentDescription = "নতুন জরিপ প্রজেক্ট")
            }
        }
    ) { modifier ->
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Section 1: পিন করা টুল (Screenshot 1)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BhumiOrangeSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PushPin, contentDescription = null, tint = BhumiOrangeCta, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("পিন করা টুল", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "যে টুলগুলো বেশি ব্যবহার করেন, সেগুলো এখানে রাখুন",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, BhumiOrangeCta.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                            .background(BhumiOrangeSoft)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "৪টি পিন",
                            style = MaterialTheme.typography.labelSmall,
                            color = BhumiOrangeCta,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DashboardGridCard(
                            title = "মৌজা ম্যাপে জমি পরিমাপ",
                            subtitle = "জমি মাপুন, ভাগবণ্টন করুন সুন্দর ভাবে",
                            icon = Icons.Default.Map,
                            accent = BhumiBlue,
                            containerBg = BhumiBlueContainer,
                            isPinned = true,
                            onClick = { showMapSourceSheet = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("card_mouza_measure")
                        )
                        DashboardGridCard(
                            title = "ম্যাপ জয়েন্ট",
                            subtitle = "একাধিক সিট একসাথে জোড়া দিন",
                            icon = Icons.Default.Layers,
                            accent = Color(0xFF0D9488),
                            containerBg = Color(0xFFCCFBF1),
                            isPinned = true,
                            onClick = { onOpenWorkspaceWithMode(firstProjectId, WorkspaceMode.MAP_JOINT) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DashboardGridCard(
                            title = "তুলনামূলক প্যান্টোগ্রাফ",
                            subtitle = "সাবেক ও হাল ম্যাপ তুলনা করুন",
                            icon = Icons.Default.Compare,
                            accent = BhumiOrangeCta,
                            containerBg = BhumiOrangeSoft,
                            isPinned = true,
                            onClick = { onOpenWorkspaceWithMode(firstProjectId, WorkspaceMode.COMPARATIVE_PANTOGRAPH) },
                            modifier = Modifier.weight(1f)
                        )
                        DashboardGridCard(
                            title = "সকল মৌজা ম্যাপ",
                            subtitle = "বাংলাদেশের মৌজা ম্যাপ ব্রাউজ ও ডাউনলোড করুন",
                            icon = Icons.Default.LibraryBooks,
                            accent = Color(0xFF854D0E),
                            containerBg = Color(0xFFFEF3C7),
                            isPinned = true,
                            onClick = { onOpenRoute("mouza_library") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Section 2: পরিমাপ (Screenshot 1)
            item {
                Spacer(Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BhumiOrangeSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Straighten, contentDescription = null, tint = BhumiOrangeCta, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("পরিমাপ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "জমি, ক্ষেত্রফল, ম্যাপ ও এককের মাপজোক",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DashboardGridCard(
                            title = "আনা-গন্ডা হিসাব",
                            subtitle = "আনা, গন্ডা, কড়া ও ক্রান্তির হিসাব",
                            icon = Icons.Default.Calculate,
                            accent = BhumiOrangeCta,
                            containerBg = BhumiOrangeSoft,
                            isPinned = false,
                            onClick = { onOpenRoute("ana_gonda") },
                            modifier = Modifier.weight(1f)
                        )
                        DashboardGridCard(
                            title = "ক্ষেত্রফল ও অজানা বাহু",
                            subtitle = "জমির ক্ষেত্রফল বা অজানা বাহু বের করুন",
                            icon = Icons.Default.Architecture,
                            accent = Color(0xFF0D9488),
                            containerBg = Color(0xFFCCFBF1),
                            isPinned = false,
                            onClick = { onOpenRoute("unknown_side_area") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DashboardGridCard(
                            title = "জমি পরিমাপ",
                            subtitle = "পরিমাপ, আনা-গন্ডা, হিস্যা ও একক রূপান্তর",
                            icon = Icons.Default.SwapHoriz,
                            accent = BhumiOrangeCta,
                            containerBg = BhumiOrangeSoft,
                            isPinned = false,
                            onClick = { onOpenRoute("unit_converter") },
                            modifier = Modifier.weight(1f)
                        )
                        DashboardGridCard(
                            title = "জমির সকল সূত্র",
                            subtitle = "জমির একক ও ক্ষেত্রফলের দরকারি সূত্র",
                            icon = Icons.Default.MenuBook,
                            accent = BhumiEmerald,
                            containerBg = BhumiEmeraldContainer,
                            isPinned = false,
                            onClick = { showFormulasDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Quick Workspace Hero Action Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(listOf(BhumiEmerald, BhumiEmeraldLight)))
                        .clickable { onOpenWorkspace(firstProjectId) }
                        .padding(16.dp)
                        .testTag("hero_mouza_survey_card")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "সরাসরি জরিপ ওয়ার্কস্পেস ও কম্পাস",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "অফলাইন CAD ও Survey Pad A4 PDF রপ্তানি সহ চালু করুন",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(BhumiOrangeCta)
                                    .clickable { showMapSourceSheet = true }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                                    .testTag("btn_choose_map_source")
                            ) {
                                Text(
                                    text = "ম্যাপের উৎস বাছুন",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White)
                                    .clickable { onOpenWorkspace(firstProjectId) }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = "ওয়ার্কস্পেস (${BengaliFormatters.toBengaliDigits(projects.size)})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BhumiEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.18f))
                                    .clickable { onOpenRoute("compass") }
                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = "কম্পাস",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Recent Projects Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("সাম্প্রতিক প্রজেক্ট", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { onSelectTab(BhumiProBottomTab.FILES) }) {
                        Text("সব দেখুন")
                    }
                }
            }

            items(projects, key = { it.id }) { proj ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenWorkspace(proj.id) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BhumiEmeraldContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = BhumiEmerald)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                proj.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${proj.mouzaName} • ${proj.scale} • প্লট: ${BengaliFormatters.toBengaliDigits(proj.plots.size)} (${BengaliFormatters.formatDecimal(proj.totalLandAreaShotangsho, 2)} শতক)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (proj.id != "draft_raujan_4") {
                            IconButton(onClick = { viewModel.deleteProject(proj.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "মুছুন", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    if (showMapSourceSheet) {
        HomeMapSourceBottomSheet(
            onOpenSavedPdfSheet = { showSavedPdfSheet = true },
            onOpenMouzaLibrary = { onOpenRoute("mouza_library") },
            onPickImageFromDevice = {
                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onPickPdfFromDevice = {
                documentPicker.launch(arrayOf("application/pdf", "image/*"))
            },
            onDismiss = { showMapSourceSheet = false }
        )
    }

    if (showSavedPdfSheet) {
        SavedPdfLibraryBottomSheet(
            projects = projects,
            mouzaLibrary = mouzaLibrary,
            onOpenProject = { projId -> onOpenWorkspace(projId) },
            onOpenMouzaRecord = { rec ->
                viewModel.openMouzaInWorkspace(rec) { newProjId ->
                    onOpenWorkspace(newProjId)
                }
            },
            onDismiss = { showSavedPdfSheet = false }
        )
    }

    if (showFormulasDialog) {
        AlertDialog(
            onDismissRequest = { showFormulasDialog = false },
            title = { Text("জমির পরিমাপ ও জরিপের সকল সূত্র", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FormulaItem("শতক ও একক মানদণ্ড", "১ শতক = ৪৩৫.৬ বর্গফুট = ১০০০ বর্গলিঙ্ক\n১ কাঠা = ৭২০ বর্গফুট (১.৬৫ শতক)\n১ বিঘা = ২০ কাঠা = ৩৩ শতক = ১৪,৪০০ বর্গফুট\n১ একর = ১০০ শতক = ৩.০৩ বিঘা")
                    HorizontalDivider()
                    FormulaItem("ত্রিভুজের হেরনের সূত্র", "অর্ধ-পরিসীমা s = (a + b + c) / ২\nক্ষেত্রফল = √[s(s - a)(s - b)(s - c)]")
                    HorizontalDivider()
                    FormulaItem("মৌজা স্কেল হিসাব", "১৬ ইঞ্চি = ১ মাইল → ১ ইঞ্চি = ৩৩০ ফুট\n৩২ ইঞ্চি = ১ মাইল → ১ ইঞ্চি = ১৬৫ ফুট\n৬৪ ইঞ্চি = ১ মাইল → ১ ইঞ্চি = ৮২.৫ ফুট\n৮০ ইঞ্চি = ১ মাইল → ১ ইঞ্চি = ৬৬ ফুট")
                    HorizontalDivider()
                    FormulaItem("আনা-গন্ডা হিস্যা", "১ পুরো অংশ = ১৬ আনা\n১ আনা = ২০ গন্ডা = ৮০ কড়া = ২৪০ ক্রান্তি")
                }
            },
            confirmButton = {
                Button(
                    onClick = { showFormulasDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = BhumiOrangeCta)
                ) { Text("বুঝেছি", color = Color.White) }
            }
        )
    }

    if (showCreateDialog) {
        var title by remember { mutableStateOf("") }
        var mouza by remember { mutableStateOf("") }
        var jl by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("নতুন জরিপ প্রজেক্ট তৈরি") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("প্রজেক্ট শিরোনাম") }, singleLine = true)
                    OutlinedTextField(value = mouza, onValueChange = { mouza = it }, label = { Text("মৌজার নাম") }, singleLine = true)
                    OutlinedTextField(value = jl, onValueChange = { jl = it }, label = { Text("জে.এল নম্বর") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCreateDialog = false
                        viewModel.createNewProject(title, mouza, jlNumber = jl) { newId ->
                            onOpenWorkspace(newId)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BhumiOrangeCta)
                ) { Text("তৈরি করুন") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("বাতিল") } }
        )
    }
}

@Composable
private fun FormulaItem(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = BhumiEmerald)
        Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun DashboardGridCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    containerBg: Color,
    isPinned: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(148.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(13.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(containerBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(22.dp))
            }
            Icon(
                imageVector = Icons.Default.PushPin,
                contentDescription = if (isPinned) "পিন করা" else "পিন",
                tint = if (isPinned) BhumiOrangeCta else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
