package com.agriedge.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agriedge.app.classifier.ClassifierState
import com.agriedge.app.llm.LLMState
import com.agriedge.app.model.ModelManager
import com.agriedge.app.ui.theme.DarkGreen
import com.agriedge.app.viewmodel.AppTab
import com.agriedge.app.viewmodel.ChatMessage
import com.agriedge.app.viewmodel.ScreenState

@Composable
fun AgriEdgeAppScreen(
    selectedTab: AppTab,
    screenState: ScreenState,
    modelStatus: ModelManager.ModelStatus?,
    visionState: ClassifierState,
    llmState: LLMState,
    diagnosisChatMessages: List<ChatMessage>,
    isDiagnosisChatGenerating: Boolean,
    agriSlmChatMessages: List<ChatMessage>,
    isAgriSlmGenerating: Boolean,
    onTabSelected: (AppTab) -> Unit,
    onOpenCamera: () -> Unit,
    onNavigateHome: () -> Unit,
    onImageCaptured: (Bitmap) -> Unit,
    onSendDiagnosisFollowUp: (String) -> Unit,
    onUpdateCropDiagnosis: (String, String) -> Unit,
    onSendAgriSlmMessage: (String) -> Unit,
    onClearAgriSlmChat: () -> Unit,
    onStopSLM: () -> Unit
) {
    Scaffold(
        bottomBar = {
            if (screenState !is ScreenState.Camera) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == AppTab.Diagnosis,
                        onClick = { onTabSelected(AppTab.Diagnosis) },
                        icon = { Icon(Icons.Default.Search, contentDescription = "Diagnosis") },
                        label = { Text("Scan & Diagnose", fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DarkGreen,
                            selectedTextColor = DarkGreen,
                            indicatorColor = Color(0xFFD8F3DC)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == AppTab.AgriSLM,
                        onClick = { onTabSelected(AppTab.AgriSLM) },
                        icon = { Icon(Icons.Default.Chat, contentDescription = "AgriSLM Chat") },
                        label = { Text("AgriSLM Chat", fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DarkGreen,
                            selectedTextColor = DarkGreen,
                            indicatorColor = Color(0xFFD8F3DC)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                AppTab.Diagnosis -> {
                    when (screenState) {
                        is ScreenState.Home -> {
                            HomeScreen(
                                modelStatus = modelStatus,
                                visionState = visionState,
                                llmState = llmState,
                                onOpenCamera = onOpenCamera,
                                onImageSelected = onImageCaptured
                            )
                        }

                        is ScreenState.Camera -> {
                            CameraScreen(
                                onImageCaptured = onImageCaptured,
                                onBack = onNavigateHome
                            )
                        }

                        is ScreenState.Analyzing -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = DarkGreen)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = screenState.message,
                                        fontSize = 16.sp,
                                        color = Color(0xFF374151)
                                    )
                                }
                            }
                        }

                        is ScreenState.Result -> {
                            ResultScreen(
                                result = screenState.classifierResult,
                                slmText = screenState.slmText,
                                isSlmGenerating = screenState.isSlmGenerating,
                                totalPipelineTimeMs = screenState.totalPipelineTimeMs,
                                chatMessages = diagnosisChatMessages,
                                isChatGenerating = isDiagnosisChatGenerating,
                                onSendFollowUp = onSendDiagnosisFollowUp,
                                onUpdateCropDiagnosis = onUpdateCropDiagnosis,
                                onScanAnother = onNavigateHome,
                                onStopSLM = onStopSLM
                            )
                        }

                        is ScreenState.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Error Occurred",
                                        fontSize = 20.sp,
                                        color = Color(0xFFC62828)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = screenState.message,
                                        fontSize = 14.sp,
                                        color = Color(0xFF374151)
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Button(onClick = onNavigateHome) {
                                        Text("Return Home")
                                    }
                                }
                            }
                        }
                    }
                }

                AppTab.AgriSLM -> {
                    AgriSlmChatScreen(
                        messages = agriSlmChatMessages,
                        isGenerating = isAgriSlmGenerating,
                        onSendMessage = onSendAgriSlmMessage,
                        onClearChat = onClearAgriSlmChat
                    )
                }
            }
        }
    }
}
