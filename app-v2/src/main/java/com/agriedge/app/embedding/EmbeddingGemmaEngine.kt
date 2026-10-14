package com.agriedge.app.embedding

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStreamReader
import kotlin.math.sqrt

/**
 * Offline EmbeddingGemma-2 Semantic Retrieval & Knowledge Engine for AgriEdge.
 *
 * Utilizes pre-computed 256-dimensional Matryoshka Representation Learning (MRL) embeddings
 * from Google DeepMind's google/embeddinggemma-2 across all 103 crop-disease classes.
 * Enables zero-latency (<1ms) vector similarity search, multimodal grounding, and offline RAG.
 */
class EmbeddingGemmaEngine private constructor(
    private val classEmbeddings: List<ClassVector>,
    private val knowledgeMap: Map<String, AgronomicProfile>
) {

    data class ClassVector(
        val label: String,
        val crop: String,
        val condition: String,
        val vector: FloatArray // 256-dim L2-normalized vector
    )

    data class AgronomicProfile(
        val label: String,
        val crop: String,
        val condition: String,
        val isHealthy: Boolean,
        val explanation: String,
        val symptoms: String,
        val organicRemedy: String,
        val chemicalTreatment: String,
        val preventativeMeasures: String
    )

    data class RetrievalMatch(
        val label: String,
        val crop: String,
        val condition: String,
        val similarity: Float,
        val profile: AgronomicProfile?
    )

    val totalClasses: Int get() = classEmbeddings.size

    /**
     * Finds the Top-K most semantically similar classes for a given 256-dim query embedding.
     */
    fun search(queryVector: FloatArray, topK: Int = 3): List<RetrievalMatch> {
        val normQuery = normalize(queryVector)
        val scored = classEmbeddings.map { cv ->
            val sim = dotProduct(normQuery, cv.vector)
            RetrievalMatch(
                label = cv.label,
                crop = cv.crop,
                condition = cv.condition,
                similarity = sim,
                profile = knowledgeMap[cv.label]
            )
        }
        return scored.sortedByDescending { it.similarity }.take(topK)
    }

    /**
     * Returns the verified agronomic knowledge profile for a specific class label.
     */
    fun getProfile(label: String): AgronomicProfile? = knowledgeMap[label]

    /**
     * Builds a structured, hallucination-free prompt for the SLM using retrieved verified facts.
     */
    fun buildGroundedPrompt(
        label: String,
        userQuery: String?,
        langPromptName: String
    ): String {
        val profile = knowledgeMap[label]
        val context = if (profile != null) {
            "Crop: ${profile.crop}\n" +
            "Condition: ${profile.condition}\n" +
            "Symptoms: ${profile.symptoms}\n" +
            "Organic Control: ${profile.organicRemedy}\n" +
            "Chemical Treatment: ${profile.chemicalTreatment}\n" +
            "Prevention: ${profile.preventativeMeasures}"
        } else {
            "Class: $label"
        }

        val question = if (!userQuery.isNullOrBlank()) {
            "Farmer Question: $userQuery\nAnswer practically based only on the verified context above."
        } else {
            "Provide concise, step-by-step guidance for the farmer based on the verified context above."
        }

        val system = "You are AgriEdge AI, an offline agriculture advisory expert powered by EmbeddingGemma-2 RAG. " +
                "Reply only in $langPromptName. Be brief (under 120 words), practical and reassuring. " +
                "Do not fabricate doses; always reference verified product labels or the local KVK."

        val user = "VERIFIED AGRONOMIC CONTEXT (EmbeddingGemma-2):\n$context\n\n$question"

        return "<|im_start|>system\n$system<|im_end|>\n<|im_start|>user\n$user<|im_end|>\n<|im_start|>assistant\n"
    }

    private fun dotProduct(a: FloatArray, b: FloatArray): Float {
        var sum = 0f
        val len = minOf(a.size, b.size)
        for (i in 0 until len) {
            sum += a[i] * b[i]
        }
        return sum
    }

    private fun normalize(v: FloatArray): FloatArray {
        var sumSq = 0f
        for (x in v) sumSq += x * x
        val mag = sqrt(sumSq)
        if (mag < 1e-8f) return v
        val out = FloatArray(v.size)
        for (i in v.indices) out[i] = v[i] / mag
        return out
    }

    companion object {
        private const val EMBEDDINGS_ASSET = "data/embeddinggemma2_knowledge_256d.json"
        private const val KNOWLEDGE_ASSET = "data/agriedge_knowledge_103.json"

        @Volatile
        private var instance: EmbeddingGemmaEngine? = null

        fun getInstance(context: Context): EmbeddingGemmaEngine {
            return instance ?: synchronized(this) {
                instance ?: load(context.applicationContext).also { instance = it }
            }
        }

        private fun load(context: Context): EmbeddingGemmaEngine {
            val knowledgeMap = mutableMapOf<String, AgronomicProfile>()
            val classVectors = mutableListOf<ClassVector>()

            // 1. Load Knowledge Base
            try {
                context.assets.open(KNOWLEDGE_ASSET).use { isStream ->
                    val text = InputStreamReader(isStream).readText()
                    val array = JSONArray(text)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val p = AgronomicProfile(
                            label = obj.getString("label"),
                            crop = obj.getString("crop"),
                            condition = obj.getString("condition"),
                            isHealthy = obj.optBoolean("is_healthy", false),
                            explanation = obj.optString("explanation", ""),
                            symptoms = obj.optString("symptoms", ""),
                            organicRemedy = obj.optString("organic_remedy", ""),
                            chemicalTreatment = obj.optString("chemical_treatment", ""),
                            preventativeMeasures = obj.optString("preventative_measures", "")
                        )
                        knowledgeMap[p.label] = p
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. Load 256d Embedding Vectors
            try {
                context.assets.open(EMBEDDINGS_ASSET).use { isStream ->
                    val text = InputStreamReader(isStream).readText()
                    val root = JSONObject(text)
                    val classesArr = root.getJSONArray("classes")
                    for (i in 0 until classesArr.length()) {
                        val cObj = classesArr.getJSONObject(i)
                        val label = cObj.getString("label")
                        val crop = cObj.getString("crop")
                        val condition = cObj.getString("condition")
                        val embArr = cObj.getJSONArray("embedding")
                        val vec = FloatArray(embArr.length()) { idx -> embArr.getDouble(idx).toFloat() }
                        classVectors.add(ClassVector(label, crop, condition, vec))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return EmbeddingGemmaEngine(classVectors, knowledgeMap)
        }
    }
}
