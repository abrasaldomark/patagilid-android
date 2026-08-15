package com.devmarkabrasaldo.PataGilid.domain.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

/// A user-created named collection of mountain IDs (e.g. "Luzon Trip 2026", "CAR Mountains").
/// Stored in Firestore under `users/{userId}/mountainLists/{listId}` and cached locally in Room.
@IgnoreExtraProperties
@Entity(tableName = "mountain_lists")
data class MountainList(
    @PrimaryKey
    var id: String = "",

    /// The authenticated user who owns this list.
    var userId: String = "",

    /// User-provided list name (e.g. "Luzon Trip 2026").
    var name: String = "",

    /// Optional emoji icon chosen by the user (e.g. "🏔️"). Stored as a raw String.
    var emoji: String = "🏔️",

    /// Ordered array of Mountain IDs belonging to this list.
    var mountainIds: List<String> = emptyList(),

    /// Creation timestamp in epoch milliseconds.
    var createdAt: Long = System.currentTimeMillis(),

    /// Last-modified timestamp in epoch milliseconds — used for ordering and future delta-sync.
    var updatedAt: Long = System.currentTimeMillis()
) {
    @get:Exclude
    val mountainCount: Int
        get() = mountainIds.size

    @get:Exclude
    val displayTitle: String
        get() = if (emoji.isNotBlank()) "$emoji $name" else name

    /// Serializes this entity into a plain Map for Firestore writes.
    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "id" to id,
        "userId" to userId,
        "name" to name,
        "emoji" to emoji,
        "mountainIds" to mountainIds,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )
}
