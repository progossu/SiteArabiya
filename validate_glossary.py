import json
import os
import sys

def validate():
    path = 'glossary_u1_u2.json'
    assert os.path.exists(path), f"File {path} does not exist!"
    
    with open(path, 'r', encoding='utf-8') as f:
        glossary = json.load(f)
        
    assert 'by_key' in glossary, "'by_key' missing from glossary"
    assert 'words' in glossary, "'words' missing from glossary"
    
    by_key = glossary['by_key']
    words = glossary['words']
    
    with open('paragraphs_words_tokens.json', 'r', encoding='utf-8') as f:
        pwt = json.load(f)
        
    u1_count = 0
    u2_count = 0
    missing = []
    empty = []
    
    for u_id in ['1', '2']:
        for p_id in pwt[u_id]:
            for t in pwt[u_id][p_id]:
                if u_id == '1': u1_count += 1
                if u_id == '2': u2_count += 1
                key = f"{u_id}_{p_id}_{t['idx']}"
                if key not in by_key:
                    missing.append(key)
                elif not by_key[key] or not by_key[key].strip():
                    empty.append(key)
                    
    print(f"Validation summary:")
    print(f"  Unit 1 tokens checked: {u1_count}")
    print(f"  Unit 2 tokens checked: {u2_count}")
    print(f"  Total tokens checked: {u1_count + u2_count}")
    print(f"  Total keys in by_key: {len(by_key)}")
    print(f"  Total words in words dict: {len(words)}")
    print(f"  Missing keys: {len(missing)}")
    print(f"  Empty translations: {len(empty)}")
    
    assert len(missing) == 0, f"Missing {len(missing)} keys: {missing[:10]}"
    assert len(empty) == 0, f"Empty {len(empty)} translations: {empty[:10]}"
    assert u1_count == 434, f"Expected 434 tokens for Unit 1, got {u1_count}"
    assert u2_count == 574, f"Expected 574 tokens for Unit 2, got {u2_count}"
    assert len(by_key) == 1008, f"Expected 1008 keys in by_key, got {len(by_key)}"
    assert len(words) == 616, f"Expected 616 words in words dict, got {len(words)}"
    
    # Check required example words in Unit 1:
    examples = {
        "1_1_9": ("مُنَجَّماً", "частями / по частям"),
        "1_1_11": ("حَسَبَ", "согласно / по мере"),
        "1_1_12": ("الْحَوَادِثِ", "событий"),
        "1_2_1": ("لِلرَّسُولِ", "Посланнику"),
        "1_2_18": ("كَالْعَصَا", "как посох"),
        "1_2_20": ("مُوسَى", "Мусы (Моисея)"),
        "1_2_23": ("وَالنَّاقَةِ", "и верблюдица"),
        "1_2_25": ("صَالِحٍ", "Салиха"),
        "1_2_28": ("وَقَدْ", "и уже"),
        "1_2_29": ("ذَهَبَتْ", "ушли / исчезли"),
        "1_2_31": ("الْمُعْجِزَاتُ", "чудеса"),
        "1_2_32": ("بِذَهَابِ", "с уходом"),
        "1_2_33": ("مَنْ", "тех, среди кого"),
        "1_2_34": ("ظَهَرَتْ", "явились"),
        "1_2_38": ("لِلْعِبْرَةِ", "для назидания / в качестве урока"),
        "1_2_39": ("أَمَّا", "что же касается"),
        "1_2_46": ("الْكَرِيمُ", "Благородный / Щедрый"),
    }
    
    print("\nChecking required prompt examples:")
    for k, (expected_w, expected_t) in examples.items():
        actual_t = by_key.get(k)
        print(f"  {k} ({expected_w}): '{actual_t}'")
        assert expected_t in actual_t, f"Mismatch for {k} ({expected_w}): expected '{expected_t}', got '{actual_t}'"
        
    print("\nALL CHECKS PASSED PERFECTLY!")

if __name__ == '__main__':
    validate()
