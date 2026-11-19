package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.ControlPoint
import com.example.data.model.GeorefQualityReport
import com.example.data.model.PointF
import com.example.domain.BengaliFormatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeorefOverlayPanel(
    opacity: Float,
    controlPoints: List<ControlPoint>,
    quality: GeorefQualityReport,
    onOpacityChange: (Float) -> Unit,
    onAddSamplePoint: (ControlPoint) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("জিওরেফারেন্স ও ওভারলে নিয়ন্ত্রণ", style = MaterialTheme.typography.titleLarge)
            Text("ওভারলে অস্বচ্ছতা: ${BengaliFormatters.toBengaliDigits((opacity * 100).toInt())}%")
            Slider(value = opacity, onValueChange = onOpacityChange, valueRange = 0.05f..1.0f)
            Text("কন্ট্রোল পয়েন্ট: ${BengaliFormatters.toBengaliDigits(quality.pointCount)} • মান: ${quality.qualityLabelBn}")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val idx = controlPoints.size + 1
                        onAddSamplePoint(
                            ControlPoint(
                                id = "cp_$idx",
                                label = "CP-$idx",
                                sourcePoint = PointF(100f * idx, 120f * idx),
                                targetPoint = PointF(100f * idx + 1.5f, 120f * idx - 1f)
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("কন্ট্রোল পয়েন্ট যোগ")
                }
            }
        }
    }
}
