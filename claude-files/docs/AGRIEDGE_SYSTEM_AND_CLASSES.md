# AgriEdge AI: Complete Technical Architecture, Implementation & 103 Crop Classes Guide 🌾📱

---

## 1. Executive Summary

**AgriEdge AI** is a state-of-the-art, 100% offline edge AI agricultural diagnostic and agronomic advisory system running natively on Android smartphones. 

Traditional agricultural AI systems rely heavily on cloud APIs, which fail in rural, low-connectivity farming environments. AgriEdge AI solves this by executing both vision classification (Vision Transformer) and natural language generation (Small Language Model) locally on the device CPU/NPU using native C++ and optimized runtimes, with **zero internet permissions**, **zero cloud dependencies**, and **zero latency overhead**.

---

## 2. Complete Technical Architecture

AgriEdge AI operates as a unified two-stage edge inference pipeline:

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                             AGRIEDGE ARCHITECTURE                                │
└──────────────────────────────────────────────────────────────────────────────────┘
                                        │
                         [CameraX / Gallery Image]
                                        │
                                        ▼
             ┌─────────────────────────────────────────────────────┐
             │            STAGE 1: VISION CLASSIFIER               │
             │   - Center-crop square aspect ratio                 │
             │   - 224x224 RGB FloatBuffer (NCHW format)           │
             │   - Normalization: Mean=[0.5,0.5,0.5], Std=[0.5]    │
             │   - Model: SigLIP ViT INT8 (agri_classifier)        │
             │   - Engine: ONNX Runtime Mobile + XNNPACK (4T)      │
             │   - Output: Top-K Softmax across 103 Indian Classes │
             └─────────────────────────────────────────────────────┘
                                        │
                           (Crop, Condition, Confidence)
                                        │
                                        ▼
             ┌─────────────────────────────────────────────────────┐
             │             STAGE 2: AGRONOMIC SLM (C++)            │
             │   - Engine: Native C++ llama.cpp via JNI            │
             │   - Library: libagriedge_llm.so (arm64-v8a)         │
             │   - Model: SmolLM2-360M-Instruct (Q4_K_M GGUF)      │
             │   - Context: 2048 tokens, 4 CPU threads             │
             │   - Prompt Template: ChatML                         │
             │   - Dynamic Agricultural System Prompt              │
             └─────────────────────────────────────────────────────┘
                                        │
                                        ▼
             ┌─────────────────────────────────────────────────────┐
             │               DUAL INTERACTION LAYER                │
             │   1. Diagnosis Card: Confidence, Top-3, Treatment   │
             │   2. Interactive Follow-Up Chat (Disease-Specific)  │
             │   3. AgriSLM Assistant Tab (General Farming Q&A)    │
             └─────────────────────────────────────────────────────┘
```

---

## 3. Supported Crops & All 103 Disease Classes

The model covers **17 major Indian crops** and **103 specific crop/disease conditions** trained on field-captured agricultural data.

### Class Index Breakdown by Crop

#### 🍌 1. Banana (3 Classes)
| Class Label in Model | Description / Symptoms |
| :--- | :--- |
| `banana__healthy` | Clean, vibrant green leaf without spots or wilting |
| `banana__segatoka` | Sigatoka leaf spot (yellow streaks turning dark brown/black with grey centers) |
| `banana__xamthomonas` | Xanthomonas bacterial wilt (yellowing, progressive wilting of leaves) |

#### 🍃 2. Betel Leaf / Paan (4 Classes)
| Class Label in Model | Description / Symptoms |
| :--- | :--- |
| `betel_leaf__bacterial_leaf_disease` | Water-soaked spots surrounded by yellow halos |
| `betel_leaf__dried_leaf` | Desiccation, brittle brown edges, moisture stress |
| `betel_leaf__fungal_brown_spot_disease` | Circular reddish-brown necrotic spots on foliage |
| `betel_leaf__healthy_leaf` | Smooth, glossy, healthy heart-shaped green leaf |

#### 🌾 3. Black Gram / Urad (5 Classes)
| Class Label in Model | Description / Symptoms |
| :--- | :--- |
| `black_gram__anthracnose` | Sunken dark lesions with defined margins on leaves and pods |
| `black_gram__healthy` | Normal healthy green foliage |
| `black_gram__leaf_crinckle` | Leaf crinkle virus (puckered, distorted, thickened leaf blades) |
| `black_gram__powdery_mildew` | White powdery fungal patches across leaf surfaces |
| `black_gram__yellow_mosaic` | Yellow mosaic virus (bright yellow mottled patches on green leaves) |

#### 🌶️ 4. Chilli / Mirchi (5 Classes)
| Class Label in Model | Description / Symptoms |
| :--- | :--- |
| `chilli__cercospora` | Cercospora leaf spot (frog-eye spots with light gray centers and dark borders) |
| `chilli__healthy` | Crisp green leaves, normal branching |
| `chilli__mites_and_trips` | Upward or downward leaf curling, thrips scarring, stunted shoot tips |
| `chilli__nutritional` | General chlorosis, nitrogen/iron/magnesium deficiency symptoms |
| `chilli__powdery_mildew` | White fungal growth on leaf undersides with yellowing on upper surfaces |

#### 🥥 5. Coconut (5 Classes)
| Class Label in Model | Description / Symptoms |
| :--- | :--- |
| `coconut__bud_root_dropping` | Premature nut or button falling, rotting at base |
| `coconut__bud_rot` | Browning and collapse of the spear leaf and central bud |
| `coconut__gray_leaf_spot` | Grayish-white spots with dark brown margins on older leaflets |
| `coconut__leaf_rot` | Blackening and rotting of leaflet distal ends |
| `coconut__stem_bleeding` | Exudation of dark reddish-brown liquid through stem cracks |

#### ☕ 6. Coffee (5 Classes)
| Class Label in Model | Description / Symptoms |
| :--- | :--- |
| `coffee__cerscospora` | Cercospora brown eye spot on leaves |
| `coffee__healthy` | Thick, waxy, dark-green healthy leaves |
| `coffee__leaf_rust` | Hemileia vastatrix (bright orange-yellow powdery pustules on underside) |
| `coffee__miner` | Leaf miner silvery serpentine tracks and blotches |
| `coffee__phoma` | Phoma leaf blight (dark necrotic lesions at leaf margins and tips) |

#### 🥒 7. Cucumber (8 Classes)
| Class Label in Model | Description / Symptoms |
| :--- | :--- |
| `cucumber__anthracnose` | Water-soaked circular lesions turning brown and shattering |
| `cucumber__bacterial_wilt` | Erwinia tracheiphila (rapid vine wilting during midday) |
| `cucumber__belly_rot` | Rhizoctonia fruit rot on fruit surface touching soil |
| `cucumber__downy_mildew` | Angular yellow chlorotic patches constrained by leaf veins |
| `cucumber__fresh_cucumber` | Healthy developing cucumber fruit |
| `cucumber__fresh_leaf` | Healthy cucumber foliage |
| `cucumber__gummy_stem_blight` | Brown circular lesions with black pycnidia and gummy exudate |
| `cucumber__pythium_fruit_rot` | Watery soft rot covered in white cottony mycelium |

#### 🍈 8. Guava / Amrood (4 Classes)
| Class Label in Model | Description / Symptoms |
| :--- | :--- |
| `guava__canker` | Pestalotiopsis canker (raised corky brown eruptions on leaves and fruit) |
| `guava__dot` | Small necrotic spots across fruit and foliage |
| `guava__mummification` | Shriveling and drying of fruit into hardened mummies |
| `guava__rust` | Puccinia psidii (yellow pustules on young foliage and shoots) |

#### 🍇 9. Jamun / Black Plum (6 Classes)
| Class Label in Model | Description / Symptoms |
| :--- | :--- |
| `jamun__bacterial_spot` | Angular dark brown bacterial lesions |
| `jamun__brown_blight` | Extensive brown blighting of leaf margins and tips |
| `jamun__dry` | Severe drought or tip desiccation |
| `jamun__healthy` | Vigorous, glossy, deep green foliage |
| `jamun__powdery_mildew` | Whitish powdery coating on young tender leaves |
| `jamun__sooty_mold` | Black superficial fungal coating resulting from honeydew excretion |

#### 🌽 10. Maize / Corn (4 Classes)
| Class Label in Model | Description / Symptoms |
| :--- | :--- |
| `maize__blight` | Northern corn leaf blight (cigar-shaped grayish-green lesions) |
| `maize__common_rust` | Puccinia sorghi (cinnamon brown pustules scattered across leaves) |
| `maize__gray_leaf_spot` | Rectangular brown lesions restricted by parallel leaf veins |
| `maize__healthy` | Robust green leaves with standard venation |

#### 🧅 11. Onion / Pyaz (4 Classes)
| Class Label in Model | Description / Symptoms |
| :--- | :--- |
| `onion__healthy` | Upright, tubular green foliage |
| `onion__iris_yellow_virus` | Straw-colored diamond or oval lesions on flower scapes and leaves |
| `onion__leaf_blight` | Stemphylium blight (light yellow-brown elliptical spots coalescing) |
| `onion__purple_blotch` | Alternaria porri (purple centered sunken lesions with yellow halo) |

#### 🍊 12. Orange / Citrus (11 Classes)
| Class Label in Model | Description / Symptoms |
| :--- | :--- |
| `orange__citrus_canker` | Raised, corky lesions with oily yellowish halo |
| `orange__citrus_greening` | Huanglongbing (asymmetric blotchy mottle, yellow veins, hard leaves) |
| `orange__citrus_mealybugs` | White waxy clusters of mealybugs on veins and shoots |
| `orange__die_back` | Progressive drying and death of twigs from tip downward |
| `orange__foliage_damaged` | Chewed, shredded, or mechanically damaged leaves |
| `orange__healthy_leaf` | Uniform rich green citrus leaves |
| `orange__powdery_mildew` | Superficial white powdery mildew colonies |
| `orange__shot_hole` | Small necrotic spots dropping out leaving shot-hole perforations |
| `orange__spiny_whitefly` | Black pupal cases with glassy spines on leaf undersides |
| `orange__yellow_dragon` | Yellow shoot symptoms associated with HLB vector attack |
| `orange__yellow_leaves` | General chlorosis, nutrient deficiency or root distress |

#### 🍈 13. Papaya (5 Classes)
| Class Label in Model | Description / Symptoms |
| :--- | :--- |
| `papaya__anthracnose` | Circular water-soaked lesions developing orange/pink spore masses |
| `papaya__bacterialspot` | Water-soaked angular spots on leaf lamina |
| `papaya__curl` | Papaya leaf curl virus (curling of leaves, thickened veins, stunted growth) |
| `papaya__healthy` | Large, healthy, palmate green leaves |
| `papaya__ringspot` | Papaya ringspot virus (yellow mosaic, shoe-string leaves, water-soaked oily streaks) |

#### 🌾 14. Rice / Paddy (14 Classes)
| Class Label in Model | Description / Symptoms |
| :--- | :--- |
| `rice__bacterial_leaf_blight` | Xanthomonas oryzae (wavy greenish-yellow lesions starting at leaf margins) |
| `rice__bacterial_leaf_streak` | Narrow, water-soaked translucent streaks between veins turning brown |
| `rice__bacterial_panicle_blight` | Discoloration and abortion of panicles |
| `rice__blast` | Magnaporthe oryzae (spindle-shaped lesions with grey/white centers) |
| `rice__brown_spot` | Bipolaris oryzae (oval brown spots resembling sesame seeds with yellow halo) |
| `rice__dead_heart` | Stem borer damage causing death of the central growing shoot |
| `rice__downy_mildew` | Yellowish-white chlorotic stripes and stunted tillers |
| `rice__healthy_rice_leaf` | Clean, linear emerald green leaf blades |
| `rice__hispa` | Dicladispa armigera (white parallel feeding streaks from leaf beetle larvae) |
| `rice__leaf_blast` | Diamond/spindle lesions coalescing across leaf blades |
| `rice__leaf_scald` | Zonate olive/brown chevron bands from leaf tip |
| `rice__normal` | Standard healthy vegetative rice plant |
| `rice__sheath_blight` | Rhizoctonia solani (irregular oval greenish-grey water-soaked lesions on leaf sheath) |
| `rice__tungro` | Rice tungro virus (yellow-orange leaf discoloration, stunted tillering) |

#### 🌻 15. Sunflower (4 Classes)
| Class Label in Model | Description / Symptoms |
| :--- | :--- |
| `sunflower__downy_mildew` | Systemic chlorosis along veins with white downy growth on undersides |
| `sunflower__fresh_leaf` | Healthy large cordate green sunflower foliage |
| `sunflower__gray_mold` | Botrytis cinerea (gray fuzzy mold on foliage and head) |
| `sunflower__leaf_scars` | Mechanical scarring, hail damage, or insect feeding scars |

#### 🍵 16. Tea (6 Classes)
| Class Label in Model | Description / Symptoms |
| :--- | :--- |
| `tea__algal_spot` | Cephaleuros virescens (orange/rust colored velvety circular algal colonies) |
| `tea__brown_blight` | Colletotrichum camelliae (dark brown patches starting at leaf margins) |
| `tea__gray_blight` | Pestalotiopsis theae (concentric zonate grayish-white patches with black dots) |
| `tea__healthy` | Tender two-leaves-and-a-bud healthy tea foliage |
| `tea__helopeltis` | Tea mosquito bug feeding punctures (brown sunken necrotic spots) |
| `tea__red_spot` | Cercospora theae reddish-brown necrotic spots |

#### 🍅 17. Tomato (10 Classes)
| Class Label in Model | Description / Symptoms |
| :--- | :--- |
| `tomato__bacterial_spot` | Small water-soaked circular dark lesions with yellow halos |
| `tomato__early_blight` | Alternaria solani (concentric rings creating target-board spots on older leaves) |
| `tomato__healthy` | Vigorously growing healthy compound leaves |
| `tomato__late_blight` | Phytophthora infestans (large water-soaked dark lesions with pale margins and white mold) |
| `tomato__leaf_mold` | Passalora fulva (pale green/yellow spots on upper surface, velvety olive mold beneath) |
| `tomato__septoria_leaf_spot` | Circular spots with dark margins and gray centers containing black pycnidia |
| `tomato__spider_mites_two_spotted_spider_mite` | Yellow stippling, fine webbing on undersides, bronzed brittle foliage |
| `tomato__target_spot` | Corynespora cassiicola (brown circular lesions with distinct target rings) |
| `tomato__tomato_mosaic_virus` | Mottled light and dark green patterns, distorted fern-like leaves |
| `tomato__tomato_yellow_leaf_curl_virus` | Upward cupping of leaves, chlorotic margins, extreme stunting |

---

## 4. Key Engineering Fixes Implemented

### 1. Vision Model Logit Calibration (The ~2.5% Confidence Fix)
- **Problem**: Model output logits were flat ($\text{std} = 0.70$), producing uniform probability $P \approx 1/103 \approx 2.5\%$.
- **Root Cause**: During int8 quantization, the SigLIP patch embedding `Conv` layer weights ($W_{\text{int8}} \in [-123, 127]$) were unscaled by the quantizer scale $s_w = 0.0010726810432970524$. The resulting $932\times$ weight inflation oversaturated the Transformer Attention and LayerNorm heads.
- **Resolution**: Directly multiplied Conv weights by $0.0010726810432970524$ into true FP32 values ($\in [-0.1319, 0.1362]$) and rebuilt the ONNX graph cleanly with `onnx.checker` verification.
- **Result**: Dynamic logit spread restored from $-2.29$ up to $+4.90$ ($\text{std} = 1.18$), giving calibrated top-1 predictions ($20\% - 80\%+$).

### 2. Removal of Artificial Crop Overrides
- Removed hardcoded fallback calibration blocks in `AgriEdgeViewModel.kt` that were artificially redirecting coffee or top-K candidates, ensuring purely data-driven model outputs are surfaced to the user.

### 3. Model Synchronization with Timestamp Invalidation
- Updated `ModelManager.kt` to compare `lastModified` timestamps in addition to byte lengths. When a newer model is pushed to `/sdcard/Android/data/com.agriedge.app/files/models/`, the internal app sandbox cache (`context.filesDir/models/`) automatically updates.

### 4. Scoped Storage Direct Path
- Pushed models to app-specific external storage (`/sdcard/Android/data/com.agriedge.app/files/models/`), which bypasses Android 11+ Scoped Storage restrictions without requiring special runtime permissions.

---

## 5. How to Run & Verify

1. **Push models to phone**:
   ```powershell
   .\push_to_device.ps1
   ```
2. **Install application**:
   ```powershell
   adb install -r AgriEdge-v1.0.0.apk
   ```
3. **Capture / upload test image**:
   - Point your camera at any of the 17 supported crops or upload a test photo from your gallery.
   - The diagnosis card will immediately display the predicted crop and condition with high confidence and instant SLM guidance.
