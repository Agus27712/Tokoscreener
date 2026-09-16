package com.tokoreader.domain.indicator

import com.tokoreader.domain.model.Kline

object MacdCalculator {
    data class MacdResult(val macd: Double, val signal: Double, val histogram: Double)

    fun calculate(candles: List<Kline>, fastPeriod: Int = 12, slowPeriod: Int = 26, signalPeriod: Int = 9): List<MacdResult> {
        val result = MutableList(candles.size) { MacdResult(Double.NaN, Double.NaN, Double.NaN) }
        if (candles.size < slowPeriod + signalPeriod) return result

        val emaFast = EmaCalculator.calculate(candles, fastPeriod)
        val emaSlow = EmaCalculator.calculate(candles, slowPeriod)

        val macdLine = MutableList(candles.size) { Double.NaN }
        for (i in slowPeriod - 1 until candles.size) {
            macdLine[i] = emaFast[i] - emaSlow[i]
        }

        // Calculate Signal line (EMA of MACD)
        val signalLine = calculateEmaForList(macdLine, signalPeriod, slowPeriod - 1)

        for (i in (slowPeriod + signalPeriod - 2) until candles.size) {
            val macd = macdLine[i]
            val signal = signalLine[i]
            val histogram = macd - signal
            result[i] = MacdResult(macd, signal, histogram)
        }

        return result
    }

    private fun calculateEmaForList(values: List<Double>, period: Int, startIndex: Int): List<Double> {
        val ema = MutableList(values.size) { Double.NaN }
        if (values.size < startIndex + period) return ema

        var sum = 0.0
        for (i in startIndex until startIndex + period) {
            sum += values[i]
        }
        var prevEma = sum / period
        ema[startIndex + period - 1] = prevEma

        val multiplier = 2.0 / (period + 1)
        for (i in startIndex + period until values.size) {
            val currentEma = (values[i] - prevEma) * multiplier + prevEma
            ema[i] = currentEma
            prevEma = currentEma
        }
        return ema
    }
}
