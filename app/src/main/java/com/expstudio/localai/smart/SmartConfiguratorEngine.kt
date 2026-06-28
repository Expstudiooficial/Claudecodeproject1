package com.expstudio.localai.smart

import com.expstudio.localai.data.model.CatalogModel
import com.expstudio.localai.data.model.InferenceParams
import com.expstudio.localai.data.model.ModelCatalog
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The brain behind the one-tap **Smart Setup** button.
 *
 * Given a live [DeviceProfile] it picks the best model the device can actually
 * run and tunes every inference parameter — deliberately conservative so the
 * app coexists with the OS and other apps instead of getting OOM-killed.
 *
 * Design choices:
 *  - Budgets from **available** RAM (not total), then keeps a safety reserve so
 *    background apps / the OS don't push us over the low-memory threshold.
 *  - Prefers an already-installed model; otherwise recommends the best catalog
 *    pick that fits (the UI then offers to download it).
 *  - Scales the context window to whatever RAM is left after loading weights.
 */
class SmartConfiguratorEngine {

    /**
     * @param installedIds ids of models already downloaded (preferred).
     * @param aggressiveness 0f = maximum safety margin, 1f = use more RAM.
     */
    fun recommend(
        device: DeviceProfile,
        installedIds: Set<String> = emptySet(),
        aggressiveness: Float = 0.5f,
    ): SmartRecommendation {
        val reasons = mutableListOf<String>()

        // 1) How much RAM can we responsibly claim?
        //    Start from what's free, then reserve headroom for the OS + other apps.
        //    Reserve scales: tighter on low-RAM devices, looser when there's plenty.
        val reservePct = lerp(0.40f, 0.22f, aggressiveness)
        val reserveMb = max(device.memoryThresholdMb * 2, (device.availableRamMb * reservePct).toLong())
        val budgetMb = (device.availableRamMb - reserveMb).coerceAtLeast(0)

        reasons += "Device reports ${device.availableRamMb} MB free of " +
            "${device.totalRamMb} MB (${device.usedRamMb} MB already in use by the OS & apps)."
        reasons += "Reserving ${reserveMb} MB so the system and background apps stay healthy → " +
            "usable budget ≈ ${budgetMb} MB."
        if (device.isLowRamDevice || device.lowMemoryState) {
            reasons += "Detected a low-RAM / memory-pressured state — being extra conservative."
        }

        // 2) Pick the best model that fits the budget.
        //    Loading a GGUF needs ~ file size + a working overhead.
        val candidates = ModelCatalog.models.sortedByDescending { it.paramsBillions }
        val installedFirst = candidates.sortedByDescending { it.id in installedIds }

        val chosen = installedFirst.firstOrNull { fitsToLoad(it, budgetMb) }
            ?: ModelCatalog.byId("qwen25-1_5b-it-q4")!! // smallest safe fallback

        if (chosen.id in installedIds) {
            reasons += "Chose \"${chosen.displayName}\" — already installed and fits the budget."
        } else {
            reasons += "Chose \"${chosen.displayName}\" (${chosen.sizeGbText}) as the largest model " +
                "that fits; it will be downloaded."
        }

        // 3) Size the context window to leftover RAM after loading weights.
        val loadMb = loadFootprintMb(chosen)
        val leftoverMb = (budgetMb - loadMb).coerceAtLeast(64)
        val kvKbPerToken = kvCacheKbPerToken(chosen)
        val ctxByRam = ((leftoverMb * 1024.0) / kvKbPerToken).toInt()
        val contextWindow = ctxByRam
            .coerceIn(512, chosen.maxContextWindow)
            .roundDownTo(512)
        reasons += "Context window set to $contextWindow tokens " +
            "(${leftoverMb} MB left after weights ÷ ~${kvKbPerToken.roundToInt()} KB/token, " +
            "capped at the model's ${chosen.maxContextWindow})."
        if (contextWindow < 2048) {
            reasons += "RAM is tight, so context is kept small to avoid out-of-memory crashes."
        }

        // 4) Threads — leave cores for the UI/OS so the phone stays responsive.
        val threads = max(2, min(device.cpuCores - 2, 6))
        reasons += "Using $threads of ${device.cpuCores} CPU cores (leaving some for the UI & system)."

        // 5) Batch size — smaller when RAM is constrained.
        val batchSize = when {
            budgetMb < 1500 -> 64
            budgetMb < 3000 -> 128
            else -> 256
        }

        val params = InferenceParams(
            temperature = 0.7f,
            topP = 0.95f,
            repeatPenalty = 1.1f,
            contextWindow = contextWindow,
            maxTokens = if (contextWindow >= 4096) 768 else 384,
            threads = threads,
            gpuLayers = 0, // CPU-first; GPU/NPU offload is a later milestone
            batchSize = batchSize,
        )

        val tokPerSec = estimateTokensPerSecond(chosen, threads, device)
        reasons += "Estimated speed ≈ %.1f tokens/sec on this device.".format(tokPerSec)

        return SmartRecommendation(
            model = chosen,
            isInstalled = chosen.id in installedIds,
            params = params,
            reasoning = reasons,
            estimatedTokensPerSecond = tokPerSec,
            device = device,
        )
    }

    // ---- heuristics ----

    /** Weights + runtime overhead, roughly. */
    private fun loadFootprintMb(m: CatalogModel): Long =
        (m.fileSizeBytes / (1024L * 1024L) * 1.12).toLong() + 128

    /** A model fits to load if its footprint plus a minimal 512-token context fits. */
    private fun fitsToLoad(m: CatalogModel, budgetMb: Long): Boolean {
        val minCtxMb = (512 * kvCacheKbPerToken(m) / 1024.0).toLong()
        return loadFootprintMb(m) + minCtxMb <= budgetMb
    }

    /** Rough KV-cache cost per token; grows with model size. */
    private fun kvCacheKbPerToken(m: CatalogModel): Double = 90.0 + m.paramsBillions * 70.0

    /** Very rough throughput model: smaller models + more cores → faster. */
    private fun estimateTokensPerSecond(m: CatalogModel, threads: Int, device: DeviceProfile): Float {
        val base = 30f / m.paramsBillions          // smaller = faster
        val coreFactor = 0.6f + 0.1f * threads      // diminishing returns
        val penalty = if (device.isLowRamDevice) 0.6f else 1f
        return (base * coreFactor * penalty).coerceIn(0.5f, 60f)
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)
    private fun Int.roundDownTo(step: Int) = (this / step) * step
}
