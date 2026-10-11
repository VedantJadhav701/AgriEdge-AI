# Export the Qwen model to GGUF for the phone

The app loads `agriedge_qwen05b_q4_k_m.gguf` from the device models folder.

## 1. Build llama.cpp tools (once)

```bash
git clone https://github.com/ggerganov/llama.cpp
cd llama.cpp
pip install -r requirements.txt
cmake -B build && cmake --build build --config Release -j
```

## 2. Convert the merged model to GGUF (F16)

```bash
python convert_hf_to_gguf.py ../training/out/agriedge-qwen05b-merged \
  --outfile ../training/out/agriedge_qwen05b_f16.gguf --outtype f16
```

## 3. Quantise to Q4_K_M

```bash
./build/bin/llama-quantize ../training/out/agriedge_qwen05b_f16.gguf \
  ../training/out/agriedge_qwen05b_q4_k_m.gguf Q4_K_M
```

## 4. Test on the PC before pushing to the phone

```bash
./build/bin/llama-cli -m ../training/out/agriedge_qwen05b_q4_k_m.gguf \
  -p "केळीला पाणी कधी द्यावे?" -n 200
```

Check answers in English, Marathi and Hindi. The model should reply in the language asked.

## 5. Push to the phone

Copy the file to `models/` and run `push_to_device.ps1`. The script pushes it as
`agriedge_qwen05b_q4_k_m.gguf`, the name `ModelManager` looks for.

## Notes

- Qwen2.5 uses the ChatML template (`<|im_start|>`). The app prompts already use ChatML, so no template change is needed.
- If the llama.cpp version on the phone is older, Qwen2 support may be missing. Rebuild the `.so` from the same llama.cpp checkout you use for the PC tests.
- Keep the Q4_K_M file; it is roughly 0.4 GB, which fits a mid-range phone.
