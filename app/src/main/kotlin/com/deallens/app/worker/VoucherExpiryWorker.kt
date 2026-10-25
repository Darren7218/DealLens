package com.deallens.app.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.deallens.core.domain.repository.VoucherProfile
import com.deallens.core.domain.repository.VoucherRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.Instant

class VoucherExpiryWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface VoucherWorkerEntryPoint {
        fun voucherRepository(): VoucherRepository
    }

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                VoucherWorkerEntryPoint::class.java
            )
            val voucherRepo = entryPoint.voucherRepository()
            val now = Instant.now()
            val expiringVouchers = voucherRepo.getVouchersExpiringSoon(now, withinHours = 48)

            if (expiringVouchers.isNotEmpty()) {
                sendExpiryNotification(expiringVouchers)
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun sendExpiryNotification(vouchers: List<VoucherProfile>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionGranted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!permissionGranted) {
                return
            }
        }

        try {
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val channelId = "voucher_reminders"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Voucher Expiry Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Reminds when saved vouchers are nearing expiration"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val title = if (vouchers.size == 1) {
                "Voucher Expiring Soon: ${vouchers.first().name}"
            } else {
                "${vouchers.size} Vouchers Expiring Soon"
            }

            val text = vouchers.joinToString(", ") { it.name }

            val notification = NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(1001, notification)
        } catch (e: SecurityException) {
            // Handle permission denial gracefully without crashing
        }
    }
}
