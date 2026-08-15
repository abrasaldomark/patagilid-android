package com.devmarkabrasaldo.PataGilid.di

import android.content.Context
import com.devmarkabrasaldo.PataGilid.data.local.PataGilidDatabase
import com.devmarkabrasaldo.PataGilid.data.remote.GoogleDriveService
import com.devmarkabrasaldo.PataGilid.data.repository.AuthRepository
import com.devmarkabrasaldo.PataGilid.data.repository.MountainListRepository
import com.devmarkabrasaldo.PataGilid.data.repository.MountainRepository
import com.devmarkabrasaldo.PataGilid.data.repository.MountainSyncService
import com.devmarkabrasaldo.PataGilid.data.repository.PhotoUploadService
import com.devmarkabrasaldo.PataGilid.data.repository.UserMountainPhotoService

class AppContainer(val context: Context) {
    val authRepository: AuthRepository by lazy { AuthRepository(context) }
    val database: PataGilidDatabase by lazy { PataGilidDatabase.getDatabase(context) }
    val syncService: MountainSyncService by lazy { MountainSyncService(database.mountainDao()) }
    val mountainRepository: MountainRepository by lazy { MountainRepository(database.mountainDao(), syncService) }
    val mountainListRepository: MountainListRepository by lazy { MountainListRepository(database.mountainListDao()) }
    val googleDriveService: GoogleDriveService by lazy { GoogleDriveService(authRepository) }
    val photoUploadService: PhotoUploadService by lazy { PhotoUploadService(context, googleDriveService) }
    val userMountainPhotoService: UserMountainPhotoService by lazy { UserMountainPhotoService(context, authRepository) }
}
