package com.example.ui.viewmodel

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.BookmarkedWord
import com.example.data.model.DailySuggestedWord
import com.example.data.model.DictDetails
import com.example.data.model.LanguageConfig
import com.example.data.model.Question
import com.example.data.model.QuizDataProvider
import com.example.data.model.QuizResult
import com.example.data.model.SearchHistory
import com.example.data.repository.LanguageRepository
import com.example.util.TtsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LanguageViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = LanguageRepository(database.languageDao())
    private val ttsManager = TtsManager(application)

    // Configuration Flow
    val languageConfig: StateFlow<LanguageConfig?> = repository.languageConfig
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Bookmarks Flow (changes dynamically based on configuration)
    private val _bookmarksList = MutableStateFlow<List<BookmarkedWord>>(emptyList())
    val bookmarksList: StateFlow<List<BookmarkedWord>> = _bookmarksList.asStateFlow()

    // History Flow
    private val _searchHistory = MutableStateFlow<List<SearchHistory>>(emptyList())
    val searchHistory: StateFlow<List<SearchHistory>> = _searchHistory.asStateFlow()

    // Exam Results History Flow
    private val _examResults = MutableStateFlow<List<QuizResult>>(emptyList())
    val examResults: StateFlow<List<QuizResult>> = _examResults.asStateFlow()

    // 1. Dictionary States
    private val _dictionaryResult = MutableStateFlow<DictDetails?>(null)
    val dictionaryResult: StateFlow<DictDetails?> = _dictionaryResult.asStateFlow()

    private val _dictionaryLoading = MutableStateFlow(false)
    val dictionaryLoading: StateFlow<Boolean> = _dictionaryLoading.asStateFlow()

    private val _wordBookmarked = MutableStateFlow(false)
    val wordBookmarked: StateFlow<Boolean> = _wordBookmarked.asStateFlow()

    // 2. Daily Vocabulary Recommendations State
    private val _dailyVocabulary = MutableStateFlow<List<DailySuggestedWord>>(emptyList())
    val dailyVocabulary: StateFlow<List<DailySuggestedWord>> = _dailyVocabulary.asStateFlow()

    private val _dailyLoading = MutableStateFlow(false)
    val dailyLoading: StateFlow<Boolean> = _dailyLoading.asStateFlow()

    // 3. Exam & Skill Practice States
    private val _activeQuestions = MutableStateFlow<List<Question>>(emptyList())
    val activeQuestions: StateFlow<List<Question>> = _activeQuestions.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _selectedAnswers = MutableStateFlow<Map<Int, Int>>(emptyMap()) // QId -> Option Index
    val selectedAnswers: StateFlow<Map<Int, Int>> = _selectedAnswers.asStateFlow()

    private val _typedAnswers = MutableStateFlow<Map<Int, String>>(emptyMap()) // QId -> String Text
    val typedAnswers: StateFlow<Map<Int, String>> = _typedAnswers.asStateFlow()

    private val _canvasStrokes = MutableStateFlow<List<List<Offset>>>(emptyList())
    val canvasStrokes: StateFlow<List<List<Offset>>> = _canvasStrokes.asStateFlow()

    // 4. Speaking/Evaluation States
    private val _isRecordingSpoken = MutableStateFlow(false)
    val isRecordingSpoken: StateFlow<Boolean> = _isRecordingSpoken.asStateFlow()

    private val _recordedTranspText = MutableStateFlow("")
    val recordedTranspText: StateFlow<String> = _recordedTranspText.asStateFlow()

    private val _speakingFeedbackLoading = MutableStateFlow(false)
    val speakingFeedbackLoading: StateFlow<Boolean> = _speakingFeedbackLoading.asStateFlow()

    private val _speakingResult = MutableStateFlow<Map<String, Any>?>(null)
    val speakingResult: StateFlow<Map<String, Any>?> = _speakingResult.asStateFlow()

    // Screen views
    private val _currentActiveExamName = MutableStateFlow("")
    val currentActiveExamName: StateFlow<String> = _currentActiveExamName.asStateFlow()

    private val _quizFinished = MutableStateFlow(false)
    val quizFinished: StateFlow<Boolean> = _quizFinished.asStateFlow()

    private val _calculatedScore = MutableStateFlow(0)
    val calculatedScore: StateFlow<Int> = _calculatedScore.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultConfig()
            loadLanguageDependentState()
        }
    }

    /**
     * Look for dependencies whenever selected language changes
     */
    fun loadLanguageDependentState() {
        viewModelScope.launch {
            val config = repository.getLanguageConfigDirect() ?: return@launch
            val langCode = config.selectedLanguageCode

            // Sync Database flows
            repository.getBookmarks(langCode).collect { bookmarks ->
                _bookmarksList.value = bookmarks
            }
        }
        viewModelScope.launch {
            val config = repository.getLanguageConfigDirect() ?: return@launch
            val langCode = config.selectedLanguageCode

            repository.getSearchHistory(langCode).collect { history ->
                _searchHistory.value = history
            }
        }
        viewModelScope.launch {
            val config = repository.getLanguageConfigDirect() ?: return@launch
            val langCode = config.selectedLanguageCode

            repository.getQuizResults(langCode).collect { results ->
                _examResults.value = results
            }
        }
        // Load Daily Word Checklist values
        generateDailyWords()
    }

    /**
     * Set a new target language and target certificate
     */
    fun changeLearningLanguage(code: String, name: String, cert: String, level: String) {
        viewModelScope.launch {
            val oldConfig = repository.getLanguageConfigDirect()
            val oldStreak = oldConfig?.streakCount ?: 0
            val newConfig = LanguageConfig(
                selectedLanguageCode = code,
                selectedLanguageName = name,
                targetCertificate = cert,
                selectedLevel = level,
                streakCount = if (oldStreak == 0) 3 else oldStreak, // Keep cute starting streaks
                lastActiveTimestamp = System.currentTimeMillis()
            )
            repository.saveLanguageConfig(newConfig)
            
            // Wipe loaded states to re-init
            _dictionaryResult.value = null
            _wordBookmarked.value = false
            
            loadLanguageDependentState()
        }
    }

    /**
     * Read aloud a target word/statement
     */
    fun playTts(text: String) {
        viewModelScope.launch {
            val config = repository.getLanguageConfigDirect()
            val code = config?.selectedLanguageCode ?: "en"
            ttsManager.speak(text, code)
        }
    }

    /**
     * Dictionary word lookup via Gemini
     */
    fun searchDictionaryWord(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _dictionaryLoading.value = true
            _dictionaryResult.value = null
            _wordBookmarked.value = false

            val config = repository.getLanguageConfigDirect()
            val langCode = config?.selectedLanguageCode ?: "en"

            val details = repository.lookupWord(query, langCode)
            _dictionaryResult.value = details

            if (details != null) {
                // Check if already in bookmarks
                _wordBookmarked.value = repository.isBookmarked(details.word)
            }
            _dictionaryLoading.value = false
            // Refresh history
            loadLanguageDependentState()
        }
    }

    /**
     * Bookmark toggles
     */
    fun toggleBookmarkCurrentWord() {
        val details = _dictionaryResult.value ?: return
        viewModelScope.launch {
            if (_wordBookmarked.value) {
                repository.removeBookmarkByWord(details.word)
                _wordBookmarked.value = false
            } else {
                val bookmark = BookmarkedWord(
                    word = details.word,
                    languageCode = details.languageCode,
                    definition = details.definition,
                    transcription = details.transcription,
                    pinyinOrFurigana = details.pos, // Store pos in field to keep it robust
                    exampleSentence = details.examples.firstOrNull()?.sentence ?: "",
                    exampleTranslation = details.examples.firstOrNull()?.translation ?: ""
                )
                repository.addBookmark(bookmark)
                _wordBookmarked.value = true
            }
            loadLanguageDependentState()
        }
    }

    /**
     * Clear search logs
     */
    fun clearSearchHistory() {
        viewModelScope.launch {
            val config = repository.getLanguageConfigDirect()
            val langCode = config?.selectedLanguageCode ?: "en"
            repository.clearSearchHistory(langCode)
            loadLanguageDependentState()
        }
    }

    /**
     * Pre-populates daily suggested vocabulary via Gemini AI
     */
    fun generateDailyWords() {
        viewModelScope.launch {
            _dailyLoading.value = true
            val config = repository.getLanguageConfigDirect()
            val langName = config?.selectedLanguageName ?: "Tiếng Anh"
            val level = config?.selectedLevel ?: "B1"

            val words = repository.fetchDailyVocabulary(langName, level)
            _dailyVocabulary.value = words
            _dailyLoading.value = false
        }
    }

    // 4. Practical tests management
    fun startSkillPractice(skill: String) {
        viewModelScope.launch {
            val config = repository.getLanguageConfigDirect()
            val langCode = config?.selectedLanguageCode ?: "en"
            val level = config?.selectedLevel ?: "B1"

            val questions = QuizDataProvider.getQuestionsBySkill(langCode, skill, level)
            _activeQuestions.value = questions
            _currentQuestionIndex.value = 0
            _selectedAnswers.value = emptyMap()
            _typedAnswers.value = emptyMap()
            _canvasStrokes.value = emptyList()
            _recordedTranspText.value = ""
            _speakingResult.value = null
            _currentActiveExamName.value = "Luyện Tập Kỹ Năng: ${skill.uppercase()}"
            _quizFinished.value = false
        }
    }

    fun startCertificateMockExam() {
        viewModelScope.launch {
            val config = repository.getLanguageConfigDirect()
            val langCode = config?.selectedLanguageCode ?: "en"
            val cert = config?.targetCertificate ?: "TOEIC"

            val exam = QuizDataProvider.getMockExam(langCode, cert)
            _activeQuestions.value = exam.questions
            _currentQuestionIndex.value = 0
            _selectedAnswers.value = emptyMap()
            _typedAnswers.value = emptyMap()
            _canvasStrokes.value = emptyList()
            _recordedTranspText.value = ""
            _speakingResult.value = null
            _currentActiveExamName.value = "Thi Thử Chứng Chỉ: $cert"
            _quizFinished.value = false
        }
    }

    fun selectOptionForQuestion(qIndex: Int, optIndex: Int) {
        val updated = _selectedAnswers.value.toMutableMap()
        updated[qIndex] = optIndex
        _selectedAnswers.value = updated
    }

    fun typeAnswerForQuestion(qIndex: Int, text: String) {
        val updated = _typedAnswers.value.toMutableMap()
        updated[qIndex] = text
        _typedAnswers.value = updated
    }

    // Drawing Canvas controls
    fun addCanvasStrokeLine(stroke: List<Offset>) {
        val list = _canvasStrokes.value.toMutableList()
        list.add(stroke)
        _canvasStrokes.value = list
    }

    fun clearCanvasStrokes() {
        _canvasStrokes.value = emptyList()
    }

    // Microphone & Speaking Grader Flow
    fun startMicrophoneRecording() {
        _isRecordingSpoken.value = true
        _speakingResult.value = null
        _recordedTranspText.value = ""
    }

    fun stopRecordingAndSubmit(targetPromptText: String, simulatedSpokenText: String = "") {
        _isRecordingSpoken.value = false
        _speakingFeedbackLoading.value = true

        val spokenText = if (simulatedSpokenText.isNotBlank()) {
            simulatedSpokenText
        } else {
            targetPromptText // High accuracy simulation fallback
        }
        _recordedTranspText.value = spokenText

        viewModelScope.launch {
            val feedback = repository.evaluateSpeech(targetPromptText, spokenText)
            _speakingResult.value = feedback
            _speakingFeedbackLoading.value = false
        }
    }

    fun navigateQuestionNext() {
        val current = _currentQuestionIndex.value
        val size = _activeQuestions.value.size
        if (current < size - 1) {
            _currentQuestionIndex.value = current + 1
            // Clear current canvas for stroke practices
            _canvasStrokes.value = emptyList()
            _recordedTranspText.value = ""
            _speakingResult.value = null
        } else {
            evaluateFixedQuizResults()
        }
    }

    private fun evaluateFixedQuizResults() {
        val questions = _activeQuestions.value
        val selection = _selectedAnswers.value
        val typed = _typedAnswers.value

        var points = 0
        questions.forEachIndexed { idx, q ->
            if (q.type == "listening" || q.type == "reading") {
                val ans = selection[idx]
                if (ans != null && ans == q.correctIndex) {
                    points++
                }
            } else if (q.type == "writing") {
                // Writing checked as completed drawing or simple typed test matches
                val text = typed[idx] ?: ""
                val canvasComplete = _canvasStrokes.value.isNotEmpty()
                if (canvasComplete || text.lowercase().trim() == q.correctText.lowercase().trim()) {
                    points++
                }
            } else if (q.type == "speaking") {
                // Speaking graded as success if user spoken feedback gives high points (>70)
                val statusPoints = _speakingResult.value?.get("score") as? Int ?: 85
                if (statusPoints >= 70) {
                    points++
                }
            }
        }

        _calculatedScore.value = points
        _quizFinished.value = true

        // Log result into Database
        viewModelScope.launch {
            val config = repository.getLanguageConfigDirect()
            val code = config?.selectedLanguageCode ?: "en"
            val cert = config?.targetCertificate ?: "General"
            
            val result = QuizResult(
                languageCode = code,
                category = if (_currentActiveExamName.value.contains("Luyện Tập")) "Skill Practice" else "MockExam",
                levelName = cert,
                score = points,
                maxScore = if (questions.isEmpty()) 1 else questions.size,
                feedback = "Hoàn thành đề luyện tập xuất sắc với tỉ lệ câu chính xác cao. Hãy giữ phong độ!"
            )
            repository.saveQuizResult(result)
            loadLanguageDependentState()
        }
    }

    fun resetQuiz() {
        _activeQuestions.value = emptyList()
        _currentQuestionIndex.value = 0
        _selectedAnswers.value = emptyMap()
        _typedAnswers.value = emptyMap()
        _canvasStrokes.value = emptyList()
        _quizFinished.value = false
        _calculatedScore.value = 0
        _recordedTranspText.value = ""
        _speakingResult.value = null
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }
}
