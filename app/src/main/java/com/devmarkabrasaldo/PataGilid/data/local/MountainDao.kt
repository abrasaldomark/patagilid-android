package com.devmarkabrasaldo.PataGilid.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devmarkabrasaldo.PataGilid.domain.models.Mountain
import kotlinx.coroutines.flow.Flow

@Dao
interface MountainDao {
    @Query("SELECT * FROM mountains WHERE isApproved != 0 OR isApproved IS NULL ORDER BY elevationMASL DESC")
    fun getAllApprovedMountainsByElevation(): Flow<List<Mountain>>

    @Query("SELECT * FROM mountains WHERE isApproved != 0 OR isApproved IS NULL ORDER BY name ASC")
    fun getAllApprovedMountainsByName(): Flow<List<Mountain>>

    @Query("SELECT * FROM mountains WHERE isApproved != 0 OR isApproved IS NULL ORDER BY updatedAt DESC")
    fun getAllApprovedMountainsByRecent(): Flow<List<Mountain>>

    @Query("SELECT * FROM mountains WHERE isApproved = 0")
    fun getPendingApprovalMountains(): Flow<List<Mountain>>

    @Query("SELECT * FROM mountains WHERE pendingLatitude IS NOT NULL AND (isApproved != 0 OR isApproved IS NULL)")
    fun getPendingGpsMountains(): Flow<List<Mountain>>

    @Query("SELECT * FROM mountains WHERE id = :id")
    suspend fun getMountainById(id: String): Mountain?

    @Query("SELECT * FROM mountains WHERE id = :id")
    fun observeMountainById(id: String): Flow<Mountain?>

    @Query("SELECT MAX(updatedAt) FROM mountains")
    suspend fun getLatestUpdatedAtTimestamp(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMountains(mountains: List<Mountain>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMountain(mountain: Mountain)

    @Query("DELETE FROM mountains WHERE id = :id")
    suspend fun deleteMountainById(id: String): Int

    @Query("DELETE FROM mountains")
    suspend fun clearAll(): Int
}

