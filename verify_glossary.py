import json

with open("glossary_u5_u6.json", "r", encoding="utf-8") as f:
    glossary = json.load(f)

with open("paragraphs_words_tokens.json", "r", encoding="utf-8") as f:
    tokens_data = json.load(f)

by_key = glossary["by_key"]
words = glossary["words"]

# Verification
errors = []
for u_id in ["5", "6"]:
    for p_id in sorted(tokens_data[u_id].keys(), key=lambda x: int(x)):
        for t in tokens_data[u_id][p_id]:
            k = f"{u_id}_{p_id}_{t['idx']}"
            if k not in by_key:
                errors.append(f"Missing key: {k}")
            elif not by_key[k] or not by_key[k].strip():
                errors.append(f"Empty translation for key: {k} (word: {t['word']})")

print(f"Validation finished. Total errors: {len(errors)}")
if errors:
    for e in errors[:10]:
        print("  ", e)
    exit(1)

print("\n--- Sample inspection across paragraphs ---")
samples = [
    "5_1_0", "5_1_11", "5_1_20", "5_1_32",
    "5_2_1", "5_2_14", "5_2_20", "5_2_61",
    "5_3_1", "5_3_24", "5_3_58", "5_3_72",
    "5_4_16", "5_4_35", "5_4_50",
    "5_5_1", "5_5_20", "5_5_35", "5_5_54", "5_5_70",
    "5_6_8", "5_6_27", "5_6_47", "5_6_67",
    "5_7_4", "5_7_18", "5_7_41", "5_7_43",
    "6_1_1", "6_1_8", "6_1_16",
    "6_2_15", "6_2_35", "6_2_51", "6_2_61",
    "6_3_5", "6_3_13", "6_3_31", "6_3_50", "6_3_91",
    "6_4_10", "6_4_40", "6_4_62", "6_4_84",
    "6_5_13", "6_5_26", "6_5_49", "6_5_69", "6_5_85", "6_5_104",
    "6_6_6", "6_6_17", "6_6_29", "6_6_33"
]

for s in samples:
    u, p, idx = s.split("_")
    word = tokens_data[u][p][int(idx)]["word"]
    trans = by_key[s]
    print(f"[{s}] {word} -> {trans}")

print(f"\nAll checks passed successfully! Total keys: {len(by_key)}, Total words: {len(words)}")
