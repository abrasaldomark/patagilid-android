package com.devmarkabrasaldo.PataGilid.data.remote

import android.util.Log
import com.devmarkabrasaldo.PataGilid.data.repository.AuthRepository
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class GoogleDriveService(private val authRepository: AuthRepository) {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val driveApiBaseUrl = "https://www.googleapis.com/drive/v3/files"
    private val driveUploadUrl = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"

    suspend fun getOrCreatePhotoFolder(): String? = withContext(Dispatchers.IO) {
        val token = authRepository.fetchDriveOAuthToken() ?: run {
            Log.e("GoogleDriveService", "No OAuth token available for Google Drive.")
            return@withContext null
        }
        val folderName = "PataGilid Climb Memories"
        val query = "name='$folderName' and mimeType='application/vnd.google-apps.folder' and trashed=false"

        try {
            // 1. Search for existing folder
            val searchUrl = "$driveApiBaseUrl?q=${java.net.URLEncoder.encode(query, "UTF-8")}&fields=files(id,name)"
            val searchRequest = Request.Builder()
                .url(searchUrl)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            client.newCall(searchRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val json = gson.fromJson(response.body?.string(), JsonObject::class.java)
                    val files = json.getAsJsonArray("files")
                    if (files != null && files.size() > 0) {
                        val folderId = files.get(0).asJsonObject.get("id").asString
                        Log.d("GoogleDriveService", "📁 Found existing PataGilid photo folder ID: $folderId")
                        return@withContext folderId
                    }
                }
            }

            // 2. Create new photo folder if not found
            val metadata = JsonObject().apply {
                addProperty("name", folderName)
                addProperty("mimeType", "application/vnd.google-apps.folder")
            }
            val createBody = gson.toJson(metadata).toRequestBody("application/json".toMediaTypeOrNull())
            val createRequest = Request.Builder()
                .url(driveApiBaseUrl)
                .addHeader("Authorization", "Bearer $token")
                .post(createBody)
                .build()

            client.newCall(createRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val json = gson.fromJson(response.body?.string(), JsonObject::class.java)
                    val newId = json.get("id").asString
                    Log.d("GoogleDriveService", "🎉 Successfully created new photo folder in Google Drive: $newId")
                    return@withContext newId
                } else {
                    Log.e("GoogleDriveService", "Failed to create folder: ${response.body?.string()}")
                }
            }
        } catch (e: Exception) {
            Log.e("GoogleDriveService", "Exception in getOrCreatePhotoFolder: ${e.message}", e)
        }
        null
    }

    suspend fun uploadPhotoAsset(
        data: ByteArray,
        fileName: String,
        fileExtension: String,
        mimeType: String
    ): String = withContext(Dispatchers.IO) {
        val token = authRepository.fetchDriveOAuthToken()
            ?: throw IOException("Missing Google Drive OAuth token. Please ensure Drive access was granted during sign in.")
        val folderId = getOrCreatePhotoFolder()
            ?: throw IOException("Could not resolve or create Google Drive photo folder.")

        val fullFileName = "$fileName.$fileExtension"
        val metadata = JsonObject().apply {
            addProperty("name", fullFileName)
            val parents = com.google.gson.JsonArray().apply { add(folderId) }
            add("parents", parents)
        }

        val metadataBody = gson.toJson(metadata).toRequestBody("application/json; charset=UTF-8".toMediaTypeOrNull())
        val fileBody = data.toRequestBody(mimeType.toMediaTypeOrNull())

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addPart(metadataBody)
            .addFormDataPart("file", fullFileName, fileBody)
            .build()

        val request = Request.Builder()
            .url(driveUploadUrl)
            .addHeader("Authorization", "Bearer $token")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorMsg = response.body?.string() ?: "Unknown HTTP Error ${response.code}"
                throw IOException("Google Drive upload failed: $errorMsg")
            }
            val json = gson.fromJson(response.body?.string(), JsonObject::class.java)
            val fileId = json.get("id").asString
            
            // Set reader permissions for public viewing link
            try {
                val permUrl = "$driveApiBaseUrl/$fileId/permissions"
                val permBody = JsonObject().apply {
                    addProperty("role", "reader")
                    addProperty("type", "anyone")
                }.let { gson.toJson(it).toRequestBody("application/json".toMediaTypeOrNull()) }
                
                val permReq = Request.Builder()
                    .url(permUrl)
                    .addHeader("Authorization", "Bearer $token")
                    .post(permBody)
                    .build()
                client.newCall(permReq).execute().close()
            } catch (e: Exception) {
                Log.w("GoogleDriveService", "Could not modify public permissions for file $fileId: ${e.message}")
            }

            val driveViewUrl = "https://drive.google.com/uc?id=$fileId&export=view"
            Log.d("GoogleDriveService", "📸 Successfully stored untouched ($fileExtension) photo in Google Drive! URL: $driveViewUrl")
            return@withContext driveViewUrl
        }
    }
}
