package com.aa.ledger.worker

import android.content.Context
import androidx.work.*
import com.aa.ledger.data.local.AppDatabase
import com.aa.ledger.util.NotificationHelper
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class SettlementReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getInstance(applicationContext)
            val ledgers = db.ledgerDao().getAllLedgers().first()
            val settlementDao = db.settlementDao()

            for (ledger in ledgers) {
                val settlements = settlementDao.getSettlementsByLedgerSync(ledger.id)
                val unpaidAmount = settlements.filter { !it.isPaid }.sumOf { it.amountCny }
                if (unpaidAmount > 0.01) {
                    NotificationHelper.showSettlementReminder(
                        applicationContext,
                        ledger.name,
                        unpaidAmount,
                        ledger.id
                    )
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SettlementReminderWorker>(
                24, TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "settlement_reminder",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
