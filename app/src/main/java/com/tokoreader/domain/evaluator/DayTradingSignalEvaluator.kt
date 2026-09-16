package com.tokoreader.domain.evaluator

import com.tokoreader.domain.indicator.AdxCalculator
import com.tokoreader.domain.indicator.AtrCalculator
import com.tokoreader.domain.indicator.EmaCalculator
import com.tokoreader.domain.indicator.MacdCalculator
import com.tokoreader.domain.indicator.RsiCalculator
import com.tokoreader.domain.indicator.VolumeCalculator
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
        val atr = AtrCalculator.calculate(candles, 14)
        val adx = AdxCalculator.calculate(candles, 14)

        val lastIndex = candles.size - 1
        val prevIndex = lastIndex - 1

        val currentEma20 = ema20[lastIndex]
        val currentEma50 = ema50[lastIndex]
        val prevMacdHist = macd[prevIndex].histogram
        val currentMacdHist = macd[lastIndex].histogram
        val currentRsi = rsi[lastIndex]
        val currentAtr = atr[lastIndex]
        val currentAdx = adx[lastIndex]
        val currentPrice = candles[lastIndex].close

        val isUptrend = currentPrice > currentEma20 && currentEma20 > currentEma50
        val isMacdBullishCross = prevMacdHist <= 0 && currentMacdHist > 0
        val isMacdBearishCross = prevMacdHist >= 0 && currentMacdHist < 0
        val isBreakdown = currentPrice < currentEma20
        
        // ADX Trend Strength Filter (> 20 means active trend, +DI > -DI means bullish)
        val isStrongTrend = currentAdx.adx >= 18.0 && currentAdx.plusDi > currentAdx.minusDi
        
        // Volume Surge Confirmation
        val isVolumeConfirmed = VolumeCalculator.isVolumeSurge(candles, period = 20, multiplier = 1.0)

        if (position == null || position.quantity <= 0.0) {
            // Context: Buy Signal
            if (isUptrend && isMacdBullishCross && currentRsi < 70 && (isStrongTrend || isVolumeConfirmed)) {
                return TradingSignal.ReadyToBuy("Uptrend + MACD Cross + ADX Bullish Strength", currentPrice)
            }
            return TradingSignal.MonitoringBuy
        } else {
            // Context: Sell Signal (Dynamic ATR SL/TP)
            val atrSl = position.averageEntryPrice - (currentAtr * 1.5)
            val atrTp = position.averageEntryPrice + (currentAtr * 3.0)

            if (currentPrice <= atrSl) {
                return TradingSignal.StopLossHit
            }
            if (currentPrice >= atrTp) {
                return TradingSignal.ReadyToSell("Dynamic ATR Take Profit (3x ATR) Hit", currentPrice)
            }

            if (isMacdBearishCross || isBreakdown || (currentRsi > 70 && rsi[prevIndex] > currentRsi)) {
                return TradingSignal.ReadyToSell("MACD Bearish / Breakdown / RSI Divergence", currentPrice)
            }
            
            return TradingSignal.MonitoringSell
        }
    }
}
