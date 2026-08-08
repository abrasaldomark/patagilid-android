package com.devmarkabrasaldo.PataGilid.ui.screens.climbs

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.devmarkabrasaldo.PataGilid.data.repository.MountainRepository
import com.devmarkabrasaldo.PataGilid.data.repository.PhotoUploadService
import com.devmarkabrasaldo.PataGilid.data.repository.SelectedPhotoAsset
import com.devmarkabrasaldo.PataGilid.domain.models.HikeLog
import com.devmarkabrasaldo.PataGilid.domain.models.Mountain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HikeLogViewModel(
    private val mountainRepository: MountainRepository,
    private val photoUploadService: PhotoUploadService,
    private val mountainId: String
) : ViewModel() {

    val mountain = MutableStateFlow<Mountain?>(null)
    val selectedAssets = MutableStateFlow<List<SelectedPhotoAsset>>(emptyList())
    
    val trailName = MutableStateFlow("")
    val didSummit = MutableStateFlow(true)
    val isTraverse = MutableStateFlow(false)
    val exitTrailName = MutableStateFlow("")
    val startDate = MutableStateFlow(System.currentTimeMillis() - 86400000L) // Yesterday
    val endDate = MutableStateFlow(System.currentTimeMillis())
    
    val isSubmitting = MutableStateFlow(false)
    val uploadProgress = MutableStateFlow<String?>(null)
    val errorMessage = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            mountain.value = mountainRepository.getMountain(mountainId)
        }
    }

    fun onPhotosSelected(uris: List<Uri>) {
        viewModelScope.launch {
            val resolvedList = mutableListOf<SelectedPhotoAsset>()
            for (uri in uris) {
                val asset = photoUploadService.resolveAssetFromUri(uri)
                if (asset != null && !selectedAssets.value.contains(asset)) {
                    resolvedList.add(asset)
                }
            }
            selectedAssets.value = selectedAssets.value + resolvedList
        }
    }

    fun removePhoto(asset: SelectedPhotoAsset) {
        selectedAssets.value = selectedAssets.value.filter { it != asset }
    }

    fun submitHikeLog(onSuccess: () -> Unit) {
        viewModelScope.launch {
            isSubmitting.value = true
            errorMessage.value = null
            try {
                var drivePhotoUrls: List<String> = emptyList()
                if (selectedAssets.value.isNotEmpty()) {
                    uploadProgress.value = "Uploading ${selectedAssets.value.size} untouched photo(s) to Google Drive..."
                    drivePhotoUrls = photoUploadService.uploadPhotos(selectedAssets.value)
                }

                uploadProgress.value = "Saving summit record to Cloud Firestore..."
                val log = HikeLog(
                    mountainId = mountainId,
                    dateTimeStart = startDate.value,
                    dateTimeEnd = endDate.value,
                    didSummit = didSummit.value,
                    photoUrls = drivePhotoUrls,
                    trailName = trailName.value.ifBlank { "Main Trail" },
                    isTraverse = isTraverse.value,
                    exitTrailName = if (isTraverse.value) exitTrailName.value else null
                )
                mountainRepository.saveHikeLog(log)
                isSubmitting.value = false
                onSuccess()
            } catch (e: Exception) {
                isSubmitting.value = false
                errorMessage.value = "Failed to save climb: ${e.localizedMessage}"
            }
        }
    }

    class Factory(
        private val mountainRepository: MountainRepository,
        private val photoUploadService: PhotoUploadService,
        private val mountainId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HikeLogViewModel(mountainRepository, photoUploadService, mountainId) as T
        }
    }
}
