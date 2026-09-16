package com.tokoreader.domain.indicator

import com.tokoreader.domain.model.Kline

object RsiCalculator {
    fun calculate(candles: List<Kline>, period: Int = 14): List<Double> {
        val rsiList = MutableList(candles.size) { Double.NaN }
        if (candles.size <= period) return rsiList

        var avgGain = 0.0
        var avgLoss = 0.0

        // Initial averages
        for (i in 1..period) {
            val change = candles[i].close - candles[i - 1].close
            if (change > 0) {
                avgGain += change
            } else {
                avgLoss += Math.abs(change)
            }
        }

        avgGain /= period
        avgLoss /= period

        var rs = if (avgLoss == 0.0) 0.0 else avgGain / avgLoss
        rsiList[period] = if (avgLoss == 0.0) 100.0 else 100.0 - (100.0 / (1.0 + rs))

        // Smoothed moving average
        for (i in (period + 1) until candles.size) {
            val change = candles[i].close - candles[i - 1].close
            val gain = if (change > 0) change else 0.0
            val loss = if (change < 0) Math.abs(change) else 0.0

            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period

            rs = if (avgLoss == 0.0) 0.0 else avgGain / avgLoss
            rsiList[i] = if (avgLoss == 0.0) 100.0 else 100.0 - (100.0 / (1.0 + rs))
        }

        return rsiList
    }
}
