package com.devmarkabrasaldo.PataGilid

import android.app.Application
import com.devmarkabrasaldo.PataGilid.di.AppContainer

class PataGilidApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
