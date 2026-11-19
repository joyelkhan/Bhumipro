package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.BengaliFormatters
import com.example.ui.theme.BhumiEmerald
import com.example.ui.theme.BhumiEmeraldContainer
import com.example.ui.theme.BhumiOrangeCta

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SaveSurveyDialog(
    currentStatus: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
    currentTitle: String = "",
    currentMouza: String = "",
    currentNotes: String = "",
    totalAreaShotok: Double = 0.0,
    plotCount: Int = 0,
    totalPerimeterFeet: Double = 0.0,
    onSaveWithMetadata: ((status: String, title: String, mouza: String, notes: String) -> Unit)? = null
) {
    var status by remember(currentStatus) { mutableStateOf(currentStatus) }
    var title by remember(currentTitle) { mutableStateOf(currentTitle) }
    var mouza by remember(currentMouza) { mutableStateOf(currentMouza) }
    var notes by remember(currentNotes) { mutableStateOf(currentNotes) }
    val statuses = listOf("সক্রিয়", "খসড়া", "চূড়ান্ত", "পর্যালোচনাধীন")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("জরিপ সংরক্ষণ করুন", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BhumiEmeraldContainer)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "মোট জমি: ${BengaliFormatters.formatDecimal(totalAreaShotok, 2)} শতক",
                        fontWeight = FontWeight.Bold,
                        color = BhumiEmerald
                    )
                    Text(
                        text = "প্লট: ${BengaliFormatters.toBengaliDigits(plotCount)} টি • পরিসীমা: ${BengaliFormatters.formatDecimal(totalPerimeterFeet, 1)} ফুট",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (onSaveWithMetadata != null) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("প্রজেক্ট শিরোনাম") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = mouza,
                        onValueChange = { mouza = it },
                        label = { Text("মৌজার নাম") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("নোট / মন্তব্য") },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text("জরিপের অবস্থা নির্বাচন করুন:", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    statuses.forEach { s ->
                        FilterChip(selected = status == s, onClick = { status = s }, label = { Text(s) })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (onSaveWithMetadata != null) {
                        onSaveWithMetadata(status, title, mouza, notes)
                    } else {
                        onSave(status)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BhumiOrangeCta)
            ) { Text("সংরক্ষণ করুন", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল", color = BhumiOrangeCta) } }
    )
}
