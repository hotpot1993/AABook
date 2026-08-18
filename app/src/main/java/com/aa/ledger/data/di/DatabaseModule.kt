package com.aa.ledger.data.di

import android.content.Context
import androidx.room.Room
import com.aa.ledger.BuildConfig
import com.aa.ledger.data.local.AppDatabase
import com.aa.ledger.data.local.dao.*
import com.aa.ledger.data.remote.ExchangeRateApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    // Note: fallbackToDestructiveMigration() is configured in AppDatabase.getInstance().
    // When adding new schema versions, add Migration objects and pass them to the
    // Room.databaseBuilder() chain before .build() in AppDatabase companion.
    // The Room schema JSON files are exported to app/schemas/ for version tracking.

    @Provides
    fun provideLedgerDao(db: AppDatabase): LedgerDao = db.ledgerDao()

    @Provides
    fun provideMemberDao(db: AppDatabase): MemberDao = db.memberDao()

    @Provides
    fun provideExpenseDao(db: AppDatabase): ExpenseDao = db.expenseDao()

    @Provides
    fun provideExpenseSplitDao(db: AppDatabase): ExpenseSplitDao = db.expenseSplitDao()

    @Provides
    fun provideExchangeRateDao(db: AppDatabase): ExchangeRateDao = db.exchangeRateDao()

    @Provides
    fun provideSettlementDao(db: AppDatabase): SettlementDao = db.settlementDao()

    @Provides
    @Singleton
    fun provideExchangeRateApi(): ExchangeRateApi {
        return Retrofit.Builder()
            .baseUrl("https://v6.exchangerate-api.com/v6/${BuildConfig.EXCHANGE_RATE_API_KEY}/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ExchangeRateApi::class.java)
    }
}
