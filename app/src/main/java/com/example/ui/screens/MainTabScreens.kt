package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.domain.UnknownSideCalculator
import com.example.ui.theme.BhumiBlue
import com.example.ui.theme.BhumiBlueContainer
import com.example.ui.theme.BhumiEmerald
import com.example.ui.theme.BhumiEmeraldContainer
import com.example.ui.theme.BhumiOrangeCta
import com.example.ui.theme.BhumiOrangeSoft
import com.example.ui.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesTabScreen(
    viewModel: DashboardViewModel,
    language: String,
    onSelectTab: (BhumiProBottomTab) -> Unit,
    onOpenWorkspace: (String) -> Unit,
    onOpenCreateDialog: () -> Unit
) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val exports by viewModel.recentExports.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("সব") }

    val filtered = remember(projects, query, statusFilter) {
        projects.filter { p ->
            val matchStatus = statusFilter == "সব" || p.status == statusFilter
            val matchQuery = query.isBlank() ||
                p.title.contains(query, ignoreCase = true) ||
                p.mouzaName.contains(query, ignoreCase = true) ||
                p.jlNumber.contains(query, ignoreCase = true)
            matchStatus && matchQuery
        }
    }

    BhumiProMainScaffold(
        selectedTab = BhumiProBottomTab.FILES,
        language = language,
        onSelectTab = onSelectTab,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("সংরক্ষিত ফাইল ও প্রজেক্ট", fontWeight = FontWeight.Bold)
                        Text(
                            "মোট ${BengaliFormatters.toBengaliDigits(projects.size)} টি জরিপ • ${BengaliFormatters.toBengaliDigits(exports.size)} টি রপ্তানি",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenCreateDialog,
                containerColor = BhumiOrangeCta,
                contentColor = Color.White,
                modifier = Modifier.testTag("files_fab_new_project")
            ) {
                Icon(Icons.Default.Add, contentDescription = "নতুন প্রজেক্ট")
            }
        }
    ) { modifier ->
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text("প্রজেক্ট বা মৌজার নাম খুঁজুন...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("সব", "সক্রিয়", "খসড়া", "চূড়ান্ত").forEach { st ->
                        FilterChip(
                            selected = statusFilter == st,
                            onClick = { statusFilter = st },
                            label = { Text(st) }
                        )
                    }
                }
            }
            items(filtered, key = { it.id }) { proj ->
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
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(proj.mouzaName.ifBlank { proj.title }, fontWeight = FontWeight.Bold)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(BhumiOrangeSoft)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(proj.status, style = MaterialTheme.typography.labelSmall, color = BhumiOrangeCta)
                                }
                            }
                            Text(
                                "${proj.scale} • প্লট: ${BengaliFormatters.toBengaliDigits(proj.plots.size)} • ${BengaliFormatters.formatDecimal(proj.totalLandAreaShotangsho, 2)} শতক",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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

            if (exports.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(6.dp))
                    Text("সাম্প্রতিক রপ্তানিকৃত নথি", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(exports, key = { it.id }) { exp ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = BhumiBlue)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(exp.fileName, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${exp.format} • প্লট: ${BengaliFormatters.toBengaliDigits(exp.plotCount)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BontonnamaScreen(
    viewModel: DashboardViewModel,
    defaultShotokFactor: Double,
    language: String,
    onSelectTab: (BhumiProBottomTab) -> Unit,
    onOpenWorkspace: (String) -> Unit
) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    var totalShotokText by remember { mutableStateOf("10.0") }
    val shareRows = remember {
        mutableStateListOf(
            "অংশীদার ১ (বড়)" to "1",
            "অংশীদার ২" to "1",
            "অংশীদার ৩" to "1",
            "অংশীদার ৪" to "1"
        )
    }

    val parsedTotal = BengaliFormatters.parseDoubleFlexible(totalShotokText) ?: 0.0
    val calculated = remember(parsedTotal, shareRows.toList(), defaultShotokFactor) {
        val pairs = shareRows.map { (name, w) -> name to (BengaliFormatters.parseDoubleFlexible(w) ?: 0.0) }
        UnknownSideCalculator.calculateBontonnamaShares(parsedTotal, pairs, defaultShotokFactor)
    }

    BhumiProMainScaffold(
        selectedTab = BhumiProBottomTab.BONTONNAMA,
        language = language,
        onSelectTab = onSelectTab,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("বণ্টননামা ও হিস্যা হিসাব", fontWeight = FontWeight.Bold)
                        Text(
                            "অংশীদার ও ওয়ারিশ জমি ভাগবণ্টন",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { modifier ->
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (projects.any { it.totalLandAreaShotangsho > 0.0 }) {
                item {
                    Text("প্রজেক্ট থেকে মোট জমি নিন:", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        projects.filter { it.totalLandAreaShotangsho > 0.0 }.take(3).forEach { p ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    totalShotokText = BengaliFormatters.formatDecimal(p.totalLandAreaShotangsho, 2, useBengali = false)
                                },
                                label = { Text("${p.mouzaName} (${BengaliFormatters.formatDecimal(p.totalLandAreaShotangsho, 2)} শতক)") }
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = totalShotokText,
                    onValueChange = { totalShotokText = it },
                    label = { Text("মোট বণ্টনযোগ্য জমি (শতক)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("অংশীদার / হিস্যা তালিকা", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(onClick = {
                            for (i in shareRows.indices) {
                                shareRows[i] = shareRows[i].first to "1"
                            }
                        }) { Text("সমান ভাগ") }
                        OutlinedButton(onClick = {
                            shareRows.add("অংশীদার ${BengaliFormatters.toBengaliDigits(shareRows.size + 1)}" to "1")
                        }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("যোগ")
                        }
                    }
                }
            }

            items(shareRows.size) { idx ->
                val (label, weightText) = shareRows[idx]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { v -> shareRows[idx] = v to weightText },
                        label = { Text("নাম / বিবরণ") },
                        singleLine = true,
                        modifier = Modifier.weight(1.4f)
                    )
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { w -> shareRows[idx] = label to w },
                        label = { Text("অংশ/অনুপাত") },
                        singleLine = true,
                        modifier = Modifier.weight(0.9f)
                    )
                    if (shareRows.size > 2) {
                        IconButton(onClick = { shareRows.removeAt(idx) }) {
                            Icon(Icons.Default.Delete, contentDescription = "মুছুন", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Equalizer, contentDescription = null, tint = BhumiEmerald)
                            Text("বণ্টননামা চূড়ান্ত সারণী", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider()
                        if (calculated.isEmpty()) {
                            Text("সঠিক অংশ/অনুপাত লিখুন", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            calculated.forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(row.label, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "${BengaliFormatters.formatDecimal(row.normalizedPercent, 2)}% • ${BengaliFormatters.formatDecimal(row.areaKatha, 2)} কাঠা",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            "${BengaliFormatters.formatDecimal(row.areaShotok, 3)} শতক",
                                            fontWeight = FontWeight.Bold,
                                            color = BhumiEmerald
                                        )
                                        Text(
                                            "${BengaliFormatters.formatDecimal(row.areaSqFt, 1)} বর্গফুট",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = { onOpenWorkspace(projects.firstOrNull()?.id ?: "draft_raujan_4") },
                    colors = ButtonDefaults.buttonColors(containerColor = BhumiOrangeCta),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ম্যাপ ওয়ার্কস্পেসে নকশা ভাগ করুন")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedTabScreen(
    viewModel: DashboardViewModel,
    language: String,
    onSelectTab: (BhumiProBottomTab) -> Unit,
    onOpenWorkspace: (String) -> Unit,
    onOpenMouzaLibrary: () -> Unit
) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val exports by viewModel.recentExports.collectAsStateWithLifecycle()
    val conversions by viewModel.conversionHistory.collectAsStateWithLifecycle()

    BhumiProMainScaffold(
        selectedTab = BhumiProBottomTab.FEED,
        language = language,
        onSelectTab = onSelectTab,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("জরিপ ফিড ও নোটিফিকেশন", fontWeight = FontWeight.Bold)
                        Text(
                            "অফলাইন কার্যক্রম ও ক্যালিব্রেশন চেকলিস্ট",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshCatalog() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "রিফ্রেশ")
                    }
                }
            )
        }
    ) { modifier ->
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                FeedCard(
                    title = "BhumiPro v1.6.0 — NaserX Labs প্রোডাকশন রিলিজ",
                    subtitle = "৮ বিভাগের অফলাইন মৌজা ক্যাটালগ, লাইভ ড্র্যাগেবল রাস্তা বণ্টন ও Survey Pad A4 PDF ইঞ্জিন সক্রিয় রয়েছে।",
                    badge = "আপডেট",
                    badgeBg = BhumiEmeraldContainer,
                    badgeFg = BhumiEmerald,
                    onClick = onOpenMouzaLibrary
                )
            }

            val uncalibrated = projects.filter { !it.isCalibrated }
            if (uncalibrated.isNotEmpty()) {
                item {
                    FeedCard(
                        title = "${BengaliFormatters.toBengaliDigits(uncalibrated.size)} টি প্রজেক্টে স্কেল ক্যালিব্রেশন বাকি",
                        subtitle = "ম্যাপে নিখুঁত পরিমাপ পেতে ১৬/৩২ ইঞ্চি স্কেল বা ২-পয়েন্ট ক্যালিব্রেশন সম্পন্ন করুন।",
                        badge = "পরামর্শ",
                        badgeBg = BhumiOrangeSoft,
                        badgeFg = BhumiOrangeCta,
                        onClick = { onOpenWorkspace(uncalibrated.first().id) }
                    )
                }
            }

            if (exports.isNotEmpty()) {
                item {
                    Text("সাম্প্রতিক PDF ও ফাইল রপ্তানি", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(exports, key = { it.id }) { exp ->
                    FeedCard(
                        title = "তৈরি হয়েছে: ${exp.fileName}",
                        subtitle = "${exp.format} • মোট প্লট: ${BengaliFormatters.toBengaliDigits(exp.plotCount)}",
                        badge = exp.format,
                        badgeBg = BhumiBlueContainer,
                        badgeFg = BhumiBlue,
                        onClick = { onOpenWorkspace(exp.projectId) }
                    )
                }
            }

            item {
                Text("জরিপ প্রজেক্ট ইতিহাস", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(projects, key = { it.id }) { proj ->
                FeedCard(
                    title = proj.mouzaName.ifBlank { proj.title },
                    subtitle = "প্লট: ${BengaliFormatters.toBengaliDigits(proj.plots.size)} • ${BengaliFormatters.formatDecimal(proj.totalLandAreaShotangsho, 2)} শতক • স্কেল ${proj.scale}",
                    badge = proj.status,
                    badgeBg = BhumiEmeraldContainer,
                    badgeFg = BhumiEmerald,
                    onClick = { onOpenWorkspace(proj.id) }
                )
            }

            if (conversions.isNotEmpty()) {
                item {
                    Text("একক রূপান্তর লগ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(conversions.take(5), key = { it.id }) { conv ->
                    FeedCard(
                        title = "${conv.sourceValue} ${conv.sourceUnit}",
                        subtitle = conv.resultSummary,
                        badge = "ক্যালকুলেটর",
                        badgeBg = BhumiOrangeSoft,
                        badgeFg = BhumiOrangeCta,
                        onClick = {}
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedCard(
    title: String,
    subtitle: String,
    badge: String,
    badgeBg: Color,
    badgeFg: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = badgeFg, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(badge, style = MaterialTheme.typography.labelSmall, color = badgeFg, fontWeight = FontWeight.Bold)
                    }
                }
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
