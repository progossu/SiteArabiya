package com.arabiyyah.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arabiyyah.app.audio.AudioPlayerManager
import com.arabiyyah.app.audio.AudioType
import com.arabiyyah.app.data.JsonDataLoader
import com.arabiyyah.app.data.UnitData
import com.arabiyyah.app.ui.theme.ArabicTitleStyle
import com.arabiyyah.app.ui.theme.EmeraldContainer
import com.arabiyyah.app.ui.theme.EmeraldDark
import com.arabiyyah.app.ui.theme.EmeraldPrimary
import com.arabiyyah.app.ui.theme.GoldAccent
import com.arabiyyah.app.ui.theme.GoldContainer
import com.arabiyyah.app.ui.theme.LocalThemeIsDark
import com.arabiyyah.app.ui.theme.LocalThemeToggle
import com.arabiyyah.app.ui.theme.OnEmeraldContainer
import com.arabiyyah.app.ui.theme.OnGoldContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitListScreen(
    audioPlayerManager: AudioPlayerManager,
    onUnitClick: (Int) -> Unit,
    onOpenVocabulary: () -> Unit,
    onOpenVerbs: () -> Unit,
    onOpenCollocations: () -> Unit
) {
    val context = LocalContext.current
    var units by remember { mutableStateOf<List<UnitData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        units = JsonDataLoader.loadUnits(context)
        isLoading = false
    }

    val isPlaying by audioPlayerManager.isPlaying.collectAsState()
    val audioType by audioPlayerManager.currentAudioType.collectAsState()
    val currentUnitNum by audioPlayerManager.currentUnitNum.collectAsState()
    val isDark = isSystemInDarkTheme()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "العربية بين يديك",
                            style = ArabicTitleStyle.copy(
                                fontSize = 22.sp,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Al-Arabiyyah Bayna Yadayk • الكتاب الثالث",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldAccent
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
                    IconButton(onClick = onOpenVocabulary) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "المفردات",
                            tint = GoldAccent
                        )
                    }
                    IconButton(onClick = onOpenVerbs) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = "الأفعال",
                            tint = GoldAccent
                        )
                    }
                    IconButton(onClick = onOpenCollocations) {
                        Icon(
                            imageVector = Icons.Default.Diamond,
                            contentDescription = "التراكيب والعبارات",
                            tint = GoldAccent
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Intro Header Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else EmeraldContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = GoldAccent,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = EmeraldDark,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "فهرس الوحدات الدراسية (الكتاب الثالث)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else OnEmeraldContainer
                                )
                                Text(
                                    text = "8 разделов • 54 текста • 100% перевод каждого слова • Караоке и чтение шейха аль-Хусари",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDark) GoldAccent else EmeraldDark.copy(alpha = 0.85f),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Units List
                items(units, key = { it.unitNum }) { unit ->
                    val isThisMasterPlaying = isPlaying &&
                            audioType == AudioType.MASTER &&
                            currentUnitNum == unit.unitNum

                    UnitItemCard(
                        unit = unit,
                        isAudioPlaying = isThisMasterPlaying,
                        onCardClick = { onUnitClick(unit.unitNum) },
                        onAudioPreviewClick = { audioPlayerManager.toggleMaster(unit.unitNum) }
                    )
                }
            }
        }
    }
}

@Composable
private fun UnitItemCard(
    unit: UnitData,
    isAudioPlaying: Boolean,
    onCardClick: () -> Unit,
    onAudioPreviewClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    // Calculate simulated progress based on unit number (e.g. Unit 1 is completed, 2 in progress, etc.)
    val progress = when (unit.unitNum) {
        1 -> 1.0f
        2 -> 0.75f
        3 -> 0.40f
        4 -> 0.20f
        else -> 0.05f
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Top row: Unit badge + audio preview play button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = EmeraldPrimary
                    ) {
                        Text(
                            text = "الوحدة ${unit.unitNum}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) Color(0xFF334155) else GoldContainer
                    ) {
                        Text(
                            text = "الدرس ${unit.lessonNum}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) GoldAccent else OnGoldContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Audio preview button
                FilledIconButton(
                    onClick = onAudioPreviewClick,
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isAudioPlaying) GoldAccent else EmeraldPrimary.copy(alpha = 0.12f),
                        contentColor = if (isAudioPlaying) EmeraldDark else EmeraldPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "معاينة الصوت",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Arabic Vocalized Title (RTL)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = unit.titleAr,
                    style = ArabicTitleStyle.copy(
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Start
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Russian Title
            Text(
                text = unit.titleRu,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Russian Description
            if (unit.descRu.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = unit.descRu,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stats row: Paragraphs, Vocab, Verbs count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatBadge(
                    icon = Icons.Default.MenuBook,
                    label = "${unit.paragraphs.size} فقرات"
                )
                StatBadge(
                    icon = Icons.Default.Translate,
                    label = "${unit.vocabulary.size} كلمة"
                )
                StatBadge(
                    icon = Icons.Default.AutoStories,
                    label = "${unit.verbs.size} فعلاً"
                )
                StatBadge(
                    icon = Icons.Default.Diamond,
                    label = "${unit.collocations.size} تعبير"
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress bar and percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = EmeraldPrimary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
            }
        }
    }
}

@Composable
private fun StatBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
