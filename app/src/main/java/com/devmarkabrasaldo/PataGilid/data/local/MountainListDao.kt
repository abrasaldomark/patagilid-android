package com.devmarkabrasaldo.PataGilid.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devmarkabrasaldo.PataGilid.domain.models.MountainList
import kotlinx.coroutines.flow.Flow

@Dao
interface MountainListDao {

    /// Observe all lists owned by [userId], ordered newest first.
    @Query("SELECT * FROM mountain_lists WHERE userId = :userId ORDER BY updatedAt DESC")
    fun observeListsByUser(userId: String): Flow<List<MountainList>>

    /// One-shot fetch for a single list by its ID.
    @Query("SELECT * FROM mountain_lists WHERE id = :listId")
    suspend fun getListById(listId: String): MountainList?

    /// Upsert — inserts or replaces the entire row on conflict.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertList(list: MountainList)

    /// Upsert a batch of lists.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLists(lists: List<MountainList>)

    /// Remove a single list by ID.
    @Query("DELETE FROM mountain_lists WHERE id = :listId")
    suspend fun deleteListById(listId: String)

    /// Remove all cached lists for a given user (e.g. on sign-out).
    @Query("DELETE FROM mountain_lists WHERE userId = :userId")
    suspend fun deleteAllListsForUser(userId: String)
}
