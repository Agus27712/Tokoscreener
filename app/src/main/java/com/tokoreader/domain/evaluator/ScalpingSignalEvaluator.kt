package com.tokoreader.domain.evaluator

import com.tokoreader.domain.indicator.EmaCalculator
import com.tokoreader.domain.indicator.RsiCalculator
import com.tokoreader.domain.model.Kline
import com.tokoreader.domain.model.Position
import com.tokoreader.domain.model.TradingSignal

interface TradingSignalEvaluator {
    fun evaluate(candles: List<Kline>, position: Position?): TradingSignal
}

class ScalpingSignalEvaluator : TradingSignalEvaluator {
    override fun evaluate(candles: List<Kline>, position: Position?): TradingSignal {
        if (candles.size < 21) return TradingSignal.NotHolding // Not enough data

        val emaFast = EmaCalculator.calculate(candles, 9)
        val emaSlow = EmaCalculator.calculate(candles, 21)
        val rsi = RsiCalculator.calculate(candles, 9)

        val lastIndex = candles.size - 1
        val prevIndex = lastIndex - 1

        val currentEmaFast = emaFast[lastIndex]
        val currentEmaSlow = emaSlow[lastIndex]
        val prevEmaFast = emaFast[prevIndex]
        val prevEmaSlow = emaSlow[prevIndex]
        val currentRsi = rsi[lastIndex]
        
        val currentPrice = candles[lastIndex].close

        // Cross Up
        val isGoldenCross = prevEmaFast <= prevEmaSlow && currentEmaFast > currentEmaSlow
        // Cross Down
        val isDeathCross = prevEmaFast >= prevEmaSlow && currentEmaFast < currentEmaSlow

        if (position == null || position.quantity <= 0.0) {
            // Context: Buy
            if (isGoldenCross && currentRsi < 40) {
                return TradingSignal.ReadyToBuy("EMA Golden Cross + RSI < 40", currentPrice)
            }
            return TradingSignal.MonitoringBuy
        } else {
            // Context: Sell
            val profitPct = (currentPrice - position.averageEntryPrice) / position.averageEntryPrice
            
            // Hardcoded tight stop loss and take profit for scalping
            if (profitPct <= -0.005) {
                return TradingSignal.StopLossHit
            }
            if (profitPct >= 0.01) {
                return TradingSignal.ReadyToSell("TP 1% Hit", currentPrice)
            }
            
            if (isDeathCross || (currentRsi > 70)) {
                return TradingSignal.ReadyToSell("EMA Death Cross / RSI Overbought", currentPrice)
            }
            
            return TradingSignal.MonitoringSell
        }
    }
}
