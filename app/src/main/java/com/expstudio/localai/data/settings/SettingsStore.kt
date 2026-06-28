package com.expstudio.localai.data.settings

import android.content.Context
import android.content.SharedPreferences
import com.expstudio.localai.agent.AgentMode
import com.expstudio.localai.agent.AgentTool
import com.expstudio.localai.data.model.InferenceParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * SharedPreferences-backed store for [AppSettings]. Exposes the current value as
 * a [StateFlow] so Compose screens recompose on change, and persists every
 * mutation synchronously so nothing is lost across process death.
 */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("localai_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun load(): AppSettings {
        val d = InferenceParams.DEFAULT
        return AppSettings(
            defaults = InferenceParams(
                temperature = prefs.getFloat(K_TEMP, d.temperature),
                topP = prefs.getFloat(K_TOP_P, d.topP),
                topK = prefs.getInt(K_TOP_K, d.topK),
                minP = prefs.getFloat(K_MIN_P, d.minP),
                typicalP = prefs.getFloat(K_TYPICAL_P, d.typicalP),
                repeatPenalty = prefs.getFloat(K_REPEAT, d.repeatPenalty),
                frequencyPenalty = prefs.getFloat(K_FREQ, d.frequencyPenalty),
                presencePenalty = prefs.getFloat(K_PRES, d.presencePenalty),
                repeatLastN = prefs.getInt(K_REPEAT_N, d.repeatLastN),
                mirostat = prefs.getInt(K_MIRO, d.mirostat),
                mirostatTau = prefs.getFloat(K_MIRO_TAU, d.mirostatTau),
                mirostatEta = prefs.getFloat(K_MIRO_ETA, d.mirostatEta),
                tfsZ = prefs.getFloat(K_TFS, d.tfsZ),
                seed = prefs.getInt(K_SEED, d.seed),
                contextWindow = prefs.getInt(K_CTX, d.contextWindow),
                maxTokens = prefs.getInt(K_MAX_TOK, d.maxTokens),
                threads = prefs.getInt(K_THREADS, d.threads),
                gpuLayers = prefs.getInt(K_GPU, d.gpuLayers),
                batchSize = prefs.getInt(K_BATCH, d.batchSize),
                useMmap = prefs.getBoolean(K_MMAP, d.useMmap),
                useMlock = prefs.getBoolean(K_MLOCK, d.useMlock),
                flashAttention = prefs.getBoolean(K_FLASH, d.flashAttention),
                systemPrompt = prefs.getString(K_SYS, d.systemPrompt) ?: d.systemPrompt,
                stopSequences = prefs.getString(K_STOP, d.stopSequences) ?: d.stopSequences,
                streamResponses = prefs.getBoolean(K_STREAM, d.streamResponses),
            ),
            agentEnabled = prefs.getBoolean(K_AGENT_ON, false),
            agentMode = runCatching {
                AgentMode.valueOf(prefs.getString(K_AGENT_MODE, AgentMode.ASK.name)!!)
            }.getOrDefault(AgentMode.ASK),
            agentPermissions = AgentToolPermissions(
                deviceInfo = prefs.getBoolean(K_P_DEVICE, true),
                listFiles = prefs.getBoolean(K_P_LIST, true),
                readFile = prefs.getBoolean(K_P_READ, true),
                writeFile = prefs.getBoolean(K_P_WRITE, true),
                makeDir = prefs.getBoolean(K_P_MKDIR, true),
                deleteFile = prefs.getBoolean(K_P_DELETE, true),
                listApps = prefs.getBoolean(K_P_APPS, true),
                openApp = prefs.getBoolean(K_P_OPEN, true),
                runCommand = prefs.getBoolean(K_P_CMD, true),
            ),
            theme = runCatching {
                AppTheme.valueOf(prefs.getString(K_THEME, AppTheme.SYSTEM.name)!!)
            }.getOrDefault(AppTheme.SYSTEM),
            dynamicColor = prefs.getBoolean(K_DYNAMIC, true),
            fontScale = prefs.getFloat(K_FONT, 1.0f),
            showTimestamps = prefs.getBoolean(K_TIMES, true),
        )
    }

    fun updateDefaults(p: InferenceParams) {
        prefs.edit()
            .putFloat(K_TEMP, p.temperature)
            .putFloat(K_TOP_P, p.topP)
            .putInt(K_TOP_K, p.topK)
            .putFloat(K_MIN_P, p.minP)
            .putFloat(K_TYPICAL_P, p.typicalP)
            .putFloat(K_REPEAT, p.repeatPenalty)
            .putFloat(K_FREQ, p.frequencyPenalty)
            .putFloat(K_PRES, p.presencePenalty)
            .putInt(K_REPEAT_N, p.repeatLastN)
            .putInt(K_MIRO, p.mirostat)
            .putFloat(K_MIRO_TAU, p.mirostatTau)
            .putFloat(K_MIRO_ETA, p.mirostatEta)
            .putFloat(K_TFS, p.tfsZ)
            .putInt(K_SEED, p.seed)
            .putInt(K_CTX, p.contextWindow)
            .putInt(K_MAX_TOK, p.maxTokens)
            .putInt(K_THREADS, p.threads)
            .putInt(K_GPU, p.gpuLayers)
            .putInt(K_BATCH, p.batchSize)
            .putBoolean(K_MMAP, p.useMmap)
            .putBoolean(K_MLOCK, p.useMlock)
            .putBoolean(K_FLASH, p.flashAttention)
            .putString(K_SYS, p.systemPrompt)
            .putString(K_STOP, p.stopSequences)
            .putBoolean(K_STREAM, p.streamResponses)
            .apply()
        _settings.value = _settings.value.copy(defaults = p)
    }

    fun resetDefaults() = updateDefaults(InferenceParams.DEFAULT)

    fun setAgentEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(K_AGENT_ON, enabled).apply()
        _settings.value = _settings.value.copy(agentEnabled = enabled)
    }

    fun setAgentMode(mode: AgentMode) {
        prefs.edit().putString(K_AGENT_MODE, mode.name).apply()
        _settings.value = _settings.value.copy(agentMode = mode)
    }

    fun setAgentPermission(tool: AgentTool, allowed: Boolean) {
        val updated = _settings.value.agentPermissions.with(tool, allowed)
        prefs.edit().putBoolean(permKey(tool), allowed).apply()
        _settings.value = _settings.value.copy(agentPermissions = updated)
    }

    fun setTheme(theme: AppTheme) {
        prefs.edit().putString(K_THEME, theme.name).apply()
        _settings.value = _settings.value.copy(theme = theme)
    }

    fun setDynamicColor(on: Boolean) {
        prefs.edit().putBoolean(K_DYNAMIC, on).apply()
        _settings.value = _settings.value.copy(dynamicColor = on)
    }

    fun setFontScale(scale: Float) {
        prefs.edit().putFloat(K_FONT, scale).apply()
        _settings.value = _settings.value.copy(fontScale = scale)
    }

    fun setShowTimestamps(on: Boolean) {
        prefs.edit().putBoolean(K_TIMES, on).apply()
        _settings.value = _settings.value.copy(showTimestamps = on)
    }

    private fun permKey(tool: AgentTool): String = when (tool) {
        AgentTool.DEVICE_INFO -> K_P_DEVICE
        AgentTool.LIST_FILES -> K_P_LIST
        AgentTool.READ_FILE -> K_P_READ
        AgentTool.WRITE_FILE -> K_P_WRITE
        AgentTool.MAKE_DIR -> K_P_MKDIR
        AgentTool.DELETE_FILE -> K_P_DELETE
        AgentTool.LIST_APPS -> K_P_APPS
        AgentTool.OPEN_APP -> K_P_OPEN
        AgentTool.RUN_COMMAND -> K_P_CMD
    }

    private companion object {
        const val K_TEMP = "temp"
        const val K_TOP_P = "top_p"
        const val K_TOP_K = "top_k"
        const val K_MIN_P = "min_p"
        const val K_TYPICAL_P = "typical_p"
        const val K_REPEAT = "repeat_penalty"
        const val K_FREQ = "freq_penalty"
        const val K_PRES = "pres_penalty"
        const val K_REPEAT_N = "repeat_last_n"
        const val K_MIRO = "mirostat"
        const val K_MIRO_TAU = "mirostat_tau"
        const val K_MIRO_ETA = "mirostat_eta"
        const val K_TFS = "tfs_z"
        const val K_SEED = "seed"
        const val K_CTX = "context_window"
        const val K_MAX_TOK = "max_tokens"
        const val K_THREADS = "threads"
        const val K_GPU = "gpu_layers"
        const val K_BATCH = "batch_size"
        const val K_MMAP = "use_mmap"
        const val K_MLOCK = "use_mlock"
        const val K_FLASH = "flash_attention"
        const val K_SYS = "system_prompt"
        const val K_STOP = "stop_sequences"
        const val K_STREAM = "stream_responses"
        const val K_AGENT_ON = "agent_enabled"
        const val K_AGENT_MODE = "agent_mode"
        const val K_P_DEVICE = "perm_device_info"
        const val K_P_LIST = "perm_list_files"
        const val K_P_READ = "perm_read_file"
        const val K_P_WRITE = "perm_write_file"
        const val K_P_MKDIR = "perm_make_dir"
        const val K_P_DELETE = "perm_delete_file"
        const val K_P_APPS = "perm_list_apps"
        const val K_P_OPEN = "perm_open_app"
        const val K_P_CMD = "perm_run_command"
        const val K_THEME = "theme"
        const val K_DYNAMIC = "dynamic_color"
        const val K_FONT = "font_scale"
        const val K_TIMES = "show_timestamps"
    }
}
