package com.tokoreader.domain.evaluator

import com.tokoreader.domain.indicator.AtrCalculator
import com.tokoreader.domain.indicator.BollingerBandsCalculator
import com.tokoreader.domain.indicator.EmaCalculator
import com.tokoreader.domain.indicator.RsiCalculator
import com.tokoreader.domain.indicator.VolumeCalculator
import com.tokoreader.domain.model.Kline
import com.tokoreader.domain.model.Position
import com.tokoreader.domain.model.TradingSignal

interface TradingSignalEvaluator {
    fun evaluate(candles: List<Kline>, position: Position?): TradingSignal
}

class ScalpingSignalEvaluator : TradingSignalEvaluator {
    override fun evaluate(candles: List<Kline>, position: Position?): TradingSignal {
        if (candles.size < 21) return TradingSignal.NotHolding // Need at least 21 candles

        val emaFast = EmaCalculator.calculate(candles, 9)
        val emaSlow = EmaCalculator.calculate(candles, 21)
        val rsi = RsiCalculator.calculate(candles, 9)
        val atr = AtrCalculator.calculate(candles, 14)
        val bb = BollingerBandsCalculator.calculate(candles, 20, 2.0)

        val lastIndex = candles.size - 1
        val prevIndex = lastIndex - 1

        val currentEmaFast = emaFast[lastIndex]
        val currentEmaSlow = emaSlow[lastIndex]
        val prevEmaFast = emaFast[prevIndex]
        val prevEmaSlow = emaSlow[prevIndex]
        val currentRsi = rsi[lastIndex]
        val currentAtr = atr[lastIndex]
        val currentBb = bb[lastIndex]
        
        val currentPrice = candles[lastIndex].close

        // Cross Up & Cross Down
        val isGoldenCross = prevEmaFast <= prevEmaSlow && currentEmaFast > currentEmaSlow
        val isDeathCross = prevEmaFast >= prevEmaSlow && currentEmaFast < currentEmaSlow

        // Volume Filter
        val isVolumeConfirmed = VolumeCalculator.isVolumeSurge(candles, period = 10, multiplier = 1.0)
        
        // Bollinger Bands Filter
        val isNearLowerBb = currentPrice <= currentBb.lower * 1.025

        if (position == null || position.quantity <= 0.0) {
            // Context: Buy Signal
            if (isGoldenCross && currentRsi < 45 && isVolumeConfirmed && isNearLowerBb) {
                return TradingSignal.ReadyToBuy("EMA 9/21 Golden Cross + RSI < 45 + Vol Confirm + Near Lower BB", currentPrice)
            }
            return TradingSignal.MonitoringBuy
        } else {
            // Context: Sell Signal (Dynamic ATR Stop Loss & Take Profit)
            val atrSl = position.averageEntryPrice - (currentAtr * 1.0)
            val atrTp = position.averageEntryPrice + (currentAtr * 1.5)

            if (currentPrice <= atrSl) {
                return TradingSignal.StopLossHit
            }
            if (currentPrice >= atrTp) {
                return TradingSignal.ReadyToSell("Dynamic ATR Take Profit (1.5x ATR) Hit", currentPrice)
            }
            
            if (isDeathCross || (currentRsi > 70) || (currentPrice >= currentBb.upper)) {
                return TradingSignal.ReadyToSell("EMA Death Cross / RSI Overbought (>70) / Upper BB Hit", currentPrice)
            }
            
            return TradingSignal.MonitoringSell
        }
    }
}
