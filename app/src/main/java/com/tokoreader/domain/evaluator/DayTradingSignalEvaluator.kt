package com.tokoreader.domain.evaluator

import com.tokoreader.domain.indicator.EmaCalculator
import com.tokoreader.domain.indicator.MacdCalculator
import com.tokoreader.domain.indicator.RsiCalculator
import com.tokoreader.domain.model.Kline
import com.tokoreader.domain.model.Position
import com.tokoreader.domain.model.TradingSignal

class DayTradingSignalEvaluator : TradingSignalEvaluator {
    override fun evaluate(candles: List<Kline>, position: Position?): TradingSignal {
        if (candles.size < 50) return TradingSignal.NotHolding

        val ema20 = EmaCalculator.calculate(candles, 20)
        val ema50 = EmaCalculator.calculate(candles, 50)
        val macd = MacdCalculator.calculate(candles)
        val rsi = RsiCalculator.calculate(candles, 14)

        val lastIndex = candles.size - 1
        val prevIndex = lastIndex - 1

        val currentEma20 = ema20[lastIndex]
        val currentEma50 = ema50[lastIndex]
        val prevMacdHist = macd[prevIndex].histogram
        val currentMacdHist = macd[lastIndex].histogram
        val currentRsi = rsi[lastIndex]
        val currentPrice = candles[lastIndex].close

        val isUptrend = currentPrice > currentEma20 && currentEma20 > currentEma50
        val isMacdBullishCross = prevMacdHist <= 0 && currentMacdHist > 0
        val isMacdBearishCross = prevMacdHist >= 0 && currentMacdHist < 0
        val isBreakdown = currentPrice < currentEma20

        if (position == null || position.quantity <= 0.0) {
            // Context: Buy
            if (isUptrend && isMacdBullishCross && currentRsi < 70) {
                return TradingSignal.ReadyToBuy("Uptrend + MACD Bullish Cross", currentPrice)
            }
            return TradingSignal.MonitoringBuy
        } else {
            // Context: Sell
            val profitPct = (currentPrice - position.averageEntryPrice) / position.averageEntryPrice
            
            if (profitPct <= -0.02) { // 2% Stop Loss
                return TradingSignal.StopLossHit
            }
            if (profitPct >= 0.03) { // 3% Take Profit
                return TradingSignal.ReadyToSell("TP 3% Hit", currentPrice)
            }

            if (isMacdBearishCross || isBreakdown || (currentRsi > 70 && rsi[prevIndex] > currentRsi)) {
                return TradingSignal.ReadyToSell("MACD Bearish / Breakdown / RSI Divergence", currentPrice)
            }
            
            return TradingSignal.MonitoringSell
        }
    }
}
