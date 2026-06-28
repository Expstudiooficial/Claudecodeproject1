package com.expstudio.localai.ui.vm

import androidx.lifecycle.viewModelScope
import com.expstudio.localai.AppContainer
import com.expstudio.localai.agent.AgentMode
import com.expstudio.localai.data.model.InferenceParams
import com.expstudio.localai.data.settings.AppSettings
import com.expstudio.localai.smart.DeviceProfile
import com.expstudio.localai.smart.SmartRecommendation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val container: AppContainer) : BaseViewModel() {

    /** App-wide settings (default generation params + agent config). */
    val settings: StateFlow<AppSettings> = container.settingsStore.settings

    fun updateDefaults(params: InferenceParams) = container.settingsStore.updateDefaults(params)
    fun setAgentEnabled(enabled: Boolean) = container.settingsStore.setAgentEnabled(enabled)
    fun setAgentMode(mode: AgentMode) = container.settingsStore.setAgentMode(mode)

    private val _device = MutableStateFlow<DeviceProfile?>(null)
    val device: StateFlow<DeviceProfile?> = _device.asStateFlow()

    private val _recommendation = MutableStateFlow<SmartRecommendation?>(null)
    val recommendation: StateFlow<SmartRecommendation?> = _recommendation.asStateFlow()

    private val _cacheBytes = MutableStateFlow(0L)
    val cacheBytes: StateFlow<Long> = _cacheBytes.asStateFlow()

    val backendLabel: String get() = container.inferenceEngine.backend

    init { refreshSystemInfo() }

    fun refreshSystemInfo() {
        _device.value = DeviceProfile.snapshot(container.appContext)
        viewModelScope.launch { _cacheBytes.value = container.modelRepository.totalBytesOnDisk() }
    }

    /** The one-tap Smart Setup. Snapshots the device live, then recommends. */
    fun runSmartSetup(aggressiveness: Float = 0.5f) {
        viewModelScope.launch {
            val device = DeviceProfile.snapshot(container.appContext)
            _device.value = device
            val installed = container.modelRepository.installedIds()
            _recommendation.value =
                container.smartConfigurator.recommend(device, installed, aggressiveness)
        }
    }

    /** Applies the current recommendation to a session (params + model if installed). */
    fun applyRecommendationTo(sessionId: Long) {
        val rec = _recommendation.value ?: return
        viewModelScope.launch {
            val repo = container.chatRepository
            val session = repo.getSession(sessionId) ?: return@launch
            repo.updateSession(
                session.copy(
                    modelId = if (rec.isInstalled) rec.model.id else session.modelId,
                    temperature = rec.params.temperature,
                    topP = rec.params.topP,
                    repeatPenalty = rec.params.repeatPenalty,
                    contextWindow = rec.params.contextWindow,
                    maxTokens = rec.params.maxTokens,
                )
            )
        }
    }

    fun dismissRecommendation() { _recommendation.value = null }
}
