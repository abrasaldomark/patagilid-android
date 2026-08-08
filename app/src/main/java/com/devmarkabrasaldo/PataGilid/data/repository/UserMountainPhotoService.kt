package com.devmarkabrasaldo.PataGilid.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Manages private, user-specific mountain cover photo URLs saved to the individual hiker's account and Google Drive album.
 * These cover photos are completely separate from the shared public catalog and only appear when the contributor is logged into their account.
 * 1:1 Parity with iOS UserMountainPhotoService.
 */
class UserMountainPhotoService(private val context: Context, private val authRepository: AuthRepository) {
    private val db = FirebaseFirestore.getInstance()
    private val _customPhotos = MutableStateFlow<Map<String, String>>(emptyMap())
    val customPhotos: StateFlow<Map<String, String>> = _customPhotos

    private var firestoreListener: ListenerRegistration? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        scope.launch {
            authRepository.currentUser.collect { user ->
                handleUserChange(user?.uid)
            }
        }
    }

    private suspend fun handleUserChange(userId: String?) = withContext(Dispatchers.IO) {
        firestoreListener?.remove()
        firestoreListener = null

        if (userId == null) {
            _customPhotos.value = emptyMap()
            return@withContext
        }

        val prefs = getPrefs(userId)
        val cachedJson = prefs.getString("photos_map", "{}") ?: "{}"
        val initialMap = mutableMapOf<String, String>()
        try {
            val jsonObject = JSONObject(cachedJson)
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                initialMap[key] = jsonObject.getString(key)
            }
        } catch (e: Exception) {
            Log.e("UserPhotoService", "Error parsing cached photos: ${e.message}")
        }
        _customPhotos.value = initialMap

        firestoreListener = db.collection("users").document(userId).collection("mountainPhotos")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val updatedMap = mutableMapOf<String, String>()
                for (doc in snapshot.documents) {
                    val photoUrl = doc.getString("photoUrl")
                    if (photoUrl != null) {
                        updatedMap[doc.id] = photoUrl
                    }
                }

                _customPhotos.value = updatedMap
                saveToCache(userId, updatedMap)
            }
    }

    private fun saveToCache(userId: String, map: Map<String, String>) {
        try {
            val jsonObject = JSONObject()
            for ((k, v) in map) {
                jsonObject.put(k, v)
            }
            getPrefs(userId).edit().putString("photos_map", jsonObject.toString()).apply()
        } catch (e: Exception) {
            Log.e("UserPhotoService", "Error saving photos cache: ${e.message}")
        }
    }

    private fun getPrefs(userId: String): SharedPreferences {
        return context.getSharedPreferences("user_mountain_photos_$userId", Context.MODE_PRIVATE)
    }

    fun getPhotoUrl(mountainId: String): String? {
        return _customPhotos.value[mountainId]
    }

    suspend fun savePhoto(mountainId: String, photoUrl: String) = withContext(Dispatchers.IO) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("You must be signed in to save mountain photos to your account.")

        // Optimistic update in memory & cache
        val currentMap = _customPhotos.value.toMutableMap()
        currentMap[mountainId] = photoUrl
        _customPhotos.value = currentMap
        saveToCache(userId, currentMap)

        // Save to private user collection in Firestore
        val data = mapOf(
            "mountainId" to mountainId,
            "photoUrl" to photoUrl,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            db.collection("users").document(userId).collection("mountainPhotos").document(mountainId)
                .set(data, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e("UserPhotoService", "Failed to save photo to Firestore: ${e.message}", e)
            throw e
        }
    }
}
