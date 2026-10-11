package com.agriedge.app.llm

sealed class LLMState {
    data object NotLoaded : LLMState()
    data object Loading : LLMState()
    data object Ready : LLMState()
    data class Generating(val partialText: String) : LLMState()
    data class Error(val message: String) : LLMState()
}
