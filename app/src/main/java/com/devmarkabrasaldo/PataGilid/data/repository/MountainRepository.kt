package com.devmarkabrasaldo.PataGilid.data.repository

import android.util.Log
import com.devmarkabrasaldo.PataGilid.data.local.MountainDao
import com.devmarkabrasaldo.PataGilid.domain.models.HikeLog
import com.devmarkabrasaldo.PataGilid.domain.models.Mountain
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MountainRepository(
    private val mountainDao: MountainDao,
    private val syncService: MountainSyncService
) {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    val allMountainsByElevation: Flow<List<Mountain>> = mountainDao.getAllApprovedMountainsByElevation()
    val allMountainsByName: Flow<List<Mountain>> = mountainDao.getAllApprovedMountainsByName()
    val allMountainsByRecent: Flow<List<Mountain>> = mountainDao.getAllApprovedMountainsByRecent()
    val unapprovedMountains: Flow<List<Mountain>> = mountainDao.getPendingApprovalMountains()
    val pendingGpsMountains: Flow<List<Mountain>> = mountainDao.getPendingGpsMountains()

    suspend fun synchronize() {
        syncService.synchronizeWithFirestore()
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
            "submittedAt" to Timestamp.now()
        )
        submissionRef.set(data).await()
        
        // Update pending GPS on mountain document in Firestore
        val mountainRef = db.collection("mountains").document(mountainId)
        val updateMap = mapOf<String, Any>(
            "pendingLatitude" to latitude,
            "pendingLongitude" to longitude,
            "pendingRegion" to region,
            "pendingContributorEmail" to (currentUser.email ?: ""),
            "pendingContributorName" to (currentUser.displayName ?: ""),
            "pendingVerifications" to 1,
            "pendingVerifierEmails" to listOf(currentUser.email ?: ""),
            "updatedAt" to Timestamp.now()
        )
        mountainRef.update(updateMap).await()
        synchronize()
    }

    suspend fun verifyPendingGps(mountainId: String) = withContext(Dispatchers.IO) {
        val email = auth.currentUser?.email ?: return@withContext
        val mountain = mountainDao.getMountainById(mountainId) ?: return@withContext
        if (mountain.pendingVerifierEmails.contains(email)) return@withContext

        val updatedList = mountain.pendingVerifierEmails + email
        val updatedCount = mountain.pendingVerifications + 1

        val mountainRef = db.collection("mountains").document(mountainId)
        val updateMap = mapOf<String, Any>(
            "pendingVerifications" to updatedCount,
            "pendingVerifierEmails" to updatedList,
            "updatedAt" to Timestamp.now()
        )
        mountainRef.update(updateMap).await()
        synchronize()
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

    suspend fun applyGpsCalibration(mountainId: String) = withContext(Dispatchers.IO) {
        val mountain = mountainDao.getMountainById(mountainId) ?: return@withContext
        val ref = db.collection("mountains").document(mountainId)
        val updateMap = mapOf<String, Any?>(
            "latitude" to mountain.pendingLatitude,
            "longitude" to mountain.pendingLongitude,
            "region" to (mountain.pendingRegion ?: mountain.region),
            "isVerifiedByCommunity" to true,
            "communityVerifications" to mountain.pendingVerifications,
            "pendingLatitude" to null,
            "pendingLongitude" to null,
            "pendingRegion" to null,
            "pendingVerifications" to 0,
            "pendingVerifierEmails" to emptyList<String>(),
            "updatedAt" to Timestamp.now()
        )
        ref.update(updateMap).await()
        synchronize()
    }

    suspend fun declineGPS(mountainId: String) = withContext(Dispatchers.IO) {
        val ref = db.collection("mountains").document(mountainId)
        val updateMap = mapOf<String, Any?>(
            "pendingLatitude" to null,
            "pendingLongitude" to null,
            "pendingRegion" to null,
            "pendingContributorEmail" to null,
            "pendingContributorName" to null,
            "pendingVerifications" to 0,
            "pendingVerifierEmails" to emptyList<String>(),
            "updatedAt" to Timestamp.now()
        )
        ref.update(updateMap).await()
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
        docRef.id
    }

    suspend fun getUserHikeLogs(): List<HikeLog> = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext emptyList()
        val snapshot = db.collection("users").document(userId).collection("hikeLogs")
            .orderBy("dateTimeEnd", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .await()
        snapshot.documents.mapNotNull { doc ->
            doc.toHikeLogSafely()
        }
    }

    suspend fun deleteHikeLog(logId: String) = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext
        db.collection("users").document(userId).collection("hikeLogs").document(logId).delete().await()
    }
}
