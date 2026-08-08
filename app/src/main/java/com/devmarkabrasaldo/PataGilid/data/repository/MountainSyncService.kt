package com.devmarkabrasaldo.PataGilid.data.repository

import android.util.Log
import com.devmarkabrasaldo.PataGilid.data.local.MountainDao
import com.devmarkabrasaldo.PataGilid.domain.models.Mountain
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date

class MountainSyncService(private val mountainDao: MountainDao) {
    private val db = FirebaseFirestore.getInstance()

    suspend fun synchronizeWithFirestore(onProgress: ((Int, Int) -> Unit)? = null) = withContext(Dispatchers.IO) {
        try {
            val latestLocalTimestamp = mountainDao.getLatestUpdatedAtTimestamp() ?: 0L
            Log.d("MountainSync", "🛡️ [Delta-Sync] Current Room database up-to-date as of epoch millis: $latestLocalTimestamp")

            val snapshot = if (latestLocalTimestamp > 0L) {
                Log.d("MountainSync", "🌐 [Delta-Sync] Querying Firestore for mountains modified after $latestLocalTimestamp...")
                db.collection("mountains")
                    .whereGreaterThan("updatedAt", Timestamp(Date(latestLocalTimestamp)))
                    .get()
                    .await()
            } else {
                Log.d("MountainSync", "📥 [Delta-Sync] First-time setup: Initializing local Room database from Cloud Firestore...")
                db.collection("mountains")
                    .get()
                    .await()
            }

            val modCount = snapshot.documents.size
            Log.d("MountainSync", "⚡️ [Delta-Sync] Firestore returned $modCount modified/new mountain documents (Read billing: $modCount reads)!")

            if (modCount == 0) return@withContext

            var appliedCount = 0
            val processedIds = mutableSetOf<String>()
            val batchToInsert = mutableListOf<Mountain>()

            for (doc in snapshot.documents) {
                val remotePeak = doc.toMountainSafely() ?: continue
                val peakId = doc.id
                if (processedIds.contains(peakId)) {
                    continue
                }
                processedIds.add(peakId)
                remotePeak.id = peakId

                batchToInsert.add(remotePeak)
                appliedCount++

                if (batchToInsert.size >= 30 || appliedCount == modCount) {
                    mountainDao.insertMountains(batchToInsert.toList())
                    batchToInsert.clear()
                    onProgress?.invoke(appliedCount, modCount)
                }
            }
        } catch (e: Exception) {
            Log.e("MountainSync", "❌ [Delta-Sync] Synchronization failed: ${e.localizedMessage}", e)
        }
    }
}
