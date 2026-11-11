import json
import re

with open("data_units.json", "r", encoding="utf-8") as f:
    units = json.load(f)

quran_pattern = re.compile(r'﴿([^﴾]+)﴾(\s*\[[^\]]+\])?')

for u_id in sorted(units.keys(), key=lambda x: int(x)):
    u = units[u_id]
    total_u_words = 0
    para_counts = []
    for p in u.get("paragraphs", []):
        text = p.get("text", "")
        lines = text.split("\n")
        p_words = 0
        for line in lines:
            line = line.strip()
            if not line or (line.startswith("الْفِقْرَةُ") and "—" in line):
                continue
            clean_line = quran_pattern.sub("", line)
            words = [w for w in re.split(r"\s+", clean_line) if w.strip()]
            for word in words:
                m_lead = re.match(r'^([\u060C\u061B\u061F\u06D4.,:;»"\'()\[\]{}—«!؟?]+)', word)
                if m_lead: word = word[len(m_lead.group(1)):]
                m_trail = re.search(r'([\u060C\u061B\u061F\u06D4.,:;»"\'()\[\]{}—«!؟?]+)$', word)
                if m_trail: word = word[:-len(m_trail.group(1))]
                word = word.strip()
                if word and word not in ["ﷺ", "ﷺ،"]:
                    p_words += 1
        para_counts.append(f"P{p['id']}:{p_words}")
        total_u_words += p_words
    print(f"Unit {u_id}: total {total_u_words} words ({', '.join(para_counts)})")
