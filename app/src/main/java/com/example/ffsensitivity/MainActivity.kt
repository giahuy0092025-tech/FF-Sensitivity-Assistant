package com.example.ffsensitivity

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.ffsensitivity.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.startHardwareMonitoring(applicationContext)
        viewModel.checkInstalledGames(packageManager)

        setupListeners()
        observeUiState()
    }

    private fun setupListeners() {
        binding.btnToggleOverlay.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                requestOverlayPermission()
            } else {
                val currentState = viewModel.uiState.value.isOverlayActive
                if (currentState) {
                    stopCrosshairService()
                } else {
                    startCrosshairService()
                }
            }
        }

        binding.btnToggleEditMode.setOnClickListener {
            viewModel.toggleEditMode()
            if (viewModel.uiState.value.isOverlayActive) {
                startCrosshairService()
            }
        }

        binding.btnCalculate.setOnClickListener {
            val currentDpi = binding.etCurrentDpi.text.toString().toIntOrNull() ?: 411
            val targetDpi = binding.etTargetDpi.text.toString().toIntOrNull() ?: 500
            val buttonSize = binding.etButtonSize.text.toString().toIntOrNull() ?: 50
            viewModel.calculateSensitivity(currentDpi, targetDpi, buttonSize, DevicePreset.BALANCED)
        }

        binding.btnOpenDevOptions.setOnClickListener {
            startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
        }

        binding.btnOpenAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        binding.btnLaunchStandard.setOnClickListener {
            launchGame("com.dts.freefireth")
        }

        binding.btnLaunchMax.setOnClickListener {
            launchGame("com.dts.freefiremax")
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.tvRamUsage.text = "RAM: ${state.availableRamMb}MB / ${state.totalRamMb}MB Free"
                    binding.tvRefreshRate.text = "Screen: ${state.refreshRateHz.toInt()} Hz"

                    binding.btnToggleOverlay.text = if (state.isOverlayActive) "Stop Overlay" else "Start Overlay"
                    binding.btnToggleEditMode.text = if (state.isEditMode) "Lock Position" else "Drag / Position Mode"

                    binding.btnLaunchStandard.isEnabled = state.isFFStandardInstalled
                    binding.btnLaunchMax.isEnabled = state.isFFMaxInstalled

                    state.sensitivityProfile?.let { profile ->
                        binding.tvResults.text = """
                            General: ${profile.general}
                            Red Dot: ${profile.redDot}
                            2x Scope: ${profile.scope2x}
                            4x Scope: ${profile.scope4x}
                            AWM Scope: ${profile.awmScope}
                            Free Look: ${profile.freeLook}
                            DPI Recommendation: ${profile.recommendedDpi}
                        """.trimIndent()
                    }
                }
            }
        }
    }

    private fun startCrosshairService() {
        val state = viewModel.uiState.value
        val intent = Intent(this, CrosshairOverlayService::class.java).apply {
            putExtra(CrosshairOverlayService.EXTRA_SIZE, state.crosshairSize)
            putExtra(CrosshairOverlayService.EXTRA_ALPHA, state.crosshairAlpha)
            putExtra(CrosshairOverlayService.EXTRA_COLOR, state.crosshairColorHex)
            putExtra(CrosshairOverlayService.EXTRA_SHAPE, state.crosshairShape)
            putExtra(CrosshairOverlayService.EXTRA_EDIT_MODE, state.isEditMode)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        viewModel.setOverlayActive(true)
    }

    private fun stopCrosshairService() {
        val intent = Intent(this, CrosshairOverlayService::class.java)
        stopService(intent)
        viewModel.setOverlayActive(false)
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun launchGame(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "Game package not found", Toast.LENGTH_SHORT).show()
        }
    }
}

