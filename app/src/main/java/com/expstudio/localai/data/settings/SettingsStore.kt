package com.expstudio.localai.data.settings

import android.content.Context
import android.content.SharedPreferences
import com.expstudio.localai.agent.AgentMode
import com.expstudio.localai.data.model.InferenceParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tiny SharedPreferences-backed store for [AppSettings]. Exposes the current
 * value as a [StateFlow] so Compose screens recompose on change, and persists
 * every mutation synchronously so nothing is lost across process death.
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
                temperature = prefs.getFloat(KEY_TEMP, d.temperature),
                topP = prefs.getFloat(KEY_TOP_P, d.topP),
                repeatPenalty = prefs.getFloat(KEY_REPEAT, d.repeatPenalty),
                contextWindow = prefs.getInt(KEY_CTX, d.contextWindow),
                maxTokens = prefs.getInt(KEY_MAX_TOK, d.maxTokens),
                threads = prefs.getInt(KEY_THREADS, d.threads),
                gpuLayers = prefs.getInt(KEY_GPU, d.gpuLayers),
                batchSize = prefs.getInt(KEY_BATCH, d.batchSize),
            ),
            agentEnabled = prefs.getBoolean(KEY_AGENT_ON, false),
            agentMode = runCatching {
                AgentMode.valueOf(prefs.getString(KEY_AGENT_MODE, AgentMode.ASK.name)!!)
            }.getOrDefault(AgentMode.ASK),
        )
    }

    fun updateDefaults(params: InferenceParams) {
        prefs.edit()
            .putFloat(KEY_TEMP, params.temperature)
            .putFloat(KEY_TOP_P, params.topP)
            .putFloat(KEY_REPEAT, params.repeatPenalty)
            .putInt(KEY_CTX, params.contextWindow)
            .putInt(KEY_MAX_TOK, params.maxTokens)
            .putInt(KEY_THREADS, params.threads)
            .putInt(KEY_GPU, params.gpuLayers)
            .putInt(KEY_BATCH, params.batchSize)
            .apply()
        _settings.value = _settings.value.copy(defaults = params)
    }

    fun setAgentEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AGENT_ON, enabled).apply()
        _settings.value = _settings.value.copy(agentEnabled = enabled)
    }

    fun setAgentMode(mode: AgentMode) {
        prefs.edit().putString(KEY_AGENT_MODE, mode.name).apply()
        _settings.value = _settings.value.copy(agentMode = mode)
    }

    private companion object {
        const val KEY_TEMP = "temp"
        const val KEY_TOP_P = "top_p"
        const val KEY_REPEAT = "repeat_penalty"
        const val KEY_CTX = "context_window"
        const val KEY_MAX_TOK = "max_tokens"
        const val KEY_THREADS = "threads"
        const val KEY_GPU = "gpu_layers"
        const val KEY_BATCH = "batch_size"
        const val KEY_AGENT_ON = "agent_enabled"
        const val KEY_AGENT_MODE = "agent_mode"
    }
}
