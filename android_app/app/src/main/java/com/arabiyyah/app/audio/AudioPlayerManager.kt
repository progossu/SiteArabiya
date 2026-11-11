package com.arabiyyah.app.audio

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.speech.tts.TextToSpeech
import android.util.Log
import com.arabiyyah.app.data.JsonDataLoader
import com.arabiyyah.app.data.WordTiming
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * Type of audio currently active in the player.
 */
enum class AudioType {
    NONE,
    PARAGRAPH,
    MASTER,
    QURAN,
    WORD
}

/**
 * Robust Android Audio Playback Engine for the Arabiyyah curriculum.
 *
 * Features:
 * - Paragraph audio playback with toggle pause/resume.
 * - Precise karaoke word sync via exact WordTiming timecodes with linear fallback.
 * - Master chapter full audio playback with seeking (-10s / +10s / scrub).
 * - Word click-to-pronounce audio playback with offline dictionary map & TTS fallback.
 * - Quran recitation playback by Sheikh Mahmoud Khalil Al-Husary with ayah resolution.
 * - Speed control (0.8x, 1.0x, 1.25x).
 * - Real-time word highlighting loop for smooth karaoke display in Jetpack Compose.
 * - Exposes reactive StateFlows for seamless Compose UI integration.
 */
class AudioPlayerManager(context: Context) {

    private val appContext: Context = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        private const val TAG = "AudioPlayerManager"

        /**
         * Latency compensation offset (ms) aligning visual highlight with acoustic output.
         * Accounts for hardware audio buffer delay and TTS phoneme planning markers.
         */
        const val SYNC_LATENCY_OFFSET_MS = 180L

        val AVAILABLE_SPEEDS = listOf(0.8f, 1.0f, 1.25f)

        /**
         * Quran Ayah audio mappings for Sheikh Mahmoud Khalil Al-Husary recitations.
         */
        val QURAN_AUDIO_MAP: Map<String, String> = mapOf(
            "الفرقان/٣٢" to "audio/quran/furqan_32.mp3",
            "الحجر/٩" to "audio/quran/hijr_9.mp3",
            "الإسراء/٨٨" to "audio/quran/isra_88.mp3",
            "النحل/٨٩" to "audio/quran/nahl_89.mp3",
            "الشعراء/١٩٣" to "audio/quran/shuara_193_195.mp3",
            "الشعراء/١٩٣-١٩٥" to "audio/quran/shuara_193_195.mp3",
            "النحل/٤٤" to "audio/quran/nahl_44.mp3",
            "النجم/٣-٤" to "audio/quran/najm_3_4.mp3",
            "الحشر/٧" to "audio/quran/hashr_7.mp3",
            "النساء/٨٠" to "audio/quran/nisa_80.mp3",
            "آل عمران/٣١" to "audio/quran/ali_imran_31.mp3",
            "الأحزاب/٣٦" to "audio/quran/ahzab_36.mp3",
            "النساء/٦٥" to "audio/quran/nisa_65.mp3",
            "العلق/١" to "audio/quran/alaq_1.mp3",
            "البقرة/٢٥٥" to "audio/quran/baqarah_255_clip.mp3",
            "الأنعام/٦٠" to "audio/quran/anam_60.mp3",
            "الذاريات/١٧" to "audio/quran/dhariyat_17.mp3",
            "النبأ/٩-١١" to "audio/quran/naba_9_11.mp3",
            "الشعراء/٢٢٤" to "audio/quran/shuara_224.mp3"
        )
    }

    // Media players & AssetFileDescriptors
    private var primaryPlayer: MediaPlayer? = null
    private var primaryAfd: AssetFileDescriptor? = null
    private var wordPlayer: MediaPlayer? = null
    private var wordAfd: AssetFileDescriptor? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady: Boolean = false

    private fun closePrimaryAfd() {
        try {
            primaryAfd?.close()
        } catch (_: Exception) {}
        primaryAfd = null
    }

    private fun closeWordAfd() {
        try {
            wordAfd?.close()
        } catch (_: Exception) {}
        wordAfd = null
    }

    // Progress polling job
    private var progressJob: Job? = null

    // Cache of words map: Arabic text -> filename (e.g. "319b293663.mp3")
    private var wordsMap: Map<String, String> = emptyMap()

    // Preloaded paragraph word timecodes: key -> List<WordTiming>
    @Volatile
    private var paragraphTimingsMap: Map<String, List<WordTiming>> = emptyMap()

    // Active paragraph word timecodes for currently playing paragraph
    @Volatile
    private var activeParagraphTimings: List<WordTiming> = emptyList()

    // Internal state tracking
    private var currentTotalWords: Int = 0
    private var onWordHighlightListener: ((Int) -> Unit)? = null

    // ---------------------------------------------------------
    // Exposed StateFlows for Jetpack Compose UI
    // ---------------------------------------------------------
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _activeWordIndex = MutableStateFlow(-1)
    val activeWordIndex: StateFlow<Int> = _activeWordIndex.asStateFlow()

    private val _activeQuranIndex = MutableStateFlow(-1)
    val activeQuranIndex: StateFlow<Int> = _activeQuranIndex.asStateFlow()

    private val _currentTimings = MutableStateFlow<List<WordTiming>>(emptyList())
    val currentTimings: StateFlow<List<WordTiming>> = _currentTimings.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _currentAudioType = MutableStateFlow(AudioType.NONE)
    val currentAudioType: StateFlow<AudioType> = _currentAudioType.asStateFlow()

    private val _currentParagraphId = MutableStateFlow<Int?>(null)
    val currentParagraphId: StateFlow<Int?> = _currentParagraphId.asStateFlow()

    private val _currentUnitNum = MutableStateFlow<Int?>(null)
    val currentUnitNum: StateFlow<Int?> = _currentUnitNum.asStateFlow()

    private val _currentQuranKey = MutableStateFlow<String?>(null)
    val currentQuranKey: StateFlow<String?> = _currentQuranKey.asStateFlow()

    private val _currentWord = MutableStateFlow<String?>(null)
    val currentWord: StateFlow<String?> = _currentWord.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    init {
        // Initialize TextToSpeech as robust fallback
        try {
            tts = TextToSpeech(appContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale("ar"))
                    isTtsReady = result != TextToSpeech.LANG_MISSING_DATA &&
                            result != TextToSpeech.LANG_NOT_SUPPORTED
                } else {
                    isTtsReady = false
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "TTS initialization failed: ${e.message}")
        }

        // Preload words audio map in background
        scope.launch(Dispatchers.IO) {
            try {
                wordsMap = JsonDataLoader.loadWordsMap(appContext)
                Log.d(TAG, "Loaded ${wordsMap.size} words in audio map")
            } catch (e: Exception) {
                Log.w(TAG, "Could not preload words_map.json: ${e.message}")
            }
        }

        // Preload paragraph word timecodes in background
        scope.launch(Dispatchers.IO) {
            try {
                paragraphTimingsMap = JsonDataLoader.loadParagraphTimingsMap(appContext)
                Log.d(TAG, "Loaded ${paragraphTimingsMap.size} paragraph timings")
            } catch (e: Exception) {
                Log.w(TAG, "Could not preload paragraphs_timecodes.json: ${e.message}")
            }
        }
    }

    /**
     * Updates or injects the timecodes mapping.
     */
    fun setParagraphTimingsMap(timingsMap: Map<String, List<WordTiming>>) {
        paragraphTimingsMap = timingsMap
    }

    /**
     * Reloads paragraph timecodes from assets.
     */
    fun reloadParagraphTimings() {
        scope.launch(Dispatchers.IO) {
            try {
                paragraphTimingsMap = JsonDataLoader.loadParagraphTimingsMap(appContext, useCache = false)
                Log.d(TAG, "Reloaded ${paragraphTimingsMap.size} paragraph timings")
            } catch (e: Exception) {
                Log.w(TAG, "reloadParagraphTimings failed: ${e.message}")
            }
        }
    }

    /**
     * Returns the active WordTiming list for the current paragraph.
     */
    fun getActiveParagraphTimings(): List<WordTiming> = activeParagraphTimings

    // ---------------------------------------------------------
    // Timecode progress callback setup
    // ---------------------------------------------------------
    /**
     * Registers a callback invoked during playback whenever the current active
     * word index in the playing paragraph changes.
     */
    fun setOnWordHighlightListener(listener: ((wordIndex: Int) -> Unit)?) {
        this.onWordHighlightListener = listener
    }

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                val player = primaryPlayer
                if (player != null && _isPlaying.value) {
                    try {
                        val pos = player.currentPosition.toLong()
                        val dur = player.duration.toLong()
                        _currentPosition.value = pos
                        if (dur > 0L) {
                            _duration.value = dur

                            // Karaoke word & Quran highlighting calculation with millisecond precision
                            if (_currentAudioType.value == AudioType.PARAGRAPH) {
                                updateActiveKaraokeIndices(pos, dur)
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore transient MediaPlayer state issues
                    }
                }
                delay(20L) // 50 updates per second for seamless real-time sync
            }
        }
    }

    /**
     * Calculates the active word and Quran indices based on exact timecodes with
     * acoustic latency compensation (180ms lead time), matching the website algorithm.
     */
    fun updateActiveKaraokeIndices(pos: Long, dur: Long) {
        val timings = activeParagraphTimings
        if (timings.isNotEmpty()) {
            val speed = _playbackSpeed.value
            // Offsetting effectivePos aligns visual highlight with acoustic sound hitting the ear
            val effectivePos = Math.max(0L, pos - (SYNC_LATENCY_OFFSET_MS * speed).toLong())

            var matchedWord = -1
            var matchedQuran = -1

            for (i in timings.indices) {
                val tc = timings[i]
                val nextTc = timings.getOrNull(i + 1)
                // Allow a graceful hold of up to 100ms or until next segment starts
                val maxHoldMs = if (nextTc != null) minOf(tc.endMs + 100L, nextTc.startMs) else tc.endMs + 100L

                if (effectivePos in tc.startMs..maxHoldMs) {
                    if (tc.isQuran) {
                        matchedQuran = if (tc.quranIndex >= 0) tc.quranIndex else 0
                    } else {
                        matchedWord = tc.wordIndex
                    }
                    break
                }
            }

            if (_activeWordIndex.value != matchedWord) {
                _activeWordIndex.value = matchedWord
                onWordHighlightListener?.invoke(matchedWord)
            }
            if (_activeQuranIndex.value != matchedQuran) {
                _activeQuranIndex.value = matchedQuran
            }
            return
        }

        // Linear interpolation fallback when exact timecodes are not available
        if (currentTotalWords > 0 && dur > 0L) {
            val progress = (pos.toDouble() / dur.toDouble()).coerceIn(0.0, 1.0)
            val calculated = (progress * currentTotalWords).toInt().coerceIn(0, currentTotalWords - 1)
            if (_activeWordIndex.value != calculated) {
                _activeWordIndex.value = calculated
                onWordHighlightListener?.invoke(calculated)
            }
        }
    }

    /**
     * Backward-compatible active word index calculator.
     */
    fun calculateActiveWordIndex(pos: Long, dur: Long): Int {
        val timings = activeParagraphTimings
        if (timings.isNotEmpty()) {
            val speed = _playbackSpeed.value
            val effectivePos = Math.max(0L, pos - (SYNC_LATENCY_OFFSET_MS * speed).toLong())
            for (i in timings.indices) {
                val tc = timings[i]
                val nextTc = timings.getOrNull(i + 1)
                val maxHoldMs = if (nextTc != null) minOf(tc.endMs + 100L, nextTc.startMs) else tc.endMs + 100L
                if (effectivePos in tc.startMs..maxHoldMs && !tc.isQuran) {
                    return tc.wordIndex
                }
            }
            return -1
        }
        if (currentTotalWords > 0 && dur > 0L) {
            val progress = (pos.toDouble() / dur.toDouble()).coerceIn(0.0, 1.0)
            return (progress * currentTotalWords).toInt().coerceIn(0, currentTotalWords - 1)
        }
        return -1
    }

    private fun stopProgressLoop() {
        progressJob?.cancel()
        progressJob = null
    }

    // ---------------------------------------------------------
    // Paragraph Audio Playback
    // ---------------------------------------------------------
    /**
     * Plays or pauses audio for a specific paragraph in a unit.
     * Calculates real-time word highlighting based on exact timecodes or [totalWords].
     */
    fun toggleParagraph(
        unitNum: Int,
        paragraphId: Int,
        totalWords: Int = 0,
        timings: List<WordTiming>? = null
    ) {
        if (_currentAudioType.value == AudioType.PARAGRAPH &&
            _currentUnitNum.value == unitNum &&
            _currentParagraphId.value == paragraphId
        ) {
            if (_isPlaying.value) {
                pause()
            } else {
                resume()
            }
            return
        }

        playParagraph(unitNum, paragraphId, totalWords, timings)
    }

    /**
     * Plays audio for a specific paragraph with exact karaoke word timecodes.
     */
    fun playParagraph(
        unitNum: Int,
        paragraphId: Int,
        totalWords: Int = 0,
        timings: List<WordTiming>? = null
    ) {
        stopAll(retainSpeed = true)

        val assetPath = "audio/u${unitNum}_para_${paragraphId}.mp3"
        currentTotalWords = totalWords

        // Resolve timings for this paragraph
        activeParagraphTimings = timings ?: resolveParagraphTimings(unitNum, paragraphId)
        _currentTimings.value = activeParagraphTimings

        _currentAudioType.value = AudioType.PARAGRAPH
        _currentUnitNum.value = unitNum
        _currentParagraphId.value = paragraphId
        _activeWordIndex.value = if (activeParagraphTimings.isNotEmpty() || totalWords > 0) 0 else -1

        playMediaSource(assetPath)
    }

    /**
     * Resolves exact word timecodes for a unit paragraph.
     */
    fun resolveParagraphTimings(unitNum: Int, paragraphId: Int): List<WordTiming> {
        if (paragraphTimingsMap.isNotEmpty()) {
            val fromMap = JsonDataLoader.getTimingsFromMap(paragraphTimingsMap, unitNum, paragraphId)
            if (fromMap.isNotEmpty()) return fromMap
        }
        return JsonDataLoader.getParagraphTimings(appContext, unitNum, paragraphId)
    }

    fun pauseParagraph() {
        if (_currentAudioType.value == AudioType.PARAGRAPH && _isPlaying.value) {
            pause()
        }
    }

    fun resumeParagraph() {
        if (_currentAudioType.value == AudioType.PARAGRAPH && !_isPlaying.value) {
            resume()
        }
    }

    // ---------------------------------------------------------
    // Master Full Chapter Audio Playback
    // ---------------------------------------------------------
    /**
     * Toggles playback of the complete audio reading of a unit.
     */
    fun toggleMaster(unitNum: Int) {
        if (_currentAudioType.value == AudioType.MASTER && _currentUnitNum.value == unitNum) {
            if (_isPlaying.value) {
                pause()
            } else {
                resume()
            }
            return
        }
        playMaster(unitNum)
    }

    /**
     * Plays the full chapter master audio for [unitNum].
     */
    fun playMaster(unitNum: Int) {
        stopAll(retainSpeed = true)

        val assetPath = "audio/u${unitNum}_full.mp3"
        currentTotalWords = 0
        activeParagraphTimings = emptyList()
        _currentTimings.value = emptyList()

        _currentAudioType.value = AudioType.MASTER
        _currentUnitNum.value = unitNum
        _currentParagraphId.value = null
        _activeWordIndex.value = -1
        _activeQuranIndex.value = -1

        playMediaSource(assetPath)
    }

    /**
     * Seeks playback position to absolute [positionMs].
     */
    fun seekTo(positionMs: Long) {
        try {
            primaryPlayer?.let { player ->
                val target = positionMs.coerceIn(0L, _duration.value.coerceAtLeast(0L))
                player.seekTo(target.toInt())
                _currentPosition.value = target
                if (_currentAudioType.value == AudioType.PARAGRAPH) {
                    val idx = calculateActiveWordIndex(target, _duration.value)
                    _activeWordIndex.value = idx
                    onWordHighlightListener?.invoke(idx)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "seekTo failed: ${e.message}")
        }
    }

    /**
     * Seeks playback to the start timestamp of the word with index [wordIndex].
     */
    fun seekToWord(wordIndex: Int): Boolean {
        val timing = activeParagraphTimings.find { it.wordIndex == wordIndex }
        return if (timing != null) {
            seekTo(timing.startMs)
            true
        } else {
            false
        }
    }

    /**
     * Seeks relatively by [deltaMs] (e.g. -10000L or +10000L).
     */
    fun seekBy(deltaMs: Long) {
        val current = _currentPosition.value
        seekTo(current + deltaMs)
    }

    // ---------------------------------------------------------
    // Quran Recitation Playback (Sheikh Al-Husary)
    // ---------------------------------------------------------
    /**
     * Toggles Sheikh Mahmoud Khalil Al-Husary's recitation for the specified ayah.
     * Ayah can be referenced by Arabic key like "الفرقان/٣٢" or surah name/number.
     */
    fun toggleQuran(ayahRef: String, customPath: String? = null) {
        if (_currentAudioType.value == AudioType.QURAN && _currentQuranKey.value == ayahRef) {
            if (_isPlaying.value) {
                pause()
            } else {
                resume()
            }
            return
        }
        playQuran(ayahRef, customPath)
    }

    /**
     * Plays Sheikh Mahmoud Khalil Al-Husary recitation.
     */
    fun playQuran(ayahRef: String, customPath: String? = null) {
        stopAll(retainSpeed = true)

        val resolvedPath = customPath ?: resolveQuranAudioPath(ayahRef)
        if (resolvedPath == null) {
            Log.w(TAG, "Quran audio not found for reference: $ayahRef")
            return
        }

        currentTotalWords = 0
        activeParagraphTimings = emptyList()
        _currentTimings.value = emptyList()

        _currentAudioType.value = AudioType.QURAN
        _currentQuranKey.value = ayahRef
        _currentParagraphId.value = null
        _activeWordIndex.value = -1
        _activeQuranIndex.value = -1

        playMediaSource(resolvedPath)
    }

    fun stopQuran() {
        if (_currentAudioType.value == AudioType.QURAN) {
            stop()
        }
    }

    /**
     * Resolves an ayah reference string to its corresponding MP3 asset path.
     */
    fun resolveQuranAudioPath(ayahRef: String): String? {
        val trimmed = ayahRef.trim()

        // 1. Exact match in map
        QURAN_AUDIO_MAP[trimmed]?.let { return it }

        // 2. Normalize Arabic digits to Western or vice versa
        val normalized = normalizeDigits(trimmed)
        for ((key, path) in QURAN_AUDIO_MAP) {
            if (normalizeDigits(key) == normalized) {
                return path
            }
        }

        // 3. Substring match
        for ((key, path) in QURAN_AUDIO_MAP) {
            if (key.contains(trimmed) || trimmed.contains(key)) {
                return path
            }
        }

        return null
    }

    // ---------------------------------------------------------
    // Word Click-to-Pronounce Audio Playback
    // ---------------------------------------------------------
    /**
     * Plays pronunciation of a clicked word or phrase.
     *
     * Lookup priority:
     * 1. Direct checks for Salawat (ﷺ), Allah with case vowels, Radiyallahu anhu.
     * 2. Dictionary lookup in [wordsMap].
     * 3. Built-in Android TextToSpeech (Arabic) fallback.
     */
    fun playWord(rawWord: String, onCompletion: (() -> Unit)? = null) {
        val word = rawWord.trim()
        val cleaned = cleanWordLookup(word)
        _currentWord.value = word

        val audioFileName = resolveWordAudioFile(word, cleaned)

        if (audioFileName != null) {
            playWordFile(audioFileName, onCompletion)
        } else {
            // TextToSpeech fallback
            speakViaTts(cleaned.ifEmpty { word }, onCompletion)
        }
    }

    fun stopWord() {
        try {
            wordPlayer?.stop()
            wordPlayer?.reset()
        } catch (e: Exception) {
            // Ignore
        }
        if (isTtsReady) {
            try {
                tts?.stop()
            } catch (e: Exception) {
                // Ignore
            }
        }
        _currentWord.value = null
    }

    private fun resolveWordAudioFile(word: String, cleaned: String): String? {
        // 1. Direct check for Salawat
        if (word == "ﷺ" || cleaned == "ﷺ" || word.contains("ﷺ") ||
            word.contains("صَلَّى") || cleaned.contains("صلى")
        ) {
            return "salawat_human.mp3"
        }

        // 2. Direct check for Allah with specific vowels (preferring Sheikh Husary's files)
        if (word.contains("اللَّهِ") || word.contains("اللهِ")) {
            return if (wordsMap.containsKey("اللهِ")) wordsMap["اللهِ"] else "h_allah_kasra.mp3"
        }
        if (word.contains("اللَّهَ") || word.contains("اللهَ")) {
            return if (wordsMap.containsKey("اللهَ")) wordsMap["اللهَ"] else "h_allah_fatha.mp3"
        }
        if (word == "اللهُ" || word == "اللَّهُ" || word == "الله" || cleaned == "الله") {
            return if (wordsMap.containsKey("اللهُ")) wordsMap["اللهُ"] else "h_allah_damma.mp3"
        }

        // 3. Direct check for Radiyallahu anhu / anha
        if ((word.contains("عَنْهُ") || word.contains("عَنْهَا")) &&
            (word.contains("رَضِيَ") || word.contains("رضي"))
        ) {
            return "radiyallahu_anhu.mp3"
        }

        // 4. Map lookup
        if (wordsMap.isNotEmpty()) {
            wordsMap[word]?.let { return it }
            wordsMap[cleaned]?.let { return it }
        }

        return null
    }

    private fun playWordFile(fileName: String, onCompletion: (() -> Unit)?) {
        try {
            closeWordAfd()
            wordPlayer?.reset()
            val player = wordPlayer ?: MediaPlayer().also { wordPlayer = it }
            player.reset()

            val attributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .build()
            player.setAudioAttributes(attributes)

            val fullAssetPath = "audio/words/$fileName"
            val fallbackAssetPath = "words/$fileName"

            val sourceLoaded = setDataSourceForPlayer(player, fullAssetPath, isPrimary = false) ||
                    setDataSourceForPlayer(player, fallbackAssetPath, isPrimary = false) ||
                    setDataSourceForPlayer(player, fileName, isPrimary = false)

            if (!sourceLoaded) {
                Log.w(TAG, "Audio asset not found for word file: $fileName, using TTS")
                speakViaTts(_currentWord.value ?: fileName, onCompletion)
                return
            }

            player.setOnPreparedListener { mp ->
                mp.start()
            }
            player.setOnCompletionListener {
                closeWordAfd()
                _currentWord.value = null
                onCompletion?.invoke()
            }
            player.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "wordPlayer error: what=$what, extra=$extra")
                closeWordAfd()
                speakViaTts(_currentWord.value ?: "", onCompletion)
                true
            }
            player.prepareAsync()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing word audio file: ${e.message}")
            closeWordAfd()
            speakViaTts(_currentWord.value ?: "", onCompletion)
        }
    }

    private fun speakViaTts(text: String, onCompletion: (() -> Unit)?) {
        if (!isTtsReady || text.isBlank()) {
            onCompletion?.invoke()
            return
        }
        try {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "WORD_TTS")
            scope.launch {
                delay(800L)
                _currentWord.value = null
                onCompletion?.invoke()
            }
        } catch (e: Exception) {
            Log.e(TAG, "TTS speak failed: ${e.message}")
            onCompletion?.invoke()
        }
    }

    // ---------------------------------------------------------
    // Speed Control
    // ---------------------------------------------------------
    /**
     * Sets playback speed (e.g. 0.8f, 1.0f, 1.25f).
     */
    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        primaryPlayer?.let { player ->
            if (_isPlaying.value) {
                applySpeedToPlayer(player, speed)
            }
        }
    }

    /**
     * Cycles through available playback speeds: 0.8x -> 1.0x -> 1.25x -> 0.8x.
     */
    fun cyclePlaybackSpeed(): Float {
        val current = _playbackSpeed.value
        val next = when {
            current < 0.95f -> 1.0f
            current < 1.15f -> 1.25f
            else -> 0.8f
        }
        setPlaybackSpeed(next)
        return next
    }

    // ---------------------------------------------------------
    // General Playback Controls
    // ---------------------------------------------------------
    fun pause() {
        try {
            primaryPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                    _isPlaying.value = false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "pause failed: ${e.message}")
        }
    }

    fun resume() {
        try {
            primaryPlayer?.let { player ->
                applySpeedToPlayer(player, _playbackSpeed.value)
                player.start()
                _isPlaying.value = true
                startProgressLoop()
            }
        } catch (e: Exception) {
            Log.e(TAG, "resume failed: ${e.message}")
        }
    }

    fun stop() {
        stopAll(retainSpeed = true)
    }

    private fun stopAll(retainSpeed: Boolean = true) {
        stopProgressLoop()
        closePrimaryAfd()
        closeWordAfd()
        try {
            primaryPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.reset()
            }
        } catch (e: Exception) {
            Log.e(TAG, "stop failed: ${e.message}")
        }

        _isPlaying.value = false
        _currentPosition.value = 0L
        _duration.value = 0L
        _activeWordIndex.value = -1
        _activeQuranIndex.value = -1
        _currentAudioType.value = AudioType.NONE
        _currentParagraphId.value = null
        _currentQuranKey.value = null
        _isBuffering.value = false
        currentTotalWords = 0
        activeParagraphTimings = emptyList()
        _currentTimings.value = emptyList()

        if (!retainSpeed) {
            _playbackSpeed.value = 1.0f
        }
    }

    /**
     * Fully releases all resources (call when ViewModel or Activity is destroyed).
     */
    fun release() {
        stopProgressLoop()
        scope.cancel()
        closePrimaryAfd()
        closeWordAfd()

        try {
            primaryPlayer?.release()
            primaryPlayer = null
        } catch (e: Exception) {
            // Ignore
        }

        try {
            wordPlayer?.release()
            wordPlayer = null
        } catch (e: Exception) {
            // Ignore
        }

        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {
            // Ignore
        }
    }

    // ---------------------------------------------------------
    // Robust MediaPlayer Data Source Helper
    // ---------------------------------------------------------
    private fun playMediaSource(path: String) {
        _isBuffering.value = true

        try {
            closePrimaryAfd()
            primaryPlayer?.reset()
            val player = primaryPlayer ?: MediaPlayer().also { primaryPlayer = it }
            player.reset()

            val attributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
            player.setAudioAttributes(attributes)

            val sourceLoaded = setDataSourceForPlayer(player, path, isPrimary = true)
            if (!sourceLoaded) {
                Log.w(TAG, "Unable to load data source for path: $path")
                _isBuffering.value = false
                _isPlaying.value = false
                return
            }

            player.setOnPreparedListener { mp ->
                _isBuffering.value = false
                _duration.value = mp.duration.toLong()
                applySpeedToPlayer(mp, _playbackSpeed.value)
                mp.start()
                _isPlaying.value = true
                startProgressLoop()
            }

            player.setOnCompletionListener {
                closePrimaryAfd()
                _isPlaying.value = false
                _currentPosition.value = 0L
                _activeWordIndex.value = -1
        _activeQuranIndex.value = -1
                _currentParagraphId.value = null
                _currentQuranKey.value = null
                _currentAudioType.value = AudioType.NONE
                activeParagraphTimings = emptyList()
                _currentTimings.value = emptyList()
                stopProgressLoop()
            }

            player.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "primaryPlayer error: what=$what, extra=$extra")
                closePrimaryAfd()
                _isBuffering.value = false
                _isPlaying.value = false
                stopProgressLoop()
                true
            }

            player.prepareAsync()
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating playback for $path: ${e.message}")
            closePrimaryAfd()
            _isBuffering.value = false
            _isPlaying.value = false
        }
    }

    /**
     * Resolves and sets data source for a MediaPlayer from local filesystem,
     * network URL, or Android assets (including handling compressed asset fallbacks).
     */
    private fun setDataSourceForPlayer(player: MediaPlayer, path: String, isPrimary: Boolean = true): Boolean {
        if (isPrimary) {
            closePrimaryAfd()
        } else {
            closeWordAfd()
        }

        // 1. Direct local file check
        val localFile = File(path)
        if (localFile.exists() && localFile.canRead()) {
            player.setDataSource(localFile.absolutePath)
            return true
        }

        // 2. HTTP/HTTPS URL
        if (path.startsWith("http://", ignoreCase = true) ||
            path.startsWith("https://", ignoreCase = true)
        ) {
            player.setDataSource(path)
            return true
        }

        // 3. Android Assets candidates
        val candidates = listOf(
            path,
            if (path.startsWith("audio/")) path.removePrefix("audio/") else "audio/$path"
        )

        for (candidate in candidates) {
            // Try AssetFileDescriptor (fastest, uncompressed assets)
            try {
                val afd = appContext.assets.openFd(candidate)
                if (isPrimary) {
                    primaryAfd = afd
                } else {
                    wordAfd = afd
                }
                player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                return true
            } catch (e: Exception) {
                // If openFd fails (compressed asset or subfolder), fall back to streaming
            }

            // Fallback: Stream from assets into temporary cache file
            try {
                val safeName = candidate.replace('/', '_').replace('\\', '_')
                val cacheFile = File(appContext.cacheDir, "audio_${safeName}")
                if (!cacheFile.exists() || cacheFile.length() == 0L) {
                    appContext.assets.open(candidate).use { input ->
                        FileOutputStream(cacheFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                if (cacheFile.exists() && cacheFile.length() > 0L) {
                    player.setDataSource(cacheFile.absolutePath)
                    return true
                }
            } catch (e: Exception) {
                // Not found in this candidate
            }
        }

        return false
    }

    private fun applySpeedToPlayer(player: MediaPlayer, speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val params = player.playbackParams
                params.speed = speed
                player.playbackParams = params
            } catch (e: Exception) {
                Log.w(TAG, "Could not set playback speed: ${e.message}")
            }
        }
    }

    private fun cleanWordLookup(text: String): String {
        return text.replace(Regex("[.,:;()\\[\\]{}«»\"'—؟!]"), "").trim()
    }

    private fun normalizeDigits(input: String): String {
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        var result = input
        for (i in 0..9) {
            result = result.replace(arabicDigits[i], ('0' + i))
        }
        return result
    }
}
