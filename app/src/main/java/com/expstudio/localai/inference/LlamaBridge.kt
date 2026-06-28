package com.expstudio.localai.inference

/**
 * Thin JNI surface over the native llama.cpp backend (see cpp/llama_bridge.cpp).
 *
 * This class is intentionally dumb: it only marshals to/from native. All policy
 * (threading, params, lifecycle) lives in [LlamaInferenceEngine].
 *
 * When the native lib is built in STUB mode (llama.cpp not fetched),
 * [backendInfo] returns "stub" and [nativeLoadModel] returns a fake handle so
 * the rest of the app keeps working for UI development.
 */
class LlamaBridge {

    /** @return opaque native handle, or 0 on failure. */
    external fun nativeLoadModel(
        path: String,
        nCtx: Int,
        nThreads: Int,
        nGpuLayers: Int,
    ): Long

    /**
     * Runs a blocking completion and returns the full generated text. Empty
     * string signals the caller to fall back (e.g. in stub mode).
     */
    external fun nativeGenerate(
        handle: Long,
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        topP: Float,
        topK: Int,
        seed: Int,
    ): String

    external fun nativeFree(handle: Long)

    /** "llama.cpp" for a real backend, "stub" otherwise. */
    external fun nativeBackendInfo(): String

    fun backendInfo(): String = runCatching { nativeBackendInfo() }.getOrDefault("unavailable")

    val isStub: Boolean get() = backendInfo() != "llama.cpp"

    companion object {
        @Volatile private var loaded = false

        /** Loads liblocalai.so once. Safe to call repeatedly. */
        fun ensureLoaded(): Boolean {
            if (loaded) return true
            return synchronized(this) {
                if (loaded) return true
                runCatching { System.loadLibrary("localai") }
                    .onSuccess { loaded = true }
                    .isSuccess
            }
        }
    }
}
