package com.yahyaoui.prayermode

import android.app.Application

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannels(this)
    }
}
