package com.expstudio.localai.agent

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings

/**
 * Central place for the device-level permissions the agent relies on, and the
 * intents to grant them. Kept free of Compose/Activity so it can be called from
 * anywhere (executor, ViewModel, UI).
 */
object AgentPermissions {

    /**
     * Whether the agent can touch files *outside* the app's own sandbox. On
     * API 30+ that means the special "All files access" grant; below that it
     * relies on legacy storage permissions (requested separately).
     */
    fun hasAllFilesAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            // Legacy WRITE/READ permissions are checked by the caller at runtime.
            true
        }

    /** Intent that opens the system screen to grant all-files access (API 30+). */
    fun allFilesAccessIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    /** Runtime permissions worth requesting on older devices for broad file access. */
    val legacyStoragePermissions: List<String> =
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            listOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )
        } else {
            emptyList()
        }
}
