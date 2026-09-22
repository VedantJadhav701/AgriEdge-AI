package com.agriedge.app.ui

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agriedge.app.classifier.ClassifierState
import com.agriedge.app.llm.LLMState
import com.agriedge.app.model.ModelManager
import com.agriedge.app.ui.theme.DarkGreen

@Composable
fun HomeScreen(
    modelStatus: ModelManager.ModelStatus?,
    visionState: ClassifierState,
    llmState: LLMState,
    onOpenCamera: () -> Unit,
    onImageSelected: (Bitmap) -> Unit
) {
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = if (Build.VERSION.SDK_INT < 28) {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, it)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                }
                onImageSelected(bitmap)
            } catch (_: Exception) {
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "AgriEdge",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = DarkGreen
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Offline Crop & Disease Assistant",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF4B5563)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OfflineStatusBadge(
                modelStatus = modelStatus,
                visionState = visionState,
                llmState = llmState
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Take a photo of a crop leaf to identify the crop and possible condition.",
                        fontSize = 15.sp,
                        color = Color(0xFF374151),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            }

            if (modelStatus != null && !modelStatus.allPresent) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Model files missing in Downloads folder",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC62828)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Please place the following model files into your phone's Download folder (/storage/emulated/0/Download/):\n" +
                                    "• agri_classifier_india_v1_int8.onnx\n" +
                                    "• india_v1_labels.json\n" +
                                    "• agriedge_slm_q4_k_m.gguf",
                            fontSize = 13.sp,
                            color = Color(0xFFB71C1C),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
            ActionButton(
                text = "Take Photo",
                onClick = onOpenCamera,
                enabled = visionState is ClassifierState.Ready
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { galleryLauncher.launch("image/*") },
                enabled = visionState is ClassifierState.Ready,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Choose Image",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkGreen
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
