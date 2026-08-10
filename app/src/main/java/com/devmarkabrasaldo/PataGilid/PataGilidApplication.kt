package com.devmarkabrasaldo.PataGilid

import android.app.Application
import com.devmarkabrasaldo.PataGilid.di.AppContainer

import com.google.android.libraries.places.api.Places

class PataGilidApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, BuildConfig.GOOGLE_MAPS_API_KEY)
        }
    }
}
