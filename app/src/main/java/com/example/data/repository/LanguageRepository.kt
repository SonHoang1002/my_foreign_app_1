package com.example.data.repository

import com.example.data.db.LanguageDao
import com.example.data.model.BookmarkedWord
import com.example.data.model.LanguageConfig
import com.example.data.model.QuizResult
import com.example.data.model.SearchHistory
import com.example.data.model.DictDetails
import com.example.data.model.DailySuggestedWord
import com.example.data.api.GeminiClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class LanguageRepository(private val languageDao: LanguageDao) {

    // Language configuration flow
    val languageConfig: Flow<LanguageConfig?> = languageDao.getLanguageConfigFlow()

    suspend fun getLanguageConfigDirect(): LanguageConfig? {
        return languageDao.getLanguageConfigDirect()
    }

    suspend fun saveLanguageConfig(config: LanguageConfig) {
        languageDao.saveLanguageConfig(config)
    }

    // Ensure we have a default config in database
    suspend fun ensureDefaultConfig() {
        val config = getLanguageConfigDirect()
        if (config == null) {
            val defaultConfig = LanguageConfig(
                selectedLanguageCode = "en",
                selectedLanguageName = "Tiếng Anh",
                targetCertificate = "TOEIC",
                selectedLevel = "B1",
                dailyGoalCount = 5,
                streakCount = 3,
                lastActiveTimestamp = System.currentTimeMillis()
            )
            languageDao.saveLanguageConfig(defaultConfig)
        }
    }

    // Bookmarks entries
    fun getBookmarks(languageCode: String): Flow<List<BookmarkedWord>> {
        return languageDao.getBookmarksByLanguage(languageCode)
    }

    suspend fun addBookmark(word: BookmarkedWord) {
        languageDao.addBookmark(word)
    }

    suspend fun removeBookmarkByWord(word: String) {
        languageDao.deleteBookmarkByWord(word)
    }

    suspend fun isBookmarked(word: String): Boolean {
        return languageDao.getBookmarkByWord(word) != null
    }

    // Search History
    fun getSearchHistory(languageCode: String): Flow<List<SearchHistory>> {
        return languageDao.getSearchHistory(languageCode)
    }

    suspend fun recordSearch(keyword: String, languageCode: String) {
        if (keyword.isBlank()) return
        languageDao.addSearchHistory(
            SearchHistory(
                keyword = keyword,
                languageCode = languageCode,
                searchedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearSearchHistory(languageCode: String) {
        languageDao.clearSearchHistory(languageCode)
    }

    // Quiz Results
    fun getQuizResults(languageCode: String): Flow<List<QuizResult>> {
        return languageDao.getQuizResultsByLanguage(languageCode)
    }

    suspend fun saveQuizResult(result: QuizResult) {
        languageDao.insertQuizResult(result)
    }

    // AI Dictionary Lookups
    suspend fun lookupWord(word: String, languageCode: String): DictDetails? {
        // Record in history first
        recordSearch(word, languageCode)
        
        val langName = when (languageCode) {
            "ja" -> "Japanese"
            "zh" -> "Chinese"
            "ko" -> "Korean"
            else -> "English"
        }
        return GeminiClient.lookupDictionary(word, langName)
    }

    // Daily suggested Vocabularies
    suspend fun fetchDailyVocabulary(languageName: String, level: String): List<DailySuggestedWord> {
        return GeminiClient.getDailySuggestions(languageName, level)
    }

    // Speech Evaluation
    suspend fun evaluateSpeech(target: String, spoken: String): Map<String, Any> {
        val resultJson = GeminiClient.gradeSpeech(target, spoken)
        return mapOf(
            "score" to resultJson.optInt("score", 85),
            "fluency" to resultJson.optString("fluency", "Tốt"),
            "feedback" to resultJson.optString("pronunciationFeedback", ""),
            "corrections" to resultJson.optString("corrections", "")
        )
    }
}
