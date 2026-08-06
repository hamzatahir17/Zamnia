package com.zamnia.quizapp.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zamnia.quizapp.ZamniaEngine
import com.zamnia.quizapp.data.model.User
import com.zamnia.quizapp.ui.zamnia.ActiveSubject
import androidx.compose.ui.graphics.Color
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel : ViewModel() {
    private val repository = ZamniaEngine.repository

    val userProfile: StateFlow<User?> = repository.getUserProfileStream()
        .onStart { 
            // Only start loading if the user is authenticated
            if (ZamniaEngine.supabase.auth.currentUserOrNull() != null) {
                _isLoading.value = true 
            } else {
                _isLoading.value = false
            }
        }
        .onEach { _isLoading.value = false }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Dynamic list of active subjects based on downloads and progress
    val activeSubjects: StateFlow<List<ActiveSubject>> = repository.getAllDownloadedPackages()
        .distinctUntilChanged()
        .flatMapLatest { downloadedList ->
            // Prevent processing if the user is not authenticated
            if (ZamniaEngine.supabase.auth.currentUserOrNull() == null) {
                return@flatMapLatest flowOf(emptyList<ActiveSubject>())
            }
            if (downloadedList.isEmpty()) return@flatMapLatest flowOf(emptyList())

            val subjectFlows = downloadedList.groupBy { it.subject.trim().lowercase() }.map { (subjectName, packages) ->
                // For each subject, sum up progress from all its packages
                val packageProgressFlows = packages.map { pkg ->
                    repository.getProgressForPackage(pkg.packageId).map { answeredCount ->
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
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // Only trigger sync cleanup if the user is authenticated
        if (ZamniaEngine.supabase.auth.currentUserOrNull() != null) {
            syncCleanup()
        }
    }

    private fun syncCleanup() {
        viewModelScope.launch {
            repository.syncAndCleanupPacks()
        }
    }
}
