package com.agriedge.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agriedge.app.classifier.ClassifierState
import com.agriedge.app.config.AppConfig
import com.agriedge.app.config.AppConfig.Lang
import com.agriedge.app.data.FarmData
import com.agriedge.app.i18n.Strings
import com.agriedge.app.model.ModelManager
import com.agriedge.app.ui.theme.*
import com.agriedge.app.viewmodel.AppTab
import com.agriedge.app.viewmodel.ChatMessage
import com.agriedge.app.viewmodel.ScreenState

@Composable
fun AgriEdgeAppScreen(
    lang: Lang,
    onLangChange: (Lang) -> Unit,
    tab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    screenState: ScreenState,
    visionState: ClassifierState,
    modelStatus: ModelManager.ModelStatus?,
    farmData: FarmData?,
    district: String,
    onDistrictChange: (String) -> Unit,
    diagnosisChatMessages: List<ChatMessage>,
    isDiagnosisChatGenerating: Boolean,
    agriSlmChatMessages: List<ChatMessage>,
    isAgriSlmGenerating: Boolean,
    onOpenCamera: () -> Unit,
    onNavigateHome: () -> Unit,
    onImageCaptured: (Bitmap) -> Unit,
    onSendDiagnosisFollowUp: (String) -> Unit,
    onSendAgriSlmMessage: (String) -> Unit,
    onClearAgriSlmChat: () -> Unit,
    onStopSLM: () -> Unit
) {
    val fullScreen = screenState is ScreenState.Camera

    Scaffold(
        containerColor = Cream,
        topBar = {
            if (!fullScreen) {
                Surface(color = Cream) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Strings.t(lang, "app_name"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Forest
                        )
                        LangSwitcher(current = lang, onChange = onLangChange)
                    }
                }
            }
        },
        bottomBar = {
            if (!fullScreen) {
                NavigationBar(containerColor = Color.White, tonalElevation = 6.dp) {
                    NavItem(tab == AppTab.Home, Icons.Default.Home, Strings.t(lang, "tab_home")) { onTabSelected(AppTab.Home) }
                    NavItem(tab == AppTab.Assistant, Icons.Default.Chat, Strings.t(lang, "tab_assistant")) { onTabSelected(AppTab.Assistant) }
                    NavItem(tab == AppTab.Market, Icons.Default.ShoppingCart, Strings.t(lang, "tab_market")) { onTabSelected(AppTab.Market) }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                AppTab.Home -> when (val state = screenState) {
                    is ScreenState.Home -> HomeScreen(
                        lang = lang,
                        visionReady = visionState is ClassifierState.Ready,
                        visionLoading = visionState is ClassifierState.Loading || visionState is ClassifierState.NotLoaded,
                        modelMissing = modelStatus != null && !modelStatus.allPresent,
                        district = district,
                        suitedCrops = farmData?.cropsForDistrict(district)?.filter { it in AppConfig.FOCUS_CROPS }.orEmpty(),
                        onImageSelected = onImageCaptured,
                        onScan = onOpenCamera,
                        onOpenAssistant = { onTabSelected(AppTab.Assistant) },
                        onOpenMarket = { onTabSelected(AppTab.Market) }
                    )

                    is ScreenState.Camera -> CameraScreen(
                        lang = lang,
                        onImageCaptured = onImageCaptured,
                        onBack = onNavigateHome
                    )

                    is ScreenState.Analyzing -> Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = Forest)
                        Spacer(Modifier.height(16.dp))
                        Text(state.message, style = MaterialTheme.typography.titleMedium, color = Muted)
                    }

                    is ScreenState.Result -> {
                        val info = farmData?.diseaseFor(state.classifierResult.rawLabel)
                        ResultScreen(
                            lang = lang,
                            result = state.classifierResult,
                            slmText = state.slmText,
                            isSlmGenerating = state.isSlmGenerating,
                            totalPipelineTimeMs = state.totalPipelineTimeMs,
                            info = info,
                            chatMessages = diagnosisChatMessages,
                            isChatGenerating = isDiagnosisChatGenerating,
                            onSendFollowUp = onSendDiagnosisFollowUp,
                            onScanAnother = onNavigateHome,
                            onStopSLM = onStopSLM
                        )
                    }

                    is ScreenState.Error -> Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(Strings.t(lang, "error_title"), style = MaterialTheme.typography.headlineSmall, color = Danger)
                        Spacer(Modifier.height(8.dp))
                        Text(state.message, style = MaterialTheme.typography.bodyMedium, color = Muted)
                        Spacer(Modifier.height(20.dp))
                        PrimaryButton(Strings.t(lang, "return_home"), onClick = onNavigateHome)
                    }
                }

                AppTab.Assistant -> AgriSlmChatScreen(
                    lang = lang,
                    messages = agriSlmChatMessages,
                    isGenerating = isAgriSlmGenerating,
                    onSendMessage = onSendAgriSlmMessage,
                    onClearChat = onClearAgriSlmChat
                )

                AppTab.Market -> MarketScreen(
                    lang = lang,
                    farmData = farmData,
                    district = district,
                    onDistrictChange = onDistrictChange
                )
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Forest,
            selectedTextColor = Forest,
            indicatorColor = Mint
        )
    )
}
