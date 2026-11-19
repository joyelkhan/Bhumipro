package com.example.ui.screens.print

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.report.ReportTemplate
import com.example.ui.theme.BhumiEmerald
import com.example.ui.theme.BhumiEmeraldContainer
import com.example.ui.theme.BhumiOrangeCta
import com.example.ui.theme.BhumiOrangeSoft
import com.example.ui.viewmodel.PrintTicketViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintTicketScreen(
    ticketViewModel: PrintTicketViewModel,
    onBack: () -> Unit
) {
    val state by ticketViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            ticketViewModel.updateTicket { it.copy(logoUri = uri.toString()) }
        }
    }
    val signaturePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            ticketViewModel.updateTicket { it.copy(signatureUri = uri.toString()) }
        }
    }

    LaunchedEffect(state.statusMessage) {
        val msg = state.statusMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        ticketViewModel.clearStatusMessage()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("print_defaults_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                    }
                },
                title = { Text("প্রিন্ট ডিফল্ট", fontWeight = FontWeight.Bold) }
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { ticketViewModel.clearAllTicket() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_clear_print_defaults")
                    ) {
                        Text("সব ডিফল্ট মুছুন")
                    }
                    Button(
                        onClick = { ticketViewModel.saveTicket() },
                        colors = ButtonDefaults.buttonColors(containerColor = BhumiOrangeCta),
                        modifier = Modifier
                            .weight(1.1f)
                            .testTag("btn_save_print_defaults")
                    ) {
                        Text("ডিফল্ট সেভ করুন", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        val t = state.ticket
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Template preview strip (Mockup 9: নমুনা ডেটা দিয়ে প্রিভিউ)
            Text("রিপোর্ট টেমপ্লেট প্রিভিউ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ReportTemplate.entries.forEach { tpl ->
                    val selected = state.selectedTemplate == tpl
                    Column(
                        modifier = Modifier
                            .width(156.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) BhumiOrangeSoft else MaterialTheme.colorScheme.surface)
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) BhumiOrangeCta else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { ticketViewModel.selectTemplate(tpl) }
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("নমুনা", style = MaterialTheme.typography.labelSmall, color = if (selected) BhumiOrangeCta else BhumiEmerald, fontWeight = FontWeight.Bold)
                            if (selected) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BhumiOrangeCta, modifier = Modifier.size(16.dp))
                            }
                        }
                        Text(tpl.titleBn, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(tpl.subtitleBn, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Section 1: হেডার ও সার্ভে ফার্ম
            SectionHeader("হেডার ও প্রতিষ্ঠান")
            OutlinedTextField(
                value = t.companyName,
                onValueChange = { v -> ticketViewModel.updateTicket { it.copy(companyName = v) } },
                label = { Text("প্রতিষ্ঠান / সার্ভে ফার্মের নাম") },
                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = t.title,
                onValueChange = { v -> ticketViewModel.updateTicket { it.copy(title = v) } },
                label = { Text("রিপোর্ট শিরোনাম") },
                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = t.surveyorName,
                onValueChange = { v -> ticketViewModel.updateTicket { it.copy(surveyorName = v) } },
                label = { Text("সার্ভেয়ারের নাম") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = t.surveyorMobile,
                onValueChange = { v -> ticketViewModel.updateTicket { it.copy(surveyorMobile = v) } },
                label = { Text("মোবাইল") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = t.address,
                onValueChange = { v -> ticketViewModel.updateTicket { it.copy(address = v) } },
                label = { Text("ঠিকানা") },
                leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = t.registrationNo,
                onValueChange = { v -> ticketViewModel.updateTicket { it.copy(registrationNo = v) } },
                label = { Text("রেজিঃ নং") },
                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Section 2: অবস্থান
            SectionHeader("অবস্থান")
            OutlinedTextField(
                value = t.districtField,
                onValueChange = { v -> ticketViewModel.updateTicket { it.copy(districtField = v) } },
                label = { Text("জেলা") },
                leadingIcon = { Icon(Icons.Default.Map, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = t.upazilaField,
                onValueChange = { v -> ticketViewModel.updateTicket { it.copy(upazilaField = v) } },
                label = { Text("থানা / উপজেলা") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Section 3: রিপোর্ট প্যাড — লোগো ও স্বাক্ষর
            SectionHeader("রিপোর্ট প্যাড")
            MediaPickerRowCard(
                icon = Icons.Default.Image,
                title = "লোগো",
                subtitle = if (t.logoUri != null) "লোগো যুক্ত হয়েছে — হেডারে ছাপা হবে" else "রিপোর্ট প্যাডের হেডারে ছাপা হবে",
                hasValue = t.logoUri != null,
                onPick = { logoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onClear = { ticketViewModel.updateTicket { it.copy(logoUri = null) } }
            )
            MediaPickerRowCard(
                icon = Icons.Default.Draw,
                title = "সার্ভেয়ারের স্বাক্ষর",
                subtitle = if (t.signatureUri != null) "স্বাক্ষর যুক্ত হয়েছে — নিচে ছাপা হবে" else "রিপোর্টের নিচে ডানদিকে ছাপা হবে",
                hasValue = t.signatureUri != null,
                onPick = { signaturePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onClear = { ticketViewModel.updateTicket { it.copy(signatureUri = null) } }
            )

            // Section 4: পদবি, যোগ্যতা ও জরিপের ধরন
            SectionHeader("পদবি ও বিবরণ")
            OutlinedTextField(
                value = t.designation,
                onValueChange = { v -> ticketViewModel.updateTicket { it.copy(designation = v) } },
                label = { Text("পদবি (যেমন ডিজিটাল ভূমি সার্ভেয়ার)") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = t.qualifications,
                onValueChange = { v -> ticketViewModel.updateTicket { it.copy(qualifications = v) } },
                label = { Text("যোগ্যতা ও সনদ (প্রতি লাইনে একটি)") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = t.surveyType,
                onValueChange = { v -> ticketViewModel.updateTicket { it.copy(surveyType = v) } },
                label = { Text("জরিপের ধরন (যেমন বি.আর.এস)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = t.slogan,
                onValueChange = { v -> ticketViewModel.updateTicket { it.copy(slogan = v) } },
                label = { Text("স্লোগান (ঐচ্ছিক)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = t.reportNote,
                onValueChange = { v -> ticketViewModel.updateTicket { it.copy(reportNote = v) } },
                label = { Text("রিপোর্ট নোট / মন্তব্য") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = t.footerLine,
                onValueChange = { v -> ticketViewModel.updateTicket { it.copy(footerLine = v) } },
                label = { Text("ফুটার লাইন") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = t.website,
                onValueChange = { v -> ticketViewModel.updateTicket { it.copy(website = v) } },
                label = { Text("ওয়েবসাইট") },
                leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = t.qrLink,
                onValueChange = { v -> ticketViewModel.updateTicket { it.copy(qrLink = v) } },
                label = { Text("QR কোড লিংক") },
                leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun MediaPickerRowCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    hasValue: Boolean,
    onPick: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .clickable(onClick = onPick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (hasValue) BhumiEmeraldContainer else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = if (hasValue) BhumiEmerald else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (hasValue) {
            IconButton(onClick = onClear) {
                Icon(Icons.Default.Close, contentDescription = "মুছুন", tint = MaterialTheme.colorScheme.error)
            }
        }
        TextButton(onClick = onPick) {
            Text(
                text = if (hasValue) "পরিবর্তন" else "যোগ করুন",
                color = BhumiOrangeCta,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
