package com.arabiyyah.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arabiyyah.app.audio.AudioPlayerManager
import com.arabiyyah.app.data.JsonDataLoader
import com.arabiyyah.app.data.UnitData
import com.arabiyyah.app.data.VocabWord
import com.arabiyyah.app.ui.components.cleanArabicWord
import com.arabiyyah.app.ui.components.removeTashkeel
import com.arabiyyah.app.ui.theme.ArabicTitleStyle
import com.arabiyyah.app.ui.theme.ArabicWordStyle
import com.arabiyyah.app.ui.theme.EmeraldContainer
import com.arabiyyah.app.ui.theme.EmeraldDark
import com.arabiyyah.app.ui.theme.EmeraldPrimary
import com.arabiyyah.app.ui.theme.GoldAccent
import com.arabiyyah.app.ui.theme.GoldContainer
import com.arabiyyah.app.ui.theme.LocalThemeIsDark
import com.arabiyyah.app.ui.theme.LocalThemeToggle
import com.arabiyyah.app.ui.theme.OnEmeraldContainer
import com.arabiyyah.app.ui.theme.OnGoldContainer

data class VocabWordWithUnit(
    val word: VocabWord,
    val unitNum: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyScreen(
    initialUnitNum: Int? = null,
    audioPlayerManager: AudioPlayerManager,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var units by remember { mutableStateOf<List<UnitData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedUnitFilter by remember { mutableIntStateOf(initialUnitNum ?: 0) } // 0 = All

    LaunchedEffect(Unit) {
        units = JsonDataLoader.loadUnits(context)
        isLoading = false
    }

    val currentPlayingWord by audioPlayerManager.currentWord.collectAsState()
    val isDark = isSystemInDarkTheme()

    // Flatten all vocabulary with their respective unit numbers
    val allVocabList = remember(units) {
        units.flatMap { u ->
            u.vocabulary.map { v -> VocabWordWithUnit(word = v, unitNum = u.unitNum) }
        }
    }

    // Filter by unit and search query
    val filteredList = remember(allVocabList, selectedUnitFilter, searchQuery) {
        val queryCleaned = searchQuery.trim().lowercase()
        val queryBare = removeTashkeel(queryCleaned)

        allVocabList.filter { item ->
            val matchesUnit = selectedUnitFilter == 0 || item.unitNum == selectedUnitFilter

            val wordClean = cleanArabicWord(item.word.word)
            val wordBare = removeTashkeel(wordClean).lowercase()
            val pluralClean = cleanArabicWord(item.word.plural)
            val pluralBare = removeTashkeel(pluralClean).lowercase()
            val meaning = item.word.meaning.lowercase()

            val matchesSearch = queryCleaned.isEmpty() ||
                    wordClean.contains(queryCleaned, ignoreCase = true) ||
                    wordBare.contains(queryBare, ignoreCase = true) ||
                    pluralClean.contains(queryCleaned, ignoreCase = true) ||
                    pluralBare.contains(queryBare, ignoreCase = true) ||
                    meaning.contains(queryCleaned, ignoreCase = true)

            matchesUnit && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (selectedUnitFilter > 0) "مفردات الوحدة $selectedUnitFilter" else "معجم المفردات والتراكيب",
                            style = ArabicTitleStyle.copy(
                                fontSize = 20.sp,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Словарь: ${filteredList.size} слов",
                            style = MaterialTheme.typography.bodySmall,
                            color = GoldAccent
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
                    val isDarkTheme = LocalThemeIsDark.current
                    val onToggleTheme = LocalThemeToggle.current

                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDarkTheme) "Светлая тема" else "Тёмная тема",
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
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = EmeraldPrimary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = {
                        Text(
                            text = "ابحث باللغة العربية أو الروسية... (Поиск)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = EmeraldPrimary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "مسح"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Unit Filter Chips Row
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedUnitFilter == 0,
                            onClick = { selectedUnitFilter = 0 },
                            label = { Text("الكل (Все)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }

                    items(units) { unit ->
                        val isSelected = selectedUnitFilter == unit.unitNum
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedUnitFilter = if (isSelected) 0 else unit.unitNum
                            },
                            label = { Text("الوحدة ${unit.unitNum}") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Vocab List
                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لم يتم العثور على كلمات مطابقة للبحث\nСлов не найдено",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            filteredList,
                            key = { "${it.unitNum}_${it.word.word}_${it.word.meaning}" }
                        ) { item ->
                            val isPlaying = currentPlayingWord == item.word.word ||
                                    currentPlayingWord == cleanArabicWord(item.word.word)

                            VocabCard(
                                item = item,
                                isAudioPlaying = isPlaying,
                                onPlayAudio = {
                                    audioPlayerManager.playWord(item.word.word)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VocabCard(
    item: VocabWordWithUnit,
    isAudioPlaying: Boolean,
    onPlayAudio: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val vocab = item.word

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isAudioPlaying) 4.dp else 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sound Button
            FilledIconButton(
                onClick = onPlayAudio,
                modifier = Modifier.size(46.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isAudioPlaying) GoldAccent else EmeraldPrimary.copy(alpha = 0.12f),
                    contentColor = if (isAudioPlaying) EmeraldDark else EmeraldPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "نطق الكلمة",
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Main details: Arabic word, plural, translation
            Column(modifier = Modifier.weight(1f)) {
                // Arabic word with RTL layout
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = vocab.word,
                        style = ArabicWordStyle.copy(
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Start
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Plural if available
                if (vocab.plural.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) EmeraldPrimary else EmeraldContainer
                        ) {
                            Text(
                                text = "ج: ${vocab.plural}",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif
                                ),
                                color = if (isDark) Color.White else OnEmeraldContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Russian translation
                if (vocab.meaning.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = vocab.meaning,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Badges column (Unit tag & Part of speech tag)
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "و ${item.unitNum}",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (vocab.pos.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isDark) Color(0xFF334155) else GoldContainer
                    ) {
                        Text(
                            text = vocab.pos,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) GoldAccent else OnGoldContainer
                        )
                    }
                }
            }
        }
    }
}
