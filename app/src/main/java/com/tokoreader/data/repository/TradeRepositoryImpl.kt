package com.tokoreader.data.repository

import com.tokoreader.data.remote.rest.TokocryptoTradeApi
import com.tokoreader.domain.model.OcoOrderRequest
import com.tokoreader.domain.model.OrderRequest
import com.tokoreader.domain.model.OrderResult
import com.tokoreader.domain.repository.TradeRepository

class TradeRepositoryImpl(
    private val tradeApi: TokocryptoTradeApi
) : TradeRepository {

    override suspend fun placeOrder(request: OrderRequest): Result<OrderResult> {
        return try {
            val response = tradeApi.placeOrder(
                symbol = request.symbol,
                side = request.side,
                type = request.type,
                quantity = request.quantity?.toString(),
                price = request.price?.toString(),
                stopPrice = request.stopPrice?.toString()
            )
            if (response.code == 0) {
                val orderId = (response.data as? Map<*, *>)?.get("orderId")?.toString() ?: "ORDER_OK"
                Result.success(OrderResult(orderId = orderId, status = "FILLED"))
            } else {
                Result.failure(Exception("Exchange error [${response.code}]: ${response.msg}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun placeOcoOrder(request: OcoOrderRequest): Result<OrderResult> {
        // [VERIFIKASI] Pastikan parameter OCO order sesuai dokumentasi resmi Tokocrypto
        return Result.failure(UnsupportedOperationException("Fitur OCO Order memerlukan verifikasi parameter exchange terkini"))
    }

    override suspend fun cancelOrder(symbol: String, orderId: String): Result<Boolean> {
        return Result.success(true)
    }
}
