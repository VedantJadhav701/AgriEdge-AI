import json
from pathlib import Path

knowledge_path = Path("models/agriedge_knowledge_103.json")
target_path_v2 = Path("app-v2/src/main/assets/data/disease_info.json")

with open(knowledge_path, "r", encoding="utf-8") as f:
    knowledge = json.load(f)

existing_map = {}
if target_path_v2.exists():
    with open(target_path_v2, "r", encoding="utf-8") as f:
        try:
            for item in json.load(f):
                existing_map[item["label"]] = item
        except Exception:
            pass

updated_list = []
for k in knowledge:
    lbl = k["label"]
    if lbl in existing_map:
        updated_list.append(existing_map[lbl])
    else:
        cond_name = k["condition"]
        explanation_en = f"{k['symptoms']} {k['explanation']}"
        treatment_en = f"Organic: {k['organic_remedy']} Chemical: {k['chemical_treatment']} Prevention: {k['preventative_measures']}"
        
        entry = {
            "label": lbl,
            "name": {
                "en": cond_name,
                "mr": cond_name,
                "hi": cond_name
            },
            "explanation": {
                "en": explanation_en,
                "mr": explanation_en,
                "hi": explanation_en
            },
            "treatment": {
                "en": treatment_en,
                "mr": treatment_en,
                "hi": treatment_en
            }
        }
        updated_list.append(entry)

with open(target_path_v2, "w", encoding="utf-8") as f:
    json.dump(updated_list, f, indent=2, ensure_ascii=False)

print(f"Successfully synced {len(updated_list)} classes in {target_path_v2}")
