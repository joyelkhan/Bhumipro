package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.DivisionReport
import com.example.domain.BengaliFormatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDivisionBottomSheet(
    report: DivisionReport,
    onUndoDivision: () -> Unit,
    onOpenPrint: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("বিভাজন ফলাফল সারসংক্ষেপ", style = MaterialTheme.typography.titleLarge)
            Text("মূল জমি: ${BengaliFormatters.formatDecimal(report.originalAreaShotok, 2)} শতক")
            Text("রাস্তা: ${BengaliFormatters.formatDecimal(report.roadAreaShotok, 2)} শতক")
            Text("বণ্টনযোগ্য নেট: ${BengaliFormatters.formatDecimal(report.netAreaShotok, 2)} শতক")
            report.parts.forEach { p ->
                Text("• ${p.name}: ${BengaliFormatters.formatDecimal(p.areaShotok, 2)} শতক (${BengaliFormatters.formatDecimal(p.percentage, 1)}%)")
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onUndoDivision, modifier = Modifier.weight(1f)) {
                    Text("আনডু করুন")
                }
                Button(onClick = onOpenPrint, modifier = Modifier.weight(1f)) {
                    Text("প্রিন্ট প্রিভিউ")
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
