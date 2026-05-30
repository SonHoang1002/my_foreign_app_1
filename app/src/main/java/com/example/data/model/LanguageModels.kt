package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "language_config")
data class LanguageConfig(
    @PrimaryKey val id: Int = 1,
    val selectedLanguageCode: String = "en", // "en", "ja", "zh", "ko"
    val selectedLanguageName: String = "Tiếng Anh",
    val targetCertificate: String = "IELTS",
    val selectedLevel: String = "B2",
    val dailyGoalCount: Int = 5,
    val streakCount: Int = 0,
    val lastActiveTimestamp: Long = 0L
)

@Entity(tableName = "bookmarked_word")
data class BookmarkedWord(
    @PrimaryKey
    val word: String,
    val languageCode: String,
    val definition: String,
    val transcription: String, // Pinyin, Furigana, phonetic, etc.
    val pinyinOrFurigana: String = "", // Extra pronunciation helpers
    val exampleSentence: String,
    val exampleTranslation: String,
    val isDailySuggested: Boolean = false,
    val bookmarkedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "search_history")
data class SearchHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val keyword: String,
    val languageCode: String,
    val searchedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "quiz_result")
data class QuizResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val languageCode: String,
    val category: String, // "Listening", "Speaking", "Reading", "Writing", "MockExam"
    val levelName: String, // "HSK1", "JLPT N5", "IELTS", etc.
    val score: Int,
    val maxScore: Int,
    val feedback: String = "",
    val takenAt: Long = System.currentTimeMillis()
)

// Standard non-entity data structures for Dict & QA parsed from JSON or Gemini
data class DictDetails(
    val word: String,
    val languageCode: String,
    val transcription: String = "",
    val pos: String = "", // Part of Speech
    val translation: String,
    val definition: String = "",
    val details: String = "",
    val strokeGuidance: String = "", // Stroke order description or URL for ja/zh/ko
    val examples: List<ExampleLine> = emptyList()
)

data class ExampleLine(
    val sentence: String,
    val translation: String,
    val visualPrompt: String = "" // AI generated visual description for illustration
)

data class DailySuggestedWord(
    val word: String,
    val transcription: String = "",
    val translation: String,
    val partOfSpeech: String = "",
    val example: String = "",
    val exampleTranslation: String = ""
)
