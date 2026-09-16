package com.tokoreader.domain.usecase

import com.tokoreader.domain.model.OrderRequest
import com.tokoreader.domain.model.OrderResult
import com.tokoreader.domain.repository.MarketDataRepository
import com.tokoreader.domain.repository.TradeRepository
import java.math.BigDecimal
import java.math.RoundingMode

class PlaceOrderUseCase(
    private val tradeRepository: TradeRepository,
    private val marketDataRepository: MarketDataRepository
) {
    suspend operator fun invoke(request: OrderRequest): Result<OrderResult> {
        val filter = marketDataRepository.getSymbolFilters(request.symbol)
            ?: return Result.failure(Exception("Symbol filters not found for ${request.symbol}"))

        // Round price according to tickSize
        val roundedPrice = request.price?.let {
            roundToStep(it, filter.tickSize)
        }

        // Round quantity according to stepSize
        val roundedQty = request.quantity?.let {
            roundToStep(it, filter.stepSize)
        }

        // Round stopPrice according to tickSize
        val roundedStopPrice = request.stopPrice?.let {
            roundToStep(it, filter.tickSize)
        }

        // Validate minimums
        if (roundedQty != null && roundedQty < filter.minQty) {
            return Result.failure(Exception("Quantity is below minimum: ${filter.minQty}"))
        }

        // Validate minNotional if price and qty exist
        if (roundedPrice != null && roundedQty != null) {
            val notional = roundedPrice * roundedQty
            if (notional < filter.minNotional) {
                return Result.failure(Exception("Order value is below minimum notional: ${filter.minNotional}"))
            }
        }

        val validRequest = request.copy(
            price = roundedPrice,
            quantity = roundedQty,
            stopPrice = roundedStopPrice
        )

        return tradeRepository.placeOrder(validRequest)
    }

    private fun roundToStep(value: Double, step: Double): Double {
        if (step <= 0.0) return value
        val bdValue = BigDecimal(value.toString())
        val bdStep = BigDecimal(step.toString())
        val divided = bdValue.divide(bdStep, 0, RoundingMode.DOWN)
        return divided.multiply(bdStep).toDouble()
    }
}
