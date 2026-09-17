package com.tokoreader.data.remote.rest

/**
 * Katalog resmi koin & pair perdagangan Tokocrypto (Spot IDR & USDT).
 * Berfungsi sebagai fallback cache instan dan perlindungan saat jaringan terkena geo-block (HTTP 451).
 */
object DefaultTokocryptoCatalog {

    val IDR_PAIRS = listOf(
        "BTCIDR", "ETHIDR", "SOLIDR", "XRPIDR", "DOGEIDR", "BNBIDR", "ADAIDR", "TKOIDR", "USDTIDR",
        "PEPEIDR", "SHIBIDR", "NEARIDR", "SUIIDR", "MATICIDR", "POLIDR", "AVAXIDR", "DOTIDR", "LTCIDR",
        "BCHIDR", "LINKIDR", "UNIDR", "TRXIDR", "FILIDR", "ATOMIDR", "ETCIDR", "ICPIDR", "ARBIDR",
        "OPIDR", "FTMIDR", "APTIDR", "RENDERIDR", "INJIDR", "FETIDR", "GRTIDR", "THETAIDR", "XLMIDR",
        "WIFIDR", "BONKIDR", "FLOKIIDR", "GALAIDR", "SANDIDR", "MANAIDR", "AXSIDR", "CHZIDR", "ENJIDR",
        "ZILIDR", "VETIDR", "EGLDIDR", "AAVEIDR", "MKRIDR", "CRVIDR", "SNXIDR", "DYDXIDR", "BLURIDR",
        "SEIIDR", "TIAIDR", "JUPIDR", "WIDR", "NOTIDR", "TONIDR", "ENAIDR", "ONDOIDR", "BBIDR", "IOIDR",
        "ZKIDR", "ZROIDR", "LISTAIDR", "BANANAIDR", "PIXELIDR", "STRKIDR", "PYTHIDR", "JTOIDR", "DYMIDR",
        "PORTALIDR", "AEVOIDR", "ETHFIIDR", "TAIKOIDR", "BOMEIDR", "MEMEIDR"
    )

    val USDT_PAIRS = listOf(
        "BTCUSDT", "ETHUSDT", "SOLUSDT", "XRPUSDT", "DOGEUSDT", "BNBUSDT", "ADAUSDT", "TKOUSDT",
        "PEPEUSDT", "SHIBUSDT", "NEARUSDT", "SUIUSDT", "MATICUSDT", "POLUSDT", "AVAXUSDT", "DOTUSDT",
        "LTCUSDT", "BCHUSDT", "LINKUSDT", "UNIUSDT", "TRXUSDT", "FILUSDT", "ATOMUSDT", "ETCUSDT",
        "ICPUSDT", "ARBUSDT", "OPUSDT", "FTMUSDT", "APTUSDT", "RENDERUSDT", "INJUSDT", "FETUSDT",
        "GRTUSDT", "THETAUSDT", "XLMUSDT", "WIFUSDT", "BONKUSDT", "FLOKIUSDT", "GALAUSDT", "SANDUSDT",
        "MANAUSDT", "AXSUSDT", "CHZUSDT", "ENJUSDT", "ZILUSDT", "VETUSDT", "EGLDUSDT", "AAVEUSDT",
        "MKRUSDT", "CRVUSDT", "SNXUSDT", "DYDXUSDT", "BLURUSDT", "SEIUSDT", "TIAUSDT", "JUPUSDT",
        "WUSDT", "NOTUSDT", "TONUSDT", "ENAUSDT", "ONDOUSDT", "BBUSDT", "IOUSDT", "ZKUSDT", "ZROUSDT",
        "LISTAUSDT", "BANANAUSDT", "PIXELUSDT", "STRKUSDT", "PYTHUSDT", "JTOUSDT", "DYMUSDT",
        "PORTALUSDT", "AEVOUSDT", "ETHFIUSDT", "TAIKOUSDT", "BOMEUSDT", "MEMEUSDT"
    )

    fun getDefaultSymbolInfoMap(): Map<String, SymbolInfo> {
        val map = mutableMapOf<String, SymbolInfo>()

        IDR_PAIRS.forEach { sym ->
            val base = sym.removeSuffix("IDR")
            map[sym] = SymbolInfo(
                symbol = sym,
                type = 3, // NextMe IDR Engine
                baseAsset = base,
                quoteAsset = "IDR",
                filters = listOf(
                    mapOf("filterType" to "PRICE_FILTER", "tickSize" to "1.0"),
                    mapOf("filterType" to "LOT_SIZE", "stepSize" to "0.00001", "minQty" to "0.00001"),
                    mapOf("filterType" to "MIN_NOTIONAL", "minNotional" to "10000.0")
                )
            )
        }

        USDT_PAIRS.forEach { sym ->
            val base = sym.removeSuffix("USDT")
            map[sym] = SymbolInfo(
                symbol = sym,
                type = 1, // MBX Engine
                baseAsset = base,
                quoteAsset = "USDT",
                filters = listOf(
                    mapOf("filterType" to "PRICE_FILTER", "tickSize" to "0.0001"),
                    mapOf("filterType" to "LOT_SIZE", "stepSize" to "0.0001", "minQty" to "0.0001"),
                    mapOf("filterType" to "MIN_NOTIONAL", "minNotional" to "5.0")
                )
            )
        }

        return map
    }
}
