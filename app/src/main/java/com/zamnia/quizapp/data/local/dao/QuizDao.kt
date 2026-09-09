package com.zamnia.quizapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zamnia.quizapp.data.local.entities.LocalQuizHistory
import com.zamnia.quizapp.data.local.entities.QuizQuestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizDao {
    @Query("SELECT * FROM quiz_history ORDER BY timestamp DESC")
    fun getAllQuizHistory(): Flow<List<LocalQuizHistory>>

    @Insert
    suspend fun insertQuizResult(history: LocalQuizHistory)
    
    @Query("SELECT SUM(coinsEarned) FROM quiz_history")
    fun getTotalCoinsEarned(): Flow<Int?>

    // --- New Offline Package Logic ---
    
    @Query("SELECT * FROM quiz_questions WHERE packageId = :packageId ORDER BY RANDOM() LIMIT 20")
    fun get20RandomQuestionsByPackage(packageId: String): Flow<List<QuizQuestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestionsBatch(questions: List<QuizQuestionEntity>)

    @Query("DELETE FROM quiz_questions WHERE packageId = :packageId")
    suspend fun deleteQuestionsByPackage(packageId: String)

    @Query("DELETE FROM user_question_progress WHERE packageId = :packageId")
    suspend fun deleteProgressByPackage(packageId: String)

    // --- Progress Tracking ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveQuestionProgress(progress: com.zamnia.quizapp.data.local.entities.UserQuestionProgressEntity)

    @Query("SELECT COUNT(DISTINCT questionId) FROM user_question_progress WHERE packageId = :packageId")
    fun getAnsweredCountForPackage(packageId: String): Flow<Int>
}
