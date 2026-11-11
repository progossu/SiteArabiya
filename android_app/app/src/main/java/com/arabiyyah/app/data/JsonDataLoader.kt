package com.arabiyyah.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import android.util.Log
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Master glossary containing exact token-level and lemma-level translations.
 */
@Serializable
data class GlossaryData(
    @SerializedName("by_key")
    @SerialName("by_key")
    val byKey: Map<String, String> = emptyMap(),

    @SerializedName("words")
    @SerialName("words")
    val words: Map<String, String> = emptyMap()
)

/**
 * Exact timecode timing for an individual word in a paragraph audio.
 */
@Serializable
data class WordTiming(
    @SerializedName(value = "wordIndex", alternate = ["word_index", "index", "i"])
    @SerialName("wordIndex")
    val wordIndex: Int = 0,

    @SerializedName(value = "word", alternate = ["text", "w"])
    @SerialName("word")
    val word: String = "",

    @SerializedName(value = "startMs", alternate = ["start_ms", "start", "startTime", "start_time"])
    @SerialName("startMs")
    val startMs: Long = 0L,

    @SerializedName(value = "endMs", alternate = ["end_ms", "end", "endTime", "end_time"])
    @SerialName("endMs")
    val endMs: Long = 0L,

    @SerializedName(value = "isQuran", alternate = ["is_quran"])
    @SerialName("isQuran")
    val isQuran: Boolean = false,

    @SerializedName(value = "quranIndex", alternate = ["quran_index"])
    @SerialName("quranIndex")
    val quranIndex: Int = -1,

    @SerializedName(value = "ayahText", alternate = ["ayah_text"])
    @SerialName("ayahText")
    val ayahText: String = ""
)

/**
 * Utility object to load and parse curriculum data, audio word mappings,
 * and karaoke word timecodes from JSON asset files using Gson or Kotlinx.serialization.
 */
object JsonDataLoader {

    private val gson: Gson by lazy { Gson() }

    private val jsonSerializer: Json by lazy {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }

    // In-memory caches to avoid re-reading and re-parsing assets repeatedly
    @Volatile
    private var cachedUnits: List<UnitData>? = null

    @Volatile
    private var cachedUnitsMap: Map<String, UnitData>? = null

    @Volatile
    private var cachedWordsMap: Map<String, String>? = null

    @Volatile
    private var cachedParagraphTimingsMap: Map<String, List<WordTiming>>? = null

    @Volatile
    private var cachedGlossary: GlossaryData? = null

    /**
     * Parses JSON string into GlossaryData.
     */
    fun parseGlossaryJson(jsonString: String): GlossaryData {
        return try {
            jsonSerializer.decodeFromString<GlossaryData>(jsonString)
        } catch (e: Exception) {
            val type = object : TypeToken<GlossaryData>() {}.type
            gson.fromJson<GlossaryData>(jsonString, type) ?: GlossaryData()
        }
    }

    /**
     * Loads the master glossary from assets (default: "data_words_glossary.json").
     */
    fun loadGlossary(
        context: Context,
        fileName: String = "data_words_glossary.json",
        useCache: Boolean = true
    ): GlossaryData {
        if (useCache && cachedGlossary != null) {
            return cachedGlossary!!
        }

        synchronized(this) {
            if (useCache && cachedGlossary != null) {
                return cachedGlossary!!
            }

            val glossary = try {
                context.assets.open(fileName).use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
                    parseGlossaryJson(reader.readText())
                }
            } catch (e: Exception) {
                Log.e("JsonDataLoader", "Error loading glossary: ${e.message}")
                GlossaryData()
            }

            if (useCache) {
                cachedGlossary = glossary
            }
            return glossary
        }
    }

    /**
     * Resolves the exact Russian translation for a word at a specific position in the curriculum,
     * with graceful fallbacks to lemma dictionary and normalization.
     */
    fun getWordTranslation(
        unitNum: Int,
        paragraphId: Int,
        wordIndex: Int,
        rawWord: String,
        glossary: GlossaryData?
    ): String {
        if (glossary == null) return ""
        val key = "${unitNum}_${paragraphId}_${wordIndex}"
        val exact = glossary.byKey[key]
        if (!exact.isNullOrBlank()) return exact

        val cleaned = rawWord.trim().replace(Regex("[،.؟!:;\"'()\\[\\]{}«»﴿﴾_~*^—\\-]"), "").trim()
        val bare = cleaned.replace(Regex("[\\u0617-\\u061A\\u064B-\\u065F\\u0670]"), "")

        return glossary.words[rawWord]
            ?: glossary.words[cleaned]
            ?: glossary.words[bare]
            ?: ""
    }

    /**
     * Parses JSON string into a map of unit ID strings ("1".."8") to UnitData.
     * Uses Kotlinx.serialization with fallback to Gson.
     */
    fun parseUnitsJson(jsonString: String): Map<String, UnitData> {
        return try {
            jsonSerializer.decodeFromString<Map<String, UnitData>>(jsonString)
        } catch (e: Exception) {
            val type = object : TypeToken<Map<String, UnitData>>() {}.type
            gson.fromJson<Map<String, UnitData>>(jsonString, type)
                ?: emptyMap()
        }
    }

    /**
     * Parses JSON string into a map of Arabic words to audio filenames.
     * Uses Kotlinx.serialization with fallback to Gson.
     */
    fun parseWordsMapJson(jsonString: String): Map<String, String> {
        return try {
            jsonSerializer.decodeFromString<Map<String, String>>(jsonString)
        } catch (e: Exception) {
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson<Map<String, String>>(jsonString, type)
                ?: emptyMap()
        }
    }

    /**
     * Parses JSON string into a map of paragraph key to List of WordTiming.
     * Keyed by "${unitNum}_${paragraphId}" (e.g. "1_1") and normalized aliases.
     * Uses Kotlinx.serialization with fallback to Gson, handling root maps or nested wrapper objects.
     */
    fun parseParagraphTimingsJson(jsonString: String): Map<String, List<WordTiming>> {
        if (jsonString.isBlank()) return emptyMap()

        // 1. Try Kotlinx.serialization
        try {
            val result = jsonSerializer.decodeFromString<Map<String, List<WordTiming>>>(jsonString)
            if (result.isNotEmpty()) return normalizeTimingsKeys(result)
        } catch (_: Exception) {}

        // 2. Try Gson with direct Map<String, List<WordTiming>>
        try {
            val type = object : TypeToken<Map<String, List<WordTiming>>>() {}.type
            val result: Map<String, List<WordTiming>>? = gson.fromJson(jsonString, type)
            if (!result.isNullOrEmpty()) {
                return normalizeTimingsKeys(result)
            }
        } catch (_: Exception) {}

        // 3. Fallback: Check if wrapped inside a root object like {"paragraphs": {...}} or {"timecodes": {...}}
        try {
            val rootObj = gson.fromJson(jsonString, JsonObject::class.java)
            for (key in listOf("paragraphs", "timecodes", "units", "data")) {
                if (rootObj.has(key) && rootObj.get(key).isJsonObject) {
                    val innerJson = rootObj.get(key).toString()
                    val type = object : TypeToken<Map<String, List<WordTiming>>>() {}.type
                    val result: Map<String, List<WordTiming>>? = gson.fromJson(innerJson, type)
                    if (!result.isNullOrEmpty()) {
                        return normalizeTimingsKeys(result)
                    }
                }
            }
        } catch (_: Exception) {}

        return emptyMap()
    }

    /**
     * Normalizes keys in the map so both "1_1" and "u1_para_1" are indexed and accessible.
     */
    private fun normalizeTimingsKeys(original: Map<String, List<WordTiming>>): Map<String, List<WordTiming>> {
        val result = HashMap<String, List<WordTiming>>(original.size * 2)
        for ((key, timings) in original) {
            result[key] = timings
            // If key is "u1_para_2", also register "1_2"
            val uMatch = Regex("""^u?(\d+)_para_?(\d+)$""").find(key)
            if (uMatch != null) {
                val u = uMatch.groupValues[1]
                val p = uMatch.groupValues[2]
                result["${u}_${p}"] = timings
            }
            // If key is "1_2", also register "u1_para_2"
            val simpleMatch = Regex("""^(\d+)_(\d+)$""").find(key)
            if (simpleMatch != null) {
                val u = simpleMatch.groupValues[1]
                val p = simpleMatch.groupValues[2]
                result["u${u}_para_${p}"] = timings
            }
        }
        return result
    }

    /**
     * Parses data from an InputStream.
     */
    fun parseUnitsStream(inputStream: InputStream): Map<String, UnitData> {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val content = reader.use { it.readText() }
        return parseUnitsJson(content)
    }

    /**
     * Parses words map from an InputStream.
     */
    fun parseWordsMapStream(inputStream: InputStream): Map<String, String> {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val content = reader.use { it.readText() }
        return parseWordsMapJson(content)
    }

    /**
     * Parses paragraph word timecodes from an InputStream.
     */
    fun parseParagraphTimingsStream(inputStream: InputStream): Map<String, List<WordTiming>> {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val content = reader.use { it.readText() }
        return parseParagraphTimingsJson(content)
    }

    /**
     * Loads units map from the given asset file (default: "data_units.json").
     */
    fun loadUnitsMap(
        context: Context,
        fileName: String = "data_units.json",
        useCache: Boolean = true
    ): Map<String, UnitData> {
        if (useCache && cachedUnitsMap != null) {
            return cachedUnitsMap!!
        }

        synchronized(this) {
            if (useCache && cachedUnitsMap != null) {
                return cachedUnitsMap!!
            }
            val map = context.assets.open(fileName).use { inputStream ->
                parseUnitsStream(inputStream)
            }
            if (useCache) {
                cachedUnitsMap = map
            }
            return map
        }
    }

    /**
     * Loads all units as a list sorted ascending by unit number (1 through 8).
     */
    fun loadUnits(
        context: Context,
        fileName: String = "data_units.json",
        useCache: Boolean = true
    ): List<UnitData> {
        if (useCache && cachedUnits != null) {
            return cachedUnits!!
        }

        synchronized(this) {
            if (useCache && cachedUnits != null) {
                return cachedUnits!!
            }
            val units = loadUnitsMap(context, fileName, useCache)
                .values
                .sortedBy { it.unitNum }
            if (useCache) {
                cachedUnits = units
            }
            return units
        }
    }

    /**
     * Loads a specific unit by its number (1..8).
     */
    fun loadUnit(
        context: Context,
        unitNum: Int,
        fileName: String = "data_units.json"
    ): UnitData? {
        return loadUnits(context, fileName).firstOrNull { it.unitNum == unitNum }
    }

    /**
     * Loads the Arabic words to MP3 audio mapping from assets (default: "words_map.json").
     */
    fun loadWordsMap(
        context: Context,
        fileName: String = "words_map.json",
        useCache: Boolean = true
    ): Map<String, String> {
        if (useCache && cachedWordsMap != null) {
            return cachedWordsMap!!
        }

        synchronized(this) {
            if (useCache && cachedWordsMap != null) {
                return cachedWordsMap!!
            }
            val map = context.assets.open(fileName).use { inputStream ->
                parseWordsMapStream(inputStream)
            }
            if (useCache) {
                cachedWordsMap = map
            }
            return map
        }
    }

    /**
     * Loads paragraph word timecodes mapping from assets (default: "paragraphs_timecodes.json").
     * Returns empty map if asset is not present or error occurs.
     */
    fun loadParagraphTimingsMap(
        context: Context,
        fileName: String = "paragraphs_timecodes.json",
        useCache: Boolean = true
    ): Map<String, List<WordTiming>> {
        if (useCache && cachedParagraphTimingsMap != null) {
            return cachedParagraphTimingsMap!!
        }

        synchronized(this) {
            if (useCache && cachedParagraphTimingsMap != null) {
                return cachedParagraphTimingsMap!!
            }
            val map = try {
                context.assets.open(fileName).use { inputStream ->
                    parseParagraphTimingsStream(inputStream)
                }
            } catch (e: Exception) {
                // If not found in root of assets, try audio/ subdirectory
                try {
                    context.assets.open("audio/$fileName").use { inputStream ->
                        parseParagraphTimingsStream(inputStream)
                    }
                } catch (e2: Exception) {
                    emptyMap()
                }
            }
            if (useCache) {
                cachedParagraphTimingsMap = map
            }
            return map
        }
    }

    /**
     * Looks up WordTiming list for a specific unit and paragraph from a pre-loaded map.
     */
    fun getTimingsFromMap(
        map: Map<String, List<WordTiming>>,
        unitNum: Int,
        paragraphId: Int
    ): List<WordTiming> {
        return map["${unitNum}_${paragraphId}"]
            ?: map["u${unitNum}_para_${paragraphId}"]
            ?: map["${unitNum}_para_${paragraphId}"]
            ?: map["unit${unitNum}_para${paragraphId}"]
            ?: map["${unitNum}-${paragraphId}"]
            ?: emptyList()
    }

    /**
     * Loads WordTiming list for a specific unit and paragraph directly.
     */
    fun getParagraphTimings(
        context: Context,
        unitNum: Int,
        paragraphId: Int,
        fileName: String = "paragraphs_timecodes.json"
    ): List<WordTiming> {
        val timingsMap = loadParagraphTimingsMap(context, fileName)
        return getTimingsFromMap(timingsMap, unitNum, paragraphId)
    }

    /**
     * Clears all in-memory caches.
     */
    fun clearCache() {
        synchronized(this) {
            cachedUnits = null
            cachedUnitsMap = null
            cachedWordsMap = null
            cachedParagraphTimingsMap = null
            cachedGlossary = null
        }
    }
}
