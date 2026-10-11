package com.agriedge.app.classifier

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.nio.FloatBuffer
import kotlin.math.exp

/**
 * Wraps agri_classifier_india_v1_int8.onnx.
 *
 * Preprocessing & ONNX specs:
 *   - Input tensor: "pixel_values", shape [1, 3, 224, 224], float32
 *   - Output tensor: "logits", shape [1, 103], float32
 *   - Normalization: mean=[0.5, 0.5, 0.5], std=[0.5, 0.5, 0.5]
 *   - Labels: india_v1_labels.json (flat 103-entry list of crop__condition strings)
 */
class VisionClassifier(
    private val modelFile: File,
    private val labelsFile: File,
    private val imagePreprocessor: ImagePreprocessor = CenterCropPreprocessor()
) {

    companion object {
        private const val TAG = "VisionClassifier"
        private const val INPUT_NAME = "pixel_values"
        private const val OUTPUT_NAME = "logits"
        private const val IMAGE_SIZE = 224
        private val MEAN = floatArrayOf(0.5f, 0.5f, 0.5f)
        private val STD = floatArrayOf(0.5f, 0.5f, 0.5f)
    }

    interface ImagePreprocessor {
        fun preprocess(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap
    }

    private fun toSoftwareBitmap(bitmap: Bitmap): Bitmap {
        return if (bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }
    }

    class CenterCropPreprocessor : ImagePreprocessor {
        override fun preprocess(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
            val safeBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                bitmap
            }
            val w = safeBitmap.width
            val h = safeBitmap.height
            val minDim = minOf(w, h)
            val startX = (w - minDim) / 2
            val startY = (h - minDim) / 2

            val cropped = Bitmap.createBitmap(safeBitmap, startX, startY, minDim, minDim)
            val scaled = Bitmap.createScaledBitmap(cropped, targetWidth, targetHeight, true)

            if (cropped !== safeBitmap && cropped !== scaled) {
                cropped.recycle()
            }
            if (safeBitmap !== bitmap && safeBitmap !== scaled) {
                safeBitmap.recycle()
            }
            return scaled
        }
    }

    class BilinearPreprocessor : ImagePreprocessor {
        override fun preprocess(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
            val safeBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                bitmap
            }
            val scaled = Bitmap.createScaledBitmap(safeBitmap, targetWidth, targetHeight, true)
            if (scaled !== safeBitmap && safeBitmap !== bitmap) {
                safeBitmap.recycle()
            }
            return scaled
        }
    }

    private var env: OrtEnvironment? = null
    private var session: OrtSession? = null
    private var labels: List<String> = emptyList()

    val isReady: Boolean get() = session != null && labels.isNotEmpty()

    suspend fun load(): ClassifierState = withContext(Dispatchers.IO) {
        try {
            require(modelFile.exists()) { "Model file not found at ${modelFile.absolutePath}" }
            require(labelsFile.exists()) { "Labels file not found at ${labelsFile.absolutePath}" }

            labels = JSONArray(labelsFile.readText()).let { arr ->
                List(arr.length()) { i -> arr.getString(i) }
            }
            // Label count is model-defined; do not hard-code it.
            require(labels.isNotEmpty()) { "Labels file is empty." }

            env = OrtEnvironment.getEnvironment()

            val fileToLoad = prepareFixedModelFile(modelFile)

            val optionStrategies = listOf<Pair<String, () -> OrtSession.SessionOptions>>(
                "XNNPACK (BASIC_OPT)" to {
                    OrtSession.SessionOptions().apply {
                        setIntraOpNumThreads(4)
                        setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT)
                        addXnnpack(mapOf("intra_op_num_threads" to "4"))
                    }
                },
                "XNNPACK (ALL_OPT)" to {
                    OrtSession.SessionOptions().apply {
                        setIntraOpNumThreads(4)
                        setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                        addXnnpack(mapOf("intra_op_num_threads" to "4"))
                    }
                },
                "CPU (BASIC_OPT)" to {
                    OrtSession.SessionOptions().apply {
                        setIntraOpNumThreads(4)
                        setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT)
                    }
                },
                "CPU (ALL_OPT)" to {
                    OrtSession.SessionOptions().apply {
                        setIntraOpNumThreads(4)
                        setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                    }
                },
                "CPU (NO_OPT)" to {
                    OrtSession.SessionOptions().apply {
                        setIntraOpNumThreads(4)
                        setOptimizationLevel(OrtSession.SessionOptions.OptLevel.NO_OPT)
                    }
                },
                "NNAPI" to {
                    OrtSession.SessionOptions().apply {
                        setIntraOpNumThreads(4)
                        addNnapi()
                    }
                }
            )

            val filesToTry = listOf(fileToLoad, modelFile).distinct()
            var createdSession: OrtSession? = null
            var lastException: Exception? = null

            for (targetFile in filesToTry) {
                for ((name, optionsFactory) in optionStrategies) {
                    try {
                        val options = optionsFactory()
                        createdSession = env!!.createSession(targetFile.absolutePath, options)
                        Log.i(TAG, "ONNX session successfully created for ${targetFile.name} using $name.")
                        break
                    } catch (e: Exception) {
                        Log.w(TAG, "Session creation failed for ${targetFile.name} with strategy '$name': ${e.message}")
                        lastException = e
                    }
                }
                if (createdSession != null) break
            }

            if (createdSession == null) {
                throw lastException ?: IllegalStateException("Failed to create ONNX session with any strategy.")
            }

            session = createdSession

            val outputNames = session!!.outputInfo.keys
            if (!outputNames.contains(OUTPUT_NAME)) {
                return@withContext ClassifierState.Error(
                    "Expected ONNX output named '$OUTPUT_NAME' — actual outputs: $outputNames"
                )
            }

            ClassifierState.Ready(labels.size)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load ONNX classifier: ${e.message}", e)
            ClassifierState.Error(e.message ?: "Unknown error loading classifier")
        }
    }

    suspend fun classify(bitmap: Bitmap, topK: Int = 3): ClassifierResult = withContext(Dispatchers.Default) {
        checkNotNull(session) { "classify() called before load() succeeded" }
        val startTime = System.currentTimeMillis()

        val softwareBitmap = toSoftwareBitmap(bitmap)
        val resized = imagePreprocessor.preprocess(softwareBitmap, IMAGE_SIZE, IMAGE_SIZE)
        val inputBuffer = bitmapToNchwFloatBuffer(resized)

        if (resized !== softwareBitmap && resized !== bitmap) resized.recycle()
        if (softwareBitmap !== bitmap) softwareBitmap.recycle()

        val inputTensor = OnnxTensor.createTensor(
            env, inputBuffer, longArrayOf(1, 3, IMAGE_SIZE.toLong(), IMAGE_SIZE.toLong())
        )

        val logits: FloatArray = inputTensor.use { tensor ->
            session!!.run(mapOf(INPUT_NAME to tensor)).use { output ->
                @Suppress("UNCHECKED_CAST")
                val raw = output.get(OUTPUT_NAME).get().value as Array<FloatArray>
                raw[0]
            }
        }

        val probs = softmax(logits)
        val ranked = probs.indices.sortedByDescending { probs[it] }
        val topIdx = ranked.first()
        val rawLabel = labels[topIdx]
        val (crop, condition) = splitLabel(rawLabel)

        ClassifierResult(
            crop = crop,
            condition = condition,
            confidence = probs[topIdx],
            rawLabel = rawLabel,
            topK = ranked.take(topK).map { labels[it] to probs[it] },
            inferenceTimeMs = System.currentTimeMillis() - startTime,
        )
    }

    private fun splitLabel(label: String): Pair<String, String> {
        val parts = label.split("__", limit = 2)
        return if (parts.size == 2) parts[0] to parts[1] else label to "unknown"
    }

    private fun softmax(logits: FloatArray): FloatArray {
        val max = logits.max()
        val exps = FloatArray(logits.size) { exp((logits[it] - max).toDouble()).toFloat() }
        val sum = exps.sum()
        return FloatArray(logits.size) { exps[it] / sum }
    }

    private fun bitmapToNchwFloatBuffer(bmp: Bitmap): FloatBuffer {
        val safeBmp = toSoftwareBitmap(bmp)
        val w = safeBmp.width
        val h = safeBmp.height
        val pixels = IntArray(w * h)
        safeBmp.getPixels(pixels, 0, w, 0, 0, w, h)
        if (safeBmp !== bmp) safeBmp.recycle()

        val buffer = FloatBuffer.allocate(3 * w * h)
        for (c in 0 until 3) {
            for (i in pixels.indices) {
                val pixel = pixels[i]
                val channelVal = when (c) {
                    0 -> (pixel shr 16) and 0xFF   // R
                    1 -> (pixel shr 8) and 0xFF    // G
                    else -> pixel and 0xFF          // B
                }
                buffer.put((channelVal / 255f - MEAN[c]) / STD[c])
            }
        }
        buffer.rewind()
        return buffer
    }

    private fun prepareFixedModelFile(originalFile: File): File {
        return originalFile
    }

    fun release() {
        session?.close()
        session = null
    }
}
