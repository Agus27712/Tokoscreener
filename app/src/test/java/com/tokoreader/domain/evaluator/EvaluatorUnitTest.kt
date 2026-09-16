package com.tokoreader.domain.evaluator

import com.tokoreader.domain.indicator.AdxCalculator
import com.tokoreader.domain.indicator.VolumeCalculator
import com.tokoreader.domain.model.Kline
import com.tokoreader.domain.model.Position
import com.tokoreader.domain.model.TradingSignal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EvaluatorUnitTest {

    private fun generateMockCandles(count: Int, basePrice: Double = 50000.0, trend: Double = 10.0): List<Kline> {
        val list = mutableListOf<Kline>()
        var current = basePrice
        val now = System.currentTimeMillis()
        for (i in 0 until count) {
            current += trend + (if (i % 2 == 0) 5.0 else -3.0)
            list.add(
                Kline(
                    openTime = now - (count - i) * 60000L,
                    open = current - 5.0,
                    high = current + 15.0,
                    low = current - 15.0,
                    close = current,
                    volume = 100.0 + (i * 2.0),
                    closeTime = now - (count - i) * 60000L + 59999L,
                    quoteVolume = (100.0 + i) * current,
                    trades = 50,
                    takerBuyBaseVol = 50.0,
                    takerBuyQuoteVol = 50.0 * current,
                    isClosed = true
                )
            )
        }
        return list
    }

    @Test
    fun testAdxCalculator() {
        val candles = generateMockCandles(40, trend = 20.0)
        val adxResults = AdxCalculator.calculate(candles, 14)
        assertEquals(40, adxResults.size)
        assertTrue(adxResults.last().adx >= 0.0)
    }

    @Test
    fun testVolumeCalculator() {
        val candles = generateMockCandles(30)
        val isSurge = VolumeCalculator.isVolumeSurge(candles, period = 20, multiplier = 1.0)
        assertTrue(isSurge)
    }

    @Test
    fun testScalpingEvaluatorNotEnoughData() {
        val candles = generateMockCandles(10)
        val evaluator = ScalpingSignalEvaluator()
        val signal = evaluator.evaluate(candles, null)
        assertEquals(TradingSignal.NotHolding, signal)
    }

    @Test
    fun testDayTradingEvaluatorNotEnoughData() {
        val candles = generateMockCandles(30)
        val evaluator = DayTradingSignalEvaluator()
        val signal = evaluator.evaluate(candles, null)
        assertEquals(TradingSignal.NotHolding, signal)
    }

    @Test
    fun testSwingEvaluatorNotEnoughData() {
        val candles = generateMockCandles(150)
        val evaluator = SwingSignalEvaluator()
        val signal = evaluator.evaluate(candles, null)
        assertEquals(TradingSignal.NotHolding, signal)
    }

    @Test
    fun testSwingEvaluatorSufficientData() {
        val candles = generateMockCandles(220, trend = 5.0)
        val evaluator = SwingSignalEvaluator()
        val signal = evaluator.evaluate(candles, null)
        assertTrue(signal is TradingSignal.MonitoringBuy || signal is TradingSignal.ReadyToBuy)
    }

    @Test
    fun testScalpingStopLossHit() {
        val candles = generateMockCandles(30, basePrice = 50000.0)
        val position = Position(
            symbol = "BTCIDR",
            quantity = 0.01,
            averageEntryPrice = 60000.0 // Far above current price
        )
        val evaluator = ScalpingSignalEvaluator()
        val signal = evaluator.evaluate(candles, position)
        assertEquals(TradingSignal.StopLossHit, signal)
    }
}
