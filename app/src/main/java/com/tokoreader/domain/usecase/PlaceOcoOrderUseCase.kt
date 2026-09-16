package com.tokoreader.domain.usecase

import com.tokoreader.domain.model.OcoOrderRequest
import com.tokoreader.domain.model.OrderResult
import com.tokoreader.domain.repository.MarketDataRepository
import com.tokoreader.domain.repository.TradeRepository
import java.math.BigDecimal
import java.math.RoundingMode

class PlaceOcoOrderUseCase(
    private val tradeRepository: TradeRepository,
    private val marketDataRepository: MarketDataRepository
) {
    suspend operator fun invoke(request: OcoOrderRequest): Result<OrderResult> {
        val filter = marketDataRepository.getSymbolFilters(request.symbol)
            ?: return Result.failure(Exception("Symbol filters not found for ${request.symbol}"))

        val roundedQty = roundToStep(request.quantity, filter.stepSize)
        val roundedPrice = roundToStep(request.price, filter.tickSize)
        val roundedStopPrice = roundToStep(request.stopPrice, filter.tickSize)
        val roundedStopLimitPrice = request.stopLimitPrice?.let {
            roundToStep(it, filter.tickSize)
        }

        if (roundedQty < filter.minQty) {
            return Result.failure(Exception("Quantity is below minimum: ${filter.minQty}"))
        }

        // Validate notional on the limit leg
        val notional = roundedPrice * roundedQty
        if (notional < filter.minNotional) {
            return Result.failure(Exception("Order value is below minimum notional: ${filter.minNotional}"))
        }

        val validRequest = request.copy(
            quantity = roundedQty,
            price = roundedPrice,
            stopPrice = roundedStopPrice,
            stopLimitPrice = roundedStopLimitPrice
        )

        return tradeRepository.placeOcoOrder(validRequest)
    }

    private fun roundToStep(value: Double, step: Double): Double {
        if (step <= 0.0) return value
        val bdValue = BigDecimal(value.toString())
        val bdStep = BigDecimal(step.toString())
        val divided = bdValue.divide(bdStep, 0, RoundingMode.DOWN)
        return divided.multiply(bdStep).toDouble()
    }
}
