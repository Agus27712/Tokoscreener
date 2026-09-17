package com.tokoreader.data.repository

import android.util.Log
import com.tokoreader.data.remote.rest.TokocryptoMarketApi
import com.tokoreader.domain.model.Kline
import com.tokoreader.domain.model.OrderBookSnapshot
import com.tokoreader.domain.model.PriceTickDirection
import com.tokoreader.domain.model.SymbolFilter
import com.tokoreader.domain.model.Ticker
import com.tokoreader.domain.repository.MarketDataRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.*
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
    private val okHttpClient: OkHttpClient? = null
) : MarketDataRepository {

    private val TAG = "MarketDataRepo"
    private val filterCache = ConcurrentHashMap<String, SymbolFilter>()

    private val wsCandidateUrls = listOf(
        "wss://stream.binance.com:9443/ws",
        "wss://stream.binance.com:443/ws",
        "wss://data-stream.binance.vision/ws",
        "wss://stream-cloud.tokocrypto.site/stream"
    )

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
     * Real-time live Candle observation via WebSocket @kline stream + initial REST load
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
                Log.w(TAG, "Initial klines load error: ${e.message}")
            }
        }

        // 2. Connect live WebSocket for real-time candle ticking
        var activeWebSocket: WebSocket? = null
        val client = okHttpClient ?: OkHttpClient()

        fun connectWs(urlIndex: Int = 0) {
            if (isClosedChannel.get() || urlIndex >= wsCandidateUrls.size) return
            val baseUrl = wsCandidateUrls[urlIndex]
            val url = "$baseUrl/$streamName"
            val request = Request.Builder().url(url).build()

            activeWebSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d(TAG, "Klines WS connected: $url")
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val json = JSONObject(text)
                        val k = json.optJSONObject("k") ?: return
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
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing kline WS message: ${e.message}")
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.w(TAG, "Kline WS failed: ${t.message}, fallback to next URL")
                    if (!isClosedChannel.get()) {
                        launch(Dispatchers.IO) {
                            delay(2000)
                            connectWs((urlIndex + 1) % wsCandidateUrls.size)
                        }
                    }
                }
            })
        }

        connectWs()

        // 3. Fallback Periodic Polling in case WebSocket is silent
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
            activeWebSocket?.close(1000, "Closed")
        }
    }

    /**
     * Real-time Orderbook Snapshot & Live Updates via WebSocket @depth20@1000ms
     */
    override fun observeOrderBook(symbol: String): Flow<OrderBookSnapshot> = callbackFlow {
        val streamName = "${symbol.lowercase(Locale.ROOT)}@depth20@1000ms"
        val isClosedChannel = AtomicBoolean(false)
        var activeWebSocket: WebSocket? = null
        val client = okHttpClient ?: OkHttpClient()

        fun connectWs(urlIndex: Int = 0) {
            if (isClosedChannel.get() || urlIndex >= wsCandidateUrls.size) return
            val baseUrl = wsCandidateUrls[urlIndex]
            val url = "$baseUrl/$streamName"
            val request = Request.Builder().url(url).build()

            activeWebSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d(TAG, "Depth WS connected: $url")
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val json = JSONObject(text)
                        val bidsArray = json.optJSONArray("bids")
                        val asksArray = json.optJSONArray("asks")
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

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.w(TAG, "Depth WS failed: ${t.message}")
                    if (!isClosedChannel.get()) {
                        launch(Dispatchers.IO) {
                            delay(2000)
                            connectWs((urlIndex + 1) % wsCandidateUrls.size)
                        }
                    }
                }
            })
        }

        connectWs()

        // Fast REST Polling Fallback (1500ms)
        val pollJob = launch(Dispatchers.IO) {
            var delayMs = 1500L
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
                    delayMs = 2000L
                } catch (e: HttpException) {
                    if (e.code() == 429) delayMs = 5000L
                } catch (_: Exception) {
                    delayMs = 3000L
                }
                delay(delayMs)
            }
        }

        awaitClose {
            isClosedChannel.set(true)
            pollJob.cancel()
            activeWebSocket?.close(1000, "Closed")
        }
    }

    /**
     * Real-time Live Ticker & Trade Stream with Sub-Second Instant Price Flashes
     */
    override fun observeTicker(symbol: String): Flow<Ticker> = callbackFlow {
        val streamTicker = "${symbol.lowercase(Locale.ROOT)}@ticker"
        val isClosedChannel = AtomicBoolean(false)
        var lastKnownPrice: Double? = null
        var activeWebSocket: WebSocket? = null
        val client = okHttpClient ?: OkHttpClient()

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

        // 1. WebSocket Live Stream
        fun connectWs(urlIndex: Int = 0) {
            if (isClosedChannel.get() || urlIndex >= wsCandidateUrls.size) return
            val baseUrl = wsCandidateUrls[urlIndex]
            val url = "$baseUrl/$streamTicker"
            val request = Request.Builder().url(url).build()

            activeWebSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d(TAG, "Ticker WS Connected: $url")
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val json = JSONObject(text)
                        val price = json.optString("c", "0").toDoubleOrNull() ?: 0.0
                        val changePct = json.optString("P", "0").toDoubleOrNull() ?: 0.0
                        val vol = json.optString("q", "0").toDoubleOrNull() ?: 0.0
                        val high = json.optString("h", "0").toDoubleOrNull() ?: 0.0
                        val low = json.optString("l", "0").toDoubleOrNull() ?: 0.0

                        if (price > 0.0) {
                            emitTickerUpdate(price, changePct, vol, high, low)
                        }
                    } catch (_: Exception) {}
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.w(TAG, "Ticker WS error: ${t.message}")
                    if (!isClosedChannel.get()) {
                        launch(Dispatchers.IO) {
                            delay(2000)
                            connectWs((urlIndex + 1) % wsCandidateUrls.size)
                        }
                    }
                }
            })
        }

        connectWs()

        // 2. Fast REST Polling Fallback (1000ms) with immediate first tick
        val pollJob = launch(Dispatchers.IO) {
            var delayMs = 1000L
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
                    delayMs = 1200L
                } catch (e: HttpException) {
                    if (e.code() == 429) delayMs = (delayMs * 2).coerceAtMost(10000L)
                } catch (_: Exception) {
                    delayMs = 2000L
                }
                delay(delayMs)
            }
        }

        awaitClose {
            isClosedChannel.set(true)
            pollJob.cancel()
            activeWebSocket?.close(1000, "Closed")
        }
    }

    override suspend fun getSymbolFilters(symbol: String): SymbolFilter? {
        filterCache[symbol]?.let { return it }
        return try {
            val exInfo = api.getExchangeInfo(symbol)
            val symInfo = exInfo.symbols?.firstOrNull { it.symbol.equals(symbol, ignoreCase = true) }
            var tickSize = 1.0
            var stepSize = 0.00001
            var minQty = 0.00001
            var minNotional = 10000.0

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
            filterCache[symbol] = filter
            filter
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            val defaultFilter = if (symbol.endsWith("IDR")) {
                SymbolFilter(tickSize = 1.0, stepSize = 0.00001, minQty = 0.00001, minNotional = 10000.0)
            } else {
                SymbolFilter(tickSize = 0.01, stepSize = 0.0001, minQty = 0.0001, minNotional = 5.0)
            }
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
            val response = api.get24hTicker()
            val result = response.mapNotNull { data ->
                val sym = data.symbol ?: return@mapNotNull null
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
}
