package com.devmarkabrasaldo.PataGilid.domain.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import java.io.Serializable

@IgnoreExtraProperties
data class HikeLog(
    @DocumentId
    var id: String? = null,
    var userId: String = "",
    var mountainId: String = "",
    var dateTimeStart: Long = 0L,
    var dateTimeEnd: Long = 0L,
    var didSummit: Boolean = true,
    var photoUrls: List<String> = emptyList(),
    var trailName: String? = null,
    var isTraverse: Boolean? = null,
    var exitTrailName: String? = null,
    var trailDifficulty: String? = null,
    var trailClass: String? = null
) : Serializable {

    @get:Exclude
    val cleanPhotoUrls: List<String>
        get() = photoUrls.map { url ->
            if (url.contains("drive.google.com/file/d/")) {
                val parts = url.split("file/d/")
                if (parts.size > 1) {
                    val idPart = parts[1].substringBefore("/").substringBefore("?")
                    return@map "https://drive.google.com/uc?id=$idPart&export=view"
                }
            }
            url
        }
}
