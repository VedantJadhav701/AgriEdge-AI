package com.agriedge.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agriedge.app.classifier.ClassifierResult

@Composable
fun TechnicalDetailsCard(
    result: ClassifierResult,
    totalPipelineTimeMs: Long
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Technical details",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155)
                )
                Text(
                    text = if (expanded) "▲" else "▼",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    TechDetailRow("Model", "AgriEdge India v1")
                    TechDetailRow("Vision Backbone", "SigLIP INT8 ONNX (103 classes)")
                    TechDetailRow("Vision Input", "224 × 224 (RGB)")
                    TechDetailRow("Vision Inference", "${result.inferenceTimeMs} ms")
                    TechDetailRow("Language Model", "SmolLM2-360M Q4_K_M GGUF")
                    TechDetailRow("Total Pipeline Latency", "$totalPipelineTimeMs ms")
                    TechDetailRow("Runtime Engine", "ONNX Runtime Mobile + llama.cpp JNI")
                    TechDetailRow("Network Mode", "Fully Offline")
                }
            }
        }
    }
}

@Composable
private fun TechDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = Color(0xFF64748B))
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
    }
}
