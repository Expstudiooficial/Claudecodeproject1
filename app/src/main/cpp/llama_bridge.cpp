// JNI bridge between the Kotlin LlamaBridge and the native llama.cpp backend.
//
// When compiled with HAVE_LLAMA (i.e. llama.cpp present), the calls drive the
// real backend. Otherwise we compile a stub so the app still links and the UI
// can be developed without the multi-hundred-MB native checkout.

#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "LlamaBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#ifdef HAVE_LLAMA
#include "llama.h"

struct LlamaSession {
    llama_model*   model   = nullptr;
    llama_context* ctx     = nullptr;
};

extern "C"
JNIEXPORT jlong JNICALL
Java_com_expstudio_localai_inference_LlamaBridge_nativeLoadModel(
        JNIEnv* env, jobject /*thiz*/, jstring path, jint nCtx, jint nThreads, jint nGpuLayers) {
    const char* cpath = env->GetStringUTFChars(path, nullptr);

    llama_backend_init();
    llama_model_params mparams = llama_model_default_params();
    mparams.n_gpu_layers = nGpuLayers;

    llama_model* model = llama_load_model_from_file(cpath, mparams);
    env->ReleaseStringUTFChars(path, cpath);
    if (!model) { LOGE("Failed to load model"); return 0; }

    llama_context_params cparams = llama_context_default_params();
    cparams.n_ctx     = (uint32_t) nCtx;
    cparams.n_threads = nThreads;
    llama_context* ctx = llama_new_context_with_model(model, cparams);
    if (!ctx) { llama_free_model(model); return 0; }

    auto* s = new LlamaSession{model, ctx};
    LOGI("Model loaded (n_ctx=%d, threads=%d, gpu_layers=%d)", nCtx, nThreads, nGpuLayers);
    return reinterpret_cast<jlong>(s);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_expstudio_localai_inference_LlamaBridge_nativeFree(
        JNIEnv* /*env*/, jobject /*thiz*/, jlong handle) {
    auto* s = reinterpret_cast<LlamaSession*>(handle);
    if (!s) return;
    if (s->ctx) llama_free(s->ctx);
    if (s->model) llama_free_model(s->model);
    delete s;
    llama_backend_free();
}

// NOTE: token-by-token generation with a streaming callback into Kotlin is
// wired in LlamaBridge.kt; the full sampling loop lives here once the
// backend is fetched. Kept compact here for the scaffold.

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
