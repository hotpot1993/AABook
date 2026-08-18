package com.aa.ledger.data.remote

import android.content.Context
import com.aa.ledger.data.local.AppDatabase
import com.aa.ledger.data.local.entity.*
import com.aa.ledger.data.remote.dto.*
import com.aa.ledger.data.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository
) {
    private val db get() = AppDatabase.getInstance(context)

    /** Clear all ledgers owned by current user on cloud. */
    suspend fun clearMyCloudLedgers(): Result<Int> {
        if (!authRepository.isLoggedIn) return Result.failure(Exception("未登录"))
        return authRepository.clearMyLedgers()
    }

    /** Upload a local ledger to cloud. Returns the cloud ledger ID and invite code. */
    suspend fun uploadLedger(ledger: LedgerEntity): Result<Pair<Int, String>> {
        if (!authRepository.isLoggedIn) return Result.failure(Exception("未登录"))
        return authRepository.createLedger(
            CreateLedgerRequest(name = ledger.name, description = ledger.description,
                coverType = ledger.coverType, baseCurrency = ledger.baseCurrency,
                budgetAmount = ledger.budgetAmount)
        ).map { Pair(it.ledger.id, it.ledger.inviteCode) }
    }

    suspend fun pullLedgers(): Result<Int> {
        if (!authRepository.isLoggedIn) return Result.failure(Exception("未登录"))
        return try {
            val response = authRepository.getLedgers().getOrThrow()
            var count = 0
            for (cl in response.ledgers) {
                try {
                    // Pull detail first — only create local ledger if successful
                    val detail = authRepository.getLedgerDetail(cl.id).getOrThrow()
                    if (detail.ledger.members.isEmpty()) continue

                    withContext(Dispatchers.IO) {
                        val memberDao = db.memberDao()
                        val expenseDao = db.expenseDao()
                        val splitDao = db.expenseSplitDao()

                        // Create local ledger
                        val totalExpense = detail.ledger.expenses.sumOf { it.totalAmountCny }
                        val localId = db.ledgerDao().insertLedger(LedgerEntity(
                            id = 0, name = "${cl.name} ☁️",
                            description = cl.description, coverType = cl.coverType,
                            baseCurrency = cl.baseCurrency, budgetAmount = cl.budgetAmount,
                            createdAt = System.currentTimeMillis()))

                        // Map cloud userId → local memberId
                        val uid2mid = mutableMapOf<Int, Long>()
                        for (cm in detail.ledger.members) {
                            val name = cm.nickname.ifBlank { "成员${cm.userId}" }
                            val mid = memberDao.insertMember(MemberEntity(ledgerId = localId, name = name))
                            uid2mid[cm.userId] = mid
                        }
                        val fallback = uid2mid.values.firstOrNull() ?: return@withContext

                        // Import expenses with splits
                        for (ce in detail.ledger.expenses) {
                            val payerId = uid2mid[ce.paidByMemberId] ?: fallback
                            val expId = expenseDao.insertExpense(ExpenseEntity(
                                ledgerId = localId, title = ce.title,
                                totalAmountCny = ce.totalAmountCny, originalCurrency = ce.originalCurrency,
                                originalAmount = ce.originalAmount, exchangeRate = ce.exchangeRate,
                                category = ce.category, paidByMemberId = payerId,
                                note = ce.note, createdAt = System.currentTimeMillis()))
                            val splits = ce.splits.map { cs ->
                                ExpenseSplitEntity(expenseId = expId,
                                    memberId = uid2mid[cs.memberId] ?: fallback,
                                    shareType = cs.shareType, shareValue = cs.shareValue, amountCny = cs.amountCny)
                            }
                            splitDao.insertSplits(splits)
                        }
                    }
                    count++
                } catch (e: Exception) {
                    // Skip failed ledgers, continue with others
                }
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(Exception("下载失败：${e.message ?: e.toString()}"))
        }
    }
}
