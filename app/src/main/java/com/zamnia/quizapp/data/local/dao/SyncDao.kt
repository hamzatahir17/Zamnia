package com.zamnia.quizapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zamnia.quizapp.data.local.entities.PendingSyncEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncDao {
    @Query("SELECT * FROM pending_sync WHERE isSynced = 0")
    fun getPendingQuizzes(): Flow<List<PendingSyncEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingQuiz(sync: PendingSyncEntity)

    @Query("UPDATE pending_sync SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Int)
}
