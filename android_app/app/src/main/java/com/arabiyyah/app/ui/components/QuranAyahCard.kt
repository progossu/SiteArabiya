package com.arabiyyah.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arabiyyah.app.audio.AudioPlayerManager
import com.arabiyyah.app.audio.AudioType
import com.arabiyyah.app.ui.theme.ArabicAyahStyle
import com.arabiyyah.app.ui.theme.EmeraldDark
import com.arabiyyah.app.ui.theme.EmeraldPrimary
import com.arabiyyah.app.ui.theme.GoldAccent
import com.arabiyyah.app.ui.theme.GoldLight
import com.arabiyyah.app.ui.theme.QuranAyahBg
import com.arabiyyah.app.ui.theme.QuranAyahBgDark

@Composable
fun QuranAyahCard(
    ayahText: String,
    ayahRef: String,
    audioPlayerManager: AudioPlayerManager,
    isRecitingInParagraph: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isPlaying by audioPlayerManager.isPlaying.collectAsState()
    val audioType by audioPlayerManager.currentAudioType.collectAsState()
    val currentQuranKey by audioPlayerManager.currentQuranKey.collectAsState()

    val isDark = isSystemInDarkTheme()
    val isStandalonePlaying = isPlaying && audioType == AudioType.QURAN &&
            (currentQuranKey == ayahRef ||
                    audioPlayerManager.resolveQuranAudioPath(ayahRef) ==
                    audioPlayerManager.resolveQuranAudioPath(currentQuranKey ?: ""))

    val isThisPlaying = isStandalonePlaying || isRecitingInParagraph

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isThisPlaying) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val cardBg = if (isDark) QuranAyahBgDark else QuranAyahBg
    val borderColor by animateColorAsState(
        targetValue = if (isThisPlaying) GoldAccent else GoldAccent.copy(alpha = 0.5f),
        label = "border"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.5.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isThisPlaying) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Reciter attribution & reference badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sheikh Al-Husary badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = GoldAccent.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "الشيخ محمود خليل الحصري",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) GoldLight else EmeraldDark,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Reference badge (e.g. سورة النحل / ٨٩)
                if (ayahRef.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = ayahRef,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quran Ayah Sacred Text with RTL direction
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = "﴿ $ayahText ﴾",
                    style = ArabicAyahStyle.copy(
                        color = if (isDark) Color(0xFFF1F5F9) else EmeraldDark,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Playback button & status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledIconButton(
                    onClick = { audioPlayerManager.toggleQuran(ayahRef) },
                    modifier = Modifier
                        .size(46.dp)
                        .scale(pulseScale),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isThisPlaying) GoldAccent else EmeraldPrimary,
                        contentColor = if (isThisPlaying) EmeraldDark else Color.White
                    )
                ) {
                    Icon(
                        imageVector = if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isThisPlaying) "إيقاف مؤقت" else "استماع للآية",
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = if (isThisPlaying) "جاري الاستماع للآية الكريمة..." else "استماع للتلاوة المرتلة",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isThisPlaying) GoldAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isThisPlaying) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
