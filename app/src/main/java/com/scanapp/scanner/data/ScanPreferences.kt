package com.scanapp.scanner.data

import android.content.Context

enum class AutoCleanupPeriod(val days: Int?, val displayName: String) {
    NEVER(null, "永不"),
    ONE_DAY(1, "1 天后"),
    SEVEN_DAYS(7, "7 天后"),
    THIRTY_DAYS(30, "30 天后"),
    NINETY_DAYS(90, "90 天后"),
}

class ScanPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("scan_preferences", Context.MODE_PRIVATE)

    var privacyMode: Boolean
        get() = preferences.getBoolean(KEY_PRIVACY_MODE, false)
        set(value) {
            preferences.edit().putBoolean(KEY_PRIVACY_MODE, value).apply()
        }

    var autoCleanupPeriod: AutoCleanupPeriod
        get() = runCatching {
            AutoCleanupPeriod.valueOf(
                preferences.getString(KEY_AUTO_CLEANUP, AutoCleanupPeriod.NEVER.name)
                    ?: AutoCleanupPeriod.NEVER.name,
            )
        }.getOrDefault(AutoCleanupPeriod.NEVER)
        set(value) {
            preferences.edit().putString(KEY_AUTO_CLEANUP, value.name).apply()
        }

    private companion object {
        const val KEY_PRIVACY_MODE = "privacy_mode"
        const val KEY_AUTO_CLEANUP = "auto_cleanup"
    }
}
