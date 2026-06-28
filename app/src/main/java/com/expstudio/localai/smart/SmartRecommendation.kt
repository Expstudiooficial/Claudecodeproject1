package com.expstudio.localai.smart

import com.expstudio.localai.data.model.CatalogModel
import com.expstudio.localai.data.model.InferenceParams

/**
 * Result of the Smart Setup: which model to use, the tuned parameters, and a
 * human-readable explanation of *why* — so the user understands (and can
 * override) every decision.
 */
data class SmartRecommendation(
    val model: CatalogModel,
    /** true when the model is already on disk; false means it must be downloaded first. */
    val isInstalled: Boolean,
    val params: InferenceParams,
    val reasoning: List<String>,
    val estimatedTokensPerSecond: Float,
    val device: DeviceProfile,
)
