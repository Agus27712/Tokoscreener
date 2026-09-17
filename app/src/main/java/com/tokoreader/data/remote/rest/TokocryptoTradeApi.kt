package com.tokoreader.data.remote.rest

import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface TokocryptoTradeApi {
    @Headers("Signed: true")
    @POST("open/v1/orders")
    suspend fun placeOrder(
        @Query("symbol") symbol: String,
        @Query("side") side: Int, // 0 = BUY, 1 = SELL
        @Query("type") type: Int, // 1=LIMIT, 2=MARKET, 3=STOP_LOSS, 4=STOP_LOSS_LIMIT, 5=TAKE_PROFIT, 6=TAKE_PROFIT_LIMIT, 7=LIMIT_MAKER
        @Query("quantity") quantity: String?,
        @Query("price") price: String?,
        @Query("stopPrice") stopPrice: String?
    ): OrderResponse

    @Headers("Signed: true")
    @POST("open/v1/orders/oco")
    suspend fun placeOcoOrder(
        @Query("symbol") symbol: String,
        @Query("side") side: Int, // 0 = BUY, 1 = SELL
        @Query("quantity") quantity: String,
        @Query("price") price: String,
        @Query("stopPrice") stopPrice: String,
        @Query("stopLimitPrice") stopLimitPrice: String?
    ): OrderResponse

    @Headers("Signed: true")
    @DELETE("open/v1/orders")
    suspend fun cancelOrder(
        @Query("symbol") symbol: String,
        @Query("orderId") orderId: String
    ): OrderResponse

    @Headers("Signed: true")
    @GET("open/v1/orders/detail")
    suspend fun getOrderDetail(
        @Query("symbol") symbol: String,
        @Query("orderId") orderId: String
    ): OrderResponse

    @Headers("Signed: true")
    @GET("open/v1/account/spot")
    suspend fun getAccountInfo(): AccountResponse

    @Headers("Signed: true")
    @POST("open/v1/user-data-stream")
    suspend fun createListenKey(): ListenKeyResponse
}

data class OrderResponse(
    val code: Int,
    val msg: String,
    val data: Any?
)

data class AccountResponse(
    val code: Int,
    val msg: String,
    val data: Any?
)

data class ListenKeyResponse(
    val code: Int,
    val msg: String,
    val data: String
)

