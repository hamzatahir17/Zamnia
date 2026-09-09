package com.zamnia.quizapp.data.repository

import android.util.Log
import com.zamnia.quizapp.ZamniaEngine
import com.zamnia.quizapp.data.local.dao.*
import com.zamnia.quizapp.data.local.entities.*
import com.zamnia.quizapp.data.model.Pack
import com.zamnia.quizapp.data.model.Question
import com.zamnia.quizapp.data.model.Theme
import com.zamnia.quizapp.data.model.User
import com.zamnia.quizapp.data.remote.SupabaseService
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ZamniaRepository(
    private val supabase: SupabaseService,
    private val database: com.zamnia.quizapp.data.local.ZamniaDatabase,
    private val packageDao: PackageDao,
    private val quizDao: QuizDao,
    private val userDao: UserDao,
    private val userPrefsDao: UserPrefsDao
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var cachedProfileFlow: Flow<User?>? = null
    private var cachedUid: String? = null

    // --- User Profile & Coins (Realtime Sync) ---
    fun getUserProfileStream(): Flow<User?> {
        val currentUid = ZamniaEngine.supabase.auth.currentUserOrNull()?.id
        if (cachedProfileFlow != null && cachedUid != null && cachedUid == currentUid) {
            return cachedProfileFlow!!
        }

        cachedUid = currentUid

        val flow = channelFlow {
            // Coroutine 1: Observe Room local cache continuously for instant UI rendering & offline play
            val localJob = launch {
                userDao.getAllUsersFlow().collect { userList ->
                    val activeUid = ZamniaEngine.supabase.auth.currentUserOrNull()?.id ?: cachedUid
                    val entity = if (activeUid != null) {
                        userList.firstOrNull { it.userId == activeUid } ?: userList.firstOrNull()
                    } else {
                        userList.firstOrNull()
                    }

                    val user = entity?.let {
                        User(
                            uid = it.userId,
                            userId = it.publicId,
                            displayName = it.name,
                            email = it.email,
                            coinBalance = it.coins,
                            activeThemeId = it.activeThemeId,
                            unlockedThemes = it.unlockedThemesCsv.split(",").filter { id -> id.isNotBlank() }.ifEmpty { listOf("default") }
                        )
                    }
                    send(user)
                }
            }

            // Coroutine 2: Listen to Supabase Realtime WebSocket for live server updates
            val realtimeJob = launch {
                try {
                    var uid = ZamniaEngine.supabase.auth.currentUserOrNull()?.id
                    while (uid == null) {
                        delay(200)
                        uid = ZamniaEngine.supabase.auth.currentUserOrNull()?.id ?: userDao.getAnyUserSync()?.userId
                    }

                    supabase.getUserProfileStream(uid).collect { remoteUser ->
                        if (remoteUser != null) {
                            userDao.insertUser(
                                UserEntity(
                                    userId = remoteUser.uid,
                                    publicId = remoteUser.userId,
                                    name = remoteUser.displayName ?: remoteUser.email.substringBefore("@"),
                                    email = remoteUser.email,
                                    coins = remoteUser.coinBalance ?: 0L,
                                    activeThemeId = remoteUser.activeThemeId ?: "default",
                                    unlockedThemesCsv = remoteUser.unlockedThemes.ifEmpty { listOf("default") }.joinToString(",")
                                )
                            )
                        } else {
                            Log.w("ZamniaRepository", "User $uid deleted from remote database! Clearing local cache...")
                            userDao.deleteUserById(uid)
                        }
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.w("ZamniaRepository", "Realtime profile stream error: ${e.message}")
                }
            }

            awaitClose {
                localJob.cancel()
                realtimeJob.cancel()
            }
        }.distinctUntilChanged()
        .shareIn(
            scope = repositoryScope,
            started = SharingStarted.WhileSubscribed(5000),
            replay = 1
        )

        cachedProfileFlow = flow
        return flow
    }

    suspend fun clearAllLocalData() {
        cachedProfileFlow = null
        cachedUid = null
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            database.clearAllTables()
        }
    }

    suspend fun getAnyLocalUser(): User? {
        val entity = userDao.getAnyUserSync() ?: return null
        return User(
            uid = entity.userId,
            userId = entity.publicId,
            displayName = entity.name,
            email = entity.email,
            coinBalance = entity.coins,
            activeThemeId = entity.activeThemeId,
            unlockedThemes = entity.unlockedThemesCsv.split(",").filter { id -> id.isNotBlank() }.ifEmpty { listOf("default") }
        )
    }

    suspend fun migrateGuestToUser(guestUid: String, newUser: User) {
        Log.d("ZamniaRepository", "Migrating guest $guestUid to new user ${newUser.uid} (${newUser.email})...")
        
        // 1. First save new upgraded user profile to Supabase & Room DB
        saveUserProfile(newUser)

        // 2. Delete old guest row from remote Supabase public.users table via RPC / Postgrest
        supabase.migrateGuestUserRpc(guestUid)

        // 3. Delete old guest row from local Room DB
        userDao.deleteUserById(guestUid)
    }

    suspend fun saveUserProfile(user: User) {
        val safeEmail = user.email.ifBlank { "guest@zamnia.com" }
        val safeUser = user.copy(email = safeEmail)

        try {
            supabase.saveUserProfile(safeUser)
        } catch (e: Exception) {
            Log.w("ZamniaRepository", "saveUserProfile remote error: ${e.message}")
        }

        userDao.insertUser(
            UserEntity(
                userId = safeUser.uid,
                publicId = safeUser.userId,
                name = safeUser.displayName.ifBlank { safeUser.email.substringBefore("@") },
                email = safeUser.email,
                coins = safeUser.coinBalance,
                activeThemeId = safeUser.activeThemeId,
                unlockedThemesCsv = safeUser.unlockedThemes.ifEmpty { listOf("default") }.joinToString(",")
            )
        )
    }

    suspend fun getUserProfile(): User? {
        val uid = com.zamnia.quizapp.ZamniaEngine.supabase.auth.currentUserOrNull()?.id ?: return null
        
        try {
            val remoteUser = supabase.getUserProfile(uid)
            if (remoteUser != null) {
                saveUserProfile(remoteUser)
                return remoteUser
            }
        } catch (e: Exception) {
            android.util.Log.w("ZamniaRepository", "Failed to fetch remote profile, falling back to local: ${e.message}")
        }

        // Fallback to local Room Database
        return userDao.getUserById(uid).firstOrNull()?.let {
            User(
                uid = it.userId,
                userId = it.publicId,
                displayName = it.name,
                email = it.email,
                coinBalance = it.coins,
                activeThemeId = it.activeThemeId
            )
        }
    }

    /**
     * Specifically checks if the user exists on the server.
     * Returns true if user exists OR if there is a network error (to allow offline play).
     * Returns false ONLY if the user is explicitly missing from the database.
     */
    suspend fun verifyRemoteSession(): Boolean {
        val uid = com.zamnia.quizapp.ZamniaEngine.supabase.auth.currentUserOrNull()?.id ?: return false
        
        return try {
            val remoteUser = supabase.getUserProfile(uid)
            if (remoteUser == null) {
                // Confirmed: User deleted from database
                userDao.deleteUserById(uid)
                false
            } else {
                // Sync local cache
                saveUserProfile(remoteUser)
                true
            }
        } catch (e: Exception) {
            // Network error occurred. We DON'T logout. 
            // We return true to allow the user to continue using cached data.
            android.util.Log.w("ZamniaRepository", "Network error during session verify: ${e.message}")
            true
        }
    }

    suspend fun hasLocalUserSession(): Boolean {
        return userDao.getAnyUserSync() != null
    }

    suspend fun transferCoins(toPublicId: String, amount: Long): String {
        return supabase.transferCoinsRpc(toPublicId, amount)
    }

    suspend fun getDailyTransferCount(): Int {
        val uid = com.zamnia.quizapp.ZamniaEngine.supabase.auth.currentUserOrNull()?.id ?: return 0
        return supabase.getDailyTransferCount(uid)
    }

    suspend fun getUserByPublicId(publicId: String): User? = supabase.getUserByPublicId(publicId)

    suspend fun isPublicIdUnique(publicId: String): Boolean = supabase.isPublicIdUnique(publicId)

    // --- Package Downloading & Cleanup ---
    
    fun getDownloadedPackages(classLevel: Int) = packageDao.observeDownloadedPackages(classLevel)

    fun getAllDownloadedPackages() = packageDao.getAllDownloadedPackages()

    suspend fun getAvailablePacks(classLevel: Int): List<Pack> = supabase.getAvailablePacks(classLevel)

    suspend fun syncAndCleanupPacks() {
        // Ensure user is authenticated before syncing
        if (com.zamnia.quizapp.ZamniaEngine.supabase.auth.currentUserOrNull() == null) return

        try {
            val result = supabase.getAvailablePacksForAllClasses()
            if (result.isSuccess) {
                val remotePacks = result.getOrNull() ?: emptyList()
                val validIds = remotePacks.map { it.id }

                if (validIds.isEmpty()) {
                    // If the server returns no packs, we wipe local cache to stay in sync
                    packageDao.cleanupAllPackages()
                } else {
                    packageDao.cleanupDeletedPackages(validIds)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ZamniaRepository", "Sync cleanup failed: ${e.message}")
        }
    }

    suspend fun downloadPackage(packageId: String, classLevel: Int, subject: String, chapter: String) {
        try {
            Log.d("ZamniaRepo", "Starting download for packageId: $packageId, class: $classLevel, subject: $subject, chapter: $chapter")

            // 1. Fetch fresh MCQs directly from Supabase
            val supabaseQuestions = supabase.getQuestionsByPackage(packageId)
            Log.d("ZamniaRepo", "Fetched ${supabaseQuestions.size} questions from Supabase for $packageId")

            if (supabaseQuestions.isEmpty()) {
                Log.w("ZamniaRepo", "No questions found on Supabase server for package $packageId")
                return
            }

            // 2. Wipe old local content FIRST to avoid duplicates
            quizDao.deleteQuestionsByPackage(packageId)
            quizDao.deleteProgressByPackage(packageId)

            // 3. Map Supabase MCQs to Room Entities
            val localQuestions = supabaseQuestions.map { q ->
                QuizQuestionEntity(
                    id = q.id?.toString() ?: "${packageId}_${q.question.hashCode()}",
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

            // 4. Update Package Metadata in Room DB
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

            // 5. Insert fresh questions batch into Room DB
            quizDao.insertQuestionsBatch(localQuestions)
            
            Log.d("ZamniaRepo", "Download Complete: $packageId with ${localQuestions.size} Qs")
        } catch (e: Exception) {
            Log.e("ZamniaRepository", "Download failed for $packageId: ${e.message}", e)
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
    suspend fun getThemes(): List<Theme> {
        return try {
            val remoteThemes = supabase.getThemes()
            val themesList = if (remoteThemes.isNotEmpty()) remoteThemes else getDefaultThemes()
            themesList.map { t ->
                if (t.id == "default") t.copy(price = 0)
                else t.copy(price = 500)
            }
        } catch (e: Exception) {
            Log.w("ZamniaRepository", "Supabase themes fetch failed, falling back to default themes: ${e.message}")
            getDefaultThemes()
        }
    }

    private fun getDefaultThemes(): List<Theme> = listOf(
        Theme(id = "default", name = "Default Midnight", price = 0, primaryColor = "#6366F1", secondaryColor = "#4F46E5"),
        Theme(id = "ocean_blue", name = "Ocean Blue", price = 500, primaryColor = "#0284C7", secondaryColor = "#0369A1"),
        Theme(id = "emerald", name = "Emerald Green", price = 500, primaryColor = "#059669", secondaryColor = "#047857"),
        Theme(id = "sunset", name = "Sunset Gold", price = 500, primaryColor = "#D97706", secondaryColor = "#B45309"),
        Theme(id = "cyberpunk", name = "Cyber Neon", price = 500, primaryColor = "#EC4899", secondaryColor = "#D946EF")
    )

    suspend fun purchaseTheme(themeId: String, price: Long): Result<Unit> {
        val uid = com.zamnia.quizapp.ZamniaEngine.supabase.auth.currentUserOrNull()?.id ?: return Result.failure(Exception("Not logged in"))
        
        // Check local balance first
        val localUser = userDao.getUserById(uid).firstOrNull()
        val currentCoins = localUser?.coins ?: 0L
        val currentUnlocked = localUser?.unlockedThemesCsv?.split(",")?.filter { it.isNotBlank() }?.toMutableList() ?: mutableListOf("default")

        if (!currentUnlocked.contains(themeId) && currentCoins < price) {
            return Result.failure(Exception("Insufficient coins! Required: $price coins, You have: $currentCoins coins."))
        }

        val result = supabase.purchaseTheme(uid, themeId, price)
        if (result.isSuccess) {
            val remoteUser = supabase.getUserProfile(uid)
            if (remoteUser != null) {
                userDao.insertUser(
                    UserEntity(
                        userId = remoteUser.uid,
                        publicId = remoteUser.userId,
                        name = remoteUser.displayName ?: remoteUser.email.substringBefore("@"),
                        email = remoteUser.email,
                        coins = remoteUser.coinBalance ?: 0L,
                        activeThemeId = remoteUser.activeThemeId ?: "default",
                        unlockedThemesCsv = remoteUser.unlockedThemes.ifEmpty { listOf("default") }.joinToString(",")
                    )
                )
            }
        }
        return result
    }

    suspend fun selectTheme(themeId: String): Result<Unit> {
        val uid = ZamniaEngine.supabase.auth.currentUserOrNull()?.id ?: return Result.failure(Exception("Not logged in"))
        
        // Instantly update local Room cache for 0ms UI feedback
        val localUser = userDao.getUserById(uid).firstOrNull()
        if (localUser != null) {
            val currentUnlocked = localUser.unlockedThemesCsv.split(",").filter { it.isNotBlank() }.toMutableList()
            if (!currentUnlocked.contains("default")) currentUnlocked.add("default")
            if (!currentUnlocked.contains(themeId)) currentUnlocked.add(themeId)
            
            userDao.insertUser(
                localUser.copy(
                    activeThemeId = themeId,
                    unlockedThemesCsv = currentUnlocked.joinToString(",")
                )
            )
        }

        return supabase.selectTheme(uid, themeId)
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
