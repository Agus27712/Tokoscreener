package com.tokoreader.presentation.radar.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokoreader.ui.theme.ErrorRed
import com.tokoreader.ui.theme.SuccessGreen

/**
 * Komponen Visual Tekanan Ask & Bid (Order Book Depth) dengan Animasi Smooth Loading:
 * - Animasi transisi mulus pada pergeseran persentase bid/ask
 * - Gelombang shimmer halus yang terus mengalir menandakan stream live data WebSocket
 * - Efek pulsating beacon untuk status koneksi live orderbook Tokocrypto
 */
@Composable
fun LiveAskBidPressureSection(
    bidPressure: Float,
    modifier: Modifier = Modifier
) {
    // 1. Animasi smooth interpolasi untuk pergerakan nilai bar
    val animatedBidPressure by animateFloatAsState(
        targetValue = bidPressure.coerceIn(0.05f, 0.95f),
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "SmoothBidPressure"
    )

    // 2. Animasi shimmer gelombang kontinu untuk smooth loading feed stream
    val infiniteTransition = rememberInfiniteTransition(label = "OrderbookStreamLoading")
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerWave"
    )

    // 3. Animasi pulse dot live feed
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LivePulse"
    )

    val bidPercent = (animatedBidPressure * 100).toInt()
    val askPercent = 100 - bidPercent

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        // Baris Header: Live Beacon + Judul
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pulsing dot indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(SuccessGreen.copy(alpha = pulseAlpha), CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "ORDER BOOK DEPTH (LIVE)",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Progress Bar Split Bid (Hijau) & Ask (Merah) dengan Animasi Smooth Loading
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(ErrorRed)
        ) {
            // Sisi Bid (Beli)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedBidPressure)
                    .background(SuccessGreen)
            )

            // Shimmer Smooth Loading Wave Overlay
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.28f),
                                Color.Transparent
                            ),
                            startX = shimmerTranslate * 800f,
                            endX = (shimmerTranslate + 0.35f) * 800f
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Nilai Angka Bid & Ask dengan Indikator Warna
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(SuccessGreen, CircleShape)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Tekanan Beli (Bid): $bidPercent%",
                    color = SuccessGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Tekanan Jual (Ask): $askPercent%",
                    color = ErrorRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(5.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(ErrorRed, CircleShape)
                )
            }
        }
    }
}
