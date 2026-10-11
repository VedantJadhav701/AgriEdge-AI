package com.agriedge.app.ui

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.unit.dp
import com.agriedge.app.config.AppConfig.Lang
import com.agriedge.app.i18n.Strings
import com.agriedge.app.ui.theme.*

@Composable
fun HomeScreen(
    lang: Lang,
    visionReady: Boolean,
    visionLoading: Boolean,
    modelMissing: Boolean,
    district: String,
    suitedCrops: List<String>,
    onImageSelected: (Bitmap) -> Unit,
    onScan: () -> Unit,
    onOpenAssistant: () -> Unit,
    onOpenMarket: () -> Unit
) {
    val context = LocalContext.current
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val bmp = if (Build.VERSION.SDK_INT < 28) {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                } else {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, it)) { decoder, _, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                }
                onImageSelected(bmp)
            } catch (_: Exception) {
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(Strings.t(lang, "greeting"), style = MaterialTheme.typography.headlineMedium, color = Forest)
            Text(Strings.t(lang, "home_sub"), style = MaterialTheme.typography.bodyMedium, color = Muted)
        }

        // Hero scan card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Forest)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(Strings.t(lang, "hero_title"), style = MaterialTheme.typography.headlineSmall, color = Color.White)
                Text(Strings.t(lang, "hero_sub"), style = MaterialTheme.typography.bodyMedium, color = Mint)
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onScan,
                    enabled = visionReady,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Amber,
                        contentColor = Ink,
                        disabledContainerColor = Color(0x66FFFFFF),
                        disabledContentColor = Color(0xCCFFFFFF)
                    )
                ) {
                    Text(Strings.t(lang, "take_photo"), style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = { gallery.launch("image/*") },
                    enabled = visionReady,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Mint),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(Strings.t(lang, "choose_image"), style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Model status
        when {
            modelMissing -> StatusPill(Strings.t(lang, "model_missing"), Danger)
            visionLoading || !visionReady -> StatusPill(Strings.t(lang, "model_loading"), Amber)
            else -> StatusPill(Strings.t(lang, "tab_home") + " ✓", Sprout)
        }

        // Crops suited to the chosen district
        AppCard {
            SectionTitle("${Strings.t(lang, "district_label")}: $district")
            Spacer(Modifier.height(10.dp))
            if (suitedCrops.isEmpty()) {
                Text(Strings.t(lang, "no_crops"), style = MaterialTheme.typography.bodyMedium, color = Muted)
            } else {
                Text(Strings.t(lang, "district_suits"), style = MaterialTheme.typography.labelLarge, color = Muted)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    suitedCrops.forEach { crop ->
                        AssistChip(
                            onClick = onOpenMarket,
                            label = { Text(cropLabel(lang, crop)) }
                        )
                    }
                }
            }
        }

        // Quick links
        AppCard(onClick = onOpenAssistant) {
            Text(Strings.t(lang, "quick_assistant"), style = MaterialTheme.typography.titleMedium, color = Forest)
            Text(Strings.t(lang, "quick_assistant_sub"), style = MaterialTheme.typography.bodyMedium, color = Muted)
        }
        AppCard(onClick = onOpenMarket) {
            Text(Strings.t(lang, "quick_market"), style = MaterialTheme.typography.titleMedium, color = Forest)
            Text(Strings.t(lang, "quick_market_sub"), style = MaterialTheme.typography.bodyMedium, color = Muted)
        }
    }
}
