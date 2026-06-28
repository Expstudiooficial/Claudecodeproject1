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

        // ---------------- 10 more models ----------------
        CatalogModel(
            id = "llama32-1b-it-q4",
            displayName = "Llama 3.2 1B Instruct",
            publisher = "Meta",
            description = "Tiny and fast — runs on almost anything. Great for quick replies " +
                "and very low-RAM devices.",
            paramsBillions = 1.2f,
            quantization = "Q4_K_M",
            fileSizeBytes = 808_000_000L,
            minRamMb = 1100,
            recommendedRamMb = 1600,
            maxContextWindow = 131072,
            downloadUrl = "$HF/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf",
            fileName = "Llama-3.2-1B-Instruct-Q4_K_M.gguf",
        ),
        CatalogModel(
            id = "qwen25-0_5b-it-q4",
            displayName = "Qwen2.5 0.5B Instruct",
            publisher = "Alibaba",
            description = "The smallest pick — astonishingly capable for its size and feather " +
                "light on RAM. Ideal for the weakest devices.",
            paramsBillions = 0.5f,
            quantization = "Q4_K_M",
            fileSizeBytes = 491_000_000L,
            minRamMb = 800,
            recommendedRamMb = 1200,
            maxContextWindow = 32768,
            downloadUrl = "$HF/bartowski/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/Qwen2.5-0.5B-Instruct-Q4_K_M.gguf",
            fileName = "Qwen2.5-0.5B-Instruct-Q4_K_M.gguf",
        ),
        CatalogModel(
            id = "qwen25-7b-it-q4",
            displayName = "Qwen2.5 7B Instruct",
            publisher = "Alibaba",
            description = "Flagship-quality reasoning, coding and multilingual chat for 6 GB+ " +
                "devices.",
            paramsBillions = 7.6f,
            quantization = "Q4_K_M",
            fileSizeBytes = 4_680_000_000L,
            minRamMb = 4900,
            recommendedRamMb = 6500,
            maxContextWindow = 32768,
            downloadUrl = "$HF/bartowski/Qwen2.5-7B-Instruct-GGUF/resolve/main/Qwen2.5-7B-Instruct-Q4_K_M.gguf",
            fileName = "Qwen2.5-7B-Instruct-Q4_K_M.gguf",
        ),
        CatalogModel(
            id = "smollm2-1_7b-it-q4",
            displayName = "SmolLM2 1.7B Instruct",
            publisher = "Hugging Face",
            description = "Efficient small model trained on a huge, clean corpus. Snappy and " +
                "surprisingly fluent.",
            paramsBillions = 1.7f,
            quantization = "Q4_K_M",
            fileSizeBytes = 1_060_000_000L,
            minRamMb = 1400,
            recommendedRamMb = 2000,
            maxContextWindow = 8192,
            downloadUrl = "$HF/bartowski/SmolLM2-1.7B-Instruct-GGUF/resolve/main/SmolLM2-1.7B-Instruct-Q4_K_M.gguf",
            fileName = "SmolLM2-1.7B-Instruct-Q4_K_M.gguf",
        ),
        CatalogModel(
            id = "tinyllama-1_1b-chat-q4",
            displayName = "TinyLlama 1.1B Chat",
            publisher = "TinyLlama",
            description = "A classic ultra-small chat model. Very fast; good for simple tasks " +
                "and constrained devices.",
            paramsBillions = 1.1f,
            quantization = "Q4_K_M",
            fileSizeBytes = 669_000_000L,
            minRamMb = 1000,
            recommendedRamMb = 1500,
            maxContextWindow = 2048,
            downloadUrl = "$HF/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf",
            fileName = "tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf",
        ),
        CatalogModel(
            id = "phi3-mini-4k-q4",
            displayName = "Phi-3 Mini 4K Instruct",
            publisher = "Microsoft",
            description = "The original Phi-3 Mini — strong reasoning in a compact package, " +
                "with a 4K context.",
            paramsBillions = 3.8f,
            quantization = "Q4_K_M",
            fileSizeBytes = 2_320_000_000L,
            minRamMb = 2600,
            recommendedRamMb = 3500,
            maxContextWindow = 4096,
            downloadUrl = "$HF/bartowski/Phi-3.1-mini-4k-instruct-GGUF/resolve/main/Phi-3.1-mini-4k-instruct-Q4_K_M.gguf",
            fileName = "Phi-3.1-mini-4k-instruct-Q4_K_M.gguf",
        ),
        CatalogModel(
            id = "gemma2-9b-it-q4",
            displayName = "Gemma 2 9B Instruct",
            publisher = "Google",
            description = "High-end Gemma for flagship phones (8 GB+). Excellent writing and " +
                "reasoning.",
            paramsBillions = 9.2f,
            quantization = "Q4_K_M",
            fileSizeBytes = 5_760_000_000L,
            minRamMb = 6000,
            recommendedRamMb = 8000,
            maxContextWindow = 8192,
            downloadUrl = "$HF/bartowski/gemma-2-9b-it-GGUF/resolve/main/gemma-2-9b-it-Q4_K_M.gguf",
            fileName = "gemma-2-9b-it-Q4_K_M.gguf",
        ),
        CatalogModel(
            id = "deepseek-r1-qwen-1_5b-q4",
            displayName = "DeepSeek-R1 Distill Qwen 1.5B",
            publisher = "DeepSeek",
            description = "A distilled reasoning model that 'thinks' step by step — strong at " +
                "math and logic for its tiny size.",
            paramsBillions = 1.5f,
            quantization = "Q4_K_M",
            fileSizeBytes = 1_120_000_000L,
            minRamMb = 1400,
            recommendedRamMb = 2000,
            maxContextWindow = 32768,
            downloadUrl = "$HF/bartowski/DeepSeek-R1-Distill-Qwen-1.5B-GGUF/resolve/main/DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf",
            fileName = "DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf",
        ),
        CatalogModel(
            id = "llama31-8b-it-q4",
            displayName = "Llama 3.1 8B Instruct",
            publisher = "Meta",
            description = "Meta's well-rounded 8B — a great general assistant for 6 GB+ " +
                "devices with a huge context window.",
            paramsBillions = 8.0f,
            quantization = "Q4_K_M",
            fileSizeBytes = 4_920_000_000L,
            minRamMb = 5200,
            recommendedRamMb = 7000,
            maxContextWindow = 131072,
            downloadUrl = "$HF/bartowski/Meta-Llama-3.1-8B-Instruct-GGUF/resolve/main/Meta-Llama-3.1-8B-Instruct-Q4_K_M.gguf",
            fileName = "Meta-Llama-3.1-8B-Instruct-Q4_K_M.gguf",
        ),
        CatalogModel(
            id = "stablelm2-zephyr-1_6b-q4",
            displayName = "StableLM 2 Zephyr 1.6B",
            publisher = "Stability AI",
            description = "Compact, friendly chat model tuned for helpful conversation on " +
                "low-RAM devices.",
            paramsBillions = 1.6f,
            quantization = "Q4_K_M",
            fileSizeBytes = 1_030_000_000L,
            minRamMb = 1300,
            recommendedRamMb = 1900,
            maxContextWindow = 4096,
            downloadUrl = "$HF/bartowski/stablelm-2-zephyr-1_6b-GGUF/resolve/main/stablelm-2-zephyr-1_6b-Q4_K_M.gguf",
            fileName = "stablelm-2-zephyr-1_6b-Q4_K_M.gguf",
        ),
    )

    val recommended: List<CatalogModel> get() = models.filter { it.isRecommended }

    fun byId(id: String): CatalogModel? = models.firstOrNull { it.id == id }
}
