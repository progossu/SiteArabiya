import json

with open("data_units.json", "r", encoding="utf-8") as f:
    units = json.load(f)

for u_id in ["5", "6"]:
    u = units[u_id]
    print(f"=== UNIT {u_id}: {u.get('title_ar', '')} / {u.get('title_ru', '')} ===")
    for p in u.get("paragraphs", []):
        print(f"--- Paragraph {p['id']}: {p.get('title_ar', '')} ---")
        lines = [line.strip() for line in p.get("text", "").split("\n") if line.strip()]
        for l in lines[:3]:
            print("  ", l)
