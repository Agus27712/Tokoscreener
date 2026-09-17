package com.tokoreader.domain.model

data class OrderRequest(
    val symbol: String,
    val side: Int, // 0 = BUY, 1 = SELL
    val type: Int,
    val quantity: Double?,
    val price: Double?,
    val stopPrice: Double?
)

data class OcoOrderRequest(
    val symbol: String,
    val side: Int,
    val quantity: Double,
    val price: Double, // Target profit price
    val stopPrice: Double, // Stop loss trigger
    val stopLimitPrice: Double? // Stop loss limit price
)

data class OrderResult(
    val orderId: String,
    val status: String
)

data class SymbolFilter(
    val tickSize: Double,
    val stepSize: Double,
    val minQty: Double,
    val minNotional: Double
)

data class OrderBookSnapshot(
    val bids: List<Pair<Double, Double>>,
    val asks: List<Pair<Double, Double>>
)

enum class PriceTickDirection {
    UP,
    DOWN,
    NEUTRAL
}

data class TradeTick(
    val price: Double,
    val quantity: Double,
    val isBuyerMaker: Boolean, // true = seller hit bid (Market Sell / Red), false = buyer hit ask (Market Buy / Green)
    val timestamp: Long
)

data class Ticker(
    val symbol: String,
    val price: Double,
    val priceChangePercent: Double,
    val volume24h: Double,
    val high24h: Double = 0.0,
    val low24h: Double = 0.0,
    val tickDirection: PriceTickDirection = PriceTickDirection.NEUTRAL,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class LocalOrder(
    val id: Long = 0,
    val orderId: String,
    val symbol: String,
    val side: String, // BUY or SELL
    val type: String, // MARKET, LIMIT, etc.
    val price: Double,
    val quantity: Double,
    val totalAmount: Double,
    val isPaper: Boolean,
    val timestamp: Long,
    val status: String // FILLED, PENDING, etc.
)

data class PaperBalances(
    val balanceIdr: Double,
    val balanceUsdt: Double
)

sealed interface UserDataEvent {
    data class BalanceUpdate(val assets: Map<String, Double>) : UserDataEvent
    data class OrderFilled(val symbol: String, val side: String, val quantity: Double, val price: Double) : UserDataEvent
}

