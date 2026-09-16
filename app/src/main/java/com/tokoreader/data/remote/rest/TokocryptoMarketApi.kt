package com.tokoreader.data.remote.rest

import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface TokocryptoMarketApi {
    @GET("api/v3/klines")
    suspend fun getKlines(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("limit") limit: Int = 100
    ): List<List<Any>>

    @GET("api/v3/depth")
    suspend fun getDepth(
        @Query("symbol") symbol: String,
        @Query("limit") limit: Int = 100
    ): DepthResponse

    @GET("api/v3/ticker/24hr")
    @Headers("Accept: application/json")
    suspend fun get24hTicker(): List<TickerResponse>

    @GET("api/v3/ticker/24hr")
    suspend fun getSingle24hTicker(
        @Query("symbol") symbol: String
    ): TickerResponse

    @GET("api/v3/exchangeInfo")
    suspend fun getExchangeInfo(
        @Query("symbol") symbol: String? = null
    ): ExchangeInfoResponse
}

data class ExchangeInfoResponse(
    val symbols: List<SymbolInfoResponse>?
)

data class SymbolInfoResponse(
    val symbol: String?,
    val status: String?,
    val baseAsset: String?,
    val quoteAsset: String?,
    val filters: List<Map<String, Any>>?
)

data class TickerResponse(
    val symbol: String?,
    val lastPrice: String?,
    val priceChangePercent: String?,
    val volume: String?,
    val quoteVolume: String?,
    val highPrice: String? = null,
    val lowPrice: String? = null
)

data class DepthResponse(
    val lastUpdateId: Long,
    val bids: List<List<String>>,
    val asks: List<List<String>>
)
