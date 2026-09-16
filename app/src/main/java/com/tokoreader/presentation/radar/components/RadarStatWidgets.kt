package com.tokoreader.presentation.radar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatBox(label: String, value: String) {
    Column {
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 11.5.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SnapshotChip(icon: ImageVector, label: String, tint: Color) {
    Row(
        modifier = Modifier
            .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(13.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, color = tint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
