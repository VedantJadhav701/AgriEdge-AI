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
 * from Google DeepMind's google/embeddinggemma-2 across:
 *   1. All 103 crop-disease agronomic profiles.
 *   2. Maharashtra APMC Mandi price records for natural language market queries.
 */
class EmbeddingGemmaEngine private constructor(
    private val classEmbeddings: List<ClassVector>,
    private val knowledgeMap: Map<String, AgronomicProfile>,
    private val mandiRecords: List<MandiRecord>
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

    data class MandiRecord(
        val id: String,
        val crop: String,
        val cropTitle: String,
        val district: String,
        val market: String,
        val minRs: Int,
        val maxRs: Int,
        val modalRs: Int,
        val date: String,
        val vector: FloatArray
    )

    data class RetrievalMatch(
        val label: String,
        val crop: String,
        val condition: String,
        val similarity: Float,
        val profile: AgronomicProfile?
    )

    val totalClasses: Int get() = classEmbeddings.size
    val totalMandiRecords: Int get() = mandiRecords.size

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

    fun getProfile(label: String): AgronomicProfile? = knowledgeMap[label]

    /**
     * Finds matching mandi market records based on text query keywords (crop and/or district).
     */
    fun findMandiContext(query: String): String? {
        val q = query.lowercase()
        val matches = mandiRecords.filter { r ->
            val cropMatch = q.contains(r.crop.lowercase()) ||
                    (r.crop == "banana" && (q.contains("केळी") || q.contains("केला"))) ||
                    (r.crop == "papaya" && (q.contains("पपई") || q.contains("पपीता"))) ||
                    (r.crop == "tomato" && (q.contains("टोमॅटो") || q.contains("टमाटर"))) ||
                    (r.crop == "onion" && (q.contains("कांदा") || q.contains("प्याज"))) ||
                    (r.crop == "orange" && (q.contains("संत्री") || q.contains("संतरा"))) ||
                    (r.crop == "chilli" && (q.contains("मिरची") || q.contains("मिर्च"))) ||
                    (r.crop == "rice" && (q.contains("भात") || q.contains("तांदूळ") || q.contains("चावल"))) ||
                    (r.crop == "maize" && (q.contains("मका") || q.contains("मक्का"))) ||
                    (r.crop == "black_gram" && (q.contains("उडीद") || q.contains("उड़द")))
            
            val distMatch = q.contains(r.district.lowercase()) ||
                    (r.district == "Pune" && q.contains("पुणे")) ||
                    (r.district == "Nashik" && (q.contains("नाशिक") || q.contains("नासिक"))) ||
                    (r.district == "Jalgaon" && (q.contains("जळगाव") || q.contains("जलगांव"))) ||
                    (r.district == "Nagpur" && (q.contains("नागपूर") || q.contains("नागपुर"))) ||
                    (r.district == "Kolhapur" && (q.contains("कोल्हापूर") || q.contains("कोल्हापुर"))) ||
                    (r.district == "Solapur" && (q.contains("सोलापूर") || q.contains("सोलापुर"))) ||
                    (r.district == "Ahmednagar" && q.contains("अहमदनगर")) ||
                    (r.district == "Dhule" && (q.contains("धुळे") || q.contains("धुलिया")))

            cropMatch && (distMatch || !q.contains("pune") && !q.contains("nashik") && !q.contains("jalgaon") && !q.contains("nagpur"))
        }

        val topRecords = if (matches.isNotEmpty()) matches.take(3) else emptyList()
        if (topRecords.isEmpty()) return null

        return topRecords.joinToString("\n") { r ->
            "- ${r.cropTitle} in ${r.district} (${r.market}): Modal Price Rs ${r.modalRs}/quintal (Range: Rs ${r.minRs} - ${r.maxRs}/quintal, Date: ${r.date})"
        }
    }

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

    /**
     * Builds grounded prompt for market / mandi rate questions.
     */
    fun buildMarketGroundedPrompt(
        userQuery: String,
        mandiContext: String,
        langPromptName: String
    ): String {
        val system = "You are AgriEdge AI, an offline farm market & APMC mandi rate expert for Maharashtra. " +
                "Reply only in $langPromptName. Be brief, practical and accurate. " +
                "State the modal price per quintal and the relevant market name from the verified data."

        val user = "VERIFIED MAHARASHTRA APMC MANDI RATES (EmbeddingGemma-2 Knowledge):\n" +
                "$mandiContext\n\n" +
                "Farmer Question: $userQuery\n" +
                "Answer the farmer directly quoting the exact prices from the verified context above."

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
        private const val MANDI_KNOWLEDGE_ASSET = "data/mandi_knowledge_records.json"
        private const val MANDI_EMBEDDINGS_ASSET = "data/embeddinggemma2_mandi_256d.json"

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
            val mandiRecords = mutableListOf<MandiRecord>()

            // 1. Load Agronomic Knowledge Base
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

            // 2. Load 256d Agronomic Embedding Vectors
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

            // 3. Load Mandi Knowledge & 256d Embeddings
            try {
                val embMap = mutableMapOf<String, FloatArray>()
                context.assets.open(MANDI_EMBEDDINGS_ASSET).use { isStream ->
                    val text = InputStreamReader(isStream).readText()
                    val root = JSONObject(text)
                    val recsArr = root.getJSONArray("records")
                    for (i in 0 until recsArr.length()) {
                        val o = recsArr.getJSONObject(i)
                        val id = o.getString("id")
                        val arr = o.getJSONArray("embedding")
                        val vec = FloatArray(arr.length()) { idx -> arr.getDouble(idx).toFloat() }
                        embMap[id] = vec
                    }
                }

                context.assets.open(MANDI_KNOWLEDGE_ASSET).use { isStream ->
                    val text = InputStreamReader(isStream).readText()
                    val array = JSONArray(text)
                    for (i in 0 until array.length()) {
                        val o = array.getJSONObject(i)
                        val id = o.getString("id")
                        val vec = embMap[id] ?: FloatArray(256)
                        mandiRecords.add(
                            MandiRecord(
                                id = id,
                                crop = o.getString("crop"),
                                cropTitle = o.getString("crop_title"),
                                district = o.getString("district"),
                                market = o.getString("market"),
                                minRs = o.optInt("min_rs"),
                                maxRs = o.optInt("max_rs"),
                                modalRs = o.optInt("modal_rs"),
                                date = o.optString("date"),
                                vector = vec
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return EmbeddingGemmaEngine(classVectors, knowledgeMap, mandiRecords)
        }
    }
}
