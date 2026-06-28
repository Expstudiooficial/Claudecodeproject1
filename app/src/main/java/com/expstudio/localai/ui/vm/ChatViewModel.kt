package com.expstudio.localai.ui.vm

import androidx.lifecycle.viewModelScope
import com.expstudio.localai.AppContainer
import com.expstudio.localai.data.db.entities.ChatMessage
import com.expstudio.localai.data.db.entities.ChatSession
import com.expstudio.localai.data.db.entities.Role
import com.expstudio.localai.data.model.InferenceParams
import com.expstudio.localai.data.model.ModelCatalog
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(private val container: AppContainer) : BaseViewModel() {

    val sessions: StateFlow<List<ChatSession>> =
        container.chatRepository.sessions
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

    /** Partial assistant text currently streaming (null when idle). */
    private val _streaming = MutableStateFlow<String?>(null)
    val streaming: StateFlow<String?> = _streaming.asStateFlow()

    val backendLabel: String get() = container.inferenceEngine.backend

    private var generationJob: Job? = null

    fun openSession(id: Long) { _currentSessionId.value = id }

    fun newSession(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val firstModel = container.modelRepository.installedIds().firstOrNull()
            val id = container.chatRepository.createSession(
                title = "New chat",
                modelId = firstModel,
            )
            _currentSessionId.value = id
            onCreated(id)
        }
    }

    fun deleteSession(id: Long) {
        viewModelScope.launch {
            container.chatRepository.deleteSession(id)
            if (_currentSessionId.value == id) _currentSessionId.value = null
        }
    }

    fun send(text: String) {
        val sessionId = _currentSessionId.value ?: return
        if (text.isBlank() || generationJob?.isActive == true) return

        generationJob = viewModelScope.launch {
            val repo = container.chatRepository
            repo.addMessage(sessionId, Role.USER, text.trim())

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

            val sb = StringBuilder()
            _streaming.value = ""
            container.inferenceEngine
                .generate(modelPath, history, params)
                .collect { chunk ->
                    sb.append(chunk)
                    _streaming.value = sb.toString()
                }

            repo.addMessage(sessionId, Role.ASSISTANT, sb.toString().trim())
            _streaming.value = null
        }
    }

    fun stopGeneration() {
        generationJob?.cancel()
        _streaming.value = null
    }
}
