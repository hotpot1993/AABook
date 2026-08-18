package com.aa.ledger.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.aa.ledger.MainActivity
import com.aa.ledger.R
import com.aa.ledger.data.local.AppDatabase
import com.aa.ledger.data.local.dao.ExpenseDao
import com.aa.ledger.data.local.dao.LedgerDao
import com.aa.ledger.data.local.dao.SettlementDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class OverviewWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val quickAddIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("navigate_to", "add_expense")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val quickAddPending = PendingIntent.getActivity(
                context, 0, quickAddIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val viewLedgerIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val viewLedgerPending = PendingIntent.getActivity(
                context, 1, viewLedgerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Load dynamic data from database
            val summaryText = try {
                runBlocking {
                    val db = AppDatabase.getInstance(context)
                    val ledgers = db.ledgerDao().getAllLedgers().first()
                    val settlementDao = db.settlementDao()

                    val totalLedgers = ledgers.size
                    var totalUnpaid = 0.0
                    for (ledger in ledgers) {
                        val settlements = settlementDao.getSettlementsByLedgerSync(ledger.id)
                        totalUnpaid += settlements.filter { !it.isPaid }.sumOf { it.amountCny }
                    }

                    if (totalLedgers == 0) "暂无账本"
                    else if (totalUnpaid > 0.01) "${totalLedgers}个账本 · 待结算¥${String.format("%.0f", totalUnpaid)}"
                    else "${totalLedgers}个账本 · 已全部结清"
                }
            } catch (_: Exception) {
                ""
            }

            val views = RemoteViews(context.packageName, R.layout.widget_overview).apply {
                setOnClickPendingIntent(R.id.widget_add_expense, quickAddPending)
                setOnClickPendingIntent(R.id.widget_view_ledger, viewLedgerPending)
                setTextViewText(R.id.widget_summary, summaryText)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
