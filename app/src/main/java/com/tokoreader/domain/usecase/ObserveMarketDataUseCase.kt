package com.tokoreader.domain.usecase

import com.tokoreader.domain.model.Kline
import com.tokoreader.domain.repository.MarketDataRepository
import kotlinx.coroutines.flow.Flow

class ObserveMarketDataUseCase(
    private val marketDataRepository: MarketDataRepository
) {
    fun observeCandles(symbol: String, timeframe: String): Flow<List<Kline>> {
        return marketDataRepository.observeClosedCandles(symbol, timeframe)
    }
}
