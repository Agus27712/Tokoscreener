package com.tokoreader.domain.indicator

object FibonacciCalculator {
    data class FibonacciLevels(
        val level0: Double, // 0.0%
        val level236: Double, // 23.6%
        val level382: Double, // 38.2%
        val level500: Double, // 50.0%
        val level618: Double, // 61.8%
        val level786: Double, // 78.6%
        val level1000: Double // 100.0%
    )

    fun calculateRetracement(swingHigh: Double, swingLow: Double): FibonacciLevels {
        val diff = swingHigh - swingLow
        return FibonacciLevels(
            level0 = swingHigh,
            level236 = swingHigh - (diff * 0.236),
            level382 = swingHigh - (diff * 0.382),
            level500 = swingHigh - (diff * 0.500),
            level618 = swingHigh - (diff * 0.618),
            level786 = swingHigh - (diff * 0.786),
            level1000 = swingLow
        )
    }
}
