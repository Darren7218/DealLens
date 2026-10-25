package com.deallens.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DealLensApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        com.deallens.app.worker.VoucherReminderScheduler.schedulePeriodicCheck(this)
    }
}
