package com.tokoreader.domain.evaluator

import com.tokoreader.domain.indicator.AdxCalculator
import com.tokoreader.domain.indicator.AtrCalculator
import com.tokoreader.domain.indicator.EmaCalculator
import com.tokoreader.domain.indicator.FibonacciCalculator
import com.tokoreader.domain.indicator.RsiCalculator
import com.tokoreader.domain.indicator.VolumeCalculator
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
        val adx = AdxCalculator.calculate(candles, 14)

        val lastIndex = candles.size - 1
        
        val currentEma50 = ema50[lastIndex]
        val currentEma200 = ema200[lastIndex]
        val currentRsi = rsi[lastIndex]
        val currentAtr = atr[lastIndex]
        val currentAdx = adx[lastIndex]
        val currentPrice = candles[lastIndex].close

        // Swing High & Low from last 100 candles for Fibonacci Retracement
        val lookback = 100
        val startIndex = (candles.size - lookback).coerceAtLeast(0)
        var swingHigh = Double.MIN_VALUE
        var swingLow = Double.MAX_VALUE
        for (i in startIndex until candles.size) {
            val high = candles[i].high
            val low = candles[i].low
            if (high > swingHigh) swingHigh = high
            if (low < swingLow) swingLow = low
        }

        val fibLevels = FibonacciCalculator.calculateRetracement(swingHigh, swingLow)
        // Fib Support Zone is usually between 38.2% and 78.6% retracement (healthy correction in uptrend)
        val isFibSupported = currentPrice >= fibLevels.level786 && currentPrice <= fibLevels.level382

        val isUptrend = currentEma50 > currentEma200
        val isOversoldRebound = currentRsi > 30 && rsi[lastIndex - 1] <= 30
        
        // ADX Trend Filter
        val isAdxConfirmed = currentAdx.adx >= 20.0 && currentAdx.plusDi > currentAdx.minusDi
        
        // Volume Filter
        val isVolumeConfirmed = VolumeCalculator.isVolumeSurge(candles, period = 20, multiplier = 1.0)

        if (position == null || position.quantity <= 0.0) {
            // Context: Buy Signal
            if (isUptrend && isOversoldRebound && isFibSupported && (isAdxConfirmed || isVolumeConfirmed)) {
                return TradingSignal.ReadyToBuy("EMA 50/200 Uptrend + RSI Rebound + Fib Support + ADX/Vol Confirm", currentPrice)
            }
            return TradingSignal.MonitoringBuy
        } else {
            // Context: Sell Signal (Dynamic ATR SL/TP)
            val atrStopLoss = position.averageEntryPrice - (currentAtr * 2.0)
            val atrTakeProfit = position.averageEntryPrice + (currentAtr * 4.0)
            
            if (currentPrice <= atrStopLoss) {
                return TradingSignal.StopLossHit
            }

            if (currentPrice >= atrTakeProfit) {
                return TradingSignal.ReadyToSell("Dynamic ATR Swing TP (4x ATR) Hit", currentPrice)
            }

            if (currentPrice >= fibLevels.level236) {
                return TradingSignal.ReadyToSell("Fibonacci Target Profit Level Hit (>23.6%)", currentPrice)
            }

            if (currentEma50 < currentEma200 || currentRsi > 70) {
                return TradingSignal.ReadyToSell("Trend Reversal / RSI Overbought (>70)", currentPrice)
            }

            return TradingSignal.MonitoringSell
        }
    }
}
