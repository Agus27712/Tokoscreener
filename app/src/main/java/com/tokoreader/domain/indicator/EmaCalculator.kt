package com.tokoreader.domain.indicator

import com.tokoreader.domain.model.Kline

object EmaCalculator {
    fun calculate(candles: List<Kline>, period: Int): List<Double> {
        if (candles.size < period) return emptyList()
        
        val ema = mutableListOf<Double>()
        var sum = 0.0
        
        // Initial SMA for first EMA
        for (i in 0 until period) {
            sum += candles[i].close
        }
        var prevEma = sum / period
        ema.add(prevEma)
        
        val multiplier = 2.0 / (period + 1)
        
        for (i in period until candles.size) {
            val currentEma = (candles[i].close - prevEma) * multiplier + prevEma
            ema.add(currentEma)
            prevEma = currentEma
        }
        
        // Pad the beginning with NaN or just return list of same size
        val result = MutableList(period - 1) { Double.NaN }
        result.addAll(ema)
        return result
    }
}
