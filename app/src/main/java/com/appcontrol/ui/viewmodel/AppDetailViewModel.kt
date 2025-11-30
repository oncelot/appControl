package com.appcontrol.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.appcontrol.data.database.AppDatabase
import com.appcontrol.data.model.AppSettings
import com.appcontrol.data.repository.AppRepository
import kotlinx.coroutines.launch

class AppDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository
    private val _appSettings = MutableLiveData<AppSettings?>()
    val appSettings: LiveData<AppSettings?> = _appSettings

    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AppRepository(
            database.appSettingsDao(),
            database.appUsageDao(),
            application
        )
    }

    fun loadAppSettings(packageName: String, appName: String) {
        viewModelScope.launch {
            val settings = repository.getAppSettings(packageName)
            if (settings != null) {
                _appSettings.value = settings
            } else {
                // Create default settings
                _appSettings.value = AppSettings(
                    packageName = packageName,
                    appName = appName,
                    isBlocked = false,
                    blockStartTime = "09:00",
                    blockEndTime = "18:00",
                    maxUsageMinutes = 30
                )
            }
        }
    }

    fun saveAppSettings(appSettings: AppSettings) {
        viewModelScope.launch {
            try {
                repository.insertOrUpdateAppSettings(appSettings)
                _saveSuccess.value = true
            } catch (e: Exception) {
                e.printStackTrace()
                _saveSuccess.value = false
            }
        }
    }

    fun getTodayUsage(packageName: String): LiveData<Long> {
        val usageLiveData = MutableLiveData<Long>()
        viewModelScope.launch {
            val usage = repository.getTodayUsage(packageName)
            usageLiveData.value = usage
        }
        return usageLiveData
    }
}
package com.appcontrol.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.appcontrol.data.database.AppDatabase
import com.appcontrol.data.model.AppInfo
import com.appcontrol.data.repository.AppRepository
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository
    private val _apps = MutableLiveData<List<AppInfo>>()
    val apps: LiveData<List<AppInfo>> = _apps

    private val _filteredApps = MutableLiveData<List<AppInfo>>()
    val filteredApps: LiveData<List<AppInfo>> = _filteredApps

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AppRepository(
            database.appSettingsDao(),
            database.appUsageDao(),
            application
        )
        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val installedApps = repository.getAllInstalledApps()
                _apps.value = installedApps
                _filteredApps.value = installedApps
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun filterApps(query: String) {
        val allApps = _apps.value ?: return
        _filteredApps.value = if (query.isEmpty()) {
            allApps
        } else {
            allApps.filter {
                it.appName.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
            }
        }
    }

    fun filterByBlockStatus(showBlocked: Boolean?) {
        val allApps = _apps.value ?: return
        _filteredApps.value = when (showBlocked) {
            true -> allApps.filter { it.isBlocked }
            false -> allApps.filter { !it.isBlocked }
            null -> allApps
        }
    }
}

