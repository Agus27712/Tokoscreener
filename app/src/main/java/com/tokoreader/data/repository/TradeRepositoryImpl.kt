package com.tokoreader.data.repository

import com.tokoreader.data.remote.rest.TokocryptoTradeApi
import com.tokoreader.data.remote.websocket.TokocryptoUserDataSocket
import com.tokoreader.domain.model.OcoOrderRequest
import com.tokoreader.domain.model.OrderRequest
import com.tokoreader.domain.model.OrderResult
import com.tokoreader.domain.model.UserDataEvent
import com.tokoreader.domain.repository.TradeRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import java.math.BigDecimal

class TradeRepositoryImpl(
    private val tradeApi: TokocryptoTradeApi,
    private val userDataSocket: TokocryptoUserDataSocket
) : TradeRepository {

    override fun observeUserDataEvents(symbolType: Int): Flow<UserDataEvent> {
        return userDataSocket.connect(symbolType)
    }

    override suspend fun placeOrder(request: OrderRequest): Result<OrderResult> {
        return executeWithRetry(maxRetries = 2) {
            val formattedQty = request.quantity?.let { formatValue(it) }
            val formattedPrice = request.price?.let { formatValue(it) }
            val formattedStop = request.stopPrice?.let { formatValue(it) }

            val response = tradeApi.placeOrder(
                symbol = request.symbol,
                side = request.side,
                type = request.type,
                quantity = formattedQty,
                price = formattedPrice,
                stopPrice = formattedStop
            )

            if (response.code == 0) {
                val orderId = when (val data = response.data) {
                    is Map<*, *> -> data["orderId"]?.toString() ?: "ORD-${System.currentTimeMillis()}"
                    is String -> data
                    else -> "ORD-${System.currentTimeMillis()}"
                }
                val finalStatus = pollOrderStatus(request.symbol, orderId)
                OrderResult(orderId = orderId, status = finalStatus)
            } else {
                throw Exception("Tokocrypto [${response.code}]: ${response.msg}")
            }
        }
    }

    override suspend fun placeOcoOrder(request: OcoOrderRequest): Result<OrderResult> {
        return executeWithRetry(maxRetries = 2) {
            val formattedQty = formatValue(request.quantity)
            val formattedPrice = formatValue(request.price)
            val formattedStop = formatValue(request.stopPrice)
            val formattedStopLimit = request.stopLimitPrice?.let { formatValue(it) }

            val response = tradeApi.placeOcoOrder(
                symbol = request.symbol,
                side = request.side,
                quantity = formattedQty,
                price = formattedPrice,
                stopPrice = formattedStop,
                stopLimitPrice = formattedStopLimit
            )

            if (response.code == 0) {
                val orderId = when (val data = response.data) {
                    is Map<*, *> -> data["orderId"]?.toString() ?: "OCO-${System.currentTimeMillis()}"
                    is String -> data
                    else -> "OCO-${System.currentTimeMillis()}"
                }
                val finalStatus = pollOrderStatus(request.symbol, orderId)
                OrderResult(orderId = orderId, status = finalStatus)
            } else {
                throw Exception("Tokocrypto OCO [${response.code}]: ${response.msg}")
            }
        }
    }

    private suspend fun pollOrderStatus(symbol: String, orderId: String, maxAttempts: Int = 5, delayMs: Long = 1000L): String {
        for (attempt in 1..maxAttempts) {
            try {
                val response = tradeApi.getOrderDetail(symbol = symbol, orderId = orderId)
                if (response.code == 0) {
                    val status = when (val data = response.data) {
                        is Map<*, *> -> data["status"]?.toString() ?: "NEW"
                        else -> "NEW"
                    }
                    if (status == "FILLED" || status == "CANCELED" || status == "REJECTED" || status == "EXPIRED") {
                        return status
                    }
                }
            } catch (e: Exception) {
                // Ignore transient network errors and continue polling
            }
            if (attempt < maxAttempts) {
                delay(delayMs)
            }
        }
        return "NEW"
    }

    override suspend fun cancelOrder(symbol: String, orderId: String): Result<Boolean> {
        return executeWithRetry(maxRetries = 2) {
            val response = tradeApi.cancelOrder(symbol = symbol, orderId = orderId)
            if (response.code == 0) {
                true
            } else {
                throw Exception("Batal Order Tokocrypto [${response.code}]: ${response.msg}")
            }
        }
    }

    private suspend fun <T> executeWithRetry(
        maxRetries: Int = 2,
        initialDelayMs: Long = 1000L,
        block: suspend () -> T
    ): Result<T> {
        var currentDelay = initialDelayMs
        for (attempt in 0..maxRetries) {
            try {
                return Result.success(block())
            } catch (e: HttpException) {
                val code = e.code()
                val errorBody = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }

                if (code == 429) {
                    if (attempt < maxRetries) {
                        delay(currentDelay * 2)
                        currentDelay *= 2
                        continue
                    } else {
                        return Result.failure(Exception("Rate limit Tokocrypto (HTTP 429). Mohon tunggu beberapa detik sebelum mencoba kembali.", e))
                    }
                }

                val msg = if (!errorBody.isNullOrBlank()) {
                    "Exchange Error [HTTP $code]: $errorBody"
                } else {
                    "HTTP $code: ${e.message()}"
                }
                return Result.failure(Exception(msg, e))
            } catch (e: Exception) {
                if (attempt == maxRetries) {
                    return Result.failure(e)
                }
                delay(currentDelay)
                currentDelay *= 2
            }
        }
        return Result.failure(Exception("Gagal menghubungi exchange setelah $maxRetries kali percobaan"))
    }

    private fun formatValue(value: Double): String {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
    }
}

