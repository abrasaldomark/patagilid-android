package com.devmarkabrasaldo.PataGilid.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devmarkabrasaldo.PataGilid.domain.models.HikeLog
import kotlinx.coroutines.flow.Flow

@Dao
interface HikeLogDao {
    @Query("SELECT * FROM hike_logs WHERE userId = :userId ORDER BY dateTimeEnd DESC")
    fun observeLogsByUser(userId: String): Flow<List<HikeLog>>

    @Query("SELECT * FROM hike_logs WHERE userId = :userId ORDER BY dateTimeEnd DESC")
    suspend fun getLogsByUser(userId: String): List<HikeLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLogs(logs: List<HikeLog>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLog(log: HikeLog)

    @Query("DELETE FROM hike_logs WHERE id = :logId")
    suspend fun deleteLogById(logId: String)

    @Query("DELETE FROM hike_logs WHERE userId = :userId")
    suspend fun deleteAllLogsForUser(userId: String)
}
