import json
import os
import glob
import re

def merge():
    files = [
        "glossary_u1_u2.json",
        "glossary_u3_u4.json",
        "glossary_u5_u6.json",
        "glossary_u7_u8.json"
    ]
    
    missing_files = [f for f in files if not os.path.exists(f)]
    if missing_files:
        print(f"Waiting for files: {missing_files}")
        return False
        
    combined_by_key = {}
    combined_words = {}
    
    for fpath in files:
        with open(fpath, "r", encoding="utf-8") as fp:
            d = json.load(fp)
            by_key = d.get("by_key", {})
            words = d.get("words", {})
            combined_by_key.update(by_key)
            combined_words.update(words)
            print(f"Loaded {fpath}: {len(by_key)} keys, {len(words)} words")
            
    # Also load data_units.json vocabulary and verbs into combined_words as fallback
    with open("data_units.json", "r", encoding="utf-8") as f:
        units = json.load(f)
    for u_id, u in units.items():
        for v in u.get("vocabulary", []):
            w = v.get("word", "").strip()
            pl = v.get("plural", "").strip()
            m = v.get("meaning", "").strip()
            if w and w not in combined_words: combined_words[w] = m
            if pl and pl not in combined_words: combined_words[pl] = f"мн. от {w}: {m}"
        for vb in u.get("verbs", []):
            m = vb.get("meaning", "").strip()
            pa = vb.get("past", "").strip()
            pr = vb.get("present", "").strip()
            ms = vb.get("masdar", "").strip()
            if pa and pa not in combined_words: combined_words[pa] = f"{m} (прош.)"
            if pr and pr not in combined_words: combined_words[pr] = f"{m} (наст.)"
            if ms and ms not in combined_words: combined_words[ms] = f"{m} (масдар)"

    # Verify every token from paragraphs_words_tokens.json
    with open("paragraphs_words_tokens.json", "r", encoding="utf-8") as f:
        pwt = json.load(f)
        
    total_tokens = 0
    missing_tokens = []
    
    for u_id in sorted(pwt.keys(), key=int):
        for p_id in sorted(pwt[u_id].keys(), key=int):
            for t in pwt[u_id][p_id]:
                total_tokens += 1
                k = f"{u_id}_{p_id}_{t['idx']}"
                w = t["word"]
                if w == "-" and combined_by_key.get(k) in ("—", "-", None, ""):
                    combined_by_key[k] = "тире"
                trans = combined_by_key.get(k) or combined_words.get(w)
                if not trans or not trans.strip():
                    missing_tokens.append((k, w))
                    
    print(f"\nVerification Results:")
    print(f"Total tokens in articles: {total_tokens}")
    print(f"Covered tokens: {total_tokens - len(missing_tokens)} ({(total_tokens - len(missing_tokens))/total_tokens*100:.2f}%)")
    print(f"Missing tokens: {len(missing_tokens)}")
    
    if missing_tokens:
        print("Sample missing tokens:", missing_tokens[:20])
        
    out_path = "data_words_glossary.json"
    with open(out_path, "w", encoding="utf-8") as fp:
        json.dump({
            "by_key": combined_by_key,
            "words": combined_words
        }, fp, ensure_ascii=False, indent=2)
    print(f"Successfully saved {out_path} with {len(combined_by_key)} keys and {len(combined_words)} words!")
    return len(missing_tokens) == 0

if __name__ == "__main__":
    merge()
