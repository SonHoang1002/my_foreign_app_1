package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BookmarkedWord
import com.example.data.model.LanguageConfig
import com.example.data.model.QuizResult
import com.example.data.model.SearchHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface LanguageDao {

    // Language Configuration
    @Query("SELECT * FROM language_config WHERE id = 1 LIMIT 1")
    fun getLanguageConfigFlow(): Flow<LanguageConfig?>

    @Query("SELECT * FROM language_config WHERE id = 1 LIMIT 1")
    suspend fun getLanguageConfigDirect(): LanguageConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLanguageConfig(config: LanguageConfig)

    // Bookmarked Vocabulary
    @Query("SELECT * FROM bookmarked_word WHERE languageCode = :languageCode ORDER BY bookmarkedAt DESC")
    fun getBookmarksByLanguage(languageCode: String): Flow<List<BookmarkedWord>>

    @Query("SELECT * FROM bookmarked_word WHERE word = :word LIMIT 1")
    suspend fun getBookmarkByWord(word: String): BookmarkedWord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addBookmark(word: BookmarkedWord)

    @Delete
    suspend fun deleteBookmark(word: BookmarkedWord)

    @Query("DELETE FROM bookmarked_word WHERE word = :word")
    suspend fun deleteBookmarkByWord(word: String)

    // Search History
    @Query("SELECT * FROM search_history WHERE languageCode = :languageCode ORDER BY searchedAt DESC LIMIT 20")
    fun getSearchHistory(languageCode: String): Flow<List<SearchHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSearchHistory(history: SearchHistory)

    @Query("DELETE FROM search_history WHERE languageCode = :languageCode")
    suspend fun clearSearchHistory(languageCode: String)

    // Quiz Results
    @Query("SELECT * FROM quiz_result ORDER BY takenAt DESC")
    fun getAllQuizResults(): Flow<List<QuizResult>>

    @Query("SELECT * FROM quiz_result WHERE languageCode = :languageCode ORDER BY takenAt DESC")
    fun getQuizResultsByLanguage(languageCode: String): Flow<List<QuizResult>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizResult(result: QuizResult)
}
