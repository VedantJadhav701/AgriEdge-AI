package com.agriedge.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Offline structured data bundled in assets/data/:
 *  - maharashtra_districts.json : district -> suitable crops, season
 *  - market_prices.json         : mandi prices (synced by training/fetch_market_prices.py)
 *  - disease_info.json          : condition name, explanation and treatment in en/mr/hi
 */
class FarmData private constructor(
    val districts: List<District>,
    val marketSyncedAt: String,
    val prices: List<MarketPrice>,
    private val diseases: Map<String, DiseaseInfo>
) {

    data class District(
        val name: String,
        val nameMr: String,
        val nameHi: String,
        val crops: List<String>,
        val season: String,
        val status: String
    ) {
        fun displayName(langCode: String): String = when (langCode) {
            "mr" -> nameMr.ifBlank { name }
            "hi" -> nameHi.ifBlank { name }
            else -> name
        }
    }

    data class MarketPrice(
        val crop: String,
        val district: String,
        val market: String,
        val minRs: Int,
        val maxRs: Int,
        val modalRs: Int,
        val unit: String,
        val date: String
    )

    data class DiseaseInfo(
        val label: String,
        val name: Map<String, String>,
        val explanation: Map<String, String>,
        val treatment: Map<String, String>
    )

    fun diseaseFor(label: String): DiseaseInfo? = diseases[label]

    fun cropsForDistrict(district: String): List<String> =
        districts.firstOrNull { it.name.equals(district, ignoreCase = true) }?.crops ?: emptyList()

    fun pricesFor(crop: String, district: String?): List<MarketPrice> =
        prices.filter { it.crop == crop && (district == null || it.district.equals(district, true)) }

    companion object {
        fun load(context: Context): FarmData {
            val districts = parseDistricts(readAsset(context, "data/maharashtra_districts.json"))
            val market = JSONObject(readAsset(context, "data/market_prices.json"))
            val prices = parsePrices(market.optJSONArray("prices") ?: JSONArray())
            val diseases = parseDiseases(readAsset(context, "data/disease_info.json"))
            return FarmData(
                districts = districts,
                marketSyncedAt = market.optString("synced_at", "never"),
                prices = prices,
                diseases = diseases
            )
        }

        private fun readAsset(context: Context, path: String): String =
            context.assets.open(path).bufferedReader().use { it.readText() }

        private fun parseDistricts(json: String): List<District> {
            val arr = JSONArray(json)
            return List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                District(
                    name = o.getString("name"),
                    nameMr = o.optString("name_mr"),
                    nameHi = o.optString("name_hi"),
                    crops = o.getJSONArray("crops").let { c -> List(c.length()) { c.getString(it) } },
                    season = o.optString("season"),
                    status = o.optString("status")
                )
            }
        }

        private fun parsePrices(arr: JSONArray): List<MarketPrice> =
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                MarketPrice(
                    crop = o.getString("crop"),
                    district = o.getString("district"),
                    market = o.optString("market"),
                    minRs = o.optInt("min_rs"),
                    maxRs = o.optInt("max_rs"),
                    modalRs = o.optInt("modal_rs"),
                    unit = o.optString("unit", "quintal"),
                    date = o.optString("date")
                )
            }

        private fun parseDiseases(json: String): Map<String, DiseaseInfo> {
            val arr = JSONArray(json)
            val out = HashMap<String, DiseaseInfo>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val label = o.getString("label")
                out[label] = DiseaseInfo(
                    label = label,
                    name = o.getJSONObject("name").toStringMap(),
                    explanation = o.getJSONObject("explanation").toStringMap(),
                    treatment = o.getJSONObject("treatment").toStringMap()
                )
            }
            return out
        }

        private fun JSONObject.toStringMap(): Map<String, String> =
            keys().asSequence().associateWith { getString(it) }
    }
}
