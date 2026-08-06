package com.zamnia.quizapp.ui.packs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zamnia.quizapp.ZamniaEngine
import com.zamnia.quizapp.data.local.entities.DownloadedPackageEntity
import com.zamnia.quizapp.data.model.Pack
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class PacksNavigationLevel {
    CLASSES, SUBJECTS, CHAPTERS
}

class PacksViewModel : ViewModel() {
    private val repository = ZamniaEngine.repository

    private val _navLevel = MutableStateFlow(PacksNavigationLevel.CLASSES)
    val navLevel: StateFlow<PacksNavigationLevel> = _navLevel.asStateFlow()

    private val _selectedClass = MutableStateFlow<Int?>(null)
    val selectedClass: StateFlow<Int?> = _selectedClass.asStateFlow()

    private val _selectedSubject = MutableStateFlow<String?>(null)
    val selectedSubject: StateFlow<String?> = _selectedSubject.asStateFlow()

    private val _availablePacks = MutableStateFlow<List<Pack>>(emptyList())
    val availablePacks: StateFlow<List<Pack>> = _availablePacks.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val downloadedPackages: StateFlow<List<DownloadedPackageEntity>> = _selectedClass
        .flatMapLatest { level -> 
            if (level != null) repository.getDownloadedPackages(level) 
            else flowOf(emptyList()) 
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isDownloading = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val isDownloading: StateFlow<Map<String, Boolean>> = _isDownloading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // --- Navigation Actions ---

    fun selectClass(level: Int) {
        _selectedClass.value = level
        _navLevel.value = PacksNavigationLevel.SUBJECTS
        loadPacksForClass(level)
    }

    fun selectSubject(subject: String) {
        _selectedSubject.value = subject
        _navLevel.value = PacksNavigationLevel.CHAPTERS
    }

    fun goBack() {
        when (_navLevel.value) {
            PacksNavigationLevel.CHAPTERS -> {
                _navLevel.value = PacksNavigationLevel.SUBJECTS
                _selectedSubject.value = null
            }
            PacksNavigationLevel.SUBJECTS -> {
                _navLevel.value = PacksNavigationLevel.CLASSES
                _selectedClass.value = null
                _availablePacks.value = emptyList()
            }
            else -> {}
        }
    }

    private fun loadPacksForClass(level: Int) {
        viewModelScope.launch {
            try {
                _availablePacks.value = repository.getAvailablePacks(level)
            } catch (e: Exception) {
                android.util.Log.e("PacksVM", "Failed to fetch available packs: ${e.message}")
                _error.value = "Offline Mode: Showing downloaded packs only."
            }
        }
    }

    fun downloadPack(packageId: String, subject: String, chapter: String) {
        viewModelScope.launch {
            _isDownloading.value = _isDownloading.value + (packageId to true)
            try {
                repository.downloadPackage(packageId, _selectedClass.value ?: 9, subject, chapter)
            } catch (e: Exception) {
                _error.value = "Download failed. Check connection."
            } finally {
                _isDownloading.value = _isDownloading.value + (packageId to false)
            }
        }
    }
}
