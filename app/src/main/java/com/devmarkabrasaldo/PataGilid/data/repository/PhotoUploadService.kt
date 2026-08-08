package com.devmarkabrasaldo.PataGilid.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.devmarkabrasaldo.PataGilid.data.remote.GoogleDriveService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class SelectedPhotoAsset(
    val uri: Uri,
    val data: ByteArray,
    val fileExtension: String,
    val mimeType: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SelectedPhotoAsset
        if (uri != other.uri) return false
        return data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = uri.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

class PhotoUploadService(
    private val context: Context,
    private val googleDriveService: GoogleDriveService
) {

    suspend fun resolveAssetFromUri(uri: Uri): SelectedPhotoAsset? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
            var ext = "jpg"
            if (mimeType.contains("png")) ext = "png"
            else if (mimeType.contains("heif") || mimeType.contains("heic")) ext = "heic"
            else if (mimeType.contains("webp")) ext = "webp"

            var displayName = UUID.randomUUID().toString()
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx >= 0) {
                        displayName = cursor.getString(nameIdx) ?: displayName
                        if (displayName.contains(".")) {
                            ext = displayName.substringAfterLast(".").lowercase()
                        }
                    }
                }
            }

            val inputStream = contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes() ?: return@withContext null
            inputStream.close()

            Log.d("PhotoUploadService", "📦 Successfully loaded raw library file '.$ext' (${bytes.size / 1024} KB) without transcoding!")
            return@withContext SelectedPhotoAsset(uri, bytes, ext, mimeType)
        } catch (e: Exception) {
            Log.e("PhotoUploadService", "Failed to extract photo data from Uri: ${e.message}", e)
        }
        null
    }

    suspend fun uploadPhotos(assets: List<SelectedPhotoAsset>): List<String> = withContext(Dispatchers.IO) {
        if (assets.isEmpty()) return@withContext emptyList()

        val downloadUrls = mutableListOf<String>()
        val assetsToUpload = assets.take(50) // Support up to 50 high-res photos per climb log

        for ((index, asset) in assetsToUpload.withIndex()) {
            val photoName = "climb_${System.currentTimeMillis()}_${index + 1}"
            try {
                val driveViewUrl = googleDriveService.uploadPhotoAsset(
                    data = asset.data,
                    fileName = photoName,
                    fileExtension = asset.fileExtension,
                    mimeType = asset.mimeType
                )
                downloadUrls.add(driveViewUrl)
                saveToLocalDiskCache(asset.data, driveViewUrl)
                Log.d("PhotoUploadService", "📸 [PhotoUploadService] Successfully stored untouched (${asset.fileExtension.uppercase()}) photo #${index + 1} in Google Drive!")
            } catch (e: Exception) {
                Log.e("PhotoUploadService", "❌ [PhotoUploadService] Failed to upload photo #${index + 1}: ${e.localizedMessage}", e)
                throw e
            }
        }
        downloadUrls
    }

    private fun saveToLocalDiskCache(data: ByteArray, url: String) {
        try {
            val cacheDir = File(context.cacheDir, "climb_photo_cache")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val safeFileName = url.hashCode().toString() + ".cache"
            val cacheFile = File(cacheDir, safeFileName)
            FileOutputStream(cacheFile).use { out -> out.write(data) }
        } catch (e: Exception) {
            Log.w("PhotoUploadService", "Could not write photo to local cache: ${e.message}")
        }
    }
}
