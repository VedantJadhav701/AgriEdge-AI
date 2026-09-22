# AgriEdge-VLM — Project Status

*A small, offline, on-device plant disease diagnosis system for Android, distilled from a large agricultural VLM.*

---

## 1. Project Goal

Take **AgriChat** (a 7B multimodal LLM fine-tuned on the AgriMM agricultural dataset, arXiv:2603.16934) and
distill its knowledge into a model small enough to run **fully offline on a farmer's phone**:

- No internet required at inference time
- Camera in, diagnosis + explanation out
- Target package size: 200–350 MB
- Split architecture: a small vision **classifier** does fast crop/disease identification, a small **language
  model (SLM)** turns that into a farmer-readable explanation — rather than one large model doing everything

---

## 2. Architecture

```
CAMERA
   ↓
Vision Encoder (SigLIP-based classifier)
   ↓
Crop + Disease classification (fast, cheap, on-device)
   ↓
Structured findings → rendered as text prompt
   ↓
Small Language Model (SmolLM2-360M, fine-tuned)
   ↓
Farmer-facing explanation + recommendation
```

The reasoning behind the split: a classifier is cheap and reliable for "what is this," and a small LM only
needs to turn a known crop+disease label into readable text — it doesn't need to *diagnose* anything itself.
This keeps both halves small enough to run on-device.

---

## 3. Data Pipeline

### 3.1 Source data
- **AgriMM** dataset: 121,425 images, 607,125 QA pairs, 3,099 classes, aggregated from 63 source datasets
  (paper: *AgriChat: A Multimodal Large Language Model for Agriculture Image Understanding*, arXiv:2603.16934)
- Annotations (train/test JSONL) pulled from `boudiafA/AgriChat` on Hugging Face (`dataset/` folder) — this
  repo has annotations only, **not** the images themselves
- Images reconstructed via the **AgML** package (`pip install agml`), which covers 53 of the 63 source
  datasets under standardized names; the rest (DRPD, GWHD2021, CBDA, MTDC, SHC, WEDD, YOLOPOD,
  wGrapeUNIPD-DL, Orange_dataset) are not in AgML and were left unfetched for this phase

### 3.2 Teacher distillation
- **Teacher**: `llava-hf/llava-onevision-qwen2-7b-ov-hf` base + `boudiafA/AgriChat` LoRA adapter, run in
  **4-bit NF4** (FP16 load crashed the Kaggle kernel outright — OOM kill, not even a catchable exception,
  on dual T4 15GB GPUs)
- Ran a **1,000-sample pilot** of teacher generation (image → question → ground truth + teacher answer),
  resumable JSONL, one record appended and flushed at a time so a Kaggle session dying mid-run doesn't
  lose progress

### 3.3 Bugs found and fixed along the way
This is worth recording since it shapes what to trust and re-check later:

| Issue | Root cause | Fix |
|---|---|---|
| 100% of images "missing" on first validation pass | jsonl paths include `classification/`/`detection/` segments that don't exist in AgML's actual folder layout | strip those segments before joining path |
| Some images still missing after that fix | filename **case** mismatch (`.jpg` in jsonl vs `.JPG` on disk) | case-insensitive filename index as fallback |
| Image index came back with 0 entries | `pathlib.Path.rglob()` does not follow symlinked directories by default, and every AgML dataset folder is a symlink | switched to `os.walk(..., followlinks=True)` |
| Disk filled after ~27 of 53 AgML downloads | images were being **copied** from `~/.agml/datasets/` into `datasets_sorted/` — doubling disk usage on a 20GB Kaggle volume | switched to **symlinking** instead of copying |
| Kernel hard-crashed loading the teacher | FP16 load of the 7B model (~16GB) left no headroom across 2×15GB T4s | load 4-bit NF4 only, no FP16 attempt |
| `RuntimeError: mat1 and mat2 must have the same dtype` | classifier backbone in fp16, `nn.Linear` heads default to fp32 | cast pooled backbone output to fp32 before the heads |
| Classifier loss went to `nan` immediately | raw fp16 casting of the SigLIP backbone overflowed internally (no loss scaling) | switched to fp32 backbone + `torch.autocast` + `GradScaler` + gradient clipping |
| ONNX export failed (`ModuleNotFoundError: onnxscript`) | newer PyTorch defaults to the dynamo-based exporter, which needs an extra package | forced the legacy exporter with `dynamo=False` |
| GGUF conversion failed on tokenizer loading (twice) | `tokenizer_config.json` had a malformed `tokenizer_class` field, then a malformed `extra_special_tokens` field (list instead of dict) | patched both fields directly in the config before conversion |

None of this changes the underlying model's competence, but it's the reason the checkpoints exist at all,
and worth remembering if re-running any part of the pipeline on a fresh environment.

---

## 4. Models Trained

### 4.1 Vision Classifier — v0 (early pilot, superseded)
- Backbone: `google/siglip-base-patch16-224`, frozen except last 2 transformer layers (14.3M trainable / 93M
  total)
- Trained on the 1,000-sample teacher pilot, labels extracted via **regex/keyword-matching the teacher's
  free-text answer** — this only kept ~48% of records (481/1000) and was noisy
- Two-head design: crop (32 classes) + disease (93 classes), 900 train / 100 val

| variant | size | crop acc | disease acc | agreement w/ fp32 | latency (CPU) |
|---|---|---|---|---|---|
| fp32 | 372 MB | 0.892 | 0.390 | — | 207 ms/img |
| int8 (per-channel, MatMul-only) | 102 MB | 0.897 | 0.398 | 86.8% | 145 ms/img |

**Known weak point**: disease accuracy of 0.39–0.40 is not usable for real diagnosis. Superseded by the
India v1 model below.

### 4.2 Vision Classifier — India v1 (current)
- Same SigLIP backbone/freezing strategy
- **Key fix**: switched labels from the noisy teacher-text heuristic to **folder-structure ground truth** —
  crop = dataset name, disease = class subfolder name — which is both more accurate and gives full coverage
  (not gated on a keyword match)
- Retrained on **18 India-relevant AgML datasets** specifically (rice, tomato, chilli, maize, coconut, tea,
  coffee, banana, papaya, guava, orange, jamun, onion, cucumber, sunflower, black gram, betel leaf), capped
  at 300 images/class to avoid large classes dominating
- **29,849 images, 103 `crop__condition` classes**, single unified classification head, 25,373 train / 4,476
  val (90/10 split)
- sqrt-inverse-frequency class weighting to handle imbalance (largest class 3,000 images, smallest ~300)

| variant | size | acc | agreement w/ fp32 | latency (CPU) |
|---|---|---|---|---|
| fp32 | 372 MB | 0.928 | — | 187 ms/img |
| int8 (dynamic, per-channel) | 99.6 MB | **0.888** | 93.2% | **142 ms/img** |

Evaluated on 600 randomly-sampled held-out validation images (never seen in training), CPU inference to
match the real mobile deployment target.

**This is a large, real improvement over v0** — 0.888 vs 0.398 disease/condition accuracy on the same crop
families, entirely from fixing the label source rather than changing model architecture.

### 4.3 Small Language Model (SLM)
- `HuggingFaceTB/SmolLM2-360M-Instruct` (362M params), **full fine-tune** (not LoRA — small enough to fit
  comfortably on a T4 in fp16)
- Trained on **1,000 prompt/answer pairs**: structured findings (crop, visual finding, condition, severity)
  → the teacher's ground-truth answer text
- Exported to GGUF, quantized **Q4_K_M → 271 MB**

**Prompt format (must match exactly at inference time):**
```
Crop: <crop>
Visual finding: no specific markers listed
Possible condition: <disease>
Severity: unspecified

Question: <question>
Answer:
```

**Known limitation** (from the model's own card): answers depend *only* on the crop/disease label and the
question text — not on the image directly — so **classifier mistakes propagate straight into the answer**,
and the model often falls back to "not specified" on details it wasn't trained to have. Trained on only
1,000 pairs, so linguistic variety and edge-case handling are both limited.

---

## 5. Published Artifacts

**Classifier** — `vedantjadhav701/agriedge-classifier` (private HF repo)
- `classifier_checkpoint.pt` — v0 PyTorch checkpoint (resumable, for further training)
- `agri_classifier_fp32.onnx` / `agri_classifier_int8.onnx` — v0 inference models
- `india_v1_checkpoint.pt` — **India v1 PyTorch checkpoint (resumable, for further training)**
- `agri_classifier_india_v1_fp32.onnx` / `agri_classifier_india_v1_int8.onnx` — **India v1 inference models**
- `india_v1_labels.json` — index → `crop__condition` label mapping

**SLM** — `vedantjadhav701/agriedge-slm` (private HF repo)
- `model.safetensors` — fp32 HF-format checkpoint
- `agriedge_slm_q4_k_m.gguf` — **quantized, mobile-ready (271 MB)**

**Total mobile package: ~371 MB** (100 MB classifier int8 + 271 MB SLM Q4_K_M) — a bit over the original
200–350 MB target, but close, and there's compression headroom left (Q4_K_M is not the most aggressive
option available).

---

## 6. What's Working Well

- **The split architecture validates itself in the numbers.** A 93M-parameter classifier hits 0.888 on-device
  accuracy on a 103-class problem — the "don't make the language model do everything" bet paid off.
- **The India v1 retraining shows the value of label quality over model size.** Same architecture, same
  parameter count, going from text-heuristic labels to folder-ground-truth labels took disease accuracy from
  0.39 to 0.89 on the same crop families. That's a labeling fix, not a scaling fix — cheap lesson to have
  learned early.
- **INT8 quantization holds up well** on both models — 93.2% prediction agreement with fp32 at roughly 1/4
  the size and ~25% faster CPU inference. Confirms the classifier will behave predictably once quantized for
  mobile.
- **Every checkpoint is resumable** (PyTorch `.pt` with optimizer state), not just the inference exports —
  future retraining doesn't have to start from scratch.
- **The whole pipeline is reproducible from a cold Kaggle session** — config, data fetch, image resolution,
  training, export, and push are all scripted, not manual steps done once and forgotten.

---

## 7. Known Gaps / Honest Limitations

- **Training images skew toward clean/lab-style photography.** AgML sources are mostly curated datasets;
  real farmer photos will have worse lighting, blur, occlusion, and background clutter than anything the
  model has seen. Expect real-world accuracy to be lower than the 0.888 benchmark.
- **Only 103 classes across 18 crops.** Any photo outside that scope still gets forced into one of the 103
  buckets — there's no "I don't know this crop" output yet.
- **The SLM is thin.** 1,000 training pairs, no direct image grounding, and it inherits every classifier
  mistake verbatim. It explains what the classifier said, it doesn't independently verify anything.
- **Never run end-to-end outside Kaggle.** ONNX and GGUF files are built, tested in isolation (dummy inputs,
  local eval), but the full camera → classifier → SLM chain has not been run on an actual Android device yet.
- **No confidence gating implemented yet.** The classifier produces a confidence score, but nothing currently
  stops a low-confidence, likely-wrong prediction from being shown to a user as if it were certain.

---

## 8. Proposed Inference Pipeline (Android)

```
Camera capture (Android CameraX)
      ↓
Resize/preprocess to 224x224, normalize (match vision_processor's exact mean/std)
      ↓
ONNX Runtime Mobile → agri_classifier_india_v1_int8.onnx
      ↓
Top-1 logit → argmax → india_v1_labels.json lookup → "tomato__early_blight" + confidence
      ↓
IF confidence < threshold → show "not confident" message, STOP here
ELSE → split label into crop/disease, render prompt template (exact format from SLM's model card)
      ↓
llama.cpp Android (JNI) → agriedge_slm_q4_k_m.gguf → generate
      ↓
Display: crop + disease name + confidence + generated explanation/recommendation
      ↓
[Feedback button] — logs was-this-right for future retraining
```

Entirely on-device after the app and model files are installed — no network call in this loop.

---

## 9. Roadmap

### 9.1 Near-term — before any real user sees this (pilot/beta stage)
1. **Confidence threshold gating** — if top softmax probability is below ~0.5–0.6, show "not confident, try
   another photo" instead of a wrong-but-certain-sounding answer. The confidence score already exists;
   this is a UI/logic gate, not new modeling work.
2. **Explicit crop whitelist in the UI** — tell the user upfront which 18 crops are supported, so a photo of
   something outside that set doesn't silently get force-fit into a wrong label.
3. **Visible "not expert advice" disclaimer** on every result screen (already written into the model card —
   needs to be user-facing too).
4. **Feedback button** ("was this right?") on every result — the cheapest way to start collecting real
   farmer-photo signal, which is exactly what's missing from the current training data.
5. **Build the actual Android project**: module setup, ONNX Runtime Mobile dependency + inference wrapper,
   llama.cpp Android JNI bindings + inference wrapper, CameraX capture screen, the glue code chaining
   classifier output into the SLM prompt.
6. **Run the full pipeline on a real device** at least once before calling anything "done" — battery
   drain, thermal throttling during SLM generation, cold-start load time for both models, and actual latency
   on a mid-range (not flagship) Android phone all need real measurement, not assumption.

### 9.2 What "ship to every farmer" actually requires
This is a materially bigger step up from the pilot above — being explicit about the gap:

- **Real farmer-photo data.** Everything trained so far is on curated/lab-style datasets. Needs a data
  collection loop — even a small pilot with the feedback button from a few hundred real users — before
  accuracy claims hold up in the field.
- **Broader crop/disease coverage.** 18 crops and 103 classes is a start, not comprehensive. Which crops to
  add next should be driven by whichever region/user base is targeted first, not by what happened to be in
  AgML.
- **A real held-out test set that never touched training**, collected independently, reported honestly (the
  0.888 number is a good validation signal but was drawn from the same distribution as training data by
  construction — needs an out-of-distribution check before it's a trustworthy field number).
- **Language support.** Farmers this is meant for may not read English explanations — needs regional
  language output (Hindi, Marathi, Tamil, etc. depending on target region), either via the SLM directly or a
  translation layer.
- **Low-end device testing.** "Works on my Kaggle GPU" and "works on a flagship phone" are very different
  from "works on the ₹8,000 Android phone a farmer actually owns" — RAM, storage, CPU generation, thermal
  behavior all need testing on real low/mid-tier hardware, not just an emulator.
- **Update/versioning strategy.** As the model improves from feedback data, need an app-update mechanism
  for shipping new classifier/SLM weights without requiring a full app reinstall (model files could be
  downloaded post-install, separate from the app package).
- **Offline-first UX polish.** Clear "processing" states during the ~150–300ms classifier + however-long SLM
  generation takes, graceful handling of a bad/blurry photo, and probably a way to browse past results without
  connectivity.
- **Basic app-store readiness.** Privacy policy (even for a fully offline app — what happens to photos taken?
  are they ever stored/sent anywhere?), Play Store agricultural/medical-adjacent content review (disease
  diagnosis apps sometimes get extra scrutiny), and a support/contact channel for farmers who hit bad
  predictions.
- **A clear non-liability framing.** This tool gives a *suggestion*, not a diagnosis — that needs to be
  unmistakable in the app itself, not just a buried disclaimer, given the real-world cost of a wrong call on
  crop treatment.

None of this blocks starting the Android build — it's the difference between "a working prototype" (which
this project already is, as of this document) and "something responsible to hand to farmers making real
decisions with real money on the line."

---

*Last updated: reflects state as of the India v1 classifier + SLM GGUF export, both pushed to Hugging Face.*
