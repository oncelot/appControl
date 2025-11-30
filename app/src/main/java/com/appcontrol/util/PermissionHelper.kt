package com.appcontrol.util

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings

object PermissionHelper {

    fun hasUsageStatsPermission(context: Context): Boolean {
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

    fun requestUsageStatsPermission(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun requestOverlayPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:${context.packageName}")
            )
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }

    fun isAccessibilityServiceEnabled(context: Context, serviceName: String): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = enabledServices.split(":")
        return colonSplitter.any {
            it.equals("${context.packageName}/$serviceName", ignoreCase = true)
        }
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}
package com.appcontrol.util

import android.content.Context
import com.appcontrol.data.repository.AppRepository
import java.text.SimpleDateFormat
import java.util.*

data class BlockResult(
    val shouldBlock: Boolean,
    val reason: String? = null
)

class BlockChecker(
    private val context: Context,
    private val repository: AppRepository
) {

    suspend fun shouldBlockApp(packageName: String): BlockResult {
        val settings = repository.getAppSettings(packageName) ?: return BlockResult(false)

        if (!settings.isBlocked) {
            return BlockResult(false)
        }

        // Check time-based blocking
        val timeBlockResult = checkTimeBlock(settings.blockStartTime, settings.blockEndTime)
        if (timeBlockResult.shouldBlock) {
            return timeBlockResult
        }

        // Check usage-based blocking
        val usageBlockResult = checkUsageBlock(packageName, settings.maxUsageMinutes)
        if (usageBlockResult.shouldBlock) {
            return usageBlockResult
        }

        return BlockResult(false)
    }

    private fun checkTimeBlock(startTime: String?, endTime: String?): BlockResult {
        if (startTime == null || endTime == null) {
            return BlockResult(false)
        }

        try {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val now = Calendar.getInstance()
            val currentTime = sdf.format(now.time)

            val start = sdf.parse(startTime)
            val end = sdf.parse(endTime)
            val current = sdf.parse(currentTime)

            if (start != null && end != null && current != null) {
                val isBlocked = if (start.before(end)) {
                    // Normal range: e.g., 09:00 to 18:00
                    current.after(start) && current.before(end) ||
                    current == start || current == end
                } else {
                    // Overnight range: e.g., 22:00 to 06:00
                    current.after(start) || current.before(end)
                }

                if (isBlocked) {
                    return BlockResult(
                        shouldBlock = true,
                        reason = "App bloccata. Orario non consentito ($startTime - $endTime)"
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return BlockResult(false)
    }

    private suspend fun checkUsageBlock(packageName: String, maxUsageMinutes: Int): BlockResult {
        val todayUsageMillis = repository.getTodayUsage(packageName)
        val maxUsageMillis = maxUsageMinutes * 60 * 1000L

        if (todayUsageMillis >= maxUsageMillis) {
            val usedMinutes = todayUsageMillis / (60 * 1000)
            return BlockResult(
                shouldBlock = true,
                reason = "Tempo massimo raggiunto ($usedMinutes/$maxUsageMinutes minuti)"
            )
        }

        return BlockResult(false)
    }
}

