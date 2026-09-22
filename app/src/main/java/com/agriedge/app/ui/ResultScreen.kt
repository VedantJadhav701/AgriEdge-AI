package com.agriedge.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.agriedge.app.classifier.ClassifierResult
import com.agriedge.app.ui.theme.DarkGreen
import com.agriedge.app.ui.theme.MediumGreen
import com.agriedge.app.viewmodel.ChatMessage

@Composable
fun ResultScreen(
    result: ClassifierResult,
    slmText: String,
    isSlmGenerating: Boolean,
    totalPipelineTimeMs: Long,
    chatMessages: List<ChatMessage>,
    isChatGenerating: Boolean,
    onSendFollowUp: (String) -> Unit,
    onUpdateCropDiagnosis: (newCrop: String, newCondition: String) -> Unit,
    onScanAnother: () -> Unit,
    onStopSLM: () -> Unit
) {
    var topKExpanded by remember { mutableStateOf(false) }
    var followUpInput by remember { mutableStateOf("") }
    var showCropDialog by remember { mutableStateOf(false) }

    val commonCrops = listOf(
        "papaya" to "curl",
        "banana" to "segatoka",
        "rice" to "leaf_blast",
        "tomato" to "early_blight",
        "chilli" to "cercospora",
        "maize" to "blight",
        "cotton" to "bacterial_blight",
        "coffee" to "leaf_rust",
        "tea" to "algal_spot"
    )

    val suggestedQuestions = listOf(
        "How to treat ${formatName(result.condition)}?",
        "Organic remedies for ${formatName(result.crop)}",
        "Best fertilizer for this condition"
    )

    if (showCropDialog) {
        AlertDialog(
            onDismissRequest = { showCropDialog = false },
            title = { Text("Select Exact Crop & Condition", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose your crop to update AI diagnosis:", fontSize = 13.sp, color = Color(0xFF4B5563))
                    commonCrops.forEach { (crop, cond) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFE8F5E9),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdateCropDiagnosis(crop, cond)
                                    showCropDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(formatName(crop), fontWeight = FontWeight.Bold, color = DarkGreen)
                                Text(formatName(cond), fontSize = 12.sp, color = MediumGreen)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCropDialog = false }) {
                    Text("Close", color = DarkGreen)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Diagnosis Result",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkGreen
                )

                OutlinedButton(
                    onClick = { showCropDialog = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Change Crop ✏️", fontSize = 12.sp, color = DarkGreen, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Crop Selection Pill Bar
            Text("Switch Crop Target:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MediumGreen)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(commonCrops) { (c, cond) ->
                    val isSelected = result.crop.equals(c, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) DarkGreen else Color(0xFFE8F5E9),
                        modifier = Modifier.clickable {
                            if (!isSelected) {
                                onUpdateCropDiagnosis(c, cond)
                            }
                        }
                    ) {
                        Text(
                            text = formatName(c),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else DarkGreen,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Crop & Condition Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    ResultField(label = "Crop", value = formatName(result.crop))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFE5E7EB))

                    ResultField(label = "Possible condition", value = formatName(result.condition))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFE5E7EB))

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Confidence", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MediumGreen)
                            Text(
                                text = "${String.format("%.1f", result.confidence * 100)}%",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { result.confidence },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = DarkGreen,
                            trackColor = Color(0xFFD8F3DC),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AI Explanation Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AI Explanation",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MediumGreen
                        )
                        if (isSlmGenerating) {
                            OutlinedButton(
                                onClick = onStopSLM,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Stop", fontSize = 12.sp, color = Color(0xFFC62828))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (slmText.isEmpty() && isSlmGenerating) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = DarkGreen
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Generating explanation...",
                                fontSize = 14.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                    } else {
                        Text(
                            text = slmText,
                            fontSize = 14.sp,
                            color = Color(0xFF1F2937),
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Diagnosis Follow-up Chat Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "💬 Diagnosis Follow-Up Chat",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkGreen
                    )
                    Text(
                        text = "Ask follow-up questions about this ${formatName(result.crop)} diagnosis.",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (chatMessages.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            chatMessages.forEach { msg ->
                                ChatBubble(message = msg)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        suggestedQuestions.take(2).forEach { q ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White,
                                modifier = Modifier.clickable {
                                    onSendFollowUp(q)
                                }
                            ) {
                                Text(
                                    text = q,
                                    fontSize = 11.sp,
                                    color = DarkGreen,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = followUpInput,
                            onValueChange = { followUpInput = it },
                            placeholder = { Text("Ask about treatments, fertilizers...", fontSize = 13.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DarkGreen,
                                unfocusedBorderColor = Color(0xFFD1D5DB),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            maxLines = 2,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (followUpInput.isNotBlank() && !isChatGenerating) {
                                    onSendFollowUp(followUpInput)
                                    followUpInput = ""
                                }
                            })
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = {
                                if (followUpInput.isNotBlank() && !isChatGenerating) {
                                    onSendFollowUp(followUpInput)
                                    followUpInput = ""
                                }
                            },
                            enabled = followUpInput.isNotBlank() && !isChatGenerating,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (followUpInput.isNotBlank() && !isChatGenerating) DarkGreen else Color(0xFFD1D5DB))
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

            Spacer(modifier = Modifier.height(16.dp))

            // Collapsible Top-K section
            if (result.topK.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { topKExpanded = !topKExpanded }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Other possible conditions",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MediumGreen
                            )
                            Text(
                                text = if (topKExpanded) "▲" else "▼",
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280)
                            )
                        }

                        AnimatedVisibility(visible = topKExpanded) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                result.topK.forEachIndexed { idx, (label, prob) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${idx + 1}. $label",
                                            fontSize = 13.sp,
                                            color = Color(0xFF374151)
                                        )
                                        Text(
                                            text = "${String.format("%.1f", prob * 100)}%",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = DarkGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Technical Details Card
            TechnicalDetailsCard(result = result, totalPipelineTimeMs = totalPipelineTimeMs)
        }

        Column(modifier = Modifier.padding(top = 24.dp)) {
            ActionButton(
                text = "Scan Another Leaf",
                onClick = onScanAnother
            )
        }
    }
}

@Composable
private fun ResultField(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MediumGreen)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DarkGreen)
    }
}

private fun formatName(name: String): String {
    return name.split("_")
        .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
}
