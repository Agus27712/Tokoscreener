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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
    onClick: () -> Unit
) {
    val isUp = ticker.priceChangePercent >= 0
    val sign = if (isUp) "+" else ""
    val changeColor = if (isUp) SuccessGreen else ErrorRed
    val (baseSymbol, quoteSymbol) = CryptoUtils.splitSymbol(ticker.symbol)

    // Determine badge status
    val (badgeText, isHot, isReadySell) = when {
        ticker.priceChangePercent >= 3.0 -> Triple("🔥 HOT", true, false)
        ticker.priceChangePercent > 0 -> Triple("READY SELL", false, true)
        else -> Triple("⏱ WAITING", false, false)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 5.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // 1. Top Section: Identity & Live Price with Percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Avatar + Pair Name + Badge + Full Coin Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    CryptoCoinAvatar(symbol = baseSymbol, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (quoteSymbol.isNotEmpty()) "$baseSymbol/$quoteSymbol" else baseSymbol,
                                color = Color.White,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            StatusBadge(badgeText = badgeText, isHot = isHot, isReadySell = isReadySell)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = CryptoUtils.getCoinName(baseSymbol),
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Right: Realtime Price + Change Percent Tag
                Column(horizontalAlignment = Alignment.End) {
                    LivePriceFlashText(
                        price = ticker.price,
                        symbol = ticker.symbol,
                        tickDirection = ticker.tickDirection,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .background(changeColor.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${sign}${String.format(Locale.US, "%.2f", ticker.priceChangePercent)}%",
                            color = changeColor,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Divider line subtle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFF1E293B).copy(alpha = 0.6f))
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Bottom Metrics Row: Volume 24H (Bigger) & High/Low 24H
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Volume 24h (Prominent)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Vol 24j: ",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = CryptoUtils.formatCryptoVolume(ticker.symbol, ticker.volume24h),
                        color = Color(0xFFE2E8F0),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 24h High / Low or Orderbook status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (ticker.high24h > 0 && ticker.low24h > 0) {
                        Text(
                            text = "H: ${CryptoUtils.formatCryptoPrice(ticker.symbol, ticker.high24h)}",
                            color = Color(0xFF10B981),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "L: ${CryptoUtils.formatCryptoPrice(ticker.symbol, ticker.low24h)}",
                            color = Color(0xFFEF4444),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            text = if (isUp) "Buyer Aktif" else "Seller Tekan",
                            color = changeColor,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = "Detail",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
