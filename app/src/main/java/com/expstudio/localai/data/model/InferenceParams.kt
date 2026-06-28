package com.expstudio.localai.data.model

/**
 * Tunable sampling / runtime parameters for a generation run. Produced by the
 * user via the Settings UI or automatically by [com.expstudio.localai.smart.SmartConfiguratorEngine].
 */
data class InferenceParams(
    val temperature: Float = 0.7f,
    val topP: Float = 0.95f,
    val repeatPenalty: Float = 1.1f,
    val contextWindow: Int = 4096,
    val maxTokens: Int = 512,
    val threads: Int = 4,
    /** layers offloaded to GPU/NPU when available; 0 = pure CPU. */
    val gpuLayers: Int = 0,
    val batchSize: Int = 256,
) {
    companion object {
        val DEFAULT = InferenceParams()
    }
}
