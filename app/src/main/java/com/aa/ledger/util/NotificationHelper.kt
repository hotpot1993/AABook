package com.aa.ledger.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.aa.ledger.MainActivity
import com.aa.ledger.R

object NotificationHelper {
    const val CHANNEL_SETTLEMENT = "settlement_reminders"
    const val CHANNEL_RATES = "rate_updates"

    fun createChannels(context: Context) {
        val settlementChannel = NotificationChannel(
            CHANNEL_SETTLEMENT,
            "结算提醒",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "待结算账本提醒"
        }

        val rateChannel = NotificationChannel(
            CHANNEL_RATES,
            "汇率更新",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "汇率自动刷新通知"
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(settlementChannel)
        manager.createNotificationChannel(rateChannel)
    }

    fun showSettlementReminder(
        context: Context,
        ledgerName: String,
        pendingAmount: Double,
        ledgerId: Long
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("navigate_to", "settlement_$ledgerId")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, ledgerId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SETTLEMENT)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("有待结算账本")
            .setContentText("「$ledgerName」待结算 ¥${"%.2f".format(pendingAmount)}，点击查看")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(ledgerId.toInt(), notification)
    }
}
