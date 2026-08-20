package com.devmarkabrasaldo.PataGilid.data.repository

import android.util.Log
import com.devmarkabrasaldo.PataGilid.data.local.MountainDao
import com.devmarkabrasaldo.PataGilid.domain.models.CoordinateSubmission
import com.devmarkabrasaldo.PataGilid.domain.models.HikeLog
import com.devmarkabrasaldo.PataGilid.domain.models.Mountain
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

import com.devmarkabrasaldo.PataGilid.data.local.HikeLogDao
import com.devmarkabrasaldo.PataGilid.data.local.CoordinateSubmissionDao

class MountainRepository(
    private val mountainDao: MountainDao,
    private val hikeLogDao: HikeLogDao,
    private val coordinateSubmissionDao: CoordinateSubmissionDao,
    private val syncService: MountainSyncService
) {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    val allMountainsByElevation: Flow<List<Mountain>> = mountainDao.getAllApprovedMountainsByElevation()
    val allMountainsByName: Flow<List<Mountain>> = mountainDao.getAllApprovedMountainsByName()
    val allMountainsByRecent: Flow<List<Mountain>> = mountainDao.getAllApprovedMountainsByRecent()
    val unapprovedMountains: Flow<List<Mountain>> = mountainDao.getPendingApprovalMountains()

    suspend fun synchronize(onProgress: ((Int, Int) -> Unit)? = null) {
        syncService.synchronizeWithFirestore(onProgress)
    }

    fun observeMountain(id: String): Flow<Mountain?> = mountainDao.observeMountainById(id)

    suspend fun getMountain(id: String): Mountain? = withContext(Dispatchers.IO) {
        mountainDao.getMountainById(id)
    }

    suspend fun submitCustomMountain(
        name: String,
        description: String,
        elevationMASL: Int,
        region: String,
        islandGroup: String,
        difficultyLevel: String,
        trailClass: String,
        latitude: Double? = null,
        longitude: Double? = null
    ): String = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: throw IllegalStateException("You must be logged in to submit a mountain.")
        val docRef = db.collection("mountains").document()
        
        val mountain = Mountain(
            id = docRef.id,
            name = name,
            descriptionText = description,
            elevationMASL = elevationMASL,
            latitude = latitude,
            longitude = longitude,
            region = region,
            islandGroup = islandGroup,
            difficultyLevel = difficultyLevel,
            trailClass = trailClass,
            isApproved = false,
            contributorId = currentUser.uid,
            contributorEmail = currentUser.email,
            contributorName = currentUser.displayName,
            updatedAt = System.currentTimeMillis()
        )
        docRef.set(mountain.toFirestoreMap()).await()
        mountainDao.insertMountain(mountain)
        docRef.id
    }

    suspend fun submitGpsCalibration(
        mountainId: String,
        latitude: Double,
        longitude: Double,
        region: String
    ) = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: throw IllegalStateException("Authentication required.")
        val submissionRef = db.collection("coordinate_submissions").document()
        val data = hashMapOf(
            "mountainId" to mountainId,
            "latitude" to latitude,
            "longitude" to longitude,
            "region" to region,
            "contributorEmail" to currentUser.email,
            "contributorName" to currentUser.displayName,
            "submittedAt" to Timestamp.now(),
            "status" to "PENDING"
        )
        submissionRef.set(data).await()
        
        // Update pending count on mountain document in Firestore
        val mountainRef = db.collection("mountains").document(mountainId)
        val updateMap = mapOf<String, Any>(
            "pendingCalibrationsCount" to FieldValue.increment(1),
            "updatedAt" to Timestamp.now()
        )
        mountainRef.update(updateMap).await()
        synchronize()
    }

    fun getPendingCoordinateSubmissionsFlow(): Flow<List<CoordinateSubmission>> {
        return coordinateSubmissionDao.getPendingSubmissions()
    }

    suspend fun syncPendingCoordinateSubmissions() = withContext(Dispatchers.IO) {
        try {
            val snapshot = db.collection("coordinate_submissions")
                .whereEqualTo("status", "PENDING")
                .get()
                .await()
            val remoteList = snapshot.documents.mapNotNull { it.toCoordinateSubmissionSafely() }
            
            coordinateSubmissionDao.clearSubmissions()
            coordinateSubmissionDao.insertSubmissions(remoteList)
        } catch (e: Exception) {
            Log.e("MountainRepository", "Error syncing pending GPS submissions", e)
        }
    }

    suspend fun deleteCoordinateSubmission(submissionId: String) = withContext(Dispatchers.IO) {
        db.collection("coordinate_submissions").document(submissionId).delete().await()
    }

    suspend fun updateCoordinateSubmission(submissionId: String, lat: Double, lon: Double) = withContext(Dispatchers.IO) {
        db.collection("coordinate_submissions").document(submissionId)
            .update(
                "latitude", lat,
                "longitude", lon,
                "submittedAt", System.currentTimeMillis()
            ).await()
    }

    suspend fun getCoordinateSubmission(submissionId: String): CoordinateSubmission? = withContext(Dispatchers.IO) {
        try {
            val snapshot = db.collection("coordinate_submissions").document(submissionId).get().await()
            snapshot.toCoordinateSubmissionSafely()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateCustomMountain(
        mountainId: String,
        name: String,
        description: String,
        elevationMASL: Int,
        region: String,
        islandGroup: String,
        difficultyLevel: String,
        trailClass: String,
        latitude: Double? = null,
        longitude: Double? = null
    ) = withContext(Dispatchers.IO) {
        val updateMap = mapOf<String, Any?>(
            "name" to name,
            "descriptionText" to description,
            "elevationMASL" to elevationMASL,
            "region" to region,
            "islandGroup" to islandGroup,
            "difficultyLevel" to difficultyLevel,
            "trailClass" to trailClass,
            "latitude" to latitude,
            "longitude" to longitude,
            "updatedAt" to System.currentTimeMillis()
        )
        db.collection("mountains").document(mountainId).update(updateMap).await()
        // Also update local cache so changes reflect instantly
        val existing = mountainDao.getMountainById(mountainId)
        if (existing != null) {
            val updated = existing.copy(
                name = name,
                descriptionText = description,
                elevationMASL = elevationMASL,
                region = region,
                islandGroup = islandGroup,
                difficultyLevel = difficultyLevel,
                trailClass = trailClass,
                latitude = latitude,
                longitude = longitude,
                updatedAt = System.currentTimeMillis()
            )
            mountainDao.insertMountain(updated)
        }
    }

    suspend fun getUserCoordinateSubmissions(email: String): List<CoordinateSubmission> = withContext(Dispatchers.IO) {
        val snapshot = db.collection("coordinate_submissions")
            .whereEqualTo("contributorEmail", email)
            .get()
            .await()
        snapshot.documents.mapNotNull { it.toCoordinateSubmissionSafely() }.sortedByDescending { it.submittedAt }
    }

    // MARK: - Admin Operations
    suspend fun approveCustomMountain(mountainId: String) = withContext(Dispatchers.IO) {
        val ref = db.collection("mountains").document(mountainId)
        ref.update(mapOf("isApproved" to true, "updatedAt" to Timestamp.now())).await()
        synchronize()
    }

    suspend fun deleteMountain(mountainId: String) = withContext(Dispatchers.IO) {
        db.collection("mountains").document(mountainId).delete().await()
        mountainDao.deleteMountainById(mountainId)
    }

    suspend fun applyGpsCalibration(submissionId: String, mountainId: String, lat: Double, lng: Double, region: String) = withContext(Dispatchers.IO) {
        val ref = db.collection("mountains").document(mountainId)
        val updateMap = mapOf<String, Any?>(
            "latitude" to lat,
            "longitude" to lng,
            "region" to region,
            "isVerifiedByCommunity" to true,
            "pendingCalibrationsCount" to 0,
            "updatedAt" to Timestamp.now()
        )
        ref.update(updateMap).await()

        // Update the approved submission
        db.collection("coordinate_submissions").document(submissionId)
            .update("status", "APPROVED").await()

        // Mark all other pending submissions for this mountain as DUPLICATE
        val snapshot = db.collection("coordinate_submissions")
            .whereEqualTo("mountainId", mountainId)
            .whereEqualTo("status", "PENDING")
            .get().await()
        
        for (doc in snapshot.documents) {
            if (doc.id != submissionId) {
                doc.reference.update("status", "DUPLICATE").await()
            }
        }
        
        synchronize()
    }

    suspend fun declineGPS(submissionId: String, mountainId: String) = withContext(Dispatchers.IO) {
        db.collection("coordinate_submissions").document(submissionId)
            .update("status", "REJECTED").await()
            
        val ref = db.collection("mountains").document(mountainId)
        ref.update(
            "pendingCalibrationsCount", FieldValue.increment(-1),
            "updatedAt", Timestamp.now()
        ).await()
        
        synchronize()
    }

    suspend fun applyAdjustedGpsCalibration(submissionId: String, mountainId: String, lat: Double, lng: Double, region: String) = withContext(Dispatchers.IO) {
        // Update the submission with new coordinates and mark as approved
        db.collection("coordinate_submissions").document(submissionId)
            .update(mapOf(
                "latitude" to lat,
                "longitude" to lng,
                "region" to region,
                "status" to "APPROVED"
            )).await()
            
        val ref = db.collection("mountains").document(mountainId)
        val updateMap = mapOf<String, Any?>(
            "latitude" to lat,
            "longitude" to lng,
            "region" to region,
            "isVerifiedByCommunity" to true,
            "pendingCalibrationsCount" to 0,
            "updatedAt" to Timestamp.now()
        )
        ref.update(updateMap).await()
        
        // Mark all other pending submissions for this mountain as DUPLICATE
        val snapshot = db.collection("coordinate_submissions")
            .whereEqualTo("mountainId", mountainId)
            .whereEqualTo("status", "PENDING")
            .get().await()
        
        for (doc in snapshot.documents) {
            if (doc.id != submissionId) {
                doc.reference.update("status", "DUPLICATE").await()
            }
        }
        
        synchronize()
    }

    suspend fun mergeMountain(duplicateId: String, targetId: String) = withContext(Dispatchers.IO) {
        val snapshot = db.collectionGroup("hikeLogs")
            .whereEqualTo("mountainId", duplicateId)
            .get()
            .await()
        
        for (doc in snapshot.documents) {
            doc.reference.update("mountainId", targetId).await()
        }
        
        db.collection("mountains").document(duplicateId).delete().await()
        mountainDao.deleteMountainById(duplicateId)
    }

    // MARK: - Hike Logs Operations
    suspend fun saveHikeLog(log: HikeLog): String = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("Not signed in")
        log.userId = userId
        val docRef = db.collection("users").document(userId).collection("hikeLogs").document()
        log.id = docRef.id
        docRef.set(log.toFirestoreMap()).await()
        hikeLogDao.upsertLog(log)
        docRef.id
    }

    suspend fun getUserHikeLogs(): List<HikeLog> = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext emptyList()
        // Start background sync if not already done
        GlobalScope.launch(Dispatchers.IO) {
            syncUserHikeLogs(userId)
        }
        hikeLogDao.getLogsByUser(userId)
    }

    fun observeUserHikeLogs(): Flow<List<HikeLog>> {
        val userId = auth.currentUser?.uid ?: return kotlinx.coroutines.flow.emptyFlow()
        // Start background sync
        GlobalScope.launch(Dispatchers.IO) {
            syncUserHikeLogs(userId)
        }
        // Return Flow from local DB
        return hikeLogDao.observeLogsByUser(userId)
    }

    private suspend fun syncUserHikeLogs(userId: String) {
        try {
            val snapshot = db.collection("users").document(userId).collection("hikeLogs")
                .get()
                .await()
            val remoteLogs = snapshot.documents.mapNotNull { doc ->
                doc.toHikeLogSafely()
            }
            hikeLogDao.deleteAllLogsForUser(userId)
            hikeLogDao.upsertLogs(remoteLogs)
        } catch (e: Exception) {
            Log.e("MountainRepository", "Error syncing hike logs: ${e.localizedMessage}")
        }
    }

    suspend fun deleteHikeLog(logId: String) = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("Not signed in")
        db.collection("users").document(userId).collection("hikeLogs").document(logId)
            .delete()
            .await()
        hikeLogDao.deleteLogById(logId)
    }
}
