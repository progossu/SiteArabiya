import json

with open("paragraphs_words_tokens.json", "r", encoding="utf-8") as f:
    tokens = json.load(f)

for p_id in ["4", "5", "6"]:
    toks = tokens["3"][p_id]
    print(f"\n--- Unit 3 Paragraph {p_id} ({len(toks)} tokens) ---")
    print(" ".join([f"[{t['idx']}]{t['word']}" for t in toks]))

for p_id in ["1", "2", "3", "4", "5"]:
    toks = tokens["4"][p_id]
    print(f"\n--- Unit 4 Paragraph {p_id} ({len(toks)} tokens) ---")
    print(" ".join([f"[{t['idx']}]{t['word']}" for t in toks]))
