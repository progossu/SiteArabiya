import json
import re
from collections import Counter

with open("data_units.json", "r", encoding="utf-8") as f:
    units = json.load(f)

# Load existing vocabulary and verbs into a lookup dict
lookup = {}
for u_id, u in units.items():
    for item in u.get("vocabulary", []):
        w = item.get("word", "").strip()
        pl = item.get("plural", "").strip()
        m = item.get("meaning", "").strip()
        if w and m:
            lookup[w] = m
        if pl and m:
            lookup[pl] = f"мн. от {w}: {m}"
            
    for item in u.get("verbs", []):
        m = item.get("meaning", "").strip()
        pa = item.get("past", "").strip()
        pr = item.get("present", "").strip()
        ms = item.get("masdar", "").strip()
        if pa and m:
            lookup[pa] = f"{m} (прош.)"
        if pr and m:
            lookup[pr] = f"{m} (наст.)"
        if ms and m:
            lookup[ms] = f"{m} (масдар)"

    for item in u.get("collocations", []):
        ph = item.get("phrase", "").strip()
        m = item.get("meaning", "").strip()
        if ph and m:
            lookup[ph] = m

print(f"Total entries from tables: {len(lookup)}")

# Now collect all words from all paragraphs
quran_pattern = re.compile(r'﴿([^﴾]+)﴾(\s*\[[^\]]+\])?')
word_counts = Counter()

for u_id, u in units.items():
    for p in u.get("paragraphs", []):
        text = p.get("text", "")
        lines = text.split("\n")
        for line in lines:
            line = line.strip()
            if not line or (line.startswith("الْفِقْرَةُ") and "—" in line):
                continue
            # Remove quran parts
            clean_line = quran_pattern.sub("", line)
            words = [w for w in re.split(r"\s+", clean_line) if w.strip()]
            for word in words:
                # strip punctuation
                m_lead = re.match(r'^([\u060C\u061B\u061F\u06D4.,:;»"\'()\[\]{}—«!؟?]+)', word)
                if m_lead:
                    word = word[len(m_lead.group(1)):]
                m_trail = re.search(r'([\u060C\u061B\u061F\u06D4.,:;»"\'()\[\]{}—«!؟?]+)$', word)
                if m_trail:
                    word = word[:-len(m_trail.group(1))]
                word = word.strip()
                if word and word not in ["ﷺ", "ﷺ،"]:
                    word_counts[word] += 1

print(f"Total article word tokens: {sum(word_counts.values())}")
print(f"Total distinct article words: {len(word_counts)}")

# How many already have direct match in lookup or BASE_GLOSS
with open("build_site.py", "r", encoding="utf-8") as f:
    bs = f.read()

# Extract BASE_GLOSS
import ast
m = re.search(r'BASE_GLOSS\s*=\s*(\{.*?\n\})', bs, re.DOTALL)
base_gloss = ast.literal_eval(m.group(1)) if m else {}
print(f"BASE_GLOSS entries: {len(base_gloss)}")

matched = 0
unmatched = []
for w, count in word_counts.most_common():
    if w in lookup or w in base_gloss:
        matched += 1
    else:
        unmatched.append((w, count))

print(f"Directly matched: {matched}")
print(f"Unmatched words: {len(unmatched)}")
