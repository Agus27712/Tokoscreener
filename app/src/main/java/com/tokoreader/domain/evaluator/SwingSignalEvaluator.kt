package com.tokoreader.domain.evaluator

import com.tokoreader.domain.indicator.AtrCalculator
import com.tokoreader.domain.indicator.EmaCalculator
import com.tokoreader.domain.indicator.MacdCalculator
import com.tokoreader.domain.indicator.RsiCalculator
import com.tokoreader.domain.model.Kline
import com.tokoreader.domain.model.Position
import com.tokoreader.domain.model.TradingSignal

class SwingSignalEvaluator : TradingSignalEvaluator {
    override fun evaluate(candles: List<Kline>, position: Position?): TradingSignal {
        if (candles.size < 200) return TradingSignal.NotHolding // Need 200 for EMA200

        val ema50 = EmaCalculator.calculate(candles, 50)
        val ema200 = EmaCalculator.calculate(candles, 200)
        val rsi = RsiCalculator.calculate(candles, 14)
        val atr = AtrCalculator.calculate(candles, 14)

        val lastIndex = candles.size - 1
        
        val currentEma50 = ema50[lastIndex]
        val currentEma200 = ema200[lastIndex]
        val currentRsi = rsi[lastIndex]
        val currentAtr = atr[lastIndex]
        val currentPrice = candles[lastIndex].close

        val isUptrend = currentEma50 > currentEma200
        val isOversoldRebound = currentRsi > 30 && rsi[lastIndex - 1] <= 30

        if (position == null || position.quantity <= 0.0) {
            // Context: Buy
            if (isUptrend && isOversoldRebound) {
                return TradingSignal.ReadyToBuy("Uptrend + RSI Oversold Rebound", currentPrice)
            }
            return TradingSignal.MonitoringBuy
        } else {
            // Context: Sell
            // Use ATR for stop loss trailing roughly
            val atrStopLoss = position.averageEntryPrice - (currentAtr * 2.0)
            
            if (currentPrice <= atrStopLoss) {
                return TradingSignal.StopLossHit
            }

            val profitPct = (currentPrice - position.averageEntryPrice) / position.averageEntryPrice
            if (profitPct >= 0.05) { // 5% Take Profit for Swing
                return TradingSignal.ReadyToSell("TP 5% Hit", currentPrice)
            }

            if (currentEma50 < currentEma200 || currentRsi > 70) {
                return TradingSignal.ReadyToSell("Trend Reversal / RSI Overbought", currentPrice)
            }

            return TradingSignal.MonitoringSell
        }
    }
}
