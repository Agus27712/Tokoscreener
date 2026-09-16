package com.tokoreader.domain.indicator

import com.tokoreader.domain.model.Kline

object VolumeCalculator {
    /**
     * Calculates Simple Moving Average of volume over specified period.
     */
    fun calculateSma(candles: List<Kline>, period: Int = 20): List<Double> {
        if (candles.isEmpty()) return emptyList()
        val result = MutableList(candles.size) { 0.0 }
        var sum = 0.0

        for (i in candles.indices) {
            sum += candles[i].volume
            if (i >= period) {
                sum -= candles[i - period].volume
                result[i] = sum / period
            } else if (i >= period - 1) {
                result[i] = sum / period
            } else {
                result[i] = sum / (i + 1)
            }
        }
        return result
    }

    /**
     * Checks if the latest candle's volume is higher than the Volume SMA by a multiplier.
     */
    fun isVolumeSurge(candles: List<Kline>, period: Int = 20, multiplier: Double = 1.0): Boolean {
        if (candles.size < period) return true // default allow if insufficient history
        val sma = calculateSma(candles, period)
        val lastVolume = candles.last().volume
        val avgVolume = sma.last()
        return lastVolume >= avgVolume * multiplier
    }
}
