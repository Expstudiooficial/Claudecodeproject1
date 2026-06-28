package com.expstudio.localai.data.model

/**
 * Tunable sampling / runtime parameters for a generation run. Produced by the
 * user via the Settings UI (now fully hand-tunable) or automatically by
 * [com.expstudio.localai.smart.SmartConfiguratorEngine].
 *
 * Every field maps to a real llama.cpp knob so power users get 100% control;
 * Smart Setup fills them all in for you based on the live device profile.
 */
data class InferenceParams(
    // ---- Core sampling ----
    val temperature: Float = 0.7f,
    val topP: Float = 0.95f,
    val topK: Int = 40,
    val minP: Float = 0.05f,
    val typicalP: Float = 1.0f,

    // ---- Repetition control ----
    val repeatPenalty: Float = 1.1f,
    val frequencyPenalty: Float = 0.0f,
    val presencePenalty: Float = 0.0f,
    val repeatLastN: Int = 64,

    // ---- Mirostat (adaptive perplexity) ----
    val mirostat: Int = 0,          // 0 = off, 1 = v1, 2 = v2
    val mirostatTau: Float = 5.0f,
    val mirostatEta: Float = 0.1f,
    val tfsZ: Float = 1.0f,         // tail-free sampling
    val seed: Int = -1,             // -1 = random each run

    // ---- Runtime / performance ----
    val contextWindow: Int = 4096,
    val maxTokens: Int = 512,
    val threads: Int = 4,
    /** layers offloaded to GPU/NPU when available; 0 = pure CPU. */
    val gpuLayers: Int = 0,
    val batchSize: Int = 256,
    val useMmap: Boolean = true,
    val useMlock: Boolean = false,
    val flashAttention: Boolean = false,

    // ---- Behaviour ----
    val systemPrompt: String = "",
    /** newline-separated stop strings. */
    val stopSequences: String = "",
    val streamResponses: Boolean = true,
) {
    companion object {
        val DEFAULT = InferenceParams()
    }
}
