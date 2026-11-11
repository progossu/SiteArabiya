# -*- coding: utf-8 -*-
import json
from generate_glossary_u7_u8 import get_u7_u8_translations

with open("paragraphs_words_tokens.json", "r", encoding="utf-8") as f:
    tokens_data = json.load(f)

translations = get_u7_u8_translations()

missing = []
empty = []
mismatches = []

for u_id in ["7", "8"]:
    for p_id in sorted(tokens_data[u_id].keys(), key=int):
        toks = tokens_data[u_id][p_id]
        for t in toks:
            key = f"{u_id}_{p_id}_{t['idx']}"
            if key not in translations:
                missing.append((key, t['word']))
            elif not translations[key] or not translations[key].strip():
                empty.append((key, t['word']))

print(f"Total keys checked: {len(translations)}")
print(f"Missing keys: {len(missing)}")
print(f"Empty translations: {len(empty)}")

if missing:
    print("Sample missing:", missing[:10])
if empty:
    print("Sample empty:", empty[:10])
