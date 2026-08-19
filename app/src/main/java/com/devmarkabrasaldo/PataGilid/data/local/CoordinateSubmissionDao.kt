package com.devmarkabrasaldo.PataGilid.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devmarkabrasaldo.PataGilid.domain.models.CoordinateSubmission
import kotlinx.coroutines.flow.Flow

@Dao
interface CoordinateSubmissionDao {
    @Query("SELECT * FROM coordinate_submissions WHERE status = 'PENDING'")
    fun getPendingSubmissions(): Flow<List<CoordinateSubmission>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubmissions(submissions: List<CoordinateSubmission>)

    @Query("DELETE FROM coordinate_submissions")
    suspend fun clearSubmissions()
}
