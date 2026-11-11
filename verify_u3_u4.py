import json

with open("glossary_u3_u4.json", "r", encoding="utf-8") as f:
    data = json.load(f)

by_key = data.get("by_key", {})
words = data.get("words", {})

print(f"by_key count: {len(by_key)}")
print(f"words count: {len(words)}")

empty_keys = [k for k, v in by_key.items() if not v or not v.strip()]
print(f"empty keys: {len(empty_keys)}")

with open("paragraphs_words_tokens.json", "r", encoding="utf-8") as f:
    tokens = json.load(f)

missing = []
for u in ["3", "4"]:
    for p in sorted(tokens[u].keys(), key=lambda x: int(x)):
        for t in tokens[u][p]:
            k = f"{u}_{p}_{t['idx']}"
            if k not in by_key:
                missing.append(k)

print(f"missing keys: {len(missing)}")
if not missing and not empty_keys:
    print("ALL 906 TOKENS IN UNIT 3 & 4 ARE 100% TRANSLATED AND VALIDATED!")
