package com.tokoreader.presentation.radar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tokoreader.TokoReaderApp
import com.tokoreader.domain.model.PriceTickDirection
import com.tokoreader.presentation.chart.FullscreenTradingViewDialog
import com.tokoreader.presentation.chart.ProperMiniCandleChart
import com.tokoreader.presentation.components.CryptoUtils
import com.tokoreader.presentation.components.LivePriceFlashText
import com.tokoreader.presentation.components.LiveTickFlashBadge
import com.tokoreader.presentation.radar.components.RadarPositionSummaryCard
import com.tokoreader.presentation.radar.components.RadarSwapDialog
import com.tokoreader.presentation.radar.components.RadarTradeControls
import com.tokoreader.presentation.radar.components.RealTradeConfirmDialog
import com.tokoreader.presentation.radar.components.SnapshotChip
import com.tokoreader.presentation.radar.components.StatBox
import com.tokoreader.ui.theme.ErrorRed
import com.tokoreader.ui.theme.SuccessGreen
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarTradeScreen(
    initialSymbol: String = "BTCIDR",
    symbol: String = initialSymbol,
    viewModel: RadarTradeViewModel = viewModel(factory = RadarTradeViewModel.Factory),
    onBackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val repository = remember(context) { (context.applicationContext as TokoReaderApp).container.marketDataRepository }
    var showFullscreenChart by remember { mutableStateOf(false) }
    var showSwapDialog by remember { mutableStateOf(false) }
    var swapDirectionToUsdt by remember { mutableStateOf(true) }
    var showRealBuyConfirmDialog by remember { mutableStateOf(false) }
    var showRealSellConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(symbol) {
        if (symbol.isNotEmpty()) {
            viewModel.initCoin(symbol)
        }
    }

    if (showFullscreenChart) {
        FullscreenTradingViewDialog(
            symbol = uiState.symbol,
            onDismissRequest = { showFullscreenChart = false }
        )
    }

    if (showSwapDialog) {
        RadarSwapDialog(
            usdtRate = uiState.usdtRate,
            paperBalanceIdr = uiState.paperBalanceIdr,
            paperBalanceUsdt = uiState.paperBalanceUsdt,
            initialDirectionToUsdt = swapDirectionToUsdt,
            onDismissRequest = { showSwapDialog = false },
            onConfirmSwap = { from, to, amt ->
                viewModel.swapCurrency(from, to, amt)
                showSwapDialog = false
            }
        )
    }

    val ticker = uiState.ticker
    val currentPrice = ticker?.price ?: 0.0
    val isPriceUp = (ticker?.priceChangePercent ?: 0.0) >= 0
    val isHolding = uiState.currentPosition != null
    val isUsdtPair = uiState.symbol.endsWith("USDT", ignoreCase = true) || uiState.symbol.endsWith("USDC", ignoreCase = true)
    val calculatedQty = if (currentPrice > 0.0) uiState.selectedNominal / currentPrice else 0.0

    // Confirmation dialog for Real Buy
    if (showRealBuyConfirmDialog) {
        RealTradeConfirmDialog(
            isBuy = true,
            symbol = uiState.symbol,
            currentPrice = currentPrice,
            nominal = uiState.selectedNominal,
            quantity = calculatedQty,
            isUsdtPair = isUsdtPair,
            onDismissRequest = { showRealBuyConfirmDialog = false },
            onConfirm = { viewModel.executeBuy() }
        )
    }

    // Confirmation dialog for Real Sell
    if (showRealSellConfirmDialog) {
        RealTradeConfirmDialog(
            isBuy = false,
            symbol = uiState.symbol,
            currentPrice = currentPrice,
            nominal = (uiState.currentPosition?.quantity ?: 0.0) * currentPrice,
            quantity = uiState.currentPosition?.quantity ?: 0.0,
            isUsdtPair = isUsdtPair,
            onDismissRequest = { showRealSellConfirmDialog = false },
            onConfirm = { viewModel.executeSell() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali ke Dashboard",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                title = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = uiState.symbol,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.5.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(SuccessGreen, CircleShape)
                            )
                        }
                        Text(
                            text = "${uiState.strategyMode} • TF ${uiState.activeTimeframe}",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    // Mode Switcher (Spot Real vs Paper Trade)
                    Row(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .background(Color(0xFF1E293B), RoundedCornerShape(18.dp))
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    if (uiState.isPaperMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { viewModel.setTradingMode(true) }
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Text(
                                "Paper",
                                color = if (uiState.isPaperMode) Color.White else Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(
                                    if (!uiState.isPaperMode) Color(0xFFF59E0B) else Color.Transparent,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { viewModel.setTradingMode(false) }
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Text(
                                "Real",
                                color = if (!uiState.isPaperMode) Color.Black else Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
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
            // Notification Message (Success/Failure feedback)
            uiState.orderMessage?.let { msg ->
                item {
                    val isSuccess = uiState.isOrderSuccess == true
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSuccess) SuccessGreen.copy(alpha = 0.2f) else ErrorRed.copy(alpha = 0.2f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSuccess) SuccessGreen else ErrorRed)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = msg,
                                color = if (isSuccess) SuccessGreen else ErrorRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.clearMessage() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Tutup", tint = Color.White)
                            }
                        }
                    }
                }
            }

            // Real Live Price Section with Tick Color Flash
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LivePriceFlashText(
                            price = currentPrice,
                            symbol = uiState.symbol,
                            tickDirection = ticker?.tickDirection ?: PriceTickDirection.NEUTRAL,
                            fontSize = 29.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        LiveTickFlashBadge(
                            tickDirection = ticker?.tickDirection ?: PriceTickDirection.NEUTRAL
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isPriceUp) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = "Trend",
                            tint = if (isPriceUp) SuccessGreen else ErrorRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${if (isPriceUp) "+" else ""}${String.format(Locale.US, "%.2f", ticker?.priceChangePercent ?: 0.0)}% (24j)",
                            color = if (isPriceUp) SuccessGreen else ErrorRed,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // High, Low, Vol Stats
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatBox("High 24j", CryptoUtils.formatCryptoPrice(uiState.symbol, uiState.highPrice24h))
                    StatBox("Low 24j", CryptoUtils.formatCryptoPrice(uiState.symbol, uiState.lowPrice24h))
                    StatBox("Vol Quote", CryptoUtils.formatCryptoVolume(uiState.symbol, ticker?.volume24h ?: 0.0))
                }
            }

            // Live Orderbook Pressure Bar
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    val bidPercent = (uiState.bidPressure * 100).toInt()
                    val askPercent = 100 - bidPercent
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tekanan Beli: $bidPercent%", color = SuccessGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Tekanan Jual: $askPercent%", color = ErrorRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { uiState.bidPressure },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = SuccessGreen,
                        trackColor = ErrorRed,
                    )
                }
            }

            // Chart Section: Proper Candlestick Mini Chart with interval selector & TradingView launcher
            item {
                ProperMiniCandleChart(
                    symbol = uiState.symbol,
                    marketDataRepository = repository,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onFullscreenClick = { showFullscreenChart = true }
                )
            }

            // Dynamic Action Section: RADAR BUY vs RADAR SELL
            item {
                Spacer(modifier = Modifier.height(16.dp))
                if (isHolding) {
                    RadarPositionSummaryCard(
                        position = uiState.currentPosition!!,
                        currentPrice = currentPrice,
                        symbol = uiState.symbol,
                        isPaperMode = uiState.isPaperMode,
                        isExecuting = uiState.isExecuting,
                        isUsdtPair = isUsdtPair,
                        onExecuteSell = {
                            if (uiState.isPaperMode) {
                                viewModel.executeSell()
                            } else {
                                showRealSellConfirmDialog = true
                            }
                        }
                    )
                } else {
                    RadarTradeControls(
                        uiState = uiState,
                        currentPrice = currentPrice,
                        isUsdtPair = isUsdtPair,
                        onSelectNominal = { viewModel.selectNominal(it) },
                        onOpenSwapDialog = {
                            swapDirectionToUsdt = !isUsdtPair || uiState.paperBalanceUsdt < 10.0
                            showSwapDialog = true
                        },
                        onExecuteBuy = {
                            if (uiState.isPaperMode) {
                                viewModel.executeBuy()
                            } else {
                                showRealBuyConfirmDialog = true
                            }
                        }
                    )
                }
            }

            // Real Technical Indicators Snapshot
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Text(
                        "INDIKATOR PASAR REALTIME",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SnapshotChip(
                            Icons.Filled.TrendingUp,
                            uiState.emaBias,
                            if (uiState.emaBias.contains("Bullish")) SuccessGreen else ErrorRed
                        )
                        SnapshotChip(
                            Icons.Filled.Speed,
                            "RSI ${String.format(Locale.US, "%.1f", uiState.rsiValue)}",
                            MaterialTheme.colorScheme.primary
                        )
                        SnapshotChip(
                            Icons.Filled.ShowChart,
                            "Volatilitas ${uiState.atrDescription}",
                            Color(0xFF38BDF8)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
