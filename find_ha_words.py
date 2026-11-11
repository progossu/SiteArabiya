import json

with open('audio/words_map.json', 'r', encoding='utf-8') as f:
    wmap = json.load(f)

damma_ha_words = []
for w, fname in wmap.items():
    if w.endswith('هُ') or w.endswith('هُ،') or w.endswith('هُ.'):
        damma_ha_words.append((w, fname))

print(f"Total words ending with damma-ha: {len(damma_ha_words)}")
for w, fname in damma_ha_words[:25]:
    print(f"  {w:20} -> {fname}")
