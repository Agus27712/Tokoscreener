package com.tokoreader.domain.indicator

import com.tokoreader.domain.model.Kline
import kotlin.math.abs
import kotlin.math.max

object AtrCalculator {
    fun calculate(candles: List<Kline>, period: Int = 14): List<Double> {
        val result = MutableList(candles.size) { Double.NaN }
        if (candles.size < period) return result

        val trList = MutableList(candles.size) { 0.0 }
        
        // First TR doesn't have previous close
        trList[0] = candles[0].high - candles[0].low
        
        for (i in 1 until candles.size) {
            val highLow = candles[i].high - candles[i].low
            val highClose = abs(candles[i].high - candles[i - 1].close)
            val lowClose = abs(candles[i].low - candles[i - 1].close)
            trList[i] = max(highLow, max(highClose, lowClose))
        }

        var sumTr = 0.0
        for (i in 0 until period) {
            sumTr += trList[i]
        }
        var prevAtr = sumTr / period
        result[period - 1] = prevAtr

        for (i in period until candles.size) {
            val currentAtr = ((prevAtr * (period - 1)) + trList[i]) / period
            result[i] = currentAtr
            prevAtr = currentAtr
        }

        return result
    }
}
