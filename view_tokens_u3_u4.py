import json

with open("paragraphs_words_tokens.json", "r", encoding="utf-8") as f:
    tokens = json.load(f)

with open("data_units.json", "r", encoding="utf-8") as f:
    units = json.load(f)

for u_id in ["3", "4"]:
    print(f"\n==================== UNIT {u_id} ====================")
    for p_id in sorted(tokens[u_id].keys(), key=lambda x: int(x)):
        toks = tokens[u_id][p_id]
        print(f"\n--- Unit {u_id} Paragraph {p_id} ({len(toks)} tokens) ---")
        tok_str = " ".join([f"[{t['idx']}]{t['word']}" for t in toks])
        print(tok_str)
