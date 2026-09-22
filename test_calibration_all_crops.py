import onnxruntime as ort
import json
import numpy as np
from PIL import Image, ImageDraw

m_path = r"C:\Users\HP\projects\agriedge\models\agri_classifier_india_v1_int8.onnx"
labels_path = r"C:\Users\HP\projects\agriedge\models\india_v1_labels.json"

with open(labels_path, 'r') as f:
    labels = json.load(f)

session = ort.InferenceSession(m_path)
in_name = session.get_inputs()[0].name
out_name = session.get_outputs()[0].name

# Compute baseline bias vector over 50 random noise samples
baseline_logits = np.zeros(103, dtype=np.float32)
for _ in range(50):
    dummy = np.random.rand(1, 3, 224, 224).astype(np.float32)
    l = session.run([out_name], {in_name: dummy})[0][0]
    baseline_logits += l
baseline_logits /= 50.0

def create_papaya_curled():
    img = Image.new('RGB', (224, 224), (230, 230, 230))
    draw = ImageDraw.Draw(img)
    # Papaya leaf with leaf curl symptoms
    draw.polygon([(112, 10), (180, 50), (210, 112), (170, 180), (112, 214), (54, 180), (14, 112), (44, 50)], fill=(100, 180, 20))
    draw.arc([30, 30, 194, 194], start=0, end=360, fill=(200, 150, 0), width=6)
    return img

def create_banana_sigatoka():
    img = Image.new('RGB', (224, 224), (240, 240, 240))
    draw = ImageDraw.Draw(img)
    draw.ellipse([20, 20, 204, 204], fill=(30, 150, 30))
    draw.line([(112, 20), (112, 204)], fill=(210, 220, 50), width=6)
    # Sigatoka dark brown lesions
    draw.rectangle([40, 50, 100, 75], fill=(50, 20, 10))
    draw.rectangle([120, 130, 180, 155], fill=(50, 20, 10))
    return img

def create_rice_blast():
    img = Image.new('RGB', (224, 224), (255, 255, 255))
    draw = ImageDraw.Draw(img)
    draw.polygon([(90, 0), (134, 0), (140, 224), (84, 224)], fill=(34, 139, 34))
    draw.ellipse([98, 40, 126, 90], fill=(139, 69, 19))
    return img

samples = [
    ("Papaya (Leaf Curl)", create_papaya_curled()),
    ("Banana (Sigatoka)", create_banana_sigatoka()),
    ("Rice (Leaf Blast)", create_rice_blast())
]

print("=== CALIBRATED PREDICTIONS WITH BASELINE SUBTRACTION ===")
for name, img in samples:
    arr = (np.array(img).astype(np.float32) / 255.0 - 0.5) / 0.5
    tensor_in = np.transpose(arr, (2, 0, 1))[np.newaxis, :, :, :].astype(np.float32)
    raw_logits = session.run([out_name], {in_name: tensor_in})[0][0]
    
    # CALIBRATE LOGITS: subtract baseline_logits
    calibrated = raw_logits - baseline_logits
    
    exp_c = np.exp(calibrated - np.max(calibrated))
    probs = exp_c / exp_c.sum()
    
    top = np.argsort(probs)[::-1][:4]
    print(f"\nSample: {name}")
    for idx in top:
        print(f"  - {labels[idx]} (idx {idx}): prob={probs[idx]*100:.2f}%, cal_logit={calibrated[idx]:.4f}")
