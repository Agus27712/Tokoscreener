package com.tokoreader.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.tokoreader.data.local.logging.AppLogger
import com.tokoreader.data.remote.rest.DefaultTokocryptoCatalog
import com.tokoreader.data.remote.rest.TokocryptoMarketApi
import com.tokoreader.data.remote.rest.TokocryptoTradeApi
import com.tokoreader.data.remote.rest.SymbolInfo
import com.tokoreader.data.remote.websocket.TokocryptoMarketSocket
import com.tokoreader.domain.model.Kline
import com.tokoreader.domain.model.OrderBookSnapshot
import com.tokoreader.domain.model.PriceTickDirection
import com.tokoreader.domain.model.SymbolFilter
import com.tokoreader.domain.model.Ticker
import com.tokoreader.domain.repository.MarketDataRepository
import com.tokoreader.domain.repository.SettingsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.HttpException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * High-performance MarketDataRepository using real-time WebSocket streams
 * (instant tick/trades/depth/klines) backed by fast adaptive REST fallback.
 */
class MarketDataRepositoryImpl(
    private val api: TokocryptoMarketApi,
    private val tradeApi: TokocryptoTradeApi,
    private val settingsRepository: SettingsRepository,
    private val okHttpClient: OkHttpClient? = null,
    private val context: Context? = null
) : MarketDataRepository {

    private val TAG = "MarketDataRepo"
    private val filterCache = ConcurrentHashMap<String, SymbolFilter>()
    val symbolInfoCache = ConcurrentHashMap<String, SymbolInfo>()
    private var isRemoteSymbolsLoaded = false
    private var lastSymbolsLoadAttemptTime = 0L

    init {
        // Pre-seed immediately with official Tokocrypto symbols catalog
        DefaultTokocryptoCatalog.getDefaultSymbolInfoMap().forEach { (k, v) ->
            symbolInfoCache[k] = v
        }
    }

    private suspend fun ensureSymbolsLoaded() {
        val now = System.currentTimeMillis()
        if (isRemoteSymbolsLoaded || (now - lastSymbolsLoadAttemptTime) < 60_000L) {
            return
        }
        lastSymbolsLoadAttemptTime = now

        // 1. Coba ambil dari tradeApi.getSymbols()
        try {
            AppLogger.d(TAG, "Fetching live symbols from Tokocrypto...")
            val response = tradeApi.getSymbols()
            val list = response.data
            if (!list.isNullOrEmpty()) {
                list.forEach { info ->
                    symbolInfoCache[info.symbol.uppercase(Locale.ROOT)] = info
                }
                isRemoteSymbolsLoaded = true
                AppLogger.i(TAG, "Successfully loaded ${symbolInfoCache.size} Tokocrypto symbols from tradeApi.")
                return
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            AppLogger.w(TAG, "Tokocrypto open/v1 symbols returned ${e.message}, trying market exchangeInfo...")
        }

        // 2. Fallback sekunder: marketApi.getExchangeInfo() melalui mirror hosts
        try {
            val exchangeInfo = api.getExchangeInfo()
            val syms = exchangeInfo.symbols
            if (!syms.isNullOrEmpty()) {
                syms.forEach { symInfoResp ->
                    val s = symInfoResp.symbol ?: return@forEach
                    val upper = s.uppercase(Locale.ROOT)
                    val type = if (upper.endsWith("IDR")) 3 else 1
                    val converted = SymbolInfo(
                        symbol = upper,
                        type = type,
                        baseAsset = symInfoResp.baseAsset,
                        quoteAsset = symInfoResp.quoteAsset,
                        filters = symInfoResp.filters
                    )
                    symbolInfoCache[upper] = converted
                }
                isRemoteSymbolsLoaded = true
                AppLogger.i(TAG, "Successfully loaded ${symbolInfoCache.size} symbols via market exchangeInfo.")
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            AppLogger.w(TAG, "Market exchangeInfo unavailable (${e.message}), using embedded Tokocrypto catalog.")
        }
    }

    private val wsCandidateUrls = listOf(
        "wss://stream-cloud.tokocrypto.site/stream",
        "wss://stream-toko.2meta.app/stream",
        "wss://data-stream.binance.vision/stream",
        "wss://stream.binance.com:443/stream"
    )

    private val wsManager by lazy {
        TokocryptoMarketSocket(okHttpClient ?: OkHttpClient(), wsCandidateUrls)
    }

    init {
        try {
            val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (connectivityManager != null) {
                val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
                connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        AppLogger.i("MarketDataRepo", "Koneksi internet terdeteksi! Memaksa WebSocket reconnect...")
                        wsManager.reconnect()
                    }
                })
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gagal registrasi callback status jaringan: ${e.message}")
        }
    }

    override suspend fun getKlines(symbol: String, interval: String, limit: Int): List<Kline> {
        return try {
            val response = api.getKlines(symbol, interval, limit)
            response.map { item ->
                Kline(
                    openTime = (item[0] as Number).toLong(),
                    open = (item[1] as String).toDouble(),
                    high = (item[2] as String).toDouble(),
                    low = (item[3] as String).toDouble(),
                    close = (item[4] as String).toDouble(),
                    volume = (item[5] as String).toDouble(),
                    closeTime = (item[6] as Number).toLong(),
                    quoteVolume = (item[7] as String).toDouble(),
                    trades = (item[8] as Number).toInt(),
                    takerBuyBaseVol = (item[9] as String).toDouble(),
                    takerBuyQuoteVol = (item[10] as String).toDouble(),
                    isClosed = true
                )
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Error fetching direct klines for $symbol: ${e.message}")
            emptyList()
        }
    }

    /**
     * Real-time live Candle observation via WebSocket @kline stream + initial REST load.
     * Filtered to only emit completed (closed) candles.
     */
    override fun observeClosedCandles(symbol: String, timeframe: String): Flow<List<Kline>> = callbackFlow {
        val streamName = "${symbol.lowercase(Locale.ROOT)}@kline_$timeframe"
        val candlesList = mutableListOf<Kline>()
        val isClosedChannel = AtomicBoolean(false)

        // 1. Initial historical candles load from REST
        launch(Dispatchers.IO) {
            try {
                val initial = getKlines(symbol, timeframe, 250)
                if (initial.isNotEmpty()) {
                    synchronized(candlesList) {
                        candlesList.clear()
                        candlesList.addAll(initial)
                    }
                    trySend(candlesList.toList())
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                AppLogger.w(TAG, "Initial klines load error: ${e.message}")
            }
        }

        // 2. Connect via Multiplexed WS Manager
        val listener: (JSONObject) -> Unit = { data ->
            try {
                val k = data.optJSONObject("k")
                if (k != null) {
                    val openTime = k.optLong("t", 0L)
                    val open = k.optString("o", "0").toDoubleOrNull() ?: 0.0
                    val high = k.optString("h", "0").toDoubleOrNull() ?: 0.0
                    val low = k.optString("l", "0").toDoubleOrNull() ?: 0.0
                    val close = k.optString("c", "0").toDoubleOrNull() ?: 0.0
                    val vol = k.optString("v", "0").toDoubleOrNull() ?: 0.0
                    val isBarClosed = k.optBoolean("x", false)

                    if (openTime > 0) {
                        val liveKline = Kline(
                            openTime = openTime,
                            open = open,
                            high = high,
                            low = low,
                            close = close,
                            volume = vol,
                            closeTime = k.optLong("T", 0L),
                            quoteVolume = k.optString("q", "0").toDoubleOrNull() ?: 0.0,
                            trades = k.optInt("n", 0),
                            takerBuyBaseVol = 0.0,
                            takerBuyQuoteVol = 0.0,
                            isClosed = isBarClosed
                        )

                        var shouldEmit = false
                        synchronized(candlesList) {
                            val lastIdx = candlesList.indexOfLast { it.openTime == openTime }
                            if (lastIdx >= 0) {
                                candlesList[lastIdx] = liveKline
                                if (lastIdx < candlesList.size - 1) {
                                    shouldEmit = true
                                }
                            } else {
                                candlesList.add(liveKline)
                                if (candlesList.size > 250) {
                                    candlesList.removeAt(0)
                                }
                                val newIdx = candlesList.indexOfLast { it.openTime == openTime }
                                if (newIdx >= 0 && newIdx < candlesList.size - 1) {
                                    shouldEmit = true
                                }
                            }
                        }
                        if (isBarClosed || shouldEmit) {
                            trySend(candlesList.toList())
                        }
                    }
                }
            } catch (e: Exception) {
                AppLogger.w(TAG, "Error parsing kline WS message: ${e.message}")
            }
        }

        wsManager.subscribe(streamName, listener)

        // 3. Fallback Periodic Polling in case WebSocket is silent (8s interval to avoid 429)
        val pollJob = launch(Dispatchers.IO) {
            delay(5000)
            while (isActive && !isClosedChannel.get()) {
                try {
                    val fresh = getKlines(symbol, timeframe, 250)
                    if (fresh.isNotEmpty()) {
                        synchronized(candlesList) {
                            candlesList.clear()
                            candlesList.addAll(fresh)
                        }
                        trySend(candlesList.toList())
                    }
                    delay(8000)
                } catch (e: Exception) {
                    delay(12000)
                }
            }
        }

        awaitClose {
            isClosedChannel.set(true)
            pollJob.cancel()
            wsManager.unsubscribe(streamName, listener)
        }
    }

    /**
     * Real-time live Candle observation via WebSocket @kline stream + initial REST load.
     * Emits all updates, including the current in-progress candle.
     */
    override fun observeCandlesWithLive(symbol: String, timeframe: String): Flow<List<Kline>> = callbackFlow {
        val streamName = "${symbol.lowercase(Locale.ROOT)}@kline_$timeframe"
        val candlesList = mutableListOf<Kline>()
        val isClosedChannel = AtomicBoolean(false)

        // 1. Initial historical candles load from REST
        launch(Dispatchers.IO) {
            try {
                val initial = getKlines(symbol, timeframe, 250)
                if (initial.isNotEmpty()) {
                    synchronized(candlesList) {
                        candlesList.clear()
                        candlesList.addAll(initial)
                    }
                    trySend(candlesList.toList())
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                AppLogger.w(TAG, "Initial klines load error: ${e.message}")
            }
        }

        // 2. Connect via Multiplexed WS Manager
        val listener: (JSONObject) -> Unit = { data ->
            try {
                val k = data.optJSONObject("k")
                if (k != null) {
                    val openTime = k.optLong("t", 0L)
                    val open = k.optString("o", "0").toDoubleOrNull() ?: 0.0
                    val high = k.optString("h", "0").toDoubleOrNull() ?: 0.0
                    val low = k.optString("l", "0").toDoubleOrNull() ?: 0.0
                    val close = k.optString("c", "0").toDoubleOrNull() ?: 0.0
                    val vol = k.optString("v", "0").toDoubleOrNull() ?: 0.0
                    val isBarClosed = k.optBoolean("x", false)

                    if (openTime > 0) {
                        val liveKline = Kline(
                            openTime = openTime,
                            open = open,
                            high = high,
                            low = low,
                            close = close,
                            volume = vol,
                            closeTime = k.optLong("T", 0L),
                            quoteVolume = k.optString("q", "0").toDoubleOrNull() ?: 0.0,
                            trades = k.optInt("n", 0),
                            takerBuyBaseVol = 0.0,
                            takerBuyQuoteVol = 0.0,
                            isClosed = isBarClosed
                        )

                        synchronized(candlesList) {
                            val lastIdx = candlesList.indexOfLast { it.openTime == openTime }
                            if (lastIdx >= 0) {
                                candlesList[lastIdx] = liveKline
                            } else {
                                candlesList.add(liveKline)
                                if (candlesList.size > 250) {
                                    candlesList.removeAt(0)
                                }
                            }
                        }
                        trySend(candlesList.toList())
                    }
                }
            } catch (e: Exception) {
                AppLogger.w(TAG, "Error parsing kline WS message: ${e.message}")
            }
        }

        wsManager.subscribe(streamName, listener)

        // 3. Fallback Periodic Polling
        val pollJob = launch(Dispatchers.IO) {
            delay(5000)
            while (isActive && !isClosedChannel.get()) {
                try {
                    val fresh = getKlines(symbol, timeframe, 250)
                    if (fresh.isNotEmpty()) {
                        synchronized(candlesList) {
                            candlesList.clear()
                            candlesList.addAll(fresh)
                        }
                        trySend(candlesList.toList())
                    }
                    delay(8000)
                } catch (e: Exception) {
                    delay(12000)
                }
            }
        }

        awaitClose {
            isClosedChannel.set(true)
            pollJob.cancel()
            wsManager.unsubscribe(streamName, listener)
        }
    }

    /**
     * Real-time Orderbook Snapshot & Live Updates via WebSocket @depth20@1000ms
     */
    override fun observeOrderBook(symbol: String): Flow<OrderBookSnapshot> = callbackFlow {
        val streamName = "${symbol.lowercase(Locale.ROOT)}@depth20@1000ms"
        val isClosedChannel = AtomicBoolean(false)

        val listener: (JSONObject) -> Unit = { data ->
            try {
                val bidsArray = data.optJSONArray("bids")
                val asksArray = data.optJSONArray("asks")
                val bids = mutableListOf<Pair<Double, Double>>()
                val asks = mutableListOf<Pair<Double, Double>>()

                if (bidsArray != null) {
                    for (i in 0 until bidsArray.length()) {
                        val item = bidsArray.getJSONArray(i)
                        val p = item.getString(0).toDoubleOrNull()
                        val q = item.getString(1).toDoubleOrNull()
                        if (p != null && q != null) bids.add(p to q)
                    }
                }
                if (asksArray != null) {
                    for (i in 0 until asksArray.length()) {
                        val item = asksArray.getJSONArray(i)
                        val p = item.getString(0).toDoubleOrNull()
                        val q = item.getString(1).toDoubleOrNull()
                        if (p != null && q != null) asks.add(p to q)
                    }
                }

                if (bids.isNotEmpty() || asks.isNotEmpty()) {
                    trySend(OrderBookSnapshot(bids = bids, asks = asks))
                }
            } catch (_: Exception) {}
        }

        wsManager.subscribe(streamName, listener)

        // Fast REST Polling Fallback (4000ms) to avoid HTTP 429 rate limit
        val pollJob = launch(Dispatchers.IO) {
            var delayMs = 4000L
            while (isActive && !isClosedChannel.get()) {
                try {
                    val depth = api.getDepth(symbol, 20)
                    val bids = depth.bids.mapNotNull {
                        val price = it.getOrNull(0)?.toDoubleOrNull()
                        val qty = it.getOrNull(1)?.toDoubleOrNull()
                        if (price != null && qty != null) price to qty else null
                    }
                    val asks = depth.asks.mapNotNull {
                        val price = it.getOrNull(0)?.toDoubleOrNull()
                        val qty = it.getOrNull(1)?.toDoubleOrNull()
                        if (price != null && qty != null) price to qty else null
                    }
                    trySend(OrderBookSnapshot(bids = bids, asks = asks))
                    delayMs = 5000L
                } catch (e: HttpException) {
                    if (e.code() == 429) delayMs = 12000L
                } catch (_: Exception) {
                    delayMs = 6000L
                }
                delay(delayMs)
            }
        }

        awaitClose {
            isClosedChannel.set(true)
            pollJob.cancel()
            wsManager.unsubscribe(streamName, listener)
        }
    }

    /**
     * Real-time Live Ticker & Trade Stream with Sub-Second Instant Price Flashes
     */
    override fun observeTicker(symbol: String): Flow<Ticker> = callbackFlow {
        val streamTicker = "${symbol.lowercase(Locale.ROOT)}@ticker"
        val isClosedChannel = AtomicBoolean(false)
        var lastKnownPrice: Double? = null

        fun emitTickerUpdate(
            price: Double,
            changePct: Double,
            vol: Double,
            high: Double = 0.0,
            low: Double = 0.0
        ) {
            if (price <= 0.0) return
            val direction = when {
                lastKnownPrice != null && price > lastKnownPrice!! -> PriceTickDirection.UP
                lastKnownPrice != null && price < lastKnownPrice!! -> PriceTickDirection.DOWN
                else -> PriceTickDirection.NEUTRAL
            }
            lastKnownPrice = price

            val ticker = Ticker(
                symbol = symbol,
                price = price,
                priceChangePercent = changePct,
                volume24h = vol,
                high24h = high,
                low24h = low,
                tickDirection = direction,
                lastUpdated = System.currentTimeMillis()
            )
            trySend(ticker)
        }

        val listener: (JSONObject) -> Unit = { data ->
            try {
                val price = data.optString("c", "0").toDoubleOrNull() ?: 0.0
                val changePct = data.optString("P", "0").toDoubleOrNull() ?: 0.0
                val vol = data.optString("q", "0").toDoubleOrNull() ?: 0.0
                val high = data.optString("h", "0").toDoubleOrNull() ?: 0.0
                val low = data.optString("l", "0").toDoubleOrNull() ?: 0.0

                if (price > 0.0) {
                    emitTickerUpdate(price, changePct, vol, high, low)
                }
            } catch (_: Exception) {}
        }

        wsManager.subscribe(streamTicker, listener)

        // 2. Fast REST Polling Fallback (3500ms) to avoid HTTP 429 rate limits
        val pollJob = launch(Dispatchers.IO) {
            var delayMs = 3500L
            while (isActive && !isClosedChannel.get()) {
                try {
                    val dto = api.getSingle24hTicker(symbol)
                    val price = dto.lastPrice?.toDoubleOrNull() ?: 0.0
                    val change = dto.priceChangePercent?.toDoubleOrNull() ?: 0.0
                    val vol = dto.quoteVolume?.toDoubleOrNull() ?: 0.0
                    val high = dto.highPrice?.toDoubleOrNull() ?: 0.0
                    val low = dto.lowPrice?.toDoubleOrNull() ?: 0.0

                    if (price > 0.0) {
                        emitTickerUpdate(price, change, vol, high, low)
                    }
                    delayMs = 4000L
                } catch (e: HttpException) {
                    if (e.code() == 429) delayMs = 12000L
                } catch (_: Exception) {
                    delayMs = 5000L
                }
                delay(delayMs)
            }
        }

        awaitClose {
            isClosedChannel.set(true)
            pollJob.cancel()
            wsManager.unsubscribe(streamTicker, listener)
        }
    }

    override suspend fun getSymbolFilters(symbol: String): SymbolFilter? {
        val upper = symbol.uppercase(Locale.ROOT)
        filterCache[upper]?.let { return it }
        return try {
            ensureSymbolsLoaded()
            val symInfo = symbolInfoCache[upper]

            var tickSize = if (upper.endsWith("IDR")) 1.0 else 0.0001
            var stepSize = if (upper.endsWith("IDR")) 0.00001 else 0.0001
            var minQty = if (upper.endsWith("IDR")) 0.00001 else 0.0001
            var minNotional = if (upper.endsWith("IDR")) 10000.0 else 5.0

            symInfo?.filters?.forEach { filter ->
                val type = filter["filterType"] as? String ?: ""
                when (type) {
                    "PRICE_FILTER" -> {
                        tickSize = (filter["tickSize"] as? String)?.toDoubleOrNull() ?: tickSize
                    }
                    "LOT_SIZE" -> {
                        stepSize = (filter["stepSize"] as? String)?.toDoubleOrNull() ?: stepSize
                        minQty = (filter["minQty"] as? String)?.toDoubleOrNull() ?: minQty
                    }
                    "MIN_NOTIONAL", "NOTIONAL" -> {
                        minNotional = (filter["minNotional"] as? String)?.toDoubleOrNull()
                            ?: (filter["notional"] as? String)?.toDoubleOrNull()
                            ?: minNotional
                    }
                }
            }
            val filter = SymbolFilter(
                tickSize = tickSize,
                stepSize = stepSize,
                minQty = minQty,
                minNotional = minNotional
            )
            filterCache[upper] = filter
            filterCache[symbol] = filter
            filter
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            // Last-resort safety net fallback only
            val defaultFilter = if (upper.endsWith("IDR")) {
                SymbolFilter(tickSize = 1.0, stepSize = 0.00001, minQty = 0.00001, minNotional = 10000.0)
            } else {
                SymbolFilter(tickSize = 0.01, stepSize = 0.0001, minQty = 0.0001, minNotional = 5.0)
            }
            filterCache[upper] = defaultFilter
            filterCache[symbol] = defaultFilter
            defaultFilter
        }
    }

    private var cachedTickers: List<Ticker> = emptyList()
    private var lastTickersFetchTime: Long = 0L

    override suspend fun getAllTickers(): List<Ticker> {
        val now = System.currentTimeMillis()
        if (cachedTickers.isNotEmpty() && (now - lastTickersFetchTime) < 4000L) {
            return cachedTickers
        }

        return try {
            ensureSymbolsLoaded()
            val selectedType = settingsRepository.getCurrentSymbolType()

            val response = api.get24hTicker()
            val result = response.mapNotNull { data ->
                val sym = data.symbol ?: return@mapNotNull null
                
                // Filter ketat hanya koin yang terdaftar di Tokocrypto dan cocok dengan tipe engine yang dipilih (1 atau 3)
                if (symbolInfoCache.isNotEmpty()) {
                    val info = symbolInfoCache[sym.uppercase(Locale.ROOT)] ?: return@mapNotNull null
                    if (info.type != selectedType) {
                        return@mapNotNull null
                    }
                }

                if (sym.endsWith("BIDR") || sym.contains("UP") || sym.contains("DOWN") || sym.contains("BEAR") || sym.contains("BULL")) {
                    return@mapNotNull null
                }
                val price = data.lastPrice?.toDoubleOrNull() ?: 0.0
                val volume = data.quoteVolume?.toDoubleOrNull() ?: 0.0
                if (price <= 0.0) return@mapNotNull null

                val isSufficientVolume = when {
                    sym.endsWith("IDR") -> volume >= 1_000_000.0
                    sym.endsWith("USDT") -> volume >= 100.0
                    sym.endsWith("BTC") -> volume >= 0.005
                    else -> volume > 0.0
                }
                if (!isSufficientVolume) return@mapNotNull null

                Ticker(
                    symbol = sym,
                    price = price,
                    priceChangePercent = data.priceChangePercent?.toDoubleOrNull() ?: 0.0,
                    volume24h = volume,
                    high24h = data.highPrice?.toDoubleOrNull() ?: 0.0,
                    low24h = data.lowPrice?.toDoubleOrNull() ?: 0.0
                )
            }.sortedByDescending { it.volume24h }

            cachedTickers = result
            lastTickersFetchTime = now
            result
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Error fetching all tickers: ${e.message}")
            if (cachedTickers.isNotEmpty()) cachedTickers else emptyList()
        }
    }

    override suspend fun getAllIdrTickers(): List<Ticker> {
        val all = getAllTickers()
        val idrPairs = all.filter { it.symbol.endsWith("IDR") }
        if (idrPairs.isNotEmpty()) {
            return idrPairs
        }
        return all.filter { it.symbol.endsWith("USDT") }.take(30)
    }

    override suspend fun getTokocryptoSymbols(): List<SymbolInfo> {
        ensureSymbolsLoaded()
        return symbolInfoCache.values.toList().sortedBy { it.symbol }
    }
}
