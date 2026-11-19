package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ExportFormat
import com.example.domain.BengaliFormatters
import com.example.ui.viewmodel.LandSurveyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportCenterScreen(
    viewModel: LandSurveyViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val project by viewModel.uiState.collectAsStateWithLifecycle()
    val statusMsg by viewModel.statusMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMsg?.id) {
        val m = statusMsg ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(m.text)
        viewModel.clearStatusMessage()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                    }
                },
                title = { Text("রপ্তানি কেন্দ্র (Export Center)") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "প্রজেক্ট: ${project.mouzaName} • মোট প্লট: ${BengaliFormatters.toBengaliDigits(project.plots.size)}",
                style = MaterialTheme.typography.titleMedium
            )
            ExportFormat.entries.forEach { format ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(format.labelBn, style = MaterialTheme.typography.titleMedium)
                            Text(".${format.extension} • ${format.mimeType}", style = MaterialTheme.typography.labelSmall)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { viewModel.exportProjectInFormat(context, format, shareAfter = false) }) {
                                Icon(Icons.Default.Download, contentDescription = "ডাউনলোড")
                            }
                            Button(onClick = { viewModel.exportProjectInFormat(context, format, shareAfter = true) }) {
                                Icon(Icons.Default.Share, contentDescription = "শেয়ার")
                            }
                        }
                    }
                }
            }
        }
    }
}
