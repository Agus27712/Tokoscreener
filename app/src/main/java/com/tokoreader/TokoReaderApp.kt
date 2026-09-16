package com.tokoreader

import android.app.Application
import android.util.Log
import com.tokoreader.di.AppContainer

/**
 * Custom Application class used to initialize our dependency container.
 */
class TokoReaderApp : Application() {

    // Instance of AppContainer that will be used by all Activities/Composables
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        Log.d("TokoReaderApp", "Application onCreate: Initializing DI container")
        container = AppContainer(this)
    }
}
