package com.devmarkabrasaldo.PataGilid.data.repository

import android.util.Log
import com.devmarkabrasaldo.PataGilid.domain.models.HikeLog
import com.devmarkabrasaldo.PataGilid.domain.models.IslandGroup
import com.devmarkabrasaldo.PataGilid.domain.models.Mountain
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

fun DocumentSnapshot.toHikeLogSafely(): HikeLog? {
    return try {
        val startVal = get("dateTimeStart")
        val startMillis = when (startVal) {
            is Timestamp -> startVal.toDate().time
            is Date -> startVal.time
            is Number -> startVal.toLong()
            is String -> startVal.toLongOrNull() ?: 0L
            else -> 0L
        }

        val endVal = get("dateTimeEnd")
        val endMillis = when (endVal) {
            is Timestamp -> endVal.toDate().time
            is Date -> endVal.time
            is Number -> endVal.toLong()
            is String -> endVal.toLongOrNull() ?: 0L
            else -> 0L
        }

        val photos = (get("photoUrls") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

        HikeLog(
            id = id,
            userId = getString("userId") ?: "",
            mountainId = getString("mountainId") ?: "",
            dateTimeStart = startMillis,
            dateTimeEnd = endMillis,
            didSummit = getBoolean("didSummit") ?: true,
            photoUrls = photos,
            trailName = getString("trailName"),
            isTraverse = getBoolean("isTraverse"),
            exitTrailName = getString("exitTrailName"),
            trailDifficulty = getString("trailDifficulty"),
            trailClass = getString("trailClass")
        )
    } catch (e: Exception) {
        Log.e("FirestoreExtensions", "Error safe-parsing HikeLog for doc $id: ${e.localizedMessage}", e)
        null
    }
}

fun HikeLog.toFirestoreMap(): Map<String, Any?> {
    return hashMapOf(
        "id" to (id ?: ""),
        "userId" to userId,
        "mountainId" to mountainId,
        "dateTimeStart" to Timestamp(Date(dateTimeStart)),
        "dateTimeEnd" to Timestamp(Date(dateTimeEnd)),
        "didSummit" to didSummit,
        "photoUrls" to photoUrls,
        "trailName" to trailName,
        "isTraverse" to isTraverse,
        "exitTrailName" to exitTrailName,
        "trailDifficulty" to trailDifficulty,
        "trailClass" to trailClass
    )
}

fun DocumentSnapshot.toMountainSafely(): Mountain? {
    return try {
        val updatedVal = get("updatedAt")
        val updatedMillis = when (updatedVal) {
            is Timestamp -> updatedVal.toDate().time
            is Date -> updatedVal.time
            is Number -> updatedVal.toLong()
            is String -> updatedVal.toLongOrNull() ?: System.currentTimeMillis()
            else -> System.currentTimeMillis()
        }

        val verifiers = (get("pendingVerifierEmails") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

        val latVal = get("latitude")
        val lat = when (latVal) {
            is Number -> latVal.toDouble()
            is String -> latVal.toDoubleOrNull()
            else -> null
        }

        val lonVal = get("longitude")
        val lon = when (lonVal) {
            is Number -> lonVal.toDouble()
            is String -> lonVal.toDoubleOrNull()
            else -> null
        }

        val pendingLatVal = get("pendingLatitude")
        val pLat = when (pendingLatVal) {
            is Number -> pendingLatVal.toDouble()
            is String -> pendingLatVal.toDoubleOrNull()
            else -> null
        }

        val pendingLonVal = get("pendingLongitude")
        val pLon = when (pendingLonVal) {
            is Number -> pendingLonVal.toDouble()
            is String -> pendingLonVal.toDoubleOrNull()
            else -> null
        }

        Mountain(
            id = getString("id") ?: id,
            name = getString("name") ?: "",
            descriptionText = getString("description") ?: getString("descriptionText") ?: "",
            elevationMASL = getLong("elevationMASL")?.toInt() ?: 0,
            latitude = lat,
            longitude = lon,
            region = getString("region") ?: "",
            islandGroup = getString("islandGroup") ?: IslandGroup.LUZON.displayName,
            difficultyLevel = getString("difficultyLevel") ?: "",
            trailClass = getString("trailClass") ?: "",
            isApproved = getBoolean("isApproved"),
            contributorId = getString("contributorId"),
            contributorEmail = getString("contributorEmail"),
            contributorName = getString("contributorName"),
            pendingLatitude = pLat,
            pendingLongitude = pLon,
            pendingRegion = getString("pendingRegion"),
            pendingContributorEmail = getString("pendingContributorEmail"),
            pendingContributorName = getString("pendingContributorName"),
            pendingVerifications = getLong("pendingVerifications")?.toInt() ?: 0,
            pendingVerifierEmails = verifiers,
            updatedAt = updatedMillis,
            isVerifiedByCommunity = getBoolean("isVerifiedByCommunity") ?: false,
            communityVerifications = getLong("communityVerifications")?.toInt() ?: 0
        )
    } catch (e: Exception) {
        Log.e("FirestoreExtensions", "Error safe-parsing Mountain for doc $id: ${e.localizedMessage}", e)
        null
    }
}

fun Mountain.toFirestoreMap(): Map<String, Any?> {
    return hashMapOf(
        "id" to id,
        "name" to name,
        "description" to descriptionText,
        "elevationMASL" to elevationMASL,
        "latitude" to latitude,
        "longitude" to longitude,
        "region" to region,
        "islandGroup" to islandGroup,
        "difficultyLevel" to difficultyLevel,
        "trailClass" to trailClass,
        "isApproved" to isApproved,
        "contributorId" to contributorId,
        "contributorEmail" to contributorEmail,
        "contributorName" to contributorName,
        "pendingLatitude" to pendingLatitude,
        "pendingLongitude" to pendingLongitude,
        "pendingRegion" to pendingRegion,
        "pendingContributorEmail" to pendingContributorEmail,
        "pendingContributorName" to pendingContributorName,
        "pendingVerifications" to pendingVerifications,
        "pendingVerifierEmails" to pendingVerifierEmails,
        "updatedAt" to Timestamp(Date(updatedAt)),
        "isVerifiedByCommunity" to isVerifiedByCommunity,
        "communityVerifications" to communityVerifications
    )
}
