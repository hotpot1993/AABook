package com.aa.ledger.util

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("aa_ledger_prefs", Context.MODE_PRIVATE)

    var defaultLedgerId: Long
        get() = prefs.getLong(KEY_DEFAULT_LEDGER, -1L)
        set(value) = prefs.edit().putLong(KEY_DEFAULT_LEDGER, value).apply()

    companion object {
        private const val KEY_DEFAULT_LEDGER = "default_ledger_id"
    }
}
