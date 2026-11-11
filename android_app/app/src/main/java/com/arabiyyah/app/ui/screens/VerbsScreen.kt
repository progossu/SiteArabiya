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
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import com.arabiyyah.app.data.Verb
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

data class VerbWithUnit(
    val verb: Verb,
    val unitNum: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerbsScreen(
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

    // Flatten all verbs with their respective unit numbers
    val allVerbsList = remember(units) {
        units.flatMap { u ->
            u.verbs.map { v -> VerbWithUnit(verb = v, unitNum = u.unitNum) }
        }
    }

    // Filter by unit and search query
    val filteredList = remember(allVerbsList, selectedUnitFilter, searchQuery) {
        val queryCleaned = searchQuery.trim().lowercase()
        val queryBare = removeTashkeel(queryCleaned)

        allVerbsList.filter { item ->
            val matchesUnit = selectedUnitFilter == 0 || item.unitNum == selectedUnitFilter

            val pastClean = cleanArabicWord(item.verb.past)
            val pastBare = removeTashkeel(pastClean).lowercase()
            val presentClean = cleanArabicWord(item.verb.present)
            val presentBare = removeTashkeel(presentClean).lowercase()
            val masdarClean = cleanArabicWord(item.verb.masdar)
            val masdarBare = removeTashkeel(masdarClean).lowercase()
            val meaning = item.verb.meaning.lowercase()
            val form = item.verb.form.lowercase()

            val matchesSearch = queryCleaned.isEmpty() ||
                    pastClean.contains(queryCleaned, ignoreCase = true) ||
                    pastBare.contains(queryBare, ignoreCase = true) ||
                    presentClean.contains(queryCleaned, ignoreCase = true) ||
                    presentBare.contains(queryBare, ignoreCase = true) ||
                    masdarClean.contains(queryCleaned, ignoreCase = true) ||
                    masdarBare.contains(queryBare, ignoreCase = true) ||
                    meaning.contains(queryCleaned, ignoreCase = true) ||
                    form.contains(queryCleaned, ignoreCase = true)

            matchesUnit && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (selectedUnitFilter > 0) "أفعال الوحدة $selectedUnitFilter" else "جدول تصريف الأفعال",
                            style = ArabicTitleStyle.copy(
                                fontSize = 20.sp,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Глаголы (прошедшее, настоящее, масдар): ${filteredList.size}",
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
                            text = "ابحث عن فعل أو مصدر أو ترجمة... (Поиск глаголов)",
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

                // Verbs List
                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لم يتم العثور على أفعال مطابقة للبحث\nГлаголов не найдено",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            filteredList,
                            key = { "${it.unitNum}_${it.verb.past}_${it.verb.present}_${it.verb.meaning}" }
                        ) { item ->
                            VerbCard(
                                item = item,
                                currentPlayingWord = currentPlayingWord,
                                onPlayWord = { audioPlayerManager.playWord(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VerbCard(
    item: VerbWithUnit,
    currentPlayingWord: String?,
    onPlayWord: (String) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val verb = item.verb

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Form pattern & Unit badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Verb Form (e.g. Form IV / باب أفعل)
                if (verb.form.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) EmeraldPrimary else EmeraldContainer
                    ) {
                        Text(
                            text = verb.form,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) Color.White else OnEmeraldContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Unit Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) Color(0xFF334155) else GoldContainer
                ) {
                    Text(
                        text = "الوحدة ${item.unitNum}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) GoldAccent else OnGoldContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3-Column Conjugation Table: Past (الماضي), Present (المضارع), Masdar (المصدر)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Column 1: الماضي (Past)
                    VerbColumnCell(
                        label = "الماضي",
                        arabicText = verb.past,
                        isAudioPlaying = currentPlayingWord == verb.past || currentPlayingWord == cleanArabicWord(verb.past),
                        onPlayAudio = { onPlayWord(verb.past) },
                        modifier = Modifier.weight(1f)
                    )

                    // Column 2: المضارع (Present)
                    VerbColumnCell(
                        label = "المضارع",
                        arabicText = verb.present,
                        isAudioPlaying = currentPlayingWord == verb.present || currentPlayingWord == cleanArabicWord(verb.present),
                        onPlayAudio = { onPlayWord(verb.present) },
                        modifier = Modifier.weight(1f)
                    )

                    // Column 3: المصدر (Masdar)
                    VerbColumnCell(
                        label = "المصدر",
                        arabicText = verb.masdar,
                        isAudioPlaying = currentPlayingWord == verb.masdar || currentPlayingWord == cleanArabicWord(verb.masdar),
                        onPlayAudio = { onPlayWord(verb.masdar) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // Meaning / Russian Translation
            Text(
                text = verb.meaning,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun VerbColumnCell(
    label: String,
    arabicText: String,
    isAudioPlaying: Boolean,
    onPlayAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = arabicText,
            style = ArabicWordStyle.copy(
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        FilledIconButton(
            onClick = onPlayAudio,
            modifier = Modifier.size(34.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isAudioPlaying) GoldAccent else EmeraldPrimary.copy(alpha = 0.1f),
                contentColor = if (isAudioPlaying) EmeraldDark else EmeraldPrimary
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "استماع",
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
