package com.tokoreader.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tokoreader.TokoReaderApp
import com.tokoreader.presentation.chart.FullscreenTradingViewDialog
import com.tokoreader.presentation.components.SnapshotBlock
import com.tokoreader.presentation.dashboard.components.AddCoinDialog
import com.tokoreader.presentation.dashboard.components.DashboardHeroCard
import com.tokoreader.presentation.dashboard.components.DashboardWatchlistFilterHeader
import com.tokoreader.presentation.dashboard.components.WatchlistItemCard
import com.tokoreader.ui.theme.ErrorRed
import com.tokoreader.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory),
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateToRadar: (String) -> Unit = onNavigateToDetail
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val repository = remember(context) { (context.applicationContext as TokoReaderApp).container.marketDataRepository }
    var selectedChartSymbol by remember { mutableStateOf<String?>(null) }
    var showAddCoinDialog by remember { mutableStateOf(false) }

    if (showAddCoinDialog) {
        AddCoinDialog(
            availableSymbols = uiState.availableTokocryptoSymbols,
            customSymbols = uiState.customSymbols,
            onAddCoin = { viewModel.addCustomCoin(it) },
            onRemoveCoin = { viewModel.removeCustomCoin(it) },
            onDismiss = { showAddCoinDialog = false }
        )
    }

    if (selectedChartSymbol != null) {
        FullscreenTradingViewDialog(
            symbol = selectedChartSymbol!!,
            onDismissRequest = { selectedChartSymbol = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "TokoCrypto Reader",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (uiState.isConnected) SuccessGreen else ErrorRed,
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (uiState.isConnected) "Live" else "Offline",
                            color = if (uiState.isConnected) SuccessGreen else ErrorRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        IconButton(onClick = { /* Notification click */ }) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "Alerts",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { /* Trigger AI Analysis */ },
                icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = "AI", modifier = Modifier.size(18.dp)) },
                text = { Text("Analisa AI", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Connection Status Bar
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Wifi,
                        contentDescription = "WS",
                        tint = if (uiState.isConnected) SuccessGreen else ErrorRed,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tokocrypto / Binance API",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isConnected) "Connected" else "Disconnected",
                        color = if (uiState.isConnected) SuccessGreen else ErrorRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (uiState.error != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .background(ErrorRed.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(1.dp, ErrorRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(14.dp)
                    ) {
                        Text("ERROR: ${uiState.error}", color = ErrorRed, fontSize = 12.sp)
                    }
                }
            }

            // Hero Card (Dynamic BTC or top coin)
            uiState.heroTicker?.let { hero ->
                item {
                    DashboardHeroCard(
                        hero = hero,
                        marketDataRepository = repository,
                        onClick = { onNavigateToDetail(hero.symbol) }
                    )
                }
            }

            // Watchlist Header + Filter/Sort Dropdown
            item {
                DashboardWatchlistFilterHeader(
                    selectedQuote = uiState.selectedQuote,
                    selectedSort = uiState.selectedSort,
                    onQuoteSelected = { viewModel.setQuoteFilter(it) },
                    onSortSelected = { viewModel.setSortOption(it) },
                    onOpenAddCoinDialog = { showAddCoinDialog = true }
                )
            }

            // Watchlist Items
            if (uiState.isLoading && uiState.watchList.isEmpty()) {
                item {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                            .wrapContentWidth(Alignment.CenterHorizontally)
                    )
                }
            } else if (uiState.watchList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tidak ada pair untuk kategori ini",
                            color = Color(0xFF64748B),
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(uiState.watchList.size) { index ->
                    val ticker = uiState.watchList[index]
                    WatchlistItemCard(
                        ticker = ticker,
                        onClick = { onNavigateToDetail(ticker.symbol) }
                    )
                }
            }

            // Market Snapshot Global
            item {
                Text(
                    text = "Market Snapshot",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SnapshotBlock(Modifier.weight(1f), Icons.Filled.Search, "Scanned", "${uiState.totalPairsScanned}", MaterialTheme.colorScheme.primary)
                    SnapshotBlock(Modifier.weight(1f), Icons.AutoMirrored.Filled.TrendingUp, "Bullish", "${uiState.bullishCount}", SuccessGreen)
                    SnapshotBlock(Modifier.weight(1f), Icons.AutoMirrored.Filled.TrendingDown, "Bearish", "${uiState.bearishCount}", ErrorRed)
                    SnapshotBlock(Modifier.weight(1f), Icons.Filled.FlashOn, "Regime", uiState.marketRegime, MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
