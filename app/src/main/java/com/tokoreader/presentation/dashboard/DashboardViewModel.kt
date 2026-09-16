package com.tokoreader.presentation.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tokoreader.TokoReaderApp
import com.tokoreader.domain.model.Ticker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class QuoteFilter(val label: String, val shortCode: String) {
    ALL("Semua", "ALL"),
    IDR("IDR (Rupiah)", "IDR"),
    USDT("USDT (Tether)", "USDT"),
    BTC("BTC (Bitcoin)", "BTC")
}

enum class SortOption(val label: String) {
    DEFAULT("Populer"),
    VOLUME("Vol 24j Terbesar"),
    GAINERS("Top Gainers (%)"),
    LOSERS("Top Losers (%)"),
    PRICE_DESC("Harga Tertinggi"),
    PRICE_ASC("Harga Terendah")
}

/**
 * UI State for the Dashboard Screen.
 */
data class DashboardUiState(
    val isLoading: Boolean = true,
    val heroTicker: Ticker? = null,
    val rawTickers: List<Ticker> = emptyList(),
    val watchList: List<Ticker> = emptyList(),
    val selectedQuote: QuoteFilter = QuoteFilter.ALL,
    val selectedSort: SortOption = SortOption.DEFAULT,
    val totalPairsScanned: Int = 0,
    val bullishCount: Int = 0,
    val bearishCount: Int = 0,
    val marketRegime: String = "Neutral",
    val isConnected: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel responsible for fetching and managing data for the DashboardScreen.
 * Keeps business logic modular and separate from UI components.
 */
class DashboardViewModel(
    private val marketDataRepository: com.tokoreader.domain.repository.MarketDataRepository
) : ViewModel() {

    private val TAG = "DashboardViewModel"

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "ViewModel initialized, starting data fetch")
        fetchDashboardData()
        startHeroTickerObservation()
    }

    /**
     * Fetches all market tickers from Tokocrypto/Binance API and updates UI state.
     */
    fun fetchDashboardData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                Log.d(TAG, "Fetching all market tickers from repository")
                val allTickers = marketDataRepository.getAllTickers()
                
                // Fallback if empty
                val validTickers = if (allTickers.isNotEmpty()) {
                    allTickers
                } else {
                    marketDataRepository.getAllIdrTickers()
                }

                // Pick BTC/IDR or BTC/USDT as Hero
                val hero = _uiState.value.heroTicker 
                    ?: validTickers.firstOrNull { it.symbol == "BTCIDR" }
                    ?: validTickers.firstOrNull { it.symbol == "BTCUSDT" }
                    ?: validTickers.firstOrNull { it.symbol.startsWith("BTC") && !it.symbol.endsWith("BIDR") }
                    ?: validTickers.firstOrNull()

                // Calculate Snapshot Metrics
                val totalPairs = validTickers.size
                val bullish = validTickers.count { it.priceChangePercent > 0 }
                val bearish = validTickers.count { it.priceChangePercent < 0 }
                val regime = when {
                    bullish > bearish * 1.5 -> "Bullish"
                    bearish > bullish * 1.5 -> "Bearish"
                    else -> "Mixed"
                }

                val currentQuote = _uiState.value.selectedQuote
                val currentSort = _uiState.value.selectedSort
                val filteredWatchList = computeWatchList(validTickers, hero?.symbol, currentQuote, currentSort)

                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        rawTickers = validTickers,
                        watchList = filteredWatchList,
                        heroTicker = hero,
                        totalPairsScanned = totalPairs,
                        bullishCount = bullish,
                        bearishCount = bearish,
                        marketRegime = regime,
                        isConnected = true
                    )
                }
                Log.d(TAG, "Successfully updated dashboard with ${validTickers.size} pairs")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch dashboard data", e)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message,
                        isConnected = false
                    )
                }
            }
        }
    }

    fun setQuoteFilter(quote: QuoteFilter) {
        val heroSym = _uiState.value.heroTicker?.symbol
        val raw = _uiState.value.rawTickers
        val sort = _uiState.value.selectedSort
        val newWatchlist = computeWatchList(raw, heroSym, quote, sort)
        _uiState.update {
            it.copy(
                selectedQuote = quote,
                watchList = newWatchlist
            )
        }
    }

    fun setSortOption(sort: SortOption) {
        val heroSym = _uiState.value.heroTicker?.symbol
        val raw = _uiState.value.rawTickers
        val quote = _uiState.value.selectedQuote
        val newWatchlist = computeWatchList(raw, heroSym, quote, sort)
        _uiState.update {
            it.copy(
                selectedSort = sort,
                watchList = newWatchlist
            )
        }
    }

    private fun computeWatchList(
        rawList: List<Ticker>,
        heroSymbol: String?,
        quote: QuoteFilter,
        sort: SortOption
    ): List<Ticker> {
        val filtered = rawList.filter { ticker ->
            val sym = ticker.symbol
            if (sym == heroSymbol && quote == QuoteFilter.ALL) return@filter false
            if (sym.endsWith("BIDR")) return@filter false

            when (quote) {
                QuoteFilter.ALL -> true
                QuoteFilter.IDR -> sym.endsWith("IDR")
                QuoteFilter.USDT -> sym.endsWith("USDT")
                QuoteFilter.BTC -> sym.endsWith("BTC")
            }
        }

        val preferredSymbols = listOf(
            "ETHIDR", "SOLIDR", "XRPIDR", "DOGEIDR", "BNBIDR", "ADAIDR", "TKOIDR",
            "ETHUSDT", "SOLUSDT", "XRPUSDT", "DOGEUSDT", "BNBUSDT", "ADAUSDT", "PEPEUSDT", "SHIBUSDT"
        )

        return when (sort) {
            SortOption.DEFAULT -> {
                filtered.sortedWith(
                    compareByDescending<Ticker> { preferredSymbols.contains(it.symbol) }
                        .thenBy { 
                            val idx = preferredSymbols.indexOf(it.symbol)
                            if (idx >= 0) idx else Int.MAX_VALUE 
                        }
                        .thenByDescending { it.volume24h }
                )
            }
            SortOption.VOLUME -> filtered.sortedByDescending { it.volume24h }
            SortOption.GAINERS -> filtered.sortedByDescending { it.priceChangePercent }
            SortOption.LOSERS -> filtered.sortedBy { it.priceChangePercent }
            SortOption.PRICE_DESC -> filtered.sortedByDescending { it.price }
            SortOption.PRICE_ASC -> filtered.sortedBy { it.price }
        }
    }

    /**
     * Simulates observing a WebSocket or polling for the main hero ticker (BTCIDR) updates.
     */
    private fun startHeroTickerObservation() {
        viewModelScope.launch {
            Log.d(TAG, "Starting continuous observation for hero ticker")
            marketDataRepository.observeTicker("BTCIDR").collect { updatedTicker ->
                _uiState.update { state ->
                    val newWatchList = state.watchList.map { 
                        if (it.symbol == updatedTicker.symbol) updatedTicker else it 
                    }
                    val newRaw = state.rawTickers.map {
                        if (it.symbol == updatedTicker.symbol) updatedTicker else it
                    }
                    state.copy(
                        heroTicker = if (state.heroTicker?.symbol == updatedTicker.symbol || state.heroTicker == null) updatedTicker else state.heroTicker,
                        watchList = newWatchList,
                        rawTickers = newRaw,
                        isConnected = true
                    )
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TokoReaderApp)
                val repository = application.container.marketDataRepository
                DashboardViewModel(marketDataRepository = repository)
            }
        }
    }
}
