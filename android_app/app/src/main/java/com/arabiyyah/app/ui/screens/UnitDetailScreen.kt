package com.arabiyyah.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arabiyyah.app.audio.AudioPlayerManager
import com.arabiyyah.app.audio.AudioType
import com.arabiyyah.app.data.GlossaryData
import com.arabiyyah.app.data.JsonDataLoader
import com.arabiyyah.app.data.Paragraph
import com.arabiyyah.app.data.UnitData
import com.arabiyyah.app.data.Verb
import com.arabiyyah.app.data.VocabWord
import com.arabiyyah.app.ui.components.AudioMasterBar
import com.arabiyyah.app.ui.components.QuranAyahCard
import com.arabiyyah.app.ui.components.WordPopupDialog
import com.arabiyyah.app.ui.components.cleanArabicWord
import com.arabiyyah.app.ui.components.removeTashkeel
import com.arabiyyah.app.ui.components.stripDiacritics
import com.arabiyyah.app.ui.theme.ArabicParagraphStyle
import com.arabiyyah.app.ui.theme.ArabicTitleStyle
import com.arabiyyah.app.ui.theme.EmeraldContainer
import com.arabiyyah.app.ui.theme.EmeraldDark
import com.arabiyyah.app.ui.theme.EmeraldPrimary
import com.arabiyyah.app.ui.theme.GoldAccent
import com.arabiyyah.app.ui.theme.LocalThemeIsDark
import com.arabiyyah.app.ui.theme.LocalThemeToggle
import com.arabiyyah.app.ui.theme.OnEmeraldContainer
import com.arabiyyah.app.ui.theme.WordHighlightBg
import com.arabiyyah.app.ui.theme.WordHighlightBgDark
import com.arabiyyah.app.ui.theme.WordHighlightBorder
import com.arabiyyah.app.ui.theme.WordHighlightText

sealed class ParagraphSegment {
    data class TextSegment(val words: List<Pair<Int, String>>) : ParagraphSegment()
    data class QuranSegment(val quranIndex: Int, val ayahText: String, val ayahRef: String) : ParagraphSegment()
}

fun cleanParagraphText(rawText: String): String {
    val lines = rawText.lines()
    val cleanedLines = lines.filterNot { line ->
        val trimmed = line.trim()
        (trimmed.startsWith("الْفِقْرَةُ") || trimmed.startsWith("الفقرة")) && trimmed.contains("—")
    }
    return cleanedLines.joinToString("\n").trim()
}

fun parseParagraphSegments(rawText: String): Pair<List<ParagraphSegment>, Int> {
    val text = cleanParagraphText(rawText)
    val quranRegex = Regex("""﴿([^﴾]+)﴾(?:\s*\[([^\]]+)\])?""")
    val segments = mutableListOf<ParagraphSegment>()
    var lastIndex = 0
    var wordCounter = 0
    var quranCounter = 0

    val matches = quranRegex.findAll(text).toList()
    for (match in matches) {
        val range = match.range
        if (range.first > lastIndex) {
            val textPart = text.substring(lastIndex, range.first)
            val words = textPart.split(Regex("\\s+")).filter { it.isNotBlank() }
            val indexedWords = words.map { word -> Pair(wordCounter++, word) }
            if (indexedWords.isNotEmpty()) {
                segments.add(ParagraphSegment.TextSegment(indexedWords))
            }
        }

        val ayahText = match.groupValues[1].trim()
        val ayahRef = match.groupValues.getOrNull(2)?.trim() ?: ""
        segments.add(ParagraphSegment.QuranSegment(quranCounter++, ayahText, ayahRef))
        lastIndex = range.last + 1
    }

    if (lastIndex < text.length) {
        val textPart = text.substring(lastIndex)
        val words = textPart.split(Regex("\\s+")).filter { it.isNotBlank() }
        val indexedWords = words.map { word -> Pair(wordCounter++, word) }
        if (indexedWords.isNotEmpty()) {
            segments.add(ParagraphSegment.TextSegment(indexedWords))
        }
    }

    return Pair(segments, wordCounter)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UnitDetailScreen(
    unitNum: Int,
    audioPlayerManager: AudioPlayerManager,
    onBackClick: () -> Unit,
    onNavigateToVocabulary: (Int) -> Unit,
    onNavigateToVerbs: (Int) -> Unit,
    onNavigateToCollocations: (Int) -> Unit
) {
    val context = LocalContext.current
    var unitData by remember { mutableStateOf<UnitData?>(null) }
    var glossaryData by remember { mutableStateOf<GlossaryData?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // User readability settings
    var showTashkeel by remember { mutableStateOf(true) }
    var arabicFontSize by remember { mutableStateOf(26.sp) }
    var showFontSizeMenu by remember { mutableStateOf(false) }

    LaunchedEffect(unitNum) {
        unitData = JsonDataLoader.loadUnit(context, unitNum)
        glossaryData = JsonDataLoader.loadGlossary(context)
        isLoading = false
    }

    val isPlaying by audioPlayerManager.isPlaying.collectAsState()
    val audioType by audioPlayerManager.currentAudioType.collectAsState()
    val currentParagraphId by audioPlayerManager.currentParagraphId.collectAsState()
    val currentAudioUnit by audioPlayerManager.currentUnitNum.collectAsState()
    val activeWordIndex by audioPlayerManager.activeWordIndex.collectAsState()
    val activeQuranIndex by audioPlayerManager.activeQuranIndex.collectAsState()
    val currentPlayingWord by audioPlayerManager.currentWord.collectAsState()

    // Dialog state
    var selectedWordForDialog by remember { mutableStateOf<String?>(null) }
    var selectedWordTranslation by remember { mutableStateOf("") }
    var matchedVocabForDialog by remember { mutableStateOf<VocabWord?>(null) }
    var matchedVerbForDialog by remember { mutableStateOf<Verb?>(null) }
    var vocalizedWordForDialog by remember { mutableStateOf("") }

    val isDark = isSystemInDarkTheme()
    val lazyListState = rememberLazyListState()

    // Smoothly scroll to active paragraph card when audio starts playing
    LaunchedEffect(currentParagraphId, isPlaying) {
        if (isPlaying && audioType == AudioType.PARAGRAPH && currentAudioUnit == unitNum && currentParagraphId != null) {
            val targetIdx = unitData?.paragraphs?.indexOfFirst { it.id == currentParagraphId } ?: -1
            if (targetIdx >= 0) {
                // Index + 1 because item 0 is the AudioMasterBar
                lazyListState.animateScrollToItem(targetIdx + 1)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = unitData?.titleAr ?: "الوحدة $unitNum",
                            style = ArabicTitleStyle.copy(
                                fontSize = 19.sp,
                                color = Color.White
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = unitData?.titleRu ?: "Урок $unitNum",
                            style = MaterialTheme.typography.bodySmall,
                            color = GoldAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Tashkeel Toggle Action (User Request #5: Toggle Tashkeel)
                    FilterChip(
                        selected = showTashkeel,
                        onClick = { showTashkeel = !showTashkeel },
                        label = {
                            Text(
                                text = if (showTashkeel) "تَشْكِيل" else "بدون",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldAccent,
                            selectedLabelColor = EmeraldDark,
                            containerColor = EmeraldPrimary.copy(alpha = 0.5f),
                            labelColor = Color.White.copy(alpha = 0.8f)
                        ),
                        border = BorderStroke(1.dp, if (showTashkeel) GoldAccent else Color.White.copy(alpha = 0.3f)),
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    // Font Size Selector (User Request #1: Font size change)
                    Box {
                        IconButton(onClick = { showFontSizeMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.FormatSize,
                                contentDescription = "Размер шрифта",
                                tint = GoldAccent
                            )
                        }
                        DropdownMenu(
                            expanded = showFontSizeMenu,
                            onDismissRequest = { showFontSizeMenu = false }
                        ) {
                            listOf(
                                "Мелкий (22sp)" to 22.sp,
                                "Стандартный (26sp)" to 26.sp,
                                "Крупный (30sp)" to 30.sp,
                                "Очень крупный (34sp)" to 34.sp
                            ).forEach { (label, size) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = label,
                                            fontWeight = if (arabicFontSize == size) FontWeight.Bold else FontWeight.Normal,
                                            color = if (arabicFontSize == size) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        arabicFontSize = size
                                        showFontSizeMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Day / Night Mode Toggle
                    val isThemeDark = LocalThemeIsDark.current
                    val onToggleTheme = LocalThemeToggle.current
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (isThemeDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isThemeDark) "Светлая тема" else "Тёмная тема",
                            tint = GoldAccent
                        )
                    }

                    // Vocabulary Screen Shortcut
                    IconButton(onClick = { onNavigateToVocabulary(unitNum) }) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "Словарь урока",
                            tint = GoldAccent
                        )
                    }

                    // Verbs Screen Shortcut
                    IconButton(onClick = { onNavigateToVerbs(unitNum) }) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = "Глаголы урока",
                            tint = GoldAccent
                        )
                    }

                    // Collocations Screen Shortcut
                    IconButton(onClick = { onNavigateToCollocations(unitNum) }) {
                        Icon(
                            imageVector = Icons.Default.Diamond,
                            contentDescription = "Словосочетания урока",
                            tint = GoldAccent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EmeraldPrimary,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ElevatedButton(
                        onClick = { onNavigateToVocabulary(unitNum) },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "المفردات (${unitData?.vocabulary?.size ?: 0})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    ElevatedButton(
                        onClick = { onNavigateToVerbs(unitNum) },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else GoldAccent,
                            contentColor = EmeraldDark
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = null,
                            tint = EmeraldDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "الأفعال (${unitData?.verbs?.size ?: 0})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    ElevatedButton(
                        onClick = { onNavigateToCollocations(unitNum) },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = if (isDark) EmeraldDark else EmeraldPrimary.copy(alpha = 0.85f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Diamond,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "التراكيب (${unitData?.collocations?.size ?: 0})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (isLoading || unitData == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = EmeraldPrimary)
            }
        } else {
            val unit = unitData!!

            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Master Full Unit Audio Playback Bar
                item {
                    AudioMasterBar(
                        unitNum = unitNum,
                        unitTitle = unit.titleAr,
                        audioPlayerManager = audioPlayerManager
                    )
                }

                // Paragraphs List
                items(unit.paragraphs, key = { it.id }) { paragraph ->
                    val (segments, wordCount) = remember(paragraph.text) {
                        parseParagraphSegments(paragraph.text)
                    }

                    val isThisParagraphPlaying = isPlaying &&
                            audioType == AudioType.PARAGRAPH &&
                            currentAudioUnit == unitNum &&
                            currentParagraphId == paragraph.id

                    ParagraphCard(
                        paragraph = paragraph,
                        segments = segments,
                        isPlaying = isThisParagraphPlaying,
                        activeWordIndex = if (isThisParagraphPlaying) activeWordIndex else -1,
                        activeQuranIndex = if (isThisParagraphPlaying) activeQuranIndex else -1,
                        showTashkeel = showTashkeel,
                        fontSize = arabicFontSize,
                        audioPlayerManager = audioPlayerManager,
                        onWordClick = { rawWord, wordIdx ->
                            val translation = JsonDataLoader.getWordTranslation(
                                unitNum = unitNum,
                                paragraphId = paragraph.id,
                                wordIndex = wordIdx,
                                rawWord = rawWord,
                                glossary = glossaryData
                            )

                            val cleaned = cleanArabicWord(rawWord)
                            val bare = removeTashkeel(cleaned)

                            // Match with vocabulary
                            val vocabMatch = unit.vocabulary.firstOrNull {
                                cleanArabicWord(it.word) == cleaned ||
                                        removeTashkeel(cleanArabicWord(it.word)) == bare
                            }

                            // Match with verbs
                            val verbMatch = unit.verbs.firstOrNull {
                                cleanArabicWord(it.past) == cleaned ||
                                        cleanArabicWord(it.present) == cleaned ||
                                        cleanArabicWord(it.masdar) == cleaned ||
                                        removeTashkeel(cleanArabicWord(it.past)) == bare ||
                                        removeTashkeel(cleanArabicWord(it.present)) == bare ||
                                        removeTashkeel(cleanArabicWord(it.masdar)) == bare
                            }

                            selectedWordForDialog = rawWord
                            selectedWordTranslation = translation
                            matchedVocabForDialog = vocabMatch
                            matchedVerbForDialog = verbMatch
                            vocalizedWordForDialog = rawWord

                            // Play word audio
                            audioPlayerManager.playWord(cleaned)
                        },
                        onPlayParagraph = {
                            audioPlayerManager.toggleParagraph(unitNum, paragraph.id, wordCount)
                        }
                    )
                }

                // =========================================================================
                // Embedded Unit Collocations & Idioms Section (Addressing User Request)
                // =========================================================================
                if (unit.collocations.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = EmeraldPrimary,
                                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.6f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Diamond,
                                        contentDescription = null,
                                        tint = GoldAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "التَّرَاكِيبُ وَالْعِبَارَاتُ الِاصْطِلَاحِيَّةُ",
                                        style = ArabicTitleStyle.copy(fontSize = 18.sp, color = Color.White)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Устойчивые выражения и сложные словосочетания (${unit.collocations.size})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(unit.collocations, key = { it.phrase }) { collocation ->
                        val isPlaying = currentPlayingWord == collocation.phrase ||
                                currentPlayingWord == cleanArabicWord(collocation.phrase)
                        val collocationWithUnit = remember(collocation) {
                            CollocationWithUnit(collocation, unitNum)
                        }

                        Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                            CollocationCard(
                                item = collocationWithUnit,
                                isAudioPlaying = isPlaying,
                                onPlayAudio = {
                                    audioPlayerManager.playWord(collocation.phrase)
                                },
                                onCopy = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText(
                                        "Collocation",
                                        "${collocation.phrase}\n${collocation.meaning}\n${collocation.note}"
                                    )
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Словосочетание скопировано", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }

        // Full Word Detail Dialog (100% Translation Coverage)
        selectedWordForDialog?.let { word ->
            WordPopupDialog(
                word = word,
                translation = selectedWordTranslation,
                vocabMatch = matchedVocabForDialog,
                verbMatch = matchedVerbForDialog,
                vocalizedWord = vocalizedWordForDialog,
                audioPlayerManager = audioPlayerManager,
                onDismiss = { selectedWordForDialog = null }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ParagraphCard(
    paragraph: Paragraph,
    segments: List<ParagraphSegment>,
    isPlaying: Boolean,
    activeWordIndex: Int,
    activeQuranIndex: Int = -1,
    showTashkeel: Boolean,
    fontSize: TextUnit,
    audioPlayerManager: AudioPlayerManager,
    onWordClick: (rawWord: String, wordIdx: Int) -> Unit,
    onPlayParagraph: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPlaying) 6.dp else 2.dp),
        border = BorderStroke(
            width = if (isPlaying) 1.5.dp else 1.dp,
            color = if (isPlaying) GoldAccent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // =========================================================================
            // Centered Paragraph Header (Addressing User Request #6)
            // =========================================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Top Row: Left placeholder, Centered Badge, Right Play Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.size(44.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isPlaying) GoldAccent else EmeraldPrimary,
                        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "📖",
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "الْفِقْرَةُ ${paragraph.id}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isPlaying) EmeraldDark else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    FilledIconButton(
                        onClick = onPlayParagraph,
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isPlaying) GoldAccent else EmeraldPrimary,
                            contentColor = if (isPlaying) EmeraldDark else Color.White
                        )
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Пауза" else "Слушать абзац",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Centered Arabic Title - Larger, Distinctive Color & Weight
                if (paragraph.titleAr.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (showTashkeel) paragraph.titleAr else stripDiacritics(paragraph.titleAr),
                        style = ArabicTitleStyle.copy(
                            fontSize = 24.sp,
                            lineHeight = 36.sp,
                            color = if (isDark) GoldAccent else EmeraldPrimary,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Centered Russian Title
                if (paragraph.titleRu.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = paragraph.titleRu,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .height(2.dp)
                        .clip(CircleShape)
                        .background(GoldAccent.copy(alpha = 0.45f))
                )
            }

            // =========================================================================
            // Paragraph Body with Interactive Words and Quran Ayah Boxes
            // =========================================================================
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    segments.forEach { segment ->
                        when (segment) {
                            is ParagraphSegment.TextSegment -> {
                                FlowRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    segment.words.forEach { (wordIndex, word) ->
                                        val isHighlighted = isPlaying && (activeWordIndex == wordIndex)

                                        // High-Contrast Karaoke Highlighting (Addressing User Request #3)
                                        val highlightBg = if (isDark) WordHighlightBgDark else WordHighlightBg
                                        val animatedBg by animateColorAsState(
                                            targetValue = if (isHighlighted) highlightBg else Color.Transparent,
                                            animationSpec = tween(durationMillis = 100),
                                            label = "karaokeBg"
                                        )
                                        val targetTextColor = when {
                                            isHighlighted -> if (isDark) Color(0xFF000000) else WordHighlightText
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                        val animatedTextColor by animateColorAsState(
                                            targetValue = targetTextColor,
                                            animationSpec = tween(durationMillis = 100),
                                            label = "karaokeText"
                                        )

                                        val displayText = if (showTashkeel) word else stripDiacritics(word)

                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 3.dp, vertical = 4.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(animatedBg)
                                                .then(
                                                    if (isHighlighted) {
                                                        Modifier.border(
                                                            width = 2.dp,
                                                            color = WordHighlightBorder,
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                    } else {
                                                        Modifier
                                                    }
                                                )
                                                .clickable { onWordClick(word, wordIndex) }
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = displayText,
                                                style = ArabicParagraphStyle.copy(
                                                    fontSize = fontSize,
                                                    lineHeight = (fontSize.value * 1.85f).sp,
                                                    color = animatedTextColor,
                                                    fontWeight = if (isHighlighted) FontWeight.ExtraBold else FontWeight.Normal
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                            is ParagraphSegment.QuranSegment -> {
                                val isRecitingInPara = isPlaying && (activeQuranIndex == segment.quranIndex)
                                QuranAyahCard(
                                    ayahText = segment.ayahText,
                                    ayahRef = segment.ayahRef,
                                    isRecitingInParagraph = isRecitingInPara,
                                    audioPlayerManager = audioPlayerManager
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
