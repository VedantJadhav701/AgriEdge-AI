package com.agriedge.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agriedge.app.classifier.ClassifierResult
import com.agriedge.app.config.AppConfig
import com.agriedge.app.config.AppConfig.Lang
import com.agriedge.app.data.FarmData
import com.agriedge.app.i18n.Strings
import com.agriedge.app.ui.theme.*
import com.agriedge.app.viewmodel.ChatMessage

@Composable
fun ResultScreen(
    lang: Lang,
    result: ClassifierResult,
    slmText: String,
    isSlmGenerating: Boolean,
    totalPipelineTimeMs: Long,
    info: FarmData.DiseaseInfo?,
    chatMessages: List<ChatMessage>,
    isChatGenerating: Boolean,
    onSendFollowUp: (String) -> Unit,
    onScanAnother: () -> Unit,
    onStopSLM: () -> Unit
) {
    var followUp by remember { mutableStateOf("") }

    val isFocus = result.crop in AppConfig.FOCUS_CROPS
    val lowConf = result.confidence < AppConfig.MIN_CONFIDENCE_TO_TRUST
    val isHealthy = result.condition.startsWith("healthy") || result.condition == "normal"
    val conditionName = info?.name?.get(lang.code)
        ?: result.condition.replace('_', ' ').replaceFirstChar { it.uppercase() }
    val pct = "%.0f%%".format(result.confidence * 100)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(Strings.t(lang, "diagnosis"), style = MaterialTheme.typography.headlineMedium, color = Forest)

        when {
            lowConf -> StatusPill(Strings.t(lang, "low_confidence"), Amber)
            isHealthy -> StatusPill(Strings.t(lang, "healthy"), Sprout)
            else -> StatusPill(Strings.t(lang, "needs_attention"), Clay)
        }

        if (!isFocus) {
            AppCard(containerColor = Amber.copy(alpha = 0.16f)) {
                Text(Strings.t(lang, "unsupported_title"), style = MaterialTheme.typography.titleMedium)
                Text(Strings.t(lang, "unsupported_msg"), style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Main result
        AppCard {
            Text(cropLabel(lang, result.crop), style = MaterialTheme.typography.labelLarge, color = Muted)
            Text(
                conditionName,
                style = MaterialTheme.typography.headlineSmall,
                color = Ink,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(Strings.t(lang, "confidence"), style = MaterialTheme.typography.bodyMedium, color = Muted)
                Text(pct, style = MaterialTheme.typography.titleMedium, color = Forest)
            }
            Spacer(Modifier.height(6.dp))
            ConfidenceBar(result.confidence)
            if (lowConf) {
                Spacer(Modifier.height(10.dp))
                Text(Strings.t(lang, "low_conf_msg"), style = MaterialTheme.typography.bodyMedium, color = Clay)
            }
        }

        // Verified explanation and treatment (offline, always available)
        if (info != null) {
            AppCard {
                SectionTitle(Strings.t(lang, "explanation"))
                Spacer(Modifier.height(6.dp))
                Text(info.explanation[lang.code].orEmpty(), style = MaterialTheme.typography.bodyLarge)
            }
            AppCard {
                SectionTitle(Strings.t(lang, "treatment"))
                Spacer(Modifier.height(6.dp))
                Text(info.treatment[lang.code].orEmpty(), style = MaterialTheme.typography.bodyLarge)
            }
        }

        // Model-written advice (streams in when the SLM is loaded)
        if (slmText.isNotEmpty() || isSlmGenerating) {
            AppCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionTitle(Strings.t(lang, "ai_advice"))
                    if (isSlmGenerating) {
                        TextButton(onClick = onStopSLM) {
                            Text(Strings.t(lang, "stop"), color = Danger)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                if (slmText.isEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Forest)
                        Spacer(Modifier.width(10.dp))
                        Text(Strings.t(lang, "generating"), style = MaterialTheme.typography.bodyMedium, color = Muted)
                    }
                } else {
                    Text(slmText, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        // Follow-up chat
        AppCard {
            SectionTitle(Strings.t(lang, "ask_title"))
            Text(Strings.t(lang, "ask_sub"), style = MaterialTheme.typography.bodyMedium, color = Muted)
            Spacer(Modifier.height(10.dp))

            if (chatMessages.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    chatMessages.forEach { ChatBubble(it) }
                }
                Spacer(Modifier.height(10.dp))
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("fu_treat", "fu_organic", "fu_prevent").forEach { key ->
                    val q = Strings.t(lang, key)
                    AssistChip(onClick = { if (!isChatGenerating) onSendFollowUp(q) }, label = { Text(q) })
                }
            }
            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = followUp,
                    onValueChange = { followUp = it },
                    placeholder = { Text(Strings.t(lang, "ask_placeholder")) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    maxLines = 2
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (followUp.isNotBlank() && !isChatGenerating) {
                            onSendFollowUp(followUp)
                            followUp = ""
                        }
                    },
                    enabled = followUp.isNotBlank() && !isChatGenerating
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = Strings.t(lang, "send"), tint = Forest)
                }
            }
        }

        // Other possibilities
        if (result.topK.size > 1) {
            AppCard {
                SectionTitle(Strings.t(lang, "other_possibilities"))
                Spacer(Modifier.height(6.dp))
                result.topK.drop(1).forEach { (label, prob) ->
                    val crop = label.substringBefore("__")
                    val cond = label.substringAfter("__", "")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "${cropLabel(lang, crop)} · ${cond.replace('_', ' ')}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text("%.0f%%".format(prob * 100), style = MaterialTheme.typography.labelLarge, color = Forest)
                    }
                }
            }
        }

        TechnicalDetailsCard(result = result, totalPipelineTimeMs = totalPipelineTimeMs)

        Spacer(Modifier.height(4.dp))
        PrimaryButton(Strings.t(lang, "scan_another"), onClick = onScanAnother)
    }
}
