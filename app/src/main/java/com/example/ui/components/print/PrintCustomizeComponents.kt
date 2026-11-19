package com.example.ui.components.print

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.PrintConfig
import com.example.data.model.PrintLineColorMode
import com.example.data.model.ReportSectionId
import com.example.ui.theme.BhumiProPrintColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PrintCustomizePanel(
    config: PrintConfig,
    onUpdateConfig: ((PrintConfig) -> PrintConfig) -> Unit,
    onSaveGlobal: () -> Unit,
    onSaveProject: () -> Unit,
    onClearProjectOverride: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("সীমানা রেখার রং", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            PrintLineColorMode.entries.forEach { mode ->
                FilterChip(
                    selected = config.lineColorMode == mode,
                    onClick = { onUpdateConfig { it.copy(lineColorMode = mode) } },
                    leadingIcon = mode.hex?.let { hex ->
                        {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                            )
                        }
                    },
                    label = { Text(mode.labelBn) }
                )
            }
        }

        Text("রিপোর্ট সেকশন ও ক্রম", style = MaterialTheme.typography.labelLarge)
        val sections = config.sectionLayout
        sections.order.forEachIndexed { idx, secId ->
            val visible = sections.isVisible(secId)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(BhumiProPrintColors.FieldBg)
                    .border(1.dp, BhumiProPrintColors.FieldBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(secId.labelBn, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                IconButton(
                    onClick = {
                        if (idx > 0) {
                            val list = sections.order.toMutableList()
                            val tmp = list[idx - 1]; list[idx - 1] = list[idx]; list[idx] = tmp
                            onUpdateConfig { it.copy(sectionLayout = it.sectionLayout.copy(order = list)) }
                        }
                    },
                    enabled = idx > 0
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "উপরে")
                }
                IconButton(
                    onClick = {
                        if (idx < sections.order.lastIndex) {
                            val list = sections.order.toMutableList()
                            val tmp = list[idx + 1]; list[idx + 1] = list[idx]; list[idx] = tmp
                            onUpdateConfig { it.copy(sectionLayout = it.sectionLayout.copy(order = list)) }
                        }
                    },
                    enabled = idx < sections.order.lastIndex
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "নিচে")
                }
                Switch(
                    checked = visible,
                    onCheckedChange = { checked ->
                        val nextHidden = sections.hidden.toMutableSet().apply {
                            if (checked) remove(secId) else add(secId)
                        }
                        onUpdateConfig { it.copy(sectionLayout = it.sectionLayout.copy(hidden = nextHidden)) }
                    }
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onSaveGlobal, modifier = Modifier.weight(1f)) {
                Text("গ্লোবাল ডিফল্ট সেভ")
            }
            Button(onClick = onSaveProject, modifier = Modifier.weight(1f)) {
                Text("এই প্রজেক্টে সেভ")
            }
        }
        TextButton(onClick = onClearProjectOverride, modifier = Modifier.align(Alignment.End)) {
            Text("প্রজেক্ট ওভাররাইড মুছুন")
        }
    }
}
