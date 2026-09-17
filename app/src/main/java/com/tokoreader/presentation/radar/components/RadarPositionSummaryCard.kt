package com.tokoreader.presentation.radar.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

/**
 * Kartu Ringkasan Posisi Aktif dengan pembedaan jelas antara:
 * 1. PnL Berjalan (Gross Unrealized PnL - Sebelum fee & pajak) + Estimasi nilai di bawah persentase
 * 2. Keuntungan / Kerugian Bersih (Net Realizable PnL - Setelah dipotong Fee CEX Tokocrypto 0.20% & Pajak Kripto PMK 68 0.21%) + Estimasi nilai bersih di bawah persentase
 * 3. Rincian potongan transparan (CEX Tokocrypto Maker/Taker + PPh 22 Final 0.10% + PPN 0.11%)
 */
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
    val quantity = position.quantity
    
    // 1. Kalkulasi Modal Beli dan Nilai Pasar Saat Ini
    val entryCost = entryPrice * quantity
    val currentGrossValue = currentPrice * quantity

    // 2. PnL Berjalan (Gross / Kotor - Sebelum Fee & Pajak)
    val grossPnlPercent = if (entryPrice > 0.0) ((currentPrice - entryPrice) / entryPrice) * 100 else 0.0
    val grossPnlAmount = (currentPrice - entryPrice) * quantity
    val isGrossProfitable = grossPnlPercent >= 0.0

    // 3. Riset & Kalkulasi Potongan Tokocrypto & Pajak Kripto RI (PMK 68/PMK.03/2022)
    // - Fee Trading Tokocrypto: 0.10% saat Beli dan 0.10% saat Jual (Maker & Taker standar)
    // - Pajak Transaksi Jual (Exchanger Bappebti Tokocrypto):
    //   * PPh Pasal 22 Final: 0.10%
    //   * PPN Kripto Efektif: 0.11%
    val buyFee = entryCost * 0.0010 // 0.10%
    val sellFee = currentGrossValue * 0.0010 // 0.10%
    val pph22Tax = currentGrossValue * 0.0010 // 0.10%
    val ppnTax = currentGrossValue * 0.0011 // 0.11%
    val totalDeduction = buyFee + sellFee + pph22Tax + ppnTax // Total ~0.41% round-trip

    // 4. Keuntungan / Kerugian Bersih (Net - Setelah Potongan)
    val netProfitAmount = (currentGrossValue - entryCost) - totalDeduction
    val netProfitPercent = if (entryCost > 0.0) (netProfitAmount / entryCost) * 100 else 0.0
    val isNetProfitable = netProfitAmount >= 0.0

    var showFeeDetails by remember { mutableStateOf(false) }

    fun formatCurrencyValue(amount: Double): String {
        return if (isUsdtPair) {
            val sign = if (amount > 0) "+" else if (amount < 0) "-" else ""
            val abs = kotlin.math.abs(amount)
            "$sign$ ${String.format(Locale.US, "%.2f", abs)}"
        } else {
            val sign = if (amount > 0) "+" else if (amount < 0) "-" else ""
            val abs = kotlin.math.abs(amount).toLong()
            "${sign}Rp ${NumberFormat.getNumberInstance(Locale.US).format(abs)}"
        }
    }

    fun formatFeeOnly(amount: Double): String {
        return if (isUsdtPair) {
            "$ ${String.format(Locale.US, "%.4f", amount)}"
        } else {
            "Rp ${NumberFormat.getNumberInstance(Locale.US).format(amount.toLong())}"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Baris Status Holding
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "Holding", tint = SuccessGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RADAR SELL (HOLDING)",
                        color = if (isNetProfitable) SuccessGreen else ErrorRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .background(
                            if (isNetProfitable) SuccessGreen.copy(alpha = 0.2f) else ErrorRed.copy(alpha = 0.2f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isNetProfitable) "SIAP TAKE PROFIT" else "CUT LOSS WATCH",
                        color = if (isNetProfitable) SuccessGreen else ErrorRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dashboard Perbandingan: PnL Berjalan (Gross) vs Keuntungan/Kerugian Bersih (Net Tokocrypto)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0B132B), RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Kolom 1: PnL Berjalan (Gross / Sebelum Potongan)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PnL Berjalan (Gross)",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${if (isGrossProfitable) "+" else ""}${String.format(Locale.US, "%.2f", grossPnlPercent)}%",
                        color = if (isGrossProfitable) SuccessGreen else ErrorRed,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    // Estimasi Nilai Kotor di bawah persentase
                    Text(
                        text = "Est. ${formatCurrencyValue(grossPnlAmount)}",
                        color = if (isGrossProfitable) SuccessGreen.copy(alpha = 0.9f) else ErrorRed.copy(alpha = 0.9f),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Belum potong fee",
                        color = Color(0xFF64748B),
                        fontSize = 9.5.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(65.dp)
                        .background(Color(0xFF1E293B))
                )

                // Kolom 2: Keuntungan / Kerugian Bersih (Net Setelah Fee & Pajak Tokocrypto)
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .padding(start = 12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AccountBalanceWallet,
                            contentDescription = null,
                            tint = if (isNetProfitable) SuccessGreen else ErrorRed,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Hasil Bersih (Net)",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${if (isNetProfitable) "+" else ""}${String.format(Locale.US, "%.2f", netProfitPercent)}%",
                        color = if (isNetProfitable) SuccessGreen else ErrorRed,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    // Estimasi Nilai Bersih di bawah persentase
                    Text(
                        text = "Est. ${formatCurrencyValue(netProfitAmount)}",
                        color = if (isNetProfitable) SuccessGreen else ErrorRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Setelah Fee & Pajak",
                        color = Color(0xFF38BDF8),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Expandable Banner Rincian Potongan Tokocrypto & Pajak Kripto
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1E293B).copy(alpha = 0.6f))
                    .clickable { showFeeDetails = !showFeeDetails }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "Fee Info",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Total Potongan Tokocrypto: -${formatFeeOnly(totalDeduction)} (~0.41%)",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Icon(
                    imageVector = if (showFeeDetails) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp)
                )
            }

            AnimatedVisibility(
                visible = showFeeDetails,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .background(Color(0xFF0B132B), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("• Fee Beli Tokocrypto (0.10%)", color = Color(0xFF94A3B8), fontSize = 10.sp)
                        Text(formatFeeOnly(buyFee), color = Color.White, fontSize = 10.sp)
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("• Fee Jual Tokocrypto (0.10%)", color = Color(0xFF94A3B8), fontSize = 10.sp)
                        Text(formatFeeOnly(sellFee), color = Color.White, fontSize = 10.sp)
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("• Pajak PPh 22 Final Kripto (0.10%)", color = Color(0xFF94A3B8), fontSize = 10.sp)
                        Text(formatFeeOnly(pph22Tax), color = Color.White, fontSize = 10.sp)
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("• Pajak PPN PMK 68 (0.11%)", color = Color(0xFF94A3B8), fontSize = 10.sp)
                        Text(formatFeeOnly(ppnTax), color = Color.White, fontSize = 10.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = Color(0xFF1E293B))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "*Dihitung otomatis sesuai tarif resmi Tokocrypto & PMK 68/PMK.03/2022 Bappebti",
                        color = Color(0xFF64748B),
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Data Aset & Harga Masuk
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Harga Beli (Avg)", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    Text(
                        CryptoUtils.formatCryptoPrice(symbol, entryPrice),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Aset Dimiliki", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    Text(
                        "${String.format(Locale.US, "%.6f", position.quantity)} ${symbol.removeSuffix("IDR").removeSuffix("USDT")}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tombol Eksekusi Sell
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
                        text = if (isPaperMode) "TUTUP POSISI / EKSEKUSI SELL PAPER" else "EKSEKUSI SELL REAL TOKOCRYPTO",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
