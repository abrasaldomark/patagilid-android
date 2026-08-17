package com.devmarkabrasaldo.PataGilid.data.repository

import android.util.Log
import com.devmarkabrasaldo.PataGilid.data.local.MountainListDao
import com.devmarkabrasaldo.PataGilid.domain.models.MountainList
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val TAG = "MountainListRepository"

/// Manages user-created mountain lists — syncing Firestore (`users/{uid}/mountainLists`)
/// with the local Room cache.
class MountainListRepository(
    private val dao: MountainListDao
) {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    // MARK: - Observe (Room)

    /// Returns a live Flow of all lists for the signed-in user from the local Room cache.
    fun observeLists(): Flow<List<MountainList>> {
        val uid = currentUserId ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return dao.observeListsByUser(uid)
    }

    // MARK: - Sync from Firestore

    /// Fetches all of the current user's lists from Firestore and upserts them into Room.
    suspend fun syncFromFirestore() = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext
        try {
            val snapshot = db.collection("users").document(uid)
                .collection("mountainLists")
                .get()
                .await()

            val lists = snapshot.documents.mapNotNull { doc ->
                try {
                    MountainList(
                        id = doc.id,
                        userId = uid,
                        name = doc.getString("name") ?: return@mapNotNull null,
                        emoji = doc.getString("emoji") ?: "🏔️",
                        mountainIds = (doc.get("mountainIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        createdAt = (doc.get("createdAt") as? com.google.firebase.Timestamp)?.toDate()?.time 
                            ?: (doc.get("createdAt") as? Number)?.toLong() 
                            ?: System.currentTimeMillis(),
                        updatedAt = (doc.get("updatedAt") as? com.google.firebase.Timestamp)?.toDate()?.time 
                            ?: (doc.get("updatedAt") as? Number)?.toLong() 
                            ?: System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse list document ${doc.id}: ${e.message}")
                    null
                }
            }
            dao.deleteAllListsForUser(uid)
            dao.upsertLists(lists)
            Log.d(TAG, "Synced ${lists.size} mountain lists from Firestore.")
        } catch (e: Exception) {
            Log.w(TAG, "Firestore sync failed (offline?): ${e.message}")
        }
    }

    // MARK: - Create

    /// Creates a new named list in Firestore and Room, returning its generated ID.
    suspend fun createList(name: String, emoji: String = "🏔️"): String = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: throw IllegalStateException("Must be signed in to create a list.")
        val docRef = db.collection("users").document(uid).collection("mountainLists").document()
        val list = MountainList(
            id = docRef.id,
            userId = uid,
            name = name.trim(),
            emoji = emoji
        )
        docRef.set(list.toFirestoreMap()).await()
        dao.upsertList(list)
        Log.d(TAG, "Created new list '${list.displayTitle}' (${list.id})")
        list.id
    }

    // MARK: - Rename

    /// Updates the name and/or emoji of an existing list.
    suspend fun renameList(listId: String, newName: String, newEmoji: String) = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext
        val now = System.currentTimeMillis()
        val updates = mapOf(
            "name" to newName.trim(),
            "emoji" to newEmoji,
            "updatedAt" to now
        )
        db.collection("users").document(uid).collection("mountainLists").document(listId)
            .update(updates).await()

        dao.getListById(listId)?.let { existing ->
            dao.upsertList(existing.copy(name = newName.trim(), emoji = newEmoji, updatedAt = now))
        }
    }

    // MARK: - Delete

    /// Permanently removes a list from Firestore and Room.
    suspend fun deleteList(listId: String) = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext
        db.collection("users").document(uid).collection("mountainLists").document(listId)
            .delete().await()
        dao.deleteListById(listId)
        Log.d(TAG, "Deleted list $listId")
    }

    // MARK: - Add / Remove Mountain

    /// Adds a mountain ID to a list using Firestore arrayUnion (idempotent).
    suspend fun addMountain(listId: String, mountainId: String) = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext
        val now = System.currentTimeMillis()
        db.collection("users").document(uid).collection("mountainLists").document(listId)
            .update(mapOf("mountainIds" to FieldValue.arrayUnion(mountainId), "updatedAt" to now))
            .await()
        dao.getListById(listId)?.let { existing ->
            if (!existing.mountainIds.contains(mountainId)) {
                dao.upsertList(existing.copy(
                    mountainIds = existing.mountainIds + mountainId,
                    updatedAt = now
                ))
            }
        }
    }

    /// Removes a mountain ID from a list using Firestore arrayRemove.
    suspend fun removeMountain(listId: String, mountainId: String) = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext
        val now = System.currentTimeMillis()
        db.collection("users").document(uid).collection("mountainLists").document(listId)
            .update(mapOf("mountainIds" to FieldValue.arrayRemove(mountainId), "updatedAt" to now))
            .await()
        dao.getListById(listId)?.let { existing ->
            dao.upsertList(existing.copy(
                mountainIds = existing.mountainIds.filter { it != mountainId },
                updatedAt = now
            ))
        }
    }
}
