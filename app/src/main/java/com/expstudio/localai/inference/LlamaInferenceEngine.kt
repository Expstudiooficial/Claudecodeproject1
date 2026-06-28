package com.expstudio.localai.inference

import android.util.Log
import com.expstudio.localai.data.db.entities.ChatMessage
import com.expstudio.localai.data.db.entities.Role
import com.expstudio.localai.data.model.InferenceParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

/**
 * Owns the model lifecycle and turns a conversation into a stream of generated
 * tokens.
 *
 * The engine keeps the currently-loaded model resident between turns (loading a
 * GGUF is expensive) and only reloads when the model or context size changes.
 *
 * If the native backend is a stub (llama.cpp not compiled in), [generate] emits
 * a clearly-labelled simulated response so the whole app remains demoable.
 */
class LlamaInferenceEngine {

    private val bridge = LlamaBridge()

    private var handle: Long = 0L
    private var loadedModelPath: String? = null
    private var loadedContext: Int = 0

    val backend: String get() = if (LlamaBridge.ensureLoaded()) bridge.backendInfo() else "unavailable"
    val isStub: Boolean get() = backend != "llama.cpp"

    /**
     * True if this exact model+context is already resident in memory, so a
     * generation won't trigger a (slow) load. Lets the UI show the "loading"
     * indicator only when an actual load will happen.
     */
    @Synchronized
    fun isModelResident(modelPath: String, params: InferenceParams): Boolean =
        handle != 0L &&
            loadedModelPath == modelPath &&
            loadedContext == params.contextWindow &&
            !isStub

    @Synchronized
    fun ensureModelLoaded(modelPath: String, params: InferenceParams): Boolean {
        if (!LlamaBridge.ensureLoaded()) {
            Log.w(TAG, "Native lib not loadable; running in simulation mode")
            return false
        }
        if (handle != 0L && loadedModelPath == modelPath && loadedContext == params.contextWindow) {
            return true // already resident with the right settings
        }
        unload()
        if (!File(modelPath).exists() && !isStub) {
            Log.e(TAG, "Model file missing: $modelPath")
            return false
        }
        handle = bridge.nativeLoadModel(
            path = modelPath,
            nCtx = params.contextWindow,
            nThreads = params.threads,
            nGpuLayers = params.gpuLayers,
        )
        loadedModelPath = modelPath
        loadedContext = params.contextWindow
        Log.i(TAG, "Loaded model handle=$handle ctx=${params.contextWindow} backend=$backend")
        return handle != 0L
    }

    @Synchronized
    fun unload() {
        if (handle != 0L) {
            runCatching { bridge.nativeFree(handle) }
            handle = 0L
            loadedModelPath = null
            loadedContext = 0
        }
    }

    /**
     * Generates a reply to [history], emitting incremental text chunks as they
     * are produced. The terminal emission is the full text.
     */
    fun generate(
        modelPath: String,
        history: List<ChatMessage>,
        params: InferenceParams,
        agentMode: Boolean = false,
    ): Flow<String> = flow {
        val ready = ensureModelLoaded(modelPath, params)

        if (!ready || isStub) {
            emitAll(simulate(history, agentMode))
            return@flow
        }

        // ---- Real backend path ----
        val prompt = buildPrompt(history, agentMode)
        val text = try {
            bridge.nativeGenerate(
                handle = handle,
                prompt = prompt,
                maxTokens = params.maxTokens,
                temperature = params.temperature,
                topP = params.topP,
                topK = params.topK,
                seed = params.seed,
            )
        } catch (t: Throwable) {
            Log.w(TAG, "Native generate failed, simulating", t)
            ""
        }

        if (text.isBlank()) {
            // Couldn't generate (stub or error) — fall back so the UI still responds.
            emitAll(simulate(history, agentMode))
            return@flow
        }

        // The native call is blocking; replay the finished text word-by-word so
        // the UI still feels like it's streaming.
        for (word in text.trim().split(" ")) {
            emit("$word ")
            delay(8)
        }
    }.flowOn(Dispatchers.Default)

    /** Builds a chat-style prompt. Kept simple; real templates are model-specific. */
    private fun buildPrompt(history: List<ChatMessage>, agentMode: Boolean): String =
        buildString {
            if (agentMode) {
                append(AGENT_SYSTEM_PROMPT).append('\n')
            }
            history.forEach { m ->
                val tag = when (m.role) {
                    Role.USER -> "User"
                    Role.ASSISTANT -> "Assistant"
                    Role.SYSTEM -> "System"
                }
                append(tag).append(": ").append(m.content).append('\n')
            }
            append("Assistant: ")
        }

    /** Token-by-token simulated stream so the UI works without a native backend. */
    private fun simulate(history: List<ChatMessage>, agentMode: Boolean): Flow<String> = flow {
        val lastUser = history.lastOrNull { it.role == Role.USER }?.content?.trim().orEmpty()
        val reply = if (agentMode) simulatedAgentReply(lastUser) else
            "[Simulated reply — install the native backend via " +
                "scripts/setup_native.sh for real inference]\n\n" +
                "You said: \"$lastUser\". On a real device this response would be " +
                "generated locally by the selected GGUF model, fully offline."
        for (word in reply.split(" ")) {
            emit("$word ")
            delay(20)
        }
    }

    /**
     * In simulation, the agent still emits a *real* tool call so the whole
     * agent loop (parse → approve → execute on the device) is exercised end to
     * end even before the native backend is installed. The tool is chosen from
     * simple keywords in the user's message.
     */
    private fun simulatedAgentReply(userText: String): String {
        val lc = userText.lowercase()
        val (intro, toolJson) = when {
            listOf("app", "open", "launch").any { it in lc } ->
                "Sure — let me look at what's installed." to
                    """{"tool":"list_apps","args":{}}"""
            listOf("file", "folder", "dir", "list", "read", "write").any { it in lc } ->
                "I'll check the app's files for you." to
                    """{"tool":"list_files","args":{"path":"."}}"""
            else ->
                "Let me inspect this device first." to
                    """{"tool":"device_info","args":{}}"""
        }
        return "$intro\n\n```tool\n$toolJson\n```\n\n" +
            "_(Simulated agent — install the native backend for a real model to drive these tools.)_"
    }

    companion object {
        private const val TAG = "LlamaEngine"

        /** Teaches the model the tool-call format the agent loop understands. */
        private val AGENT_SYSTEM_PROMPT = """
            System: You are an on-device agent that can act on the user's phone.
            To use a tool, output a fenced block exactly like:
            ```tool
            {"tool":"list_files","args":{"path":"."}}
            ```
            Available tools: device_info, list_files, read_file, write_file, make_dir,
            delete_file, list_apps, open_app, run_command. Use one tool per block, then
            wait for the result before continuing. Explain briefly what you're doing.
        """.trimIndent()
    }
}
