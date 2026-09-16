package com.tokoreader.presentation.chart

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Fullscreen Interactive TradingView Binance Chart.
 * Embeds official TradingView widget optimized for mobile devices with dark theme and landscape rotation support.
 */
@Composable
fun FullscreenTradingViewDialog(
    symbol: String,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var isLandscape by remember { mutableStateOf(false) }

    DisposableEffect(isLandscape) {
        if (activity != null) {
            activity.requestedOrientation = if (isLandscape) {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Dialog(
        onDismissRequest = {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            onDismissRequest()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF131722) // Official TradingView dark navy background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E222D))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$symbol (TradingView Pro)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF2962FF).copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("PRO CHART", color = Color(0xFF2962FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { isLandscape = !isLandscape }) {
                            Icon(
                                Icons.Filled.ScreenRotation,
                                contentDescription = "Rotasi Landscape",
                                tint = if (isLandscape) Color(0xFF38BDF8) else Color.White
                            )
                        }
                        IconButton(onClick = {
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            onDismissRequest()
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = "Tutup Chart", tint = Color.White)
                        }
                    }
                }

                // Interactive Chart WebView
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    TradingViewWebView(symbol = symbol)
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TradingViewWebView(
    symbol: String,
    modifier: Modifier = Modifier
) {
    // Standardize symbol for Binance widget (e.g. BTCIDR -> BINANCE:BTCUSDT or BINANCE:BTCIDR)
    val tradingViewSymbol = remember(symbol) {
        val clean = symbol.uppercase()
        if (clean.endsWith("IDR")) {
            // For pairs with IDR, check if Binance has USDT equivalent for richest TradingView liquidity
            val base = clean.removeSuffix("IDR").removeSuffix("_IDR").removeSuffix("/")
            "BINANCE:${base}USDT"
        } else if (!clean.contains(":")) {
            "BINANCE:$clean"
        } else {
            clean
        }
    }

    val htmlContent = remember(tradingViewSymbol) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <style>
                body, html { margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden; background-color: #131722; }
                #tv_chart_container { width: 100%; height: 100%; }
            </style>
            <script type="text/javascript" src="https://s3.tradingview.com/tv.js"></script>
        </head>
        <body>
            <div id="tv_chart_container"></div>
            <script type="text/javascript">
                new TradingView.widget({
                    "autosize": true,
                    "symbol": "$tradingViewSymbol",
                    "interval": "15",
                    "timezone": "Asia/Jakarta",
                    "theme": "dark",
                    "style": "1",
                    "locale": "id",
                    "toolbar_bg": "#1e222d",
                    "enable_publishing": false,
                    "hide_side_toolbar": false,
                    "allow_symbol_change": true,
                    "save_image": false,
                    "studies": [
                        "MASimple@tv-basicstudies",
                        "RSI@tv-basicstudies",
                        "Volume@tv-basicstudies"
                    ],
                    "container_id": "tv_chart_container"
                });
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                settings.allowContentAccess = true
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()
                loadDataWithBaseURL("https://www.tradingview.com", htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://www.tradingview.com", htmlContent, "text/html", "UTF-8", null)
        }
    )
}
