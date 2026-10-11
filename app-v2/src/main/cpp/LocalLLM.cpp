#include <jni.h>
#include <string>
#include <vector>
#include <sstream>
#include <atomic>
#include <android/log.h>
#include "llama.h"

#define LOG_TAG "AgriEdgeNativeLLM"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

struct LLMContext {
    std::string modelPath;
    int nThreads;
    llama_model* model = nullptr;
    llama_context* ctx = nullptr;
    const llama_vocab* vocab = nullptr;
    std::atomic<bool> stopRequested{false};
    bool isLoaded{false};
};

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_agriedge_app_llm_LocalLLM_nativeInit(
    JNIEnv* env,
    jobject /* thiz */,
    jstring modelPath_,
    jint nThreads
) {
    const char* modelPath = env->GetStringUTFChars(modelPath_, nullptr);
    LOGI("Initializing native SLM from path: %s with %d threads", modelPath, nThreads);

    LLMContext* ctx = new LLMContext();
    ctx->modelPath = modelPath;
    ctx->nThreads = nThreads > 0 ? nThreads : 4;

    llama_backend_init();

    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0; // CPU execution on Android device

    ctx->model = llama_model_load_from_file(modelPath, model_params);
    env->ReleaseStringUTFChars(modelPath_, modelPath);

    if (!ctx->model) {
        LOGE("Failed to load llama model from file: %s", ctx->modelPath.c_str());
        delete ctx;
        return 0;
    }

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = 2048;
    ctx_params.n_threads = ctx->nThreads;
    ctx_params.n_threads_batch = ctx->nThreads;

    ctx->ctx = llama_init_from_model(ctx->model, ctx_params);
    if (!ctx->ctx) {
        LOGE("Failed to create llama context");
        llama_model_free(ctx->model);
        delete ctx;
        return 0;
    }

    ctx->vocab = llama_model_get_vocab(ctx->model);
    ctx->isLoaded = true;
    LOGI("Successfully loaded GGUF model and initialized llama.cpp context!");

    return reinterpret_cast<jlong>(ctx);
}

JNIEXPORT jboolean JNICALL
Java_com_agriedge_app_llm_LocalLLM_nativeIsReady(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong handle
) {
    if (handle == 0) return JNI_FALSE;
    LLMContext* ctx = reinterpret_cast<LLMContext*>(handle);
    return ctx->isLoaded ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_agriedge_app_llm_LocalLLM_nativeGenerate(
    JNIEnv* env,
    jobject /* thiz */,
    jlong handle,
    jstring prompt_,
    jint maxTokens,
    jfloat temperature,
    jobject callback
) {
    if (handle == 0 || callback == nullptr) return;
    LLMContext* ctx = reinterpret_cast<LLMContext*>(handle);
    if (!ctx->isLoaded || !ctx->ctx || !ctx->vocab) return;

    ctx->stopRequested.store(false);

    const char* promptCStr = env->GetStringUTFChars(prompt_, nullptr);
    std::string prompt(promptCStr);
    env->ReleaseStringUTFChars(prompt_, promptCStr);

    LOGI("Starting real SLM inference for prompt (%zu chars), maxTokens=%d", prompt.length(), maxTokens);

    jclass callbackClass = env->GetObjectClass(callback);
    jmethodID onTokenMethod = env->GetMethodID(callbackClass, "onToken", "(Ljava/lang/String;)V");
    if (onTokenMethod == nullptr) {
        LOGE("Failed to find onToken method on callback object");
        return;
    }

    // Tokenize prompt
    std::vector<llama_token> tokens(prompt.length() + 32);
    int n_tokens = llama_tokenize(
        ctx->vocab,
        prompt.c_str(),
        static_cast<int32_t>(prompt.length()),
        tokens.data(),
        static_cast<int32_t>(tokens.size()),
        true,  // add_special (BOS)
        true   // parse_special
    );

    if (n_tokens < 0) {
        tokens.resize(-n_tokens);
        n_tokens = llama_tokenize(
            ctx->vocab,
            prompt.c_str(),
            static_cast<int32_t>(prompt.length()),
            tokens.data(),
            static_cast<int32_t>(tokens.size()),
            true,
            true
        );
    }

    if (n_tokens <= 0) {
        LOGE("Failed to tokenize prompt");
        return;
    }
    tokens.resize(n_tokens);

    // Initialize sampler
    auto sparams = llama_sampler_chain_default_params();
    llama_sampler* sampler = llama_sampler_chain_init(sparams);
    if (temperature <= 0.0f) {
        llama_sampler_chain_add(sampler, llama_sampler_init_greedy());
    } else {
        llama_sampler_chain_add(sampler, llama_sampler_init_temp(temperature));
        llama_sampler_chain_add(sampler, llama_sampler_init_dist(1337));
    }

    // Decode initial prompt batch
    llama_batch batch = llama_batch_get_one(tokens.data(), n_tokens);
    if (llama_decode(ctx->ctx, batch) != 0) {
        LOGE("llama_decode failed on prompt batch");
        llama_sampler_free(sampler);
        return;
    }

    int n_generated = 0;
    while (n_generated < maxTokens && !ctx->stopRequested.load()) {
        llama_token new_token_id = llama_sampler_sample(sampler, ctx->ctx, -1);

        if (llama_vocab_is_eog(ctx->vocab, new_token_id)) {
            LOGI("Reached End-Of-Generation token");
            break;
        }

        char pieceBuf[256];
        int n_piece = llama_token_to_piece(ctx->vocab, new_token_id, pieceBuf, sizeof(pieceBuf), 0, true);
        if (n_piece > 0) {
            std::string pieceStr(pieceBuf, n_piece);
            jstring jToken = env->NewStringUTF(pieceStr.c_str());
            env->CallVoidMethod(callback, onTokenMethod, jToken);
            env->DeleteLocalRef(jToken);
        }

        // Decode single new token
        llama_batch single_batch = llama_batch_get_one(&new_token_id, 1);
        if (llama_decode(ctx->ctx, single_batch) != 0) {
            LOGE("llama_decode failed during generation loop");
            break;
        }

        n_generated++;
    }

    llama_sampler_free(sampler);
    LOGI("Finished SLM inference (generated %d tokens)", n_generated);
}

JNIEXPORT void JNICALL
Java_com_agriedge_app_llm_LocalLLM_nativeStop(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong handle
) {
    if (handle == 0) return;
    LLMContext* ctx = reinterpret_cast<LLMContext*>(handle);
    ctx->stopRequested.store(true);
    LOGI("Stop requested for SLM context");
}

JNIEXPORT void JNICALL
Java_com_agriedge_app_llm_LocalLLM_nativeRelease(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong handle
) {
    if (handle == 0) return;
    LLMContext* ctx = reinterpret_cast<LLMContext*>(handle);
    if (ctx->ctx) {
        llama_free(ctx->ctx);
        ctx->ctx = nullptr;
    }
    if (ctx->model) {
        llama_model_free(ctx->model);
        ctx->model = nullptr;
    }
    delete ctx;
    LOGI("Released native llama context & model");
}

} // extern "C"
