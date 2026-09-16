package com.tokoreader.domain.usecase

import com.tokoreader.domain.repository.TradeRepository

class CancelOrderUseCase(
    private val tradeRepository: TradeRepository
) {
    suspend operator fun invoke(symbol: String, orderId: String): Result<Boolean> {
        return tradeRepository.cancelOrder(symbol, orderId)
    }
}
