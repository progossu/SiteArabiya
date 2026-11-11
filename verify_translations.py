import json
import sys
from translate_u1 import unit1_data
from translate_u2 import unit2_data

with open('paragraphs_words_tokens.json', 'r', encoding='utf-8') as f:
    pwt = json.load(f)

errors = []

for u_id, u_data in [("1", unit1_data), ("2", unit2_data)]:
    for p_id in sorted(pwt[u_id].keys(), key=int):
        toks = pwt[u_id][p_id]
        p_trans = u_data.get(p_id, {})
        
        if len(toks) != len(p_trans):
            errors.append(f"Unit {u_id} P{p_id}: token count mismatch! PWT has {len(toks)}, translations has {len(p_trans)}")
            
        for t in toks:
            idx = t['idx']
            word = t['word']
            if idx not in p_trans:
                errors.append(f"Missing translation: {u_id}_{p_id}_{idx} ({word})")
            elif not p_trans[idx] or not p_trans[idx].strip():
                errors.append(f"Empty translation: {u_id}_{p_id}_{idx} ({word})")

if errors:
    print(f"FAILED with {len(errors)} errors:")
    for e in errors[:20]:
        print(" ", e)
    sys.exit(1)
else:
    print("ALL TOKENS HAVE TRANSLATIONS! Unit 1 & Unit 2 counts match perfectly.")
