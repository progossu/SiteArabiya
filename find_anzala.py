import json

with open('audio/words_map.json', 'r', encoding='utf-8') as f:
    wmap = json.load(f)

for k, v in wmap.items():
    if 'أَنْزَلَ' in k or 'أنزل' in k:
        print('In words_map:', repr(k), '->', v)

with open('data_units.json', 'r', encoding='utf-8') as f:
    units = json.load(f)

for u_id, u_data in units.items():
    for p in u_data.get('paragraphs', []):
        text = p.get('text', '')
        if 'أَنْزَلَ' in text or 'أنزل' in text:
            pid = p['id']
            matches = [w for w in text.split() if 'أَنْزَلَ' in w or 'أنزل' in w]
            print(f'In Unit {u_id} Para {pid}: {matches}')
