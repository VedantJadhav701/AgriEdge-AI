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

# Create synthetic Rice Leaf Blast image (long green leaf blade with reddish-brown diamond lesions)
img_rice = Image.new('RGB', (224, 224), (240, 240, 240)) # light background
draw = ImageDraw.Draw(img_rice)
# Draw vertical rice leaf blade
draw.polygon([(90, 10), (134, 10), (140, 214), (84, 214)], fill=(34, 139, 34)) # forest green
# Draw spindle / diamond blast lesions
draw.ellipse([100, 50, 124, 80], fill=(139, 69, 19)) # brown lesion
draw.ellipse([105, 55, 119, 75], fill=(128, 128, 128)) # ash center
draw.ellipse([96, 120, 120, 150], fill=(139, 69, 19))
draw.ellipse([101, 125, 115, 145], fill=(128, 128, 128))

# Preprocess image to tensor
arr = np.array(img_rice).astype(np.float32) / 255.0
arr_norm = (arr - 0.5) / 0.5
tensor_input = np.transpose(arr_norm, (2, 0, 1))[np.newaxis, :, :, :]

# Run ONNX inference
logits = session.run([out_name], {in_name: tensor_input})[0][0]

# Raw softmax
exp_l = np.exp(logits - np.max(logits))
probs = exp_l / exp_l.sum()

top = np.argsort(probs)[::-1][:5]
print("--- Synthetic Rice Leaf Blast Prediction (Raw) ---")
for idx in top:
    print(f"  {labels[idx]}: prob={probs[idx]*100:.2f}%, logit={logits[idx]:.4f}")
