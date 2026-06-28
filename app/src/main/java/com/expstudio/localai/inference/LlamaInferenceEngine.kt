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
    ): Flow<String> = flow {
        val ready = ensureModelLoaded(modelPath, params)

        if (!ready || isStub) {
            emitAll(simulate(history))
            return@flow
        }

        // ---- Real backend path ----
        // The streaming sampling loop lives in native code once llama.cpp is
        // fetched (see cpp/llama_bridge.cpp). Until that native entry point is
        // present we fall back to simulation rather than crashing.
        try {
            // Placeholder for: bridge.nativeGenerate(handle, prompt, params) { token -> emit(token) }
            emitAll(simulate(history))
        } catch (t: UnsatisfiedLinkError) {
            Log.w(TAG, "Native generate not available, simulating", t)
            emitAll(simulate(history))
        }
    }.flowOn(Dispatchers.Default)

    /** Builds a chat-style prompt. Kept simple; real templates are model-specific. */
    private fun buildPrompt(history: List<ChatMessage>): String =
        buildString {
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
    private fun simulate(history: List<ChatMessage>): Flow<String> = flow {
        val lastUser = history.lastOrNull { it.role == Role.USER }?.content?.trim().orEmpty()
        val reply = "[Simulated reply — install the native backend via " +
            "scripts/setup_native.sh for real inference]\n\n" +
            "You said: \"$lastUser\". On a real device this response would be " +
            "generated locally by the selected GGUF model, fully offline."
        for (word in reply.split(" ")) {
            emit("$word ")
            delay(25)
        }
    }

    companion object { private const val TAG = "LlamaEngine" }
}
