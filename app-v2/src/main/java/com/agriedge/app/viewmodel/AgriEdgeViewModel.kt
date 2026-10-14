package com.agriedge.app.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agriedge.app.classifier.ClassifierResult
import com.agriedge.app.classifier.ClassifierState
import com.agriedge.app.classifier.VisionClassifier
import com.agriedge.app.config.AppConfig
import com.agriedge.app.config.AppConfig.Lang
import com.agriedge.app.data.FarmData
import com.agriedge.app.embedding.EmbeddingGemmaEngine
import com.agriedge.app.i18n.Strings
import com.agriedge.app.llm.LLMState
import com.agriedge.app.llm.LocalLLM
import com.agriedge.app.model.ModelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppTab { Home, Assistant, Market }

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val isStreaming: Boolean = false
)

sealed class ScreenState {
    data object Home : ScreenState()
    data object Camera : ScreenState()
    data class Analyzing(val message: String) : ScreenState()
    data class Result(
        val classifierResult: ClassifierResult,
        val slmText: String,
        val isSlmGenerating: Boolean,
        val totalPipelineTimeMs: Long
    ) : ScreenState()
    data class Error(val message: String) : ScreenState()
}

class AgriEdgeViewModel(application: Application) : AndroidViewModel(application) {

    private val modelManager = ModelManager(application)
    private val embeddingEngine = EmbeddingGemmaEngine.getInstance(application)
    private var visionClassifier: VisionClassifier? = null
    private var localLLM: LocalLLM? = null

    private val _selectedTab = MutableStateFlow(AppTab.Home)
    val selectedTab: StateFlow<AppTab> = _selectedTab.asStateFlow()

    private val _lang = MutableStateFlow(Lang.ENGLISH)
    val lang: StateFlow<Lang> = _lang.asStateFlow()

    private val _district = MutableStateFlow("")
    val district: StateFlow<String> = _district.asStateFlow()

    private val _farmData = MutableStateFlow<FarmData?>(null)
    val farmData: StateFlow<FarmData?> = _farmData.asStateFlow()

    private val _screenState = MutableStateFlow<ScreenState>(ScreenState.Home)
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    private val _modelStatus = MutableStateFlow<ModelManager.ModelStatus?>(null)
    val modelStatus: StateFlow<ModelManager.ModelStatus?> = _modelStatus.asStateFlow()

    private val _visionState = MutableStateFlow<ClassifierState>(ClassifierState.NotLoaded)
    val visionState: StateFlow<ClassifierState> = _visionState.asStateFlow()

    private val _llmState = MutableStateFlow<LLMState>(LLMState.NotLoaded)
    val llmState: StateFlow<LLMState> = _llmState.asStateFlow()

    private val _diagnosisChatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val diagnosisChatMessages: StateFlow<List<ChatMessage>> = _diagnosisChatMessages.asStateFlow()

    private val _agriSlmChatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val agriSlmChatMessages: StateFlow<List<ChatMessage>> = _agriSlmChatMessages.asStateFlow()

    private val _isDiagnosisChatGenerating = MutableStateFlow(false)
    val isDiagnosisChatGenerating: StateFlow<Boolean> = _isDiagnosisChatGenerating.asStateFlow()

    private val _isAgriSlmGenerating = MutableStateFlow(false)
    val isAgriSlmGenerating: StateFlow<Boolean> = _isAgriSlmGenerating.asStateFlow()

    init {
        initializeModels()
        viewModelScope.launch(Dispatchers.IO) {
            val data = FarmData.load(getApplication())
            _farmData.value = data
            if (_district.value.isEmpty()) {
                _district.value = data.districts.firstOrNull()?.name ?: ""
            }
        }
    }

    fun selectTab(tab: AppTab) { _selectedTab.value = tab }
    fun setLanguage(l: Lang) { _lang.value = l }
    fun setDistrict(d: String) { _district.value = d }

    fun initializeModels() {
        viewModelScope.launch(Dispatchers.IO) {
            val status = modelManager.checkAndPrepareModels()
            _modelStatus.value = status

            if (status.classifierPresent && status.labelsPresent) {
                _visionState.value = ClassifierState.Loading
                val classifier = VisionClassifier(modelManager.classifierOnnxFile, modelManager.labelsFile)
                val state = classifier.load()
                _visionState.value = state
                if (state is ClassifierState.Ready) visionClassifier = classifier
            } else {
                _visionState.value = ClassifierState.Error("Vision model files missing.")
            }

            if (status.slmPresent) {
                _llmState.value = LLMState.Loading
                val llm = LocalLLM(modelManager.slmGgufFile)
                val state = llm.load()
                _llmState.value = state
                if (state is LLMState.Ready) localLLM = llm
            } else {
                _llmState.value = LLMState.Error("SLM model file missing.")
            }
        }
    }

    fun openCamera() { _screenState.value = ScreenState.Camera }

    fun navigateHome() {
        _screenState.value = ScreenState.Home
        _diagnosisChatMessages.value = emptyList()
    }

    fun processCapturedImage(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.Default) {
            val classifier = visionClassifier
            if (classifier == null || !classifier.isReady) {
                _screenState.value = ScreenState.Error(Strings.t(_lang.value, "vision_not_ready"))
                return@launch
            }
            _screenState.value = ScreenState.Analyzing(Strings.t(_lang.value, "analyzing"))
            _diagnosisChatMessages.value = emptyList()
            val start = System.currentTimeMillis()
            val result = try {
                classifier.classify(bitmap)
            } catch (e: Exception) {
                _screenState.value = ScreenState.Error(Strings.t(_lang.value, "classify_failed") + ": " + (e.message ?: ""))
                return@launch
            }
            explain(result, start)
        }
    }

    /**
     * Shows the verified result immediately. The SLM explanation streams in only if the model is loaded.
     * The verified explanation and treatment always come from disease_info.json, so the screen is useful
     * even when the language model is not ready.
     */
    private suspend fun explain(result: ClassifierResult, start: Long) {
        val llm = localLLM
        if (llm == null || !llm.isReady) {
            _screenState.value = ScreenState.Result(result, "", false, System.currentTimeMillis() - start)
            return
        }
        val sb = StringBuilder()
        _screenState.value = ScreenState.Result(result, "", true, System.currentTimeMillis() - start)
        try {
            llm.generate(buildExplainPrompt(result), maxTokens = AppConfig.SLM_MAX_TOKENS_EXPLAIN, temperature = 0.4f) { token ->
                sb.append(token)
                _screenState.value = ScreenState.Result(result, sb.toString(), true, System.currentTimeMillis() - start)
            }
        } catch (_: Exception) {
        }
        _screenState.value = ScreenState.Result(result, sb.toString(), false, System.currentTimeMillis() - start)
    }

    private fun buildExplainPrompt(result: ClassifierResult): String {
        val lang = _lang.value
        val profile = embeddingEngine.getProfile(result.rawLabel)
        if (profile != null) {
            return embeddingEngine.buildGroundedPrompt(result.rawLabel, null, lang.promptName)
        }
        val guidance = _farmData.value?.diseaseFor(result.rawLabel)?.treatment?.get(lang.code).orEmpty()
        val system = "You are AgriEdge AI, an offline farm advisor for Maharashtra. Reply only in ${lang.promptName}. " +
                "Be brief (under 120 words), practical and calm. Do not invent chemical doses; refer to the product label or the local KVK."
        val pct = "%.0f".format(result.confidence * 100)
        val user = "Crop: ${result.crop}. Condition: ${result.condition}. Model confidence: $pct%. " +
                "Verified guidance: $guidance. What should the farmer do next?"
        return chatml(system, user)
    }

    fun sendDiagnosisFollowUp(question: String) {
        val cur = _screenState.value as? ScreenState.Result ?: return
        val r = cur.classifierResult
        val groundedPrompt = embeddingEngine.buildGroundedPrompt(r.rawLabel, question, _lang.value.promptName)
        askDirectPrompt(_diagnosisChatMessages, _isDiagnosisChatGenerating, question, groundedPrompt, AppConfig.SLM_MAX_TOKENS_CHAT)
    }

    fun sendAgriSlmQuestion(question: String) {
        val system = "You are AgriSLM, an offline agriculture expert for Maharashtra farmers. " +
                "Reply only in ${_lang.value.promptName}. Be brief and practical. Do not invent chemical doses; refer to the label or KVK."
        ask(_agriSlmChatMessages, _isAgriSlmGenerating, question, system, AppConfig.SLM_MAX_TOKENS_CHAT)
    }

    private fun askDirectPrompt(
        flow: MutableStateFlow<List<ChatMessage>>,
        generating: MutableStateFlow<Boolean>,
        question: String,
        fullPrompt: String,
        maxTokens: Int
    ) {
        if (question.isBlank() || generating.value) return
        viewModelScope.launch(Dispatchers.Default) {
            generating.value = true
            flow.value = flow.value + ChatMessage(isUser = true, text = question)
            val bot = ChatMessage(isUser = false, text = "", isStreaming = true)
            flow.value = flow.value + bot

            val sb = StringBuilder()
            val llm = localLLM
            if (llm != null && llm.isReady) {
                try {
                    llm.generate(fullPrompt, maxTokens = maxTokens, temperature = 0.5f) { t ->
                        sb.append(t)
                        flow.patch(bot.id, sb.toString(), true)
                    }
                } catch (_: Exception) {
                }
            } else {
                sb.append(Strings.t(_lang.value, "llm_not_ready"))
            }
            if (sb.isEmpty()) sb.append(Strings.t(_lang.value, "llm_empty"))
            flow.patch(bot.id, sb.toString(), false)
            generating.value = false
        }
    }

    private fun ask(
        flow: MutableStateFlow<List<ChatMessage>>,
        generating: MutableStateFlow<Boolean>,
        question: String,
        system: String,
        maxTokens: Int
    ) {
        askDirectPrompt(flow, generating, question, chatml(system, question), maxTokens)
    }

    private fun MutableStateFlow<List<ChatMessage>>.patch(id: String, text: String, streaming: Boolean) {
        value = value.map { if (it.id == id) it.copy(text = text, isStreaming = streaming) else it }
    }

    fun clearAgriSlmChat() { _agriSlmChatMessages.value = emptyList() }

    fun stopSLMGeneration() {
        localLLM?.stopGeneration()
        _isDiagnosisChatGenerating.value = false
        _isAgriSlmGenerating.value = false
    }

    private fun chatml(system: String, user: String): String =
        "<|im_start|>system\n$system<|im_end|>\n<|im_start|>user\n$user<|im_end|>\n<|im_start|>assistant\n"

    override fun onCleared() {
        super.onCleared()
        visionClassifier?.release()
        localLLM?.release()
    }
}
