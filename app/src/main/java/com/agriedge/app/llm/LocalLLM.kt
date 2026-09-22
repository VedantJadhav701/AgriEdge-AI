package com.agriedge.app.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

interface TokenCallback {
    fun onToken(token: String)
}

class LocalLLM(private val modelFile: File) {

    companion object {
        init {
            try {
                System.loadLibrary("agriedge_llm")
            } catch (e: UnsatisfiedLinkError) {
                // Handled gracefully in load() state check
            }
        }
    }

    private val _state = MutableStateFlow<LLMState>(LLMState.NotLoaded)
    val state: StateFlow<LLMState> = _state.asStateFlow()

    private var nativePtr: Long = 0

    val isReady: Boolean get() = _state.value is LLMState.Ready || _state.value is LLMState.Generating

    suspend fun load(nThreads: Int = 4): LLMState = withContext(Dispatchers.IO) {
        if (!modelFile.exists()) {
            val error = LLMState.Error("SLM model file not found at ${modelFile.absolutePath}")
            _state.value = error
            return@withContext error
        }

        _state.value = LLMState.Loading
        try {
            nativePtr = nativeInit(modelFile.absolutePath, nThreads)
            if (nativePtr != 0L) {
                val ready = LLMState.Ready
                _state.value = ready
                ready
            } else {
                val error = LLMState.Error("Failed to initialize llama.cpp native engine")
                _state.value = error
                error
            }
        } catch (e: Throwable) {
            val error = LLMState.Error(e.message ?: "Unknown error initializing native SLM")
            _state.value = error
            error
        }
    }

    suspend fun generate(
        prompt: String,
        maxTokens: Int = 256,
        temperature: Float = 0.7f,
        onTokenStream: (String) -> Unit = {}
    ): String = withContext(Dispatchers.Default) {
        check(nativePtr != 0L) { "generate() called before load() succeeded" }

        val sb = StringBuilder()
        _state.value = LLMState.Generating("")

        val callback = object : TokenCallback {
            override fun onToken(token: String) {
                sb.append(token)
                val text = sb.toString()
                _state.value = LLMState.Generating(text)
                onTokenStream(token)
            }
        }

        try {
            nativeGenerate(nativePtr, prompt, maxTokens, temperature, callback)
        } catch (e: Exception) {
            _state.value = LLMState.Error("SLM generation error: ${e.message}")
        }

        val result = sb.toString()
        _state.value = LLMState.Ready
        result
    }

    fun stopGeneration() {
        if (nativePtr != 0L) {
            nativeStop(nativePtr)
        }
    }

    fun release() {
        if (nativePtr != 0L) {
            nativeRelease(nativePtr)
            nativePtr = 0L
        }
        _state.value = LLMState.NotLoaded
    }

    private external fun nativeInit(modelPath: String, nThreads: Int): Long
    private external fun nativeIsReady(handle: Long): Boolean
    private external fun nativeGenerate(handle: Long, prompt: String, maxTokens: Int, temperature: Float, callback: TokenCallback)
    private external fun nativeStop(handle: Long)
    private external fun nativeRelease(handle: Long)
}
