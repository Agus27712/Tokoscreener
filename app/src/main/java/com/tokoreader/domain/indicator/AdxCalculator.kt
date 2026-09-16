package com.tokoreader.domain.indicator

import com.tokoreader.domain.model.Kline
import kotlin.math.abs
import kotlin.math.max

data class AdxResult(
    val adx: Double,
    val plusDi: Double,
    val minusDi: Double
)

object AdxCalculator {
    /**
     * Calculates ADX (Average Directional Index), +DI, and -DI over specified period (default 14).
     * Returns a list of AdxResult of equal size as input candles.
     */
    fun calculate(candles: List<Kline>, period: Int = 14): List<AdxResult> {
        if (candles.isEmpty()) return emptyList()
        if (candles.size < period * 2) {
            return List(candles.size) { AdxResult(0.0, 0.0, 0.0) }
        }

        val trList = MutableList(candles.size) { 0.0 }
        val plusDmList = MutableList(candles.size) { 0.0 }
        val minusDmList = MutableList(candles.size) { 0.0 }

        for (i in 1 until candles.size) {
            val high = candles[i].high
            val low = candles[i].low
            val prevHigh = candles[i - 1].high
            val prevLow = candles[i - 1].low
            val prevClose = candles[i - 1].close

            val tr = max(high - low, max(abs(high - prevClose), abs(low - prevClose)))
            trList[i] = tr

            val upMove = high - prevHigh
            val downMove = prevLow - low

            if (upMove > downMove && upMove > 0) {
                plusDmList[i] = upMove
            } else {
                plusDmList[i] = 0.0
            }

            if (downMove > upMove && downMove > 0) {
                minusDmList[i] = downMove
            } else {
                minusDmList[i] = 0.0
            }
        }

        // Wilder's Smoothing for initial period
        var smoothedTr = trList.subList(1, period + 1).sum()
        var smoothedPlusDm = plusDmList.subList(1, period + 1).sum()
        var smoothedMinusDm = minusDmList.subList(1, period + 1).sum()

        val results = MutableList(candles.size) { AdxResult(0.0, 0.0, 0.0) }

        val dxList = MutableList(candles.size) { 0.0 }

        for (i in period until candles.size) {
            if (i > period) {
                smoothedTr = smoothedTr - (smoothedTr / period) + trList[i]
                smoothedPlusDm = smoothedPlusDm - (smoothedPlusDm / period) + plusDmList[i]
                smoothedMinusDm = smoothedMinusDm - (smoothedMinusDm / period) + minusDmList[i]
            }

            val plusDi = if (smoothedTr != 0.0) (smoothedPlusDm / smoothedTr) * 100.0 else 0.0
            val minusDi = if (smoothedTr != 0.0) (smoothedMinusDm / smoothedTr) * 100.0 else 0.0

            val diSum = plusDi + minusDi
            val dx = if (diSum != 0.0) (abs(plusDi - minusDi) / diSum) * 100.0 else 0.0

            dxList[i] = dx
            results[i] = AdxResult(0.0, plusDi, minusDi)
        }

        // Smooth DX to get ADX
        if (candles.size >= period * 2) {
            var adx = dxList.subList(period, period * 2).sum() / period
            results[period * 2 - 1] = results[period * 2 - 1].copy(adx = adx)

            for (i in (period * 2) until candles.size) {
                adx = (adx * (period - 1) + dxList[i]) / period
                results[i] = results[i].copy(adx = adx)
            }
        }

        return results
    }
}
