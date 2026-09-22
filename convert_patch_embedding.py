import onnx
from onnx import helper, numpy_helper
import numpy as np

model_path = r"C:\Users\HP\projects\agriedge\models\agri_classifier_india_v1_int8_opt.onnx"
output_path = r"C:\Users\HP\projects\agriedge\models\agri_classifier_india_v1_int8_opt.onnx"

print("Loading ONNX classifier model...")
model = onnx.load(model_path)
graph = model.graph

init_map = {init.name: init for init in graph.initializer}

conv_int_node = None
for node in graph.node:
    if node.op_type == "ConvInteger":
        conv_int_node = node
        break

if conv_int_node:
    print(f"Found ConvInteger node: {conv_int_node.name}")
    
    w_name = conv_int_node.input[1]
    w_init = init_map[w_name]
    w_int8 = numpy_helper.to_array(w_init)
    
    w_zp = 0
    if len(conv_int_node.input) > 3 and conv_int_node.input[3] in init_map:
        w_zp = numpy_helper.to_array(init_map[conv_int_node.input[3]])
        
    scale = 1.0
    for node in graph.node:
        if conv_int_node.output[0] in node.input and (node.op_type == "Mul" or node.op_type == "Cast" or node.op_type == "DequantizeLinear"):
            for inp in node.input:
                if inp in init_map:
                    arr = numpy_helper.to_array(init_map[inp])
                    if arr.size == 1 or arr.shape == w_int8.shape[:1]:
                        scale = arr
                        break

    w_float32 = (w_int8.astype(np.float32) - w_zp) * scale
    
    float_w_name = w_name + "_fp32"
    float_w_init = numpy_helper.from_array(w_float32.astype(np.float32), name=float_w_name)
    graph.initializer.append(float_w_init)
    
    conv_node = helper.make_node(
        "Conv",
        inputs=["pixel_values", float_w_name],
        outputs=conv_int_node.output,
        name=conv_int_node.name + "_std_conv"
    )
    for attr in conv_int_node.attribute:
        conv_node.attribute.append(attr)

    idx = list(graph.node).index(conv_int_node)
    graph.node.remove(conv_int_node)
    graph.node.insert(idx, conv_node)
    
    for node in list(graph.node):
        if "pixel_values" in node.input and node.op_type == "DynamicQuantizeLinear":
            graph.node.remove(node)
            print(f"Removed unused node: {node.name}")

    print("Successfully converted patch_embedding to standard Conv node!")

onnx.save(model, output_path)
print(f"Saved optimized mobile ONNX model to {output_path}")
