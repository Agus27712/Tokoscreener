package com.tokoreader.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tokoreader.data.local.TokoDatabase
import com.tokoreader.data.local.prefs.SettingsRepositoryImpl
import com.tokoreader.data.remote.rest.TokocryptoMarketApi
import com.tokoreader.data.remote.rest.TokocryptoTradeApi
import com.tokoreader.data.remote.signing.SigningInterceptor
import com.tokoreader.data.repository.MarketDataRepositoryImpl
import com.tokoreader.data.repository.PaperTradeRepositoryImpl
import com.tokoreader.data.repository.PositionRepositoryImpl
import com.tokoreader.data.repository.TradeRepositoryImpl
import com.tokoreader.domain.evaluator.DayTradingSignalEvaluator
import com.tokoreader.domain.evaluator.ScalpingSignalEvaluator
import com.tokoreader.domain.evaluator.SwingSignalEvaluator
import com.tokoreader.domain.model.TradingMode
import com.tokoreader.domain.usecase.ObserveTradingSignalUseCase
import com.tokoreader.domain.usecase.PlaceOrderUseCase
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Interceptor that automatically falls back to alternative Binance / Tokocrypto hosts
 * when encountering HTTP 451 (geo-restrictions), 403, timeouts, or connection failures.
 */
class FallbackHostInterceptor : Interceptor {
    private val TAG = "FallbackInterceptor"

    private val candidateHosts = listOf(
        "www.tokocrypto.site",
        "data-api.binance.vision",
        "api.binance.com"
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        var lastException: IOException? = null
        var lastResponse: Response? = null

        for (host in candidateHosts) {
            val newUrl = originalRequest.url.newBuilder()
                .host(host)
                .build()
            val newRequest = originalRequest.newBuilder()
                .url(newUrl)
                .build()

            try {
                val response = chain.proceed(newRequest)
                if (response.isSuccessful) {
                    return response
                }
                if (response.code == 451 || response.code == 403 || response.code == 502 || response.code == 503) {
                    Log.w(TAG, "Host $host returned HTTP ${response.code}, trying fallback host")
                    response.close()
                    lastResponse = response
                    continue
                }
                return response
            } catch (e: IOException) {
                Log.w(TAG, "Host $host connection failed (${e.message}), trying fallback host")
                lastException = e
            }
        }

        if (lastResponse != null) {
            return lastResponse
        }
        throw lastException ?: IOException("Failed to connect to any candidate market data host")
    }
}

/**
 * Manual Dependency Injection Container.
 */
class AppContainer(context: Context) {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // 1. Settings Repository
    val settingsRepository = SettingsRepositoryImpl(context)

    // 2. Room Database
    val database = Room.databaseBuilder(
        context.applicationContext,
        TokoDatabase::class.java,
        "tokoreader.db"
    ).fallbackToDestructiveMigration().build()

    // 3. OkHttp Clients
    private val marketOkHttpClient = OkHttpClient.Builder()
        .addInterceptor(FallbackHostInterceptor())
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val tradeOkHttpClient = OkHttpClient.Builder()
        .addInterceptor(SigningInterceptor(settingsRepository))
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // 4. Retrofit Instances
    private val marketRetrofit = Retrofit.Builder()
        .baseUrl("https://www.tokocrypto.site/")
        .client(marketOkHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val tradeRetrofit = Retrofit.Builder()
        .baseUrl("https://www.tokocrypto.com/")
        .client(tradeOkHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    // 5. APIs
    val marketApi: TokocryptoMarketApi = marketRetrofit.create(TokocryptoMarketApi::class.java)
    val tradeApi: TokocryptoTradeApi = tradeRetrofit.create(TokocryptoTradeApi::class.java)

    // 6. Repositories
    val marketDataRepository = MarketDataRepositoryImpl(marketApi, marketOkHttpClient)
    val positionRepository = PositionRepositoryImpl(database.positionDao())
    val paperTradeRepository = PaperTradeRepositoryImpl(
        database.paperAccountDao(),
        database.positionDao(),
        database.localOrderDao()
    )
    val tradeRepository = TradeRepositoryImpl(tradeApi)

    // 7. Evaluators & UseCases
    val evaluators = mapOf(
        TradingMode.SCALPING to ScalpingSignalEvaluator(),
        TradingMode.DAY_TRADING to DayTradingSignalEvaluator(),
        TradingMode.SWING to SwingSignalEvaluator()
    )

    val observeTradingSignalUseCase = ObserveTradingSignalUseCase(
        positionRepository = positionRepository,
        marketDataRepository = marketDataRepository,
        evaluators = evaluators
    )

    val placeOrderUseCase = PlaceOrderUseCase(
        tradeRepository = tradeRepository,
        marketDataRepository = marketDataRepository
    )
}
