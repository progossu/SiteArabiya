package com.arabiyyah.app.ui.components

import androidx.compose.runtime.compositionLocalOf
import com.arabiyyah.app.audio.AudioPlayerManager

val LocalAudioPlayerManager = compositionLocalOf<AudioPlayerManager> {
    error("AudioPlayerManager not provided")
}

/**
 * Formats milliseconds into mm:ss format.
 */
fun formatTimeMs(millis: Long): String {
    if (millis <= 0L) return "00:00"
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

/**
 * Strips only Arabic diacritical marks (harakat/tashkeel: fatha, damma, kasra, sukun, shadda, tanween)
 * while keeping all Arabic letter shapes (alif with hamza, ta marbuta, alif maqsura, etc.) completely intact.
 * Used for toggling reading with or without vocalization.
 */
fun stripDiacritics(text: String): String {
    val diacriticsRegex = Regex("[\\u0617-\\u061A\\u064B-\\u065F\\u0670]")
    return text.replace(diacriticsRegex, "")
}

/**
 * Strips Arabic tashkeel (harakat / diacritics) for fuzzy search matching.
 */
fun removeTashkeel(text: String): String {
    val tashkeelRegex = Regex("[\\u0617-\\u061A\\u064B-\\u0652\\u0670]")
    return text.replace(tashkeelRegex, "")
        .replace("أ", "ا")
        .replace("إ", "ا")
        .replace("آ", "ا")
        .replace("ة", "ه")
        .replace("ى", "ي")
}

/**
 * Cleans punctuation from an Arabic word for dictionary lookup and audio playback.
 */
fun cleanArabicWord(raw: String): String {
    return raw.trim()
        .replace(Regex("[،.؟!:;\"'()\\[\\]{}«»﴿﴾_~*^]"), "")
        .trim()
}
