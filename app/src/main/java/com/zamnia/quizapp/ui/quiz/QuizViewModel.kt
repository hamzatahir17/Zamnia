package com.zamnia.quizapp.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zamnia.quizapp.ZamniaEngine
import com.zamnia.quizapp.data.model.Question
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class QuizResponse(
    val question: Question,
    val selectedIndex: Int,
    val isCorrect: Boolean
)

class QuizViewModel : ViewModel() {
    private val repository = ZamniaEngine.repository

    private val _questions = MutableStateFlow<List<Question>>(emptyList())
    val questions: StateFlow<List<Question>> = _questions.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _selectedAnswer = MutableStateFlow<Int?>(null)
    val selectedAnswer: StateFlow<Int?> = _selectedAnswer.asStateFlow()

    private val _quizResponses = MutableStateFlow<List<QuizResponse>>(emptyList())
    val quizResponses: StateFlow<List<QuizResponse>> = _quizResponses.asStateFlow()

    private val _timerSeconds = MutableStateFlow(0)
    val timerSeconds: StateFlow<Int> = _timerSeconds.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _coinsEarned = MutableStateFlow(0)
    val coinsEarned: StateFlow<Int> = _coinsEarned.asStateFlow()

    private val _isFinished = MutableStateFlow(false)
    val isFinished: StateFlow<Boolean> = _isFinished.asStateFlow()

    private val _hasNoQuestions = MutableStateFlow(false)
    val hasNoQuestions: StateFlow<Boolean> = _hasNoQuestions.asStateFlow()

    private var timerJob: Job? = null
    private var customTimerValue: Int = 30
    private var currentPackageId: String? = null

    fun startQuiz(customTimer: Int, packageId: String? = null) {
        customTimerValue = customTimer
        currentPackageId = packageId
        _hasNoQuestions.value = false
        _quizResponses.value = emptyList()
        
        viewModelScope.launch {
            repository.saveCustomTimer(customTimer)
            
            if (packageId != null) {
                try {
                    val localQuestions = repository.get20RandomQuestions(packageId).first()
                    if (localQuestions.isEmpty()) {
                        _hasNoQuestions.value = true
                    } else {
                        _questions.value = localQuestions.map { lq ->
                            Question(
                                id = lq.id.toLongOrNull() ?: 0L,
                                question = lq.questionText,
                                options = listOf(lq.optionA, lq.optionB, lq.optionC, lq.optionD),
                                correctAnswerIndex = lq.correctOption,
                                category = lq.subject
                            )
                        }
                        resetAndStart()
                    }
                } catch (e: Exception) {
                    _hasNoQuestions.value = true
                }
            } else {
                val remoteQuestions = repository.getQuestions()
                if (remoteQuestions.isEmpty()) {
                    _hasNoQuestions.value = true
                } else {
                    _questions.value = remoteQuestions
                    resetAndStart()
                }
            }
        }
    }
    
    private fun resetAndStart() {
        _currentQuestionIndex.value = 0
        _score.value = 0
        _coinsEarned.value = 0
        _isFinished.value = false
        _selectedAnswer.value = null
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        if (customTimerValue <= 0) return
        
        _timerSeconds.value = customTimerValue
        timerJob = viewModelScope.launch {
            while (_timerSeconds.value > 0) {
                delay(1000)
                _timerSeconds.value--
            }
            submitAnswer(-1)
        }
    }

    fun submitAnswer(selectedIndex: Int) {
        if (_selectedAnswer.value != null || _isFinished.value) return
        
        timerJob?.cancel()
        val currentQuestion = _questions.value.getOrNull(_currentQuestionIndex.value) ?: return
        
        val isCorrect = selectedIndex == currentQuestion.correctAnswerIndex
        _selectedAnswer.value = selectedIndex
        
        // Record response for review
        _quizResponses.value = _quizResponses.value + QuizResponse(currentQuestion, selectedIndex, isCorrect)

        if (isCorrect) {
            _score.value++
            _coinsEarned.value += 10
        } else {
            _coinsEarned.value -= 5
        }

        viewModelScope.launch {
            repository.submitQuizAnswer(isCorrect)
            
            currentPackageId?.let { pkgId ->
                repository.saveQuestionProgress(currentQuestion.id.toString(), pkgId, isCorrect)
            }
            
            delay(1200) // Show results for 1.2 seconds
            
            if (_currentQuestionIndex.value < _questions.value.size - 1) {
                _selectedAnswer.value = null
                _currentQuestionIndex.value++
                startTimer()
            } else {
                _isFinished.value = true
                repository.saveQuizHistory(_score.value, _questions.value.size, _coinsEarned.value)
            }
        }
    }
}
