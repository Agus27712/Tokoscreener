package com.tokoreader.presentation.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokoreader.domain.model.Kline
import com.tokoreader.domain.repository.MarketDataRepository
import com.tokoreader.presentation.components.CryptoUtils.formatCryptoPrice
import com.tokoreader.ui.theme.ErrorRed
import com.tokoreader.ui.theme.SuccessGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@Composable
fun ProperMiniCandleChart(
    symbol: String,
    marketDataRepository: MarketDataRepository,
    modifier: Modifier = Modifier,
    onFullscreenClick: (() -> Unit)? = null
) {
    var selectedInterval by remember { mutableStateOf("1m") }
    var allCandles by remember { mutableStateOf<List<Kline>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedCandle by remember { mutableStateOf<Kline?>(null) }
    var crosshairOffset by remember { mutableStateOf<Offset?>(null) }
    
    // Zoom and pan state
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }

    val textMeasurer = rememberTextMeasurer()
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    LaunchedEffect(symbol, selectedInterval) {
        isLoading = true
        selectedCandle = null
        crosshairOffset = null
        zoomScale = 1f
        panOffsetX = 0f
        try {
            // First load from REST
            val initial = withContext(Dispatchers.IO) {
                marketDataRepository.getKlines(symbol, selectedInterval, limit = 60)
            }
            if (initial.isNotEmpty()) {
                allCandles = initial
                isLoading = false
            }
        } catch (_: Exception) {}

        // Observe continuous WebSocket updates
        marketDataRepository.observeCandlesWithLive(symbol, selectedInterval).collectLatest { klines ->
            if (klines.isNotEmpty()) {
                allCandles = klines
                isLoading = false
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1326)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: Interval Selector, Zoom Reset, & Fullscreen
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Intervals
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("1m", "5m", "15m", "1h", "4h", "1d").forEach { interval ->
                        val isSel = interval == selectedInterval
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSel) MaterialTheme.colorScheme.primary else Color(0xFF1E293B),
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { selectedInterval = interval }
                                .padding(horizontal = 7.dp, vertical = 3.5.dp)
                        ) {
                            Text(
                                text = interval,
                                color = if (isSel) Color.White else Color(0xFF94A3B8),
                                fontSize = 10.5.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (zoomScale != 1f || panOffsetX != 0f || selectedCandle != null) {
                        IconButton(
                            onClick = {
                                zoomScale = 1f
                                panOffsetX = 0f
                                selectedCandle = null
                                crosshairOffset = null
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Reset Zoom", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                        }
                    }

                    if (onFullscreenClick != null) {
                        TextButton(
                            onClick = onFullscreenClick,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Icon(Icons.Filled.Fullscreen, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Full Pro ↗", color = Color(0xFF38BDF8), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Candle Detail Row (OHLC when touched/crosshair or latest)
            val displayCandle = selectedCandle ?: allCandles.lastOrNull()
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (displayCandle != null) {
                    val isBull = displayCandle.close >= displayCandle.open
                    val candleColor = if (isBull) SuccessGreen else ErrorRed
                    val timeStr = timeFormatter.format(Date(displayCandle.openTime))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("[$timeStr]", color = Color(0xFF64748B), fontSize = 9.sp)
                        Text("O: ${formatCryptoPrice(symbol, displayCandle.open)}", color = Color(0xFF94A3B8), fontSize = 9.sp)
                        Text("H: ${formatCryptoPrice(symbol, displayCandle.high)}", color = Color(0xFF94A3B8), fontSize = 9.sp)
                        Text("L: ${formatCryptoPrice(symbol, displayCandle.low)}", color = Color(0xFF94A3B8), fontSize = 9.sp)
                        Text("C: ${formatCryptoPrice(symbol, displayCandle.close)}", color = candleColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text("Menghubungkan WebSocket live candles...", color = Color(0xFF64748B), fontSize = 9.5.sp)
                }

                if (zoomScale > 1.05f || zoomScale < 0.95f) {
                    Text(
                        text = "${String.format(Locale.US, "%.1f", zoomScale)}x Zoom",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Canvas Chart Area with Spacious Uncompressed Candle Layout, Pan, Zoom & Crosshair
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(165.dp)
            ) {
                if (isLoading && allCandles.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                    }
                } else if (allCandles.isNotEmpty()) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(allCandles) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    zoomScale = (zoomScale * zoom).coerceIn(0.5f, 3.5f)
                                    panOffsetX = (panOffsetX + pan.x).coerceIn(-1000f, 1000f)
                                }
                            }
                            .pointerInput(allCandles) {
                                detectTapGestures(
                                    onPress = { tapOffset ->
                                        crosshairOffset = tapOffset
                                        val baseCandleCount = 26
                                        val minAllowed = minOf(8, allCandles.size).coerceAtLeast(1)
                                        val maxAllowed = allCandles.size.coerceAtLeast(minAllowed)
                                        val visibleCount = ((baseCandleCount / zoomScale).toInt()).coerceIn(minAllowed, maxAllowed)
                                        val candles = allCandles.takeLast(visibleCount)
                                        if (candles.isNotEmpty()) {
                                            val candleWidth = size.width / candles.size
                                            val idx = ((tapOffset.x) / candleWidth).toInt().coerceIn(0, candles.size - 1)
                                            selectedCandle = candles[idx]
                                        }
                                    },
                                    onTap = { tapOffset ->
                                        crosshairOffset = tapOffset
                                        val baseCandleCount = 26
                                        val minAllowed = minOf(8, allCandles.size).coerceAtLeast(1)
                                        val maxAllowed = allCandles.size.coerceAtLeast(minAllowed)
                                        val visibleCount = ((baseCandleCount / zoomScale).toInt()).coerceIn(minAllowed, maxAllowed)
                                        val candles = allCandles.takeLast(visibleCount)
                                        if (candles.isNotEmpty()) {
                                            val candleWidth = size.width / candles.size
                                            val idx = ((tapOffset.x) / candleWidth).toInt().coerceIn(0, candles.size - 1)
                                            selectedCandle = candles[idx]
                                        }
                                    }
                                )
                            }
                    ) {
                        val width = size.width
                        val height = size.height

                        // Spacious uncrowded display: 24-28 candles visible by default on mobile
                        val baseCandleCount = 26
                        val minAllowed = minOf(8, allCandles.size).coerceAtLeast(1)
                        val maxAllowed = allCandles.size.coerceAtLeast(minAllowed)
                        val visibleCount = ((baseCandleCount / zoomScale).toInt()).coerceIn(minAllowed, maxAllowed)
                        
                        // Calculate candle window with panning
                        val maxPanCandles = (allCandles.size - visibleCount).coerceAtLeast(0)
                        val candleStep = (width / visibleCount).coerceAtLeast(1f)
                        val panShiftCandles = if (maxPanCandles > 0) {
                            (-panOffsetX / candleStep).toInt().coerceIn(-maxPanCandles, 0)
                        } else {
                            0
                        }
                        val endIndex = (allCandles.size + panShiftCandles).coerceIn(visibleCount, allCandles.size)
                        val startIndex = (endIndex - visibleCount).coerceAtLeast(0)

                        val candles = if (startIndex <= endIndex && endIndex <= allCandles.size) {
                            allCandles.subList(startIndex, endIndex)
                        } else {
                            allCandles.takeLast(visibleCount)
                        }
                        val count = candles.size
                        if (count < 2) return@Canvas

                        val rawMinPrice = candles.minOf { it.low }
                        val rawMaxPrice = candles.maxOf { it.high }
                        val rawPriceRange = (rawMaxPrice - rawMinPrice).coerceAtLeast(1e-8)
                        
                        // Generous 12% vertical padding so candles have breathing room and don't look flat
                        val yPadding = rawPriceRange * 0.12
                        val minPrice = (rawMinPrice - yPadding).coerceAtLeast(0.0)
                        val maxPrice = rawMaxPrice + yPadding
                        val priceRange = (maxPrice - minPrice).coerceAtLeast(1e-8)
                        val maxVolume = candles.maxOfOrNull { it.volume } ?: 1.0

                        val candleAreaHeight = height * 0.76f
                        val volumeAreaHeight = height * 0.20f
                        val volumeBaseY = height

                        // Grid lines
                        val gridPaint = Color(0xFF1E293B)
                        val numHorizontalLines = 3
                        for (i in 1..numHorizontalLines) {
                            val y = candleAreaHeight * (i.toFloat() / (numHorizontalLines + 1))
                            drawLine(
                                color = gridPaint,
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                            )
                        }

                        val bodyWidth = (candleStep * 0.68f).coerceIn(3.5f, 22f)

                        candles.forEachIndexed { idx, kline ->
                            val isBull = kline.close >= kline.open
                            val candleColor = if (isBull) SuccessGreen else ErrorRed
                            val xCenter = idx * candleStep + (candleStep / 2f)

                            // 1. High-Low Wick
                            val highY = candleAreaHeight - ((kline.high - minPrice) / priceRange * candleAreaHeight).toFloat()
                            val lowY = candleAreaHeight - ((kline.low - minPrice) / priceRange * candleAreaHeight).toFloat()
                            drawLine(
                                color = candleColor,
                                start = Offset(xCenter, highY),
                                end = Offset(xCenter, lowY),
                                strokeWidth = 1.8f
                            )

                            // 2. Open-Close Body
                            val openY = candleAreaHeight - ((kline.open - minPrice) / priceRange * candleAreaHeight).toFloat()
                            val closeY = candleAreaHeight - ((kline.close - minPrice) / priceRange * candleAreaHeight).toFloat()
                            val bodyTop = min(openY, closeY)
                            val bodyHeight = max(abs(openY - closeY), 2.5f)

                            drawRect(
                                color = candleColor,
                                topLeft = Offset(xCenter - (bodyWidth / 2f), bodyTop),
                                size = Size(bodyWidth, bodyHeight)
                            )

                            // 3. Volume Bar at bottom
                            val volRatio = (kline.volume / max(maxVolume, 1e-6)).toFloat().coerceIn(0.05f, 1f)
                            val volHeight = volumeAreaHeight * volRatio
                            drawRect(
                                color = candleColor.copy(alpha = 0.4f),
                                topLeft = Offset(xCenter - (bodyWidth / 2f), volumeBaseY - volHeight),
                                size = Size(bodyWidth, volHeight)
                            )
                        }

                        // Last Price Line
                        val lastCandle = candles.last()
                        val lastPriceY = candleAreaHeight - ((lastCandle.close - minPrice) / priceRange * candleAreaHeight).toFloat()
                        val lastColor = if (lastCandle.close >= lastCandle.open) SuccessGreen else ErrorRed
                        drawLine(
                            color = lastColor.copy(alpha = 0.85f),
                            start = Offset(0f, lastPriceY),
                            end = Offset(width, lastPriceY),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                        )

                        // Crosshair Indicator when user touches chart
                        crosshairOffset?.let { offset ->
                            // Vertical Line
                            drawLine(
                                color = Color.White.copy(alpha = 0.6f),
                                start = Offset(offset.x, 0f),
                                end = Offset(offset.x, height),
                                strokeWidth = 1f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                            )
                            // Horizontal Line
                            drawLine(
                                color = Color.White.copy(alpha = 0.6f),
                                start = Offset(0f, offset.y.coerceIn(0f, candleAreaHeight)),
                                end = Offset(width, offset.y.coerceIn(0f, candleAreaHeight)),
                                strokeWidth = 1f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                            )
                        }

                        // Price labels
                        val textStyle = TextStyle(color = Color(0xFF64748B), fontSize = 8.5.sp, fontWeight = FontWeight.Normal)
                        val maxText = formatCryptoPrice(symbol, rawMaxPrice)
                        val minText = formatCryptoPrice(symbol, rawMinPrice)
                        drawText(
                            textMeasurer = textMeasurer,
                            text = maxText,
                            style = textStyle,
                            topLeft = Offset(width - 85.dp.toPx(), 2f)
                        )
                        drawText(
                            textMeasurer = textMeasurer,
                            text = minText,
                            style = textStyle,
                            topLeft = Offset(width - 85.dp.toPx(), candleAreaHeight - 14.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}

private fun abs(value: Float): Float = if (value < 0f) -value else value
