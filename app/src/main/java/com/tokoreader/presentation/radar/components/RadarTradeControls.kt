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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
    onManualNominalChange: (String) -> Unit = {},
    onManualTpChange: (Double?, Double?) -> Unit = { _, _ -> },
    onToggleManualTp: (Boolean) -> Unit = {},
    onOpenSwapDialog: () -> Unit,
    onExecuteBuy: () -> Unit
) {
    var tp1Input by remember { mutableStateOf("") }
    var tp2Input by remember { mutableStateOf("") }

    // Sync initial default TP values if manual mode turned on
    LaunchedEffect(uiState.isManualTp, currentPrice) {
        if (uiState.isManualTp && currentPrice > 0.0) {
            if (tp1Input.isBlank()) {
                val defaultTp1 = currentPrice * 1.015
                tp1Input = String.format(Locale.US, if (isUsdtPair) "%.4f" else "%.0f", defaultTp1)
            }
            if (tp2Input.isBlank()) {
                val defaultTp2 = currentPrice * 1.030
                tp2Input = String.format(Locale.US, if (isUsdtPair) "%.4f" else "%.0f", defaultTp2)
            }
            val p1 = tp1Input.replace(",", ".").toDoubleOrNull()
            val p2 = tp2Input.replace(",", ".").toDoubleOrNull()
            onManualTpChange(p1, p2)
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

            // Balance Row
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

            // Nominal Selection Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nominal Trading",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isUsdtPair) "$ ${String.format(Locale.US, "%.2f", uiState.selectedNominal)}" else "Rp ${NumberFormat.getNumberInstance(Locale.US).format(uiState.selectedNominal.toLong())}",
                    color = SuccessGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Nominal Preset Buttons
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
                    val isSelected = !uiState.isCustomNominal && uiState.selectedNominal == amount
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF1E293B),
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { onSelectNominal(amount) }
                            .padding(vertical = 7.dp),
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

            Spacer(modifier = Modifier.height(8.dp))

            // Manual Nominal Input Field
            OutlinedTextField(
                value = uiState.manualNominalInput,
                onValueChange = { onManualNominalChange(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Input Nominal Manual (${if (isUsdtPair) "USDT" else "Rupiah"})", fontSize = 11.sp) },
                leadingIcon = {
                    Text(
                        text = if (isUsdtPair) "  $" else "  Rp",
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit Manual",
                        tint = if (uiState.isCustomNominal) SuccessGreen else Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = SuccessGreen,
                    unfocusedBorderColor = if (uiState.isCustomNominal) SuccessGreen.copy(alpha = 0.6f) else Color(0xFF334155),
                    focusedContainerColor = Color(0xFF1E293B),
                    unfocusedContainerColor = Color(0xFF1E293B)
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Take Profit (TP) Section Header with Auto vs Manual Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Take Profit (TP 1 & TP 2)",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = !uiState.isManualTp,
                        onClick = { onToggleManualTp(false) },
                        label = { Text("Auto (${uiState.strategyMode})", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFF94A3B8)
                        ),
                        border = BorderStroke(1.dp, if (!uiState.isManualTp) MaterialTheme.colorScheme.primary else Color(0xFF334155)),
                        modifier = Modifier.height(28.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    FilterChip(
                        selected = uiState.isManualTp,
                        onClick = { onToggleManualTp(true) },
                        label = { Text("Manual TP", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SuccessGreen,
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFF94A3B8)
                        ),
                        border = BorderStroke(1.dp, if (uiState.isManualTp) SuccessGreen else Color(0xFF334155)),
                        modifier = Modifier.height(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Dynamic Targets based on strategy mode (Scalping, Intraday, Swing) or Manual TP
            if (!uiState.isManualTp) {
                // Auto Mode Targets
                val (tp1Percent, tp2Percent) = when (uiState.strategyMode) {
                    "Scalping" -> Pair(0.012, 0.025)
                    "Intraday" -> Pair(0.025, 0.050)
                    "Swing" -> Pair(0.060, 0.120)
                    else -> Pair(0.020, 0.045)
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
                            Text(uiState.strategyMode, color = Color(0xFF64748B), fontSize = 9.sp)
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
            } else {
                // Manual TP Inputs
                val manualTp1Parsed = tp1Input.replace(",", ".").toDoubleOrNull()
                val manualTp2Parsed = tp2Input.replace(",", ".").toDoubleOrNull()

                val p1Pct = if (currentPrice > 0.0 && manualTp1Parsed != null) {
                    ((manualTp1Parsed - currentPrice) / currentPrice) * 100.0
                } else null

                val p2Pct = if (currentPrice > 0.0 && manualTp2Parsed != null) {
                    ((manualTp2Parsed - currentPrice) / currentPrice) * 100.0
                } else null

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // TP 1 Field
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = tp1Input,
                                onValueChange = {
                                    tp1Input = it
                                    val p1 = it.replace(",", ".").toDoubleOrNull()
                                    onManualTpChange(p1, manualTp2Parsed)
                                },
                                label = { Text("Harga TP 1", fontSize = 10.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = SuccessGreen,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = SuccessGreen,
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedContainerColor = Color(0xFF1E293B),
                                    unfocusedContainerColor = Color(0xFF1E293B)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                            if (p1Pct != null) {
                                Text(
                                    text = if (p1Pct >= 0) "+${String.format(Locale.US, "%.2f", p1Pct)}% Profit" else "${String.format(Locale.US, "%.2f", p1Pct)}%",
                                    color = if (p1Pct >= 0) SuccessGreen else ErrorRed,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 2.dp, start = 4.dp)
                                )
                            }
                        }

                        // TP 2 Field
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = tp2Input,
                                onValueChange = {
                                    tp2Input = it
                                    val p2 = it.replace(",", ".").toDoubleOrNull()
                                    onManualTpChange(manualTp1Parsed, p2)
                                },
                                label = { Text("Harga TP 2", fontSize = 10.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = SuccessGreen,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = SuccessGreen,
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedContainerColor = Color(0xFF1E293B),
                                    unfocusedContainerColor = Color(0xFF1E293B)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                            if (p2Pct != null) {
                                Text(
                                    text = if (p2Pct >= 0) "+${String.format(Locale.US, "%.2f", p2Pct)}% Profit" else "${String.format(Locale.US, "%.2f", p2Pct)}%",
                                    color = if (p2Pct >= 0) SuccessGreen else ErrorRed,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 2.dp, start = 4.dp)
                                )
                            }
                        }
                    }

                    // Quick Percentage chips to set TP easily
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0.01 to "+1%", 0.02 to "+2%", 0.03 to "+3%", 0.05 to "+5%").forEach { (pct, label) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1E293B))
                                    .clickable {
                                        if (currentPrice > 0.0) {
                                            val t1 = currentPrice * (1.0 + pct)
                                            val t2 = currentPrice * (1.0 + pct * 2.0)
                                            tp1Input = String.format(Locale.US, if (isUsdtPair) "%.4f" else "%.0f", t1)
                                            tp2Input = String.format(Locale.US, if (isUsdtPair) "%.4f" else "%.0f", t2)
                                            onManualTpChange(t1, t2)
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = label, color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
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
