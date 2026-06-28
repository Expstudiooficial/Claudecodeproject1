package com.expstudio.localai.data.settings

import com.expstudio.localai.agent.AgentMode
import com.expstudio.localai.agent.AgentTool
import com.expstudio.localai.data.model.InferenceParams

/** App-wide theme choice. */
enum class AppTheme(val label: String) {
    SYSTEM("Follow system"),
    LIGHT("Light"),
    DARK("Dark"),
}

/**
 * Per-tool on/off switches for the agent — a finer-grained layer on top of the
 * Ask / Accept / Auto [AgentMode]. A disabled tool is never executed, no matter
 * the mode. This is the "more permissions" the agent exposes beyond the 3 modes.
 */
data class AgentToolPermissions(
    val deviceInfo: Boolean = true,
    val listFiles: Boolean = true,
    val readFile: Boolean = true,
    val writeFile: Boolean = true,
    val makeDir: Boolean = true,
    val deleteFile: Boolean = true,
    val listApps: Boolean = true,
    val openApp: Boolean = true,
    val runCommand: Boolean = true,
) {
    fun isAllowed(tool: AgentTool): Boolean = when (tool) {
        AgentTool.DEVICE_INFO -> deviceInfo
        AgentTool.LIST_FILES -> listFiles
        AgentTool.READ_FILE -> readFile
        AgentTool.WRITE_FILE -> writeFile
        AgentTool.MAKE_DIR -> makeDir
        AgentTool.DELETE_FILE -> deleteFile
        AgentTool.LIST_APPS -> listApps
        AgentTool.OPEN_APP -> openApp
        AgentTool.RUN_COMMAND -> runCommand
    }

    fun with(tool: AgentTool, allowed: Boolean): AgentToolPermissions = when (tool) {
        AgentTool.DEVICE_INFO -> copy(deviceInfo = allowed)
        AgentTool.LIST_FILES -> copy(listFiles = allowed)
        AgentTool.READ_FILE -> copy(readFile = allowed)
        AgentTool.WRITE_FILE -> copy(writeFile = allowed)
        AgentTool.MAKE_DIR -> copy(makeDir = allowed)
        AgentTool.DELETE_FILE -> copy(deleteFile = allowed)
        AgentTool.LIST_APPS -> copy(listApps = allowed)
        AgentTool.OPEN_APP -> copy(openApp = allowed)
        AgentTool.RUN_COMMAND -> copy(runCommand = allowed)
    }
}

/**
 * User-controlled, app-wide preferences.
 *
 * [defaults] are the generation parameters applied to *new* chat sessions (the
 * manual counterpart to the one-tap Smart Setup). The agent block drives the
 * in-chat agent. The appearance block restyles the whole app.
 */
data class AppSettings(
    val defaults: InferenceParams = InferenceParams.DEFAULT,
    // Agent
    val agentEnabled: Boolean = false,
    val agentMode: AgentMode = AgentMode.ASK,
    val agentPermissions: AgentToolPermissions = AgentToolPermissions(),
    // Appearance
    val theme: AppTheme = AppTheme.SYSTEM,
    val dynamicColor: Boolean = true,
    val fontScale: Float = 1.0f,
    val showTimestamps: Boolean = true,
)
