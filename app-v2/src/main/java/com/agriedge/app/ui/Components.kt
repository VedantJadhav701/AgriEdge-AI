package com.agriedge.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agriedge.app.config.AppConfig.Lang
import com.agriedge.app.i18n.Strings
import com.agriedge.app.ui.theme.*
import com.agriedge.app.viewmodel.ChatMessage

/** Crop name in the current language. Unknown crops fall back to a tidy English label. */
fun cropLabel(lang: Lang, crop: String): String = when (crop.lowercase()) {
    "banana" -> when (lang) { Lang.MARATHI -> "केळी"; Lang.HINDI -> "केला"; else -> "Banana" }
    "papaya" -> when (lang) { Lang.MARATHI -> "पपई"; Lang.HINDI -> "पपीता"; else -> "Papaya" }
    "rice" -> when (lang) { Lang.MARATHI -> "भात (तांदूळ)"; Lang.HINDI -> "धान (चावल)"; else -> "Rice" }
    "tomato" -> when (lang) { Lang.MARATHI -> "टोमॅटो"; Lang.HINDI -> "टमाटर"; else -> "Tomato" }
    "chilli" -> when (lang) { Lang.MARATHI -> "मिरची"; Lang.HINDI -> "मिर्च"; else -> "Chilli" }
    "coconut" -> when (lang) { Lang.MARATHI -> "नारळ"; Lang.HINDI -> "नारियल"; else -> "Coconut" }
    "coffee" -> when (lang) { Lang.MARATHI -> "कॉफी"; Lang.HINDI -> "कॉफ़ी"; else -> "Coffee" }
    "cucumber" -> when (lang) { Lang.MARATHI -> "काकडी"; Lang.HINDI -> "खीरा"; else -> "Cucumber" }
    "guava" -> when (lang) { Lang.MARATHI -> "पेरू"; Lang.HINDI -> "अमरूद"; else -> "Guava" }
    "jamun" -> when (lang) { Lang.MARATHI -> "जांभूळ"; Lang.HINDI -> "जामुन"; else -> "Jamun" }
    "maize" -> when (lang) { Lang.MARATHI -> "मका"; Lang.HINDI -> "मक्का"; else -> "Maize" }
    "onion" -> when (lang) { Lang.MARATHI -> "कांदा"; Lang.HINDI -> "प्याज"; else -> "Onion" }
    "orange" -> when (lang) { Lang.MARATHI -> "संत्री"; Lang.HINDI -> "संतरा"; else -> "Orange" }
    "sunflower" -> when (lang) { Lang.MARATHI -> "सूर्यफूल"; Lang.HINDI -> "सूरजमुखी"; else -> "Sunflower" }
    "tea" -> when (lang) { Lang.MARATHI -> "चहा"; Lang.HINDI -> "चाय"; else -> "Tea" }
    "betel_leaf" -> when (lang) { Lang.MARATHI -> "नागवेली पान"; Lang.HINDI -> "पान"; else -> "Betel Leaf" }
    "black_gram" -> when (lang) { Lang.MARATHI -> "उडीद"; Lang.HINDI -> "उड़द"; else -> "Black Gram" }
    else -> crop.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    containerColor: Color = Color.White,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val base = modifier.fillMaxWidth()
    val m = if (onClick != null) base.clickable(onClick = onClick) else base
    Card(
        modifier = m,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), content = content)
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = Leaf)
}

@Composable
fun StatusPill(text: String, dot: Color) {
    Surface(shape = RoundedCornerShape(50), color = dot.copy(alpha = 0.14f)) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = dot, modifier = Modifier.size(9.dp)) {}
            Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, color = Ink)
        }
    }
}

@Composable
fun ConfidenceBar(confidence: Float) {
    LinearProgressIndicator(
        progress = { confidence.coerceIn(0f, 1f) },
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp),
        color = Leaf,
        trackColor = Mint
    )
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Forest, contentColor = Color.White)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun LangSwitcher(current: Lang, onChange: (Lang) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Lang.values().forEach { l ->
            FilterChip(
                selected = l == current,
                onClick = { onChange(l) },
                label = { Text(l.nativeName, style = MaterialTheme.typography.labelLarge) }
            )
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.isUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (isUser) Forest else Color(0xFFEEF2EC),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Box(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (message.text.isEmpty() && message.isStreaming) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Forest
                    )
                } else {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isUser) Color.White else Ink
                    )
                }
            }
        }
    }
}
