package com.tokoreader.domain.repository

import com.tokoreader.domain.model.NewsItem
import kotlinx.coroutines.flow.Flow

interface NewsRepository {
    fun observeNews(): Flow<List<NewsItem>>
    suspend fun refreshNews(): Result<Unit>
}

interface AiAnalyzerRepository {
    suspend fun analyzeCoin(symbol: String, news: List<NewsItem>, marketData: String, provider: String): Result<String>
    suspend fun translateText(text: String, targetLanguage: String): Result<String>
}
