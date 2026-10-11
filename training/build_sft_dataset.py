"""
Build the multilingual SFT dataset (English, Marathi, Hindi) for the AgriEdge SLM.

Base model: Qwen/Qwen2.5-0.5B-Instruct (ChatML template, same as the app prompts).

Sources:
  1. training/data/qa_seed.jsonl                       hand-written Q&A (all three languages)
  2. app/src/main/assets/data/disease_info.json        verified condition explanations and treatments
  3. app/src/main/assets/data/maharashtra_districts.json  district crop suitability (SEED until verified)
  4. app/src/main/assets/data/market_prices.json       mandi prices (only if synced; never invented)

Output: training/out/sft_train.jsonl  (one {"messages": [...]} per line)

Run:  python build_sft_dataset.py
"""
import json
import random
import re
from collections import Counter
from pathlib import Path

HERE = Path(__file__).resolve().parent
ASSETS = HERE.parent / "app" / "src" / "main" / "assets" / "data"
OUT = HERE / "out"
OUT.mkdir(exist_ok=True)

LANGS = ("en", "mr", "hi")

LANG_NAME = {"en": "English", "mr": "Marathi", "hi": "Hindi"}

# Same system-prompt form the app sends at runtime, so training matches inference.
SYSTEM = {
    code: (f"You are AgriEdge AI, an offline farm advisor for Maharashtra. Reply only in {name}. "
           "Be brief, practical and calm. Do not invent chemical doses; refer to the product label or the local KVK.")
    for code, name in LANG_NAME.items()
}

# User question templates. {crop} and {cond} are localised names.
DIAG_Q = {
    "en": [
        "My {crop} leaf shows {cond}. What should I do?",
        "The app says my {crop} has {cond}. How do I treat it?",
        "My {crop} leaves have signs of {cond}. Please advise.",
        "What is {cond} in {crop} and how do I control it?",
    ],
    "mr": [
        "माझ्या {crop} पानावर {cond} दिसत आहे. मी काय करावे?",
        "अॅप म्हणते माझ्या {crop} ला {cond} आहे. उपचार कसा करू?",
        "माझ्या {crop} च्या पानांवर {cond} ची लक्षणे आहेत. मार्गदर्शन करा.",
        "{crop} मधील {cond} म्हणजे काय आणि ते कसे नियंत्रित करायचे?",
    ],
    "hi": [
        "मेरी {crop} की पत्ती पर {cond} दिख रहा है। मुझे क्या करना चाहिए?",
        "ऐप कह रहा है मेरी {crop} में {cond} है। इलाज कैसे करूँ?",
        "मेरी {crop} की पत्तियों पर {cond} के लक्षण हैं। सलाह दें।",
        "{crop} में {cond} क्या है और इसे कैसे नियंत्रित करें?",
    ],
}

HEALTHY_Q = {
    "en": "Is my {crop} leaf healthy?",
    "mr": "माझे {crop} पान निरोगी आहे का?",
    "hi": "क्या मेरी {crop} की पत्ती स्वस्थ है?",
}

CROP_NAME = {
    "banana": {"en": "banana", "mr": "केळी", "hi": "केला"},
    "papaya": {"en": "papaya", "mr": "पपई", "hi": "पपीता"},
}

DIST_Q = {
    "en": ["Which crops suit {d} district?", "What should I grow in {d}?"],
    "mr": ["{d} जिल्ह्यासाठी कोणती पिके योग्य आहेत?", "{d} मध्ये मी कोणते पीक घ्यावे?"],
    "hi": ["{d} ज़िले के लिए कौन सी फसलें ठीक हैं?", "{d} में मुझे कौन सी फसल लगानी चाहिए?"],
}

DIST_A = {
    "en": "In {d}, banana and papaya are candidate crops: {crops}. Season: {season}. Confirm with your KVK before planting.",
    "mr": "{d} मध्ये केळी आणि पपई ही संभाव्य पिके आहेत: {crops}. हंगाम: {season}. लागवडीपूर्वी KVK कडून खात्री करा.",
    "hi": "{d} में केला और पपीता संभावित फसलें हैं: {crops}। मौसम: {season}। बोने से पहले KVK से पुष्टि करें।",
}

MARKET_Q = {
    "en": "What is the {crop} price in {d}?",
    "mr": "{d} मध्ये {crop} चा भाव किती आहे?",
    "hi": "{d} में {crop} का भाव क्या है?",
}

MARKET_A = {
    "en": "{crop} in {d} ({market}), {date}: modal price Rs {modal} per quintal (min Rs {min}, max Rs {max}). Prices change daily; check the latest mandi rate.",
    "mr": "{d} ({market}), {date} येथे {crop} चा सर्वसाधारण भाव रु. {modal} प्रति क्विंटल (किमान रु. {min}, कमाल रु. {max}). भाव रोज बदलतात; ताजा मंडी भाव तपासा.",
    "hi": "{d} ({market}), {date} में {crop} का सामान्य भाव ₹{modal} प्रति क्विंटल (न्यूनतम ₹{min}, अधिकतम ₹{max})। भाव रोज़ बदलते हैं; ताज़ा मंडी भाव देखें।",
}


def chat(lang, user, assistant):
    return {
        "messages": [
            {"role": "system", "content": SYSTEM[lang]},
            {"role": "user", "content": user},
            {"role": "assistant", "content": assistant},
        ],
        "lang": lang,
    }


def load_json(path, default):
    return json.loads(path.read_text(encoding="utf-8")) if path.exists() else default


def disease_rows(diseases):
    rows = []
    for d in diseases:
        crop = d["label"].split("__")[0]
        if crop not in CROP_NAME:
            continue
        for lang in LANGS:
            cn = CROP_NAME[crop][lang]
            answer = f'{d["explanation"][lang]}\n\n{d["treatment"][lang]}'
            if d["label"].endswith("__healthy"):
                user = HEALTHY_Q[lang].format(crop=cn)
                rows.append(chat(lang, user, answer))
                continue
            name = d["name"].get(lang, d["name"]["en"])
            for tpl in DIAG_Q[lang]:
                rows.append(chat(lang, tpl.format(crop=cn, cond=name), answer))
    return rows


def district_rows(districts):
    rows = []
    for d in districts:
        for lang in LANGS:
            dn = d["name"] if lang == "en" else d.get(f"name_{lang}", d["name"])
            crops = ", ".join(CROP_NAME[c][lang] for c in d["crops"] if c in CROP_NAME)
            if not crops:
                continue
            for tpl in DIST_Q[lang]:
                rows.append(chat(lang, tpl.format(d=dn), DIST_A[lang].format(
                    d=dn, crops=crops, season=d.get("season", ""))))
    return rows


def market_rows(market):
    rows = []
    for p in market.get("prices", []):
        if p.get("modal_rs", 0) <= 0 or p.get("crop") not in CROP_NAME:
            continue
        for lang in LANGS:
            vals = dict(
                d=p["district"], market=p.get("market", ""), date=p.get("date", ""),
                crop=CROP_NAME[p["crop"]][lang], modal=p["modal_rs"],
                min=p.get("min_rs", 0), max=p.get("max_rs", 0),
            )
            rows.append(chat(lang, MARKET_Q[lang].format(crop=vals["crop"], d=vals["d"]),
                             MARKET_A[lang].format(**vals)))
    return rows


def seed_rows():
    path = HERE / "data" / "qa_seed.jsonl"
    rows = []
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        msgs = json.loads(line)["messages"]
        m = re.search(r"Reply only in (English|Marathi|Hindi)", msgs[0]["content"])
        lang = {"English": "en", "Marathi": "mr", "Hindi": "hi"}[m.group(1)] if m else "en"
        rows.append({"messages": msgs, "lang": lang})
    return rows


def main():
    diseases = load_json(ASSETS / "disease_info.json", [])
    districts = load_json(ASSETS / "maharashtra_districts.json", [])
    market = load_json(ASSETS / "market_prices.json", {"prices": []})

    rows = disease_rows(diseases) + district_rows(districts) + market_rows(market) + seed_rows()

    # Basic validation: no empty answers, each row has system/user/assistant.
    clean = []
    for r in rows:
        m = r["messages"]
        if len(m) == 3 and all(x["content"].strip() for x in m):
            clean.append(r)
    random.seed(7)
    random.shuffle(clean)

    out = OUT / "sft_train.jsonl"
    with out.open("w", encoding="utf-8") as f:
        for r in clean:
            f.write(json.dumps({"messages": r["messages"]}, ensure_ascii=False) + "\n")

    counts = Counter(r["lang"] for r in clean)
    print(f"Wrote {len(clean)} examples -> {out}")
    print("By language:", dict(counts))
    if any(d.get("status", "").startswith("SEED") for d in districts):
        print("WARNING: district data is SEED. Verify with KVKs before training on it.")
    if not market.get("prices"):
        print("NOTE: no market prices synced; run fetch_market_prices.py to add them.")


if __name__ == "__main__":
    main()
