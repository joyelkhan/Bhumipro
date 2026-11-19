package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.BengaliFormatters
import com.example.domain.UnknownSideCalculator
import com.example.ui.theme.BhumiEmerald
import com.example.ui.theme.BhumiOrangeCta

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnknownSideAreaScreen(
    defaultShotokFactor: Double = 435.6,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                    }
                },
                title = { Text("ক্ষেত্রফল ও অজানা বাহু", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("ক্ষেত্রফল নির্ণয়") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("অজানা বাহু নির্ণয়") })
            }

            if (selectedTab == 0) {
                AreaHeronAndQuadTab(defaultShotokFactor)
            } else {
                MissingSideSolverTab(defaultShotokFactor)
            }
        }
    }
}

@Composable
private fun AreaHeronAndQuadTab(defaultShotokFactor: Double) {
    var shapeMode by remember { mutableStateOf("quad") } // quad | triangle
    var aText by remember { mutableStateOf("60") }
    var bText by remember { mutableStateOf("58") }
    var cText by remember { mutableStateOf("45") }
    var dText by remember { mutableStateOf("44") }
    var diagText by remember { mutableStateOf("74") }

    val a = BengaliFormatters.parseDoubleFlexible(aText) ?: 0.0
    val b = BengaliFormatters.parseDoubleFlexible(bText) ?: 0.0
    val c = BengaliFormatters.parseDoubleFlexible(cText) ?: 0.0
    val d = BengaliFormatters.parseDoubleFlexible(dText) ?: 0.0
    val diag = BengaliFormatters.parseDoubleFlexible(diagText)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = shapeMode == "quad",
                onClick = { shapeMode = "quad" },
                label = { Text("৪-বাহু (চতুর্ভুজ ও কর্ণ)") }
            )
            FilterChip(
                selected = shapeMode == "triangle",
                onClick = { shapeMode = "triangle" },
                label = { Text("৩-বাহু (হেরনের ত্রিভুজ)") }
            )
        }

        if (shapeMode == "triangle") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = aText, onValueChange = { aText = it }, label = { Text("বাহু ক (ফুট)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = bText, onValueChange = { bText = it }, label = { Text("বাহু খ (ফুট)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = cText, onValueChange = { cText = it }, label = { Text("বাহু গ (ফুট)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
            }
            val tri = remember(a, b, c, defaultShotokFactor) {
                UnknownSideCalculator.calculateTriangleHeron(a, b, c, defaultShotokFactor)
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!tri.isValid) {
                        Text(tri.errorBn ?: "সঠিক মান দিন", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                    } else {
                        Text("হেরনের সূত্র অনুযায়ী নির্ভুল ক্ষেত্রফল", fontWeight = FontWeight.Bold)
                        Text("${BengaliFormatters.formatDecimal(tri.areaShotok, 4)} শতক", style = MaterialTheme.typography.headlineSmall, color = BhumiEmerald)
                        Text("${BengaliFormatters.formatDecimal(tri.areaSqFt, 2)} বর্গফুট • ${BengaliFormatters.formatDecimal(tri.areaKatha, 3)} কাঠা")
                        Text("পরিসীমা: ${BengaliFormatters.formatDecimal(tri.perimeterFeet, 2)} ফুট")
                    }
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = aText, onValueChange = { aText = it }, label = { Text("উত্তর (ফুট)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = bText, onValueChange = { bText = it }, label = { Text("দক্ষিণ (ফুট)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = cText, onValueChange = { cText = it }, label = { Text("পূর্ব (ফুট)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = dText, onValueChange = { dText = it }, label = { Text("পশ্চিম (ফুট)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(
                value = diagText,
                onValueChange = { diagText = it },
                label = { Text("কর্ণের দৈর্ঘ্য / কোণ-থেকে-কোণ (ঐচ্ছিক, নির্ভুল মাপের জন্য)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            val quad = remember(a, b, c, d, diag, defaultShotokFactor) {
                UnknownSideCalculator.calculateQuadrilateral(a, b, c, d, diag, defaultShotokFactor)
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!quad.isValid) {
                        Text(quad.errorBn ?: "সঠিক মান দিন", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                    } else {
                        Text("নির্ভুল কর্ণ-বিভক্ত ক্ষেত্রফল", fontWeight = FontWeight.Bold)
                        Text("${BengaliFormatters.formatDecimal(quad.exactAreaShotok, 4)} শতক", style = MaterialTheme.typography.headlineSmall, color = BhumiEmerald)
                        Text("${BengaliFormatters.formatDecimal(quad.exactAreaSqFt, 2)} বর্গফুট")
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            "গড় পদ্ধতি ((উ+দ)/২ × (পূ+প)/২): ${BengaliFormatters.formatDecimal(quad.averageAreaShotok, 4)} শতক",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (quad.differenceShotok > 0.0) {
                            Text(
                                "তফাৎ: ${BengaliFormatters.formatDecimal(quad.differenceShotok, 4)} শতক",
                                style = MaterialTheme.typography.labelLarge,
                                color = BhumiOrangeCta
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MissingSideSolverTab(defaultShotokFactor: Double) {
    var targetShotokText by remember { mutableStateOf("5.0") }
    var knownSideText by remember { mutableStateOf("40.0") }
    var oppositeSideText by remember { mutableStateOf("") }

    val targetShotok = BengaliFormatters.parseDoubleFlexible(targetShotokText) ?: 0.0
    val knownSide = BengaliFormatters.parseDoubleFlexible(knownSideText) ?: 0.0
    val oppSide = BengaliFormatters.parseDoubleFlexible(oppositeSideText)

    val result = remember(targetShotok, knownSide, oppSide, defaultShotokFactor) {
        UnknownSideCalculator.solveMissingSide(targetShotok, knownSide, oppSide, defaultShotokFactor)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "কাঙ্ক্ষিত পরিমাণ জমি পেতে জানা প্রস্থের বিপরীতে কতটুকু দৈর্ঘ্য/গভীরতা মাপতে হবে তা নির্ণয় করুন।",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = targetShotokText,
            onValueChange = { targetShotokText = it },
            label = { Text("প্রাপ্য / কাঙ্ক্ষিত জমি (শতক)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = knownSideText,
            onValueChange = { knownSideText = it },
            label = { Text("জানা পাশ / প্রস্থ ১ (ফুট)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = oppositeSideText,
            onValueChange = { oppositeSideText = it },
            label = { Text("বিপরীত সমান্তরাল পাশ / প্রস্থ ২ (ফুট — ট্রাপিজিয়ামের জন্য ঐচ্ছিক)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!result.isValid) {
                    Text(result.errorBn ?: "সঠিক মান দিন", color = MaterialTheme.colorScheme.error)
                } else {
                    Text("প্রয়োজনীয় অজানা বাহু / দৈর্ঘ্য", fontWeight = FontWeight.Bold)
                    Text(
                        result.formattedLengthFtIn,
                        style = MaterialTheme.typography.headlineSmall,
                        color = BhumiOrangeCta
                    )
                    Text("দশমিক ফুট: ${BengaliFormatters.formatDecimal(result.requiredLengthFeet, 3)} ফু")
                    Text("মোট বর্গফুট (১ শতক = ${BengaliFormatters.formatDecimal(defaultShotokFactor, 1)}): ${BengaliFormatters.formatDecimal(result.totalSqFt, 2)} ফু²")
                }
            }
        }
    }
}
