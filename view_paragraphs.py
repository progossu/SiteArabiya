import json

with open("paragraphs_words_tokens.json", "r", encoding="utf-8") as f:
    tokens_data = json.load(f)

with open("data_units.json", "r", encoding="utf-8") as f:
    units_data = json.load(f)

for u_id in ["5", "6"]:
    for p in units_data[u_id]["paragraphs"]:
        p_id = str(p["id"])
        print("="*60)
        print(f"UNIT {u_id} - PARAGRAPH {p_id}: {p.get('title_ar')} ({p.get('title_ru')})")
        print("TEXT:")
        print(p.get("text"))
        print("\nTOKENS:")
        p_tokens = tokens_data[u_id][p_id]
        for t in p_tokens:
            print(f"  {u_id}_{p_id}_{t['idx']}: {t['word']}")
