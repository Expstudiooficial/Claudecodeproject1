package com.expstudio.localai.data.model

/**
 * Curated, mobile-optimised model catalog.
 *
 * The four [CatalogModel.isRecommended] entries are the headline picks tuned to
 * run on modest devices (≈2–3 GB usable RAM) using 3–4 bit GGUF quantization.
 * The remainder are optional downloads for users with more headroom.
 *
 * Download URLs use HuggingFace `resolve/main` direct links. Sizes are
 * approximate and feed the Smart Configurator's RAM math.
 */
object ModelCatalog {

    private const val HF = "https://huggingface.co"

    val models: List<CatalogModel> = listOf(

        // ---------------- 4 headline picks ----------------
        CatalogModel(
            id = "gemma2-2b-it-q4",
            displayName = "Gemma 2 2B Instruct",
            publisher = "Google",
            description = "Great all-round chat model that fits on 2 GB-class devices. " +
                "Q4_K_M balances quality and footprint.",
            paramsBillions = 2.6f,
            quantization = "Q4_K_M",
            fileSizeBytes = 1_708_000_000L,
            minRamMb = 1800,
            recommendedRamMb = 2600,
            maxContextWindow = 8192,
            downloadUrl = "$HF/bartowski/gemma-2-2b-it-GGUF/resolve/main/gemma-2-2b-it-Q4_K_M.gguf",
            fileName = "gemma-2-2b-it-Q4_K_M.gguf",
            isRecommended = true,
        ),
        CatalogModel(
            id = "qwen25-1_5b-it-q4",
            displayName = "Qwen2.5 1.5B Instruct",
            publisher = "Alibaba",
            description = "Fastest pick — snappy on low-end phones, strong multilingual " +
                "support. Ideal when RAM is very tight (3-bit also available).",
            paramsBillions = 1.5f,
            quantization = "Q4_K_M",
            fileSizeBytes = 1_040_000_000L,
            minRamMb = 1300,
            recommendedRamMb = 2000,
            maxContextWindow = 32768,
            downloadUrl = "$HF/bartowski/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/Qwen2.5-1.5B-Instruct-Q4_K_M.gguf",
            fileName = "Qwen2.5-1.5B-Instruct-Q4_K_M.gguf",
            isRecommended = true,
        ),
        CatalogModel(
            id = "phi35-mini-q4",
            displayName = "Phi-3.5 Mini Instruct",
            publisher = "Microsoft",
            description = "Punches above its size on reasoning and code. The modern " +
                "successor to Phi-2. Needs a bit more RAM for long context.",
            paramsBillions = 3.8f,
            quantization = "Q4_K_M",
            fileSizeBytes = 2_390_000_000L,
            minRamMb = 2600,
            recommendedRamMb = 3600,
            maxContextWindow = 131072,
            downloadUrl = "$HF/bartowski/Phi-3.5-mini-instruct-GGUF/resolve/main/Phi-3.5-mini-instruct-Q4_K_M.gguf",
            fileName = "Phi-3.5-mini-instruct-Q4_K_M.gguf",
            isRecommended = true,
        ),
        CatalogModel(
            id = "mistral-7b-v03-q4",
            displayName = "Mistral 7B Instruct v0.3",
            publisher = "Mistral AI",
            description = "Best reasoning of the headline picks. For 4 GB+ devices; the " +
                "Smart Setup will only suggest it when there's real headroom.",
            paramsBillions = 7.2f,
            quantization = "Q4_K_M",
            fileSizeBytes = 4_370_000_000L,
            minRamMb = 4600,
            recommendedRamMb = 6000,
            maxContextWindow = 32768,
            downloadUrl = "$HF/bartowski/Mistral-7B-Instruct-v0.3-GGUF/resolve/main/Mistral-7B-Instruct-v0.3-Q4_K_M.gguf",
            fileName = "Mistral-7B-Instruct-v0.3-Q4_K_M.gguf",
            isRecommended = true,
        ),

        // ---------------- Additional downloads ----------------
        CatalogModel(
            id = "gemma3n-e2b-q4",
            displayName = "Gemma 3n E2B",
            publisher = "Google",
            description = "Next-gen efficient Gemma. ~2B effective params with strong " +
                "quality-per-byte; good long-context behaviour on phones.",
            paramsBillions = 2.0f,
            quantization = "Q4_K_M",
            fileSizeBytes = 2_020_000_000L,
            minRamMb = 2200,
            recommendedRamMb = 3000,
            maxContextWindow = 32768,
            downloadUrl = "$HF/unsloth/gemma-3n-E2B-it-GGUF/resolve/main/gemma-3n-E2B-it-Q4_K_M.gguf",
            fileName = "gemma-3n-E2B-it-Q4_K_M.gguf",
        ),
        CatalogModel(
            id = "gemma3n-e4b-q4",
            displayName = "Gemma 3n E4B",
            publisher = "Google",
            description = "Larger Gemma 3n for devices with more headroom. Better quality " +
                "than E2B at the cost of extra RAM.",
            paramsBillions = 4.0f,
            quantization = "Q4_K_M",
            fileSizeBytes = 3_640_000_000L,
            minRamMb = 3800,
            recommendedRamMb = 5000,
            maxContextWindow = 32768,
            downloadUrl = "$HF/unsloth/gemma-3n-E4B-it-GGUF/resolve/main/gemma-3n-E4B-it-Q4_K_M.gguf",
            fileName = "gemma-3n-E4B-it-Q4_K_M.gguf",
        ),
        CatalogModel(
            id = "qwen25-3b-it-q4",
            displayName = "Qwen2.5 3B Instruct",
            publisher = "Alibaba",
            description = "Step up from the 1.5B — noticeably stronger while still mobile " +
                "friendly on mid-range devices.",
            paramsBillions = 3.1f,
            quantization = "Q4_K_M",
            fileSizeBytes = 1_930_000_000L,
            minRamMb = 2200,
            recommendedRamMb = 3000,
            maxContextWindow = 32768,
            downloadUrl = "$HF/bartowski/Qwen2.5-3B-Instruct-GGUF/resolve/main/Qwen2.5-3B-Instruct-Q4_K_M.gguf",
            fileName = "Qwen2.5-3B-Instruct-Q4_K_M.gguf",
        ),
        CatalogModel(
            id = "llama32-3b-it-q4",
            displayName = "Llama 3.2 3B Instruct",
            publisher = "Meta",
            description = "Meta's compact instruct model — solid general assistant with " +
                "good instruction following.",
            paramsBillions = 3.2f,
            quantization = "Q4_K_M",
            fileSizeBytes = 2_020_000_000L,
            minRamMb = 2300,
            recommendedRamMb = 3100,
            maxContextWindow = 131072,
            downloadUrl = "$HF/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_M.gguf",
            fileName = "Llama-3.2-3B-Instruct-Q4_K_M.gguf",
        ),
    )

    val recommended: List<CatalogModel> get() = models.filter { it.isRecommended }

    fun byId(id: String): CatalogModel? = models.firstOrNull { it.id == id }
}
