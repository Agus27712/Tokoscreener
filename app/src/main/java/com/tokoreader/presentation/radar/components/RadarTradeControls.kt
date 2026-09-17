package com.tokoreader.presentation.radar.components

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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokoreader.presentation.components.CryptoUtils
import com.tokoreader.presentation.radar.RadarTradeUiState
import com.tokoreader.ui.theme.ErrorRed
import com.tokoreader.ui.theme.SuccessGreen
import java.text.NumberFormat
import java.util.Locale

@Composable
fun RadarTradeControls(
    uiState: RadarTradeUiState,
    currentPrice: Double,
    isUsdtPair: Boolean,
    onSelectNominal: (Double) -> Unit,
    onOpenSwapDialog: () -> Unit,
    onExecuteBuy: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Sequential Validation Flow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Validasi Alur Sinyal (Step 1-4)",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (uiState.step4Pass) "Sinyal Valid ✓" else "Proses Konfirmasi",
                    color = if (uiState.step4Pass) SuccessGreen else Color(0xFFF59E0B),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            SequentialStepperWithProgress(
                step1Pass = uiState.step1Pass,
                step1Detail = uiState.step1Detail,
                step2Pass = uiState.step2Pass,
                step2Detail = uiState.step2Detail,
                step3Pass = uiState.step3Pass,
                step3Detail = uiState.step3Detail,
                step4Pass = uiState.step4Pass,
                step4Detail = uiState.step4Detail,
                progress1To2 = uiState.progressStep1To2,
                progress2To3 = uiState.progressStep2To3,
                progress3To4 = uiState.progressStep3To4
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Balance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isUsdtPair) "Saldo USDT:" else "Saldo IDR:",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                    if (uiState.isPaperMode) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isUsdtPair) "[+ Beli/Swap USDT]" else "[+ Swap USDT]",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onOpenSwapDialog() }
                        )
                    }
                }
                Text(
                    text = if (uiState.isPaperMode) {
                        if (isUsdtPair) "$ ${String.format(Locale.US, "%,.2f", uiState.paperBalanceUsdt)}"
                        else "Rp ${NumberFormat.getNumberInstance(Locale.US).format(uiState.paperBalanceIdr.toLong())}"
                    } else "Sesuai Akun Tokocrypto",
                    color = if (isUsdtPair && uiState.paperBalanceUsdt < uiState.selectedNominal) ErrorRed else Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Nominal Selection (Dynamically adapts to USDT vs IDR quote currency)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val nominals = if (isUsdtPair) {
                    listOf(
                        25.0 to "$25",
                        50.0 to "$50",
                        100.0 to "$100",
                        250.0 to "$250",
                        500.0 to "$500"
                    )
                } else {
                    listOf(
                        500_000.0 to "500rb",
                        1_000_000.0 to "1jt",
                        2_500_000.0 to "2.5jt",
                        5_000_000.0 to "5jt",
                        10_000_000.0 to "10jt"
                    )
                }

                nominals.forEach { (amount, label) ->
                    val isSelected = uiState.selectedNominal == amount
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF1E293B),
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { onSelectNominal(amount) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dynamic Targets based on strategy mode (Scalping, Intraday, Swing)
            val (tp1Percent, tp2Percent, slPercent) = when (uiState.strategyMode) {
                "Scalping" -> Triple(0.012, 0.025, 0.010)
                "Intraday" -> Triple(0.025, 0.050, 0.020)
                "Swing" -> Triple(0.060, 0.120, 0.040)
                else -> Triple(0.020, 0.045, 0.018)
            }

            val tp1Price = currentPrice * (1.0 + tp1Percent)
            val tp2Price = currentPrice * (1.0 + tp2Percent)
            val tp1EstGain = uiState.selectedNominal * tp1Percent
            val tp2EstGain = uiState.selectedNominal * tp2Percent

            val tp1GainFormatted = if (isUsdtPair) {
                "+$ ${String.format(Locale.US, "%.2f", tp1EstGain)}"
            } else {
                "+Rp ${NumberFormat.getNumberInstance(Locale.US).format(tp1EstGain.toLong())}"
            }

            val tp2GainFormatted = if (isUsdtPair) {
                "+$ ${String.format(Locale.US, "%.2f", tp2EstGain)}"
            } else {
                "+Rp ${NumberFormat.getNumberInstance(Locale.US).format(tp2EstGain.toLong())}"
            }

            val tp1Label = "+${String.format(Locale.US, "%.1f", tp1Percent * 100)}%"
            val tp2Label = "+${String.format(Locale.US, "%.1f", tp2Percent * 100)}%"

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Entry Live", color = Color(0xFF94A3B8), fontSize = 10.sp, maxLines = 1)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            CryptoUtils.formatCryptoPrice(uiState.symbol, currentPrice),
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text("${uiState.strategyMode}", color = Color(0xFF64748B), fontSize = 9.sp)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("TP 1 ($tp1Label)", color = SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            CryptoUtils.formatCryptoPrice(uiState.symbol, tp1Price),
                            color = SuccessGreen,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(tp1GainFormatted, color = SuccessGreen.copy(alpha = 0.8f), fontSize = 9.sp, maxLines = 1)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("TP 2 ($tp2Label)", color = SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            CryptoUtils.formatCryptoPrice(uiState.symbol, tp2Price),
                            color = SuccessGreen,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(tp2GainFormatted, color = SuccessGreen.copy(alpha = 0.8f), fontSize = 9.sp, maxLines = 1)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Est Qty & Confidence
            val estQty = if (currentPrice > 0.0) uiState.selectedNominal / currentPrice else 0.0
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Fee: 0.10%", color = Color(0xFF94A3B8), fontSize = 11.sp)
                Text("Est. Qty: ${String.format(Locale.US, "%.6f", estQty)}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text("Keyakinan AI: ${uiState.orderConfidence}%", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Button
            val canEntry = uiState.step4Pass
            val waitingStepReason = when {
                !uiState.step1Pass -> "Menunggu Step 1 (Bias)"
                !uiState.step2Pass -> "Menunggu Step 2 (Setup)"
                !uiState.step3Pass -> "Menunggu Step 3 (Trigger)"
                else -> "Sinyal Siap"
            }

            Button(
                onClick = onExecuteBuy,
                enabled = !uiState.isExecuting && canEntry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canEntry) SuccessGreen else Color(0xFF1E293B),
                    disabledContainerColor = Color(0xFF1E293B).copy(alpha = 0.6f),
                    contentColor = Color.White,
                    disabledContentColor = Color(0xFF64748B)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (uiState.isExecuting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Icon(
                        if (canEntry) Icons.Filled.RocketLaunch else Icons.Filled.Lock,
                        contentDescription = "Action",
                        tint = if (canEntry) Color.White else Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val formattedNominal = if (isUsdtPair) {
                        "$ ${String.format(Locale.US, "%.0f", uiState.selectedNominal)}"
                    } else {
                        "Rp ${NumberFormat.getNumberInstance(Locale.US).format(uiState.selectedNominal.toLong())}"
                    }
                    val actionText = if (uiState.isPaperMode) {
                        if (canEntry) "Paper Buy $formattedNominal" else waitingStepReason
                    } else {
                        if (canEntry) "Eksekusi Real Buy Tokocrypto ($formattedNominal)" else waitingStepReason
                    }
                    Text(
                        text = actionText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                }
            }
        }
    }
}
