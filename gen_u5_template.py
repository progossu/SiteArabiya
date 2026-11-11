import json

with open("paragraphs_words_tokens.json", "r", encoding="utf-8") as f:
    tokens_data = json.load(f)

with open("data_units.json", "r", encoding="utf-8") as f:
    units_data = json.load(f)

for u_id in ["5"]:
    for p_id in sorted(tokens_data[u_id].keys(), key=lambda x: int(x)):
        p_tokens = tokens_data[u_id][p_id]
        print(f"\n==================== UNIT {u_id} PARAGRAPH {p_id} ({len(p_tokens)} tokens) ====================")
        for t in p_tokens:
            print(f'"{u_id}_{p_id}_{t["idx"]}": ("{t["word"]}", ""),')
