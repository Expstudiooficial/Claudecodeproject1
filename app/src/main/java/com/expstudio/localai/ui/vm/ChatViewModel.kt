package com.expstudio.localai.ui.vm

import androidx.lifecycle.viewModelScope
import com.expstudio.localai.AppContainer
import com.expstudio.localai.agent.AgentMode
import com.expstudio.localai.agent.AgentTool
import com.expstudio.localai.agent.PendingApproval
import com.expstudio.localai.agent.ToolCall
import com.expstudio.localai.agent.ToolCallParser
import com.expstudio.localai.data.settings.AgentToolPermissions
import com.expstudio.localai.data.db.entities.ChatMessage
import com.expstudio.localai.data.db.entities.ChatSession
import com.expstudio.localai.data.db.entities.InstalledModel
import com.expstudio.localai.data.db.entities.Role
import com.expstudio.localai.data.model.InferenceParams
import com.expstudio.localai.data.model.ModelCatalog
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(private val container: AppContainer) : BaseViewModel() {

    private val settings = container.settingsStore.settings

    val sessions: StateFlow<List<ChatSession>> =
        container.chatRepository.sessions
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projects: StateFlow<List<com.expstudio.localai.data.db.entities.Project>> =
        container.chatRepository.projects
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val installedModels: StateFlow<List<InstalledModel>> =
        container.modelRepository.installedModels
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentSessionId = MutableStateFlow<Long?>(null)
    val currentSessionId: StateFlow<Long?> = _currentSessionId.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<ChatMessage>> =
        _currentSessionId
            .flatMapLatest { id ->
                if (id == null) flowOf(emptyList()) else container.chatRepository.messages(id)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** The live [ChatSession] for the open chat (drives the model/params editor). */
    val currentSession: StateFlow<ChatSession?> =
        combine(sessions, _currentSessionId) { list, id ->
            id?.let { list.firstOrNull { s -> s.id == it } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Partial assistant text currently streaming (null when idle). */
    private val _streaming = MutableStateFlow<String?>(null)
    val streaming: StateFlow<String?> = _streaming.asStateFlow()

    /** Name of the model being loaded into memory (null when not loading). */
    private val _loadingModel = MutableStateFlow<String?>(null)
    val loadingModel: StateFlow<String?> = _loadingModel.asStateFlow()

    // ---- Agent state (mirrors the global settings store) ----
    val agentEnabled: StateFlow<Boolean> =
        settings.map { it.agentEnabled }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val agentMode: StateFlow<AgentMode> =
        settings.map { it.agentMode }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AgentMode.ASK)

    val agentPermissions: StateFlow<AgentToolPermissions> =
        settings.map { it.agentPermissions }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AgentToolPermissions())

    val showTimestamps: StateFlow<Boolean> =
        settings.map { it.showTimestamps }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val fontScale: StateFlow<Float> =
        settings.map { it.fontScale }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    private val _pendingApproval = MutableStateFlow<PendingApproval?>(null)
    val pendingApproval: StateFlow<PendingApproval?> = _pendingApproval.asStateFlow()

    val backendLabel: String get() = container.inferenceEngine.backend

    private var generationJob: Job? = null

    fun openSession(id: Long) { _currentSessionId.value = id }

    fun setAgentEnabled(enabled: Boolean) = container.settingsStore.setAgentEnabled(enabled)
    fun setAgentMode(mode: AgentMode) = container.settingsStore.setAgentMode(mode)
    fun setAgentPermission(tool: AgentTool, allowed: Boolean) =
        container.settingsStore.setAgentPermission(tool, allowed)

    fun newSession(projectId: Long? = null, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val firstModel = container.modelRepository.installedIds().firstOrNull()
            val id = container.chatRepository.createSession(
                title = "New chat",
                modelId = firstModel,
                params = settings.value.defaults,
                projectId = projectId,
            )
            _currentSessionId.value = id
            onCreated(id)
        }
    }

    fun createProject(name: String, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = container.chatRepository.createProject(
                name = name.ifBlank { "New project" },
                colorIndex = (projects.value.size % 6),
            )
            onCreated(id)
        }
    }

    fun deleteProject(id: Long) {
        viewModelScope.launch { container.chatRepository.deleteProject(id) }
    }

    fun moveSessionToProject(sessionId: Long, projectId: Long?) {
        viewModelScope.launch { container.chatRepository.assignSessionToProject(sessionId, projectId) }
    }

    fun deleteSession(id: Long) {
        viewModelScope.launch {
            container.chatRepository.deleteSession(id)
            if (_currentSessionId.value == id) _currentSessionId.value = null
        }
    }

    /** Change the model bound to the open session. */
    fun selectModel(modelId: String) {
        val id = _currentSessionId.value ?: return
        viewModelScope.launch {
            val session = container.chatRepository.getSession(id) ?: return@launch
            container.chatRepository.updateSession(session.copy(modelId = modelId))
        }
    }

    /** Apply manually-edited generation parameters to the open session. */
    fun updateSessionParams(params: InferenceParams) {
        val id = _currentSessionId.value ?: return
        viewModelScope.launch {
            val session = container.chatRepository.getSession(id) ?: return@launch
            container.chatRepository.updateSession(
                session.copy(
                    temperature = params.temperature,
                    topP = params.topP,
                    repeatPenalty = params.repeatPenalty,
                    contextWindow = params.contextWindow,
                    maxTokens = params.maxTokens,
                )
            )
        }
    }

    fun send(text: String) {
        val sessionId = _currentSessionId.value ?: return
        if (text.isBlank() || generationJob?.isActive == true) return

        generationJob = viewModelScope.launch {
            container.chatRepository.addMessage(sessionId, Role.USER, text.trim())
            runAssistantTurn(sessionId)
        }
    }

    /** One assistant turn: generate, then (if agent on) execute any tool calls. */
    private suspend fun runAssistantTurn(sessionId: Long) {
        val repo = container.chatRepository
        val session = repo.getSession(sessionId)
        val model = session?.modelId?.let { ModelCatalog.byId(it) }
        val params = session?.let {
            InferenceParams(
                temperature = it.temperature,
                topP = it.topP,
                repeatPenalty = it.repeatPenalty,
                contextWindow = it.contextWindow,
                maxTokens = it.maxTokens,
            )
        } ?: InferenceParams.DEFAULT

        val history = repo.historyFor(sessionId)
        val modelPath = model?.let { container.modelRepository.filePathFor(it) } ?: ""
        val agentOn = settings.value.agentEnabled

        // ---- Memory safeguard: refuse to load a model that won't fit (unless disabled). ----
        if (settings.value.memorySafeguards && model != null) {
            val device = com.expstudio.localai.smart.DeviceProfile.snapshot(container.appContext)
            if (device.availableRamMb in 1 until model.minRamMb) {
                repo.addMessage(
                    sessionId, Role.SYSTEM,
                    "🛡️ Safeguard: ${model.displayName} needs ~${model.minRamMb} MB free but only " +
                        "${device.availableRamMb} MB is available. Pick a smaller model, free up " +
                        "memory, or turn off Memory safeguards in Settings to force it.",
                )
                return
            }
        }

        val sb = StringBuilder()
        // Only show the loading indicator when the model isn't already resident —
        // i.e. on the first message or after a model switch, not every turn.
        val needsLoad = model != null &&
            !container.inferenceEngine.isModelResident(modelPath, params)
        _loadingModel.value = if (needsLoad) model?.displayName else null
        _streaming.value = ""
        try {
            container.inferenceEngine
                .generate(modelPath, history, params, agentMode = agentOn)
                .collect { chunk ->
                    _loadingModel.value = null // first token → done loading
                    sb.append(chunk)
                    _streaming.value = sb.toString()
                }
        } finally {
            _loadingModel.value = null
        }

        val reply = sb.toString().trim()
        repo.addMessage(sessionId, Role.ASSISTANT, reply)
        _streaming.value = null

        if (agentOn) runAgentTools(sessionId, reply)
    }

    /** Parse tool calls from [reply], gate them on the mode, execute, report. */
    private suspend fun runAgentTools(sessionId: Long, reply: String) {
        val repo = container.chatRepository
        val perms = settings.value.agentPermissions
        for (call in ToolCallParser.parse(reply)) {
            if (!perms.isAllowed(call.tool)) {
                repo.addMessage(
                    sessionId, Role.SYSTEM,
                    "🔒 Blocked: ${call.tool.label} is turned off in agent permissions.",
                )
                continue
            }
            if (!approveIfNeeded(call)) {
                repo.addMessage(sessionId, Role.SYSTEM, "🚫 Denied: ${call.summary}")
                continue
            }
            val result = container.agentToolExecutor.execute(call)
            repo.addMessage(sessionId, Role.SYSTEM, result.render())
        }
    }

    /** Suspends on a [PendingApproval] when the current mode demands confirmation. */
    private suspend fun approveIfNeeded(call: ToolCall): Boolean {
        val mode = settings.value.agentMode
        if (!mode.requiresApproval(call.tool)) return true
        val deferred = CompletableDeferred<Boolean>()
        _pendingApproval.value = PendingApproval(call, deferred)
        val decision = deferred.await()
        _pendingApproval.value = null
        return decision
    }

    fun resolveApproval(approved: Boolean) {
        _pendingApproval.value?.deferred?.complete(approved)
    }

    fun stopGeneration() {
        generationJob?.cancel()
        _streaming.value = null
        _loadingModel.value = null
        _pendingApproval.value?.deferred?.complete(false)
        _pendingApproval.value = null
    }
}
