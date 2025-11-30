package com.appcontrol.data.dao

import androidx.room.*
import com.appcontrol.data.model.AppUsage

@Dao
interface AppUsageDao {

    @Query("SELECT * FROM app_usage WHERE packageName = :packageName AND date = :date")
    suspend fun getUsageForDate(packageName: String, date: String): AppUsage?

    @Query("SELECT totalUsageMillis FROM app_usage WHERE packageName = :packageName AND date = :date")
    suspend fun getTotalUsageForDate(packageName: String, date: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(appUsage: AppUsage)

    @Update
    suspend fun update(appUsage: AppUsage)

    @Query("UPDATE app_usage SET totalUsageMillis = :totalMillis WHERE packageName = :packageName AND date = :date")
    suspend fun updateUsageTime(packageName: String, date: String, totalMillis: Long)

    @Query("DELETE FROM app_usage WHERE date < :date")
    suspend fun deleteOldRecords(date: String)
}
package com.appcontrol.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.appcontrol.data.model.AppSettings

@Dao
interface AppSettingsDao {

    @Query("SELECT * FROM app_settings ORDER BY appName ASC")
    fun getAllAppSettings(): LiveData<List<AppSettings>>

    @Query("SELECT * FROM app_settings WHERE packageName = :packageName")
    suspend fun getAppSettings(packageName: String): AppSettings?

    @Query("SELECT * FROM app_settings WHERE isBlocked = 1")
    suspend fun getBlockedApps(): List<AppSettings>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(appSettings: AppSettings)

    @Update
    suspend fun update(appSettings: AppSettings)

    @Delete
    suspend fun delete(appSettings: AppSettings)

    @Query("DELETE FROM app_settings WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)
}

