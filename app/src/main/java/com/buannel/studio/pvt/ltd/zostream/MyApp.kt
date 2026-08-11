package com.buannel.studio.pvt.ltd.zostream

import android.app.Application
import com.buannel.studio.pvt.ltd.zostream.api.Api
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // API is initialized only after official verification returns api_base_url.
        Api.init(this)
    }
}
