package com.agriedge.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agriedge.app.classifier.ClassifierState
import com.agriedge.app.llm.LLMState
import com.agriedge.app.model.ModelManager
import com.agriedge.app.ui.theme.DarkGreen
import com.agriedge.app.ui.theme.MediumGreen

@Composable
fun OfflineStatusBadge(
    modelStatus: ModelManager.ModelStatus?,
    visionState: ClassifierState,
    llmState: LLMState
) {
    val (statusText, statusColor) = when {
        visionState is ClassifierState.Ready -> Pair("Offline AI Ready", Color(0xFF2E7D32))
        visionState is ClassifierState.Loading || llmState is LLMState.Loading -> Pair("Loading models...", Color(0xFFE65100))
        visionState is ClassifierState.Error -> Pair("Vision Model Error: ${(visionState as ClassifierState.Error).message}", Color(0xFFC62828))
        llmState is LLMState.Error -> Pair("SLM Error: ${(llmState as LLMState.Error).message}", Color(0xFFC62828))
        modelStatus != null && !modelStatus.allPresent -> Pair("Model Files Missing", Color(0xFFC62828))
        else -> Pair("Initializing...", Color(0xFF1565C0))
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = statusColor.copy(alpha = 0.12f),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = statusText,
                color = statusColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = DarkGreen,
    contentColor: Color = Color.White
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MediumGreen
            )
            Spacer(modifier = Modifier.height(6.dp))
            content()
        }
    }
}
