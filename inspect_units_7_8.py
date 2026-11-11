import json

with open("data_units.json", "r", encoding="utf-8") as f:
    units = json.load(f)

for u_id in ["7", "8"]:
    u = units[u_id]
    print(f"=== UNIT {u_id}: {u.get('title_ar')} ({u.get('title_ru')}) ===")
    print(f"Desc: {u.get('desc_ru')}")
    print("\n-- PARAGRAPHS --")
    for p in u.get("paragraphs", []):
        print(f"P {p['id']}:")
        print(p.get("text", "").strip())
        print("-" * 40)
    print("\n-- VOCABULARY --")
    for v in u.get("vocabulary", []):
        print(f"{v.get('word')} | {v.get('plural')} | {v.get('meaning')}")
    print("\n-- VERBS --")
    for vb in u.get("verbs", []):
        print(f"{vb.get('past')} - {vb.get('present')} - {vb.get('masdar')} | {vb.get('meaning')}")
    print("\n-- COLLOCATIONS --")
    for c in u.get("collocations", []):
        print(f"{c.get('phrase')} | {c.get('meaning')}")
    print("=" * 60)
