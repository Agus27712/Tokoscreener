package com.tokoreader.data.remote.websocket

import com.tokoreader.data.local.logging.AppLogger
import com.tokoreader.data.remote.rest.TokocryptoTradeApi
import com.tokoreader.domain.model.UserDataEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.*
import org.json.JSONObject

class TokocryptoUserDataSocket(
    private val tradeApi: TokocryptoTradeApi,
    private val client: OkHttpClient
) {
    private val TAG = "UserDataSocket"

    fun connect(symbolType: Int): Flow<UserDataEvent> = callbackFlow {
        var activeWebSocket: WebSocket? = null
        var keepAliveJob: Job? = null
        var listenKey: String? = null
        var isClosed = false

        var connectWebSocket: (suspend () -> Unit)? = null
        var triggerReconnectFailure: (() -> Unit)? = null

        connectWebSocket = {
            try {
                val response = tradeApi.createListenKey()
                if (response.code == 0 && !response.data.isNullOrBlank()) {
                    val currentListenKey = response.data
                    listenKey = currentListenKey
                    val wsUrl = if (symbolType == 1) {
                        "wss://stream-cloud.tokocrypto.site/stream?streams=$currentListenKey"
                    } else {
                        "wss://stream-toko.2meta.app?streams=$currentListenKey"
                    }

                    AppLogger.i(TAG, "Membuka User Data Stream WebSocket ke: $wsUrl")
                    val request = Request.Builder().url(wsUrl).build()

                    activeWebSocket = client.newWebSocket(request, object : WebSocketListener() {
                        override fun onOpen(webSocket: WebSocket, response: Response) {
                            AppLogger.i(TAG, "User Data Stream WebSocket Berhasil Terbuka.")
                            
                            // Start keepAlive loop
                            keepAliveJob?.cancel()
                            keepAliveJob = launch {
                                while (isActive) {
                                    delay(30 * 60 * 1000L) // 30 minutes
                                    try {
                                        AppLogger.d(TAG, "Mengirim keepAlive untuk listenKey...")
                                        tradeApi.keepAliveListenKey(currentListenKey)
                                    } catch (e: Exception) {
                                        AppLogger.w(TAG, "Gagal mengirim keepAlive listenKey: ${e.message}")
                                    }
                                }
                            }
                        }

                        override fun onMessage(webSocket: WebSocket, text: String) {
                            try {
                                val json = JSONObject(text)
                                val eventType = json.optString("e")
                                if (eventType == "outboundAccountPosition") {
                                    val balancesArray = json.optJSONArray("B")
                                    if (balancesArray != null) {
                                        val assetsMap = mutableMapOf<String, Double>()
                                        for (i in 0 until balancesArray.length()) {
                                            val b = balancesArray.getJSONObject(i)
                                            val asset = b.optString("a")
                                            val free = b.optString("f").toDoubleOrNull() ?: 0.0
                                            assetsMap[asset] = free
                                        }
                                        trySend(UserDataEvent.BalanceUpdate(assetsMap))
                                    }
                                } else if (eventType == "executionReport") {
                                    val symbol = json.optString("s")
                                    val side = json.optString("S")
                                    val orderStatus = json.optString("X")
                                    val qty = json.optString("q").toDoubleOrNull() ?: 0.0
                                    val price = json.optString("p").toDoubleOrNull() ?: 0.0

                                    if (orderStatus == "FILLED") {
                                        trySend(UserDataEvent.OrderFilled(symbol, side, qty, price))
                                    }
                                }
                            } catch (e: Exception) {
                                AppLogger.w(TAG, "Gagal parsing User Data Stream message: ${e.message}")
                            }
                        }

                        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                            AppLogger.w(TAG, "User Data Stream ditutup: $code / $reason")
                            triggerReconnect()
                        }

                        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                            AppLogger.e(TAG, "User Data Stream WS error: ${t.message}", t)
                            triggerReconnect()
                        }

                        private fun triggerReconnect() {
                            keepAliveJob?.cancel()
                            activeWebSocket = null
                            if (!isClosed) {
                                launch {
                                    delay(5000)
                                    AppLogger.i(TAG, "Mencoba menghubungkan ulang User Data Stream...")
                                    connectWebSocket?.invoke()
                                }
                            }
                        }
                    })
                } else {
                    AppLogger.w(TAG, "Gagal membuat listenKey (code=${response.code})")
                    triggerReconnectFailure?.invoke()
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Gagal menginisialisasi User Data Stream: ${e.message}", e)
                triggerReconnectFailure?.invoke()
            }
        }

        triggerReconnectFailure = {
            if (!isClosed) {
                launch {
                    delay(5000)
                    connectWebSocket?.invoke()
                }
            }
        }

        launch {
            connectWebSocket?.invoke()
        }

        awaitClose {
            isClosed = true
            keepAliveJob?.cancel()
            activeWebSocket?.close(1000, "Closed by flow consumer")
            activeWebSocket = null
            listenKey?.let { lKey ->
                launch(Dispatchers.IO) {
                    try {
                        AppLogger.i(TAG, "Menutup listenKey di server...")
                        tradeApi.closeListenKey(lKey)
                    } catch (_: Exception) {}
                }
            }
        }
    }
}
