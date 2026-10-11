package com.agriedge.app.model

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Manages model file resolution for AgriEdge v2 across:
 *  1. App v2 external folder: /storage/emulated/0/Android/data/com.agriedge.v2/files/models/
 *  2. Shared v1 external folder: /storage/emulated/0/Android/data/com.agriedge.app/files/models/
 *  3. Internal private app storage: context.filesDir/models/
 *  4. External Download folder: /storage/emulated/0/Download/
 */
class ModelManager(private val context: Context) {

    companion object {
        private const val TAG = "AgriEdgeModelManagerV2"
    }

    private val internalModelsDir = File(context.filesDir, "models").apply { mkdirs() }
    private val v2ExtModelsDir = File("/storage/emulated/0/Android/data/com.agriedge.v2/files/models")
    private val v1ExtModelsDir = File("/storage/emulated/0/Android/data/com.agriedge.app/files/models")
    private val downloadDir = File(Environment.getExternalStorageDirectory(), "Download")
    private val altDownloadDir = File("/storage/emulated/0/Download")

    val classifierOnnxFile: File get() {
        val optFile = resolveModelFile("agri_classifier_india_v1_int8_opt.onnx")
        if (optFile.exists() && optFile.length() > 0) return optFile
        return resolveModelFile("agri_classifier_india_v1_int8.onnx")
    }
    val labelsFile: File get() = resolveModelFile("india_v1_labels.json")
    val slmGgufFile: File get() {
        val qwenGguf = resolveModelFile("qwen2.5-0.5b-instruct-q4_k_m.gguf")
        if (qwenGguf.exists() && qwenGguf.length() > 0) return qwenGguf
        val qwenCap = resolveModelFile("Qwen2.5-0.5B-Instruct-Q4_K_M.gguf")
        if (qwenCap.exists() && qwenCap.length() > 0) return qwenCap
        val agriQwen = resolveModelFile("agriedge_qwen05b_q4_k_m.gguf")
        if (agriQwen.exists() && agriQwen.length() > 0) return agriQwen
        val smol = resolveModelFile("SmolLM2-360M-Instruct-Q4_K_M.gguf")
        if (smol.exists() && smol.length() > 0) return smol
        return resolveModelFile("qwen2.5-0.5b-instruct-q4_k_m.gguf")
    }

    data class ModelStatus(
        val classifierPresent: Boolean,
        val labelsPresent: Boolean,
        val slmPresent: Boolean,
        val classifierSizeMb: Double,
        val slmSizeMb: Double,
    ) {
        val allPresent: Boolean get() = classifierPresent && labelsPresent && slmPresent
    }

    suspend fun checkAndPrepareModels(): ModelStatus = withContext(Dispatchers.IO) {
        prepareModel("agri_classifier_india_v1_int8_opt.onnx")
        prepareModel("agri_classifier_india_v1_int8.onnx")
        prepareModel("india_v1_labels.json")
        prepareModel("qwen2.5-0.5b-instruct-q4_k_m.gguf")
        prepareModel("Qwen2.5-0.5B-Instruct-Q4_K_M.gguf")
        prepareModel("agriedge_qwen05b_q4_k_m.gguf")
        prepareModel("SmolLM2-360M-Instruct-Q4_K_M.gguf")

        val cFile = classifierOnnxFile
        val lFile = labelsFile
        val sFile = slmGgufFile

        ModelStatus(
            classifierPresent = cFile.exists() && cFile.length() > 0,
            labelsPresent = lFile.exists() && lFile.length() > 0,
            slmPresent = sFile.exists() && sFile.length() > 0,
            classifierSizeMb = if (cFile.exists()) cFile.length() / 1e6 else 0.0,
            slmSizeMb = if (sFile.exists()) sFile.length() / 1e6 else 0.0,
        )
    }

    private fun prepareModel(fileName: String) {
        val targetFile = File(internalModelsDir, fileName)

        fun shouldUpdate(source: File): Boolean {
            return !targetFile.exists() || 
                   targetFile.length() != source.length() || 
                   source.lastModified() > targetFile.lastModified()
        }

        // Check app v2 external folder
        val v2ExtFile = File(v2ExtModelsDir, fileName)
        if (v2ExtFile.exists() && v2ExtFile.length() > 0) {
            if (shouldUpdate(v2ExtFile)) {
                copyFile(v2ExtFile, targetFile)
            }
            return
        }

        // Check app v1 external folder
        val v1ExtFile = File(v1ExtModelsDir, fileName)
        if (v1ExtFile.exists() && v1ExtFile.length() > 0) {
            if (shouldUpdate(v1ExtFile)) {
                copyFile(v1ExtFile, targetFile)
            }
            return
        }

        // Check context.getExternalFilesDir("models")
        try {
            val extDir = context.getExternalFilesDir("models")
            if (extDir != null) {
                val extFile = File(extDir, fileName)
                if (extFile.exists() && extFile.length() > 0) {
                    if (shouldUpdate(extFile)) {
                        copyFile(extFile, targetFile)
                    }
                    return
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "External files dir check skipped: ${e.message}")
        }

        // Check Download folder
        val dlFile = File(downloadDir, fileName)
        if (dlFile.exists() && dlFile.length() > 0) {
            if (shouldUpdate(dlFile)) {
                copyFile(dlFile, targetFile)
            }
            return
        }

        val altDlFile = File(altDownloadDir, fileName)
        if (altDlFile.exists() && altDlFile.length() > 0) {
            if (shouldUpdate(altDlFile)) {
                copyFile(altDlFile, targetFile)
            }
            return
        }
    }

    private fun resolveModelFile(fileName: String): File {
        val internalFile = File(internalModelsDir, fileName)
        if (internalFile.exists() && internalFile.length() > 0) {
            return internalFile
        }

        val v2ExtFile = File(v2ExtModelsDir, fileName)
        if (v2ExtFile.exists() && v2ExtFile.length() > 0) {
            return v2ExtFile
        }

        val v1ExtFile = File(v1ExtModelsDir, fileName)
        if (v1ExtFile.exists() && v1ExtFile.length() > 0) {
            return v1ExtFile
        }

        val dlFile = File(downloadDir, fileName)
        if (dlFile.exists() && dlFile.length() > 0) return dlFile

        val altDlFile = File(altDownloadDir, fileName)
        if (altDlFile.exists() && altDlFile.length() > 0) return altDlFile

        return internalFile
    }

    private fun copyFile(source: File, destination: File) {
        try {
            Log.i(TAG, "Copying model from ${source.absolutePath} to ${destination.absolutePath}")
            if (destination.exists()) {
                destination.delete()
            }
            FileInputStream(source).use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            }
            Log.i(TAG, "Successfully copied ${source.name} (${destination.length()} bytes)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy ${source.name}: ${e.message}")
        }
    }

    fun modelsDirPath(): String = internalModelsDir.absolutePath
}
