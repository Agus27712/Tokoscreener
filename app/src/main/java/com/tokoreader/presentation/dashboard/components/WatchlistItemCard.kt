package com.tokoreader.presentation.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokoreader.domain.model.Ticker
import com.tokoreader.domain.repository.MarketDataRepository
import com.tokoreader.presentation.chart.TokocryptoMiniChart
import com.tokoreader.presentation.components.CryptoCoinAvatar
import com.tokoreader.presentation.components.CryptoUtils
import com.tokoreader.presentation.components.LivePriceFlashText
import com.tokoreader.presentation.components.StatusBadge
import com.tokoreader.ui.theme.ErrorRed
import com.tokoreader.ui.theme.SuccessGreen
import java.util.Locale

@Composable
fun WatchlistItemCard(
    ticker: Ticker,
    marketDataRepository: MarketDataRepository,
    onClick: () -> Unit
) {
    val isUp = ticker.priceChangePercent >= 0
    val sign = if (isUp) "+" else ""
    val changeColor = if (isUp) SuccessGreen else ErrorRed
    val (baseSymbol, quoteSymbol) = CryptoUtils.splitSymbol(ticker.symbol)

    // Determine badge state
    val (badgeText, isHot, isReadySell) = when {
        ticker.priceChangePercent >= 3.0 -> Triple("🔥 HOT", true, false)
        ticker.priceChangePercent > 0 -> Triple("READY SELL", false, true)
        else -> Triple("⏱ WAITING", false, false)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // 1. Top Section: Left (Avatar + Pair + Badge + Full Coin Name) & Right (Price + Percent Pill)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Coin Identity
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CryptoCoinAvatar(symbol = baseSymbol, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.width(9.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (quoteSymbol.isNotEmpty()) "$baseSymbol/$quoteSymbol" else baseSymbol,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            StatusBadge(badgeText = badgeText, isHot = isHot, isReadySell = isReadySell)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = CryptoUtils.getCoinName(baseSymbol),
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Right: Realtime Price & Percent
                Column(horizontalAlignment = Alignment.End) {
                    LivePriceFlashText(
                        price = ticker.price,
                        symbol = ticker.symbol,
                        tickDirection = ticker.tickDirection,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .background(changeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${sign}${String.format(Locale.US, "%.2f", ticker.priceChangePercent)}%",
                            color = changeColor,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Full-Width Sparkline from Left to Right
            TokocryptoMiniChart(
                symbol = ticker.symbol,
                priceChangePercent = ticker.priceChangePercent,
                marketDataRepository = marketDataRepository,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                strokeWidth = 2.dp,
                showEndGlow = true
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 3. Bottom Row: Aligned Orderbook & 24h Volume & Signal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Orderbook Pressure
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Orderbook: ",
                        color = Color(0xFF64748B),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        text = if (isUp) "↑ Buyer Dominan" else "↓ Seller Tekan",
                        color = changeColor,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // 24h Volume
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Vol 24j: ",
                        color = Color(0xFF64748B),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        text = CryptoUtils.formatCryptoVolume(ticker.symbol, ticker.volume24h),
                        color = Color(0xFFCBD5E1),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
