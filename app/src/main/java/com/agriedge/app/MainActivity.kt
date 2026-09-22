package com.agriedge.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.agriedge.app.ui.AgriEdgeAppScreen
import com.agriedge.app.ui.theme.AgriEdgeTheme
import com.agriedge.app.viewmodel.AgriEdgeViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AgriEdgeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgriEdgeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val selectedTab by viewModel.selectedTab.collectAsState()
                    val screenState by viewModel.screenState.collectAsState()
                    val modelStatus by viewModel.modelStatus.collectAsState()
                    val visionState by viewModel.visionState.collectAsState()
                    val llmState by viewModel.llmState.collectAsState()
                    val diagnosisChatMessages by viewModel.diagnosisChatMessages.collectAsState()
                    val isDiagnosisChatGenerating by viewModel.isDiagnosisChatGenerating.collectAsState()
                    val agriSlmChatMessages by viewModel.agriSlmChatMessages.collectAsState()
                    val isAgriSlmGenerating by viewModel.isAgriSlmGenerating.collectAsState()

                    AgriEdgeAppScreen(
                        selectedTab = selectedTab,
                        screenState = screenState,
                        modelStatus = modelStatus,
                        visionState = visionState,
                        llmState = llmState,
                        diagnosisChatMessages = diagnosisChatMessages,
                        isDiagnosisChatGenerating = isDiagnosisChatGenerating,
                        agriSlmChatMessages = agriSlmChatMessages,
                        isAgriSlmGenerating = isAgriSlmGenerating,
                        onTabSelected = { tab -> viewModel.selectTab(tab) },
                        onOpenCamera = { viewModel.openCamera() },
                        onNavigateHome = { viewModel.navigateHome() },
                        onImageCaptured = { bitmap -> viewModel.processCapturedImage(bitmap) },
                        onSendDiagnosisFollowUp = { question -> viewModel.sendDiagnosisFollowUp(question) },
                        onUpdateCropDiagnosis = { crop, cond -> viewModel.updateCropDiagnosis(crop, cond) },
                        onSendAgriSlmMessage = { question -> viewModel.sendAgriSlmQuestion(question) },
                        onClearAgriSlmChat = { viewModel.clearAgriSlmChat() },
                        onStopSLM = { viewModel.stopSLMGeneration() }
                    )
                }
            }
        }
    }
}
