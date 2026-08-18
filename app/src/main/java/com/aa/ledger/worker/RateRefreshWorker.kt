package com.aa.ledger.worker

import android.content.Context
import androidx.work.*
import com.aa.ledger.data.local.AppDatabase
import com.aa.ledger.data.remote.ExchangeRateApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class RateRefreshWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val api = Retrofit.Builder()
                .baseUrl("https://v6.exchangerate-api.com/v6/${com.aa.ledger.BuildConfig.EXCHANGE_RATE_API_KEY}/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ExchangeRateApi::class.java)

            val response = api.getRates()
            if (response.result == "success") {
                val db = AppDatabase.getInstance(applicationContext)
                val rates = response.conversion_rates.map { (code: String, rate: Double) ->
                    com.aa.ledger.data.local.entity.ExchangeRateEntity(
                        currencyCode = code,
                        rateToCny = 1.0.div(rate),
                        currencyName = code
                    )
                }
                withContext(Dispatchers.IO) {
                    db.exchangeRateDao().clearAllRates()
                    db.exchangeRateDao().insertRates(rates)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<RateRefreshWorker>(
                24, TimeUnit.HOURS,
                12, TimeUnit.HOURS
            ).setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "rate_refresh",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
