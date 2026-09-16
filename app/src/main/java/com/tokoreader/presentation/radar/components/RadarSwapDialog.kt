package com.tokoreader.presentation.radar.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokoreader.ui.theme.SuccessGreen
import java.text.NumberFormat
import java.util.Locale

@Composable
fun RadarSwapDialog(
    usdtRate: Double,
    paperBalanceIdr: Double,
    paperBalanceUsdt: Double,
    initialDirectionToUsdt: Boolean,
    onDismissRequest: () -> Unit,
    onConfirmSwap: (fromCurrency: String, toCurrency: String, amount: Double) -> Unit
) {
    var swapAmountInput by remember { mutableStateOf("") }
    val rate = usdtRate.coerceAtLeast(1000.0)

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                if (initialDirectionToUsdt) "Konversi IDR ke USDT" else "Konversi USDT ke IDR",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column {
                Text(
                    "Kurs Live: 1 USDT = Rp ${NumberFormat.getNumberInstance(Locale.US).format(rate.toLong())}",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Saldo IDR: Rp ${NumberFormat.getNumberInstance(Locale.US).format(paperBalanceIdr.toLong())}\nSaldo USDT: $ ${String.format(Locale.US, "%.2f", paperBalanceUsdt)}",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.5.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = swapAmountInput,
                    onValueChange = { swapAmountInput = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(if (initialDirectionToUsdt) "Nominal IDR yang ditukar" else "Nominal USDT yang ditukar") },
                    placeholder = { Text(if (initialDirectionToUsdt) "Contoh: 1620000" else "Contoh: 100") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                val inputVal = swapAmountInput.toDoubleOrNull() ?: 0.0
                val estReceive = if (initialDirectionToUsdt) {
                    if (rate > 0) inputVal / rate else 0.0
                } else {
                    inputVal * rate
                }
                if (inputVal > 0) {
                    Text(
                        text = if (initialDirectionToUsdt) "Perkiraan didapat: $ ${String.format(Locale.US, "%.2f", estReceive)} USDT" else "Perkiraan didapat: Rp ${NumberFormat.getNumberInstance(Locale.US).format(estReceive.toLong())}",
                        color = SuccessGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = swapAmountInput.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        if (initialDirectionToUsdt) {
                            onConfirmSwap("IDR", "USDT", amt)
                        } else {
                            onConfirmSwap("USDT", "IDR", amt)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Konversi Sekarang", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Batal")
            }
        }
    )
}
