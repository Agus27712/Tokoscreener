package com.tokoreader.domain.repository

import com.tokoreader.domain.model.ApiCredentials
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getApiCredentials(): Flow<ApiCredentials>
    suspend fun saveApiCredentials(apiKey: String, secret: String)
    suspend fun clearCredentials()
    
    fun getRealBuyMode(): Flow<Boolean>
    suspend fun setRealBuyMode(enabled: Boolean)

    fun getTradingMode(): Flow<String>
    suspend fun saveTradingMode(mode: String)

    fun getThemeMode(): Flow<String>
    suspend fun saveThemeMode(mode: String)

    fun getAccentColor(): Flow<String>
    suspend fun saveAccentColor(color: String)

    fun getSymbolType(): Flow<Int>
    suspend fun saveSymbolType(type: Int)
    fun getCurrentSymbolType(): Int

    fun getCustomWatchlistSymbols(): Flow<Set<String>>
    suspend fun addCustomWatchlistSymbol(symbol: String)
    suspend fun removeCustomWatchlistSymbol(symbol: String)
}
