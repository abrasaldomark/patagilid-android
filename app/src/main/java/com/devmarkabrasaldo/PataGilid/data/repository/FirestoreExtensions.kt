package com.devmarkabrasaldo.PataGilid.data.repository

import android.util.Log
import com.devmarkabrasaldo.PataGilid.domain.models.HikeLog
import com.devmarkabrasaldo.PataGilid.domain.models.IslandGroup
import com.devmarkabrasaldo.PataGilid.domain.models.Mountain
import com.devmarkabrasaldo.PataGilid.domain.models.CoordinateSubmission
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
        val waypointsList = (get("waypoints") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

        HikeLog(
            id = id,
            userId = getString("userId") ?: "",
            mountainId = getString("mountainId") ?: "",
            dateTimeStart = startMillis,
            dateTimeEnd = endMillis,
            didSummit = getBoolean("didSummit") ?: true,
            photoUrls = photos,
            trailName = getString("trailName") ?: "",
            routeType = getString("routeType") ?: "",
            exitTrailName = getString("exitTrailName") ?: "",
            trailDifficulty = getString("trailDifficulty") ?: "",
            trailClass = getString("trailClass") ?: "",
            waypoints = waypointsList
        )
    } catch (e: Exception) {
        Log.e("FirestoreExtensions", "Error safe-parsing HikeLog for doc $id: ${e.localizedMessage}", e)
        null
    }
}

fun HikeLog.toFirestoreMap(): Map<String, Any?> {
    return hashMapOf(
        "id" to id,
        "userId" to userId,
        "mountainId" to mountainId,
        "dateTimeStart" to Timestamp(Date(dateTimeStart)),
        "dateTimeEnd" to Timestamp(Date(dateTimeEnd)),
        "didSummit" to didSummit,
        "photoUrls" to photoUrls,
        "trailName" to trailName,
        "routeType" to routeType,
        "exitTrailName" to exitTrailName,
        "trailDifficulty" to trailDifficulty,
        "trailClass" to trailClass,
        "waypoints" to waypoints
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

        val pendingCount = getLong("pendingCalibrationsCount")?.toInt() ?: 0

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
            pendingCalibrationsCount = pendingCount,
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
        "pendingCalibrationsCount" to pendingCalibrationsCount,
        "updatedAt" to Timestamp(Date(updatedAt)),
        "isVerifiedByCommunity" to isVerifiedByCommunity,
        "communityVerifications" to communityVerifications
    )
}

fun DocumentSnapshot.toCoordinateSubmissionSafely(): CoordinateSubmission? {
    return try {
        val submittedVal = get("submittedAt")
        val submittedMillis = when (submittedVal) {
            is Timestamp -> submittedVal.toDate().time
            is Date -> submittedVal.time
            is Number -> submittedVal.toLong()
            is String -> submittedVal.toLongOrNull() ?: System.currentTimeMillis()
            else -> System.currentTimeMillis()
        }

        val latVal = get("latitude")
        val lat = when (latVal) {
            is Number -> latVal.toDouble()
            is String -> latVal.toDoubleOrNull()
            else -> 0.0
        }

        val lonVal = get("longitude")
        val lon = when (lonVal) {
            is Number -> lonVal.toDouble()
            is String -> lonVal.toDoubleOrNull()
            else -> 0.0
        }

        CoordinateSubmission(
            id = getString("id") ?: id,
            mountainId = getString("mountainId") ?: "",
            latitude = lat ?: 0.0,
            longitude = lon ?: 0.0,
            region = getString("region") ?: "",
            contributorEmail = getString("contributorEmail") ?: "",
            contributorName = getString("contributorName") ?: "",
            submittedAt = submittedMillis,
            status = getString("status") ?: "PENDING"
        )
    } catch (e: Exception) {
        Log.e("FirestoreExtensions", "Error safe-parsing CoordinateSubmission for doc $id: ${e.localizedMessage}", e)
        null
    }
}

fun CoordinateSubmission.toFirestoreMap(): Map<String, Any?> {
    return hashMapOf(
        "id" to id,
        "mountainId" to mountainId,
        "latitude" to latitude,
        "longitude" to longitude,
        "region" to region,
        "contributorEmail" to contributorEmail,
        "contributorName" to contributorName,
        "submittedAt" to Timestamp(Date(submittedAt)),
        "status" to status
    )
}
