# TokoReader - Tokocrypto Spot Trading & Screener Engine

Terminal screener & trading bot pintar berbasis Kotlin & Jetpack Compose untuk pasar **Tokocrypto (API v3 & WebSocket Live Feed)**.

---

## 🚀 Fitur Utama

- **3 Mode Strategi Trading**:
  - **Scalping (TF 1m - 5m)**: Menggunakan EMA 9/21, RSI 9, Volume Surge, & Stop Loss/Take Profit berbasis ATR.
  - **Day Trading / Intraday (TF 15m - 1h)**: Menggunakan EMA 20/50, MACD Histogram, ADX Trend Strength (>18), RSI 14, & Dynamic ATR 1.5x/3x.
  - **Swing Trading (TF 4h - 1d)**: Menggunakan EMA 50/200 Golden Cross, RSI Rebound (≤30), ADX (>20), & Dynamic ATR 2x/4x Risk-Reward.

- **Real-Time Data Feed**:
  - WebSocket multi-endpoint (Binance, Binance Vision & Tokocrypto Stream fallback).
  - High-precision sub-second price tick flashes (Neon Green & Neon Red).
  - Fast adaptive REST fallback saat koneksi terputus.

- **Risk & Portfolio Management**:
  - Simulated Paper Trading & Live Trading Mode.
  - Dual Cash Balance Management (**IDR & USDT**) dengan fitur Konversi/Swap Instan secara live.
  - Dynamic ATR-based Stop Loss & Take Profit (tidak hardcoded %).

- **Performa & Modul Codebase**:
  - Terpisah ke dalam layer domain, data, dan presentation yang modular.
  - Bebas "God Code" (setiap composable screen < 500 baris).

---

## 🛠️ Arsitektur Aplikasi

```
com.tokoreader
├── data
│   ├── local (Room DB & Encrypted Preferences)
│   ├── remote (Retrofit REST API v3 & OkHttp WebSockets)
│   └── repository (MarketData, PaperTrade, Position, Settings)
├── domain
│   ├── evaluator (Scalping, DayTrading, Swing Signal Engine)
│   ├── indicator (EMA, RSI, MACD, ATR, ADX, Volume, Bollinger, Fibonacci)
│   └── model (Ticker, Kline, Position, TradingSignal, SymbolFilter)
└── presentation
    ├── components (CryptoUtils, LivePriceFlashText, Custom Visuals)
    ├── dashboard (Watchlist, Filter & Hero Widgets)
    ├── radar (Sequential Pipeline Stepper, Trade Controls & Swap Dialog)
    ├── portfolio (Portfolio & Order History)
    └── settings (Theme, Strategy & Security Settings)
```

---

## 🔑 Cara Setup Kredensial API Tokocrypto

1. Buka aplikasi TokoReader.
2. Navigasi ke **Pengaturan (Settings)**.
3. Masukkan **API Key** dan **API Secret** Tokocrypto Anda.
4. Pilih mode transaksi **DEMO (Paper Trade)** atau **LIVE**.

---

## 🧪 Jalankan Unit Test

Untuk menjalankan pengujian otomatis pada indikator dan evaluator:
```bash
./gradlew testDebugUnitTest
```

---

*Dikembangkan dengan Kotlin, Jetpack Compose, Coroutines Flow & Material 3.*
