import json

with open("paragraphs_words_tokens.json", "r", encoding="utf-8") as f:
    tokens_data = json.load(f)

with open("glossary_u3_u4.json", "r", encoding="utf-8") as f:
    glossary = json.load(f)

by_key = glossary["by_key"]

for u in ["3", "4"]:
    for p in sorted(tokens_data[u].keys(), key=lambda x: int(x)):
        toks = tokens_data[u][p]
        print(f"\n================ Unit {u} Paragraph {p} ================")
        for t in toks:
            idx = t["idx"]
            word = t["word"]
            key = f"{u}_{p}_{idx}"
            trans = by_key.get(key, "MISSING")
            print(f"{key:10} | {word:20} | {trans}")
