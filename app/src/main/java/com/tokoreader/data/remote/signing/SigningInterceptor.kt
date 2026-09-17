package com.tokoreader.data.remote.signing

import com.tokoreader.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class SigningInterceptor(
    private val settingsRepository: SettingsRepository
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Only sign requests that explicitly have the placeholder header "Signed: true"
        if (originalRequest.header("Signed") != "true") {
            return chain.proceed(originalRequest)
        }

        // Remove the marker header
        var builder = originalRequest.newBuilder()
            .removeHeader("Signed")

        val credentials = runBlocking { settingsRepository.getApiCredentials().first() }
        val apiKey = credentials.apiKey.trim()
        val secret = credentials.secret.trim()

        if (apiKey.isEmpty() || secret.isEmpty()) {
            throw IOException("Kredensial API Tokocrypto belum diatur. Masukkan API Key & Secret di menu Pengaturan.")
        }

        builder = builder.addHeader("X-MBX-APIKEY", apiKey)

        val originalUrl = originalRequest.url
        val timestamp = System.currentTimeMillis().toString()
        
        // Append timestamp and recvWindow to url builder
        val urlBuilder = originalUrl.newBuilder()
            .addQueryParameter("timestamp", timestamp)
            .addQueryParameter("recvWindow", "60000")
            
        // Generate payload for signature
        val urlWithParams = urlBuilder.build()
        val payload = urlWithParams.query ?: ""

        val signature = HmacSha256Signer.sign(secret, payload)
        urlBuilder.addQueryParameter("signature", signature)

        val finalRequest = builder.url(urlBuilder.build()).build()
        return chain.proceed(finalRequest)
    }
}
