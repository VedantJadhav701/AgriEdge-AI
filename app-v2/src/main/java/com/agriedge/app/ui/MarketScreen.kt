package com.agriedge.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agriedge.app.config.AppConfig
import com.agriedge.app.config.AppConfig.Lang
import com.agriedge.app.data.FarmData
import com.agriedge.app.i18n.Strings
import com.agriedge.app.ui.theme.*

@Composable
fun MarketScreen(
    lang: Lang,
    farmData: FarmData?,
    district: String,
    onDistrictChange: (String) -> Unit
) {
    var crop by remember { mutableStateOf(AppConfig.DEFAULT_CROP) }
    var yieldQtl by remember { mutableStateOf("") }
    var costRs by remember { mutableStateOf("") }

    if (farmData == null) {
        Box(Modifier.fillMaxSize().padding(20.dp)) {
            Text(Strings.t(lang, "model_loading"), color = Muted)
        }
        return
    }

    val prices = farmData.pricesFor(crop, district)
    val modal = prices.map { it.modalRs }.filter { it > 0 }.takeIf { it.isNotEmpty() }?.average()
    val yieldValue = yieldQtl.toDoubleOrNull()
    val costValue = costRs.toDoubleOrNull()
    val profit = if (yieldValue != null && costValue != null && modal != null) {
        yieldValue * modal - costValue
    } else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(Strings.t(lang, "market_title"), style = MaterialTheme.typography.headlineMedium, color = Forest)
        Text(Strings.t(lang, "market_sub"), style = MaterialTheme.typography.bodyMedium, color = Muted)

        // District chips
        SectionTitle(Strings.t(lang, "select_district"))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            farmData.districts.forEach { d ->
                FilterChip(
                    selected = d.name == district,
                    onClick = { onDistrictChange(d.name) },
                    label = { Text(d.displayName(lang.code)) }
                )
            }
        }

        // Crop chips
        SectionTitle(Strings.t(lang, "select_crop"))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppConfig.FOCUS_CROPS.forEach { c ->
                FilterChip(
                    selected = c == crop,
                    onClick = { crop = c },
                    label = { Text(cropLabel(lang, c)) }
                )
            }
        }

        // Price card
        AppCard {
            SectionTitle(Strings.t(lang, "avg_price"))
            Spacer(Modifier.height(6.dp))
            if (modal == null) {
                Text(Strings.t(lang, "price_none"), style = MaterialTheme.typography.bodyMedium, color = Muted)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${Strings.t(lang, "last_synced")}: ${farmData.marketSyncedAt}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Muted
                )
            } else {
                Text(
                    "Rs ${modal.toInt()} ${Strings.t(lang, "per_quintal")}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Forest,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(Strings.t(lang, "top_markets"), style = MaterialTheme.typography.labelLarge, color = Muted)
                prices.take(5).forEach {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(it.market.ifBlank { it.district }, style = MaterialTheme.typography.bodyMedium)
                        Text("Rs ${it.modalRs}", style = MaterialTheme.typography.bodyMedium, color = Forest)
                    }
                }
            }
        }

        // Profit calculator
        AppCard {
            SectionTitle(Strings.t(lang, "profit_label"))
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = yieldQtl,
                onValueChange = { yieldQtl = it },
                label = { Text(Strings.t(lang, "yield_input")) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = costRs,
                onValueChange = { costRs = it },
                label = { Text(Strings.t(lang, "cost_input")) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            if (profit != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "${if (profit >= 0) Strings.t(lang, "profit_label") else Strings.t(lang, "profit_loss")}: Rs ${"%.0f".format(kotlin.math.abs(profit))}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (profit >= 0) Forest else Danger
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(Strings.t(lang, "profit_note"), style = MaterialTheme.typography.labelLarge, color = Muted)
        }
    }
}
