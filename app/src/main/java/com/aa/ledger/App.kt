package com.aa.ledger

import android.app.Application
import android.util.Log
import com.aa.ledger.util.NotificationHelper
import com.aa.ledger.worker.RateRefreshWorker
import com.aa.ledger.worker.SettlementReminderWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // 全局崩溃日志
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("AALedger", "FATAL CRASH", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // Create notification channels
        NotificationHelper.createChannels(this)

        // Schedule background workers
        RateRefreshWorker.schedule(this)
        SettlementReminderWorker.schedule(this)
    }
}
