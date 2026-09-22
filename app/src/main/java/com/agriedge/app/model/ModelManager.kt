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
 * Manages model file resolution across:
 *  1. Primary external app folder: /storage/emulated/0/Android/data/com.agriedge.app/files/models/
 *  2. Internal private app storage: context.filesDir/models/
 *  3. External Download folder: /storage/emulated/0/Download/
 */
class ModelManager(private val context: Context) {

    companion object {
        private const val TAG = "AgriEdgeModelManager"
    }

    private val internalModelsDir = File(context.filesDir, "models").apply { mkdirs() }
    private val primaryExtModelsDir = File("/storage/emulated/0/Android/data/com.agriedge.app/files/models")
    private val downloadDir = File(Environment.getExternalStorageDirectory(), "Download")
    private val altDownloadDir = File("/storage/emulated/0/Download")

    val classifierOnnxFile: File get() {
        val optFile = resolveModelFile("agri_classifier_india_v1_int8_opt.onnx")
        if (optFile.exists() && optFile.length() > 0) return optFile
        return resolveModelFile("agri_classifier_india_v1_int8.onnx")
    }
    val labelsFile: File get() = resolveModelFile("india_v1_labels.json")
    val slmGgufFile: File get() {
        val instructFile = resolveModelFile("SmolLM2-360M-Instruct-Q4_K_M.gguf")
        if (instructFile.exists() && instructFile.length() > 0) return instructFile
        return resolveModelFile("agriedge_slm_q4_k_m.gguf")
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
        prepareModel("SmolLM2-360M-Instruct-Q4_K_M.gguf")
        prepareModel("agriedge_slm_q4_k_m.gguf")

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

        // Check primary external app folder
        val primaryExtFile = File(primaryExtModelsDir, fileName)
        if (primaryExtFile.exists() && primaryExtFile.length() > 0) {
            if (!targetFile.exists() || targetFile.length() != primaryExtFile.length()) {
                copyFile(primaryExtFile, targetFile)
            }
            return
        }

        // Check context.getExternalFilesDir("models")
        try {
            val extDir = context.getExternalFilesDir("models")
            if (extDir != null) {
                val extFile = File(extDir, fileName)
                if (extFile.exists() && extFile.length() > 0) {
                    if (!targetFile.exists() || targetFile.length() != extFile.length()) {
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
            if (!targetFile.exists() || targetFile.length() != dlFile.length()) {
                copyFile(dlFile, targetFile)
            }
            return
        }

        val altDlFile = File(altDownloadDir, fileName)
        if (altDlFile.exists() && altDlFile.length() > 0) {
            if (!targetFile.exists() || targetFile.length() != altDlFile.length()) {
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

        val primaryExtFile = File(primaryExtModelsDir, fileName)
        if (primaryExtFile.exists() && primaryExtFile.length() > 0) {
            return primaryExtFile
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
