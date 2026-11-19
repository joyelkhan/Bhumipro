package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CanvasDisplaySettings
import com.example.data.model.LandPlot
import com.example.data.model.MapTextPosition
import com.example.data.model.SCAN_DPI_AUTO
import com.example.data.model.SCAN_DPI_CUSTOM
import com.example.data.model.SurveyProject
import com.example.data.model.ZoomTextMode
import com.example.data.model.mouzaScalePresets
import com.example.data.model.scanDpiPresets
import com.example.domain.BengaliFormatters
import com.example.ui.theme.BhumiBlue
import com.example.ui.theme.BhumiBlueContainer
import com.example.ui.theme.BhumiEmerald
import com.example.ui.theme.BhumiEmeraldContainer
import com.example.ui.theme.BhumiOrangeCta
import com.example.ui.theme.BhumiOrangeSoft

@Composable
fun ChooseSourceModal(
    onPickImage: () -> Unit,
    onPickPdfOrFile: () -> Unit,
    onPickDxfCad: () -> Unit,
    onLoadSampleDxf: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ম্যাপ সোর্স নির্বাচন করুন", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "আপনার ডিভাইস থেকে স্ক্যান করা মৌজা ম্যাপ (PNG/JPG/PDF) অথবা CAD (DXF) ফাইল ইমপোর্ট করুন।",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = onPickImage,
                    colors = ButtonDefaults.buttonColors(containerColor = BhumiOrangeCta),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("গ্যালারি থেকে ম্যাপ ছবি (ফটো পিকার)")
                }
                OutlinedButton(onClick = onPickPdfOrFile, modifier = Modifier.fillMaxWidth()) {
                    Text("ফাইল থেকে PDF / স্ক্যান ম্যাপ")
                }
                OutlinedButton(onClick = onPickDxfCad, modifier = Modifier.fillMaxWidth()) {
                    Text("DXF / CAD ওভারলে ফাইল")
                }
                TextButton(onClick = onLoadSampleDxf, modifier = Modifier.fillMaxWidth()) {
                    Text("নমুনা DXF স্তর লোড করুন")
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("বন্ধ করুন") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaleCalibrationModal(
    currentScaleId: String,
    currentScanDpi: Int,
    onApplyPreset: (scaleId: String, scanDpi: Int, customDpi: Int?, customFeetPerInch: Double?) -> Unit,
    onStartTwoPointCalibration: () -> Unit,
    onDismiss: () -> Unit,
    isPdfSource: Boolean = false
) {
    var selectedScaleId by remember(currentScaleId) { mutableStateOf(currentScaleId) }
    var useCustomScale by remember { mutableStateOf(false) }
    var selectedDpi by remember(currentScanDpi) { mutableStateOf(currentScanDpi) }
    var customDpiText by remember { mutableStateOf(if (currentScanDpi > 0) currentScanDpi.toString() else "300") }
    var customFeetPerInchText by remember { mutableStateOf("") }

    var scaleDropdownExpanded by remember { mutableStateOf(false) }
    var dpiDropdownExpanded by remember { mutableStateOf(false) }

    val selectedScaleLabel = if (useCustomScale) {
        "কাস্টম স্কেল (ফুট/ইঞ্চি)"
    } else {
        mouzaScalePresets.firstOrNull { it.id == selectedScaleId }?.labelBn
            ?: mouzaScalePresets.first().labelBn
    }
    val selectedDpiLabel = if (isPdfSource) {
        "স্বয়ংক্রিয় (${BengaliFormatters.toBengaliDigits(if (selectedDpi > 0) selectedDpi else 300)} DPI)"
    } else {
        scanDpiPresets.firstOrNull { it.dpi == selectedDpi }?.labelBn
            ?: "${BengaliFormatters.toBengaliDigits(selectedDpi)} DPI"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Straighten, contentDescription = null, tint = BhumiOrangeCta)
                Text("স্কেল সেটিংস", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Method 1 Blue-Outlined Card (Mockup #3 / Workflow 4)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.5.dp, BhumiBlue, RoundedCornerShape(12.dp))
                        .background(BhumiBlueContainer.copy(alpha = 0.2f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "পদ্ধতি ১ — মৌজার স্কেল",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = BhumiBlue
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(BhumiEmerald)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "সর্বাধিক নিখুঁত",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text("ম্যাপের স্কেল নির্বাচন করুন", style = MaterialTheme.typography.labelLarge)
                    ExposedDropdownMenuBox(
                        expanded = scaleDropdownExpanded,
                        onExpandedChange = { scaleDropdownExpanded = !scaleDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedScaleLabel,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = scaleDropdownExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = scaleDropdownExpanded,
                            onDismissRequest = { scaleDropdownExpanded = false }
                        ) {
                            mouzaScalePresets.forEach { preset ->
                                DropdownMenuItem(
                                    text = { Text(preset.labelBn) },
                                    onClick = {
                                        selectedScaleId = preset.id
                                        useCustomScale = false
                                        customFeetPerInchText = ""
                                        scaleDropdownExpanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("কাস্টম ফুট/ইঞ্চি") },
                                onClick = {
                                    useCustomScale = true
                                    scaleDropdownExpanded = false
                                }
                            )
                        }
                    }

                    if (useCustomScale) {
                        OutlinedTextField(
                            value = customFeetPerInchText,
                            onValueChange = { customFeetPerInchText = it },
                            label = { Text("কাস্টম ফুট / ইঞ্চি (যেমন 330)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("স্ক্যান DPI", style = MaterialTheme.typography.labelLarge)
                        if (isPdfSource) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "PDF-এর জন্য বন্ধ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else if (selectedDpi == SCAN_DPI_AUTO) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "স্বয়ংক্রিয়",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = dpiDropdownExpanded && !isPdfSource,
                        onExpandedChange = {
                            if (!isPdfSource) dpiDropdownExpanded = !dpiDropdownExpanded
                        }
                    ) {
                        OutlinedTextField(
                            value = selectedDpiLabel,
                            onValueChange = {},
                            readOnly = true,
                            enabled = !isPdfSource,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = dpiDropdownExpanded && !isPdfSource)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        if (!isPdfSource) {
                            ExposedDropdownMenu(
                                expanded = dpiDropdownExpanded,
                                onDismissRequest = { dpiDropdownExpanded = false }
                            ) {
                                scanDpiPresets.forEach { dpiPreset ->
                                    DropdownMenuItem(
                                        text = { Text(dpiPreset.labelBn) },
                                        onClick = {
                                            selectedDpi = dpiPreset.dpi
                                            dpiDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (!isPdfSource && selectedDpi == SCAN_DPI_CUSTOM) {
                        OutlinedTextField(
                            value = customDpiText,
                            onValueChange = { customDpiText = it },
                            label = { Text("কাস্টম DPI (৫০–২৪০০)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Text(
                        if (isPdfSource) {
                            "PDF ফাইলের জন্য সঠিক স্কেল স্বয়ংক্রিয়ভাবে নির্ধারিত হয়। ছবি আপলোড করলে DPI নির্বাচন করা যাবে।"
                        } else {
                            "ছবি আপলোড করলে সঠিক স্ক্যান DPI এখান থেকে নির্বাচন করুন।"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Method 2: Two-Point Calibration
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("পদ্ধতি ২ — ম্যাপে ২-পয়েন্ট ক্যালিব্রেশন", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    OutlinedButton(
                        onClick = onStartTwoPointCalibration,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ম্যাপে ২-পয়েন্ট ট্যাপ করে ক্যালিব্রেট করুন")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val customDpi = if (selectedDpi == SCAN_DPI_CUSTOM) {
                        BengaliFormatters.parseIntFlexible(customDpiText) ?: 300
                    } else null
                    val customFpi = if (useCustomScale) {
                        BengaliFormatters.parseDoubleFlexible(customFeetPerInchText)
                    } else null
                    onApplyPreset(selectedScaleId, selectedDpi, customDpi, customFpi)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BhumiOrangeCta)
            ) { Text("সেভ করুন", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল", color = BhumiOrangeCta)
            }
        }
    )
}

@Composable
fun CalibrationDistanceDialog(
    pixelDistance: Double,
    onApply: (Double) -> Unit,
    onCancel: () -> Unit
) {
    var feetInput by remember { mutableStateOf("100") }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("২-পয়েন্ট দূরত্ব নির্ধারণ") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("নির্বাচিত পিক্সেল দূরত্ব: ${BengaliFormatters.formatDecimal(pixelDistance, 1)} px")
                OutlinedTextField(
                    value = feetInput,
                    onValueChange = { feetInput = it },
                    label = { Text("বাস্তব দূরত্ব (ফুট)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val feet = BengaliFormatters.parseDoubleFlexible(feetInput) ?: 0.0
                    if (feet > 0.0) onApply(feet)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BhumiOrangeCta)
            ) { Text("ক্যালিব্রেট করুন", color = Color.White) }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("বাতিল") } }
    )
}

@Composable
fun ShotokStandardModal(
    currentFactor: Double,
    onApply: (factor: Double, saveAsDefault: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var factorText by remember(currentFactor) { mutableStateOf(currentFactor.toString()) }
    var saveAsDefault by remember { mutableStateOf(false) }

    val parsed = BengaliFormatters.parseDoubleFlexible(factorText) ?: currentFactor

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.GridOn, contentDescription = null, tint = BhumiOrangeCta)
                Text("১ শতক সমান কত বর্গফুট?", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BhumiBlueContainer)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = BhumiBlue, modifier = Modifier.size(18.dp))
                    Text(
                        "বাংলাদেশে প্রচলিত মানদণ্ড অনুযায়ী ১ শতক = ৪৩৫.৬ বর্গফুট। আঞ্চলিক পার্থক্যে ৪৩২ বা ৪৩০ বর্গফুটও ব্যবহৃত হয়।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BhumiBlue
                    )
                }

                Text("প্রচলিত মানদণ্ড", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)

                val presets = listOf(
                    Triple(435.6, "৪৩৫.৬ বর্গফুট", "আদর্শ মানদণ্ড (সর্বত্র প্রচলিত)" to true),
                    Triple(432.0, "৪৩২ বর্গফুট", "কিছু আঞ্চলিক পরিমাপে প্রচলিত" to false),
                    Triple(430.0, "৪৩০ বর্গফুট", "নির্দিষ্ট এলাকা ভিত্তিক পরিমাপ" to false)
                )

                presets.forEach { (value, title, subPair) ->
                    val (subtitle, isDefaultBadge) = subPair
                    val selected = kotlin.math.abs(parsed - value) < 0.05
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) BhumiOrangeCta else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .background(if (selected) BhumiOrangeSoft else MaterialTheme.colorScheme.surface)
                            .clickable { factorText = value.toString() }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { factorText = value.toString() },
                            colors = RadioButtonDefaults.colors(selectedColor = BhumiOrangeCta)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(title, fontWeight = FontWeight.Bold)
                                if (isDefaultBadge) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(BhumiEmeraldContainer)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            "ডিফল্ট",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BhumiEmerald,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Text(
                                subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text("কাস্টম মান (বর্গফুট)", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val next = (parsed - 1.0).coerceIn(100.0, 2000.0)
                            factorText = BengaliFormatters.formatDecimal(next, 1, useBengali = false)
                        }
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "কমান")
                    }
                    OutlinedTextField(
                        value = factorText,
                        onValueChange = { factorText = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = {
                            val next = (parsed + 1.0).coerceIn(100.0, 2000.0)
                            factorText = BengaliFormatters.formatDecimal(next, 1, useBengali = false)
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "বাড়ান")
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = saveAsDefault, onCheckedChange = { saveAsDefault = it })
                    Text("অ্যাপের ডিফল্ট মানদণ্ড হিসেবে সংরক্ষণ করুন")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val v = (BengaliFormatters.parseDoubleFlexible(factorText) ?: 435.6).coerceIn(100.0, 2000.0)
                    onApply(v, saveAsDefault)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BhumiOrangeCta)
            ) { Text("প্রয়োগ করুন", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল", color = BhumiOrangeCta)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextLineSizeModal(
    currentFontSizeSp: Float,
    currentStrokeWidthPx: Float,
    onApply: (Float, Float) -> Unit,
    onDismiss: () -> Unit,
    canvasSettings: CanvasDisplaySettings = CanvasDisplaySettings(),
    onUpdateCanvasSettings: (((CanvasDisplaySettings) -> CanvasDisplaySettings) -> Unit)? = null
) {
    var fontSize by remember(currentFontSizeSp) { mutableFloatStateOf(currentFontSizeSp) }
    var strokeWidth by remember(currentStrokeWidthPx) { mutableFloatStateOf(currentStrokeWidthPx) }
    var outlineScale by remember(canvasSettings) { mutableFloatStateOf(canvasSettings.effectiveOutlineScale()) }
    var pointScale by remember(canvasSettings) { mutableFloatStateOf(canvasSettings.effectivePointScale()) }
    var showOutside by remember(canvasSettings) { mutableStateOf(canvasSettings.showMeasurementsOutside) }
    var showSplitTotal by remember(canvasSettings) { mutableStateOf(canvasSettings.showSplitTotalInside) }
    var textPos by remember(canvasSettings) { mutableStateOf(canvasSettings.effectiveTextPosition()) }
    var zoomMode by remember(canvasSettings) { mutableStateOf(canvasSettings.effectiveZoomTextMode()) }

    var posDropdown by remember { mutableStateOf(false) }
    var zoomDropdown by remember { mutableStateOf(false) }

    val activePreset = when {
        kotlin.math.abs(fontSize - 8.5f) < 1f && kotlin.math.abs(strokeWidth - 2.2f) < 0.6f -> "small"
        kotlin.math.abs(fontSize - 11f) < 1f && kotlin.math.abs(strokeWidth - 3f) < 0.6f -> "normal"
        kotlin.math.abs(fontSize - 15f) < 1.5f && kotlin.math.abs(strokeWidth - 4.8f) < 0.8f -> "medium"
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aa লেখা ও লাইনের আকার", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Live Miniature CAD Preview Box (Mockups #4 & #6)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(126.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF1F5F9))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val p1 = Offset(size.width * 0.14f, size.height * 0.72f)
                        val p2 = Offset(size.width * 0.50f, size.height * 0.25f)
                        val p3 = Offset(size.width * 0.86f, size.height * 0.62f)
                        val sw = strokeWidth.dp.toPx().coerceIn(2f, 20f)
                        // White outline
                        drawLine(Color.White, p1, p2, strokeWidth = sw * (1f + outlineScale * 1.4f), cap = StrokeCap.Round)
                        drawLine(Color.White, p2, p3, strokeWidth = sw * (1f + outlineScale * 1.4f), cap = StrokeCap.Round)
                        // Blue main CAD line
                        drawLine(Color(0xFF2563EB), p1, p2, strokeWidth = sw, cap = StrokeCap.Round)
                        drawLine(Color(0xFF2563EB), p2, p3, strokeWidth = sw, cap = StrokeCap.Round)
                        // Corner dots
                        val pr = (3.5f * pointScale).dp.toPx().coerceIn(4f, 22f)
                        listOf(p1, p2, p3).forEach { pt ->
                            drawCircle(Color.White, radius = pr, center = pt)
                            drawCircle(Color(0xFF2563EB), radius = pr, center = pt, style = Stroke(width = 2.dp.toPx()))
                        }
                    }

                    // Dimension label pill
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 18.dp, top = 14.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BhumiEmerald)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "১১০' ৫\"",
                            fontSize = fontSize.coerceIn(8f, 18f).sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Area label pill
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "১২.৩৪ শতক",
                            fontSize = fontSize.coerceIn(9f, 20f).sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Segmented CAD Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple("small", "ছোট", 8.5f to 2.2f),
                        Triple("normal", "স্বাভাবিক", 11f to 3.0f),
                        Triple("medium", "মাঝারি", 15f to 4.8f)
                    ).forEach { (id, lbl, vals) ->
                        FilterChip(
                            selected = activePreset == id,
                            onClick = {
                                fontSize = vals.first
                                strokeWidth = vals.second
                                when (id) {
                                    "small" -> { outlineScale = 0.45f; pointScale = 0.7f }
                                    "normal" -> { outlineScale = 0.65f; pointScale = 1.0f }
                                    else -> { outlineScale = 1.1f; pointScale = 1.35f }
                                }
                            },
                            label = { Text(lbl, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                SliderStepperRow(
                    label = "লেখার সাইজ",
                    valueText = "${BengaliFormatters.formatDecimal(fontSize.toDouble(), 1)} sp",
                    value = fontSize,
                    range = 7f..24f,
                    step = 1f,
                    onValueChange = { fontSize = it }
                )
                SliderStepperRow(
                    label = "লাইনের সাইজ",
                    valueText = "${BengaliFormatters.formatDecimal(strokeWidth.toDouble(), 1)} px",
                    value = strokeWidth,
                    range = 1.5f..10f,
                    step = 0.5f,
                    onValueChange = { strokeWidth = it }
                )
                SliderStepperRow(
                    label = "আউটলাইন স্কেল",
                    valueText = "${BengaliFormatters.formatDecimal(outlineScale.toDouble(), 2)}×",
                    value = outlineScale,
                    range = 0.1f..2f,
                    step = 0.1f,
                    onValueChange = { outlineScale = it }
                )
                SliderStepperRow(
                    label = "পয়েন্ট সাইজ",
                    valueText = "${BengaliFormatters.formatDecimal(pointScale.toDouble(), 2)}×",
                    value = pointScale,
                    range = 0.5f..2.5f,
                    step = 0.1f,
                    onValueChange = { pointScale = it }
                )

                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ম্যাপের লাইন বাইরে দেখান", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = showOutside, onCheckedChange = { showOutside = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("বিভক্ত বাহুর মোট মাপ", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = showSplitTotal, onCheckedChange = { showSplitTotal = it })
                }

                // Compact Dropdowns: মাপের লেখা & জুমে লেখা
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = posDropdown,
                        onExpandedChange = { posDropdown = !posDropdown },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = textPos.labelBn,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("মাপের লেখা") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = posDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = posDropdown,
                            onDismissRequest = { posDropdown = false }
                        ) {
                            MapTextPosition.entries.forEach { pos ->
                                DropdownMenuItem(
                                    text = { Text(pos.labelBn) },
                                    onClick = { textPos = pos; posDropdown = false }
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = zoomDropdown,
                        onExpandedChange = { zoomDropdown = !zoomDropdown },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = zoomMode.labelBn,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("জুমে লেখা") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = zoomDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = zoomDropdown,
                            onDismissRequest = { zoomDropdown = false }
                        ) {
                            ZoomTextMode.entries.forEach { zm ->
                                DropdownMenuItem(
                                    text = { Text(zm.labelBn) },
                                    onClick = { zoomMode = zm; zoomDropdown = false }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onUpdateCanvasSettings?.invoke { cs ->
                        cs.copy(
                            outlineScale = outlineScale,
                            pointScale = pointScale,
                            showMeasurementsOutside = showOutside,
                            showSplitTotalInside = showSplitTotal,
                            mapTextPosition = textPos.id,
                            zoomTextMode = zoomMode.id
                        )
                    }
                    onApply(fontSize, strokeWidth)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BhumiOrangeCta)
            ) { Text("প্রয়োগ", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল", color = BhumiOrangeCta)
            }
        }
    )
}

@Composable
private fun SliderStepperRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    step: Float,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(valueText, style = MaterialTheme.typography.labelMedium, color = BhumiOrangeCta, fontWeight = FontWeight.Bold)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onValueChange((value - step).coerceIn(range.start, range.endInclusive)) },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "কমান", modifier = Modifier.size(18.dp))
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = range,
                colors = SliderDefaults.colors(thumbColor = BhumiOrangeCta, activeTrackColor = BhumiOrangeCta),
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { onValueChange((value + step).coerceIn(range.start, range.endInclusive)) },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "বাড়ান", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun ProjectMetadataDialog(
    project: SurveyProject,
    onSave: (title: String, mouza: String, district: String, upazila: String, jl: String, khatian: String, dag: String, owner: String, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember(project) { mutableStateOf(project.title) }
    var mouza by remember(project) { mutableStateOf(project.mouzaName) }
    var district by remember(project) { mutableStateOf(project.district) }
    var upazila by remember(project) { mutableStateOf(project.upazila) }
    var jl by remember(project) { mutableStateOf(project.jlNumber) }
    var khatian by remember(project) { mutableStateOf(project.khatianNo) }
    var dag by remember(project) { mutableStateOf(project.dagNo) }
    var owner by remember(project) { mutableStateOf(project.ownerName) }
    var notes by remember(project) { mutableStateOf(project.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("প্রজেক্ট ও মৌজার তথ্য", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("প্রজেক্ট শিরোনাম") }, singleLine = true)
                OutlinedTextField(value = mouza, onValueChange = { mouza = it }, label = { Text("মৌজার নাম") }, singleLine = true)
                OutlinedTextField(value = district, onValueChange = { district = it }, label = { Text("জেলা") }, singleLine = true)
                OutlinedTextField(value = upazila, onValueChange = { upazila = it }, label = { Text("উপজেলা / থানা") }, singleLine = true)
                OutlinedTextField(value = jl, onValueChange = { jl = it }, label = { Text("জে.এল নম্বর") }, singleLine = true)
                OutlinedTextField(value = khatian, onValueChange = { khatian = it }, label = { Text("খতিয়ান নম্বর") }, singleLine = true)
                OutlinedTextField(value = dag, onValueChange = { dag = it }, label = { Text("দাগ নম্বর") }, singleLine = true)
                OutlinedTextField(value = owner, onValueChange = { owner = it }, label = { Text("জমির মালিক") }, singleLine = true)
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("মন্তব্য / নোট") })
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, mouza, district, upazila, jl, khatian, dag, owner, notes) },
                colors = ButtonDefaults.buttonColors(containerColor = BhumiOrangeCta)
            ) { Text("সংরক্ষণ", color = Color.White) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } }
    )
}

@Composable
fun RenameDialog(
    currentTitle: String,
    onRename: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember(currentTitle) { mutableStateOf(currentTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("প্রজেক্টের নাম পরিবর্তন") },
        text = {
            OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("নতুন নাম") }, singleLine = true)
        },
        confirmButton = { Button(onClick = { onRename(text) }) { Text("সংরক্ষণ") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } }
    )
}

@Composable
fun AnnotationDialog(
    initialText: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember(initialText) { mutableStateOf(initialText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("প্লট টীকা / নোট") },
        text = {
            OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("টীকা লিখুন") })
        },
        confirmButton = { Button(onClick = { onSave(text) }) { Text("সংরক্ষণ") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } }
    )
}

@Composable
fun PlotMetadataDialog(
    plot: LandPlot,
    onSave: (name: String, dag: String, owner: String, khatian: String, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(plot) { mutableStateOf(plot.name) }
    var dag by remember(plot) { mutableStateOf(plot.dagNumber) }
    var owner by remember(plot) { mutableStateOf(plot.ownerName) }
    var khatian by remember(plot) { mutableStateOf(plot.khatianRef) }
    var notes by remember(plot) { mutableStateOf(plot.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("প্লটের তথ্য সম্পাদনা", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("প্লটের নাম") }, singleLine = true)
                OutlinedTextField(value = dag, onValueChange = { dag = it }, label = { Text("দাগ নম্বর") }, singleLine = true)
                OutlinedTextField(value = owner, onValueChange = { owner = it }, label = { Text("মালিকের নাম") }, singleLine = true)
                OutlinedTextField(value = khatian, onValueChange = { khatian = it }, label = { Text("খতিয়ান") }, singleLine = true)
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("মন্তব্য") })
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, dag, owner, khatian, notes) },
                colors = ButtonDefaults.buttonColors(containerColor = BhumiOrangeCta)
            ) { Text("সংরক্ষণ", color = Color.White) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } }
    )
}
