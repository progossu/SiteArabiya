package com.arabiyyah.app.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arabiyyah.app.audio.AudioPlayerManager
import com.arabiyyah.app.audio.AudioType
import com.arabiyyah.app.ui.theme.EmeraldDark
import com.arabiyyah.app.ui.theme.EmeraldPrimary
import com.arabiyyah.app.ui.theme.GoldAccent

@Composable
fun AudioMasterBar(
    unitNum: Int,
    unitTitle: String,
    audioPlayerManager: AudioPlayerManager,
    modifier: Modifier = Modifier
) {
    val isPlaying by audioPlayerManager.isPlaying.collectAsState()
    val audioType by audioPlayerManager.currentAudioType.collectAsState()
    val currentUnitNum by audioPlayerManager.currentUnitNum.collectAsState()
    val currentPosition by audioPlayerManager.currentPosition.collectAsState()
    val duration by audioPlayerManager.duration.collectAsState()
    val playbackSpeed by audioPlayerManager.playbackSpeed.collectAsState()

    val isThisMasterPlaying = (currentUnitNum == unitNum) && (audioType == AudioType.MASTER)
    val isDark = isSystemInDarkTheme()

    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderLocalValue by remember { mutableFloatStateOf(0f) }

    val effectivePosition = if (isDraggingSlider) {
        sliderLocalValue.toLong()
    } else {
        if (isThisMasterPlaying) currentPosition else 0L
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else EmeraldPrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Audio title & badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "التسجيل الصوتي الكامل للدرس",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GoldAccent
                ) {
                    Text(
                        text = "الوحدة $unitNum",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scrub Slider
            Slider(
                value = effectivePosition.toFloat(),
                onValueChange = {
                    isDraggingSlider = true
                    sliderLocalValue = it
                },
                onValueChangeFinished = {
                    audioPlayerManager.seekTo(sliderLocalValue.toLong())
                    isDraggingSlider = false
                },
                valueRange = 0f..(if (duration > 0L && isThisMasterPlaying) duration.toFloat() else 100f),
                colors = SliderDefaults.colors(
                    thumbColor = GoldAccent,
                    activeTrackColor = GoldAccent,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Timestamps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTimeMs(effectivePosition),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = if (isThisMasterPlaying && duration > 0L) formatTimeMs(duration) else "--:--",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Playback Controls: -10s, Play/Pause, +10s, Speed selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Playback speed chips
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    AudioPlayerManager.AVAILABLE_SPEEDS.forEach { speed ->
                        val isSelected = playbackSpeed == speed
                        FilterChip(
                            selected = isSelected,
                            onClick = { audioPlayerManager.setPlaybackSpeed(speed) },
                            label = {
                                Text(
                                    text = "${speed}x",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldAccent,
                                selectedLabelColor = EmeraldDark,
                                containerColor = Color.White.copy(alpha = 0.15f),
                                labelColor = Color.White
                            ),
                            border = null
                        )
                    }
                }

                // Skip & Play controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Rewind 10s
                    IconButton(
                        onClick = { audioPlayerManager.seekBy(-10000L) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = "ترجيع 10 ثوان",
                            tint = Color.White
                        )
                    }

                    // Master Play/Pause Button
                    FilledIconButton(
                        onClick = { audioPlayerManager.toggleMaster(unitNum) },
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = GoldAccent,
                            contentColor = EmeraldDark
                        )
                    ) {
                        Icon(
                            imageVector = if (isThisMasterPlaying && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isThisMasterPlaying && isPlaying) "إيقاف مؤقت" else "تشغيل",
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Forward 10s
                    IconButton(
                        onClick = { audioPlayerManager.seekBy(10000L) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "تقديم 10 ثوان",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
