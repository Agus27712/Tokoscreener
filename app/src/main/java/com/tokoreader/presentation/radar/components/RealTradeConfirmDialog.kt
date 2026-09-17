package com.tokoreader.presentation.radar.components

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
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokoreader.presentation.components.CryptoUtils
import com.tokoreader.ui.theme.ErrorRed
import com.tokoreader.ui.theme.SuccessGreen
import java.text.NumberFormat
import java.util.Locale

@Composable
fun RealTradeConfirmDialog(
    isBuy: Boolean,
    symbol: String,
    currentPrice: Double,
    nominal: Double,
    quantity: Double,
    isUsdtPair: Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
) {
    val actionName = if (isBuy) "REAL BUY SPOT" else "REAL SELL SPOT"
    val actionColor = if (isBuy) SuccessGreen else ErrorRed

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xFF0F172A),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.WarningAmber,
                    contentDescription = "Warning",
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Konfirmasi $actionName",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF59E0B).copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "PERINGATAN: Order ini akan langsung dikirim ke exchange Tokocrypto menggunakan saldo & API Key akun asli Anda.",
                        color = Color(0xFFFCD34D),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Detail Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailRow(label = "Pasangan Koin", value = symbol)
                    DetailRow(label = "Tipe Order", value = "MARKET ORDER")
                    DetailRow(label = "Harga Estimasi", value = CryptoUtils.formatCryptoPrice(symbol, currentPrice))
                    if (isBuy) {
                        val formattedNominal = if (isUsdtPair) {
                            "$ ${String.format(Locale.US, "%,.2f", nominal)}"
                        } else {
                            "Rp ${NumberFormat.getNumberInstance(Locale.US).format(nominal.toLong())}"
                        }
                        DetailRow(label = "Nominal Eksekusi", value = formattedNominal)
                    }
                    DetailRow(label = "Kuantitas", value = "${String.format(Locale.US, "%.6f", quantity)} ${symbol.removeSuffix("IDR").removeSuffix("USDT")}")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismissRequest()
                },
                colors = ButtonDefaults.buttonColors(containerColor = actionColor),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (isBuy) "Ya, Kirim Real Buy" else "Ya, Kirim Real Sell",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismissRequest,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Batal", color = Color(0xFF94A3B8))
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 12.sp)
        Text(text = value, color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
    }
}
