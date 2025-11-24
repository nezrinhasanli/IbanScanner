package com.example.ibanscanner

import android.app.Application
import io.scanbot.sdk.ScanbotSDKInitializer

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ScanbotSDKInitializer()
            // .license(this, YOUR_LICENSE_KEY)  // optional if you have a license
            .initialize(this)
    }
}
