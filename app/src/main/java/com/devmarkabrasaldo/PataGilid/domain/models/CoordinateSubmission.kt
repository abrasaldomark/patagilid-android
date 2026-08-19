package com.devmarkabrasaldo.PataGilid.domain.models

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import java.io.Serializable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "coordinate_submissions")
@IgnoreExtraProperties
data class CoordinateSubmission(
    @PrimaryKey var id: String = "",
    var mountainId: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var region: String = "",
    var contributorEmail: String = "",
    var contributorName: String = "",
    var submittedAt: Long = System.currentTimeMillis(),
    var status: String = "PENDING" // "PENDING", "APPROVED", "REJECTED", "DUPLICATE"
) : Serializable {

    @get:Exclude
    val displayContributorName: String?
        get() {
            if (contributorName.isNotBlank()) return contributorName
            return contributorEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
        }
}
