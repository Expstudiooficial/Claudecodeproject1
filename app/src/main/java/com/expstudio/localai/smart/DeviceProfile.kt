package com.expstudio.localai.smart

import android.app.ActivityManager
import android.content.Context
import android.os.Build

/**
 * A snapshot of what the device can offer *right now* — crucially using
 * currently-available RAM, not just total, so the recommendation accounts for
 * the OS and other running apps rather than pretending the app owns the device.
 */
data class DeviceProfile(
    val totalRamMb: Long,
    val availableRamMb: Long,
    val lowMemoryState: Boolean,
    /** OS-defined threshold below which Android starts killing background apps. */
    val memoryThresholdMb: Long,
    val cpuCores: Int,
    val isLowRamDevice: Boolean,
    val deviceModel: String,
    val primaryAbi: String,
) {
    val usedRamMb: Long get() = (totalRamMb - availableRamMb).coerceAtLeast(0)

    companion object {
        fun snapshot(context: Context): DeviceProfile {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
            val mb = 1024L * 1024L
            return DeviceProfile(
                totalRamMb = mi.totalMem / mb,
                availableRamMb = mi.availMem / mb,
                lowMemoryState = mi.lowMemory,
                memoryThresholdMb = mi.threshold / mb,
                cpuCores = Runtime.getRuntime().availableProcessors(),
                isLowRamDevice = am.isLowRamDevice,
                deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                primaryAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown",
            )
        }
    }
}
