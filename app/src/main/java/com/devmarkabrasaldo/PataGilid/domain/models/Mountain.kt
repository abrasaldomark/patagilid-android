package com.devmarkabrasaldo.PataGilid.domain.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import java.io.Serializable
import java.util.Date

@IgnoreExtraProperties
@Entity(tableName = "mountains")
data class Mountain(
    @PrimaryKey
    var id: String = "",
    var name: String = "",
    var descriptionText: String = "",
    var elevationMASL: Int = 0,
    var latitude: Double? = null,
    var longitude: Double? = null,
    var region: String = "",
    var islandGroup: String = IslandGroup.LUZON.displayName,
    var difficultyLevel: String = "",
    var trailClass: String = "",
    
    // Community Submission & Moderation Metadata
    var isApproved: Boolean? = null,
    var contributorId: String? = null,
    var contributorEmail: String? = null,
    var contributorName: String? = null,
    
    // Pending Crowdsourced GPS Calibration
    var pendingLatitude: Double? = null,
    var pendingLongitude: Double? = null,
    var pendingRegion: String? = null,
    var pendingContributorEmail: String? = null,
    var pendingContributorName: String? = null,
    var pendingVerifications: Int = 0,
    var pendingVerifierEmails: List<String> = emptyList(),
    
    // Delta-Sync & Crowdsourced Verification Metadata
    var updatedAt: Long = System.currentTimeMillis(),
    var isVerifiedByCommunity: Boolean = false,
    var communityVerifications: Int = 0
) : Serializable {

    @get:Exclude
    val isPubliclyApproved: Boolean
        get() = isApproved != false

    @get:Exclude
    val islandGroupEnum: IslandGroup
        get() = IslandGroup.fromDisplayName(islandGroup)

    @get:Exclude
    val displayContributorName: String?
        get() {
            if (!contributorName.isNullOrBlank()) return contributorName
            return contributorEmail?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
        }

    @get:Exclude
    val displayPendingContributorName: String?
        get() {
            if (!pendingContributorName.isNullOrBlank()) return pendingContributorName
            return pendingContributorEmail?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
        }
}
