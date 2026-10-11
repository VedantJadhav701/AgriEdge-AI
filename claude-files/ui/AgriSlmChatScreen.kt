package com.agriedge.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agriedge.app.ui.theme.DarkGreen
import com.agriedge.app.ui.theme.MediumGreen
import com.agriedge.app.viewmodel.ChatMessage

@Composable
fun AgriSlmChatScreen(
    messages: List<ChatMessage>,
    isGenerating: Boolean,
    onSendMessage: (String) -> Unit,
    onClearChat: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val suggestions = listOf(
        "🌾 Rice Blast treatment",
        "🌱 Best NPK ratio for Wheat",
        "💧 Water tips for Paddy",
        "🐛 Organic pest control"
    )

    LaunchedEffect(messages.size, messages.lastOrNull()?.text?.length) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
    ) {
        // Header
        Surface(
            shadowElevation = 2.dp,
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AgriSLM Assistant",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkGreen
                    )
                    Text(
                        text = "100% Offline Agriculture Expert",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                }

                if (messages.isNotEmpty()) {
                    IconButton(onClick = onClearChat) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear Chat",
                            tint = Color(0xFF9CA3AF)
                        )
                    }
                }
            }
        }

        // Suggested Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(suggestions) { chip ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFE8F5E9),
                    modifier = Modifier.clickable {
                        onSendMessage(chip)
                    }
                ) {
                    Text(
                        text = chip,
                        fontSize = 13.sp,
                        color = DarkGreen,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Chat List
        Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "💬 Ask AgriSLM Anything",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkGreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Ask questions about crop diseases, fertilizers, pest control, or irrigation.",
                            fontSize = 13.sp,
                            color = Color(0xFF6B7280),
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatBubble(message = msg)
                    }
                }
            }
        }

        // Input Field Bar
        Surface(
            shadowElevation = 8.dp,
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask about crops, pests, fertilizers...", fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkGreen,
                        unfocusedBorderColor = Color(0xFFD1D5DB),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (inputText.isNotBlank() && !isGenerating) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    })
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank() && !isGenerating) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank() && !isGenerating,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank() && !isGenerating) DarkGreen else Color(0xFFE5E7EB))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (message.isUser) 18.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 18.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser) DarkGreen else Color(0xFFEFEFEF)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (message.text.isEmpty() && message.isStreaming) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = DarkGreen
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Thinking...",
                            fontSize = 13.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                } else {
                    Text(
                        text = message.text,
                        fontSize = 14.sp,
                        color = if (message.isUser) Color.White else Color(0xFF1F2937),
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}
