package com.appcontrol.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.appcontrol.R
import com.appcontrol.databinding.ActivityPermissionsBinding
import com.appcontrol.util.PermissionHelper

class PermissionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPermissionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Configura Permessi"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupListeners()
        updatePermissionStatus()
    }

    private fun setupListeners() {
        binding.cardUsageStats.setOnClickListener {
            PermissionHelper.requestUsageStatsPermission(this)
        }

        binding.cardOverlay.setOnClickListener {
            PermissionHelper.requestOverlayPermission(this)
        }

        binding.cardAccessibility.setOnClickListener {
            PermissionHelper.openAccessibilitySettings(this)
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    private fun updatePermissionStatus() {
        // Usage Stats
        val hasUsageStats = PermissionHelper.hasUsageStatsPermission(this)
        binding.textUsageStatsStatus.text = if (hasUsageStats) "✓ Attivo" else "✗ Non attivo"
        binding.textUsageStatsStatus.setTextColor(
            getColor(if (hasUsageStats) R.color.green else R.color.red)
        )

        // Overlay
        val hasOverlay = PermissionHelper.hasOverlayPermission(this)
        binding.textOverlayStatus.text = if (hasOverlay) "✓ Attivo" else "✗ Non attivo"
        binding.textOverlayStatus.setTextColor(
            getColor(if (hasOverlay) R.color.green else R.color.red)
        )

        // Accessibility
        val hasAccessibility = PermissionHelper.isAccessibilityServiceEnabled(
            this,
            "com.appcontrol.service.AppBlockAccessibilityService"
        )
        binding.textAccessibilityStatus.text = if (hasAccessibility) "✓ Attivo" else "✗ Non attivo"
        binding.textAccessibilityStatus.setTextColor(
            getColor(if (hasAccessibility) R.color.green else R.color.red)
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
package com.appcontrol.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.appcontrol.databinding.ActivityBlockOverlayBinding

class BlockOverlayActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBlockOverlayBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlockOverlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val reason = intent.getStringExtra("BLOCK_REASON") ?: "App bloccata"
        binding.textBlockReason.text = reason

        binding.buttonClose.setOnClickListener {
            finish()
        }

        // Auto close after 3 seconds
        binding.root.postDelayed({
            if (!isFinishing) {
                finish()
            }
        }, 3000)
    }
}

