package com.appcontrol.util

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import com.appcontrol.data.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UsageTracker(
    private val context: Context,
    private val repository: AppRepository
) {

    private var lastTrackedPackage: String? = null
    private var lastTrackTime = System.currentTimeMillis()

    suspend fun trackCurrentAppUsage() = withContext(Dispatchers.IO) {
        val currentPackage = getCurrentForegroundApp() ?: return@withContext
        val currentTime = System.currentTimeMillis()

        // Check if this is a blocked app
        val settings = repository.getAppSettings(currentPackage)
        if (settings?.isBlocked == true) {
            // If same app as last time, calculate elapsed time
            if (currentPackage == lastTrackedPackage) {
                val elapsedMillis = currentTime - lastTrackTime
                if (elapsedMillis > 0 && elapsedMillis < 10000) { // Max 10 seconds between checks
                    repository.updateUsageTime(currentPackage, elapsedMillis)
                }
            }
        }

        lastTrackedPackage = currentPackage
        lastTrackTime = currentTime
    }

    private fun getCurrentForegroundApp(): String? {
        if (!hasUsageStatsPermission()) {
            return null
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val currentTime = System.currentTimeMillis()

        // Query events from last 1 second
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            currentTime - 1000,
            currentTime
        )

        if (stats.isNullOrEmpty()) {
            return null
        }

        // Get the app with most recent usage
        val sortedStats = stats.sortedByDescending { it.lastTimeUsed }
        return sortedStats.firstOrNull()?.packageName
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOpsManager.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }
}

