import json
import re

with open('data_units.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

modified_count = 0
for u_key, u in data.items():
    for p_idx, p in enumerate(u.get('paragraphs', [])):
        text = p.get('text', '')
        lines = text.split('\n')
        # Check first line
        if lines and ('الْفِقْرَةُ' in lines[0] or 'الفقرة' in lines[0]) and '—' in lines[0]:
            # Remove this first line and any immediate empty lines
            new_lines = lines[1:]
            while new_lines and not new_lines[0].strip():
                new_lines = new_lines[1:]
            p['text'] = '\n'.join(new_lines)
            modified_count += 1
            print(f"Cleaned U{u_key} P{p_idx+1}: First line now -> {new_lines[0][:50] if new_lines else ''}")

print(f"Total paragraphs modified: {modified_count}")

with open('data_units.json', 'w', encoding='utf-8') as f:
    json.dump(data, f, ensure_ascii=False, indent=2)

print("Saved to data_units.json")
