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

print(f"Testing model: {m_path}")

# Test 1: Papaya image
img_papaya = Image.new('RGB', (224, 224), (240, 240, 240))
draw1 = ImageDraw.Draw(img_papaya)
draw1.polygon([(112, 10), (170, 60), (210, 112), (160, 160), (112, 210), (64, 160), (14, 112), (54, 60)], fill=(130, 200, 30))
draw1.ellipse([90, 70, 134, 114], outline=(220, 120, 0), width=5) # ringspot

# Test 2: Banana image
img_banana = Image.new('RGB', (224, 224), (245, 245, 245))
draw2 = ImageDraw.Draw(img_banana)
draw2.ellipse([20, 30, 204, 194], fill=(40, 170, 45))
draw2.line([(112, 30), (112, 194)], fill=(200, 230, 60), width=8)

# Test 3: Rice image
img_rice = Image.new('RGB', (224, 224), (255, 255, 255))
draw3 = ImageDraw.Draw(img_rice)
draw3.polygon([(90, 0), (134, 0), (140, 224), (84, 224)], fill=(34, 139, 34))
draw3.ellipse([98, 40, 126, 90], fill=(139, 69, 19))

samples = [("Papaya Leaf", img_papaya), ("Banana Leaf", img_banana), ("Rice Leaf", img_rice)]

norm_schemes = {
    "SigLIP ((x/255.0 - 0.5) / 0.5)": lambda img: ((np.array(img).astype(np.float32) / 255.0 - 0.5) / 0.5),
    "ImageNet ((x/255.0 - mean) / std)": lambda img: ((np.array(img).astype(np.float32)/255.0 - np.array([0.485,0.456,0.406], dtype=np.float32)) / np.array([0.229,0.224,0.225], dtype=np.float32)),
    "Scale 0..1 (x/255.0)": lambda img: (np.array(img).astype(np.float32) / 255.0)
}

for name, img in samples:
    print(f"\n==================== {name} ====================")
    for s_name, s_fn in norm_schemes.items():
        arr = s_fn(img)
        tensor_in = np.transpose(arr, (2, 0, 1))[np.newaxis, :, :, :].astype(np.float32)
        logits = session.run([out_name], {in_name: tensor_in})[0][0]
        top = np.argsort(logits)[::-1][:3]
        print(f"\n  Scheme: {s_name}")
        for idx in top:
            print(f"    - {labels[idx]} (idx {idx}): logit={logits[idx]:.4f}")
