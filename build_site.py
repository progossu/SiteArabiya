# -*- coding: utf-8 -*-
"""
Master Builder for SiteArabiya:
Generates index.html (Grand Table of Contents) and unit1.html through unit8.html.
"""
import json
import re
import os
import data_units

# Load words audio map
with open('audio/words_map.json', 'r', encoding='utf-8') as f:
    words_map = json.load(f)
words_map_json = json.dumps(words_map, ensure_ascii=False)

# Load CSS and JS templates
with open('template_css.txt', 'r', encoding='utf-8') as f:
    css_content = f.read()

with open('template_js.txt', 'r', encoding='utf-8') as f:
    js_template = f.read()

# Add extra CSS for navigation and index portal
EXTRA_CSS = """
    /* Reverent styling for the Divine Name (when not in karaoke) */
    .ar-allah:not(.audio-active) {
      color: #1b4332 !important;
      font-weight: 700;
      text-shadow: 0 0 1px rgba(27, 67, 50, 0.2);
    }
    [data-theme="dark"] .ar-allah:not(.audio-active) {
      color: #52b788 !important;
      text-shadow: 0 0 2px rgba(82, 183, 136, 0.3);
    }

    /* User selected word highlighting on click */
    .ar-word.user-selected {
      background: rgba(45, 106, 79, 0.25) !important;
      outline: 2px solid #2d6a4f;
      border-radius: 4px;
    }

    /* Top Navigation Bar */
    .top-nav {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 0.75rem 1.5rem;
      background: var(--surface);
      border-bottom: 1px solid var(--border);
      position: sticky;
      top: 0;
      z-index: 100;
      box-shadow: 0 2px 8px rgba(0,0,0,0.05);
    }

    .nav-breadcrumbs {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      font-size: 0.95rem;
      font-weight: 600;
      flex-wrap: wrap;
    }

    .nav-link {
      color: var(--primary);
      text-decoration: none;
      display: inline-flex;
      align-items: center;
      gap: 0.4rem;
      padding: 0.35rem 0.75rem;
      border-radius: 6px;
      background: var(--surface-alt);
      border: 1px solid var(--border);
      transition: all 0.2s;
    }

    .nav-link:hover {
      background: var(--primary);
      color: white;
      border-color: var(--primary);
    }

    .nav-controls {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      flex-wrap: wrap;
    }

    /* Index Portal Styling */
    .portal-hero {
      text-align: center;
      padding: 3rem 1.5rem 2rem;
      background: linear-gradient(135deg, rgba(27,67,50,0.05) 0%, rgba(201,151,56,0.08) 100%);
      border-bottom: 1px solid var(--border);
    }

    .portal-hero h1 {
      font-family: var(--arabic-font);
      font-size: 2.75rem;
      color: var(--primary);
      margin-bottom: 0.5rem;
      direction: rtl;
    }

    .portal-hero .subhead {
      font-size: 1.25rem;
      font-weight: 700;
      color: var(--text);
      margin-bottom: 0.75rem;
    }

    .portal-hero .desc {
      font-size: 1rem;
      color: var(--text-muted);
      max-width: 800px;
      margin: 0 auto 1.5rem;
      line-height: 1.6;
    }

    .stats-row {
      display: flex;
      justify-content: center;
      gap: 1.5rem;
      flex-wrap: wrap;
      margin-bottom: 2rem;
    }

    .stat-badge {
      background: var(--surface);
      border: 1px solid var(--border);
      padding: 0.6rem 1.25rem;
      border-radius: 30px;
      font-size: 0.9rem;
      font-weight: 600;
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
      box-shadow: var(--shadow-sm);
    }

    .units-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
      gap: 1.75rem;
      margin: 2.5rem 0 4rem;
    }

    .unit-card {
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: 14px;
      padding: 1.75rem;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      transition: transform 0.25s, box-shadow 0.25s, border-color 0.25s;
      box-shadow: var(--shadow-sm);
      position: relative;
      overflow: hidden;
    }

    .unit-card::before {
      content: "";
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      height: 4px;
      background: linear-gradient(90deg, var(--primary) 0%, var(--accent) 100%);
    }

    .unit-card:hover {
      transform: translateY(-4px);
      box-shadow: var(--shadow-md);
      border-color: var(--primary-light);
    }

    .unit-card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1rem;
    }

    .unit-card-badge {
      font-size: 0.8rem;
      font-weight: 700;
      background: rgba(27,67,50,0.1);
      color: var(--primary);
      padding: 0.25rem 0.65rem;
      border-radius: 20px;
    }

    [data-theme="dark"] .unit-card-badge {
      background: rgba(45,106,79,0.3);
      color: #95d5b2;
    }

    .unit-card-icon {
      font-size: 1.85rem;
    }

    .unit-card-title-ar {
      font-family: var(--arabic-font);
      font-size: 1.75rem;
      font-weight: 700;
      color: var(--primary);
      margin-bottom: 0.25rem;
      direction: rtl;
      text-align: right;
    }

    .unit-card-title-ru {
      font-size: 1.15rem;
      font-weight: 700;
      color: var(--text);
      margin-bottom: 0.75rem;
    }

    .unit-card-desc {
      font-size: 0.88rem;
      color: var(--text-muted);
      line-height: 1.55;
      margin-bottom: 1.25rem;
      flex-grow: 1;
    }

    .unit-card-features {
      display: flex;
      gap: 0.4rem;
      flex-wrap: wrap;
      margin-bottom: 1.25rem;
    }

    .unit-feature-pill {
      font-size: 0.75rem;
      background: var(--surface-alt);
      border: 1px solid var(--border);
      padding: 0.2rem 0.5rem;
      border-radius: 4px;
      color: var(--text-muted);
    }

    .unit-card-actions {
      display: flex;
      gap: 0.75rem;
      align-items: center;
    }

    .btn-open-unit {
      flex-grow: 1;
      background: var(--primary);
      color: white;
      text-decoration: none;
      text-align: center;
      padding: 0.65rem 1rem;
      border-radius: 8px;
      font-weight: 600;
      font-size: 0.95rem;
      transition: background 0.2s, transform 0.15s;
      border: none;
      display: inline-flex;
      justify-content: center;
      align-items: center;
      gap: 0.5rem;
    }

    .btn-open-unit:hover {
      background: var(--primary-light);
      color: white;
      transform: scale(1.02);
    }

    .btn-preview-audio {
      background: var(--surface-alt);
      border: 1px solid var(--border);
      color: var(--text);
      padding: 0.65rem 0.85rem;
      border-radius: 8px;
      font-size: 1rem;
      cursor: pointer;
      transition: all 0.2s;
    }

    .btn-preview-audio:hover {
      background: var(--primary);
      color: white;
      border-color: var(--primary);
    }
"""

# Insert EXTRA_CSS before </style>
css_full = css_content.replace('</style>', EXTRA_CSS + '\n  </style>')

def clean_word(w):
    return re.sub(r'[\u060C\u061B\u061F\u06D4.,:;»"\'()\[\]{}—«!؟?ـ\s]', '', w).strip()

def strip_tashkeel(w):
    if not w:
        return ''
    return re.sub(r'[\u064B-\u065F\u0670]', '', w)

BASE_GLOSS = {
    'الْقُرْآنُ': 'Коран', 'الْقُرْآنِ': 'Корана', 'الْقُرْآنَ': 'Коран',
    'مُعْجِزَةُ': 'чудо', 'مُعْجِزَاتٌ': 'чудеса', 'مُعْجِزٌ': 'неподражаемый',
    'الرَّسُولِ': 'Посланника', 'الرَّسُولُ': 'Посланник', 'الرَّسُولَ': 'Посланника',
    'النَّبِيِّ': 'Пророка', 'النَّبِيُّ': 'Пророк', 'أَنْزَلَهُ': 'ниспослал его',
    'سُبْحَانَهُ': 'Пречист Он', 'وَتَعَالَى': 'и Всевышен', 'تَعَالَى': 'Всевышний',
    'عَلَيْهِ': 'на него / ему', 'مُنَجَّماً': 'порциями / по частям', 'مُفَرَّقاً': 'раздельно',
    'حَسَبَ': 'по мере / согласно', 'الْحَوَادِثِ': 'событий', 'وَالْحِكْمَةُ': 'и мудрость',
    'مِنْ': 'из / от', 'ذَلِكَ': 'этого / того', 'تَثْبِيتُ': 'укрепление',
    'قَلْبِ': 'сердца', 'وَقُلُوبِ': 'и сердец', 'الْمُسْلِمِينَ': 'мусульман',
    'الْمُسْلِمُونَ': 'мусульмане', 'وَلِيَكُونَ': 'и чтобы было', 'حِفْظُهُ': 'его заучивание',
    'سَهْلاً': 'легким', 'عَلَيْهِمْ': 'для них', 'قَالَ': 'сказал', 'تَعَالَى:': 'Всевышний:',
    'إِذَا': 'когда / если', 'بَزَغَ': 'взошел / забрезжил', 'فَجْرُ': 'рассвет',
    'يَوْمٍ': 'дня', 'جَدِيدٍ': 'нового', 'فِي': 'в / внутри', 'حَيَاةِ': 'жизни',
    'النَّاشِئِ': 'юноши', 'الْمُسْلِمِ': 'мусульманина', 'يَدْعُو': 'обращается с мольбой',
    'بَعْدَ': 'после', 'أَنْ': 'что / чтобы', 'يَسْتَيْقِظَ': 'проснется',
    'بِالدُّعَاءِ': 'с мольбой', 'الْمَشْهُورِ': 'известной', 'الْحَمْدُ': 'хвала',
    'لِلَّهِ': 'Аллаху', 'الَّذِي': 'Который', 'أَحْيَانَا': 'оживил нас',
    'مَا': 'того как / что', 'أَمَاتَنَا': 'умертвил нас', 'وَإِلَيْهِ': 'и к Нему',
    'النُّشُورُ': 'воскрешение', 'وَلَا': 'и не', 'عَلَى': 'на / над', 'إِلَى': 'к / в',
    'عَنْ': 'от / о', 'مَعَ': 'с / вместе', 'هَذَا': 'этот', 'هَذِهِ': 'эта',
    'الَّتِي': 'которая', 'الَّذِينَ': 'которые', 'كَانَ': 'был', 'كَانَتْ': 'была',
    'يَكُونُ': 'бывает / будет', 'أَوْ': 'или', 'ثُمَّ': 'затем', 'كُلِّ': 'каждого / всего',
    'بَيْنَ': 'между', 'عِنْدَ': 'у / при', 'قَبْلَ': 'до / перед', 'حَتَّى': 'пока / чтобы',
    'غَيْرِ': 'не / кроме', 'كَمَا': 'как и', 'لَا': 'не / нет', 'إِنَّ': 'поистине',
    'أَنَّ': 'что', 'لَكِنْ': 'но / однако', 'فَقَالَ': 'и сказал', 'فَقَالَتْ': 'и сказала',
    'وَقَالَ': 'и сказал', 'قَالُوا': 'они сказали', 'مَسْجِدِ': 'мечети',
    'الْحَيِّ': 'района / квартала', 'صَلَاةُ': 'молитва', 'الْفَجْرِ': 'утренней зари',
    'جَمَاعَةً': 'коллективно', 'تَهَجُّداً': 'бодрствуя ночью в молитве',
    'الْقِيَامَةِ': 'Воскресения', 'الْجَنَّةَ': 'Рай', 'النَّارِ': 'Огня / Ада',
    'الدُّعَاءِ': 'мольбы', 'الْفِطْرَةَ': 'природу / естество', 'الْعَصْرِ': 'эпохи / века',
    'الْحَيَاةُ': 'жизнь', 'الْخَيْرِ': 'добра / блага', 'الْقِرَاءَةُ': 'чтение',
    'الْكُتُبِ': 'книг', 'الْعُلَمَاءِ': 'ученых', 'الْأَطْفَالِ': 'детей',
    'الْعُقُولِ': 'умов / интеллекта', 'الْهِجْرَةُ': 'эмиграция', 'الْخَارِجِ': 'за рубежом',
    'النَّوْمُ': 'сон', 'الْجَسَدِ': 'тела', 'النَّهَارِ': 'дня', 'اللَّيْلِ': 'ночи',
    'السَّلَامُ': 'мир / приветствие', 'عَلَيْكُمْ': 'вам', 'طَعَامٌ': 'еда / пища'
}

def get_word_translation(raw_w, u_data):
    w = clean_word(raw_w)
    if not w:
        return ""
    if raw_w in ['ﷺ', 'صَلَّى اللَّهُ عَلَيْهِ وَسَلَّمَ']:
        return "да благословит его Аллах и приветствует"
    if 'رَضِيَ اللَّهُ عَنْهُ' in raw_w:
        return "да будет доволен им Аллах"
    if 'رَضِيَ اللَّهُ عَنْهَا' in raw_w:
        return "да будет доволен ею Аллах"
    if 'اللَّه' in w:
        return "Аллах"
    if w in BASE_GLOSS:
        return BASE_GLOSS[w]
    if raw_w in BASE_GLOSS:
        return BASE_GLOSS[raw_w]
        
    for item in u_data.get('vocabulary', []):
        if clean_word(item.get('word', '')) == w:
            return item.get('meaning', '')
        if clean_word(item.get('plural', '')) == w:
            return f"мн. от {item.get('word', '')}: {item.get('meaning', '')}"
            
    for item in u_data.get('verbs', []):
        if clean_word(item.get('past', '')) == w:
            return f"{item.get('meaning', '')} (прош.)"
        if clean_word(item.get('present', '')) == w:
            return f"{item.get('meaning', '')} (наст.)"
        if clean_word(item.get('masdar', '')) == w:
            return f"{item.get('meaning', '')} (масдар)"
            
    for item in u_data.get('collocations', []):
        if w in clean_word(item.get('phrase', '')):
            return item.get('meaning', '')
            
    return ""

QURAN_AUDIO_MAP = {
    'الفرقان/٣٢': 'audio/quran/furqan_32.mp3',
    'الحجر/٩': 'audio/quran/hijr_9.mp3',
    'الإسراء/٨٨': 'audio/quran/isra_88.mp3',
    'النحل/٨٩': 'audio/quran/nahl_89.mp3',
    'الشعراء/١٩٣': 'audio/quran/shuara_193_195.mp3',
    'الشعراء/١٩٣-١٩٥': 'audio/quran/shuara_193_195.mp3',
    'النحل/٤٤': 'audio/quran/nahl_44.mp3',
    'النجم/٣-٤': 'audio/quran/najm_3_4.mp3',
    'الحشر/٧': 'audio/quran/hashr_7.mp3',
    'النساء/٨٠': 'audio/quran/nisa_80.mp3',
    'آل عمران/٣١': 'audio/quran/ali_imran_31.mp3',
    'الأحزاب/٣٦': 'audio/quran/ahzab_36.mp3',
    'النساء/٦٥': 'audio/quran/nisa_65.mp3',
    'العلق/١': 'audio/quran/alaq_1.mp3',
    'البقرة/٢٥٥': 'audio/quran/baqarah_255_clip.mp3',
    'الأنعام/٦٠': 'audio/quran/anam_60.mp3',
    'الذاريات/١٧': 'audio/quran/dhariyat_17.mp3',
    'النبأ/٩-١١': 'audio/quran/naba_9_11.mp3',
    'الشعراء/٢٢٤': 'audio/quran/shuara_224.mp3',
}

GLOSSARY_DATA = {}
if os.path.exists('data_words_glossary.json'):
    try:
        with open('data_words_glossary.json', 'r', encoding='utf-8') as f_g:
            GLOSSARY_DATA = json.load(f_g)
    except Exception as e:
        print('Warning loading data_words_glossary.json:', e)

GLOSSARY_BY_KEY = GLOSSARY_DATA.get('by_key', {})
GLOSSARY_WORDS = GLOSSARY_DATA.get('words', {})

def format_paragraph_text_html(p_text, p_id, u_data, u_num=1):
    lines = p_text.split('\n')
    formatted_lines = []

    quran_pattern = re.compile(r'﴿([^﴾]+)﴾(\s*\[[^\]]+\])?')
    word_idx = 0
    quran_idx = 0

    for line in lines:
        line = line.strip()
        if not line:
            continue
        if line.startswith('الْفِقْرَةُ') and '—' in line:
            continue

        # Segment line by Quran quotes directly (no placeholders!)
        last_idx = 0
        segments = []
        for m in quran_pattern.finditer(line):
            if m.start() > last_idx:
                segments.append(('text', line[last_idx:m.start()]))
            segments.append(('quran', m.group(1).strip(), m.group(2).strip() if m.group(2) else ''))
            last_idx = m.end()
        if last_idx < len(line):
            segments.append(('text', line[last_idx:]))

        line_html_parts = []
        for seg in segments:
            if seg[0] == 'quran':
                ayah_text = seg[1]
                ref_text = seg[2]
                audio_src = ""
                for k, a_path in QURAN_AUDIO_MAP.items():
                    if k in ref_text:
                        audio_src = a_path
                        break

                btn_html = f'<button class="quran-inline-btn" onclick="playQuranAyah(\'{audio_src}\', this)" title="Слушать чтение шейха аль-Хусари">▶ аль-Хусари</button>' if audio_src else ''
                q_html = f'<span class="quran-inline" data-quran-idx="{quran_idx}"><span class="quran-ayah">﴿ {ayah_text} ﴾</span> <span class="quran-ref">{ref_text}</span> {btn_html}</span>'
                quran_idx += 1
                line_html_parts.append(q_html)
            else:
                words = [w for w in re.split(r'\s+', seg[1]) if w.strip()]
                for word in words:
                    if not word:
                        continue
                    if word in ['ﷺ', 'ﷺ،']:
                        trailing = '،' if '،' in word else ''
                        line_html_parts.append(f'<span class="ar-word ar-salawat" data-idx="{word_idx}" data-tashkeel="ﷺ" data-plain="ﷺ" data-trans="да благословит его Аллах и приветствует" onclick="selectWord(this, \'ﷺ\', \'да благословит его Аллах и приветствует\')">ﷺ</span>{trailing}')
                        word_idx += 1
                    else:
                        leading = ''
                        trailing = ''
                        m_lead = re.match(r'^([\u060C\u061B\u061F\u06D4.,:;»"\'()\[\]{}—«!؟?]+)', word)
                        if m_lead:
                            leading = m_lead.group(1)
                            word = word[len(leading):]
                        m_trail = re.search(r'([\u060C\u061B\u061F\u06D4.,:;»"\'()\[\]{}—«!؟?]+)$', word)
                        if m_trail:
                            trailing = m_trail.group(1)
                            word = word[:-len(trailing)]

                        cleaned = clean_word(word)
                        if not cleaned:
                            line_html_parts.append(leading + trailing)
                            continue

                        plain_w = strip_tashkeel(word)
                        key = f"{u_num}_{p_id}_{word_idx}"
                        trans = GLOSSARY_BY_KEY.get(key, "")
                        if not trans:
                            trans = GLOSSARY_WORDS.get(word) or GLOSSARY_WORDS.get(cleaned) or GLOSSARY_WORDS.get(plain_w)
                        if not trans:
                            trans = get_word_translation(word, u_data) or get_word_translation(cleaned, u_data) or ''

                        extra_cls = ' ar-allah' if 'اللَّه' in word or 'الله' in word else ''
                        trans_safe = trans.replace('"', '&quot;').replace("'", "&#39;")
                        line_html_parts.append(f'{leading}<span class="ar-word{extra_cls}" data-idx="{word_idx}" data-tashkeel="{word}" data-plain="{plain_w}" data-trans="{trans_safe}" onclick="selectWord(this, \'{word}\', \'{trans_safe}\')">{word}</span>{trailing}')
                        word_idx += 1

        formatted_lines.append(' '.join(line_html_parts))

    return f'<div class="arabic-article" id="p-{p_id}">' + '<br><br>'.join(formatted_lines) + '</div>'

def generate_unit_page(u_num):
    u = data_units.UNITS_DATA[u_num]

    # Load timecodes for this unit
    unit_timecodes = {}
    timecodes_path = 'audio/paragraphs_timecodes.json'
    if os.path.exists(timecodes_path):
        with open(timecodes_path, 'r', encoding='utf-8') as f_tc:
            all_tc = json.load(f_tc)
        for p in u['paragraphs']:
            pid = p['id']
            unit_timecodes[pid] = all_tc.get(f"u{u_num}_para_{pid}", [])
    unit_tc_json = json.dumps(unit_timecodes, ensure_ascii=False)
    timecodes_script = f'''<script>
    const UNIT_TIMECODES = {unit_tc_json};
  </script>'''
    total_units = 8
    
    # Breadcrumbs & Navigation
    prev_link = f'<a href="unit{u_num-1}.html" class="nav-link">← Глава {u_num-1}</a>' if u_num > 1 else ''
    next_link = f'<a href="unit{u_num+1}.html" class="nav-link">Глава {u_num+1} →</a>' if u_num < total_units else ''
    
    # Audio elements
    audio_elements = [
        f'<audio id="audioMaster" src="audio/u{u_num}_full.mp3?v=husary_v3" preload="metadata"></audio>'
    ]
    for p in u['paragraphs']:
        p_id = p['id']
        audio_elements.append(f'<audio id="audioPara{p_id}" src="audio/u{u_num}_para_{p_id}.mp3?v=husary_v3" preload="metadata"></audio>')
    audio_elements_html = '\n  '.join(audio_elements)
    
    # Paragraph cards
    para_cards_html = []
    for p in u['paragraphs']:
        p_id = p['id']
        p_title_ar = p['title_ar']
        p_title_ru = p['title_ru']
        body_html = format_paragraph_text_html(p['text'], p_id, u, u_num=u_num)
        
        card = f'''
      <div class="para-card" id="paraCard{p_id}">
        <div class="para-card-topbar">
          <div class="para-badge-pill" data-tashkeel="الْفِقْرَةُ {p_id}" data-plain="الفقرة {p_id}">
            <span class="para-badge-icon">📖</span>
            <span class="para-badge-text">الْفِقْرَةُ {p_id}</span>
          </div>
          <div class="para-actions">
            <button class="para-play-btn" id="btnPlayPara{p_id}" onclick="toggleParaAudio({p_id})" title="Слушать абзац">
              <span class="play-icon">▶</span>
              <span class="play-text">Слушать абзац</span>
            </button>
            <button class="action-icon-btn" onclick="copyParagraph({p_id})" title="Скопировать арабский текст">📋</button>
          </div>
        </div>

        <div class="para-title-center">
          <h3 class="para-title" data-tashkeel="{p_title_ar}" data-plain="{strip_tashkeel(p_title_ar)}">{p_title_ar}</h3>
          <div class="para-subtitle-ru">{p_title_ru}</div>
        </div>

        {body_html}
      </div>'''
        para_cards_html.append(card)
        
    paragraphs_section_html = '\n'.join(para_cards_html)
    
    # Verbs table rows
    verbs_rows = []
    for v in u.get('verbs', []):
        m_ar = v.get('masdar', '')
        pr_ar = v.get('present', '')
        pa_ar = v.get('past', '')
        v_meaning = v.get('meaning', '').replace('"', '&quot;').replace("'", "&#39;")
        verbs_rows.append(f'''
              <tr>
                <td class="ru-cell">{v.get('meaning', '')}</td>
                <td class="ar-cell"><span class="ar-word" data-tashkeel="{m_ar}" data-plain="{strip_tashkeel(m_ar)}" data-trans="{v_meaning} (масдар)" onclick="selectWord(this, \'{m_ar}\', \'{v_meaning} (масдар)\')">{m_ar}</span></td>
                <td class="ar-cell"><span class="ar-word" data-tashkeel="{pr_ar}" data-plain="{strip_tashkeel(pr_ar)}" data-trans="{v_meaning} (наст.)" onclick="selectWord(this, \'{pr_ar}\', \'{v_meaning} (наст.)\')">{pr_ar}</span></td>
                <td class="ar-cell"><span class="ar-word" data-tashkeel="{pa_ar}" data-plain="{strip_tashkeel(pa_ar)}" data-trans="{v_meaning} (прош.)" onclick="selectWord(this, \'{pa_ar}\', \'{v_meaning} (прош.)\')">{pa_ar}</span></td>
                <td><span class="badge-form">{v.get('form', '')}</span></td>
                <td style="text-align:center;"><button class="speak-btn" onclick="playWordAudio('{pa_ar}')">🔊</button></td>
              </tr>''')
    verbs_rows_html = ''.join(verbs_rows)
    
    # Collocations cards
    colloc_cards = []
    for c in u.get('collocations', []):
        ph_ar = c.get('phrase', '')
        ph_meaning = c.get('meaning', '').replace('"', '&quot;').replace("'", "&#39;")
        colloc_cards.append(f'''
          <div class="collocation-card">
            <div>
              <div class="colloc-ar">
                <span class="ar-word" data-tashkeel="{ph_ar}" data-plain="{strip_tashkeel(ph_ar)}" data-trans="{ph_meaning}" onclick="selectWord(this, \'{ph_ar}\', \'{ph_meaning}\')">{ph_ar}</span>
                <button class="speak-btn" onclick="playWordAudio('{clean_word(ph_ar)}')">🔊</button>
              </div>
              <div class="colloc-ru">{c.get('meaning', '')}</div>
            </div>
            <div class="colloc-note">{c.get('note', '')}</div>
          </div>''')
    colloc_cards_html = ''.join(colloc_cards)
    
    # Vocabulary table rows
    vocab_rows = []
    for voc in u.get('vocabulary', []):
        pl_ar = voc.get('plural', '')
        w_ar = voc.get('word', '')
        w_meaning = voc.get('meaning', '').replace('"', '&quot;').replace("'", "&#39;")
        pl_meaning = f"мн. от {w_ar}: {w_meaning}" if pl_ar else w_meaning
        vocab_rows.append(f'''
              <tr>
                <td class="ru-cell">{voc.get('meaning', '')}</td>
                <td class="ar-cell"><span class="ar-word" data-tashkeel="{pl_ar}" data-plain="{strip_tashkeel(pl_ar)}" data-trans="{pl_meaning}" onclick="selectWord(this, \'{pl_ar}\', \'{pl_meaning}\')">{pl_ar}</span></td>
                <td class="ar-cell"><span class="ar-word" data-tashkeel="{w_ar}" data-plain="{strip_tashkeel(w_ar)}" data-trans="{w_meaning}" onclick="selectWord(this, \'{w_ar}\', \'{w_meaning}\')">{w_ar}</span></td>
                <td><span class="badge-form">{voc.get('pos', '')}</span></td>
                <td style="text-align:center;"><button class="speak-btn" onclick="playWordAudio('{clean_word(w_ar)}')">🔊</button></td>
              </tr>''')
    vocab_rows_html = ''.join(vocab_rows)
    vocab_rows_html = ''.join(vocab_rows)
    
    # Assemble full page HTML
    page_html = f'''<!DOCTYPE html>
<html lang="ru" dir="ltr">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>العربية بين يديك — الكتاب الثالث | الوحدة {u_num}: {u['title_ar']}</title>
  
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Amiri+Quran&family=Amiri:ital,wght@0,400;0,700;1,400&family=Plus+Jakarta+Sans:wght@400;500;600;700&family=Scheherazade+New:wght@400;600;700&display=swap" rel="stylesheet">

  <!-- PWA Manifest & Mobile Meta -->
  <link rel="manifest" href="manifest.json">
  <meta name="theme-color" content="#1b4332">
  <meta name="mobile-web-app-capable" content="yes">
  <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
  <meta name="apple-mobile-web-app-title" content="العربية">
  <link rel="apple-touch-icon" href="icons/icon-192.png">

  {css_full}
</head>
<body>

  {audio_elements_html}

  <!-- Sticky Top Navigation -->
  <nav class="top-nav">
    <div class="nav-breadcrumbs">
      <a href="index.html" class="nav-link">📖 Оглавление курса</a>
      <span>›</span>
      <span style="color:var(--text-muted);">الوحدة {u_num}: {u['title_ar']}</span>
      {prev_link}
      {next_link}
    </div>
    <div class="nav-controls">
      <button class="pwa-install-btn" id="pwaInstallBtn" onclick="promptInstallPWA()" title="Установить приложение на устройство">📲 Установить</button>
      <button class="btn" onclick="toggleTheme()" id="themeBtn" title="Переключить тему">🌓 Тема</button>
      <button class="btn" onclick="toggleTashkeel()" id="tashkeelBtn" title="Включить / отключить огласовки (ташкель)">🔤 Ташкиль: Вкл</button>
      <select id="fontSelect" class="btn" onchange="changeArabicFont(this.value)" style="cursor:pointer;" title="Шрифт текста">
        <option value="'Amiri', serif">Шрифт: Amiri</option>
        <option value="'Scheherazade New', serif">Шрифт: Scheherazade</option>
        <option value="'Amiri Quran', serif">Шрифт: Amiri Quran</option>
      </select>
      <button class="btn" onclick="adjustFontSize(-2)" title="Уменьшить шрифт">A−</button>
      <button class="btn" onclick="adjustFontSize(2)" title="Увеличить шрифт">A+</button>
    </div>
  </nav>

  <!-- Header Hero -->
  <header>
    <div class="header-badge">العربية بين يديك • الكتاب الثالث • الوحدة {u_num} (الدرس {u['lesson_num']})</div>
    <h1>الوحدة {u_num}: {u['title_ar']}</h1>
    <p style="font-weight:700; color:#ffffff; font-size:1.15rem; margin-top:0.4rem;">{u['title_ru']}</p>
    <p style="max-width:850px; margin:0.5rem auto 0; font-size:0.95rem; line-height:1.6; color:rgba(255,255,255,0.85);">{u['desc_ru']}</p>
  </header>

  <!-- Global Master Audio Player Banner -->
  <div class="audio-player-banner">
    <div class="player-left">
      <button class="play-master-btn" id="masterPlayBtn" onclick="toggleMasterAudio()" title="Воспроизвести всю главу">▶</button>
      <div class="player-info">
        <span class="player-title">Аудиозапись всего урока (Полный текст главы {u_num})</span>
        <span class="player-subtitle">Академический диктор (Хамид) • Имя Аллаха: шейх Мухаммад Сиддик аль-Миншави • Аяты: шейх аль-Хусари • Студийный саляват</span>
      </div>
    </div>

    <div class="player-controls">
      <button class="btn" onclick="seekAudio(-10)" title="Назад на 10 секунд">↺ 10с</button>
      <div class="player-progress-bar" id="masterProgressBar" onclick="seekMasterAudio(event)">
        <div class="player-progress-fill" id="masterProgressFill"></div>
      </div>
      <button class="btn" onclick="seekAudio(10)" title="Вперед на 10 секунд">10с ↻</button>
      <span class="player-time" id="masterTimeDisplay">0:00 / 0:00</span>
    </div>

    <div style="display: flex; align-items: center; gap: 0.5rem; flex-wrap: wrap;">
      <div style="display: flex; align-items: center; gap: 0.35rem;">
        <span style="font-size:0.8rem; font-weight:600; color:var(--text-muted);">Скорость:</span>
        <select class="btn" id="speedSelect" onchange="changePlaybackRate(this.value)" style="padding:0.25rem 0.5rem; font-size:0.8rem;">
          <option value="0.8">0.8x</option>
          <option value="0.9">0.9x</option>
          <option value="1.0" selected>1.0x</option>
          <option value="1.1">1.1x</option>
          <option value="1.25">1.25x</option>
        </select>
      </div>

      <div style="display: flex; align-items: center; gap: 0.35rem;" title="Точная калибровка синхронизации звука и караоке (компенсация задержки аудио)">
        <span style="font-size:0.8rem; font-weight:600; color:var(--text-muted);">Караоке:</span>
        <select class="btn" id="syncOffsetSelect" onchange="changeSyncOffset(this.value)" style="padding:0.25rem 0.5rem; font-size:0.8rem;">
          <option value="0">0мс (без смещения)</option>
          <option value="100">+100мс</option>
          <option value="140">+140мс</option>
          <option value="180" selected>Точно (+180мс)</option>
          <option value="220">+220мс (наушники)</option>
          <option value="260">+260мс (Bluetooth)</option>
        </select>
      </div>
    </div>
  </div>

  <!-- Main Container -->
  <div class="container">
    
    <!-- Paragraphs Section -->
    <div style="margin-bottom: 2.5rem;">
      {paragraphs_section_html}
    </div>

    <!-- Verbs Section -->
    <div class="card" style="margin-bottom: 2rem;">
      <div class="card-header">
        <div class="card-title">
          <span>⚡ Таблица глаголов текста в основных формах</span>
        </div>
        <div class="card-title-ar">جَدْوَلُ الْأَفْعَالِ</div>
      </div>

      <div class="filter-box">
        <input type="text" id="verbSearch" class="search-input" placeholder="🔍 Поиск глагола (по-русски или по-арабски)..." onkeyup="filterVerbs()">
      </div>

      <div class="table-container">
        <table id="verbsTable">
          <thead>
            <tr>
              <th style="width: 25%;">Значение (Русский)</th>
              <th style="width: 17%;">الْمَصْدَرُ (Масдар)</th>
              <th style="width: 17%;">الْمُضَارِعُ (Наст.-буд.)</th>
              <th style="width: 17%;">الْمَاضِي (Прошедшее)</th>
              <th style="width: 14%;">Порода</th>
              <th style="width: 10%; text-align:center;">Озвучка</th>
            </tr>
          </thead>
          <tbody>
            {verbs_rows_html}
          </tbody>
        </table>
      </div>
    </div>

    <!-- Collocations Section -->
    <div class="card" style="margin-bottom: 2rem;">
      <div class="card-header">
        <div class="card-title">
          <span>💎 Трудные обороты, идиомы и устойчивые выражения</span>
        </div>
        <div class="card-title-ar">التَّرَاكِيبُ وَالْعِبَارَاتُ الِاصْطِلَاحِيَّةُ</div>
      </div>

      <div class="collocations-grid">
        {colloc_cards_html}
      </div>
    </div>

    <!-- Vocabulary Section -->
    <div class="card" style="margin-bottom: 4rem;">
      <div class="card-header">
        <div class="card-title">
          <span>📚 Словарь урока: единственное и множественное число</span>
        </div>
        <div class="card-title-ar">الْمُفْرَدَاتُ وَالْجَمْعُ</div>
      </div>

      <div class="filter-box">
        <input type="text" id="vocabSearch" class="search-input" placeholder="🔍 Поиск слова (по-русски или по-арабски)..." onkeyup="filterVocab()">
      </div>

      <div class="table-container">
        <table id="vocabTable">
          <thead>
            <tr>
              <th style="width: 30%;">Значение (Русский)</th>
              <th style="width: 25%;">الْجَمْعُ (Множественное)</th>
              <th style="width: 25%;">الْمُفْرَدُ (Единственное)</th>
              <th style="width: 10%;">Часть речи</th>
              <th style="width: 10%; text-align:center;">Озвучка</th>
            </tr>
          </thead>
          <tbody>
            {vocab_rows_html}
          </tbody>
        </table>
      </div>
    </div>

  </div>

  <!-- Live Tooltip Element -->
  <div id="tooltip"></div>

  <!-- Fixed Bottom Inspector Bar -->
  <div class="inspector-bar" id="inspectorBar">
    <div class="inspector-content">
      <span class="inspector-context">Выбранное слово</span>
      <span class="inspector-ar" id="inspAr">{clean_word(u['paragraphs'][0]['title_ar'].split()[0])}</span>
      <span class="inspector-ru" id="inspRu">Нажмите на любое слово для перевода и озвучки</span>
    </div>
    <div style="display:flex; gap:0.5rem; align-items:center;">
      <button class="btn btn-active" onclick="speakCurrentWord()">🔊 Озвучить слово</button>
      <button class="btn" onclick="hideInspector()">✕</button>
    </div>
  </div>

  <!-- Unit Paragraph Timecodes & Engine -->
  {timecodes_script}
  {js_template}
</body>
</html>'''

    out_file = f'unit{u_num}.html'
    with open(out_file, 'w', encoding='utf-8') as f:
        f.write(page_html)
    print(f"Generated {out_file} successfully! Size: {len(page_html)}")

def generate_index_page():
    total_units = 8
    cards_html = []
    
    for u_num in range(1, total_units + 1):
        u = data_units.UNITS_DATA[u_num]
        card = f'''
        <div class="unit-card">
          <div>
            <div class="unit-card-header">
              <span class="unit-card-badge">الوحدة {u_num} • الدرس {u['lesson_num']}</span>
              <span class="unit-card-icon">{u['icon']}</span>
            </div>
            <div class="unit-card-title-ar">{u['title_ar']}</div>
            <div class="unit-card-title-ru">{u['title_ru']}</div>
            <div class="unit-card-desc">{u['desc_ru']}</div>
            
            <div class="unit-card-features">
              <span class="unit-feature-pill">📖 {len(u['paragraphs'])} тематических текстов</span>
              <span class="unit-feature-pill">⚡ {len(u['verbs'])} глаголов с породами</span>
              <span class="unit-feature-pill">💎 {len(u['collocations'])} идиом</span>
              <span class="unit-feature-pill">📚 {len(u['vocabulary'])} словарных статей</span>
              <span class="unit-feature-pill">🎵 Аудио Хамид & аль-Хусари</span>
            </div>
          </div>

          <div class="unit-card-actions">
            <button class="btn-preview-audio" onclick="playIndexPreview('audio/u{u_num}_full.mp3?v=husary_v3', this)" title="Слушать превью урока">▶ Аудио</button>
            <a href="unit{u_num}.html" class="btn-open-unit">
              <span>Изучать главу</span>
              <span>→</span>
            </a>
          </div>
        </div>'''
        cards_html.append(card)
        
    cards_grid_html = '\n'.join(cards_html)
    
    index_html = f'''<!DOCTYPE html>
<html lang="ru" dir="ltr">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>العربية بين يديك — الكتاب الثالث | فهرس الوحدات (Оглавление)</title>
  
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Amiri+Quran&family=Amiri:ital,wght@0,400;0,700;1,400&family=Plus+Jakarta+Sans:wght@400;500;600;700&family=Scheherazade+New:wght@400;600;700&display=swap" rel="stylesheet">

  <!-- PWA Manifest & Mobile Meta -->
  <link rel="manifest" href="manifest.json">
  <meta name="theme-color" content="#1b4332">
  <meta name="mobile-web-app-capable" content="yes">
  <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
  <meta name="apple-mobile-web-app-title" content="العربية">
  <link rel="apple-touch-icon" href="icons/icon-192.png">

  {css_full}
</head>
<body>

  <!-- Hidden Preview Audio Player -->
  <audio id="indexAudioPreview" preload="none"></audio>

  <!-- Sticky Top Navigation -->
  <nav class="top-nav">
    <div class="nav-breadcrumbs">
      <span style="font-family:var(--arabic-font); font-size:1.35rem; color:var(--primary); font-weight:bold;">العربية بين يديك</span>
      <span>•</span>
      <span style="color:var(--text); font-weight:700;">الكتاب الثالث (الجزء الأول)</span>
    </div>
    <div class="nav-controls">
      <button class="pwa-install-btn" id="pwaInstallBtn" onclick="promptInstallPWA()" title="Установить приложение на устройство">📲 Установить</button>
      <button class="btn" onclick="toggleTheme()" id="themeBtn" title="Переключить тему">🌓 Тема</button>
      <select id="fontSelect" class="btn" onchange="changeArabicFont(this.value)" style="cursor:pointer;" title="Шрифт текста">
        <option value="'Amiri', serif">Шрифт: Amiri</option>
        <option value="'Scheherazade New', serif">Шрифт: Scheherazade</option>
        <option value="'Amiri Quran', serif">Шрифт: Amiri Quran</option>
      </select>
    </div>
  </nav>

  <!-- Portal Hero -->
  <div class="portal-hero">
    <div class="header-badge" style="margin-bottom:0.75rem;">سِلْسِلَةُ تَعْلِيمِ اللُّغَةِ الْعَرَبِيَّةِ لِغَيْرِ النَّاطِقِينَ بِهَا</div>
    <h1>الْعَرَبِيَّةُ بَيْنَ يَدَيْك — الْكِتَابُ الثَّالِثُ</h1>
    <div class="subhead">Интерактивный учебный курс • Полное оглавление (Главы 1–8)</div>
    <p class="desc">
      Инновационный интерактивный портал для углубленного изучения арабского языка: полный текст всех глав с огласовками, 
      профессиональное саудовское озвучивание диктором Хамидом, чтение имени Аллаха шейхом Мухаммадом Сиддиком аль-Миншави, чтение коранических аятов шейхом Махмудом Халилем аль-Хусари, 
      аутентичные студийные салаваты Пророку ﷺ, пословный интерактивный перевод по наведению и таблицы форм глаголов.
    </p>

    <!-- Stats Row -->
    <div class="stats-row">
      <div class="stat-badge">📖 8 Полных учебных глав</div>
      <div class="stat-badge">🎙️ 48 Озвученных тем и абзацев</div>
      <div class="stat-badge">⚡ 120+ Глаголов с породами и масдарами</div>
      <div class="stat-badge">🔊 1000+ Озвученных слов и терминов</div>
      <div class="stat-badge">✨ 100% Академический чистый фусха</div>
    </div>
  </div>

  <!-- Units Grid Container -->
  <div class="container">
    <div class="units-grid">
      {cards_grid_html}
    </div>
  </div>

  <script>
    // Theme toggle
    function toggleTheme() {{
      const current = document.documentElement.getAttribute('data-theme') || 'light';
      const target = current === 'dark' ? 'light' : 'dark';
      document.documentElement.setAttribute('data-theme', target);
      localStorage.setItem('site_theme', target);
      const btn = document.getElementById('themeBtn');
      if (btn) btn.textContent = target === 'dark' ? '☀️ Светлая' : '🌙 Тёмная';
    }}

    // Restore theme on load
    const savedTheme = localStorage.getItem('site_theme') || 'light';
    document.documentElement.setAttribute('data-theme', savedTheme);
    window.addEventListener('DOMContentLoaded', () => {{
      const btn = document.getElementById('themeBtn');
      if (btn) btn.textContent = savedTheme === 'dark' ? '☀️ Светлая' : '🌙 Тёмная';
    }});

    // Font family change
    function changeArabicFont(fontFamily) {{
      document.documentElement.style.setProperty('--arabic-font', fontFamily);
      localStorage.setItem('site_ar_font', fontFamily);
    }}

    const savedFont = localStorage.getItem('site_ar_font');
    if (savedFont) {{
      document.documentElement.style.setProperty('--arabic-font', savedFont);
      window.addEventListener('DOMContentLoaded', () => {{
        const select = document.getElementById('fontSelect');
        if (select) select.value = savedFont;
      }});
    }}

    // Preview audio player
    let currentPreviewBtn = null;
    const previewAudio = document.getElementById('indexAudioPreview');

    function playIndexPreview(audioSrc, btn) {{
      if (previewAudio.src.endsWith(audioSrc) && !previewAudio.paused) {{
        previewAudio.pause();
        btn.textContent = '▶ Аудио';
        currentPreviewBtn = null;
      }} else {{
        if (currentPreviewBtn) {{
          currentPreviewBtn.textContent = '▶ Аудио';
        }}
        previewAudio.src = audioSrc;
        previewAudio.play().then(() => {{
          btn.textContent = '⏸ Пауза';
          currentPreviewBtn = btn;
        }}).catch(e => console.error(e));
      }}
    }}

    previewAudio.addEventListener('ended', () => {{
      if (currentPreviewBtn) {{
        currentPreviewBtn.textContent = '▶ Аудио';
        currentPreviewBtn = null;
      }}
    }});

    // PWA Service Worker & Install Prompt
    let deferredPrompt = null;
    if ('serviceWorker' in navigator) {{
      window.addEventListener('load', () => {{
        navigator.serviceWorker.register('./sw.js')
          .then(reg => console.log('[PWA] Service Worker registered with scope:', reg.scope))
          .catch(err => console.log('[PWA] Service Worker registration failed:', err));
      }});
    }}

    window.addEventListener('beforeinstallprompt', (e) => {{
      e.preventDefault();
      deferredPrompt = e;
      const btn = document.getElementById('pwaInstallBtn');
      if (btn) btn.style.display = 'inline-flex';
    }});

    function promptInstallPWA() {{
      if (deferredPrompt) {{
        deferredPrompt.prompt();
        deferredPrompt.userChoice.then((choiceResult) => {{
          if (choiceResult.outcome === 'accepted') {{
            const btn = document.getElementById('pwaInstallBtn');
            if (btn) btn.style.display = 'none';
          }}
          deferredPrompt = null;
        }});
      }}
    }}
  </script>
</body>
</html>'''

    with open('index.html', 'w', encoding='utf-8') as f:
        f.write(index_html)
    print(f"Generated index.html successfully! Size: {len(index_html)}")

def main():
    for u in range(1, 9):
        generate_unit_page(u)
    generate_index_page()
    print("ALL 9 PAGES GENERATED SUCCESSFULLY!")

if __name__ == '__main__':
    main()
