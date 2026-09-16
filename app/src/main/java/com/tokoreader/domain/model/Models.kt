package com.tokoreader.domain.model

data class ApiCredentials(
    val apiKey: String,
    val secret: String
)

data class Kline(
    val openTime: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val closeTime: Long,
    val quoteVolume: Double,
    val trades: Int,
    val takerBuyBaseVol: Double,
    val takerBuyQuoteVol: Double,
    val isClosed: Boolean = true
)

data class Position(
    val symbol: String,
    val quantity: Double,
    val averageEntryPrice: Double
)

enum class TradingMode(val timeframe: String) {
    SCALPING("1m"),
    DAY_TRADING("15m"),
    SWING("4h")
}

sealed interface TradingSignal {
    data object NotHolding : TradingSignal
    data object MonitoringBuy : TradingSignal
    data class ReadyToBuy(val reason: String, val price: Double) : TradingSignal
    data object MonitoringSell : TradingSignal
    data object ApproachingTarget : TradingSignal
    data class ReadyToSell(val reason: String, val price: Double) : TradingSignal
    data object TrailingTriggered : TradingSignal
    data object StopLossHit : TradingSignal
}
