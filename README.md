# 🚀 TokoReader - Tokocrypto Spot Quantitative Trading & Screener Engine

![Android CI/CD](https://github.com/your-username/TokoReader/actions/workflows/build-and-release.yml/badge.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-blue.svg?logo=kotlin)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-brightgreen.svg)
![Android SDK](https://img.shields.io/badge/minSdk-24%20%7C%20targetSdk-36-orange.svg)
![License](https://img.shields.io/badge/License-MIT-green.svg)

**TokoReader** adalah terminal screener dan engine kuantitatif trading otomatis untuk pasar spot **Tokocrypto (API v3 & WebSocket Real-time Feed)**. Dibangun menggunakan arsitektur modern Android (Kotlin, Jetpack Compose Material 3, Coroutines Flow, Room DB, dan Clean Architecture).

---

## 📥 Unduh Debug APK (GitHub Releases & Actions)

Anda dapat langsung mengunduh file APK siap pakai melalui dua cara:

1. **GitHub Releases**:
   - Buka tab **[Releases](../../releases)** pada repositori ini.
   - Unduh file `TokoReader-debug.apk` versi terbaru.
2. **GitHub Actions Artifact**:
   - Buka tab **[Actions](../../actions)** pada repositori ini.
   - Klik workflow run terbaru, lalu scroll ke bagian **Artifacts** di bawah dan unduh `TokoReader-Debug-APK`.

---

## ✨ Fitur Utama

### 1. 🎯 3 Mode Strategi Kuantitatif (Spot Trading)
- **⚡ Scalping (Timeframe 1m - 5m)**:
  - Indikator: EMA 9 & EMA 21 Crossover, RSI 9 Rebound, Volume Surge Threshold (> 1.25x MA20 Volume).
  - Risk Management: Dynamic ATR-based Stop Loss & Take Profit (1:2 RRR).
- **📊 Day Trading / Intraday (Timeframe 15m - 1h)**:
  - Indikator: EMA 20 & EMA 50 Crossover, MACD Histogram Momentum, ADX Trend Strength (> 18), RSI 14.
  - Risk Management: Dynamic ATR 1.5x SL / 3x TP.
- **🌊 Swing Trading (Timeframe 4h - 1d)**:
  - Indikator: EMA 50 & EMA 200 Golden Cross, RSI Oversold Rebound (≤ 30), ADX (> 20).
  - Risk Management: Dynamic ATR 2x SL / 4x TP.

### 2. ⚡ Multi-Tier Real-Time Data Feed & Fallback
- **Multiplexed WebSockets**: Koneksi real-time stream ganda (Binance Vision, Tokocrypto Stream) dengan auto-reconnect & fallback otomatis.
- **Sub-Second Price Flash**: Deteksi perubahan tick harga secara instan dengan visual flash Neon Green / Neon Red.
- **Multi-Host REST Fallback**: Penanganan cerdas terhadap geo-blocking / HTTP 451 Tokocrypto dengan kandidat mirror host (`www.tokocrypto.site`, `cloudme-toko.2meta.app`, `data-api.binance.vision`) dan pre-seeded catalog resmi.

### 3. 🛡️ Risk & Portfolio Management
- **Paper Trading (Demo) & Live Mode**: Uji strategi tanpa risiko finansial atau jalankan eksekusi spot order riil melalui API resmi.
- **Dual Balance IDR & USDT**: Saldo multi-aset dengan fitur swap/konversi instan terintegrasi.
- **Sequential Stepper Pipeline**: Validasi sinyal 4 langkah bertingkat (*Bias*, *Setup*, *Trigger*, *Entry*) dengan animasi 3D flip vertikal.

---

## 🛠️ Arsitektur Aplikasi

Proyek ini menerapkan prinsip **Clean Architecture & SOLID** dengan modularitas tinggi:

```
com.tokoreader
├── data
│   ├── local (Room Database, SharedPreferences & Encrypted Storage)
│   ├── remote
│   │   ├── rest (Tokocrypto REST API, Fallback Interceptors & Catalog)
│   │   ├── signing (HMAC-SHA256 Request Signer)
│   │   └── websocket (Multiplexed Market Socket & Kline Streams)
│   └── repository (MarketData, PaperTrade, Position, Settings)
├── domain
│   ├── evaluator (Scalping, DayTrading, Swing Signal Engines)
│   ├── indicator (EMA, RSI, MACD, ATR, ADX, Bollinger Bands, Fibonacci)
│   └── model (Ticker, Kline, Position, TradingSignal, SymbolFilter)
└── presentation
    ├── components (LivePriceFlash, CryptoUtils, Charts)
    ├── dashboard (Screener Watchlist, Filters & Navigation)
    ├── radar (Sequential Stepper, Visual Candlestick & Trade Controls)
    ├── portfolio (Asset Distribution, Open Positions & Order History)
    └── settings (API Credentials, Strategy Parameters & Theme)
```

---

## 🤖 Otomatisasi CI/CD (GitHub Actions)

Alur kerja GitHub Actions telah dikonfigurasi di `.github/workflows/build-and-release.yml` untuk melakukan:
- ✅ Kompilasi otomatis dengan JDK 17 & Gradle 9+.
- ✅ Menjalankan Unit Test indikator & evaluator kuantitatif.
- ✅ Membangun file APK Debug (`./gradlew assembleDebug`).
- ✅ Menghitung verifikasi checksum SHA-256.
- ✅ Mengunggah APK ke **Artifacts** pada setiap push/PR.
- ✅ Merilis otomatis ke **GitHub Releases** ketika tag Git (misal: `v1.0.0`) dibuat atau dipicu melalui *Manual Workflow Dispatch*.

### Cara Memicu Rilis Otomatis via Git Tag:
```bash
git tag v1.0.0
git push origin v1.0.0
```

---

## 💻 Kompilasi & Menjalankan Lokal

### Prasyarat:
- JDK 17 atau lebih baru
- Android Studio Ladybug / Meerkat atau Android SDK Build Tools (API 34-36)

### Langkah Build:
1. Clone repositori:
   ```bash
   git clone https://github.com/your-username/TokoReader.git
   cd TokoReader
   ```
2. Buat file `.env` (atau salin dari `.env.example`):
   ```bash
   cp .env.example .env
   ```
3. Decode debug keystore (jika diperlukan):
   ```bash
   base64 -d debug.keystore.base64 > debug.keystore
   ```
4. Jalankan Unit Test:
   ```bash
   ./gradlew testDebugUnitTest
   ```
5. Bangun Debug APK:
   ```bash
   ./gradlew assembleDebug
   ```
   *Output APK akan berada di `app/build/outputs/apk/debug/app-debug.apk`.*

---

## 🔑 Konfigurasi Kredensial API Tokocrypto

1. Buka aplikasi **TokoReader** pada perangkat Android Anda.
2. Buka menu **Pengaturan (Settings)** di pojok kanan atas.
3. Masukkan **API Key** dan **Secret Key** yang didapatkan dari akun Tokocrypto Anda.
4. Pilih mode transaksi **DEMO (Paper Trade)** untuk simulasi atau **LIVE** untuk eksekusi nyata.

---

## 📄 Lisensi

Proyek ini dirilis di bawah lisensi [MIT License](LICENSE).
