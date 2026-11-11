# Script to generate complete glossary_u7_u8.json

import json

# Load existing token structure
with open('paragraphs_words_tokens.json', 'r', encoding='utf-8') as f:
    tokens_data = json.load(f)

u7_tokens = tokens_data['7']
u8_tokens = tokens_data['8']

by_key = {}
words = {}

# We will populate by_key for Unit 7 and Unit 8
