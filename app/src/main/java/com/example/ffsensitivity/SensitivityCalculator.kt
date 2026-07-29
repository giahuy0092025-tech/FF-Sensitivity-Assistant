package com.example.ffsensitivity

import kotlin.math.roundToInt

enum class DevicePreset(
    val displayName: String,
    val baseMultiplier: Float,
    val targetDpi: Int
) {
    BALANCED("Standard Balanced", 1.0f, 411),
    SAMSUNG("Samsung Galaxy Optimizations", 1.05f, 450),
    XIAOMI("Xiaomi / Poco High Response", 1.08f, 440),
    ROG_PHONE("ROG / Gaming High DPI", 1.15f, 520),
    IOS_LIKE("iOS Smooth Ratio Scale", 0.95f, 380)
}

data class SensitivityProfile(
    val general: Int,
    val redDot: Int,
    val scope2x: Int,
    val scope4x: Int,
    val awmScope: Int,
    val freeLook: Int,
    val recommendedDpi: Int
)

object SensitivityCalculator {

    fun calculateProfile(
        currentDpi: Int,
        targetDpi: Int,
        fireButtonSizePercent: Int,
        preset: DevicePreset
    ): SensitivityProfile {
        val safeCurrentDpi = currentDpi.coerceAtLeast(160)
        val safeTargetDpi = targetDpi.coerceIn(200, 1000)
        val safeButtonSize = fireButtonSizePercent.coerceIn(10, 100)

        val dpiRatio = safeTargetDpi.toFloat() / safeCurrentDpi.toFloat()
        val buttonFactor = (100 - safeButtonSize) / 100.0f
        val multiplier = dpiRatio * preset.baseMultiplier

        val baseGeneral = (50f * multiplier + (buttonFactor * 20f)).coerceIn(10f, 100f)
        val baseRedDot = (baseGeneral * 0.92f).coerceIn(10f, 100f)
        val baseScope2x = (baseGeneral * 0.85f).coerceIn(10f, 100f)
        val baseScope4x = (baseGeneral * 0.78f).coerceIn(10f, 100f)
        val baseAwm = (baseGeneral * 0.55f).coerceIn(10f, 100f)
        val baseFreeLook = (baseGeneral * 0.95f).coerceIn(10f, 100f)

        return SensitivityProfile(
            general = baseGeneral.roundToInt(),
            redDot = baseRedDot.roundToInt(),
            scope2x = baseScope2x.roundToInt(),
            scope4x = baseScope4x.roundToInt(),
            awmScope = baseAwm.roundToInt(),
            freeLook = baseFreeLook.roundToInt(),
            recommendedDpi = safeTargetDpi
        )
    }
}

