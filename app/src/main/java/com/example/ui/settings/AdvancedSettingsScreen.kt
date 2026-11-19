package com.example.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.data.model.MeasurementUnit
import com.example.domain.BengaliFormatters
import com.example.ui.screens.BhumiProBottomTab
import com.example.ui.screens.BhumiProMainScaffold
import com.example.ui.theme.BhumiEmerald
import com.example.ui.theme.BhumiEmeraldContainer
import com.example.ui.theme.BhumiOrangeCta
import com.example.ui.theme.BhumiOrangeSoft
import com.example.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdvancedSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onSelectTab: ((BhumiProBottomTab) -> Unit)? = null,
    onOpenPrintTicket: () -> Unit = {},
    onOpenFeed: () -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val statusMsg by viewModel.statusMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val feedback = remember { FeedbackController({ prefs.hapticFeedback }, snackbarHostState, { prefs.soundEffects }) }

    var showExportDialog by remember { mutableStateOf(false) }
    var exportedJson by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonInput by remember { mutableStateOf("") }
    var showResetConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(statusMsg) {
        val msg = statusMsg ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearStatusMessage()
    }

    val topBarContent: @Composable () -> Unit = {
        TopAppBar(
            navigationIcon = {
                if (onSelectTab == null) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                    }
                }
            },
            title = { Text(BpStrings.get("settings_title", prefs.language), fontWeight = FontWeight.Bold) }
        )
    }

    val bodyContent: @Composable (Modifier) -> Unit = { bodyModifier ->
        Box(modifier = bodyModifier) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Pro / Brand Banner Card (Mockup 8)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BhumiEmeraldContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = BhumiEmerald, modifier = Modifier.size(34.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("সার্ভেয়ার প্রো — NaserX Labs", fontWeight = FontWeight.Bold, color = BhumiEmerald)
                            Text(
                                "সম্পূর্ণ অফলাইন মৌজা CAD, রাস্তা বণ্টন ও প্রো সার্ভে প্যাড PDF সক্রিয় রয়েছে",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Print Defaults & Feed Quick Links (Mockup 8)
                SettingsSectionCard(title = BpStrings.get("section_print_feed", prefs.language)) {
                    SettingsNavRow(
                        icon = Icons.Default.ConfirmationNumber,
                        iconTint = BhumiOrangeCta,
                        iconBg = BhumiOrangeSoft,
                        label = BpStrings.get("print_defaults", prefs.language),
                        onClick = {
                            feedback.performTapFeedback(view)
                            onOpenPrintTicket()
                        },
                        testTag = "settings_print_defaults_row"
                    )
                    SettingsNavRow(
                        icon = Icons.Default.Notifications,
                        iconTint = BhumiEmerald,
                        iconBg = BhumiEmeraldContainer,
                        label = BpStrings.get("open_feed", prefs.language),
                        onClick = {
                            feedback.performTapFeedback(view)
                            onOpenFeed()
                        },
                        testTag = "settings_feed_row"
                    )
                }

                // Section: Appearance & Language
                SettingsSectionCard(title = BpStrings.get("section_appearance", prefs.language)) {
                    Text(BpStrings.get("theme_mode", prefs.language), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("লাইট", "ডার্ক", "সিস্টেম").forEach { mode ->
                            FilterChip(
                                selected = prefs.themeMode == mode,
                                onClick = {
                                    feedback.performTapFeedback(view)
                                    viewModel.updatePreferences { it.copy(themeMode = mode) }
                                },
                                label = { Text(mode) },
                                modifier = Modifier.testTag("theme_chip_$mode")
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(BpStrings.get("language", prefs.language), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("বাংলা", "English").forEach { lang ->
                            FilterChip(
                                selected = prefs.language == lang,
                                onClick = {
                                    feedback.performTapFeedback(view)
                                    viewModel.updatePreferences { it.copy(language = lang) }
                                },
                                label = { Text(lang) },
                                modifier = Modifier.testTag("lang_chip_$lang")
                            )
                        }
                    }
                }

                // Section: Measurement Defaults
                SettingsSectionCard(title = BpStrings.get("section_measurement", prefs.language)) {
                    Text(BpStrings.get("shotok_factor", prefs.language), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            435.6 to "৪৩৫.৬ (মানক)",
                            432.0 to "৪৩২.০ (আঞ্চলিক)",
                            430.0 to "৪৩০.০ (আঞ্চলিক)"
                        ).forEach { (factor, label) ->
                            FilterChip(
                                selected = kotlin.math.abs(prefs.appDefaultShotokSqFt - factor) < 0.01,
                                onClick = {
                                    feedback.performTapFeedback(view)
                                    viewModel.updatePreferences { it.copy(appDefaultShotokSqFt = factor) }
                                },
                                label = { Text(label) }
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(BpStrings.get("default_unit", prefs.language), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MeasurementUnit.entries.forEach { unit ->
                            FilterChip(
                                selected = prefs.defaultUnitId == unit.id,
                                onClick = {
                                    feedback.performTapFeedback(view)
                                    viewModel.updatePreferences { it.copy(defaultUnitId = unit.id) }
                                },
                                label = { Text(unit.bengaliName) }
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(BpStrings.get("default_precision", prefs.language), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0, 1, 2, 3).forEach { dec ->
                            FilterChip(
                                selected = prefs.defaultPrecision == dec,
                                onClick = {
                                    feedback.performTapFeedback(view)
                                    viewModel.updatePreferences { it.copy(defaultPrecision = dec) }
                                },
                                label = { Text("${BengaliFormatters.toBengaliDigits(dec)} ঘর") }
                            )
                        }
                    }
                }

                // Section: Behavior
                SettingsSectionCard(title = BpStrings.get("section_behavior", prefs.language)) {
                    SettingsToggleRow(
                        label = BpStrings.get("sound_effects", prefs.language),
                        checked = prefs.soundEffects,
                        testTag = "toggle_sound",
                        onCheckedChange = { checked ->
                            feedback.performTapFeedback(view)
                            viewModel.updatePreferences { it.copy(soundEffects = checked) }
                        }
                    )
                    SettingsToggleRow(
                        label = BpStrings.get("haptic_feedback", prefs.language),
                        checked = prefs.hapticFeedback,
                        testTag = "toggle_haptic",
                        onCheckedChange = { checked ->
                            feedback.performTapFeedback(view)
                            viewModel.updatePreferences { it.copy(hapticFeedback = checked) }
                        }
                    )
                    SettingsToggleRow(
                        label = BpStrings.get("notifications_enabled", prefs.language),
                        checked = prefs.notificationsEnabled,
                        testTag = "toggle_notifications",
                        onCheckedChange = { checked ->
                            feedback.performTapFeedback(view)
                            viewModel.updatePreferences { it.copy(notificationsEnabled = checked) }
                        }
                    )
                    SettingsToggleRow(
                        label = BpStrings.get("quick_tips", prefs.language),
                        checked = prefs.showQuickTips,
                        testTag = "toggle_quick_tips",
                        onCheckedChange = { checked ->
                            feedback.performTapFeedback(view)
                            viewModel.updatePreferences { it.copy(showQuickTips = checked) }
                        }
                    )
                    SettingsToggleRow(
                        label = BpStrings.get("auto_snapshot", prefs.language),
                        checked = prefs.autoSaveSnapshots,
                        testTag = "toggle_auto_snapshots",
                        onCheckedChange = { checked ->
                            feedback.performTapFeedback(view)
                            viewModel.updatePreferences { it.copy(autoSaveSnapshots = checked) }
                        }
                    )
                }

                // Section: Backup & Safe Reset
                SettingsSectionCard(title = BpStrings.get("section_backup", prefs.language)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    exportedJson = viewModel.exportSettingsJson()
                                    showExportDialog = true
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_export_settings")
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null)
                            Text(" ${BpStrings.get("export_json", prefs.language)}")
                        }
                        OutlinedButton(
                            onClick = { showImportDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_import_settings")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Text(" ${BpStrings.get("import_json", prefs.language)}")
                        }
                    }

                    Button(
                        onClick = { showResetConfirm = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_reset_settings")
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null)
                        Text(" ${BpStrings.get("reset_settings", prefs.language)}")
                    }
                }

                // Section: Developer & Brand Identity
                SettingsSectionCard(title = BpStrings.get("section_about", prefs.language)) {
                    Text(
                        "BhumiPro — মৌজা ম্যাপে জমির পরিমাণ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "সংস্করণ: v${BuildConfig.VERSION_NAME} (কোড ${BengaliFormatters.toBengaliDigits(BuildConfig.VERSION_CODE)})",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "ডেভেলপার: Abu Naser Khan",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "প্রতিষ্ঠান: NaserX Labs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "সম্পূর্ণ অফলাইন মৌজা ম্যাপ ডিজিটাইজেশন, JTS জমি ভাগবণ্টন, ও সার্ভে প্যাড PDF প্রিন্ট ইঞ্জিন।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    if (onSelectTab != null) {
        BhumiProMainScaffold(
            selectedTab = BhumiProBottomTab.SETTINGS,
            language = prefs.language,
            onSelectTab = onSelectTab,
            topBar = topBarContent,
            content = bodyContent
        )
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = topBarContent
        ) { innerPadding ->
            bodyContent(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .navigationBarsPadding()
            )
        }
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("সেটিংস JSON ব্যাকআপ") },
            text = {
                OutlinedTextField(
                    value = exportedJson,
                    onValueChange = {},
                    readOnly = true,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    clipboard?.setPrimaryClip(ClipData.newPlainText("BhumiPro Settings", exportedJson))
                    showExportDialog = false
                    scope.launch { feedback.showMessage("সেটিংস JSON ক্লিপবোর্ডে কপি হয়েছে") }
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Text(" কপি করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) { Text("বন্ধ করুন") }
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("সেটিংস JSON ইমপোর্ট") },
            text = {
                OutlinedTextField(
                    value = importJsonInput,
                    onValueChange = { importJsonInput = it },
                    label = { Text("এখানে JSON পেস্ট করুন") },
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.importSettingsJson(importJsonInput)
                    showImportDialog = false
                    importJsonInput = ""
                }) { Text("ইমপোর্ট করুন") }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("বাতিল") }
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("সেটিংস রিসেট নিশ্চিত করুন") },
            text = {
                Text("শুধুমাত্র অ্যাপের পছন্দসমূহ (থিম, ভাষা, ডিফল্ট একক) রিসেট হবে। আপনার সংরক্ষিত কোনো জরিপ প্রজেক্ট বা মৌজা ম্যাপ মুছে যাবে না।")
            },
            confirmButton = {
                Button(onClick = {
                    showResetConfirm = false
                    viewModel.resetSettings()
                }) { Text("হ্যাঁ, রিসেট করুন") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("না") }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    label: String,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
