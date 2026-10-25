package com.deallens.app.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object VoucherReminderScheduler {
    private const val WORK_NAME = "voucher_expiry_check"

    fun schedulePeriodicCheck(context: Context) {
        val request = PeriodicWorkRequestBuilder<VoucherExpiryWorker>(
            24, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
