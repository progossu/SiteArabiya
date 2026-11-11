import asyncio
import edge_tts
import imageio_ffmpeg
import subprocess
import numpy as np
import os
import json

ffmpeg = imageio_ffmpeg.get_ffmpeg_exe()

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

async def synth_word_full_ending(word, out_path):
    # Synthesize with continuation word so Edge TTS articulates final Tashkeel / Harakat!
    clean_w = word.strip().rstrip('.,:;«»"\'—?!؟')
    text = f"{clean_w} بِالْحَقِّ"
    comm = edge_tts.Communicate(text, "ar-SA-HamedNeural", rate="-4%", boundary="WordBoundary")
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

    tmp = f"tmp_w_{abs(hash(word))}.mp3"
    with open(tmp, "wb") as f: f.write(audio_bytes)
    pcm = load_pcm(tmp)
    try: os.remove(tmp)
    except: pass

    # Find where the word ends (could be multiple tokens if compound, e.g. "غَسْلُ الْمَيِّتِ وَتَكْفِينُهُ")
    w_tokens = [t for t in clean_w.split() if t.strip()]
    target_idx = min(len(wb_events) - 1, len(w_tokens) - 1)
    if target_idx < 0:
        return

    w_end_ms = wb_events[target_idx]["end"]
    cut_idx = min(len(pcm), int(((w_end_ms + 30) / 1000.0) * 24000))
    clip = pcm[:cut_idx].copy()
    fo = int(24000 * 0.02)
    clip[-fo:] = (clip[-fo:] * np.linspace(1, 0, fo)).astype(np.int16)

    encode_mp3(clip, out_path)
    print(f"Fixed ending: {word:25} -> {out_path} ({len(clip)/24000.0:.2f}s)")

async def main():
    with open('audio/words_map.json', 'r', encoding='utf-8') as f:
        wmap = json.load(f)

    for w, fname in wmap.items():
        # Check words ending with dammat-ul-haa
        if any(w.endswith(sfx) for sfx in ['هُ', 'هُ،', 'هُ.', 'هُ»']):
            if 'الله' in w or 'اللَّه' in w:
                continue # handled by Minshawi!
            out_p = os.path.join('audio/words', fname)
            await synth_word_full_ending(w, out_p)

asyncio.run(main())
