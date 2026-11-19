package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.AnaGondaHelper
import com.example.domain.BangladeshiUnitConverter
import com.example.domain.BengaliFormatters
import com.example.domain.LandAreaUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnaGondaCalculatorScreen(
    defaultShotokFactor: Double = 435.6,
    onBack: () -> Unit
) {
    var totalShotokText by remember { mutableStateOf("100") }
    var anaText by remember { mutableStateOf("4") }
    var gondaText by remember { mutableStateOf("0") }
    var koraText by remember { mutableStateOf("0") }
    var krantiText by remember { mutableStateOf("0") }
    var tilText by remember { mutableStateOf("0") }

    val breakdown = remember(totalShotokText, anaText, gondaText, koraText, krantiText, tilText, defaultShotokFactor) {
        AnaGondaHelper.calculateShare(
            totalShotok = BengaliFormatters.parseDoubleFlexible(totalShotokText) ?: 0.0,
            ana = BengaliFormatters.parseIntFlexible(anaText) ?: 0,
            gonda = BengaliFormatters.parseIntFlexible(gondaText) ?: 0,
            kora = BengaliFormatters.parseIntFlexible(koraText) ?: 0,
            kranti = BengaliFormatters.parseIntFlexible(krantiText) ?: 0,
            til = BengaliFormatters.parseIntFlexible(tilText) ?: 0,
            sqFeetPerShotok = defaultShotokFactor
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                    }
                },
                title = { Text("আনা-গন্ডা ক্যালকুলেটর") }
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
            OutlinedTextField(
                value = totalShotokText,
                onValueChange = { totalShotokText = it },
                label = { Text("খতিয়ানের মোট জমি (শতক)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = anaText, onValueChange = { anaText = it }, label = { Text("আনা") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = gondaText, onValueChange = { gondaText = it }, label = { Text("গন্ডা") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = koraText, onValueChange = { koraText = it }, label = { Text("কড়া") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = krantiText, onValueChange = { krantiText = it }, label = { Text("ক্রান্তি") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = tilText, onValueChange = { tilText = it }, label = { Text("তিল") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("ফলাফল (১ শতক = ${BengaliFormatters.formatDecimal(defaultShotokFactor, 1)} বর্গফুট)", fontWeight = FontWeight.Bold)
                    Text("দশমিক অংশ: ${BengaliFormatters.formatDecimal(breakdown.decimalFraction, 6)}")
                    Text("প্রাপ্য জমি: ${BengaliFormatters.formatDecimal(breakdown.shareShotok, 4)} শতক", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text("বর্গফুটে: ${BengaliFormatters.formatDecimal(breakdown.shareSqFt, 2)} বর্গফুট")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UnitConverterScreen(
    defaultShotokFactor: Double = 435.6,
    onBack: () -> Unit
) {
    var inputText by remember { mutableStateOf("10") }
    var selectedUnit by remember { mutableStateOf(LandAreaUnit.SHOTOK) }

    val inputVal = BengaliFormatters.parseDoubleFlexible(inputText) ?: 0.0
    val converted = remember(inputVal, selectedUnit, defaultShotokFactor) {
        BangladeshiUnitConverter.convertAll(inputVal, selectedUnit, defaultShotokFactor)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                    }
                },
                title = { Text("জমি একক রূপান্তর") }
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
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text("পরিমাণ লিখুন") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LandAreaUnit.entries.forEach { u ->
                    FilterChip(
                        selected = selectedUnit == u,
                        onClick = { selectedUnit = u },
                        label = { Text(u.labelBn) }
                    )
                }
            }
            converted.forEach { (unit, value) ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(unit.labelBn, fontWeight = FontWeight.Medium)
                        Text(BengaliFormatters.formatDecimal(value, 4), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
