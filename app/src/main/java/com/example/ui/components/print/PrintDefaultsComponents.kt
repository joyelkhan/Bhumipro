package com.example.ui.components.print

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BhumiProPrintColors

@Composable
fun PrintInfoBanner(
    text: String,
    isError: Boolean = false,
    isWarning: Boolean = false,
    modifier: Modifier = Modifier
) {
    val bg = when {
        isError -> Color(0xFFFEE2E2)
        isWarning -> BhumiProPrintColors.OrangeCream
        else -> Color(0xFFEFF6FF)
    }
    val border = when {
        isError -> Color(0xFFEF4444)
        isWarning -> BhumiProPrintColors.OrangePrimary
        else -> Color(0xFF3B82F6)
    }
    val fg = when {
        isError -> Color(0xFF991B1B)
        isWarning -> BhumiProPrintColors.OrangeDeep
        else -> Color(0xFF1E3A8A)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, border.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isError || isWarning) Icons.Default.Warning else Icons.Default.Info,
            contentDescription = null,
            tint = fg
        )
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = fg)
    }
}
