"""
LoRA SFT for the AgriEdge SLM, base model Qwen/Qwen2.5-0.5B-Instruct.

Install (GPU machine, e.g. Brev A100 or a 16 GB+ GPU):
  pip install -U "transformers>=4.46" "trl>=0.12" peft datasets accelerate

Run:
  python build_sft_dataset.py
  python train_lora.py

Outputs:
  out/agriedge-qwen05b-lora/        LoRA adapter
  out/agriedge-qwen05b-merged/      merged full model (input to GGUF export)
"""
import argparse

import torch
from datasets import load_dataset
from peft import LoraConfig, PeftModel
from transformers import AutoModelForCausalLM, AutoTokenizer
from trl import SFTConfig, SFTTrainer

BASE = "Qwen/Qwen2.5-0.5B-Instruct"


def train(args):
    tok = AutoTokenizer.from_pretrained(BASE)
    if tok.pad_token is None:
        tok.pad_token = tok.eos_token

    model = AutoModelForCausalLM.from_pretrained(
        BASE,
        torch_dtype=torch.bfloat16,
        attn_implementation="sdpa",
    )

    ds = load_dataset("json", data_files=args.data, split="train")
    split = ds.train_test_split(test_size=0.05, seed=7)

    peft_cfg = LoraConfig(
        r=args.rank,
        lora_alpha=args.rank * 2,
        lora_dropout=0.05,
        bias="none",
        task_type="CAUSAL_LM",
        target_modules=["q_proj", "k_proj", "v_proj", "o_proj", "gate_proj", "up_proj", "down_proj"],
    )

    cfg = SFTConfig(
        output_dir=f"{args.out}/checkpoints",
        num_train_epochs=args.epochs,
        learning_rate=args.lr,
        per_device_train_batch_size=8,
        gradient_accumulation_steps=2,
        warmup_ratio=0.05,
        lr_scheduler_type="cosine",
        logging_steps=10,
        eval_strategy="epoch",
        save_strategy="epoch",
        load_best_model_at_end=True,
        metric_for_best_model="eval_loss",
        greater_is_better=False,
        bf16=True,
        max_length=1024,
        packing=False,
        report_to="none",
        seed=7,
    )

    trainer = SFTTrainer(
        model=model,
        args=cfg,
        train_dataset=split["train"],
        eval_dataset=split["test"],
        processing_class=tok,
        peft_config=peft_cfg,
    )
    trainer.train()
    trainer.save_model(f"{args.out}/agriedge-qwen05b-lora")
    tok.save_pretrained(f"{args.out}/agriedge-qwen05b-lora")
    print("LoRA adapter saved.")


def merge(args):
    """Merge the adapter into the base model so llama.cpp can convert it to GGUF."""
    base = AutoModelForCausalLM.from_pretrained(BASE, torch_dtype=torch.float16)
    tok = AutoTokenizer.from_pretrained(BASE)
    merged = PeftModel.from_pretrained(base, f"{args.out}/agriedge-qwen05b-lora").merge_and_unload()
    merged.save_pretrained(f"{args.out}/agriedge-qwen05b-merged", safe_serialization=True)
    tok.save_pretrained(f"{args.out}/agriedge-qwen05b-merged")
    print("Merged model saved.")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--data", default="out/sft_train.jsonl")
    ap.add_argument("--out", default="out")
    ap.add_argument("--epochs", type=float, default=3)
    ap.add_argument("--lr", type=float, default=2e-4)
    ap.add_argument("--rank", type=int, default=16)
    ap.add_argument("--merge-only", action="store_true")
    args = ap.parse_args()

    if not args.merge_only:
        train(args)
    merge(args)


if __name__ == "__main__":
    main()
