package com.zamnia.quizapp.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zamnia.quizapp.ZamniaEngine
import com.zamnia.quizapp.data.model.User
import com.zamnia.quizapp.ui.zamnia.ActiveSubject
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import io.github.jan.supabase.auth.auth

class DashboardViewModel : ViewModel() {
    private val repository = ZamniaEngine.repository

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile.asStateFlow()

    // Dynamic list of active subjects based on downloads and progress
    val activeSubjects: StateFlow<List<ActiveSubject>> = repository.getAllDownloadedPackages()
        .flatMapLatest { downloadedList ->
            if (downloadedList.isEmpty()) return@flatMapLatest flowOf(emptyList())

            val subjectFlows = downloadedList.groupBy { it.subject.trim().lowercase() }.map { (subjectName, packages) ->
                // For each subject, sum up progress from all its packages
                val packageProgressFlows = packages.map { pkg ->
                    repository.getProgressForPackage(pkg.packageId).map { answeredCount ->
                        // Debug log to confirm progress is being read
                        android.util.Log.d("DashboardVM", "Package ${pkg.packageId}: Answered $answeredCount / ${pkg.totalMcqs}")
                        answeredCount to pkg.totalMcqs
                    }
                }

                combine(packageProgressFlows) { results ->
                    var totalAnswered = 0
                    var totalQuestions = 0
                    results.forEach { (answered, total) ->
                        totalAnswered += answered
                        totalQuestions += total
                    }

                    val color = when(subjectName.lowercase()) {
                        "physics" -> Color(0xFF4CAF50)
                        "biology" -> Color(0xFFE91E63)
                        "mathematics", "math" -> Color(0xFFF44336)
                        "chemistry" -> Color(0xFFFF9800)
                        else -> Color(0xFF6200EE)
                    }

                    ActiveSubject(
                        name = subjectName,
                        lastChapter = packages.lastOrNull()?.chapterName ?: "None",
                        progress = if (totalQuestions > 0) totalAnswered.toFloat() / totalQuestions else 0f,
                        color = color
                    )
                }
            }

            combine(subjectFlows) { it.toList() }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadUserProfile()
        syncCleanup()
    }

    private fun syncCleanup() {
        viewModelScope.launch {
            repository.syncAndCleanupPacks()
        }
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            val auth = com.zamnia.quizapp.ZamniaEngine.supabase.auth
            val currentUser = auth.currentUserOrNull()
            
            if (currentUser != null) {
                _isLoading.value = true
                repository.getUserProfileStream().collect { profile ->
                    _userProfile.value = profile
                    _isLoading.value = false
                }
            } else {
                _userProfile.value = null
                _isLoading.value = false
            }
        }
    }
}
