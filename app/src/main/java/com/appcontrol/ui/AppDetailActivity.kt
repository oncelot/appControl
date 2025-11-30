package com.appcontrol.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.appcontrol.R
import com.appcontrol.data.model.AppSettings
import com.appcontrol.databinding.ActivityAppDetailBinding
import com.appcontrol.ui.viewmodel.AppDetailViewModel
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat

class AppDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppDetailBinding
    private val viewModel: AppDetailViewModel by viewModels()
    private var currentSettings: AppSettings? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val packageName = intent.getStringExtra("PACKAGE_NAME") ?: return finish()
        val appName = intent.getStringExtra("APP_NAME") ?: "App"

        supportActionBar?.title = appName
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupUsageTimeSpinner()
        setupObservers()
        setupListeners(packageName)

        viewModel.loadAppSettings(packageName, appName)
    }

    private fun setupUsageTimeSpinner() {
        val usageOptions = listOf(5, 10, 15, 30, 45, 60, 90, 120)
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            usageOptions.map { "$it minuti" }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerMaxUsage.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.appSettings.observe(this) { settings ->
            if (settings != null) {
                currentSettings = settings
                updateUI(settings)
            }
        }

        viewModel.saveSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Impostazioni salvate", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Errore nel salvataggio", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUI(settings: AppSettings) {
        binding.switchBlockEnabled.isChecked = settings.isBlocked
        binding.textStartTime.text = settings.blockStartTime ?: "09:00"
        binding.textEndTime.text = settings.blockEndTime ?: "18:00"

        val usageOptions = listOf(5, 10, 15, 30, 45, 60, 90, 120)
        val index = usageOptions.indexOf(settings.maxUsageMinutes)
        if (index >= 0) {
            binding.spinnerMaxUsage.setSelection(index)
        }

        updateBlockSettingsVisibility(settings.isBlocked)

        // Load today's usage
        viewModel.getTodayUsage(settings.packageName).observe(this) { usageMillis ->
            val minutes = usageMillis / (60 * 1000)
            binding.textCurrentUsage.text = "Utilizzo oggi: $minutes minuti"
        }
    }

    private fun setupListeners(packageName: String) {
        binding.switchBlockEnabled.setOnCheckedChangeListener { _, isChecked ->
            updateBlockSettingsVisibility(isChecked)
        }

        binding.layoutStartTime.setOnClickListener {
            showTimePicker(true)
        }

        binding.layoutEndTime.setOnClickListener {
            showTimePicker(false)
        }

        binding.buttonSave.setOnClickListener {
            saveSettings(packageName)
        }
    }

    private fun updateBlockSettingsVisibility(isBlocked: Boolean) {
        val visibility = if (isBlocked) android.view.View.VISIBLE else android.view.View.GONE
        binding.layoutStartTime.visibility = visibility
        binding.layoutEndTime.visibility = visibility
        binding.layoutMaxUsage.visibility = visibility
        binding.textCurrentUsage.visibility = visibility
    }

    private fun showTimePicker(isStartTime: Boolean) {
        val currentTime = if (isStartTime) {
            binding.textStartTime.text.toString()
        } else {
            binding.textEndTime.text.toString()
        }

        val parts = currentTime.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(hour)
            .setMinute(minute)
            .setTitleText(if (isStartTime) "Orario inizio blocco" else "Orario fine blocco")
            .build()

        picker.addOnPositiveButtonClickListener {
            val timeString = String.format("%02d:%02d", picker.hour, picker.minute)
            if (isStartTime) {
                binding.textStartTime.text = timeString
            } else {
                binding.textEndTime.text = timeString
            }
        }

        picker.show(supportFragmentManager, "TIME_PICKER")
    }

    private fun saveSettings(packageName: String) {
        val current = currentSettings ?: return

        val usageOptions = listOf(5, 10, 15, 30, 45, 60, 90, 120)
        val selectedUsage = usageOptions[binding.spinnerMaxUsage.selectedItemPosition]

        val updatedSettings = current.copy(
            isBlocked = binding.switchBlockEnabled.isChecked,
            blockStartTime = binding.textStartTime.text.toString(),
            blockEndTime = binding.textEndTime.text.toString(),
            maxUsageMinutes = selectedUsage
        )

        viewModel.saveAppSettings(updatedSettings)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
package com.appcontrol.ui

import android.content.Intent
import android.os.Bundle
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.appcontrol.R
import com.appcontrol.databinding.ActivityMainBinding
import com.appcontrol.service.AppMonitorService
import com.appcontrol.ui.adapter.AppListAdapter
import com.appcontrol.ui.viewmodel.MainViewModel
import com.appcontrol.util.PermissionHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var adapter: AppListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupSearchView()
        setupObservers()
        setupButtons()
        checkPermissions()
        startMonitoringService()
    }

    private fun setupRecyclerView() {
        adapter = AppListAdapter { appInfo ->
            // Navigate to detail activity
            val intent = Intent(this, AppDetailActivity::class.java).apply {
                putExtra("PACKAGE_NAME", appInfo.packageName)
                putExtra("APP_NAME", appInfo.appName)
            }
            startActivity(intent)
        }

        binding.recyclerViewApps.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewApps.adapter = adapter
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.filterApps(newText ?: "")
                return true
            }
        })
    }

    private fun setupObservers() {
        viewModel.filteredApps.observe(this) { apps ->
            adapter.submitList(apps)
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
        }
    }

    private fun setupButtons() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadApps()
        }

        binding.chipAll.setOnClickListener {
            viewModel.filterByBlockStatus(null)
        }

        binding.chipBlocked.setOnClickListener {
            viewModel.filterByBlockStatus(true)
        }

        binding.chipNotBlocked.setOnClickListener {
            viewModel.filterByBlockStatus(false)
        }
    }

    private fun checkPermissions() {
        val missingPermissions = mutableListOf<String>()

        if (!PermissionHelper.hasUsageStatsPermission(this)) {
            missingPermissions.add("Statistiche utilizzo")
        }

        if (!PermissionHelper.hasOverlayPermission(this)) {
            missingPermissions.add("Overlay schermo")
        }

        if (!PermissionHelper.isAccessibilityServiceEnabled(
                this,
                "com.appcontrol.service.AppBlockAccessibilityService"
            )) {
            missingPermissions.add("Servizio accessibilità")
        }

        if (missingPermissions.isNotEmpty()) {
            showPermissionsDialog(missingPermissions)
        }
    }

    private fun showPermissionsDialog(permissions: List<String>) {
        AlertDialog.Builder(this)
            .setTitle("Permessi necessari")
            .setMessage("Per funzionare correttamente, l'app necessita dei seguenti permessi:\n\n" +
                    permissions.joinToString("\n• ", "• "))
            .setPositiveButton("Configura") { _, _ ->
                // Open settings activity
                startActivity(Intent(this, PermissionsActivity::class.java))
            }
            .setNegativeButton("Dopo") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun startMonitoringService() {
        val serviceIntent = Intent(this, AppMonitorService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadApps()
    }
}

