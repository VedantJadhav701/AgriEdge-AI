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

# Create synthetic Rice Blast Leaf image (green leaf blade + diamond lesions)
img_rice = Image.new('RGB', (224, 224), (255, 255, 255))
draw = ImageDraw.Draw(img_rice)
draw.polygon([(90, 0), (134, 0), (140, 224), (84, 224)], fill=(34, 139, 34)) # green blade
draw.ellipse([98, 40, 126, 90], fill=(139, 69, 19)) # diamond brown blast spot
draw.ellipse([104, 52, 120, 78], fill=(180, 180, 180)) # ash center

arr_raw = np.array(img_rice).astype(np.float32) # Range [0, 255]
tensor_raw = np.transpose(arr_raw, (2, 0, 1))[np.newaxis, :, :, :]

logits = session.run([out_name], {in_name: tensor_raw})[0][0]

exp_l = np.exp(logits - np.max(logits))
probs = exp_l / exp_l.sum()

top = np.argsort(probs)[::-1][:5]
print("--- Rice Leaf Image (Raw [0, 255] range) ---")
for idx in top:
    print(f"  {labels[idx]}: prob={probs[idx]*100:.2f}%, logit={logits[idx]:.4f}")
