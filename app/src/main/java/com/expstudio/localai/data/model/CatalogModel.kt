package com.expstudio.localai.data.model

/**
 * A downloadable model entry. The catalog ships a curated set of
 * mobile-optimised GGUF builds; users pick one and the app downloads it.
 *
 * Sizes/RAM are approximate and used by the Smart Configurator to decide what
 * a device can comfortably run *alongside the OS and other apps*.
 */
data class CatalogModel(
    val id: String,
    val displayName: String,
    val publisher: String,
    val description: String,
    val paramsBillions: Float,
    val quantization: String,
    val fileSizeBytes: Long,
    /** absolute minimum free RAM to load at a small context (MB). */
    val minRamMb: Int,
    /** comfortable free RAM for a roomy context (MB). */
    val recommendedRamMb: Int,
    val maxContextWindow: Int,
    val downloadUrl: String,
    val fileName: String,
    val sha256: String? = null,
    /** one of the four headline picks shown first in the UI. */
    val isRecommended: Boolean = false,
) {
    val sizeGbText: String get() = "%.1f GB".format(fileSizeBytes / 1_073_741_824.0)
}
