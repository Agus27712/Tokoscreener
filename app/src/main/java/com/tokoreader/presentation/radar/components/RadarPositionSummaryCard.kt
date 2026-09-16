package com.tokoreader.presentation.radar.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokoreader.domain.model.Position
import com.tokoreader.presentation.components.CryptoUtils
import com.tokoreader.ui.theme.ErrorRed
import com.tokoreader.ui.theme.SuccessGreen
import java.text.NumberFormat
import java.util.Locale

@Composable
fun RadarPositionSummaryCard(
    position: Position,
    currentPrice: Double,
    symbol: String,
    isPaperMode: Boolean,
    isExecuting: Boolean,
    isUsdtPair: Boolean,
    onExecuteSell: () -> Unit
) {
    val entryPrice = position.averageEntryPrice
    val pnlPercent = if (entryPrice > 0.0) ((currentPrice - entryPrice) / entryPrice) * 100 else 0.0
    val pnlAmount = (currentPrice - entryPrice) * position.quantity
    val isProfitable = pnlPercent >= 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "Holding", tint = SuccessGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("RADAR SELL (HOLDING)", color = ErrorRed, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Box(
                    modifier = Modifier
                        .background(if (isProfitable) SuccessGreen.copy(alpha = 0.2f) else ErrorRed.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isProfitable) "SIAP PROFIT" else "CUT LOSS WATCH",
                        color = if (isProfitable) SuccessGreen else ErrorRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // PnL Performance Dashboard
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("PnL Berjalan", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    Text(
                        text = "${if (isProfitable) "+" else ""}${String.format(Locale.US, "%.2f", pnlPercent)}%",
                        color = if (isProfitable) SuccessGreen else ErrorRed,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Estimasi Keuntungan/Rugi", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    val formattedPnlGain = if (isUsdtPair) {
                        "${if (isProfitable) "+" else ""}$ ${String.format(Locale.US, "%.2f", pnlAmount)}"
                    } else {
                        "${if (isProfitable) "+" else ""}Rp ${NumberFormat.getNumberInstance(Locale.US).format(pnlAmount.toLong())}"
                    }
                    Text(
                        text = formattedPnlGain,
                        color = if (isProfitable) SuccessGreen else ErrorRed,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Harga Beli (Avg)", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    Text(CryptoUtils.formatCryptoPrice(symbol, entryPrice), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Aset Dimiliki", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    Text("${String.format(Locale.US, "%.6f", position.quantity)} ${symbol.removeSuffix("IDR").removeSuffix("USDT")}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onExecuteSell,
                enabled = !isExecuting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isExecuting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Icon(Icons.Filled.Sell, contentDescription = "Sell", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPaperMode) "TUTUP POSISI / EKSEKUSI SELL PAPER" else "EKSEKUSI SELL REAL",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
