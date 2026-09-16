package com.tokoreader.data.remote.websocket

import android.util.Log
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
        val request = Request.Builder().url(baseUrl).build()
        webSocket = client.newWebSocket(request, this)
    }

    fun subscribe(streams: List<String>) {
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
        Log.d("MarketSocket", "Connected to $baseUrl")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        // Parse message based on streams (kline, depth, miniTicker, trade)
        // Log.d("MarketSocket", "Message: $text")
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.d("MarketSocket", "Closed: $reason")
        // Trigger reconnect logic here
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.e("MarketSocket", "Error: ${t.message}")
        // Trigger reconnect logic here with backoff
    }
}
