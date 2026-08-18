package com.aa.ledger.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aa.ledger.data.local.dao.*
import com.aa.ledger.data.local.entity.*

@Database(
    entities = [
        LedgerEntity::class,
        MemberEntity::class,
        ExpenseEntity::class,
        ExpenseSplitEntity::class,
        ExchangeRateEntity::class,
        SettlementEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ledgerDao(): LedgerDao
    abstract fun memberDao(): MemberDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun expenseSplitDao(): ExpenseSplitDao
    abstract fun exchangeRateDao(): ExchangeRateDao
    abstract fun settlementDao(): SettlementDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE ledgers ADD COLUMN budgetAmount REAL NOT NULL DEFAULT 0.0")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aa_ledger.db"
                ).addMigrations(MIGRATION_1_2)
                 .fallbackToDestructiveMigration()
                 .build().also { INSTANCE = it }
            }
        }
    }
}
