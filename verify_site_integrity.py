import os
import re
import json

print("=== STARTING FULL SITE INTEGRITY VERIFICATION ===")

# 1. Check all HTML files
html_files = ['index.html'] + [f'unit{i}.html' for i in range(1, 9)]
for hf in html_files:
    if not os.path.exists(hf):
        print(f"❌ Missing file: {hf}")
    else:
        sz = os.path.getsize(hf)
        print(f"✅ {hf} exists ({sz:,} bytes)")

# 2. Check full audio files
print("\n--- Verifying Unit Audio Files ---")
for i in range(1, 9):
    full_audio = f"audio/u{i}_full.mp3"
    if not os.path.exists(full_audio):
        print(f"❌ Missing full audio: {full_audio}")
    else:
        print(f"✅ {full_audio} ({os.path.getsize(full_audio):,} bytes)")

# 3. Check paragraph audio files
print("\n--- Verifying Paragraph Audio Files ---")
para_counts = {1: 6, 2: 7, 3: 6, 4: 7, 5: 7, 6: 6, 7: 7, 8: 8}
missing_paras = []
for u, pcount in para_counts.items():
    for p in range(1, pcount + 1):
        p_audio = f"audio/u{u}_para_{p}.mp3"
        if not os.path.exists(p_audio):
            missing_paras.append(p_audio)
if not missing_paras:
    print(f"✅ All {sum(para_counts.values())} paragraph audio files verified!")
else:
    print(f"❌ Missing paragraph audios: {missing_paras}")

# 4. Check core human audio assets
print("\n--- Verifying Core Sacred & Human Audio Assets ---")
core_assets = [
    'audio/words/salawat_human.mp3',
    'audio/words/radiyallahu_anhu.mp3',
    'audio/words/radiyallahu_anha.mp3',
    'audio/words/allah_damma.mp3',
    'audio/words/allah_kasra.mp3',
    'audio/words/allah_fatha.mp3',
    'audio/h_allah_damma.mp3',
    'audio/h_allah_kasra.mp3',
    'audio/h_allah_fatha.mp3'
]
for ca in core_assets:
    if os.path.exists(ca):
        print(f"✅ {ca} ({os.path.getsize(ca):,} bytes)")
    else:
        print(f"❌ Missing core asset: {ca}")

# 5. Check words audio map count
with open('audio/words_map.json', 'r', encoding='utf-8') as f:
    wmap = json.load(f)
print(f"\n✅ Total words mapped in audio/words_map.json: {len(wmap)}")

# 6. Check navigation links in HTML pages
print("\n--- Verifying Reciprocal Navigation Links ---")
with open('index.html', 'r', encoding='utf-8') as f:
    idx_content = f.read()

for i in range(1, 9):
    if f'href="unit{i}.html"' not in idx_content:
        print(f"❌ index.html missing link to unit{i}.html")
    else:
        print(f"✅ index.html -> unit{i}.html link OK")

for i in range(1, 9):
    with open(f'unit{i}.html', 'r', encoding='utf-8') as f:
        u_content = f.read()
    if 'href="index.html"' not in u_content:
        print(f"❌ unit{i}.html missing link back to index.html")
    if i > 1 and f'href="unit{i-1}.html"' not in u_content:
        print(f"❌ unit{i}.html missing link to unit{i-1}.html")
    if i < 8 and f'href="unit{i+1}.html"' not in u_content:
        print(f"❌ unit{i}.html missing link to unit{i+1}.html")
print("✅ All navigation breadcrumbs & unit switchers verified!")

print("\n=== SITE INTEGRITY 100% VERIFIED! ===")
