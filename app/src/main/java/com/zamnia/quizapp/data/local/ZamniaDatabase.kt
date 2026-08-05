package com.zamnia.quizapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.zamnia.quizapp.data.local.dao.*
import com.zamnia.quizapp.data.local.entities.*

@Database(
    entities = [
        LocalUserPrefs::class,
        LocalQuizHistory::class,
        DownloadedPackageEntity::class,
        UserEntity::class,
        QuizQuestionEntity::class,
        PdfMetaDataEntity::class,
        PendingSyncEntity::class,
        UserQuestionProgressEntity::class
    ],
    version = 3, // Increment version for new table
    exportSchema = false
)
abstract class ZamniaDatabase : RoomDatabase() {
    abstract fun userPrefsDao(): UserPrefsDao
    abstract fun quizDao(): QuizDao
    abstract fun packageDao(): PackageDao
    abstract fun userDao(): UserDao
    abstract fun syncDao(): SyncDao
}
