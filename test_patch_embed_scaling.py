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

# Create synthetic Rice Leaf Blast image
img_rice = Image.new('RGB', (224, 224), (240, 240, 240))
draw = ImageDraw.Draw(img_rice)
draw.polygon([(90, 10), (134, 10), (140, 214), (84, 214)], fill=(34, 139, 34))
draw.ellipse([100, 50, 124, 80], fill=(139, 69, 19))
draw.ellipse([105, 55, 119, 75], fill=(128, 128, 128))

arr = np.array(img_rice).astype(np.float32)

print("Testing scale factors for input array:")
for scale in [1.0, 1.0/255.0, 255.0, 1.0/127.5, 127.5]:
    test_input = np.transpose(arr * scale, (2, 0, 1))[np.newaxis, :, :, :]
    logits = session.run([out_name], {in_name: test_input})[0][0]
    top = np.argsort(logits)[::-1][:3]
    print(f"\nScale factor {scale}:")
    for idx in top:
        print(f"  {labels[idx]}: logit={logits[idx]:.4f}")
