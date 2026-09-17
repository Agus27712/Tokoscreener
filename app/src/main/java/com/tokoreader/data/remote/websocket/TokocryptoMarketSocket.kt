package com.tokoreader.data.remote.websocket

import com.tokoreader.data.local.logging.AppLogger
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class TokocryptoMarketSocket(
    private val client: OkHttpClient,
    private val wsCandidateUrls: List<String>
) {
    private val TAG = "MultiplexedWS"
    private var activeWebSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val streamSubscribers = ConcurrentHashMap<String, Int>()
    private val streamListeners = ConcurrentHashMap<String, MutableSet<(JSONObject) -> Unit>>()

    private var currentUrlIndex = 0
    private var isConnecting = false
    private val isClosed = AtomicBoolean(false)

    // Robust reconnection variables
    private var retryAttempt = 0
    private val maxRetryDelayMs = 30000L

    @Synchronized
    fun ensureConnected() {
        if (isClosed.get() || isConnecting || activeWebSocket != null) return
        isConnecting = true
        
        val baseUrl = wsCandidateUrls[currentUrlIndex]
        val request = Request.Builder().url(baseUrl).build()
        AppLogger.i(TAG, "Menghubungkan ke Multiplexed WebSocket: $baseUrl")

        activeWebSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnecting = false
                retryAttempt = 0 // Reset retry count upon successful connection
                AppLogger.i(TAG, "Multiplexed WebSocket Tersambung: $baseUrl")
                
                // Re-subscribe to all active streams
                val activeStreams = streamSubscribers.keys().toList()
                if (activeStreams.isNotEmpty()) {
                    sendSubscription("SUBSCRIBE", activeStreams)
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val streamName = json.optString("stream")
                    val data = if (streamName.isNotEmpty()) json.optJSONObject("data") else json
                    
                    if (data != null) {
                        val targetStream = if (streamName.isNotEmpty()) streamName else {
                            val eventType = json.optString("e")
                            val s = json.optString("s").lowercase()
                            when (eventType) {
                                "24hrTicker", "ticker" -> "${s}@ticker"
                                "kline" -> {
                                    val k = json.optJSONObject("k")
                                    val interval = k?.optString("i") ?: "1m"
                                    "${s}@kline_${interval}"
                                }
                                else -> ""
                            }
                        }
                        
                        if (targetStream.isNotEmpty()) {
                            streamListeners[targetStream]?.forEach { listener ->
                                listener(data)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Fail silently
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnecting = false
                activeWebSocket = null
                
                val delayMs = (2000L * (1 shl retryAttempt)).coerceAtMost(maxRetryDelayMs)
                retryAttempt++

                AppLogger.w(TAG, "Koneksi Multiplexed WebSocket gagal (${t.message}). Mencoba ulang dalam ${delayMs / 1000} detik...")
                
                if (!isClosed.get()) {
                    scope.launch {
                        delay(delayMs)
                        currentUrlIndex = (currentUrlIndex + 1) % wsCandidateUrls.size
                        ensureConnected()
                    }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                activeWebSocket = null
            }
        })
    }

    private fun sendSubscription(method: String, streams: List<String>) {
        val ws = activeWebSocket ?: return
        if (streams.isEmpty()) return
        try {
            val json = JSONObject().apply {
                put("method", method)
                put("params", JSONArray(streams))
                put("id", System.currentTimeMillis().toInt() and 0xFFFF)
            }
            ws.send(json.toString())
            AppLogger.d(TAG, "Mengirim perintah WS: $method untuk aliran: $streams")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Gagal mengirim perintah berlangganan $method", e)
        }
    }

    fun subscribe(streamName: String, onMessage: (JSONObject) -> Unit) {
        val lowercaseStream = streamName.lowercase()
        
        synchronized(this) {
            val listeners = streamListeners.getOrPut(lowercaseStream) { mutableSetOf() }
            listeners.add(onMessage)

            val subs = streamSubscribers.getOrDefault(lowercaseStream, 0)
            streamSubscribers[lowercaseStream] = subs + 1

            ensureConnected()

            if (subs == 0) {
                sendSubscription("SUBSCRIBE", listOf(lowercaseStream))
            }
        }
    }

    fun unsubscribe(streamName: String, onMessage: (JSONObject) -> Unit) {
        val lowercaseStream = streamName.lowercase()
        
        synchronized(this) {
            val listeners = streamListeners[lowercaseStream]
            listeners?.remove(onMessage)
            if (listeners?.isEmpty() == true) {
                streamListeners.remove(lowercaseStream)
            }

            val subs = streamSubscribers.getOrDefault(lowercaseStream, 0)
            if (subs <= 1) {
                streamSubscribers.remove(lowercaseStream)
                sendSubscription("UNSUBSCRIBE", listOf(lowercaseStream))
            } else {
                streamSubscribers[lowercaseStream] = subs - 1
            }
        }
    }

    fun reconnect() {
        synchronized(this) {
            AppLogger.i(TAG, "Memaksa rekonstruksi koneksi WebSocket...")
            activeWebSocket?.close(1000, "Forced Reconnect")
            activeWebSocket = null
            retryAttempt = 0
            ensureConnected()
        }
    }

    fun close() {
        isClosed.set(true)
        activeWebSocket?.close(1000, "App closed")
        activeWebSocket = null
        scope.cancel()
    }
}
