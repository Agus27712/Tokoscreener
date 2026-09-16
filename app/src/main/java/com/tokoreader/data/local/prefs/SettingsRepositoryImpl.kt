package com.tokoreader.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.tokoreader.domain.model.ApiCredentials
import com.tokoreader.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepositoryImpl(private val context: Context) : SettingsRepository {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs = EncryptedSharedPreferences.create(
        context,
        "secure_api_keys",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val THEME_KEY = stringPreferencesKey("theme_mode")
    private val ACCENT_COLOR_KEY = stringPreferencesKey("accent_color")
    private val STRATEGY_KEY = stringPreferencesKey("strategy_mode")
    private val AI_PROVIDER_KEY = stringPreferencesKey("ai_provider")
    
    // Security Keys
    private val API_KEY = "tokocrypto_api_key"
    private val API_SECRET = "tokocrypto_api_secret"
    private val APP_PIN = "app_security_pin"
    private val REAL_BUY_MODE = booleanPreferencesKey("real_buy_mode")

    override fun getApiCredentials(): Flow<ApiCredentials> = context.dataStore.data.map {
        // We trigger flow emit, but read actual secure keys from EncryptedPrefs
        val key = securePrefs.getString(API_KEY, "") ?: ""
        val secret = securePrefs.getString(API_SECRET, "") ?: ""
        ApiCredentials(key, secret)
    }

    override suspend fun saveApiCredentials(apiKey: String, secret: String) {
        securePrefs.edit()
            .putString(API_KEY, apiKey)
            .putString(API_SECRET, secret)
            .apply()
    }

    override suspend fun clearCredentials() {
        securePrefs.edit()
            .remove(API_KEY)
            .remove(API_SECRET)
            .remove(APP_PIN)
            .apply()
    }

    // Security PIN
    fun savePin(pin: String) {
        securePrefs.edit().putString(APP_PIN, pin).apply()
    }

    fun verifyPin(pin: String): Boolean {
        val savedPin = securePrefs.getString(APP_PIN, "")
        return savedPin == pin || savedPin.isNullOrEmpty()
    }
    
    fun hasPinSet(): Boolean {
        return !securePrefs.getString(APP_PIN, "").isNullOrEmpty()
    }

    override fun getTradingMode(): Flow<String> = context.dataStore.data.map {
        it[STRATEGY_KEY] ?: "Scalping"
    }

    override suspend fun saveTradingMode(mode: String) {
        context.dataStore.edit { it[STRATEGY_KEY] = mode }
    }

    override fun getThemeMode(): Flow<String> = context.dataStore.data.map {
        it[THEME_KEY] ?: "Dark Navy"
    }

    override suspend fun saveThemeMode(mode: String) {
        context.dataStore.edit { it[THEME_KEY] = mode }
    }

    override fun getAccentColor(): Flow<String> = context.dataStore.data.map {
        it[ACCENT_COLOR_KEY] ?: "Electric Blue"
    }

    override suspend fun saveAccentColor(color: String) {
        context.dataStore.edit { it[ACCENT_COLOR_KEY] = color }
    }
    
    fun getAiProvider(): Flow<String> = context.dataStore.data.map {
        it[AI_PROVIDER_KEY] ?: "Gemini"
    }
    
    suspend fun saveAiProvider(provider: String) {
        context.dataStore.edit { it[AI_PROVIDER_KEY] = provider }
    }
    
    fun getRealBuyMode(): Flow<Boolean> = context.dataStore.data.map {
        it[REAL_BUY_MODE] ?: false
    }
    
    suspend fun setRealBuyMode(enabled: Boolean) {
        context.dataStore.edit { it[REAL_BUY_MODE] = enabled }
    }
}
