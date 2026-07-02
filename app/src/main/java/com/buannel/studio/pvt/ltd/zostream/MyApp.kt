package com.buannel.studio.pvt.ltd.zostream

import android.app.Application
import com.buannel.studio.pvt.ltd.zostream.api.Api
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Retrofit API
        Api.init(this)
    }
}
