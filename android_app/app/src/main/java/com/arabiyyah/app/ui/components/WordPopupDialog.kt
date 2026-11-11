package com.arabiyyah.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.arabiyyah.app.audio.AudioPlayerManager
import com.arabiyyah.app.data.Verb
import com.arabiyyah.app.data.VocabWord
import com.arabiyyah.app.ui.theme.ArabicTitleStyle
import com.arabiyyah.app.ui.theme.EmeraldContainer
import com.arabiyyah.app.ui.theme.EmeraldDark
import com.arabiyyah.app.ui.theme.EmeraldPrimary
import com.arabiyyah.app.ui.theme.GoldAccent
import com.arabiyyah.app.ui.theme.GoldContainer
import com.arabiyyah.app.ui.theme.OnEmeraldContainer
import com.arabiyyah.app.ui.theme.OnGoldContainer

@Composable
fun WordPopupDialog(
    word: String,
    translation: String = "",
    vocabMatch: VocabWord? = null,
    verbMatch: Verb? = null,
    vocalizedWord: String = word,
    audioPlayerManager: AudioPlayerManager,
    onDismiss: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    val displayMeaning = when {
        translation.isNotBlank() -> translation
        vocabMatch?.meaning?.isNotBlank() == true -> vocabMatch.meaning
        verbMatch?.meaning?.isNotBlank() == true -> verbMatch.meaning
        else -> ""
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with title and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) EmeraldPrimary else EmeraldContainer
                        ) {
                            Text(
                                text = "مُفْرَدَة",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDark) Color.White else OnEmeraldContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Слово и перевод",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Arabic Vocalized Word Display
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = vocalizedWord,
                        style = ArabicTitleStyle.copy(
                            fontSize = 34.sp,
                            lineHeight = 50.sp,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Audio Play Button
                FilledIconButton(
                    onClick = { audioPlayerManager.playWord(vocalizedWord) },
                    modifier = Modifier.size(54.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = GoldAccent,
                        contentColor = EmeraldDark
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Слушать произношение",
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Translation Card
                if (displayMeaning.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "ПЕРЕВОД",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDark) GoldAccent else EmeraldPrimary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = displayMeaning,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 17.sp,
                                    lineHeight = 24.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )

                            // Additional grammatical info: Vocabulary plural or pos
                            if (vocabMatch != null) {
                                if (vocabMatch.plural.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isDark) EmeraldPrimary else EmeraldContainer
                                        ) {
                                            Text(
                                                text = "الْجَمْعُ",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isDark) Color.White else OnEmeraldContainer,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = vocabMatch.plural,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                if (vocabMatch.pos.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isDark) Color(0xFF334155) else GoldContainer
                                    ) {
                                        Text(
                                            text = vocabMatch.pos,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isDark) GoldAccent else OnGoldContainer
                                        )
                                    }
                                }
                            }

                            // Additional grammatical info: Verb conjugation
                            if (verbMatch != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("الْمَاضِي", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(verbMatch.past, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("الْمُضَارِعُ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(verbMatch.present, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("الْمَصْدَرُ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(verbMatch.masdar, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                if (verbMatch.form.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isDark) Color(0xFF334155) else GoldContainer
                                    ) {
                                        Text(
                                            text = "الْوَزْنُ: ${verbMatch.form}",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isDark) GoldAccent else OnGoldContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "انقر على زر الصوت للاستماع لنطق الكلمة عبر التسجيل الصوتي المباشر.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
