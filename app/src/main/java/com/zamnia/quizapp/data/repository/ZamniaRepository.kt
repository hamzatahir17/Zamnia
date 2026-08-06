package com.zamnia.quizapp.data.repository

import com.zamnia.quizapp.data.local.dao.*
import com.zamnia.quizapp.data.local.entities.*
import com.zamnia.quizapp.data.model.Pack
import com.zamnia.quizapp.data.model.Theme
import com.zamnia.quizapp.data.model.User
import com.zamnia.quizapp.data.remote.SupabaseService
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.*

class ZamniaRepository(
    private val supabase: SupabaseService,
    private val packageDao: PackageDao,
    private val quizDao: QuizDao,
    private val userDao: UserDao,
    private val userPrefsDao: UserPrefsDao
) {
    // --- User Profile & Coins ---
    fun getUserProfileStream(): Flow<User?> = flow {
        val uid = com.zamnia.quizapp.ZamniaEngine.supabase.auth.currentUserOrNull()?.id ?: return@flow emit(null)
        
        val localFlow = userDao.getUserById(uid).map { entity ->
            entity?.let {
                User(
                    uid = it.userId,
                    displayName = it.name,
                    email = it.email,
                    coinBalance = it.coins,
                    activeThemeId = it.activeThemeId
                )
            }
        }
        
        val remoteFlow = supabase.getUserProfileStream(uid)
            .onEach { user ->
                if (user != null) {
                    userDao.insertUser(
                        UserEntity(
                            userId = user.uid,
                            name = user.displayName,
                            email = user.email,
                            coins = user.coinBalance,
                            activeThemeId = user.activeThemeId
                        )
                    )
                }
            }
            .catch { }

        emitAll(combine(localFlow, remoteFlow) { local, remote ->
            remote ?: local
        })
    }

    suspend fun saveUserProfile(user: User) {
        supabase.saveUserProfile(user)
        userDao.insertUser(
            UserEntity(
                userId = user.uid,
                name = user.displayName,
                email = user.email,
                coins = user.coinBalance,
                activeThemeId = user.activeThemeId
            )
        )
    }

    suspend fun getUserProfile(): User? {
        val uid = com.zamnia.quizapp.ZamniaEngine.supabase.auth.currentUserOrNull()?.id ?: return null
        val remoteUser = supabase.getUserProfile(uid)
        if (remoteUser != null) {
            saveUserProfile(remoteUser)
            return remoteUser
        }
        return userDao.getUserById(uid).first()?.let {
            User(
                uid = it.userId,
                displayName = it.name,
                email = it.email,
                coinBalance = it.coins,
                activeThemeId = it.activeThemeId
            )
        }
    }

    /**
     * Specifically checks if the user exists on the server.
     * If not found, it clears the local user data.
     */
    suspend fun verifyRemoteSession(): Boolean {
        val uid = com.zamnia.quizapp.ZamniaEngine.supabase.auth.currentUserOrNull()?.id ?: return false
        val remoteUser = supabase.getUserProfile(uid)
        
        return if (remoteUser == null) {
            // User deleted from Supabase, clear local cache
            userDao.deleteUserById(uid)
            false
        } else {
            // Sync local with latest remote data
            saveUserProfile(remoteUser)
            true
        }
    }

    suspend fun transferCoins(toPublicId: String, amount: Long): Result<Unit> {
        val fromUid = com.zamnia.quizapp.ZamniaEngine.supabase.auth.currentUserOrNull()?.id ?: return Result.failure(Exception("Not logged in"))
        return supabase.transferCoins(fromUid, toPublicId, amount)
    }

    suspend fun getUserByPublicId(publicId: String): User? = supabase.getUserByPublicId(publicId)

    suspend fun isPublicIdUnique(publicId: String): Boolean = supabase.isPublicIdUnique(publicId)

    // --- Package Downloading & Cleanup ---
    
    fun getDownloadedPackages(classLevel: Int) = packageDao.observeDownloadedPackages(classLevel)

    fun getAllDownloadedPackages() = packageDao.getAllDownloadedPackages()

    suspend fun getAvailablePacks(classLevel: Int): List<Pack> = supabase.getAvailablePacks(classLevel)

    suspend fun syncAndCleanupPacks() {
        try {
            val result = supabase.getAvailablePacksForAllClasses()
            if (result.isSuccess) {
                val remotePacks = result.getOrNull() ?: emptyList()
                val validIds = remotePacks.map { it.id }
                
                if (validIds.isEmpty()) {
                    // If server is empty, wipe all local downloaded packages
                    packageDao.cleanupAllPackages()
                } else {
                    packageDao.cleanupDeletedPackages(validIds)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ZamniaRepository", "Cleanup failed: ${e.message}")
        }
    }

    suspend fun downloadPackage(packageId: String, classLevel: Int, subject: String, chapter: String) {
        try {
            // 1. Wipe old content FIRST to avoid mix-up
            quizDao.deleteQuestionsByPackage(packageId)
            quizDao.deleteProgressByPackage(packageId)

            // 2. Fetch fresh MCQs from Supabase
            val supabaseQuestions = supabase.getQuestionsByPackage(packageId)
            if (supabaseQuestions.isEmpty()) {
                // If server is empty, we just leave it wiped
                return
            }

            // 3. Map to Room Entities
            val localQuestions = supabaseQuestions.map { q ->
                QuizQuestionEntity(
                    id = q.id.toString(),
                    packageId = packageId,
                    classLevel = classLevel,
                    subject = subject,
                    chapter = chapter,
                    questionText = q.question,
                    optionA = q.options.getOrNull(0) ?: "",
                    optionB = q.options.getOrNull(1) ?: "",
                    optionC = q.options.getOrNull(2) ?: "",
                    optionD = q.options.getOrNull(3) ?: "",
                    correctOption = q.correctAnswerIndex
                )
            }

            // 4. Update Package Metadata
            packageDao.insertPackage(
                DownloadedPackageEntity(
                    packageId = packageId,
                    classLevel = classLevel,
                    subject = subject,
                    chapterName = chapter,
                    totalMcqs = localQuestions.size,
                    isDownloaded = true
                )
            )

            // 5. Insert fresh questions
            quizDao.insertQuestionsBatch(localQuestions)
            
            android.util.Log.d("ZamniaRepo", "Sync Complete: $packageId with ${localQuestions.size} Qs")
        } catch (e: Exception) {
            android.util.Log.e("ZamniaRepository", "Download failed: ${e.message}")
            throw e
        }
    }

    // --- Quiz Logic ---
    
    fun get20RandomQuestions(packageId: String): Flow<List<QuizQuestionEntity>> {
        return quizDao.get20RandomQuestionsByPackage(packageId)
    }

    suspend fun submitQuizAnswer(isCorrect: Boolean) {
        val uid = com.zamnia.quizapp.ZamniaEngine.supabase.auth.currentUserOrNull()?.id ?: return
        supabase.updateQuizCoins(uid, isCorrect)
    }

    suspend fun getQuestions(): List<com.zamnia.quizapp.data.model.Question> = supabase.getQuestions()

    suspend fun saveQuizHistory(score: Int, total: Int, coins: Int) {
        quizDao.insertQuizResult(
            LocalQuizHistory(
                score = score,
                totalQuestions = total,
                coinsEarned = coins,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    fun getAllQuizHistory(): Flow<List<LocalQuizHistory>> = quizDao.getAllQuizHistory()

    // --- Dynamic Progress Tracking ---

    suspend fun saveQuestionProgress(questionId: String, packageId: String, isCorrect: Boolean) {
        quizDao.saveQuestionProgress(
            UserQuestionProgressEntity(questionId, packageId, isCorrect)
        )
    }

    fun getProgressForPackage(packageId: String): Flow<Int> = quizDao.getAnsweredCountForPackage(packageId)

    // --- Themes & Preferences ---
    suspend fun getThemes(): List<Theme> = supabase.getThemes()

    suspend fun purchaseTheme(themeId: String, price: Long): Result<Unit> {
        val uid = com.zamnia.quizapp.ZamniaEngine.supabase.auth.currentUserOrNull()?.id ?: return Result.failure(Exception("Not logged in"))
        return supabase.purchaseTheme(uid, themeId, price)
    }

    fun getUserPrefs(): Flow<LocalUserPrefs?> {
        val uid = com.zamnia.quizapp.ZamniaEngine.supabase.auth.currentUserOrNull()?.id ?: return kotlinx.coroutines.flow.flowOf(null)
        return userPrefsDao.getUserPrefs(uid)
    }

    suspend fun saveCustomTimer(seconds: Int) {
        val uid = com.zamnia.quizapp.ZamniaEngine.supabase.auth.currentUserOrNull()?.id ?: return
        val currentPrefs = userPrefsDao.getUserPrefs(uid).first()
        userPrefsDao.saveUserPrefs(
            LocalUserPrefs(
                userId = uid,
                lastCustomTimer = seconds,
                activeThemeId = currentPrefs?.activeThemeId ?: "default"
            )
        )
    }
}
