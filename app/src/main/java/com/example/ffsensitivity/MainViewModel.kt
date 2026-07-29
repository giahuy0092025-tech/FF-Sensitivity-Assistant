package com.example.ffsensitivity

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.view.WindowManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val availableRamMb: Long = 0,
    val totalRamMb: Long = 0,
    val refreshRateHz: Float = 60f,
    val isOverlayActive: Boolean = false,
    val isEditMode: Boolean = false,
    val crosshairSize: Int = 24,
    val crosshairAlpha: Int = 255,
    val crosshairColorHex: String = "#66FCF1",
    val crosshairShape: String = "DOT",
    val sensitivityProfile: SensitivityProfile? = null,
    val isFFStandardInstalled: Boolean = false,
    val isFFMaxInstalled: Boolean = false
)

class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun startHardwareMonitoring(context: Context) {
        viewModelScope.launch {
            while (true) {
                updateHardwareStats(context)
                delay(2000)
            }
        }
    }

    private fun updateHardwareStats(context: Context) {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val totalRam = memoryInfo.totalMem / (1024 * 1024)
        val availRam = memoryInfo.availMem / (1024 * 1024)

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val refreshRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display?.refreshRate ?: 60f
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.refreshRate
        }

        _uiState.update {
            it.copy(
                availableRamMb = availRam,
                totalRamMb = totalRam,
                refreshRateHz = refreshRate
            )
        }
    }

    fun checkInstalledGames(packageManager: PackageManager) {
        val standardInstalled = isPackageInstalled("com.dts.freefireth", packageManager)
        val maxInstalled = isPackageInstalled("com.dts.freefiremax", packageManager)

        _uiState.update {
            it.copy(
                isFFStandardInstalled = standardInstalled,
                isFFMaxInstalled = maxInstalled
            )
        }
    }

    private fun isPackageInstalled(packageName: String, packageManager: PackageManager): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun calculateSensitivity(
        currentDpi: Int,
        targetDpi: Int,
        buttonSize: Int,
        preset: DevicePreset
    ) {
        val profile = SensitivityCalculator.calculateProfile(currentDpi, targetDpi, buttonSize, preset)
        _uiState.update { it.copy(sensitivityProfile = profile) }
    }

    fun setCrosshairSize(size: Int) {
        _uiState.update { it.copy(crosshairSize = size) }
    }

    fun setCrosshairAlpha(alpha: Int) {
        _uiState.update { it.copy(crosshairAlpha = alpha) }
    }

    fun setCrosshairColor(hex: String) {
        _uiState.update { it.copy(crosshairColorHex = hex) }
    }

    fun setCrosshairShape(shape: String) {
        _uiState.update { it.copy(crosshairShape = shape) }
    }

    fun toggleEditMode() {
        _uiState.update { it.copy(isEditMode = !it.isEditMode) }
    }

    fun setOverlayActive(active: Boolean) {
        _uiState.update { it.copy(isOverlayActive = active) }
    }
}

