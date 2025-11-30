package com.appcontrol.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.appcontrol.R
import com.appcontrol.data.database.AppDatabase
import com.appcontrol.data.repository.AppRepository
import com.appcontrol.ui.MainActivity
import com.appcontrol.util.UsageTracker
import kotlinx.coroutines.*

class AppMonitorService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var repository: AppRepository
    private lateinit var usageTracker: UsageTracker
    private var monitoringJob: Job? = null

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "app_monitor_channel"
    }

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getDatabase(applicationContext)
        repository = AppRepository(
            database.appSettingsDao(),
            database.appUsageDao(),
            applicationContext
        )
        usageTracker = UsageTracker(applicationContext, repository)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        startMonitoring()
        return START_STICKY
    }

    private fun startMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = serviceScope.launch {
            while (isActive) {
                try {
                    // Track app usage every 5 seconds
                    usageTracker.trackCurrentAppUsage()

                    // Clean old records once a day
                    if (shouldCleanOldRecords()) {
                        repository.cleanOldUsageRecords()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(5000) // Check every 5 seconds
            }
        }
    }

    private var lastCleanupDate = 0L

    private fun shouldCleanOldRecords(): Boolean {
        val today = System.currentTimeMillis() / (1000 * 60 * 60 * 24)
        if (today > lastCleanupDate) {
            lastCleanupDate = today
            return true
        }
        return false
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AppControl attivo")
            .setContentText("Monitoraggio app in corso")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Monitor Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitora l'utilizzo delle app"
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        monitoringJob?.cancel()
        serviceScope.cancel()
    }
}
package com.appcontrol.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import androidx.lifecycle.ViewModelProvider
import com.appcontrol.data.database.AppDatabase
import com.appcontrol.data.repository.AppRepository
import com.appcontrol.util.BlockChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppBlockAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var repository: AppRepository
    private lateinit var blockChecker: BlockChecker
    private var lastPackageName: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        val database = AppDatabase.getDatabase(applicationContext)
        repository = AppRepository(
            database.appSettingsDao(),
            database.appUsageDao(),
            applicationContext
        )
        blockChecker = BlockChecker(applicationContext, repository)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

            // Avoid checking the same app multiple times
            if (packageName == lastPackageName) return
            lastPackageName = packageName

            // Don't block our own app
            if (packageName == this.packageName) return

            // Check if app should be blocked
            serviceScope.launch {
                val blockResult = blockChecker.shouldBlockApp(packageName)
                if (blockResult.shouldBlock) {
                    blockApp(blockResult.reason ?: "App bloccata")
                }
            }
        }
    }

    private fun blockApp(reason: String) {
        // Go to home screen
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)

        // Show block overlay
        val overlayIntent = Intent(this, BlockOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("BLOCK_REASON", reason)
        }
        startActivity(overlayIntent)
    }

    override fun onInterrupt() {
        // Handle service interruption
    }
}

