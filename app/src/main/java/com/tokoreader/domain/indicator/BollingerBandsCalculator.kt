package com.tokoreader.domain.indicator

import com.tokoreader.domain.model.Kline
import kotlin.math.sqrt

object BollingerBandsCalculator {
    data class BollingerBandsResult(val upper: Double, val middle: Double, val lower: Double)

    fun calculate(candles: List<Kline>, period: Int = 20, multiplier: Double = 2.0): List<BollingerBandsResult> {
        val result = MutableList(candles.size) { BollingerBandsResult(Double.NaN, Double.NaN, Double.NaN) }
        if (candles.size < period) return result

        for (i in period - 1 until candles.size) {
            var sum = 0.0
            for (j in i downTo i - period + 1) {
                sum += candles[j].close
            }
            val sma = sum / period

            var varianceSum = 0.0
            for (j in i downTo i - period + 1) {
                val diff = candles[j].close - sma
                varianceSum += diff * diff
            }
            val stdDev = sqrt(varianceSum / period)

            val upper = sma + (multiplier * stdDev)
            val lower = sma - (multiplier * stdDev)
            
            result[i] = BollingerBandsResult(upper, sma, lower)
        }

        return result
    }
}
