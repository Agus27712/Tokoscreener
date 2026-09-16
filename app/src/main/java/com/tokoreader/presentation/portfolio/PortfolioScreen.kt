package com.tokoreader.presentation.portfolio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tokoreader.presentation.chart.FullscreenTradingViewDialog
import com.tokoreader.presentation.components.CryptoUtils
import com.tokoreader.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    viewModel: PortfolioViewModel = viewModel(factory = PortfolioViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showResetDialog by remember { mutableStateOf(false) }
    var showSwapDialog by remember { mutableStateOf(false) }
    var swapAmountInput by remember { mutableStateOf("") }
    var swapDirectionToUsdt by remember { mutableStateOf(true) }
    var selectedChartSymbol by remember { mutableStateOf<String?>(null) }

    if (selectedChartSymbol != null) {
        FullscreenTradingViewDialog(
            symbol = selectedChartSymbol!!,
            onDismissRequest = { selectedChartSymbol = null }
        )
    }

    if (showSwapDialog) {
        val rate = uiState.usdtRate.coerceAtLeast(1000.0)
        AlertDialog(
            onDismissRequest = { showSwapDialog = false },
            title = {
                Text(
                    if (swapDirectionToUsdt) "Konversi IDR ke USDT" else "Konversi USDT ke IDR",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column {
                    Text(
                        "Kurs Live Tokocrypto: 1 USDT = Rp ${NumberFormat.getNumberInstance(Locale.US).format(rate.toLong())}",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Saldo IDR: Rp ${NumberFormat.getNumberInstance(Locale.US).format(uiState.cashBalanceIdr.toLong())}\nSaldo USDT: $ ${String.format(Locale.US, "%.2f", uiState.cashBalanceUsdt)}",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = swapAmountInput,
                        onValueChange = { swapAmountInput = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(if (swapDirectionToUsdt) "Nominal IDR yang ditukar" else "Nominal USDT yang ditukar") },
                        placeholder = { Text(if (swapDirectionToUsdt) "Contoh: 1620000" else "Contoh: 100") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val inputVal = swapAmountInput.toDoubleOrNull() ?: 0.0
                    val estReceive = if (swapDirectionToUsdt) {
                        if (rate > 0) inputVal / rate else 0.0
                    } else {
                        inputVal * rate
                    }
                    if (inputVal > 0) {
                        Text(
                            text = if (swapDirectionToUsdt) "Perkiraan didapat: $ ${String.format(Locale.US, "%.2f", estReceive)} USDT" else "Perkiraan didapat: Rp ${NumberFormat.getNumberInstance(Locale.US).format(estReceive.toLong())}",
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
                            if (swapDirectionToUsdt) {
                                viewModel.swapCurrency("IDR", "USDT", amt)
                            } else {
                                viewModel.swapCurrency("USDT", "IDR", amt)
                            }
                            showSwapDialog = false
                            swapAmountInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Konversi Sekarang", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSwapDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Akun Paper Trade?", fontWeight = FontWeight.Bold) },
            text = { Text("Saldo virtual akan dikembalikan ke Rp 100.000.000 + $5.000 USDT dan seluruh riwayat serta posisi terbuka akan dihapus.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetPaperAccount()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Reset Sekarang", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Portofolio Trading",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                actions = {
                    // Reset Button
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(Icons.Filled.RestartAlt, contentDescription = "Reset Paper Account", tint = Color(0xFF94A3B8))
                    }
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("PAPER TRADING", color = MaterialTheme.colorScheme.primary, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Notification Message
            uiState.notificationMessage?.let { msg ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, SuccessGreen)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(msg, color = SuccessGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            IconButton(onClick = { viewModel.clearNotification() }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "Tutup", tint = Color.White)
                            }
                        }
                    }
                }
            }

            // Portfolio Balance Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
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
                            Text("Total Net Worth Portofolio (IDR Eq):", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text(
                                text = "1 USDT ≈ Rp ${NumberFormat.getNumberInstance(Locale.US).format(uiState.usdtRate.toLong())}",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Rp ${NumberFormat.getNumberInstance(Locale.US).format(uiState.totalPortfolioValueIdr.toLong())}",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Dual Cash Balances: IDR & USDT
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Saldo IDR", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Rp ${NumberFormat.getNumberInstance(Locale.US).format(uiState.cashBalanceIdr.toLong())}",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Saldo USDT", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$ ${String.format(Locale.US, "%,.2f", uiState.cashBalanceUsdt)}",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Swap Button & Total PnL
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { 
                                    swapDirectionToUsdt = true
                                    showSwapDialog = true 
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Filled.SwapHoriz, contentDescription = "Swap", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Konversi IDR ⇄ USDT", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                val isProfitable = uiState.totalPnlPercent >= 0
                                Text("Total PnL", color = Color(0xFF94A3B8), fontSize = 10.5.sp)
                                Text(
                                    text = "${if (isProfitable) "+" else ""}Rp ${NumberFormat.getNumberInstance(Locale.US).format(uiState.totalPnlIdr.toLong())} (${String.format(Locale.US, "%.2f", uiState.totalPnlPercent)}%)",
                                    color = if (isProfitable) SuccessGreen else ErrorRed,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Asset Allocation Bar
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text("Alokasi Portofolio", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    val total = uiState.totalPortfolioValueIdr.coerceAtLeast(1.0)
                    val rate = uiState.usdtRate.coerceAtLeast(1000.0)
                    val cashIdrWeight = (uiState.cashBalanceIdr / total).toFloat().coerceIn(0.005f, 1f)
                    val cashUsdtWeight = ((uiState.cashBalanceUsdt * rate) / total).toFloat().coerceIn(0.005f, 1f)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .background(Color(0xFF1E293B), RoundedCornerShape(4.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(cashIdrWeight)
                                .fillMaxHeight()
                                .background(Color(0xFF3B82F6), RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (cashIdrWeight > 0.12f) {
                                Text("IDR ${(cashIdrWeight * 100).toInt()}%", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(cashUsdtWeight)
                                .fillMaxHeight()
                                .background(Color(0xFF0EA5E9)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (cashUsdtWeight > 0.12f) {
                                Text("USDT ${(cashUsdtWeight * 100).toInt()}%", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        uiState.positions.forEachIndexed { idx, item ->
                            val posWeight = (item.currentValueIdr / total).toFloat().coerceAtLeast(0.01f)
                            val colors = listOf(Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFF8B5CF6), Color(0xFFEC4899))
                            val color = colors[idx % colors.size]
                            Box(
                                modifier = Modifier
                                    .weight(posWeight)
                                    .fillMaxHeight()
                                    .background(color),
                                contentAlignment = Alignment.Center
                            ) {
                                if (posWeight > 0.12f) {
                                    Text(item.position.symbol.removeSuffix("IDR").removeSuffix("USDT"), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFF3B82F6), CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("IDR ${(cashIdrWeight * 100).toInt()}%", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFF0EA5E9), CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("USDT ${(cashUsdtWeight * 100).toInt()}%", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                        uiState.positions.take(3).forEachIndexed { idx, item ->
                            val colors = listOf(Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFF8B5CF6), Color(0xFFEC4899))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(colors[idx % colors.size], CircleShape))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(item.position.symbol.removeSuffix("IDR").removeSuffix("USDT"), color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Open Positions Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Posisi Terbuka (${uiState.positions.size})",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (uiState.positions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        border = BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.AccountBalanceWallet, contentDescription = "Empty", tint = Color(0xFF64748B), modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Belum ada posisi terbuka", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Buka tab Radar untuk melihat sinyal dan melakukan Paper Trade!", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(uiState.positions) { posItem ->
                    PositionRowItem(
                        item = posItem,
                        onCardClick = { selectedChartSymbol = posItem.position.symbol },
                        onSellClick = { viewModel.sellPosition(posItem) }
                    )
                }
            }

            // Trade History Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Riwayat Transaksi Paper Trade",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (uiState.recentOrders.isEmpty()) {
                item {
                    Text(
                        text = "Belum ada transaksi tercatat.",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            } else {
                items(uiState.recentOrders) { order ->
                    OrderHistoryRow(order = order)
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun PositionRowItem(
    item: PositionDisplayItem,
    onCardClick: () -> Unit,
    onSellClick: () -> Unit
) {
    val (baseSymbol, quoteSymbol) = CryptoUtils.splitSymbol(item.position.symbol)
    val pnlColor = if (item.isProfitable) SuccessGreen else ErrorRed

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onCardClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$baseSymbol/$quoteSymbol",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Qty: ${String.format(Locale.US, "%.6f", item.position.quantity)} • Entry: ${CryptoUtils.formatCryptoPrice(item.position.symbol, item.position.averageEntryPrice)}",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .background(pnlColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${if (item.isProfitable) "+" else ""}${String.format(Locale.US, "%.2f", item.pnlPercent)}%",
                            color = pnlColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = CryptoUtils.formatCryptoPrice(item.position.symbol, item.currentPrice),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Row: Jual Button + Chart view
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.clickable(onClick = onCardClick),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.ShowChart, contentDescription = "Chart", tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("TradingView Chart", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onSellClick,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Filled.Sell, contentDescription = "Sell", modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Jual Posisi", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun OrderHistoryRow(order: com.tokoreader.domain.model.LocalOrder) {
    val isBuy = order.side.equals("BUY", ignoreCase = true)
    val color = if (isBuy) SuccessGreen else ErrorRed
    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale("id", "ID")) }
    val formattedTime = remember(order.timestamp) { dateFormat.format(Date(order.timestamp)) }
    val isUsdtPair = order.symbol.endsWith("USDT", ignoreCase = true) || order.symbol.endsWith("USDC", ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(color.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isBuy) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                        contentDescription = order.side,
                        tint = color,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(order.side, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(order.symbol, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "Harga: ${CryptoUtils.formatCryptoPrice(order.symbol, order.price)}",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val formattedTotal = if (isUsdtPair) {
                    "$ ${String.format(Locale.US, "%,.2f", order.totalAmount)}"
                } else {
                    "Rp ${NumberFormat.getNumberInstance(Locale.US).format(order.totalAmount.toLong())}"
                }
                Text(
                    text = formattedTotal,
                    color = Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formattedTime,
                    color = Color(0xFF64748B),
                    fontSize = 10.5.sp
                )
            }
        }
    }
}
