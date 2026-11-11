import glob
import re

all_words = set()
for hf in glob.glob('unit*.html'):
    txt = open(hf, 'r', encoding='utf-8').read()
    articles = re.findall(r'<div class="arabic-article" id="p-\d+">(.*?)</div>', txt, re.DOTALL)
    for art in articles:
        matches = re.findall(r'<span class="ar-word[^"]*"[^>]*>([^<]*)</span>', art)
        for w in matches:
            all_words.add(w.strip())

print(f"Total unique words in articles: {len(all_words)}")
