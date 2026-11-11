import json

with open("data_units.json", "r", encoding="utf-8") as f:
    units = json.load(f)

for u_id in ["3", "4"]:
    u = units[u_id]
    print(f"=== UNIT {u_id}: {u.get('title_ar', '')} / {u.get('title_ru', '')} ===")
    for p in u.get("paragraphs", []):
        print(f"\n--- Unit {u_id} Paragraph {p['id']}: {p.get('title_ar', '')} ({p.get('title_ru', '')}) ---")
        lines = [line.strip() for line in p.get("text", "").split("\n") if line.strip()]
        for l in lines:
            print("  ", l)
