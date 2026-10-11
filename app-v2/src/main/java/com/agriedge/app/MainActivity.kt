package com.agriedge.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.agriedge.app.ui.AgriEdgeAppScreen
import com.agriedge.app.ui.theme.AgriEdgeTheme
import com.agriedge.app.ui.theme.Cream
import com.agriedge.app.viewmodel.AgriEdgeViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AgriEdgeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgriEdgeTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Cream) {
                    val lang by viewModel.lang.collectAsState()
                    val tab by viewModel.selectedTab.collectAsState()
                    val district by viewModel.district.collectAsState()
                    val farmData by viewModel.farmData.collectAsState()
                    val screenState by viewModel.screenState.collectAsState()
                    val modelStatus by viewModel.modelStatus.collectAsState()
                    val visionState by viewModel.visionState.collectAsState()
                    val diagChat by viewModel.diagnosisChatMessages.collectAsState()
                    val diagBusy by viewModel.isDiagnosisChatGenerating.collectAsState()
                    val asstChat by viewModel.agriSlmChatMessages.collectAsState()
                    val asstBusy by viewModel.isAgriSlmGenerating.collectAsState()

                    AgriEdgeAppScreen(
                        lang = lang,
                        onLangChange = { viewModel.setLanguage(it) },
                        tab = tab,
                        onTabSelected = { viewModel.selectTab(it) },
                        screenState = screenState,
                        visionState = visionState,
                        modelStatus = modelStatus,
                        farmData = farmData,
                        district = district,
                        onDistrictChange = { viewModel.setDistrict(it) },
                        diagnosisChatMessages = diagChat,
                        isDiagnosisChatGenerating = diagBusy,
                        agriSlmChatMessages = asstChat,
                        isAgriSlmGenerating = asstBusy,
                        onOpenCamera = { viewModel.openCamera() },
                        onNavigateHome = { viewModel.navigateHome() },
                        onImageCaptured = { viewModel.processCapturedImage(it) },
                        onSendDiagnosisFollowUp = { viewModel.sendDiagnosisFollowUp(it) },
                        onSendAgriSlmMessage = { viewModel.sendAgriSlmQuestion(it) },
                        onClearAgriSlmChat = { viewModel.clearAgriSlmChat() },
                        onStopSLM = { viewModel.stopSLMGeneration() }
                    )
                }
            }
        }
    }
}
