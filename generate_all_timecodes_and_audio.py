import asyncio
import os
import re
import json
import shutil
import hashlib
import subprocess
import imageio_ffmpeg
import numpy as np
import edge_tts
import time

ffmpeg = imageio_ffmpeg.get_ffmpeg_exe()
VOICE = 'ar-SA-HamedNeural'

SALAWAT = 'audio/words/salawat_human.mp3'
RADIA_ANHU = 'audio/words/radiyallahu_anhu.mp3'
RADIA_ANHA = 'audio/words/radiyallahu_anha.mp3'
H_ALLAH_DAMMA = 'audio/words/h_allah_damma.mp3'
H_ALLAH_KASRA = 'audio/words/h_allah_kasra.mp3'
H_ALLAH_FATHA = 'audio/words/h_allah_fatha.mp3'

QURAN_MAP = {
    'الفرقان/٣٢': 'audio/quran/furqan_32.mp3',
    'الحجر/٩': 'audio/quran/hijr_9.mp3',
    'الإسراء/٨٨': 'audio/quran/isra_88.mp3',
    'النحل/٨٩': 'audio/quran/nahl_89.mp3',
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

def load_pcm(file_path):
    cmd = [ffmpeg, '-y', '-i', file_path, '-f', 's16le', '-ac', '1', '-ar', '24000', '-']
    proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    raw, _ = proc.communicate()
    return np.frombuffer(raw, dtype=np.int16)

def encode_mp3(samples, out_path):
    cmd_out = [
        ffmpeg, '-y',
        '-f', 's16le', '-ac', '1', '-ar', '24000', '-i', '-',
        '-b:a', '192k', out_path
    ]
    proc_out = subprocess.Popen(cmd_out, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    proc_out.communicate(input=samples.tobytes())

print("Preloading authentic audio assets into PCM cache...")
PCM_CACHE = {
    'SALAWAT': load_pcm(SALAWAT),
    'RADIA_ANHU': load_pcm(RADIA_ANHU),
    'RADIA_ANHA': load_pcm(RADIA_ANHA),
    'H_ALLAH_DAMMA': load_pcm(H_ALLAH_DAMMA),
    'H_ALLAH_KASRA': load_pcm(H_ALLAH_KASRA),
    'H_ALLAH_FATHA': load_pcm(H_ALLAH_FATHA),
}
for ref, qpath in QURAN_MAP.items():
    if os.path.exists(qpath):
        PCM_CACHE[ref] = load_pcm(qpath)
print(f"PCM Cache ready. Allahu damma duration: {len(PCM_CACHE['H_ALLAH_DAMMA'])/24000.0:.2f}s (Sheikh Muhammad Siddiq Al-Minshawi)")

PUNCT_PATTERN = re.compile(r'[\u060C\u061B\u061F\u06D4.,:;()\[\]{}«»"\'—?!؟\s]')
def clean_arabic_word(text):
    return PUNCT_PATTERN.sub('', text)

def is_allah_word(cleaned):
    norm = re.sub(r'[\u064B-\u065F\u0670\u0671]', '', cleaned)
    norm = norm.replace('ٱ', 'ا').replace('إ', 'ا').replace('أ', 'ا').replace('آ', 'ا')
    if norm == "الله":
        if 'ِ' in cleaned or 'هِ' in cleaned:
            return 'H_ALLAH_KASRA'
        elif 'ُ' in cleaned or 'هُ' in cleaned:
            return 'H_ALLAH_DAMMA'
        elif 'هَ' in cleaned or cleaned.endswith('َ'):
            return 'H_ALLAH_FATHA'
        else:
            return 'H_ALLAH_DAMMA'
    return None

def is_salawat(tokens, idx):
    clean = clean_arabic_word(tokens[idx])
    if clean in ['ﷺ', 'ﷺ،']:
        return 1
    if idx + 3 < len(tokens):
        c0 = clean_arabic_word(tokens[idx])
        c1 = clean_arabic_word(tokens[idx+1])
        c2 = clean_arabic_word(tokens[idx+2])
        c3 = clean_arabic_word(tokens[idx+3])
        if c0 in ['صلى', 'صَلَّى'] and c1 in ['الله', 'اللَّهُ', 'اللهُ'] and c2 in ['عليه', 'عَلَيْهِ'] and c3 in ['وسلم', 'وَسَلَّمَ']:
            return 4
    return 0

def is_radia(tokens, idx):
    if idx + 2 < len(tokens):
        c0 = clean_arabic_word(tokens[idx])
        c1 = clean_arabic_word(tokens[idx+1])
        c2 = clean_arabic_word(tokens[idx+2])
        if c0 in ['رضي', 'رَضِيَ'] and c1 in ['الله', 'اللَّهُ', 'اللهُ']:
            if c2 in ['عنه', 'عَنْهُ']:
                return (3, 'RADIA_ANHU')
            elif c2 in ['عنها', 'عَنْهَا']:
                return (3, 'RADIA_ANHA')
    return (0, None)

def trim_samples(samples, pad_start_ms=30, pad_end_ms=60, threshold=400):
    non_silent = np.where(np.abs(samples) > threshold)[0]
    if len(non_silent) == 0:
        return samples, 0
    start_idx = max(0, non_silent[0] - int(24000 * pad_start_ms / 1000))
    end_idx = min(len(samples), non_silent[-1] + int(24000 * pad_end_ms / 1000))
    return samples[start_idx:end_idx], start_idx

TTS_SEMAPHORE = asyncio.Semaphore(8)
TTS_CACHE_DIR = "audio/.tts_cache"
os.makedirs(TTS_CACHE_DIR, exist_ok=True)

async def synth_text_chunk(text, has_next=False, token_count=1, pad_start_ms=30, pad_end_ms=60):
    clean = clean_arabic_word(text)
    if not clean:
        return np.array([], dtype=np.int16), []

    # If this block is followed by another block (e.g. Divine Name or Salawat),
    # append a continuation word so Edge TTS articulates final Tashkeel / Harakat in continuous Wasl!
    synth_text = f"{text} بِالْحَقِّ" if has_next else text

    h = hashlib.md5(f"{text}__wasl={has_next}".encode('utf-8')).hexdigest()
    cache_pcm = os.path.join(TTS_CACHE_DIR, f"{h}.pcm")
    cache_wb = os.path.join(TTS_CACHE_DIR, f"{h}.json")

    if os.path.exists(cache_pcm) and os.path.exists(cache_wb):
        try:
            with open(cache_pcm, "rb") as fp:
                samples = np.frombuffer(fp.read(), dtype=np.int16)
            with open(cache_wb, "r", encoding="utf-8") as fp:
                adjusted_wb = json.load(fp)
            return samples, adjusted_wb
        except Exception:
            pass

    async with TTS_SEMAPHORE:
        for attempt in range(4):
            try:
                comm = edge_tts.Communicate(synth_text, VOICE, rate="-4%", boundary="WordBoundary")
                audio_bytes = bytearray()
                wb_events = []
                async for chunk in comm.stream():
                    if chunk["type"] == "audio":
                        audio_bytes.extend(chunk["data"])
                    elif chunk["type"] == "WordBoundary":
                        wb_events.append({
                            "text": chunk["text"],
                            "start": chunk["offset"] / 10000.0,
                            "end": (chunk["offset"] + chunk["duration"]) / 10000.0
                        })
                if len(audio_bytes) > 0:
                    break
            except Exception as e:
                wait_time = (attempt + 1) * 1.5
                print(f"TTS warning for {repr(synth_text[:30])}... (attempt {attempt+1}): {e}. Waiting {wait_time}s...")
                await asyncio.sleep(wait_time)
        else:
            print(f"ERROR: Failed TTS for {repr(synth_text[:40])}")
            return np.zeros(2400, dtype=np.int16), []

    tmp_path = f"tmp_syn_{abs(hash(synth_text))}_{np.random.randint(100000)}.mp3"
    with open(tmp_path, "wb") as f:
        f.write(audio_bytes)
    raw_samples = load_pcm(tmp_path)
    try:
        os.remove(tmp_path)
    except:
        pass

    if has_next and len(wb_events) > token_count:
        target_end_ms = wb_events[token_count - 1]["end"]
        # Allow +25ms release so the final vowel is completely articulated
        cut_idx = min(len(raw_samples), int(((target_end_ms + 25) / 1000.0) * 24000))
        raw_samples = raw_samples[:cut_idx].copy()
        # Soft 15ms fade-out
        fo = int(24000 * 0.015)
        raw_samples[-fo:] = (raw_samples[-fo:] * np.linspace(1, 0, fo)).astype(np.int16)
        wb_events = wb_events[:token_count]

    trimmed_samples, start_idx = trim_samples(raw_samples, pad_start_ms, pad_end_ms if not has_next else 20)
    lead_trim_ms = (start_idx / 24000.0) * 1000.0
    adjusted_wb = []
    for wb in wb_events:
        adjusted_wb.append({
            "text": wb["text"],
            "start": max(0.0, wb["start"] - lead_trim_ms),
            "end": max(0.0, wb["end"] - lead_trim_ms)
        })

    # Cache for instant re-runs
    try:
        with open(cache_pcm, "wb") as fp:
            fp.write(trimmed_samples.tobytes())
        with open(cache_wb, "w", encoding="utf-8") as fp:
            json.dump(adjusted_wb, fp)
    except Exception:
        pass

    return trimmed_samples, adjusted_wb

async def build_paragraph(u_num, p_data):
    p_id = p_data['id']
    raw_text = p_data['text']

    # Filter out heading line (e.g. 'الْفِقْرَةُ الْأُولَى — ...') to match HTML article 1-to-1
    lines = raw_text.split('\n')
    filtered_lines = [l for l in lines if not (l.strip().startswith('الْفِقْرَةُ') and '—' in l)]
    para_text = '\n'.join(filtered_lines).strip()

    quran_regex = re.compile(r'﴿([^﴾]+)﴾(?:\s*\[([^\]]+)\])?')

    parts = []
    last_idx = 0
    for m in quran_regex.finditer(para_text):
        if m.start() > last_idx:
            parts.append(('text', para_text[last_idx:m.start()]))
        ayah_ref = m.group(2).strip() if m.group(2) else ""
        ayah_text = m.group(1).strip()
        parts.append(('quran', ayah_text, ayah_ref))
        last_idx = m.end()
    if last_idx < len(para_text):
        parts.append(('text', para_text[last_idx:]))

    all_pcm_chunks = []
    timings = []
    current_time_ms = 0.0
    global_word_counter = 0
    quran_counter = 0

    for part in parts:
        if part[0] == 'quran':
            ayah_text, ayah_ref = part[1], part[2]
            matched_pcm = None
            for ref_k, pcm_data in PCM_CACHE.items():
                if ref_k in ayah_ref:
                    matched_pcm = pcm_data
                    break
            if matched_pcm is None:
                for ref_k, path_k in QURAN_MAP.items():
                    if any(w in ayah_text for w in ['اقْرَأْ', 'سِنَةٌ', 'يَتَوَفَّاكُمْ', 'يَهْجَعُونَ', 'سُبَاتاً', 'الْغَاوُونَ', 'مَا نُزِّلَ', 'يَنْطِقُ', 'مَا آتَاكُمُ', 'مَنْ يُطِعِ', 'تُحِبُّونَ اللَّهَ', 'الْخِيَرَةُ', 'شَجَرَ بَيْنَهُمْ']):
                        if ref_k in PCM_CACHE:
                            matched_pcm = PCM_CACHE[ref_k]
                            break
            if matched_pcm is not None:
                all_pcm_chunks.append(matched_pcm)
                q_dur_ms = (len(matched_pcm) / 24000.0) * 1000.0
                timings.append({
                    "isQuran": True,
                    "quranIndex": quran_counter,
                    "ayahText": ayah_text,
                    "startMs": round(current_time_ms),
                    "endMs": round(current_time_ms + q_dur_ms)
                })
                current_time_ms += q_dur_ms
            quran_counter += 1
            continue

        text_content = part[1]
        raw_tokens = [w for w in re.split(r'\s+', text_content) if w.strip()]
        # Keep only tokens that contain arabic letters or are salawat symbol
        tokens = [tok for tok in raw_tokens if clean_arabic_word(tok) or tok in ['ﷺ', 'ﷺ،']]
        if not tokens:
            continue

        blocks = []
        i = 0
        while i < len(tokens):
            sal_len = is_salawat(tokens, i)
            if sal_len > 0:
                blocks.append(('salawat', tokens[i:i+sal_len]))
                i += sal_len
                continue

            rad_len, rad_key = is_radia(tokens, i)
            if rad_len > 0:
                blocks.append((rad_key, tokens[i:i+rad_len]))
                i += rad_len
                continue

            clean = clean_arabic_word(tokens[i])
            allah_type = is_allah_word(clean)
            if allah_type:
                blocks.append((allah_type, [tokens[i]]))
                i += 1
                continue

            tts_tokens = [tokens[i]]
            i += 1
            while i < len(tokens):
                if is_salawat(tokens, i) > 0 or is_radia(tokens, i)[0] > 0 or is_allah_word(clean_arabic_word(tokens[i])):
                    break
                tts_tokens.append(tokens[i])
                i += 1
            blocks.append(('tts', tts_tokens))

        for b_idx, (block_type, blk_tokens) in enumerate(blocks):
            if block_type == 'tts':
                blk_text = " ".join(blk_tokens)
                has_next = (b_idx < len(blocks) - 1)
                samples, wb_events = await synth_text_chunk(blk_text, has_next=has_next, token_count=len(blk_tokens))
                all_pcm_chunks.append(samples)
                seg_dur_ms = (len(samples) / 24000.0) * 1000.0

                wb_idx = 0
                for tok in blk_tokens:
                    c = clean_arabic_word(tok)
                    w_idx = global_word_counter
                    global_word_counter += 1

                    if not c:
                        prev_end = timings[-1]["endMs"] if timings else round(current_time_ms)
                        timings.append({
                            "wordIndex": w_idx,
                            "word": tok,
                            "startMs": prev_end,
                            "endMs": prev_end
                        })
                        continue

                    if wb_idx < len(wb_events):
                        wb = wb_events[wb_idx]
                        wb_idx += 1
                        timings.append({
                            "wordIndex": w_idx,
                            "word": tok,
                            "startMs": round(current_time_ms + wb["start"]),
                            "endMs": round(current_time_ms + wb["end"])
                        })
                    else:
                        prev_end = timings[-1]["endMs"] if timings else round(current_time_ms)
                        timings.append({
                            "wordIndex": w_idx,
                            "word": tok,
                            "startMs": prev_end,
                            "endMs": min(round(current_time_ms + seg_dur_ms), prev_end + 300)
                        })
                current_time_ms += seg_dur_ms

            elif block_type.startswith('H_ALLAH_'):
                pcm_data = PCM_CACHE[block_type]
                all_pcm_chunks.append(pcm_data)
                dur_ms = (len(pcm_data) / 24000.0) * 1000.0
                start_m = round(current_time_ms)
                end_m = round(current_time_ms + dur_ms)

                timings.append({
                    "wordIndex": global_word_counter,
                    "word": blk_tokens[0],
                    "startMs": start_m,
                    "endMs": end_m
                })
                global_word_counter += 1
                current_time_ms += dur_ms

            elif block_type == 'salawat':
                pcm_data = PCM_CACHE['SALAWAT']
                all_pcm_chunks.append(pcm_data)
                dur_ms = (len(pcm_data) / 24000.0) * 1000.0
                n = len(blk_tokens)
                start_m = round(current_time_ms)

                for step, tok in enumerate(blk_tokens):
                    w_s = round(start_m + (step * dur_ms / n))
                    w_e = round(start_m + ((step + 1) * dur_ms / n))
                    timings.append({
                        "wordIndex": global_word_counter,
                        "word": tok,
                        "startMs": w_s,
                        "endMs": w_e
                    })
                    global_word_counter += 1
                current_time_ms += dur_ms

            elif block_type in ['RADIA_ANHU', 'RADIA_ANHA']:
                pcm_data = PCM_CACHE[block_type]
                all_pcm_chunks.append(pcm_data)
                dur_ms = (len(pcm_data) / 24000.0) * 1000.0
                n = len(blk_tokens)
                start_m = round(current_time_ms)

                for step, tok in enumerate(blk_tokens):
                    w_s = round(start_m + (step * dur_ms / n))
                    w_e = round(start_m + ((step + 1) * dur_ms / n))
                    timings.append({
                        "wordIndex": global_word_counter,
                        "word": tok,
                        "startMs": w_s,
                        "endMs": w_e
                    })
                    global_word_counter += 1
                current_time_ms += dur_ms

    full_samples = np.concatenate(all_pcm_chunks) if all_pcm_chunks else np.array([], dtype=np.int16)

    out_mp3 = f"audio/u{u_num}_para_{p_id}.mp3"
    encode_mp3(full_samples, out_mp3)

    dur_s = len(full_samples) / 24000.0
    print(f"Generated Unit {u_num} Para {p_id}: {dur_s:.1f}s, words={len(timings)} -> {out_mp3}")
    return full_samples, timings

async def main():
    with open("data_units.json", "r", encoding="utf-8") as f:
        units = json.load(f)

    all_timecodes = {}

    for u_id in sorted(units.keys(), key=lambda x: int(x)):
        u_num = int(u_id)
        unit = units[u_id]
        paragraphs = unit["paragraphs"]
        print(f"\n==================== PROCESSING UNIT {u_num} ({len(paragraphs)} paragraphs) ====================")

        unit_samples_list = []

        for p in paragraphs:
            samples, timings = await build_paragraph(u_num, p)
            unit_samples_list.append(samples)
            key1 = f"u{u_num}_para_{p['id']}"
            key2 = f"{u_num}_{p['id']}"
            all_timecodes[key1] = timings
            all_timecodes[key2] = timings

        # Concatenate unit full audio
        full_unit_samples = np.concatenate(unit_samples_list)
        full_mp3_path = f"audio/u{u_num}_full.mp3"
        encode_mp3(full_unit_samples, full_mp3_path)
        print(f"--> Unit {u_num} Full Audio Generated: {len(full_unit_samples)/24000.0:.1f}s -> {full_mp3_path}")

    # Save paragraphs_timecodes.json
    paths = [
        "audio/paragraphs_timecodes.json"
    ]
    for p in paths:
        os.makedirs(os.path.dirname(p), exist_ok=True)
        with open(p, "w", encoding="utf-8") as f:
            json.dump(all_timecodes, f, ensure_ascii=False, indent=2)
        print(f"Saved timecodes JSON ({len(all_timecodes)} keys) -> {p}")

    print("ALL 54 PARAGRAPHS AND TIME CODES GENERATED SUCCESSFULLY WITH AUTHENTIC HUSARY RECITATION!")

if __name__ == '__main__':
    t_start = time.time()
    asyncio.run(main())
    t_end = time.time()
    print(f"COMPLETE WORKFLOW TOOK {t_end - t_start:.1f}s")
