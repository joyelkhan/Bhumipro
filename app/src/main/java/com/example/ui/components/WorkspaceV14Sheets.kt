package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ProjectSnapshotEntity
import com.example.data.model.CanvasDisplaySettings
import com.example.data.model.GisLayerItem
import com.example.data.model.LandPlot
import com.example.data.model.MapTextPosition
import com.example.data.model.MeasurementUnit
import com.example.data.model.PlotLabelStyle
import com.example.data.model.ZoomTextMode
import com.example.domain.BengaliFormatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayersBottomSheet(
    layers: List<GisLayerItem>,
    onToggleVisibility: (String) -> Unit,
    onToggleLocked: (String) -> Unit,
    onSetOpacity: (String, Float) -> Unit,
    onMoveLayer: (String, Boolean) -> Unit,
    onShowAll: () -> Unit,
    onHideAll: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("জিআইএস লেয়ার ব্যবস্থাপনা", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = onShowAll) { Text("সব দেখান") }
                    TextButton(onClick = onHideAll) { Text("সব লুকান") }
                }
            }
            layers.sortedBy { it.zOrder }.forEach { layer ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = layer.nameBengali,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onMoveLayer(layer.id, false) }) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "উপরে")
                        }
                        IconButton(onClick = { onMoveLayer(layer.id, true) }) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "নিচে")
                        }
                        IconButton(onClick = { onToggleLocked(layer.id) }) {
                            Icon(
                                if (layer.locked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "লক"
                            )
                        }
                        IconButton(onClick = { onToggleVisibility(layer.id) }) {
                            Icon(
                                if (layer.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "দৃশ্যমানতা"
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("স্বচ্ছতা: ${BengaliFormatters.toBengaliDigits((layer.opacity * 100).toInt())}%", style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.width(8.dp))
                        Slider(
                            value = layer.opacity,
                            onValueChange = { onSetOpacity(layer.id, it) },
                            valueRange = 0.05f..1f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CanvasSettingsSheet(
    settings: CanvasDisplaySettings,
    onUpdate: ((CanvasDisplaySettings) -> CanvasDisplaySettings) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ক্যানভাস ও CAD প্রদর্শন", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onReset) { Text("ডিফল্ট রিসেট") }
            }

            Text("দৈর্ঘ্য একক", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MeasurementUnit.entries.forEach { u ->
                    FilterChip(
                        selected = settings.measurementUnit == u.id,
                        onClick = { onUpdate { it.copy(measurementUnit = u.id) } },
                        label = { Text(u.bengaliName) }
                    )
                }
            }

            Text("দশমিক নির্ভুলতা: ${BengaliFormatters.toBengaliDigits(settings.decimalPrecision)}", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = settings.decimalPrecision.toFloat(),
                onValueChange = { v -> onUpdate { it.copy(decimalPrecision = v.toInt().coerceIn(0, 3)) } },
                valueRange = 0f..3f,
                steps = 2
            )

            Text("CAD প্রিসেট", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("small" to "ছোট", "normal" to "স্বাভাবিক", "medium" to "মাঝারি").forEach { (id, lbl) ->
                    FilterChip(
                        selected = settings.presetIdOrNull() == id,
                        onClick = { onUpdate { it.withCadPreset(id) } },
                        label = { Text(lbl) }
                    )
                }
            }

            Text("ফন্ট স্কেল: ${BengaliFormatters.formatDecimal(settings.fontScale.toDouble(), 2)}×", style = MaterialTheme.typography.labelMedium)
            Slider(value = settings.fontScale, onValueChange = { v -> onUpdate { it.copy(fontScale = v) } }, valueRange = 0.4f..3f)

            Text("লাইন প্রস্থ: ${BengaliFormatters.formatDecimal(settings.lineScale.toDouble(), 2)}×", style = MaterialTheme.typography.labelMedium)
            Slider(value = settings.lineScale, onValueChange = { v -> onUpdate { it.copy(lineScale = v) } }, valueRange = 0.4f..3f)

            Text("আউটলাইন স্কেল: ${BengaliFormatters.formatDecimal(settings.outlineScale.toDouble(), 2)}×", style = MaterialTheme.typography.labelMedium)
            Slider(value = settings.outlineScale, onValueChange = { v -> onUpdate { it.copy(outlineScale = v) } }, valueRange = 0.1f..2f)

            Text("পয়েন্ট স্কেল: ${BengaliFormatters.formatDecimal(settings.pointScale.toDouble(), 2)}×", style = MaterialTheme.typography.labelMedium)
            Slider(value = settings.pointScale, onValueChange = { v -> onUpdate { it.copy(pointScale = v) } }, valueRange = 0.4f..3f)

            Text("মাপের লেখার অবস্থান", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MapTextPosition.entries.forEach { pos ->
                    FilterChip(
                        selected = settings.mapTextPosition == pos.id,
                        onClick = { onUpdate { it.copy(mapTextPosition = pos.id) } },
                        label = { Text(pos.labelBn) }
                    )
                }
            }

            Text("জুম টেক্সট মোড", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ZoomTextMode.entries.forEach { zm ->
                    FilterChip(
                        selected = settings.zoomTextMode == zm.id,
                        onClick = { onUpdate { it.copy(zoomTextMode = zm.id) } },
                        label = { Text(zm.labelBn) }
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("কর্নার পয়েন্ট দেখান")
                Switch(checked = settings.showCornerPoints, onCheckedChange = { c -> onUpdate { it.copy(showCornerPoints = c) } })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("ক্রসহেয়ার দেখান")
                Switch(checked = settings.showCrosshair, onCheckedChange = { c -> onUpdate { it.copy(showCrosshair = c) } })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("ম্যাপের লাইন বাইরে দেখান")
                Switch(checked = settings.showMeasurementsOutside, onCheckedChange = { c -> onUpdate { it.copy(showMeasurementsOutside = c) } })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("বিভক্ত বাহুর মোট মাপ ভিতরে")
                Switch(checked = settings.showSplitTotalInside, onCheckedChange = { c -> onUpdate { it.copy(showSplitTotalInside = c) } })
            }

            HorizontalDivider()
            Text("ম্যাপ রাস্টার ফিল্টার", style = MaterialTheme.typography.labelLarge)
            Text("ব্রাইটনেস: ${BengaliFormatters.formatDecimal(settings.brightness.toDouble(), 2)}")
            Slider(value = settings.brightness, onValueChange = { v -> onUpdate { it.copy(brightness = v) } }, valueRange = -0.5f..0.5f)
            Text("কনট্রাস্ট: ${BengaliFormatters.formatDecimal(settings.contrast.toDouble(), 2)}")
            Slider(value = settings.contrast, onValueChange = { v -> onUpdate { it.copy(contrast = v) } }, valueRange = -0.5f..0.5f)
            Text("ম্যাপ অস্বচ্ছতা: ${BengaliFormatters.toBengaliDigits((settings.mapOpacity * 100).toInt())}%")
            Slider(value = settings.mapOpacity, onValueChange = { v -> onUpdate { it.copy(mapOpacity = v) } }, valueRange = 0.05f..1f)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("গ্রেস্কেল ম্যাপ")
                Switch(checked = settings.grayscale, onCheckedChange = { c -> onUpdate { it.copy(grayscale = c) } })
            }

            Spacer(Modifier.height(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MultiPlotSelectionSheet(
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onInvert: () -> Unit,
    onSelectAdjacent: () -> Unit,
    onSelectByDag: (String) -> Unit,
    onSelectByOwner: (String) -> Unit,
    onSelectByAreaRange: (Double, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var dagQuery by remember { mutableStateOf("") }
    var ownerQuery by remember { mutableStateOf("") }
    var minArea by remember { mutableStateOf("0") }
    var maxArea by remember { mutableStateOf("100") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "বহু-প্লট নির্বাচন (${BengaliFormatters.toBengaliDigits(selectedCount)} টি প্লট নির্বাচিত)",
                style = MaterialTheme.typography.titleLarge
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = onSelectAll) { Text("সব নির্বাচন") }
                OutlinedButton(onClick = onDeselectAll) { Text("নির্বাচন মুছুন") }
                OutlinedButton(onClick = onInvert) { Text("বিপরীত নির্বাচন") }
                OutlinedButton(onClick = onSelectAdjacent) { Text("সংলগ্ন প্লট") }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = dagQuery,
                    onValueChange = { dagQuery = it },
                    label = { Text("দাগ নম্বর") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Button(onClick = { onSelectByDag(dagQuery) }) { Text("খুঁজুন") }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = ownerQuery,
                    onValueChange = { ownerQuery = it },
                    label = { Text("মালিকের নাম") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Button(onClick = { onSelectByOwner(ownerQuery) }) { Text("খুঁজুন") }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = minArea,
                    onValueChange = { minArea = it },
                    label = { Text("সর্বনিম্ন শতক") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = maxArea,
                    onValueChange = { maxArea = it },
                    label = { Text("সর্বোচ্চ শতক") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Button(onClick = {
                    val lo = BengaliFormatters.parseDoubleFlexible(minArea) ?: 0.0
                    val hi = BengaliFormatters.parseDoubleFlexible(maxArea) ?: 100.0
                    onSelectByAreaRange(lo, hi)
                }) { Text("ফিল্টার") }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun PlotLabelStyleDialog(
    plot: LandPlot,
    onUpdate: ((PlotLabelStyle) -> PlotLabelStyle) -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val style = plot.labelStyle ?: PlotLabelStyle.DEFAULT
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("লেবেল স্টাইল — ${plot.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ফন্ট স্কেল: ${BengaliFormatters.formatDecimal(style.fontSizeScale.toDouble(), 2)}×")
                Slider(
                    value = style.fontSizeScale,
                    onValueChange = { v -> onUpdate { it.copy(fontSizeScale = v) } },
                    valueRange = 0.4f..4.0f
                )
                Text("স্বচ্ছতা: ${BengaliFormatters.toBengaliDigits((style.opacity * 100).toInt())}%")
                Slider(
                    value = style.opacity,
                    onValueChange = { v -> onUpdate { it.copy(opacity = v) } },
                    valueRange = 0.05f..1.0f
                )
                Text("ঘূর্ণন: ${BengaliFormatters.toBengaliDigits(style.rotationDeg.toInt())}°")
                Slider(
                    value = style.rotationDeg,
                    onValueChange = { v -> onUpdate { it.copy(rotationDeg = v) } },
                    valueRange = -180f..180f
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = style.bold, onClick = { onUpdate { it.copy(bold = !it.bold) } }, label = { Text("বোল্ড") })
                    FilterChip(selected = style.italic, onClick = { onUpdate { it.copy(italic = !it.italic) } }, label = { Text("ইটালিক") })
                    FilterChip(selected = style.underline, onClick = { onUpdate { it.copy(underline = !it.underline) } }, label = { Text("আন্ডারলাইন") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("#111827", "#2563EB", "#DC2626", "#16A34A", "#9333EA").forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .clickable { onUpdate { it.copy(colorHex = hex) } }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = onCopy) { Text("কপি") }
                    OutlinedButton(onClick = onPaste) { Text("পেস্ট") }
                    TextButton(onClick = onReset) { Text("রিসেট") }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("সম্পন্ন") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnapshotsSheet(
    snapshots: List<ProjectSnapshotEntity>,
    onCreateSnapshot: (String) -> Unit,
    onRestoreSnapshot: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("প্রজেক্ট স্ন্যাপশট", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("স্ন্যাপশটের নাম") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Button(onClick = { onCreateSnapshot(label); label = "" }) {
                    Text("সংরক্ষণ")
                }
            }
            if (snapshots.isEmpty()) {
                Text("এখনো কোনো স্ন্যাপশট সংরক্ষিত হয়নি", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                snapshots.forEach { snap ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(snap.label, fontWeight = FontWeight.SemiBold)
                            Text("১ শতক = ${BengaliFormatters.formatDecimal(snap.sqFeetPerShotok, 1)} বর্গফুট", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(onClick = { onRestoreSnapshot(snap.id) }) {
                            Text("পুনরুদ্ধার")
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
