import os
import sys
from huggingface_hub import hf_hub_download

token = sys.argv[1] if len(sys.argv) > 1 else os.environ.get("HF_TOKEN")

if not token:
    print("Usage: python download_models.py <YOUR_HF_TOKEN>")
    print("Or set HF_TOKEN environment variable.")
    sys.exit(1)

output_dir = r"C:\Users\HP\projects\agriedge\models"
os.makedirs(output_dir, exist_ok=True)

print("Downloading AgriEdge classifier models...")
hf_hub_download(
    repo_id="vedantjadhav701/agriedge-classifier",
    filename="agri_classifier_india_v1_int8.onnx",
    local_dir=output_dir,
    token=token
)

hf_hub_download(
    repo_id="vedantjadhav701/agriedge-classifier",
    filename="india_v1_labels.json",
    local_dir=output_dir,
    token=token
)

print("Downloading AgriEdge SLM model...")
hf_hub_download(
    repo_id="vedantjadhav701/agriedge-slm",
    filename="agriedge_slm_q4_k_m.gguf",
    local_dir=output_dir,
    token=token
)

print("\nAll model files downloaded successfully to:")
print(output_dir)
