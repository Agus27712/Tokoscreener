package com.tokoreader.presentation.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tokoreader.TokoReaderApp
import com.tokoreader.domain.model.LocalOrder
import com.tokoreader.domain.model.Position
import com.tokoreader.domain.model.Ticker
import com.tokoreader.domain.repository.MarketDataRepository
import com.tokoreader.domain.repository.PaperTradeRepository
import com.tokoreader.domain.repository.PositionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale

data class PositionDisplayItem(
    val position: Position,
    val quoteAsset: String, // "IDR" or "USDT"
    val currentPrice: Double,
    val currentValueQuote: Double,
    val currentValueIdr: Double,
    val pnlPercent: Double,
    val pnlAmountQuote: Double,
    val pnlAmountIdr: Double,
    val netPnlPercent: Double = 0.0,
    val netPnlAmountQuote: Double = 0.0,
    val netPnlAmountIdr: Double = 0.0,
    val totalDeductionQuote: Double = 0.0,
    val isProfitable: Boolean
)

data class PortfolioUiState(
    val isPaperMode: Boolean = true,
    val cashBalanceIdr: Double = 100_000_000.0,
    val cashBalanceUsdt: Double = 5_000.0,
    val usdtRate: Double = 16_200.0,
    val positions: List<PositionDisplayItem> = emptyList(),
    val totalCoinsValueIdr: Double = 0.0,
    val totalPortfolioValueIdr: Double = 100_000_000.0,
    val totalPnlIdr: Double = 0.0,
    val totalPnlPercent: Double = 0.0,
    val recentOrders: List<LocalOrder> = emptyList(),
    val isExecuting: Boolean = false,
    val notificationMessage: String? = null
)

class PortfolioViewModel(
    private val paperTradeRepository: PaperTradeRepository,
    private val positionRepository: PositionRepository,
    private val marketDataRepository: MarketDataRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PortfolioUiState())
    val uiState: StateFlow<PortfolioUiState> = _uiState.asStateFlow()

    private val tickersMap = MutableStateFlow<Map<String, Ticker>>(emptyMap())

    init {
        loadData()
    }

    private fun loadData() {
        // 1. Fetch all tickers for live price calculation and USDT rate
        viewModelScope.launch {
            try {
                val tickers = marketDataRepository.getAllTickers()
                tickersMap.value = tickers.associateBy { it.symbol }
                val usdtTicker = tickers.find { it.symbol == "USDTIDR" }
                if (usdtTicker != null && usdtTicker.price > 0.0) {
                    _uiState.update { it.copy(usdtRate = usdtTicker.price) }
                }
            } catch (_: Exception) {}
        }

        // 2. Observe Paper Balances (IDR & USDT)
        viewModelScope.launch {
            paperTradeRepository.observePaperBalances().collect { balances ->
                _uiState.update { 
                    it.copy(
                        cashBalanceIdr = balances.balanceIdr,
                        cashBalanceUsdt = balances.balanceUsdt
                    ) 
                }
                recalculateTotals()
            }
        }

        // 3. Observe Positions
        viewModelScope.launch {
            combine(
                positionRepository.observeAllPositions(),
                tickersMap
            ) { positions, map ->
                val rate = _uiState.value.usdtRate.coerceAtLeast(1000.0)
                positions.map { pos ->
                    val isUsdt = pos.symbol.endsWith("USDT") || pos.symbol.endsWith("USDC") || pos.symbol.endsWith("BUSD")
                    val quote = if (isUsdt) "USDT" else "IDR"
                    val ticker = map[pos.symbol]
                    val curPrice = ticker?.price ?: pos.averageEntryPrice
                    val curValQuote = pos.quantity * curPrice
                    val curValIdr = if (isUsdt) curValQuote * rate else curValQuote

                    val entryCostQuote = pos.averageEntryPrice * pos.quantity
                    val pnlPct = if (pos.averageEntryPrice > 0) ((curPrice - pos.averageEntryPrice) / pos.averageEntryPrice) * 100 else 0.0
                    val pnlAmtQuote = (curPrice - pos.averageEntryPrice) * pos.quantity
                    val pnlAmtIdr = if (isUsdt) pnlAmtQuote * rate else pnlAmtQuote

                    // Riset Potongan Tokocrypto: Fee Beli 0.10%, Fee Jual 0.10%, PPh 22 0.10%, PPN PMK68 0.11%
                    val buyFee = entryCostQuote * 0.0010
                    val sellFee = curValQuote * 0.0010
                    val pphTax = curValQuote * 0.0010
                    val ppnTax = curValQuote * 0.0011
                    val totalDeductionQuote = buyFee + sellFee + pphTax + ppnTax
                    val totalDeductionIdr = if (isUsdt) totalDeductionQuote * rate else totalDeductionQuote

                    val netProfitAmtQuote = (curValQuote - entryCostQuote) - totalDeductionQuote
                    val netProfitAmtIdr = if (isUsdt) netProfitAmtQuote * rate else netProfitAmtQuote
                    val netProfitPct = if (entryCostQuote > 0) (netProfitAmtQuote / entryCostQuote) * 100 else 0.0

                    PositionDisplayItem(
                        position = pos,
                        quoteAsset = quote,
                        currentPrice = curPrice,
                        currentValueQuote = curValQuote,
                        currentValueIdr = curValIdr,
                        pnlPercent = pnlPct,
                        pnlAmountQuote = pnlAmtQuote,
                        pnlAmountIdr = pnlAmtIdr,
                        netPnlPercent = netProfitPct,
                        netPnlAmountQuote = netProfitAmtQuote,
                        netPnlAmountIdr = netProfitAmtIdr,
                        totalDeductionQuote = totalDeductionQuote,
                        isProfitable = netProfitPct >= 0
                    )
                }
            }.collect { items ->
                _uiState.update { it.copy(positions = items) }
                recalculateTotals()
            }
        }

        // 4. Observe Orders
        viewModelScope.launch {
            paperTradeRepository.observePaperOrders().collect { orders ->
                _uiState.update { it.copy(recentOrders = orders) }
            }
        }
    }

    private fun recalculateTotals() {
        val state = _uiState.value
        val rate = state.usdtRate.coerceAtLeast(1000.0)
        val cashUsdtInIdr = state.cashBalanceUsdt * rate
        val totalCoinsIdr = state.positions.sumOf { it.currentValueIdr }
        val totalPortfolioIdr = state.cashBalanceIdr + cashUsdtInIdr + totalCoinsIdr
        val totalPnlIdr = state.positions.sumOf { it.pnlAmountIdr }
        val initialInvested = totalPortfolioIdr - totalPnlIdr
        val totalPnlPct = if (initialInvested > 0) (totalPnlIdr / initialInvested) * 100 else 0.0

        _uiState.update {
            it.copy(
                totalCoinsValueIdr = totalCoinsIdr,
                totalPortfolioValueIdr = totalPortfolioIdr,
                totalPnlIdr = totalPnlIdr,
                totalPnlPercent = totalPnlPct
            )
        }
    }

    fun swapCurrency(fromAsset: String, toAsset: String, amount: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExecuting = true, notificationMessage = null) }
            val rate = _uiState.value.usdtRate.coerceAtLeast(1000.0)
            val res = paperTradeRepository.swapPaperCurrency(fromAsset, toAsset, amount, rate)
            if (res.isSuccess) {
                _uiState.update {
                    it.copy(
                        isExecuting = false,
                        notificationMessage = "Konversi $amount $fromAsset ke $toAsset berhasil!"
                    )
                }
                recalculateTotals()
            } else {
                _uiState.update {
                    it.copy(
                        isExecuting = false,
                        notificationMessage = "Gagal konversi: ${res.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }

    fun sellPosition(item: PositionDisplayItem) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExecuting = true) }
            val res = paperTradeRepository.executeSell(
                symbol = item.position.symbol,
                quantity = item.position.quantity,
                price = item.currentPrice
            )
            if (res.isSuccess) {
                _uiState.update {
                    it.copy(
                        isExecuting = false,
                        notificationMessage = "Berhasil menjual semua ${item.position.symbol} di harga pasar!"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isExecuting = false,
                        notificationMessage = "Gagal menjual: ${res.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }

    fun resetPaperAccount() {
        viewModelScope.launch {
            paperTradeRepository.resetPaperAccount()
            _uiState.update { it.copy(notificationMessage = "Akun Paper Trade di-reset ke Rp 100.000.000 dan $5.000 USDT!") }
        }
    }

    fun clearNotification() {
        _uiState.update { it.copy(notificationMessage = null) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TokoReaderApp)
                PortfolioViewModel(
                    paperTradeRepository = app.container.paperTradeRepository,
                    positionRepository = app.container.positionRepository,
                    marketDataRepository = app.container.marketDataRepository
                )
            }
        }
    }
}

