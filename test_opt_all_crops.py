import onnxruntime as ort
import json
import numpy as np
from PIL import Image, ImageDraw

opt_model_path = r"C:\Users\HP\projects\agriedge\models\agri_classifier_india_v1_int8_opt.onnx"
labels_path = r"C:\Users\HP\projects\agriedge\models\india_v1_labels.json"

with open(labels_path, 'r') as f:
    labels = json.load(f)

session = ort.InferenceSession(opt_model_path)
in_name = session.get_inputs()[0].name
out_name = session.get_outputs()[0].name

print(f"Loaded {opt_model_path}")
print("Inputs:", [i.name for i in session.get_inputs()])
print("Outputs:", [o.name for o in session.get_outputs()])

def create_papaya():
    img = Image.new('RGB', (224, 224), (240, 240, 240))
    draw = ImageDraw.Draw(img)
    draw.polygon([(112, 10), (170, 60), (210, 112), (160, 160), (112, 210), (64, 160), (14, 112), (54, 60)], fill=(130, 200, 30))
    draw.ellipse([90, 70, 134, 114], outline=(220, 120, 0), width=5)
    return img

def create_banana():
    img = Image.new('RGB', (224, 224), (245, 245, 245))
    draw = ImageDraw.Draw(img)
    draw.ellipse([20, 30, 204, 194], fill=(40, 170, 45))
    draw.line([(112, 30), (112, 194)], fill=(200, 230, 60), width=8)
    return img

def create_rice():
    img = Image.new('RGB', (224, 224), (255, 255, 255))
    draw = ImageDraw.Draw(img)
    draw.polygon([(90, 0), (134, 0), (140, 224), (84, 224)], fill=(34, 139, 34))
    draw.ellipse([98, 40, 126, 90], fill=(139, 69, 19))
    return img

samples = [("Papaya", create_papaya()), ("Banana", create_banana()), ("Rice", create_rice())]

for name, img in samples:
    print(f"\n==================== {name} ====================")
    arr = (np.array(img).astype(np.float32) / 255.0 - 0.5) / 0.5
    tensor_in = np.transpose(arr, (2, 0, 1))[np.newaxis, :, :, :].astype(np.float32)
    
    # Run
    logits = session.run([out_name], {in_name: tensor_in})[0][0]
    top = np.argsort(logits)[::-1][:5]
    print(f"Top predictions for {name}:")
    for idx in top:
        print(f"  [{idx}] {labels[idx]}: logit={logits[idx]:.4f}")
