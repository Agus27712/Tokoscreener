package com.tokoreader.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CryptoCoinAvatar(symbol: String, modifier: Modifier = Modifier) {
    val cleanSym = symbol.uppercase()
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                when (cleanSym) {
                    "BTC" -> Color(0xFFF7931A).copy(alpha = 0.15f)
                    "ETH" -> Color(0xFF627EEA).copy(alpha = 0.15f)
                    "SOL" -> Color(0xFF14F195).copy(alpha = 0.12f)
                    "XRP" -> Color(0xFF23292F)
                    "DOGE" -> Color(0xFFFBBF24).copy(alpha = 0.15f)
                    "BNB" -> Color(0xFFF3BA2F).copy(alpha = 0.15f)
                    else -> Color(0xFF1E293B)
                }
            )
            .border(
                1.dp,
                when (cleanSym) {
                    "BTC" -> Color(0xFFF7931A).copy(alpha = 0.5f)
                    "ETH" -> Color(0xFF627EEA).copy(alpha = 0.5f)
                    "SOL" -> Color(0xFF9945FF).copy(alpha = 0.5f)
                    "XRP" -> Color(0xFF475569)
                    "DOGE" -> Color(0xFFFBBF24).copy(alpha = 0.6f)
                    "BNB" -> Color(0xFFF3BA2F).copy(alpha = 0.5f)
                    else -> Color(0xFF334155)
                },
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        when (cleanSym) {
            "ETH" -> {
                Canvas(modifier = Modifier.size(18.dp)) {
                    val w = size.width
                    val h = size.height
                    val ethPath = Path().apply {
                        moveTo(w * 0.5f, 0f)
                        lineTo(w, h * 0.55f)
                        lineTo(w * 0.5f, h * 0.72f)
                        lineTo(0f, h * 0.55f)
                        close()
                    }
                    drawPath(ethPath, Color(0xFF8C9FF0))
                    val bottomPath = Path().apply {
                        moveTo(w * 0.5f, h * 0.77f)
                        lineTo(w, h * 0.62f)
                        lineTo(w * 0.5f, h)
                        lineTo(0f, h * 0.62f)
                        close()
                    }
                    drawPath(bottomPath, Color(0xFF627EEA))
                }
            }
            "SOL" -> {
                Canvas(modifier = Modifier.size(16.dp)) {
                    val w = size.width
                    val h = size.height
                    val barH = h * 0.18f
                    val solBrush = Brush.horizontalGradient(listOf(Color(0xFF00FFA3), Color(0xFFDC1FFF)))
                    drawRoundRect(solBrush, Offset(0f, 0f), Size(w, barH), CornerRadius(barH / 2))
                    drawRoundRect(solBrush, Offset(0f, h * 0.41f), Size(w, barH), CornerRadius(barH / 2))
                    drawRoundRect(solBrush, Offset(0f, h * 0.82f), Size(w, barH), CornerRadius(barH / 2))
                }
            }
            "XRP" -> {
                Canvas(modifier = Modifier.size(14.dp)) {
                    val w = size.width
                    val h = size.height
                    drawLine(Color.White, Offset(0f, 0f), Offset(w, h), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(Color.White, Offset(w, 0f), Offset(0f, h), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                }
            }
            "DOGE" -> {
                Text(
                    text = "Ð",
                    color = Color(0xFFFBBF24),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black
                )
            }
            "BTC" -> {
                Text(
                    text = "₿",
                    color = Color(0xFFF7931A),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            "BNB" -> {
                Text(
                    text = "BNB",
                    color = Color(0xFFF3BA2F),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            else -> {
                Text(
                    text = cleanSym.take(3),
                    color = Color(0xFFE2E8F0),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
