package com.agriedge.app.classifier

/**
 * Single-head result from agri_classifier_india_v1_int8.onnx.
 * Confirmed via direct ONNX/label-file inspection:
 *   - output tensor "logits", shape [batch, 103]
 *   - india_v1_labels.json is a flat List<String> of "crop__condition" strings,
 *     index-aligned with the logits — argmax(logits) -> labels[index] -> split("__")
 */
data class ClassifierResult(
    val crop: String,
    val condition: String,
    val confidence: Float,          // softmax probability of top class
    val rawLabel: String,           // untouched "crop__condition" string
    val topK: List<Pair<String, Float>> = emptyList(), // (label, probability) pairs, highest first
    val inferenceTimeMs: Long = 0,
)

sealed class ClassifierState {
    data object NotLoaded : ClassifierState()
    data object Loading : ClassifierState()
    data class Ready(val labelCount: Int) : ClassifierState()
    data class Error(val message: String) : ClassifierState()
}
