package com.arabiyyah.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lightbulb
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
import com.arabiyyah.app.data.Collocation
import com.arabiyyah.app.data.JsonDataLoader
import com.arabiyyah.app.data.UnitData
import com.arabiyyah.app.ui.components.cleanArabicWord
import com.arabiyyah.app.ui.components.removeTashkeel
import com.arabiyyah.app.ui.theme.ArabicTitleStyle
import com.arabiyyah.app.ui.theme.EmeraldDark
import com.arabiyyah.app.ui.theme.EmeraldPrimary
import com.arabiyyah.app.ui.theme.GoldAccent
import com.arabiyyah.app.ui.theme.GoldContainer
import com.arabiyyah.app.ui.theme.LocalThemeIsDark
import com.arabiyyah.app.ui.theme.LocalThemeToggle
import com.arabiyyah.app.ui.theme.OnGoldContainer

data class CollocationWithUnit(
    val collocation: Collocation,
    val unitNum: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollocationsScreen(
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

    // Flatten all collocations with their unit numbers
    val allCollocations = remember(units) {
        units.flatMap { u ->
            u.collocations.map { c -> CollocationWithUnit(collocation = c, unitNum = u.unitNum) }
        }
    }

    // Filter by unit and search query
    val filteredList = remember(allCollocations, selectedUnitFilter, searchQuery) {
        val queryCleaned = searchQuery.trim().lowercase()
        val queryBare = removeTashkeel(cleanArabicWord(queryCleaned))

        allCollocations.filter { item ->
            val matchUnit = (selectedUnitFilter == 0 || item.unitNum == selectedUnitFilter)
            if (!matchUnit) return@filter false

            if (queryCleaned.isEmpty()) return@filter true

            val phraseClean = cleanArabicWord(item.collocation.phrase).lowercase()
            val phraseBare = removeTashkeel(phraseClean)
            val meaning = item.collocation.meaning.lowercase()
            val note = item.collocation.note.lowercase()

            phraseClean.contains(queryCleaned) ||
                    phraseBare.contains(queryBare) ||
                    meaning.contains(queryCleaned) ||
                    note.contains(queryCleaned)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "التَّرَاكِيبُ وَالْعِبَارَاتُ",
                            style = ArabicTitleStyle.copy(
                                fontSize = 20.sp,
                                color = Color.White
                            )
                        )
                        Text(
                            text = if (selectedUnitFilter > 0) "Словосочетания: Урок $selectedUnitFilter" else "Сложные словосочетания и идиомы",
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
                    val isThemeDark = LocalThemeIsDark.current
                    val onToggleTheme = LocalThemeToggle.current
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (isThemeDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isThemeDark) "Светлая тема" else "Тёмная тема",
                            tint = GoldAccent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EmeraldPrimary,
                    titleContentColor = Color.White
                )
            )
        }
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
                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = {
                        Text(
                            text = "Поиск по фразе, переводу или пояснению...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Поиск",
                            tint = EmeraldPrimary
                        )
                    },
                    trailingIcon = {
                        AnimatedVisibility(visible = searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Очистить"
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
                            label = { Text("الكل (Все: ${allCollocations.size})") },
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
                            label = { Text("الوحدة ${unit.unitNum} (${unit.collocations.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // List of Collocations
                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لم يتم العثور على تراكيب مطابقة للبحث\nСловосочетаний не найдено",
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
                            key = { "${it.unitNum}_${it.collocation.phrase}" }
                        ) { item ->
                            val isPlaying = currentPlayingWord == item.collocation.phrase ||
                                    currentPlayingWord == cleanArabicWord(item.collocation.phrase)

                            CollocationCard(
                                item = item,
                                isAudioPlaying = isPlaying,
                                onPlayAudio = {
                                    audioPlayerManager.playWord(item.collocation.phrase)
                                },
                                onCopy = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText(
                                        "Collocation",
                                        "${item.collocation.phrase}\n${item.collocation.meaning}\n${item.collocation.note}"
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
    }
}

@Composable
fun CollocationCard(
    item: CollocationWithUnit,
    isAudioPlaying: Boolean,
    onPlayAudio: () -> Unit,
    onCopy: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val c = item.collocation

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isAudioPlaying) 5.dp else 2.dp),
        border = BorderStroke(
            width = if (isAudioPlaying) 1.5.dp else 1.dp,
            color = if (isAudioPlaying) GoldAccent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Unit Pill, Diamond Tag, Copy & Audio buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EmeraldPrimary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "الوحدة ${item.unitNum}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Diamond,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "تَرْكِيبٌ",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) GoldAccent else OnGoldContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Скопировать",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FilledIconButton(
                        onClick = onPlayAudio,
                        modifier = Modifier.size(38.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isAudioPlaying) GoldAccent else EmeraldPrimary.copy(alpha = 0.12f),
                            contentColor = if (isAudioPlaying) EmeraldDark else EmeraldPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Озвучить",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Arabic Vocalized Phrase (RTL)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = c.phrase,
                    style = ArabicTitleStyle.copy(
                        fontSize = 24.sp,
                        color = if (isDark) GoldAccent else EmeraldDark,
                        lineHeight = 36.sp,
                        textAlign = TextAlign.Start
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Russian Meaning
            Text(
                text = c.meaning,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Context/Explanation Note
            if (c.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Пояснение и контекст:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) GoldAccent else EmeraldPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = c.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
