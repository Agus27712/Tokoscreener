package com.tokoreader.domain.usecase

import com.tokoreader.domain.repository.AiAnalyzerRepository
import com.tokoreader.domain.repository.NewsRepository

class ScreenAndAnalyzeUseCase(
    private val newsRepository: NewsRepository,
    private val aiAnalyzerRepository: AiAnalyzerRepository
) {
    suspend fun fetchAndTranslateNews(): Result<List<String>> {
        val newsResult = newsRepository.refreshNews()
        if (newsResult.isFailure) return Result.failure(newsResult.exceptionOrNull() ?: Exception("Unknown error"))

        // For simplicity, we just trigger refresh here. Actual fetching happens via observeNews
        return Result.success(emptyList()) // Placeholder, actual translation would iterate news items
    }
    
    suspend fun analyzeCoin(symbol: String, marketData: String, provider: String): Result<String> {
        return aiAnalyzerRepository.analyzeCoin(symbol, emptyList(), marketData, provider)
    }
}
