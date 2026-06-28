// JNI bridge between the Kotlin LlamaBridge and the native llama.cpp backend.
//
// When compiled with HAVE_LLAMA (i.e. llama.cpp present), the calls drive the
// real backend and generate actual tokens. Otherwise we compile a stub so the
// app still links and the UI can be developed without the native checkout.

#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>

#define LOG_TAG "LlamaBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#ifdef HAVE_LLAMA
#include "llama.h"

// The model is loaded once and kept resident. A fresh context is created per
// generation so each reply starts from a clean KV cache fed the full prompt —
// simple and robust, and it avoids cross-conversation state leaking.
struct LlamaSession {
    llama_model* model    = nullptr;
    int          n_ctx    = 4096;
    int          n_threads = 4;
};

extern "C"
JNIEXPORT jlong JNICALL
Java_com_expstudio_localai_inference_LlamaBridge_nativeLoadModel(
        JNIEnv* env, jobject /*thiz*/, jstring path, jint nCtx, jint nThreads, jint nGpuLayers) {
    const char* cpath = env->GetStringUTFChars(path, nullptr);

    llama_backend_init();
    llama_model_params mparams = llama_model_default_params();
    mparams.n_gpu_layers = nGpuLayers;

    llama_model* model = llama_model_load_from_file(cpath, mparams);
    env->ReleaseStringUTFChars(path, cpath);
    if (!model) { LOGE("Failed to load model"); return 0; }

    auto* s = new LlamaSession();
    s->model = model;
    s->n_ctx = nCtx > 0 ? nCtx : 4096;
    s->n_threads = nThreads > 0 ? nThreads : 4;
    LOGI("Model loaded (n_ctx=%d, threads=%d, gpu_layers=%d)", s->n_ctx, s->n_threads, nGpuLayers);
    return reinterpret_cast<jlong>(s);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_expstudio_localai_inference_LlamaBridge_nativeGenerate(
        JNIEnv* env, jobject /*thiz*/, jlong handle, jstring prompt,
        jint maxTokens, jfloat temperature, jfloat topP, jint topK, jint seed) {
    auto* s = reinterpret_cast<LlamaSession*>(handle);
    if (!s || !s->model) return env->NewStringUTF("");

    const llama_vocab* vocab = llama_model_get_vocab(s->model);

    const char* cprompt = env->GetStringUTFChars(prompt, nullptr);
    const std::string text(cprompt ? cprompt : "");
    env->ReleaseStringUTFChars(prompt, cprompt);

    // Fresh context per call → clean KV cache.
    llama_context_params cparams = llama_context_default_params();
    cparams.n_ctx          = (uint32_t) s->n_ctx;
    cparams.n_batch        = (uint32_t) s->n_ctx;
    cparams.n_threads      = s->n_threads;
    cparams.n_threads_batch = s->n_threads;
    llama_context* ctx = llama_init_from_model(s->model, cparams);
    if (!ctx) { LOGE("Failed to create context"); return env->NewStringUTF(""); }

    // Tokenize the full prompt (add BOS / special tokens).
    int n_tokens = -llama_tokenize(vocab, text.c_str(), (int) text.size(),
                                   nullptr, 0, true, true);
    std::vector<llama_token> tokens(n_tokens);
    if (llama_tokenize(vocab, text.c_str(), (int) text.size(),
                       tokens.data(), (int) tokens.size(), true, true) < 0) {
        llama_free(ctx);
        return env->NewStringUTF("");
    }

    // Sampler chain: top-k -> top-p -> temperature -> distribution.
    llama_sampler* smpl = llama_sampler_chain_init(llama_sampler_chain_default_params());
    if (topK > 0) llama_sampler_chain_add(smpl, llama_sampler_init_top_k(topK));
    llama_sampler_chain_add(smpl, llama_sampler_init_top_p(topP, 1));
    llama_sampler_chain_add(smpl, llama_sampler_init_temp(temperature));
    llama_sampler_chain_add(smpl, llama_sampler_init_dist(
            seed < 0 ? LLAMA_DEFAULT_SEED : (uint32_t) seed));

    std::string result;
    llama_batch batch = llama_batch_get_one(tokens.data(), (int) tokens.size());
    llama_token new_id;
    int n_past = 0;
    int generated = 0;
    while (generated < maxTokens) {
        if (n_past + batch.n_tokens > s->n_ctx) break; // out of context room
        if (llama_decode(ctx, batch) != 0) break;
        n_past += batch.n_tokens;

        new_id = llama_sampler_sample(smpl, ctx, -1);
        if (llama_vocab_is_eog(vocab, new_id)) break;

        char buf[256];
        int n = llama_token_to_piece(vocab, new_id, buf, sizeof(buf), 0, true);
        if (n < 0) break;
        result.append(buf, n);
        generated++;

        batch = llama_batch_get_one(&new_id, 1);
    }

    llama_sampler_free(smpl);
    llama_free(ctx);
    return env->NewStringUTF(result.c_str());
}

extern "C"
JNIEXPORT void JNICALL
Java_com_expstudio_localai_inference_LlamaBridge_nativeFree(
        JNIEnv* /*env*/, jobject /*thiz*/, jlong handle) {
    auto* s = reinterpret_cast<LlamaSession*>(handle);
    if (!s) return;
    if (s->model) llama_model_free(s->model);
    delete s;
    llama_backend_free();
}

#else  // ---------------- STUB MODE ----------------

extern "C"
JNIEXPORT jlong JNICALL
Java_com_expstudio_localai_inference_LlamaBridge_nativeLoadModel(
        JNIEnv* /*env*/, jobject /*thiz*/, jstring /*path*/, jint /*nCtx*/,
        jint /*nThreads*/, jint /*nGpuLayers*/) {
    LOGI("STUB nativeLoadModel — llama.cpp not compiled in");
    return 1; // non-zero fake handle
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_expstudio_localai_inference_LlamaBridge_nativeGenerate(
        JNIEnv* env, jobject /*thiz*/, jlong /*handle*/, jstring /*prompt*/,
        jint /*maxTokens*/, jfloat /*temperature*/, jfloat /*topP*/, jint /*topK*/, jint /*seed*/) {
    return env->NewStringUTF(""); // empty → Kotlin falls back to simulation
}

extern "C"
JNIEXPORT void JNICALL
Java_com_expstudio_localai_inference_LlamaBridge_nativeFree(
        JNIEnv* /*env*/, jobject /*thiz*/, jlong /*handle*/) {
    LOGI("STUB nativeFree");
}

#endif

// Returns a short build identifier so Kotlin can tell stub from real backend.
extern "C"
JNIEXPORT jstring JNICALL
Java_com_expstudio_localai_inference_LlamaBridge_nativeBackendInfo(
        JNIEnv* env, jobject /*thiz*/) {
#ifdef HAVE_LLAMA
    return env->NewStringUTF("llama.cpp");
#else
    return env->NewStringUTF("stub");
#endif
}
