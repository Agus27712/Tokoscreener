package com.tokoreader.presentation.components

import java.text.NumberFormat
import java.util.Locale

object CryptoUtils {

    fun splitSymbol(symbol: String): Pair<String, String> {
        return when {
            symbol.endsWith("IDR") -> Pair(symbol.removeSuffix("IDR"), "IDR")
            symbol.endsWith("BIDR") -> Pair(symbol.removeSuffix("BIDR"), "BIDR")
            symbol.endsWith("USDT") -> Pair(symbol.removeSuffix("USDT"), "USDT")
            symbol.endsWith("USDC") -> Pair(symbol.removeSuffix("USDC"), "USDC")
            symbol.endsWith("BUSD") -> Pair(symbol.removeSuffix("BUSD"), "BUSD")
            symbol.endsWith("BTC") && symbol.length > 3 -> Pair(symbol.removeSuffix("BTC"), "BTC")
            symbol.endsWith("ETH") && symbol.length > 3 -> Pair(symbol.removeSuffix("ETH"), "ETH")
            symbol.endsWith("BNB") && symbol.length > 3 -> Pair(symbol.removeSuffix("BNB"), "BNB")
            else -> Pair(symbol, "")
        }
    }

    /**
     * Price formatter following Tokocrypto standards:
     * - IDR / BIDR quote pairs get "Rp " prefix.
     * - USDT / USDC / BUSD quote pairs get "$ " prefix with dynamic precision.
     * - BTC quote pairs get "₿ " prefix.
     */
    fun formatCryptoPrice(symbol: String, price: Double): String {
        val isIdr = symbol.endsWith("IDR") || symbol.endsWith("BIDR")
        val isBtc = symbol.endsWith("BTC")
        val isUsd = symbol.endsWith("USDT") || symbol.endsWith("BUSD") || symbol.endsWith("USDC")

        val prefix = when {
            isIdr -> "Rp "
            isUsd -> "$ "
            isBtc -> "₿ "
            else -> ""
        }
        val locale = if (isIdr) Locale("id", "ID") else Locale.US
        val formatter = NumberFormat.getNumberInstance(locale)

        return when {
            isIdr -> {
                if (price >= 100.0) {
                    formatter.maximumFractionDigits = 0
                    formatter.minimumFractionDigits = 0
                    prefix + formatter.format(price.toLong())
                } else if (price >= 1.0) {
                    formatter.maximumFractionDigits = 2
                    formatter.minimumFractionDigits = 2
                    prefix + formatter.format(price)
                } else {
                    formatter.maximumFractionDigits = 4
                    formatter.minimumFractionDigits = 2
                    prefix + formatter.format(price)
                }
            }
            isBtc -> {
                String.format(Locale.US, "%s%.6f", prefix, price)
            }
            else -> { // USDT and others
                if (price >= 1000.0) {
                    formatter.maximumFractionDigits = 2
                    formatter.minimumFractionDigits = 2
                    prefix + formatter.format(price)
                } else if (price >= 1.0) {
                    formatter.maximumFractionDigits = 2
                    formatter.minimumFractionDigits = 2
                    prefix + formatter.format(price)
                } else if (price >= 0.01) {
                    formatter.maximumFractionDigits = 4
                    formatter.minimumFractionDigits = 2
                    prefix + formatter.format(price)
                } else {
                    formatter.maximumFractionDigits = 7
                    formatter.minimumFractionDigits = 4
                    prefix + formatter.format(price)
                }
            }
        }
    }

    /**
     * 24h Volume Formatter following standard abbreviation
     */
    fun formatCryptoVolume(symbol: String, volume: Double): String {
        val isIdr = symbol.endsWith("IDR") || symbol.endsWith("BIDR")
        val isBtc = symbol.endsWith("BTC")
        val prefix = when {
            isIdr -> "Rp "
            isBtc -> "₿ "
            else -> "$ "
        }
        val locale = if (isIdr) Locale("id", "ID") else Locale.US

        return if (isIdr) {
            when {
                volume >= 1_000_000_000_000.0 -> String.format(locale, "%s%.2f T", prefix, volume / 1_000_000_000_000.0)
                volume >= 1_000_000_000.0 -> String.format(locale, "%s%.1f M", prefix, volume / 1_000_000_000.0)
                volume >= 1_000_000.0 -> String.format(locale, "%s%.1f Jt", prefix, volume / 1_000_000.0)
                else -> prefix + NumberFormat.getNumberInstance(locale).format(volume.toLong())
            }
        } else if (isBtc) {
            when {
                volume >= 1000.0 -> String.format(Locale.US, "%s%.1fK", prefix, volume / 1000.0)
                else -> String.format(Locale.US, "%s%.2f", prefix, volume)
            }
        } else {
            when {
                volume >= 1_000_000_000.0 -> String.format(Locale.US, "%s%.2f B", prefix, volume / 1_000_000_000.0)
                volume >= 1_000_000.0 -> String.format(Locale.US, "%s%.1f M", prefix, volume / 1_000_000.0)
                volume >= 1_000.0 -> String.format(Locale.US, "%s%.1f K", prefix, volume / 1_000.0)
                else -> prefix + NumberFormat.getNumberInstance(Locale.US).format(volume.toLong())
            }
        }
    }

    fun getCoinName(baseSymbol: String): String {
        return when (baseSymbol.uppercase()) {
            "BTC" -> "Bitcoin"
            "ETH" -> "Ethereum"
            "SOL" -> "Solana"
            "XRP" -> "Ripple"
            "DOGE" -> "Dogecoin"
            "BNB" -> "BNB"
            "ADA" -> "Cardano"
            "TKO" -> "Tokocrypto"
            "USDT" -> "Tether USD"
            "USDC" -> "USD Coin"
            "AVAX" -> "Avalanche"
            "DOT" -> "Polkadot"
            "MATIC", "POL" -> "Polygon"
            "SHIB" -> "Shiba Inu"
            "PEPE" -> "Pepe"
            "NEAR" -> "NEAR Protocol"
            "SUI" -> "Sui"
            "ARB" -> "Arbitrum"
            "OP" -> "Optimism"
            "FIL" -> "Filecoin"
            "LINK" -> "Chainlink"
            "LTC" -> "Litecoin"
            "TRX" -> "TRON"
            else -> baseSymbol
        }
    }

    fun generateSparklinePoints(symbol: String, priceChangePercent: Double, steps: Int = 16): List<Float> {
        val seed = symbol.fold(0L) { acc, c -> acc * 31L + c.code.toLong() }
        val random = java.util.Random(seed)

        val waveFreq = 1.0f + (random.nextFloat() * 0.5f)
        val phase = random.nextFloat() * Math.PI.toFloat() * 2f
        val trend = (priceChangePercent / 100.0).toFloat().coerceIn(-0.40f, 0.40f)

        val points = mutableListOf<Float>()
        for (i in 0 until steps) {
            val t = i.toFloat() / (steps - 1).toFloat()
            val wave1 = kotlin.math.sin(t * Math.PI.toFloat() * waveFreq + phase) * 0.12f
            val wave2 = kotlin.math.sin(t * Math.PI.toFloat() * 2.2f + phase * 1.5f) * 0.04f
            val trendComponent = (t - 0.5f) * trend * 1.5f
            val y = 1.0f + trendComponent + wave1 + wave2
            points.add(y)
        }

        val firstVal = points.first()
        if (priceChangePercent > 0 && points.last() <= firstVal) {
            points[points.size - 1] = firstVal + (trend * 0.8f).coerceAtLeast(0.08f)
        } else if (priceChangePercent < 0 && points.last() >= firstVal) {
            points[points.size - 1] = firstVal + (trend * 0.8f).coerceAtMost(-0.08f)
        }
        return points
    }
}
