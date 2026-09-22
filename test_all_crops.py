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

def create_synthetic_papaya():
    img = Image.new('RGB', (224, 224), (250, 250, 250))
    draw = ImageDraw.Draw(img)
    draw.polygon([(112, 20), (160, 70), (200, 112), (150, 150), (112, 200), (74, 150), (24, 112), (64, 70)], fill=(120, 190, 32))
    draw.ellipse([90, 80, 130, 120], outline=(200, 100, 0), width=4)
    draw.ellipse([50, 90, 80, 120], outline=(200, 100, 0), width=3)
    return img

def create_synthetic_banana():
    img = Image.new('RGB', (224, 224), (245, 245, 245))
    draw = ImageDraw.Draw(img)
    draw.ellipse([30, 20, 194, 204], fill=(34, 160, 40))
    draw.line([(112, 20), (112, 204)], fill=(180, 220, 50), width=6)
    draw.rectangle([50, 60, 90, 70], fill=(60, 30, 10))
    draw.rectangle([130, 120, 170, 130], fill=(60, 30, 10))
    return img

def create_synthetic_rice():
    img = Image.new('RGB', (224, 224), (255, 255, 255))
    draw = ImageDraw.Draw(img)
    draw.polygon([(90, 0), (134, 0), (140, 224), (84, 224)], fill=(34, 139, 34))
    draw.ellipse([98, 40, 126, 90], fill=(139, 69, 19))
    return img

test_cases = [
    ("Papaya", create_synthetic_papaya()),
    ("Banana", create_synthetic_banana()),
    ("Rice", create_synthetic_rice())
]

preprocessors = {
    "A: [0, 1] range": lambda img: (np.array(img).astype(np.float32) / 255.0),
    "B: [-1, 1] range": lambda img: ((np.array(img).astype(np.float32) / 255.0 - 0.5) / 0.5),
    "C: ImageNet": lambda img: ((np.array(img).astype(np.float32)/255.0 - np.array([0.485,0.456,0.406], dtype=np.float32)) / np.array([0.229,0.224,0.225], dtype=np.float32)),
    "D: [0, 255] raw range": lambda img: np.array(img).astype(np.float32)
}

for crop_name, img in test_cases:
    print(f"\n==================== TEST CROP: {crop_name} ====================")
    for prep_name, prep_fn in preprocessors.items():
        arr = prep_fn(img)
        tensor_in = np.transpose(arr, (2, 0, 1))[np.newaxis, :, :, :].astype(np.float32)
        logits = session.run([out_name], {in_name: tensor_in})[0][0]
        top = np.argsort(logits)[::-1][:3]
        print(f"\n  Scheme {prep_name}:")
        for idx in top:
            print(f"    - {labels[idx]} (idx {idx}): logit={logits[idx]:.4f}")
