package com.appcontrol.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.LiveData
import com.appcontrol.data.dao.AppSettingsDao
import com.appcontrol.data.dao.AppUsageDao
import com.appcontrol.data.model.AppInfo
import com.appcontrol.data.model.AppSettings
import com.appcontrol.data.model.AppUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AppRepository(
    private val appSettingsDao: AppSettingsDao,
    private val appUsageDao: AppUsageDao,
    private val context: Context
) {

    val allAppSettings: LiveData<List<AppSettings>> = appSettingsDao.getAllAppSettings()

    // Get all installed apps
    suspend fun getAllInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        val currentPackageName = context.packageName

        packages
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 ||
                     (it.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0) }
            .filter { it.packageName != currentPackageName } // Exclude our app
            .map { appInfo ->
                val settings = appSettingsDao.getAppSettings(appInfo.packageName)
                AppInfo(
                    packageName = appInfo.packageName,
                    appName = appInfo.loadLabel(packageManager).toString(),
                    icon = appInfo.loadIcon(packageManager),
                    isBlocked = settings?.isBlocked ?: false
                )
            }
            .sortedBy { it.appName }
    }

    // App Settings operations
    suspend fun getAppSettings(packageName: String): AppSettings? {
        return appSettingsDao.getAppSettings(packageName)
    }

    suspend fun getBlockedApps(): List<AppSettings> {
        return appSettingsDao.getBlockedApps()
    }

    suspend fun insertOrUpdateAppSettings(appSettings: AppSettings) {
        appSettingsDao.insertOrUpdate(appSettings)
    }

    suspend fun deleteAppSettings(packageName: String) {
        appSettingsDao.deleteByPackageName(packageName)
    }

    // App Usage operations
    suspend fun getTodayUsage(packageName: String): Long {
        val today = getCurrentDate()
        return appUsageDao.getTotalUsageForDate(packageName, today) ?: 0L
    }

    suspend fun updateUsageTime(packageName: String, additionalMillis: Long) {
        val today = getCurrentDate()
        val currentUsage = appUsageDao.getUsageForDate(packageName, today)

        if (currentUsage != null) {
            val newTotal = currentUsage.totalUsageMillis + additionalMillis
            appUsageDao.updateUsageTime(packageName, today, newTotal)
        } else {
            appUsageDao.insert(
                AppUsage(
                    packageName = packageName,
                    date = today,
                    totalUsageMillis = additionalMillis
                )
            )
        }
    }

    suspend fun cleanOldUsageRecords() {
        val sevenDaysAgo = getDateDaysAgo(7)
        appUsageDao.deleteOldRecords(sevenDaysAgo)
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun getDateDaysAgo(days: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(calendar.time)
    }
}
package com.appcontrol.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.appcontrol.data.dao.AppSettingsDao
import com.appcontrol.data.dao.AppUsageDao
import com.appcontrol.data.model.AppSettings
import com.appcontrol.data.model.AppUsage

@Database(
    entities = [AppSettings::class, AppUsage::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun appUsageDao(): AppUsageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_control_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

