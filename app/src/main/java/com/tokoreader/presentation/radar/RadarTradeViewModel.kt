package com.tokoreader.presentation.radar

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tokoreader.TokoReaderApp
import com.tokoreader.domain.indicator.AtrCalculator
import com.tokoreader.domain.indicator.EmaCalculator
import com.tokoreader.domain.indicator.MacdCalculator
import com.tokoreader.domain.indicator.RsiCalculator
import com.tokoreader.domain.model.*
import com.tokoreader.domain.repository.MarketDataRepository
import com.tokoreader.domain.repository.PaperTradeRepository
import com.tokoreader.domain.repository.PositionRepository
import com.tokoreader.domain.repository.SettingsRepository
import com.tokoreader.domain.repository.TradeRepository
import com.tokoreader.domain.usecase.PlaceOrderUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.Response
import org.json.JSONObject

data class RadarTradeUiState(
    val symbol: String = "BTCIDR",
    val isPaperMode: Boolean = true,
    val strategyMode: String = "Scalping", // Diambil dari SettingsRepository: Scalping, Intraday, Swing
    val activeTimeframe: String = "1m",
    val ticker: Ticker? = null,
    val highPrice24h: Double = 0.0,
    val lowPrice24h: Double = 0.0,
    val bidPressure: Float = 0.5f,
    val askPressure: Float = 0.5f,
    val rsiValue: Double = 50.0,
    val emaBias: String = "Neutral",
    val atrDescription: String = "Normal",
    val currentPosition: Position? = null,
    val paperBalanceIdr: Double = 100_000_000.0,
    val paperBalanceUsdt: Double = 5_000.0,
    val quoteCurrency: String = "IDR",
    val selectedNominal: Double = 1_000_000.0,
    val usdtRate: Double = 16_200.0,
    val orderConfidence: Int = 78,
    val isExecuting: Boolean = false,
    val orderMessage: String? = null,
    val isOrderSuccess: Boolean? = null,
    // Sequential Pipeline Evaluation States
    val isEvaluating: Boolean = false,
    val step1Pass: Boolean = false,
    val step1Detail: String = "Menilai Bias...",
    val step2Pass: Boolean = false,
    val step2Detail: String = "Terkunci",
    val step3Pass: Boolean = false,
    val step3Detail: String = "Terkunci",
    val step4Pass: Boolean = false,
    val step4Detail: String = "Terkunci",
    // Progress antar jeda stepper (0.0f - 1.0f)
    val progressStep1To2: Float = 0f,
    val progressStep2To3: Float = 0f,
    val progressStep3To4: Float = 0f
)

class RadarTradeViewModel(
    private val marketDataRepository: MarketDataRepository,
    private val positionRepository: PositionRepository,
    private val paperTradeRepository: PaperTradeRepository,
    private val tradeRepository: TradeRepository,
    private val placeOrderUseCase: PlaceOrderUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val TAG = "RadarTradeViewModel"
    private val _uiState = MutableStateFlow(RadarTradeUiState())
    val uiState: StateFlow<RadarTradeUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null
    private var depthJob: Job? = null
    private var candlesJob: Job? = null
    private var positionJob: Job? = null
    private var settingsModeJob: Job? = null
    private var userDataStreamWebSocket: WebSocket? = null
    private var userDataStreamJob: Job? = null
    private var balanceObservationJob: Job? = null

    init {
        observeSettingsMode()
        initCoin("BTCIDR")
    }

    private fun observeSettingsMode() {
        settingsModeJob?.cancel()
        settingsModeJob = viewModelScope.launch {
            launch {
                settingsRepository.getTradingMode().collect { mode ->
                    val tf = when (mode) {
                        "Scalping" -> "1m"
                        "Intraday" -> "15m"
                        "Swing" -> "1d"
                        else -> "1m"
                    }
                    _uiState.update { it.copy(strategyMode = mode, activeTimeframe = tf) }
                    // Re-start candles observation on new timeframe for current symbol
                    startCandlesObservation(_uiState.value.symbol, tf)
                }
            }
            launch {
                settingsRepository.getRealBuyMode().collect { isReal ->
                    _uiState.update { it.copy(isPaperMode = !isReal) }
                    if (isReal) {
                        balanceObservationJob?.cancel()
                        fetchRealBalances()
                        startUserDataStream()
                    } else {
                        userDataStreamWebSocket?.close(1000, "Switch to Paper")
                        userDataStreamJob?.cancel()
                        observePaperBalance()
                    }
                }
            }
        }
    }

    fun setTradingMode(isPaper: Boolean) {
        _uiState.update { it.copy(isPaperMode = isPaper) }
    }

    fun selectNominal(nominal: Double) {
        _uiState.update { it.copy(selectedNominal = nominal) }
    }

    fun initCoin(symbol: String) {
        val targetSymbol = if (symbol.endsWith("IDR") || symbol.endsWith("USDT")) symbol else "${symbol}IDR"
        val isUsdt = targetSymbol.endsWith("USDT") || targetSymbol.endsWith("USDC") || targetSymbol.endsWith("BUSD")
        val quote = if (isUsdt) "USDT" else "IDR"
        val defaultNominal = if (isUsdt) 100.0 else 1_000_000.0

        _uiState.update { 
            it.copy(
                symbol = targetSymbol,
                quoteCurrency = quote,
                selectedNominal = defaultNominal
            ) 
        }

        tickerJob?.cancel()
        depthJob?.cancel()
        positionJob?.cancel()

        // Fetch live USDT/IDR rate
        viewModelScope.launch {
            try {
                val usdtTicker = marketDataRepository.getAllIdrTickers().find { it.symbol == "USDTIDR" }
                if (usdtTicker != null && usdtTicker.price > 0.0) {
                    _uiState.update { it.copy(usdtRate = usdtTicker.price) }
                }
            } catch (_: Exception) {}
        }

        // 1. Ticker Stream WebSocket
        tickerJob = viewModelScope.launch {
            marketDataRepository.observeTicker(targetSymbol).collect { tick ->
                _uiState.update { current ->
                    val high = if (current.highPrice24h == 0.0) tick.price * 1.02 else current.highPrice24h.coerceAtLeast(tick.price)
                    val low = if (current.lowPrice24h == 0.0) tick.price * 0.98 else current.lowPrice24h.coerceAtMost(tick.price)
                    current.copy(
                        ticker = tick,
                        highPrice24h = high,
                        lowPrice24h = low
                    )
                }
                evaluatePipeline()
            }
        }

        // 2. Depth / Orderbook Stream WebSocket
        depthJob = viewModelScope.launch {
            marketDataRepository.observeOrderBook(targetSymbol).collect { snapshot ->
                val sumBid = snapshot.bids.sumOf { it.second * it.first }
                val sumAsk = snapshot.asks.sumOf { it.second * it.first }
                val total = sumBid + sumAsk
                if (total > 0.0) {
                    val bidRatio = (sumBid / total).toFloat().coerceIn(0.1f, 0.9f)
                    val askRatio = 1f - bidRatio
                    _uiState.update { it.copy(bidPressure = bidRatio, askPressure = askRatio) }
                    evaluatePipeline()
                }
            }
        }

        // 3. Position observation
        positionJob = viewModelScope.launch {
            positionRepository.observePosition(targetSymbol).collect { pos ->
                _uiState.update { it.copy(currentPosition = pos) }
            }
        }

        // 4. Start Candles Observation according to current strategy mode
        startCandlesObservation(targetSymbol, _uiState.value.activeTimeframe)
    }

    private fun startCandlesObservation(symbol: String, interval: String) {
        candlesJob?.cancel()
        candlesJob = viewModelScope.launch {
            _uiState.update { it.copy(isEvaluating = true) }
            marketDataRepository.observeClosedCandles(symbol, interval).collect { klines ->
                if (klines.size >= 10) {
                    val rsiList = RsiCalculator.calculate(klines, if (interval == "1m") 9 else 14)
                    val rsi = rsiList.lastOrNull { !it.isNaN() } ?: 50.0

                    val ema7List = EmaCalculator.calculate(klines, 7)
                    val ema7 = ema7List.lastOrNull { !it.isNaN() } ?: 0.0

                    val ema25List = EmaCalculator.calculate(klines, 25)
                    val ema25 = ema25List.lastOrNull { !it.isNaN() } ?: 0.0

                    val atrList = AtrCalculator.calculate(klines, 14)
                    val atr = atrList.lastOrNull { !it.isNaN() } ?: 0.0

                    val lastClose = klines.lastOrNull()?.close ?: 1.0
                    val bias = if (ema7 > ema25) "Bullish Trend" else "Bearish Trend"
                    val atrDesc = if (atr > 0.02 * lastClose) "Tinggi" else "Normal"

                    val confidence = when {
                        rsi in 40.0..60.0 && ema7 > ema25 -> 88
                        ema7 > ema25 -> 80
                        rsi < 35.0 -> 82 // Oversold bounce
                        else -> 65
                    }

                    _uiState.update {
                        it.copy(
                            rsiValue = rsi,
                            emaBias = bias,
                            atrDescription = atrDesc,
                            orderConfidence = confidence,
                            isEvaluating = false
                        )
                    }
                    evaluatePipeline()
                }
            }
        }
    }

    /**
     * Sequential Stepper Evaluator:
     * Step 1: Bias Trend (Bullish)
     * Step 2: Setup (RSI/ATR sesuai mode) -> HANYA JIKA Step 1 lolos
     * Step 3: Trigger (Orderbook Buyer > 50%) -> HANYA JIKA Step 1 & 2 lolos
     * Step 4: Entry Siap -> HANYA JIKA Step 1, 2, dan 3 lolos semua!
     */
    private fun evaluatePipeline() {
        val state = _uiState.value
        val mode = state.strategyMode

        // Step 1: Bias
        val isBiasPass = state.emaBias.contains("Bullish", ignoreCase = true)
        val step1Detail = if (isBiasPass) "Bullish (${state.activeTimeframe})" else "Bearish (Wait)"

        if (!isBiasPass) {
            _uiState.update {
                it.copy(
                    step1Pass = false,
                    step1Detail = step1Detail,
                    progressStep1To2 = 0f,
                    step2Pass = false,
                    step2Detail = "Menunggu Step 1",
                    progressStep2To3 = 0f,
                    step3Pass = false,
                    step3Detail = "Menunggu Step 2",
                    progressStep3To4 = 0f,
                    step4Pass = false,
                    step4Detail = "Terkunci"
                )
            }
            return
        }

        // Step 1 Pass -> Evaluasi Step 2 (Setup)
        val rsi = state.rsiValue
        val isSetupPass = when (mode) {
            "Scalping" -> rsi in 38.0..68.0
            "Intraday" -> rsi in 35.0..65.0
            "Swing" -> rsi in 32.0..60.0
            else -> rsi in 35.0..65.0
        }
        val step2Detail = if (isSetupPass) "RSI ${rsi.toInt()} OK" else "RSI ${rsi.toInt()} Diluar Setup"

        if (!isSetupPass) {
            _uiState.update {
                it.copy(
                    step1Pass = true,
                    step1Detail = step1Detail,
                    progressStep1To2 = 1f,
                    step2Pass = false,
                    step2Detail = step2Detail,
                    progressStep2To3 = 0f,
                    step3Pass = false,
                    step3Detail = "Menunggu Step 2",
                    progressStep3To4 = 0f,
                    step4Pass = false,
                    step4Detail = "Terkunci"
                )
            }
            return
        }

        // Step 1 & Step 2 Pass -> Evaluasi Step 3 (Trigger Orderbook)
        val bidRatio = state.bidPressure
        val isTriggerPass = bidRatio >= 0.50f
        val step3Detail = if (isTriggerPass) "Bid ${(bidRatio * 100).toInt()}% Dominan" else "Bid ${(bidRatio * 100).toInt()}% Lemah"
        val progress3To4 = if (isTriggerPass) 1f else (bidRatio / 0.50f).coerceIn(0f, 0.95f)

        if (!isTriggerPass) {
            _uiState.update {
                it.copy(
                    step1Pass = true,
                    step1Detail = step1Detail,
                    progressStep1To2 = 1f,
                    step2Pass = true,
                    step2Detail = step2Detail,
                    progressStep2To3 = 1f,
                    step3Pass = false,
                    step3Detail = step3Detail,
                    progressStep3To4 = progress3To4,
                    step4Pass = false,
                    step4Detail = "Antri Beli"
                )
            }
            return
        }

        // Step 1, 2, dan 3 SEMUA LOLOS -> Step 4 Entry Siap
        _uiState.update {
            it.copy(
                step1Pass = true,
                step1Detail = step1Detail,
                progressStep1To2 = 1f,
                step2Pass = true,
                step2Detail = step2Detail,
                progressStep2To3 = 1f,
                step3Pass = true,
                step3Detail = step3Detail,
                progressStep3To4 = 1f,
                step4Pass = true,
                step4Detail = "SIAP ENTRY"
            )
        }
    }

    private fun observePaperBalance() {
        balanceObservationJob?.cancel()
        balanceObservationJob = viewModelScope.launch {
            paperTradeRepository.observePaperBalances().collect { balances ->
                _uiState.update { 
                    it.copy(
                        paperBalanceIdr = balances.balanceIdr,
                        paperBalanceUsdt = balances.balanceUsdt
                    ) 
                }
            }
        }
    }

    private fun fetchRealBalances() {
        val state = _uiState.value
        if (state.isPaperMode) return
        viewModelScope.launch {
            try {
                val creds = settingsRepository.getApiCredentials().first()
                if (creds.apiKey.isNotBlank() && creds.secret.isNotBlank()) {
                    val response = TokoReaderApp.instance.container.tradeApi.getAccountInfo()
                    if (response.code == 0) {
                        val dataMap = response.data as? Map<*, *>
                        val balancesList = dataMap?.get("balances") as? List<*>
                        var realIdr = 0.0
                        var realUsdt = 0.0
                        balancesList?.forEach { item ->
                            val balanceMap = item as? Map<*, *>
                            val asset = balanceMap?.get("asset")?.toString() ?: ""
                            val free = balanceMap?.get("free")?.toString()?.toDoubleOrNull() ?: 0.0
                            if (asset == "BIDR" || asset == "IDR") {
                                realIdr = free
                            } else if (asset == "USDT") {
                                realUsdt = free
                            }
                        }
                        _uiState.update { 
                            it.copy(
                                paperBalanceIdr = realIdr,
                                paperBalanceUsdt = realUsdt
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gagal memuat saldo riil: ${e.message}")
            }
        }
    }

    private fun startUserDataStream() {
        userDataStreamJob?.cancel()
        userDataStreamWebSocket?.close(1000, "Switch mode")

        val state = _uiState.value
        if (state.isPaperMode) return

        userDataStreamJob = viewModelScope.launch {
            try {
                val creds = settingsRepository.getApiCredentials().first()
                if (creds.apiKey.isBlank() || creds.secret.isBlank()) return@launch

                val response = TokoReaderApp.instance.container.tradeApi.createListenKey()
                if (response.code == 0) {
                    val listenKey = response.data
                    val symbolType = settingsRepository.getCurrentSymbolType()
                    val wsUrl = if (symbolType == 1) {
                        "wss://stream-cloud.tokocrypto.site/stream?streams=$listenKey"
                    } else {
                        "wss://stream-toko.2meta.app?streams=$listenKey"
                    }

                    val request = Request.Builder().url(wsUrl).build()
                    val okHttpClient = OkHttpClient.Builder().build()
                    userDataStreamWebSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                        override fun onOpen(webSocket: WebSocket, response: Response) {
                            Log.d(TAG, "User Data Stream WebSocket Connected")
                        }

                        override fun onMessage(webSocket: WebSocket, text: String) {
                            try {
                                val json = JSONObject(text)
                                val eventType = json.optString("e")
                                if (eventType == "outboundAccountPosition") {
                                    val balancesArray = json.optJSONArray("B")
                                    if (balancesArray != null) {
                                        var realIdr = _uiState.value.paperBalanceIdr
                                        var realUsdt = _uiState.value.paperBalanceUsdt
                                        for (i in 0 until balancesArray.length()) {
                                            val b = balancesArray.getJSONObject(i)
                                            val asset = b.optString("a")
                                            val free = b.optString("f").toDoubleOrNull() ?: 0.0
                                            if (asset == "BIDR" || asset == "IDR") {
                                                realIdr = free
                                            } else if (asset == "USDT") {
                                                realUsdt = free
                                            }
                                        }
                                        _uiState.update {
                                            it.copy(
                                                paperBalanceIdr = realIdr,
                                                paperBalanceUsdt = realUsdt
                                            )
                                        }
                                    }
                                } else if (eventType == "executionReport") {
                                    val symbol = json.optString("s")
                                    val side = json.optString("S")
                                    val orderStatus = json.optString("X")
                                    val qty = json.optString("q").toDoubleOrNull() ?: 0.0
                                    val price = json.optString("p").toDoubleOrNull() ?: 0.0

                                    if (orderStatus == "FILLED") {
                                        viewModelScope.launch {
                                            if (side == "BUY") {
                                                positionRepository.savePosition(
                                                    Position(symbol = symbol, quantity = qty, averageEntryPrice = price)
                                                )
                                            } else {
                                                positionRepository.removePosition(symbol)
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Error parsing User Data Stream message: ${e.message}")
                            }
                        }

                        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                            Log.w(TAG, "User Data Stream WS error: ${t.message}")
                            viewModelScope.launch {
                                delay(5000)
                                if (!_uiState.value.isPaperMode) {
                                    startUserDataStream()
                                }
                            }
                        }
                    })
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gagal start User Data Stream: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
        depthJob?.cancel()
        candlesJob?.cancel()
        positionJob?.cancel()
        settingsModeJob?.cancel()
        userDataStreamJob?.cancel()
        balanceObservationJob?.cancel()
        userDataStreamWebSocket?.close(1000, "Cleared")
    }

    fun swapCurrency(fromAsset: String, toAsset: String, amount: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExecuting = true, orderMessage = null) }
            val rate = _uiState.value.usdtRate.coerceAtLeast(1000.0)
            val res = paperTradeRepository.swapPaperCurrency(fromAsset, toAsset, amount, rate)
            if (res.isSuccess) {
                _uiState.update {
                    it.copy(
                        isExecuting = false,
                        orderMessage = "Konversi $fromAsset ke $toAsset berhasil!",
                        isOrderSuccess = true
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isExecuting = false,
                        orderMessage = "Gagal konversi: ${res.exceptionOrNull()?.message}",
                        isOrderSuccess = false
                    )
                }
            }
        }
    }

    fun executeBuy() {
        val state = _uiState.value
        val currentPrice = state.ticker?.price ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isExecuting = true, orderMessage = null) }
            if (state.isPaperMode) {
                val result = paperTradeRepository.executeBuy(
                    symbol = state.symbol,
                    amount = state.selectedNominal,
                    price = currentPrice
                )
                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            isExecuting = false,
                            orderMessage = "Order Paper BUY Berhasil! ID: ${result.getOrNull()?.orderId}",
                            isOrderSuccess = true
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isExecuting = false,
                            orderMessage = "Gagal Paper: ${result.exceptionOrNull()?.message}",
                            isOrderSuccess = false
                        )
                    }
                }
            } else {
                // Real Spot execution
                val creds = settingsRepository.getApiCredentials().first()
                if (creds.apiKey.isBlank() || creds.secret.isBlank()) {
                    _uiState.update {
                        it.copy(
                            isExecuting = false,
                            orderMessage = "API Key Tokocrypto belum diatur. Silakan atur di menu Pengaturan.",
                            isOrderSuccess = false
                        )
                    }
                    return@launch
                }

                val rawQty = state.selectedNominal / currentPrice
                val req = OrderRequest(
                    symbol = state.symbol,
                    side = 0, // BUY
                    type = 2, // MARKET
                    quantity = rawQty,
                    price = null,
                    stopPrice = null
                )
                val res = placeOrderUseCase(req)
                if (res.isSuccess) {
                    val orderResult = res.getOrNull()
                    positionRepository.savePosition(
                        Position(
                            symbol = state.symbol,
                            quantity = rawQty,
                            averageEntryPrice = currentPrice
                        )
                    )
                    _uiState.update {
                        it.copy(
                            isExecuting = false,
                            orderMessage = "Spot BUY Real Terkirim! Order ID: ${orderResult?.orderId ?: "FILLED"}",
                            isOrderSuccess = true
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isExecuting = false,
                            orderMessage = "Gagal Spot: ${res.exceptionOrNull()?.message}",
                            isOrderSuccess = false
                        )
                    }
                }
            }
        }
    }

    fun executeSell() {
        val state = _uiState.value
        val pos = state.currentPosition ?: return
        val currentPrice = state.ticker?.price ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isExecuting = true, orderMessage = null) }
            if (state.isPaperMode) {
                val result = paperTradeRepository.executeSell(
                    symbol = state.symbol,
                    quantity = pos.quantity,
                    price = currentPrice
                )
                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            isExecuting = false,
                            orderMessage = "Order Paper SELL Berhasil! Posisi ditutup.",
                            isOrderSuccess = true
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isExecuting = false,
                            orderMessage = "Gagal Paper SELL: ${result.exceptionOrNull()?.message}",
                            isOrderSuccess = false
                        )
                    }
                }
            } else {
                val creds = settingsRepository.getApiCredentials().first()
                if (creds.apiKey.isBlank() || creds.secret.isBlank()) {
                    _uiState.update {
                        it.copy(
                            isExecuting = false,
                            orderMessage = "API Key Tokocrypto belum diatur. Silakan atur di menu Pengaturan.",
                            isOrderSuccess = false
                        )
                    }
                    return@launch
                }

                val req = OrderRequest(
                    symbol = state.symbol,
                    side = 1, // SELL
                    type = 2, // MARKET
                    quantity = pos.quantity,
                    price = null,
                    stopPrice = null
                )
                val res = placeOrderUseCase(req)
                if (res.isSuccess) {
                    positionRepository.removePosition(state.symbol)
                    _uiState.update {
                        it.copy(
                            isExecuting = false,
                            orderMessage = "Spot SELL Real Terkirim! Posisi ditutup.",
                            isOrderSuccess = true
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isExecuting = false,
                            orderMessage = "Gagal Spot SELL: ${res.exceptionOrNull()?.message}",
                            isOrderSuccess = false
                        )
                    }
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(orderMessage = null, isOrderSuccess = null) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TokoReaderApp)
                RadarTradeViewModel(
                    marketDataRepository = app.container.marketDataRepository,
                    positionRepository = app.container.positionRepository,
                    paperTradeRepository = app.container.paperTradeRepository,
                    tradeRepository = app.container.tradeRepository,
                    placeOrderUseCase = app.container.placeOrderUseCase,
                    settingsRepository = app.container.settingsRepository
                )
            }
        }
    }
}
