package com.example.ui.screens.print

import android.app.Activity
import android.graphics.Bitmap
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.BarcodeAndQrHelper
import com.example.domain.BengaliFormatters
import com.example.domain.PdfAndPrintHelper
import com.example.domain.report.BengaliSurveyReportRenderer
import com.example.domain.report.PageLayout
import com.example.domain.report.ReportTemplate
import com.example.ui.components.print.PrintInfoBanner
import com.example.ui.theme.BhumiProPrintColors
import com.example.ui.viewmodel.LandSurveyViewModel
import com.example.ui.viewmodel.PrintTicketViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LivePrintPreviewScreen(
    viewModel: LandSurveyViewModel,
    ticketViewModel: PrintTicketViewModel,
    onBack: () -> Unit,
    onOpenTicket: () -> Unit,
    onOpenExportCenter: () -> Unit
) {
    val context = LocalContext.current
    val project by viewModel.uiState.collectAsStateWithLifecycle()
    val printConfig by viewModel.printConfig.collectAsStateWithLifecycle()
    val canUndoPrint by viewModel.canUndoPrint.collectAsStateWithLifecycle()
    val canRedoPrint by viewModel.canRedoPrint.collectAsStateWithLifecycle()
    val printJobState by viewModel.printJobState.collectAsStateWithLifecycle()
    val presets by viewModel.printPresets.collectAsStateWithLifecycle()
    val insetBitmap by viewModel.pdfBitmap.collectAsStateWithLifecycle()
    val statusMsg by viewModel.statusMessage.collectAsStateWithLifecycle()
    val ticketState by ticketViewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusMsg?.id) {
        val m = statusMsg ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(m.text)
        viewModel.clearStatusMessage()
    }

    // Screenshot policy: FLAG_SECURE on print preview
    DisposableEffect(context) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var regenNonce by remember { mutableIntStateOf(0) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val pageLayout = remember(printConfig) { PageLayout.from(printConfig) }
    val reportData = remember(project, printConfig, ticketState.ticket, regenNonce) {
        viewModel.buildSurveyReportData(ticketState.ticket)
    }

    LaunchedEffect(reportData, ticketState.selectedTemplate, pageLayout, insetBitmap, printConfig.sectionLayout, regenNonce) {
        previewBitmap = withContext(Dispatchers.Default) {
            val typeface = PdfAndPrintHelper.resolveBengaliTypeface(context)
            val qr = if (reportData.ticket.showQr) BarcodeAndQrHelper.generateQrBitmap(reportData.ticket.qrLink) else null
            val sig = PdfAndPrintHelper.decodeUriToBitmap(context, reportData.ticket.signatureUri, 512)
            val logo = PdfAndPrintHelper.decodeUriToBitmap(context, reportData.ticket.logoUri, 512)
            BengaliSurveyReportRenderer.renderPreviewBitmap(
                data = reportData,
                template = ticketState.selectedTemplate,
                typeface = typeface,
                qrBitmap = qr,
                logoBitmap = logo,
                signatureBitmap = sig,
                insetBitmap = insetBitmap,
                sections = printConfig.sectionLayout,
                pageLayout = pageLayout
            )
        }
    }

    Scaffold(
        containerColor = BhumiProPrintColors.ScreenBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BhumiProPrintColors.CardWhite),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                    }
                },
                title = { Text("প্রিন্ট প্রিভিউ", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = { viewModel.undoPrintChange() },
                        enabled = canUndoPrint,
                        modifier = Modifier.testTag("print_undo")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "আনডু")
                    }
                    IconButton(
                        onClick = { viewModel.redoPrintChange() },
                        enabled = canRedoPrint,
                        modifier = Modifier.testTag("print_redo")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "রিডু")
                    }
                    IconButton(
                        onClick = { viewModel.resetPrintFidelityToCanvas() },
                        modifier = Modifier.testTag("print_reset_canvas")
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "ক্যানভাস অবস্থায় রিসেট")
                    }
                    IconButton(
                        onClick = { viewModel.applyPrintStylesToCanvas() },
                        modifier = Modifier.testTag("print_apply_canvas")
                    ) {
                        Icon(Icons.Default.PushPin, contentDescription = "প্রিন্ট স্টাইল ক্যানভাসে প্রয়োগ")
                    }
                    IconButton(
                        onClick = onOpenTicket,
                        modifier = Modifier.testTag("preview_open_ticket")
                    ) {
                        Icon(Icons.Default.ConfirmationNumber, contentDescription = "টিকেট সেটিংস")
                    }
                    IconButton(
                        onClick = { regenNonce++ },
                        modifier = Modifier.testTag("preview_regenerate")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "রিজেনারেট")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = BhumiProPrintColors.CardWhite,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.exportSurveyPadPdf(context, ticketState.ticket, ticketState.selectedTemplate)
                        },
                        enabled = printJobState.isReady && !printJobState.isExporting,
                        colors = ButtonDefaults.buttonColors(containerColor = BhumiProPrintColors.OrangePrimary),
                        modifier = Modifier
                            .weight(1.25f)
                            .testTag("action_download_pdf")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("PDF ডাউনলোড")
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.exportAndSharePdf(context, ticketState.ticket, ticketState.selectedTemplate)
                        },
                        enabled = printJobState.isReady && !printJobState.isExporting,
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("action_share")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("শেয়ার")
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.printSurveyA4(context, ticketState.ticket, ticketState.selectedTemplate)
                        },
                        enabled = printJobState.isReady && !printJobState.isExporting,
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("action_print")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("প্রিন্ট")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = BhumiProPrintColors.CardWhite,
                modifier = Modifier.testTag("print_tabs")
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("প্রিভিউ") })
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("কাস্টমাইজ") },
                    modifier = Modifier.testTag("customize_toggle")
                )
            }

            if (printJobState.isExporting) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BhumiProPrintColors.OrangeCream)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "PDF তৈরি হচ্ছে — পৃষ্ঠা ${BengaliFormatters.toBengaliDigits(printJobState.exportProgressPage)}/${BengaliFormatters.toBengaliDigits(printJobState.exportTotalPages.coerceAtLeast(1))}",
                            style = MaterialTheme.typography.labelLarge
                        )
                        LinearProgressIndicator(
                            progress = {
                                val t = printJobState.exportTotalPages.coerceAtLeast(1)
                                (printJobState.exportProgressPage.toFloat() / t).coerceIn(0f, 1f)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    TextButton(onClick = { viewModel.cancelPrintExport() }) {
                        Text("বাতিল", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (selectedTab == 0) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    printJobState.blockedReasonBn?.let { PrintInfoBanner(it, isError = true) }
                    printJobState.warningBn?.let { PrintInfoBanner(it, isWarning = true) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            PrintInfoBanner("এখানে স্ক্রিনশট বন্ধ। ছবি পাঠাতে পাশের PNG বাটন ব্যবহার করুন।")
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.exportProjectInFormat(
                                    context,
                                    com.example.data.model.ExportFormat.PNG,
                                    shareAfter = true
                                )
                            },
                            modifier = Modifier.testTag("action_share_png")
                        ) {
                            Text("ছবি (PNG)")
                        }
                    }

                    // Plot selection row
                    Text("রিপোর্টে কোন প্লট যাবে?", style = MaterialTheme.typography.titleMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("plot_selection_row")
                    ) {
                        if (project.plots.isEmpty()) {
                            Text(
                                "কোনো প্লট নির্বাচিত নয় — ম্যাপে বা এখানে ট্যাপ করে রিপোর্টে প্লট যোগ করুন।",
                                style = MaterialTheme.typography.bodyMedium,
                                color = BhumiProPrintColors.InkSecondary
                            )
                        } else {
                            project.plots.forEach { plot ->
                                val selected = plot.id in project.selectedPlotIds
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.togglePlotReportSelection(plot.id) },
                                    label = { Text(plot.name) }
                                )
                            }
                            if (project.selectedPlotIds.isNotEmpty()) {
                                TextButton(onClick = { viewModel.clearPlotSelection() }) {
                                    Text("নির্বাচন মুছুন")
                                }
                            }
                        }
                    }

                    // Zoomable Page Bitmap Preview
                    var zoomScale by remember { mutableFloatStateOf(1f) }
                    var panX by remember { mutableFloatStateOf(0f) }
                    var panY by remember { mutableFloatStateOf(0f) }
                    val aspect = (pageLayout.widthPt / pageLayout.heightPt).coerceIn(0.4f, 2.2f)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(aspect)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .border(1.dp, BhumiProPrintColors.CardBorder, RoundedCornerShape(10.dp))
                            .testTag("preview_canvas")
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    zoomScale = (zoomScale * zoom).coerceIn(1f, 5f)
                                    if (zoomScale > 1.02f) {
                                        panX += pan.x
                                        panY += pan.y
                                    } else {
                                        panX = 0f
                                        panY = 0f
                                    }
                                }
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(onDoubleTap = {
                                    if (zoomScale > 1.1f) {
                                        zoomScale = 1f; panX = 0f; panY = 0f
                                    } else {
                                        zoomScale = 2.2f
                                    }
                                })
                            }
                    ) {
                        previewBitmap?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "সার্ভে প্যাড প্রিভিউ",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = zoomScale,
                                        scaleY = zoomScale,
                                        translationX = panX,
                                        translationY = panY
                                    )
                            )
                        }
                        if (zoomScale > 1.05f) {
                            TextButton(
                                onClick = { zoomScale = 1f; panX = 0f; panY = 0f },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                            ) {
                                Text("রিসেট জুম")
                            }
                        }
                    }

                    // Template selector cards
                    Text("রিপোর্ট টেমপ্লেট", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReportTemplate.entries.forEach { tpl ->
                            val isSelected = ticketState.selectedTemplate == tpl
                            Column(
                                modifier = Modifier
                                    .width(140.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) BhumiProPrintColors.OrangeCream else BhumiProPrintColors.CardWhite)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) BhumiProPrintColors.OrangePrimary else BhumiProPrintColors.CardBorder,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { ticketViewModel.selectTemplate(tpl) }
                                    .padding(10.dp)
                            ) {
                                Text(tpl.titleBn, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                Text(tpl.subtitleBn, style = MaterialTheme.typography.labelSmall, color = BhumiProPrintColors.InkSecondary)
                            }
                        }
                    }

                    // Quick print text fields
                    Text("প্রিন্টের লেখা", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = printConfig.headerTitle,
                        onValueChange = { v -> viewModel.updatePrintConfig { it.copy(headerTitle = v) } },
                        label = { Text("শিরোনাম") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = printConfig.surveyorName,
                        onValueChange = { v -> viewModel.updatePrintConfig { it.copy(surveyorName = v) } },
                        label = { Text("সার্ভেয়ার") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedButton(
                        onClick = onOpenExportCenter,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("preview_open_export_center")
                    ) {
                        Text("উন্নত প্রিন্ট সেটিংস ও রপ্তানি কেন্দ্র")
                    }
                }
            } else {
                PrintSurveyPadCustomizePanel(
                    printConfig = printConfig,
                    canvasSettings = project.canvasSettings,
                    layers = project.layers,
                    presets = presets,
                    onUpdatePrintConfig = { transform -> viewModel.updatePrintConfigUndoable(transform) },
                    onUpdateCanvasSettings = { transform -> viewModel.updateCanvasSettings(transform) },
                    onToggleGridLayer = { viewModel.toggleShowGrid() },
                    onSaveGlobal = { viewModel.persistPrintConfig(saveAsGlobalDefault = true) },
                    onSaveProject = { viewModel.persistPrintConfig(saveAsGlobalDefault = false) },
                    onClearProjectOverride = { viewModel.clearProjectPrintOverride() },
                    onSavePreset = { name -> viewModel.savePrintPreset(name) },
                    onApplyPreset = { id -> viewModel.applyPrintPreset(id) },
                    onDeletePreset = { id -> viewModel.deletePrintPreset(id) }
                )
            }
        }
    }
}
