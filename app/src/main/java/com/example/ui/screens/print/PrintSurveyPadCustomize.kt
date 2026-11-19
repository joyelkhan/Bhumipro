package com.example.ui.screens.print

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.PrintPresetEntity
import com.example.data.model.AreaDisplayUnit
import com.example.data.model.CanvasDisplaySettings
import com.example.data.model.GisLayerItem
import com.example.data.model.MapTextPosition
import com.example.data.model.MeasurementUnit
import com.example.data.model.PaperSize
import com.example.data.model.PrintConfig
import com.example.data.model.PrintOrientation
import com.example.domain.BengaliFormatters
import com.example.ui.components.print.PrintCustomizePanel
import com.example.ui.theme.BhumiProPrintColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PrintSurveyPadCustomizePanel(
    printConfig: PrintConfig,
    canvasSettings: CanvasDisplaySettings,
    layers: List<GisLayerItem>,
    presets: List<PrintPresetEntity>,
    onUpdatePrintConfig: ((PrintConfig) -> PrintConfig) -> Unit,
    onUpdateCanvasSettings: ((CanvasDisplaySettings) -> CanvasDisplaySettings) -> Unit,
    onToggleGridLayer: () -> Unit,
    onSaveGlobal: () -> Unit,
    onSaveProject: () -> Unit,
    onClearProjectOverride: () -> Unit,
    onSavePreset: (String) -> Unit,
    onApplyPreset: (String) -> Unit,
    onDeletePreset: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var presetName by remember { mutableStateOf("") }
    var marginText by remember(printConfig.marginMm) { mutableStateOf(printConfig.marginMm.toString()) }
    var customWText by remember(printConfig.customPaperWidthPt) { mutableStateOf(printConfig.customPaperWidthPt.toInt().toString()) }
    var customHText by remember(printConfig.customPaperHeightPt) { mutableStateOf(printConfig.customPaperHeightPt.toInt().toString()) }

    val gridVisible = layers.firstOrNull { it.id == "layer_grid" }?.visible ?: false

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. কাগজ ও লেআউট
        ExpandableCustomizeSection("১. কাগজ ও লেআউট", initiallyExpanded = true) {
            Text("কাগজের মাপ", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PaperSize.entries.forEach { size ->
                    FilterChip(
                        selected = printConfig.paperSize == size,
                        onClick = { onUpdatePrintConfig { it.copy(paperSize = size) } },
                        label = { Text(size.labelBn) }
                    )
                }
            }
            if (printConfig.paperSize == PaperSize.CUSTOM) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customWText,
                        onValueChange = {
                            customWText = it
                            BengaliFormatters.parseDoubleFlexible(it)?.let { w ->
                                onUpdatePrintConfig { c -> c.copy(customPaperWidthPt = w.toFloat()) }
                            }
                        },
                        label = { Text("কাস্টম প্রস্থ (pt)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = customHText,
                        onValueChange = {
                            customHText = it
                            BengaliFormatters.parseDoubleFlexible(it)?.let { h ->
                                onUpdatePrintConfig { c -> c.copy(customPaperHeightPt = h.toFloat()) }
                            }
                        },
                        label = { Text("কাস্টম উচ্চতা (pt)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Text("অরিয়েন্টেশন", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrintOrientation.entries.forEach { ori ->
                    FilterChip(
                        selected = printConfig.orientation == ori,
                        onClick = { onUpdatePrintConfig { it.copy(orientation = ori) } },
                        label = { Text(ori.labelBn) }
                    )
                }
            }

            OutlinedTextField(
                value = marginText,
                onValueChange = {
                    marginText = it
                    BengaliFormatters.parseDoubleFlexible(it)?.let { mm ->
                        onUpdatePrintConfig { c -> c.copy(marginMm = mm.toFloat().coerceIn(4f, 30f)) }
                    }
                },
                label = { Text("মার্জিন (মিমি)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            ToggleRow("পৃষ্ঠা নম্বর", printConfig.showPageNumbers) { v ->
                onUpdatePrintConfig { it.copy(showPageNumbers = v) }
            }
            ToggleRow("প্রতি প্লট আলাদা পাতা", printConfig.individualPlotPages) { v ->
                onUpdatePrintConfig { it.copy(individualPlotPages = v) }
            }
        }

        // 2. ম্যাপ কন্টেন্ট ও প্লট
        ExpandableCustomizeSection("২. ম্যাপ কন্টেন্ট ও প্লট") {
            Text("ম্যাপ কন্টেন্ট মোড", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("full" to "পূর্ণ ম্যাপ", "selected" to "সক্রিয় প্লট", "crop" to "কনটেন্টে ক্রপ").forEach { (id, lbl) ->
                    FilterChip(
                        selected = printConfig.mapContentMode == id,
                        onClick = { onUpdatePrintConfig { it.copy(mapContentMode = id) } },
                        label = { Text(lbl) }
                    )
                }
            }
            ToggleRow("ভারটেক্স নম্বর দেখান (১,২,৩…)", printConfig.showVertexNumbers) { v ->
                onUpdatePrintConfig { it.copy(showVertexNumbers = v) }
            }
            Text("প্লট ফিল অস্বচ্ছতা: ${BengaliFormatters.toBengaliDigits((printConfig.plotFillOpacity * 100).toInt())}%")
            Slider(
                value = printConfig.plotFillOpacity,
                onValueChange = { v -> onUpdatePrintConfig { it.copy(plotFillOpacity = v) } },
                valueRange = 0f..1f
            )
            ToggleRow("লেবেল সংঘর্ষ এড়ানো (স্বয়ংক্রিয় সরানো)", printConfig.labelCollisionAvoidance) { v ->
                onUpdatePrintConfig { it.copy(labelCollisionAvoidance = v) }
            }
            ToggleRow("কর্নার পয়েন্ট দেখান (ক্যানভাস মান)", canvasSettings.showCornerPoints) { v ->
                onUpdateCanvasSettings { it.copy(showCornerPoints = v) }
            }
        }

        // 3. একক ও লেবেল
        ExpandableCustomizeSection("৩. একক ও লেবেল") {
            Text("দৈর্ঘ্য একক (সব লেবেল সাথে সাথে বদলাবে)", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MeasurementUnit.entries.forEach { u ->
                    FilterChip(
                        selected = canvasSettings.measurementUnit == u.id,
                        onClick = { onUpdateCanvasSettings { it.copy(measurementUnit = u.id) } },
                        label = { Text(u.bengaliName) }
                    )
                }
            }
            Text("দশমিক নির্ভুলতা (০–৩): ${BengaliFormatters.toBengaliDigits(canvasSettings.decimalPrecision)}")
            Slider(
                value = canvasSettings.decimalPrecision.toFloat(),
                onValueChange = { v -> onUpdateCanvasSettings { it.copy(decimalPrecision = v.toInt().coerceIn(0, 3)) } },
                valueRange = 0f..3f,
                steps = 2
            )

            Text("ক্ষেত্রফল একক", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AreaDisplayUnit.entries.forEach { au ->
                    FilterChip(
                        selected = printConfig.areaUnitId == au.id,
                        onClick = { onUpdatePrintConfig { it.copy(areaUnitId = au.id) } },
                        label = { Text(au.labelBn) }
                    )
                }
            }
            Text(
                "শতক রূপান্তর ফ্যাক্টর ক্যানভাসের সেটিং অনুযায়ী লাইভ প্রয়োগ হয়",
                style = MaterialTheme.typography.labelSmall,
                color = BhumiProPrintColors.InkSecondary
            )

            ToggleRow("পরিধি দেখান (পরিধিঃ …)", printConfig.showPerimeterLabel) { v ->
                onUpdatePrintConfig { it.copy(showPerimeterLabel = v) }
            }
            ToggleRow("মোট ক্ষেত্রফল সারি দেখান", printConfig.showTotalAreaLabel) { v ->
                onUpdatePrintConfig { it.copy(showTotalAreaLabel = v) }
            }
            ToggleRow("নির্বাচিত প্লটের লেবেল দেখান", printConfig.showSelectedAreaLabel) { v ->
                onUpdatePrintConfig { it.copy(showSelectedAreaLabel = v) }
            }
            ToggleRow("ডাইমেনশন লেবেল (মাস্টার)", printConfig.sectionLayout.showDimensionLabels) { v ->
                onUpdatePrintConfig { it.copy(sectionLayout = it.sectionLayout.copy(showDimensionLabels = v)) }
            }

            Text("স্কেল বারের একক", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = printConfig.scaleBarUnitId == "inherit",
                    onClick = { onUpdatePrintConfig { it.copy(scaleBarUnitId = "inherit") } },
                    label = { Text("ক্যানভাস") }
                )
                listOf(
                    MeasurementUnit.FEET,
                    MeasurementUnit.METERS,
                    MeasurementUnit.LINKS,
                    MeasurementUnit.CHAINS,
                    MeasurementUnit.HAT
                ).forEach { u ->
                    FilterChip(
                        selected = printConfig.scaleBarUnitId == u.id,
                        onClick = { onUpdatePrintConfig { it.copy(scaleBarUnitId = u.id) } },
                        label = { Text(u.bengaliName) }
                    )
                }
            }
        }

        // 4. CAD স্টাইল (ক্যানভাস লাইভ)
        ExpandableCustomizeSection("৪. CAD স্টাইল (ক্যানভাস লাইভ)") {
            PercentSlider("ফন্ট সাইজ গুণক", canvasSettings.fontScale, 0.4f..3f) { v ->
                onUpdateCanvasSettings { it.copy(fontScale = v) }
            }
            PercentSlider("লাইন প্রস্থ গুণক", canvasSettings.lineScale, 0.4f..3f) { v ->
                onUpdateCanvasSettings { it.copy(lineScale = v) }
            }
            PercentSlider("আউটলাইন গুণক", canvasSettings.outlineScale, 0.1f..2f) { v ->
                onUpdateCanvasSettings { it.copy(outlineScale = v) }
            }
            PercentSlider("পয়েন্ট সাইজ গুণক", canvasSettings.pointScale, 0.4f..3f) { v ->
                onUpdateCanvasSettings { it.copy(pointScale = v) }
            }
            PercentSlider("মাপের লেখা প্রস্থ গুণক", canvasSettings.measurementStrokeScale, 0.4f..3f) { v ->
                onUpdateCanvasSettings { it.copy(measurementStrokeScale = v) }
            }

            Text("মাপের লেখার অবস্থান", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MapTextPosition.entries.forEach { pos ->
                    FilterChip(
                        selected = canvasSettings.mapTextPosition == pos.id,
                        onClick = { onUpdateCanvasSettings { it.copy(mapTextPosition = pos.id) } },
                        label = { Text(pos.labelBn) }
                    )
                }
            }

            ToggleRow("ম্যাপের লাইন বাইরে দেখান", canvasSettings.showMeasurementsOutside) { v ->
                onUpdateCanvasSettings { it.copy(showMeasurementsOutside = v) }
            }
            ToggleRow("বিভক্ত বাহুর মোট মাপ ভিতরে", canvasSettings.showSplitTotalInside) { v ->
                onUpdateCanvasSettings { it.copy(showSplitTotalInside = v) }
            }
            ToggleRow("সার্ভে গ্রিড দেখান (layer_grid)", gridVisible) { _ ->
                onToggleGridLayer()
            }
            ToggleRow("ক্রসহেয়ার দেখান", canvasSettings.showCrosshair) { v ->
                onUpdateCanvasSettings { it.copy(showCrosshair = v) }
            }
            Text("গ্রিড স্পেসিং: ${BengaliFormatters.toBengaliDigits(canvasSettings.gridSpacingUnits.toInt())}")
            Slider(
                value = canvasSettings.gridSpacingUnits,
                onValueChange = { v -> onUpdateCanvasSettings { it.copy(gridSpacingUnits = v) } },
                valueRange = 8f..160f
            )

            HorizontalDivider()
            Text("ম্যাপ ফিল্টার (ক্যানভাসের মতোই কাগজে)", style = MaterialTheme.typography.labelLarge)
            Text("ব্রাইটনেস: ${BengaliFormatters.formatDecimal(canvasSettings.brightness.toDouble(), 2)}")
            Slider(
                value = canvasSettings.brightness,
                onValueChange = { v -> onUpdateCanvasSettings { it.copy(brightness = v) } },
                valueRange = -0.5f..0.5f
            )
            Text("কনট্রাস্ট: ${BengaliFormatters.formatDecimal(canvasSettings.contrast.toDouble(), 2)}")
            Slider(
                value = canvasSettings.contrast,
                onValueChange = { v -> onUpdateCanvasSettings { it.copy(contrast = v) } },
                valueRange = -0.5f..0.5f
            )
            ToggleRow("গ্রেস্কেল", canvasSettings.grayscale) { v ->
                onUpdateCanvasSettings { it.copy(grayscale = v) }
            }
            PercentSlider("ম্যাপ অস্বচ্ছতা", canvasSettings.mapOpacity, 0.05f..1f) { v ->
                onUpdateCanvasSettings { it.copy(mapOpacity = v) }
            }
        }

        // 5. অফিসিয়াল উপাদান
        ExpandableCustomizeSection("৫. অফিসিয়াল উপাদান") {
            Text("নর্থ অ্যারো সাইজ: ${BengaliFormatters.toBengaliDigits(printConfig.northArrowSizePercent)}%")
            Slider(
                value = printConfig.northArrowSizePercent.toFloat(),
                onValueChange = { v -> onUpdatePrintConfig { it.copy(northArrowSizePercent = v.toInt().coerceIn(60, 160)) } },
                valueRange = 60f..160f
            )
            ToggleRow("ওয়াটারমার্ক", printConfig.showWatermark) { v ->
                onUpdatePrintConfig { it.copy(showWatermark = v) }
            }
            ToggleRow("QR কোড", printConfig.showQrCode) { v ->
                onUpdatePrintConfig { it.copy(showQrCode = v) }
            }
            ToggleRow("বারকোড", printConfig.showBarcode) { v ->
                onUpdatePrintConfig { it.copy(showBarcode = v) }
            }
            ToggleRow("লিজেন্ড", printConfig.showLegend) { v ->
                onUpdatePrintConfig { it.copy(showLegend = v) }
            }
            ToggleRow("ক্ষেত্রফল সারসংক্ষেপ", printConfig.showAreaSummary) { v ->
                onUpdatePrintConfig { it.copy(showAreaSummary = v) }
            }
            ToggleRow("হেডার ও পরিচিতি", printConfig.showHeaderAndInfo) { v ->
                onUpdatePrintConfig { it.copy(showHeaderAndInfo = v) }
            }
            ToggleRow("ফুটার ও পাতা", printConfig.showFooterAndPage) { v ->
                onUpdatePrintConfig { it.copy(showFooterAndPage = v) }
            }
        }

        // 6. রিপোর্ট সেকশন ও সীমানা রং
        ExpandableCustomizeSection("৬. রিপোর্ট সেকশন ও সীমানা রং") {
            PrintCustomizePanel(
                config = printConfig,
                onUpdateConfig = onUpdatePrintConfig,
                onSaveGlobal = onSaveGlobal,
                onSaveProject = onSaveProject,
                onClearProjectOverride = onClearProjectOverride
            )
        }

        // 7. প্রিন্ট প্রিসেট
        ExpandableCustomizeSection("৭. প্রিন্ট প্রিসেট") {
            if (presets.isEmpty()) {
                Text(
                    "কোনো প্রিসেট সংরক্ষিত নেই — নিচে নাম দিয়ে সেভ করুন",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BhumiProPrintColors.InkSecondary
                )
            } else {
                presets.forEach { preset ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(BhumiProPrintColors.FieldBg)
                            .border(1.dp, BhumiProPrintColors.FieldBorder, RoundedCornerShape(8.dp))
                            .clickable { onApplyPreset(preset.id) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(preset.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        OutlinedButton(onClick = { onApplyPreset(preset.id) }) {
                            Text("প্রয়োগ")
                        }
                        IconButton(onClick = { onDeletePreset(preset.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "মুছুন", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("নতুন প্রিসেটের নাম") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        if (presetName.isNotBlank()) {
                            onSavePreset(presetName)
                            presetName = ""
                        }
                    }
                ) { Text("সেভ") }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ExpandableCustomizeSection(
    title: String,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BhumiProPrintColors.CardWhite)
            .border(1.dp, BhumiProPrintColors.CardBorder, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HorizontalDivider()
                content()
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PercentSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column {
        Text("$label: ${BengaliFormatters.toBengaliDigits((value * 100).toInt())}%")
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}
