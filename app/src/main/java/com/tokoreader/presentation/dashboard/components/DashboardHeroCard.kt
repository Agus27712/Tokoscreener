package com.tokoreader.presentation.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokoreader.domain.model.Ticker
import com.tokoreader.domain.repository.MarketDataRepository
import com.tokoreader.presentation.chart.TokocryptoMiniChart
import com.tokoreader.presentation.components.CryptoUtils
import com.tokoreader.presentation.components.LivePriceFlashText
import com.tokoreader.ui.theme.ErrorRed
import com.tokoreader.ui.theme.SuccessGreen
import java.util.Locale

@Composable
fun DashboardHeroCard(
    hero: Ticker,
    marketDataRepository: MarketDataRepository,
    onClick: () -> Unit
) {
    val isUp = hero.priceChangePercent >= 0
    val changeSign = if (isUp) "+" else ""
    val (baseSymbol, quoteSymbol) = CryptoUtils.splitSymbol(hero.symbol)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Section: Symbol, Price + Percentage, Badge
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$baseSymbol/$quoteSymbol",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LivePriceFlashText(
                            price = hero.price,
                            symbol = hero.symbol,
                            tickDirection = hero.tickDirection,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${changeSign}${String.format(Locale.US, "%.2f", hero.priceChangePercent)}%",
                            color = if (isUp) SuccessGreen else ErrorRed,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    // AI Signal Pill
                    Box(
                        modifier = Modifier
                            .border(
                                1.dp,
                                if (isUp) Color(0xFF059669).copy(alpha = 0.6f) else Color(0xFFDC2626).copy(alpha = 0.6f),
                                RoundedCornerShape(8.dp)
                            )
                            .background(
                                if (isUp) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFF7F1D1D).copy(alpha = 0.35f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isUp) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = "Trend",
                                tint = if (isUp) SuccessGreen else ErrorRed,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "SCALPING · AI Signal",
                                color = if (isUp) SuccessGreen else ErrorRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Right Section: Mini Candlestick Sparkline
                TokocryptoMiniChart(
                    symbol = hero.symbol,
                    priceChangePercent = hero.priceChangePercent,
                    marketDataRepository = marketDataRepository,
                    modifier = Modifier
                        .size(width = 120.dp, height = 75.dp)
                        .padding(start = 8.dp),
                    strokeWidth = 2.dp,
                    showEndGlow = true
                )
            }
        }
    }
}
