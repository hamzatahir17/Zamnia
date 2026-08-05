package com.zamnia.quizapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zamnia.quizapp.data.local.entities.LocalUserPrefs
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPrefsDao {
    @Query("SELECT * FROM user_prefs WHERE userId = :userId")
    fun getUserPrefs(userId: String): Flow<LocalUserPrefs?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserPrefs(prefs: LocalUserPrefs)
}
