package com.tokoreader.data.remote.websocket

import com.tokoreader.data.local.logging.AppLogger
import okhttp3.*
import okio.ByteString

class TokocryptoMarketSocket(
    private val client: OkHttpClient,
    private val symbolType: Int = 1 // 1 for MBX, 3 for NextMe
) : WebSocketListener() {

    private var webSocket: WebSocket? = null
    
    // [VERIFIKASI] Pastikan base URL ini akurat dengan dokumentasi terbaru Tokocrypto
    private val baseUrl = if (symbolType == 1) {
        "wss://stream-cloud.tokocrypto.site/stream"
    } else {
        "wss://stream-toko.2meta.app"
    }

    fun connect() {
        AppLogger.i("MarketSocket", "Connecting to Market Stream: $baseUrl")
        val request = Request.Builder().url(baseUrl).build()
        webSocket = client.newWebSocket(request, this)
    }

    fun subscribe(streams: List<String>) {
        AppLogger.i("MarketSocket", "Subscribing to: $streams")
        val payload = """
            {
                "method": "SUBSCRIBE",
                "params": [
                    ${streams.joinToString(",") { "\"$it\"" }}
                ],
                "id": 1
            }
        """.trimIndent()
        webSocket?.send(payload)
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        AppLogger.i("MarketSocket", "Connected successfully to $baseUrl")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        // Parse message based on streams (kline, depth, miniTicker, trade)
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        AppLogger.w("MarketSocket", "Closed with code $code: $reason")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        AppLogger.e("MarketSocket", "Connection Failure: ${t.message}", t)
    }
}
