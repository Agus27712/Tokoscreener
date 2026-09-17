package com.tokoreader.domain.repository

import com.tokoreader.domain.model.*
import kotlinx.coroutines.flow.Flow

interface MarketDataRepository {
    fun observeClosedCandles(symbol: String, timeframe: String): Flow<List<Kline>>
    fun observeCandlesWithLive(symbol: String, timeframe: String): Flow<List<Kline>>
    fun observeOrderBook(symbol: String): Flow<OrderBookSnapshot>
    fun observeTicker(symbol: String): Flow<Ticker>
    suspend fun getKlines(symbol: String, interval: String, limit: Int = 30): List<Kline>
    suspend fun getSymbolFilters(symbol: String): SymbolFilter?
    suspend fun getAllIdrTickers(): List<Ticker>
    suspend fun getAllTickers(): List<Ticker>
    suspend fun getTokocryptoSymbols(): List<com.tokoreader.data.remote.rest.SymbolInfo>
}

interface PositionRepository {
    fun observePosition(symbol: String): Flow<Position?>
    fun observeAllPositions(): Flow<List<Position>>
    suspend fun getPosition(symbol: String): Position?
    suspend fun savePosition(position: Position)
    suspend fun removePosition(symbol: String)
}

interface PaperTradeRepository {
    fun observePaperBalance(): Flow<Double>
    fun observePaperBalances(): Flow<PaperBalances>
    fun observePaperPositions(): Flow<List<Position>>
    fun observePaperOrders(): Flow<List<LocalOrder>>
    suspend fun executeBuy(symbol: String, amount: Double, price: Double): Result<OrderResult>
    suspend fun executeSell(symbol: String, quantity: Double, price: Double): Result<OrderResult>
    suspend fun swapPaperCurrency(fromAsset: String, toAsset: String, amount: Double, rateUsdtIdr: Double): Result<Boolean>
    suspend fun resetPaperAccount()
}

interface TradeRepository {
    suspend fun placeOrder(request: OrderRequest): Result<OrderResult>
    suspend fun placeOcoOrder(request: OcoOrderRequest): Result<OrderResult>
    suspend fun cancelOrder(symbol: String, orderId: String): Result<Boolean>
    fun observeUserDataEvents(symbolType: Int): Flow<UserDataEvent>
}
