import onnx
from onnx import helper, numpy_helper
import numpy as np

model_path = r"C:\Users\HP\projects\agriedge\models\agri_classifier_india_v1_int8.onnx"
output_path = r"C:\Users\HP\projects\agriedge\models\agri_classifier_india_v1_int8_mobile.onnx"

print(f"Loading ONNX model from {model_path}...")
model = onnx.load(model_path)
graph = model.graph

# Locate initializers and nodes
initializer_map = {init.name: init for init in graph.initializer}
node_map = {node.name: node for node in graph.node}

target_node = None
for node in graph.node:
    if node.op_type == "ConvInteger":
        target_node = node
        break

if target_node:
    print(f"Found target ConvInteger node: {target_node.name}")
    input_x = target_node.input[0]
    input_w = target_node.input[1]
    input_x_zero_point = target_node.input[2] if len(target_node.input) > 2 else None
    input_w_zero_point = target_node.input[3] if len(target_node.input) > 3 else None
    output_y = target_node.output[0]

    # Convert weights to float32
    w_init = initializer_map.get(input_w)
    if w_init:
        w_data = numpy_helper.to_array(w_init).astype(np.float32)
        w_zero_point = 0
        if input_w_zero_point and input_w_zero_point in initializer_map:
            w_zero_point = numpy_helper.to_array(initializer_map[input_w_zero_point])
        w_data = w_data - w_zero_point
        
        float_w_name = input_w + "_float32"
        float_w_init = numpy_helper.from_array(w_data, name=float_w_name)
        graph.initializer.append(float_w_init)

        # Create standard Conv node
        conv_node = helper.make_node(
            "Conv",
            inputs=[input_x, float_w_name],
            outputs=[output_y],
            name=target_node.name + "_fp32_conv",
            domain=target_node.domain
        )
        
        # Copy attributes (kernel_shape, strides, pads, etc.)
        for attr in target_node.attribute:
            conv_node.attribute.append(attr)

        # Replace node in graph
        idx = list(graph.node).index(target_node)
        graph.node.remove(target_node)
        graph.node.insert(idx, conv_node)
        print("Replaced ConvInteger with standard Conv node!")

print(f"Saving mobile-compatible ONNX model to {output_path}...")
onnx.save(model, output_path)

# Also overwrite the active agri_classifier_india_v1_int8.onnx
onnx.save(model, model_path)
print("Updated agri_classifier_india_v1_int8.onnx successfully!")
