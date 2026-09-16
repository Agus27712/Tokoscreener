package com.tokoreader.domain.usecase

import com.tokoreader.domain.evaluator.TradingSignalEvaluator
import com.tokoreader.domain.model.TradingMode
import com.tokoreader.domain.model.TradingSignal
import com.tokoreader.domain.repository.MarketDataRepository
import com.tokoreader.domain.repository.PositionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveTradingSignalUseCase(
    private val positionRepository: PositionRepository,
    private val marketDataRepository: MarketDataRepository,
    private val evaluators: Map<TradingMode, TradingSignalEvaluator>
) {
    operator fun invoke(symbol: String, mode: TradingMode): Flow<TradingSignal> =
        combine(
            positionRepository.observePosition(symbol),
            marketDataRepository.observeClosedCandles(symbol, mode.timeframe)
        ) { position, candles ->
            evaluators[mode]?.evaluate(candles, position) ?: TradingSignal.NotHolding
        }
}
