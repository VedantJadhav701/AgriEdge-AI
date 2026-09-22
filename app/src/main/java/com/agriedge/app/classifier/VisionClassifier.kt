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
        private val BASELINE_LOGIT_BIAS = floatArrayOf(
            -0.9102f, -0.5141f, -0.8479f, -1.5017f, -1.1332f, -2.3356f, 1.0781f, -1.0730f, 2.3571f, 0.3021f,
            1.6875f, -1.3213f, -0.5136f, 0.6138f, -3.2442f, -2.1254f, -1.6986f, -0.9233f, -1.3713f, -0.1581f,
            0.0661f, -1.1261f, -0.6757f, 1.6941f, 6.9135f, -0.8863f, -1.0358f, -0.8500f, 0.8209f, -1.4249f,
            -1.6366f, -2.0688f, -0.0162f, -0.9708f, 0.5084f, -1.1248f, 0.5601f, 0.2823f, 1.4567f, -0.2582f,
            -2.9615f, -0.6454f, -0.5090f, -0.8340f, 0.3933f, -0.4576f, -1.7881f, 0.0386f, 1.0655f, 1.4494f,
            -1.8825f, -2.6618f, 0.5306f, -1.4485f, -0.9097f, -0.2562f, -0.5410f, -2.3553f, -0.3772f, -1.2110f,
            -0.6976f, -1.4306f, 0.0756f, -1.8203f, -1.5333f, 1.1031f, -0.5627f, 1.0095f, -0.6199f, -0.9154f,
            1.4496f, 0.6814f, 0.0949f, 1.9717f, 1.0489f, 0.4318f, -0.3624f, 2.0056f, -0.6157f, -0.8836f,
            1.3834f, 0.9030f, 1.5351f, -0.2694f, 1.2244f, -1.4668f, -0.1687f, -0.8581f, -1.6203f, -1.2232f,
            -0.7373f, -2.0729f, -1.1887f, -1.2241f, 0.7830f, -0.6750f, -1.6891f, -1.0580f, -1.8162f, -1.8746f,
            -1.6523f, -0.3056f, -1.0127f
        )
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
            require(labels.size == 103) {
                "Expected 103 labels (india_v1_labels.json), got ${labels.size}."
            }

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
