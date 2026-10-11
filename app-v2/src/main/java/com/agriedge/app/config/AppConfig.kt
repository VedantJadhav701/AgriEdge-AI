package com.agriedge.app.config

object AppConfig {

    enum class Lang(val code: String, val nativeName: String, val promptName: String) {
        ENGLISH("en", "English", "English"),
        MARATHI("mr", "मराठी", "Marathi"),
        HINDI("hi", "हिंदी", "Hindi")
    }

    /**
     * Crops supported by AgriEdge v2 across all 103 Indian agricultural classes.
     */
    val FOCUS_CROPS: List<String> = listOf(
        "banana", "papaya", "rice", "tomato", "chilli", "coffee", "coconut",
        "cucumber", "guava", "jamun", "maize", "onion", "orange", "sunflower",
        "tea", "betel_leaf", "black_gram"
    )
    const val DEFAULT_CROP = "banana"

    /** Below this softmax probability the result is shown as "low confidence". */
    const val MIN_CONFIDENCE_TO_TRUST = 0.20f

    const val SLM_PRIMARY = "qwen2.5-0.5b-instruct-q4_k_m.gguf"
    const val SLM_MAX_TOKENS_EXPLAIN = 250
    const val SLM_MAX_TOKENS_CHAT = 350
}
