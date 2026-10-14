# AgriEdge Full System Accuracy & Benchmark Evaluation (English) 📊🌾

This report provides the full evaluation metrics across all **103 agricultural classes** and **17 crops** on the test benchmark dataset (4,476 test images evaluated across 29,849 total dataset images).

---

## 1. Overall System Performance Summary

| Metric | Score | Note |
| :--- | :--- | :--- |
| **Top-1 Accuracy** | **92.47%** | Exact crop and condition match |
| **Top-5 Accuracy** | **99.78%** | True condition is within the top-5 candidates |
| **Macro Precision** | **92.26%** | Unweighted mean precision across all 103 classes |
| **Macro Recall** | **91.71%** | Unweighted mean recall across all 103 classes |
| **Macro F1-Score** | **91.52%** | Unweighted mean F1 across all 103 classes |
| **Weighted F1-Score** | **92.36%** | Class-frequency weighted harmonic mean |

---

## 2. Accuracy & F1-Score Grouped by Crop (17 Crops)

Ranked from highest performing to lowest:

| Crop | Classes Count | Mean Precision | Mean Recall | Mean F1-Score | Test Sample Count (Support) |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **Jamun** | 6 | 100.0% | 100.0% | **100.0%** | 270 |
| **Orange / Citrus** | 11 | 99.57% | 99.60% | **99.58%** | 488 |
| **Black Gram (Urad)** | 5 | 99.43% | 99.26% | **99.33%** | 151 |
| **Coconut** | 5 | 99.15% | 99.11% | **99.11%** | 225 |
| **Coffee** | 5 | 98.31% | 98.22% | **98.22%** | 225 |
| **Cucumber** | 8 | 97.48% | 96.94% | **97.06%** | 360 |
| **Tomato** | 10 | 95.80% | 95.56% | **95.55%** | 450 |
| **Tea** | 6 | 95.82% | 95.56% | **95.55%** | 270 |
| **Sunflower** | 4 | 94.44% | 95.00% | **94.71%** | 180 |
| **Maize** | 4 | 94.50% | 94.44% | **94.47%** | 180 |
| **Betel Leaf (Paan)** | 4 | 94.51% | 94.44% | **94.13%** | 180 |
| **Chilli** | 5 | 92.07% | 91.56% | **91.45%** | 225 |
| **Onion** | 4 | 90.17% | 89.44% | **89.21%** | 180 |
| **Banana** | 3 | 86.23% | 85.31% | **85.72%** | 113 |
| **Papaya** | 5 | 83.94% | 83.41% | **82.67%** | 214 |
| **Rice / Paddy** | 14 | 82.46% | 81.67% | **80.48%** | 720 |
| **Guava** | 4 | 55.56% | 49.05% | **49.42%** | 45 |

---

## 3. Detailed Class-by-Class Breakdown (All 103 Classes)

### 🍌 Banana (3 classes)
- `banana__healthy`: Precision: 82.61%, Recall: 82.61%, F1: 82.61%
- `banana__segatoka`: Precision: 95.24%, Recall: 88.89%, F1: 91.95%
- `banana__xamthomonas`: Precision: 80.85%, Recall: 84.44%, F1: 82.61%

### 🍃 Betel Leaf (4 classes)
- `betel_leaf__bacterial_leaf_disease`: Precision: 100.0%, Recall: 82.22%, F1: 90.24%
- `betel_leaf__dried_leaf`: Precision: 100.0%, Recall: 100.0%, F1: 100.0%
- `betel_leaf__fungal_brown_spot_disease`: Precision: 91.49%, Recall: 95.56%, F1: 93.48%
- `betel_leaf__healthy_leaf`: Precision: 86.54%, Recall: 100.0%, F1: 92.78%

### 🌾 Black Gram (5 classes)
- `black_gram__anthracnose`: Precision: 97.14%, Recall: 100.0%, F1: 98.55%
- `black_gram__healthy`: Precision: 100.0%, Recall: 100.0%, F1: 100.0%
- `black_gram__leaf_crinckle`: Precision: 100.0%, Recall: 100.0%, F1: 100.0%
- `black_gram__powdery_mildew`: Precision: 100.0%, Recall: 96.30%, F1: 98.11%
- `black_gram__yellow_mosaic`: Precision: 100.0%, Recall: 100.0%, F1: 100.0%

### 🌶️ Chilli (5 classes)
- `chilli__cercospora`: Precision: 95.74%, Recall: 100.0%, F1: 97.83%
- `chilli__healthy`: Precision: 87.18%, Recall: 75.56%, F1: 80.95%
- `chilli__mites_and_trips`: Precision: 80.00%, Recall: 97.78%, F1: 88.00%
- `chilli__nutritional`: Precision: 97.44%, Recall: 84.44%, F1: 90.48%
- `chilli__powdery_mildew`: Precision: 100.0%, Recall: 100.0%, F1: 100.0%

### 🥥 Coconut (5 classes)
- `coconut__bud_root_dropping`: Precision: 100.0%, Recall: 100.0%, F1: 100.0%
- `coconut__bud_rot`: Precision: 100.0%, Recall: 100.0%, F1: 100.0%
- `coconut__gray_leaf_spot`: Precision: 95.74%, Recall: 100.0%, F1: 97.83%
- `coconut__leaf_rot`: Precision: 100.0%, Recall: 95.56%, F1: 97.73%
- `coconut__stem_bleeding`: Precision: 100.0%, Recall: 100.0%, F1: 100.0%

### ☕ Coffee (5 classes)
- `coffee__cerscospora`: Precision: 100.0%, Recall: 93.33%, F1: 96.55%
- `coffee__healthy`: Precision: 100.0%, Recall: 100.0%, F1: 100.0%
- `coffee__leaf_rust`: Precision: 100.0%, Recall: 100.0%, F1: 100.0%
- `coffee__miner`: Precision: 93.75%, Recall: 100.0%, F1: 96.77%
- `coffee__phoma`: Precision: 97.78%, Recall: 97.78%, F1: 97.78%

### 🥒 Cucumber (8 classes)
- `cucumber__anthracnose`: Precision: 100.0%, Recall: 84.44%, F1: 91.57%
- `cucumber__bacterial_wilt`: Precision: 100.0%, Recall: 95.56%, F1: 97.73%
- `cucumber__belly_rot`: Precision: 100.0%, Recall: 100.0%, F1: 100.0%
- `cucumber__downy_mildew`: Precision: 86.54%, Recall: 100.0%, F1: 92.78%
- `cucumber__fresh_cucumber`: Precision: 100.0%, Recall: 100.0%, F1: 100.0%
- `cucumber__fresh_leaf`: Precision: 93.75%, Recall: 100.0%, F1: 96.77%
- `cucumber__gummy_stem_blight`: Precision: 100.0%, Recall: 95.56%, F1: 97.73%
- `cucumber__pythium_fruit_rot`: Precision: 97.83%, Recall: 100.0%, F1: 98.90%

### 🍈 Guava (4 classes)
- `guava__canker`: Precision: 80.00%, Recall: 33.33%, F1: 47.06%
- `guava__dot`: Precision: 40.00%, Recall: 54.55%, F1: 46.15%
- `guava__mummification`: Precision: 46.67%, Recall: 58.33%, F1: 51.85%
- `guava__rust`: Precision: 55.56%, Recall: 50.00%, F1: 52.63%

### 🍇 Jamun (6 classes)
- `jamun__bacterial_spot`: Precision: 100.0%, Recall: 100.0%, F1: 100.0%
- `jamun__brown_blight`: Precision: 100.0%, Recall: 100.0%, F1: 100.0%
- `jamun__dry`: Precision: 100.0%, Recall: 100.0%, F1: 100.0%
- `jamun__healthy`: Precision: 100.0%, Recall: 100.0%, F1: 100.0%
- `jamun__powdery_mildew`: Precision: 100.0%, Recall: 100.0%, F1: 100.0%
- `jamun__sooty_mold`: Precision: 100.0%, Recall: 100.0%, F1: 100.0%

### 🌽 Maize (4 classes)
- `maize__blight`: Precision: 87.80%, Recall: 80.00%, F1: 83.72%
- `maize__common_rust`: Precision: 97.83%, Recall: 100.0%, F1: 98.90%
- `maize__gray_leaf_spot`: Precision: 92.50%, Recall: 97.78%, F1: 95.05%
- `maize__healthy`: Precision: 100.0%, Recall: 100.0%, F1: 100.0%

### 🧅 Onion (4 classes)
- `onion__healthy`: Precision: 86.27%, Recall: 97.78%, F1: 91.67%
- `onion__iris_yellow_virus`: Precision: 100.0%, Recall: 100.0%, F1: 100.0%
- `onion__leaf_blight`: Precision: 74.58%, Recall: 97.78%, F1: 84.62%
- `onion__purple_blotch`: Precision: 100.0%, Recall: 62.22%, F1: 76.71%

### 🍊 Orange / Citrus (11 classes)
- `orange__citrus_canker`: Precision: 100.0%, Recall: 100.0%, F1: 100.0%
- `orange__citrus_greening`: Precision: 100.0%, Recall: 95.56%, F1: 97.73%
- `orange__citrus_mealybugs`: Precision: 100.0%, Recall: 100.0%, F1: 100.0%
- `orange__die_back`: Precision: 100.0%, Recall: 100.0%, F1: 100.0%
- `orange__foliage_damaged`: Precision: 100.0%, Recall: 100.0%, F1: 100.0%
- `orange__healthy_leaf`: Precision: 100.0%, Recall: 100.0%, F1: 100.0%
- `orange__powdery_mildew`: Precision: 100.0%, Recall: 100.0%, F1: 100.0%
- `orange__shot_hole`: Precision: 100.0%, Recall: 100.0%, F1: 100.0%
- `orange__spiny_whitefly`: Precision: 100.0%, Recall: 100.0%, F1: 100.0%
- `orange__yellow_dragon`: Precision: 95.26%, Recall: 100.0%, F1: 97.57%
- `orange__yellow_leaves`: Precision: 100.0%, Recall: 100.0%, F1: 100.0%

### 🍈 Papaya (5 classes)
- `papaya__anthracnose`: Precision: 82.22%, Recall: 86.05%, F1: 84.09%
- `papaya__bacterialspot`: Precision: 75.00%, Recall: 76.74%, F1: 75.86%
- `papaya__curl`: Precision: 82.93%, Recall: 79.07%, F1: 80.95%
- `papaya__healthy`: Precision: 97.56%, Recall: 93.02%, F1: 95.24%
- `papaya__ringspot`: Precision: 81.08%, Recall: 69.77%, F1: 75.00%

### 🌾 Rice / Paddy (14 classes)
- `rice__bacterial_leaf_blight`: Precision: 65.57%, Recall: 80.00%, F1: 72.07%
- `rice__bacterial_leaf_streak`: Precision: 74.00%, Recall: 82.22%, F1: 77.89%
- `rice__bacterial_panicle_blight`: Precision: 95.45%, Recall: 93.33%, F1: 94.38%
- `rice__blast`: Precision: 66.67%, Recall: 53.33%, F1: 59.26%
- `rice__brown_spot`: Precision: 81.48%, Recall: 88.00%, F1: 84.62%
- `rice__dead_heart`: Precision: 100.0%, Recall: 93.33%, F1: 96.55%
- `rice__downy_mildew`: Precision: 75.56%, Recall: 75.56%, F1: 75.56%
- `rice__healthy_rice_leaf`: Precision: 100.0%, Recall: 100.0%, F1: 100.0%
- `rice__hispa`: Precision: 76.92%, Recall: 66.67%, F1: 71.43%
- `rice__leaf_blast`: Precision: 72.55%, Recall: 82.22%, F1: 77.08%
- `rice__leaf_scald`: Precision: 95.24%, Recall: 44.44%, F1: 60.61%
- `rice__normal`: Precision: 70.00%, Recall: 77.78%, F1: 73.68%
- `rice__sheath_blight`: Precision: 80.95%, Recall: 75.56%, F1: 78.16%
- `rice__tungro`: Precision: 95.56%, Recall: 86.00%, F1: 90.53%

### 🌻 Sunflower (4 classes)
- `sunflower__downy_mildew`: Precision: 97.83%, Recall: 100.0%, F1: 98.90%
- `sunflower__fresh_leaf`: Precision: 90.00%, Recall: 100.0%, F1: 94.74%
- `sunflower__gray_mold`: Precision: 100.0%, Recall: 95.56%, F1: 97.73%
- `sunflower__leaf_scars`: Precision: 90.00%, Recall: 84.44%, F1: 87.13%

### 🍵 Tea (6 classes)
- `tea__algal_spot`: Precision: 89.80%, Recall: 97.78%, F1: 93.62%
- `tea__brown_blight`: Precision: 95.74%, Recall: 100.0%, F1: 97.83%
- `tea__gray_blight`: Precision: 97.73%, Recall: 95.56%, F1: 96.63%
- `tea__healthy`: Precision: 95.74%, Recall: 100.0%, F1: 97.83%
- `tea__helopeltis`: Precision: 100.0%, Recall: 80.00%, F1: 88.89%
- `tea__red_spot`: Precision: 100.0%, Recall: 100.0%, F1: 100.0%

### 🍅 Tomato (10 classes)
- `tomato__bacterial_spot`: Precision: 93.75%, Recall: 100.0%, F1: 96.77%
- `tomato__early_blight`: Precision: 88.24%, Recall: 100.0%, F1: 93.75%
- `tomato__healthy`: Precision: 95.45%, Recall: 93.33%, F1: 94.38%
- `tomato__late_blight`: Precision: 95.74%, Recall: 100.0%, F1: 97.83%
- `tomato__leaf_mold`: Precision: 95.65%, Recall: 97.78%, F1: 96.70%
- `tomato__septoria_leaf_spot`: Precision: 97.56%, Recall: 88.89%, F1: 93.02%
- `tomato__spider_mites_two_spotted_spider_mite`: Precision: 97.83%, Recall: 100.0%, F1: 98.90%
- `tomato__target_spot`: Precision: 95.56%, Recall: 95.56%, F1: 95.56%
- `tomato__tomato_mosaic_virus`: Precision: 100.0%, Recall: 88.89%, F1: 94.12%
- `tomato__tomato_yellow_leaf_curl_virus`: Precision: 97.83%, Recall: 100.0%, F1: 98.90%

---

## 4. End-to-End Latency & Edge Footprint Comparison

| Configuration | Precision | Vision Size | SLM Size | Mobile CPU Latency (Vision + SLM) | Top-1 Accuracy |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **AgriEdge (Current Edge Deployment)** | **INT8 + Q4_K_M** | **101.4 MB** | **270.6 MB (SmolLM2) / 491.4 MB (Qwen2.5)** | **~164 ms (Vision) + ~850 ms (SLM)** | **92.47% (Top-1) / 99.78% (Top-5)** |
| AgriEdge Baseline (FP16) | FP16 | 186.0 MB | 572.7 MB | ~234 ms + ~1200 ms | 92.47% |
| MobileNetV3 + SLM | FP32 | 17.2 MB | 270.6 MB | ~158 ms + ~850 ms | 91.67% |
| EfficientNet-B0 + SLM | FP32 | 16.4 MB | 270.6 MB | ~174 ms + ~850 ms | 93.03% |

---

## 5. Potential Role of `google/embeddinggemma-2` in AgriEdge

**EmbeddingGemma 2** (released by Google DeepMind) is a 740M parameter multimodal embedding model with a modular 170M vision encoder and 270M text encoder.

### Where it fits in our roadmap:
1. **Multimodal Crop RAG (Retrieval-Augmented Generation)**:
   - Instead of feeding just classification text (`Crop: banana, Condition: sigatoka`) into the SLM, we can embed the farmer's leaf photo directly into the shared 768d space.
   - Using MRL (Matryoshka Representation Learning) at **256d**, we can index agricultural treatment manuals and KVK university guides offline on the phone with minimal storage footprint.
2. **Zero-Shot / Few-Shot Visual Retrieval for Rare Diseases**:
   - For classes with lower training sample support (e.g., `guava`), EmbeddingGemma-2 can compare leaf embeddings against reference visual prototypes directly.
3. **Current Comparison**:
   - Our current **SigLIP INT8 (101 MB)** runs in **164 ms** on device CPU and achieves **92.47% accuracy**.
   - EmbeddingGemma-2's vision encoder (170M params) can be evaluated in parallel as a secondary feature extractor if we want full multimodal semantic search on mobile.
