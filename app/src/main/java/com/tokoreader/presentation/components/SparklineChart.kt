package com.tokoreader.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SparklineChart(
    points: List<Float>,
    isUp: Boolean,
    modifier: Modifier = Modifier,
    showEndGlow: Boolean = true,
    strokeWidth: Dp = 2.dp
) {
    if (points.size < 2) return

    val lineColor = if (isUp) Color(0xFF10B981) else Color(0xFFEF4444)
    val fillBrush = Brush.verticalGradient(
        colors = listOf(
            lineColor.copy(alpha = 0.38f),
            lineColor.copy(alpha = 0.10f),
            Color.Transparent
        )
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val rawMin = points.minOrNull() ?: 0f
        val rawMax = points.maxOrNull() ?: 1f
        val range = (rawMax - rawMin).let { if (it == 0f) 1f else it }

        val verticalPadding = height * 0.15f
        val usableHeight = height - (verticalPadding * 2)

        val coordinates = points.mapIndexed { index, value ->
            val x = index.toFloat() / (points.size - 1) * width
            val normalizedY = (value - rawMin) / range
            val y = height - verticalPadding - (normalizedY * usableHeight)
            Offset(x, y)
        }

        val strokePath = Path().apply {
            moveTo(coordinates.first().x, coordinates.first().y)
            for (i in 0 until coordinates.size - 1) {
                val p0 = coordinates[i]
                val p1 = coordinates[i + 1]
                val midX = (p0.x + p1.x) / 2f
                cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
            }
        }

        val fillPath = Path().apply {
            addPath(strokePath)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }

        drawPath(path = fillPath, brush = fillBrush)
        drawPath(
            path = strokePath,
            color = lineColor,
            style = Stroke(
                width = strokeWidth.toPx(),
                cap = StrokeCap.Round
            )
        )

        if (showEndGlow) {
            val last = coordinates.last()
            drawCircle(
                color = lineColor.copy(alpha = 0.35f),
                radius = 5.dp.toPx(),
                center = last
            )
            drawCircle(
                color = lineColor,
                radius = 3.dp.toPx(),
                center = last
            )
            drawCircle(
                color = Color.White,
                radius = 1.5.dp.toPx(),
                center = last
            )
        }
    }
}

@Composable
fun StatusBadge(badgeText: String, isHot: Boolean, isReadySell: Boolean) {
    val (borderColor, bgColor, textColor) = when {
        isReadySell -> Triple(
            Color(0xFF10B981).copy(alpha = 0.8f),
            Color(0xFF10B981).copy(alpha = 0.12f),
            Color(0xFF10B981)
        )
        isHot -> Triple(
            Color(0xFFF59E0B).copy(alpha = 0.8f),
            Color(0xFFF59E0B).copy(alpha = 0.12f),
            Color(0xFFF59E0B)
        )
        else -> Triple(
            Color(0xFF334155),
            Color(0xFF1E293B).copy(alpha = 0.6f),
            Color(0xFF94A3B8)
        )
    }

    Box(
        modifier = Modifier
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = badgeText,
            color = textColor,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun SnapshotBlock(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    value: String,
    tint: Color
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column {
            Icon(icon, contentDescription = title, tint = tint, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                maxLines = 1,
                softWrap = false
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = tint,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
