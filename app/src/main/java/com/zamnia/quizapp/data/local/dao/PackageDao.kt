package com.zamnia.quizapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zamnia.quizapp.data.local.entities.DownloadedPackageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PackageDao {
    @Query("SELECT * FROM downloaded_packages WHERE classLevel = :classLevel")
    fun observeDownloadedPackages(classLevel: Int): Flow<List<DownloadedPackageEntity>>

    @Query("SELECT * FROM downloaded_packages WHERE isDownloaded = 1")
    fun getAllDownloadedPackages(): Flow<List<DownloadedPackageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackage(pkg: DownloadedPackageEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_packages WHERE packageId = :packageId AND isDownloaded = 1)")
    suspend fun isPackageDownloaded(packageId: String): Boolean

    @Query("DELETE FROM downloaded_packages WHERE packageId NOT IN (:validIds)")
    suspend fun cleanupDeletedPackages(validIds: List<String>)

    @Query("DELETE FROM downloaded_packages")
    suspend fun cleanupAllPackages()
}
