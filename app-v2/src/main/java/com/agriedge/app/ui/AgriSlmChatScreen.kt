package com.agriedge.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.agriedge.app.config.AppConfig.Lang
import com.agriedge.app.i18n.Strings
import com.agriedge.app.ui.theme.*
import com.agriedge.app.viewmodel.ChatMessage

@Composable
fun AgriSlmChatScreen(
    lang: Lang,
    messages: List<ChatMessage>,
    isGenerating: Boolean,
    onSendMessage: (String) -> Unit,
    onClearChat: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, messages.lastOrNull()?.text?.length) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(Strings.t(lang, "assistant_title"), style = MaterialTheme.typography.headlineSmall, color = Forest)
                Text(Strings.t(lang, "assistant_sub"), style = MaterialTheme.typography.bodyMedium, color = Muted)
            }
            if (messages.isNotEmpty()) {
                IconButton(onClick = onClearChat) {
                    Icon(Icons.Default.Delete, contentDescription = Strings.t(lang, "clear"), tint = Muted)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("suggest_1", "suggest_2", "suggest_3", "suggest_4").forEach { key ->
                val q = Strings.t(lang, key)
                AssistChip(onClick = { if (!isGenerating) onSendMessage(q) }, label = { Text(q) })
            }
        }

        Spacer(Modifier.height(12.dp))

        Box(modifier = Modifier.weight(1f)) {
            if (messages.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(Strings.t(lang, "assistant_empty"), style = MaterialTheme.typography.titleMedium, color = Muted)
                }
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(messages, key = { it.id }) { ChatBubble(it) }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text(Strings.t(lang, "ask_placeholder")) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (input.isNotBlank() && !isGenerating) {
                        onSendMessage(input)
                        input = ""
                    }
                })
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = {
                    if (input.isNotBlank() && !isGenerating) {
                        onSendMessage(input)
                        input = ""
                    }
                },
                enabled = input.isNotBlank() && !isGenerating,
                modifier = Modifier.size(52.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = Strings.t(lang, "send"))
            }
        }
    }
}

