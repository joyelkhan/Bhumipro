package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditRoad
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.DivisionType
import com.example.data.model.roadOptionsList
import com.example.domain.BengaliFormatters
import com.example.ui.theme.BhumiEmerald
import com.example.ui.theme.BhumiEmeraldContainer
import com.example.ui.theme.BhumiOrangeCta
import com.example.ui.theme.BhumiOrangeSoft
import com.example.ui.viewmodel.DivisionUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DivisionModal(
    state: DivisionUiState,
    activePlotName: String,
    onSetType: (DivisionType) -> Unit,
    onAdjustParts: (Int) -> Unit,
    onUpdatePercentage: (Int, Double) -> Unit,
    onUpdateCustomShotok: (Int, Double) -> Unit,
    onSelectRoad: (String) -> Unit,
    onAdjustRoadWidth: (Float) -> Unit,
    onSweepAngleChange: (Float) -> Unit,
    onPreview: () -> Unit,
    onCommit: () -> Unit,
    onDismiss: () -> Unit,
    onStartInteractiveRoad: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("জমি বিভাজন — $activePlotName", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Interactive Polyline Road Mode CTA Card (Mockups #16 & #18)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BhumiOrangeSoft)
                        .border(1.dp, BhumiOrangeCta.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "ইন্টারঅ্যাক্টিভ রাস্তা বসান",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = BhumiOrangeCta
                    )
                    Text(
                        "ম্যাপের ওপর সাদা বিন্দু টেনে যেকোনো বাঁকা/সোজা রাস্তা ও সমান বা কাস্টম ভাগ করুন।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = onStartInteractiveRoad,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.EditRoad, contentDescription = null, tint = BhumiOrangeCta)
                        Spacer(Modifier.width(6.dp))
                        Text("বিন্দু টেনে রাস্তা বসান", color = BhumiOrangeCta, fontWeight = FontWeight.Bold)
                    }
                }

                Text("বিভাজন পদ্ধতি", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DivisionType.entries.forEach { t ->
                        FilterChip(
                            selected = state.divisionType == t,
                            onClick = { onSetType(t) },
                            label = { Text(t.labelBn) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("অংশ সংখ্যা: ${BengaliFormatters.toBengaliDigits(state.divisionPartsCount)}", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onAdjustParts(-1) }) { Text("−") }
                        OutlinedButton(onClick = { onAdjustParts(1) }) { Text("+") }
                    }
                }

                if (state.divisionType == DivisionType.PERCENTAGE) {
                    state.divisionPercentages.take(state.divisionPartsCount).forEachIndexed { idx, pct ->
                        OutlinedTextField(
                            value = BengaliFormatters.formatDecimal(pct, 1, useBengali = false),
                            onValueChange = { raw ->
                                BengaliFormatters.parseDoubleFlexible(raw)?.let { onUpdatePercentage(idx, it) }
                            },
                            label = { Text("অংশ ${BengaliFormatters.toBengaliDigits(idx + 1)} (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else if (state.divisionType == DivisionType.CUSTOM_AREA) {
                    state.customTargetShotoks.take(state.divisionPartsCount - 1).forEachIndexed { idx, s ->
                        OutlinedTextField(
                            value = BengaliFormatters.formatDecimal(s, 2, useBengali = false),
                            onValueChange = { raw ->
                                BengaliFormatters.parseDoubleFlexible(raw)?.let { onUpdateCustomShotok(idx, it) }
                            },
                            label = { Text("অংশ ${BengaliFormatters.toBengaliDigits(idx + 1)} (শতক)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("রাস্তার প্রস্থ: ${BengaliFormatters.formatDecimal(state.roadWidthFeet.toDouble(), 1)} ফুট")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { onAdjustRoadWidth(-1f) }) { Text("−১") }
                        OutlinedButton(onClick = { onAdjustRoadWidth(1f) }) { Text("+১") }
                    }
                }

                Text("রাস্তা ও বিভাজন লেআউট (১৯টি ধরন)", style = MaterialTheme.typography.labelLarge)
                val grouped = roadOptionsList.groupBy { it.groupBn }
                grouped.forEach { (group, options) ->
                    Text(group, style = MaterialTheme.typography.labelSmall, color = BhumiEmerald, fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        options.forEach { opt ->
                            FilterChip(
                                selected = state.selectedRoadOption == opt.id,
                                onClick = { onSelectRoad(opt.id) },
                                label = { Text(opt.labelBn) }
                            )
                        }
                    }
                }

                Text("কাটিং কোণ: ${BengaliFormatters.toBengaliDigits(state.divisionSweepAngleDeg.toInt())}°")
                Slider(
                    value = state.divisionSweepAngleDeg,
                    onValueChange = onSweepAngleChange,
                    valueRange = -90f..90f
                )

                val rep = state.divisionReport
                if (rep != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(BhumiEmeraldContainer)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (!rep.warningMessage.isNullOrBlank()) {
                            Text(rep.warningMessage, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        } else {
                            Text("মোট জমি: ${BengaliFormatters.formatDecimal(rep.originalAreaShotok, 2)} শতক", fontWeight = FontWeight.SemiBold)
                            Text("রাস্তা: ${BengaliFormatters.formatDecimal(rep.roadAreaShotok, 2)} শতক • নেট: ${BengaliFormatters.formatDecimal(rep.netAreaShotok, 2)} শতক")
                            rep.parts.forEach { part ->
                                Text("• ${part.name}: ${BengaliFormatters.formatDecimal(part.areaShotok, 2)} শতক (${BengaliFormatters.formatDecimal(part.percentage, 1)}%)")
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                OutlinedButton(onClick = onPreview, modifier = Modifier.fillMaxWidth()) {
                    Text("প্রিভিউ দেখুন")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onCommit,
                colors = ButtonDefaults.buttonColors(containerColor = BhumiOrangeCta)
            ) { Text("ভাগ করুন", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল", color = BhumiOrangeCta) }
        }
    )
}
