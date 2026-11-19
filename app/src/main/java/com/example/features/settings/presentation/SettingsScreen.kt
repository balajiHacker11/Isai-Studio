package com.example.features.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.core.model.AccentTheme
import com.example.core.model.UserPreferences
import com.example.core.player.PlaybackSnapshot
import com.example.shared.components.formatDurationMs
import com.example.ui.theme.CoralPrimary
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.VioletPrimary
import com.google.firebase.auth.FirebaseUser

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    currentUser: FirebaseUser,
    settingsViewModel: SettingsViewModel,
    playbackSnapshot: PlaybackSnapshot,
    activeEqPreset: String,
    onOpenEqualizer: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onBulkAutoMatchClick: () -> Unit,
    onReplayIntroClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    val preferences by settingsViewModel.preferences.collectAsStateWithLifecycle()
    val linkedAudio by settingsViewModel.linkedAudio.collectAsStateWithLifecycle()
    var showClearConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "STUDIO CONSOLE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Preferences & FX",
                    style = MaterialTheme.typography.displayMedium
                )
            }
        }

        // Google Account Card
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (currentUser.photoUrl != null) {
                            AsyncImage(
                                model = currentUser.photoUrl,
                                contentDescription = currentUser.displayName ?: "Profile",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentUser.displayName?.ifBlank { "Isai Listener" } ?: "Isai Listener",
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentUser.email ?: "Signed in with Google",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onReplayIntroClick,
                            modifier = Modifier.weight(1f).testTag("settings_replay_intro")
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("6-Photo Intro")
                        }
                        OutlinedButton(
                            onClick = onSignOutClick,
                            modifier = Modifier.weight(1f).testTag("sign_out_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Sign Out")
                        }
                    }
                }
            }
        }

        // Theme & Accent
        item {
            SettingsSectionCard(
                icon = Icons.Rounded.Palette,
                title = "Theme & Visual Polish"
            ) {
                SettingsToggleRow(
                    title = "Obsidian Dark Mode",
                    subtitle = "High-contrast dark studio canvas",
                    checked = preferences.darkThemeEnabled,
                    onCheckedChange = settingsViewModel::setDarkTheme,
                    testTag = "settings_dark_mode_switch"
                )
                SettingsToggleRow(
                    title = "Dynamic Artwork Tinting",
                    subtitle = "Color mini-player and full player from cover Palette",
                    checked = preferences.dynamicTintEnabled,
                    onCheckedChange = settingsViewModel::setDynamicTint,
                    testTag = "settings_dynamic_tint_switch"
                )

                Text(
                    text = "Studio Accent Palette",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AccentTheme.entries.forEach { accent ->
                        val selected = preferences.accentKey == accent.key
                        val swatch = when (accent) {
                            AccentTheme.CORAL -> CoralPrimary
                            AccentTheme.CYAN -> CyanPrimary
                            AccentTheme.VIOLET -> VioletPrimary
                            AccentTheme.GOLD -> GoldPrimary
                        }
                        Surface(
                            color = if (selected) swatch.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .then(
                                    if (selected) Modifier.border(1.5.dp, swatch, RoundedCornerShape(12.dp))
                                    else Modifier
                                )
                                .clickable { settingsViewModel.setAccentTheme(accent) }
                                .testTag("accent_pill_${accent.key}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(swatch)
                                )
                                Text(
                                    text = accent.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Audio & Playback Console
        item {
            SettingsSectionCard(
                icon = Icons.Rounded.GraphicEq,
                title = "Audio & Playback Console"
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Crossfade Duration", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${preferences.crossfadeSeconds}s",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = preferences.crossfadeSeconds.toFloat(),
                        onValueChange = { settingsViewModel.setCrossfadeSeconds(it.toInt()) },
                        valueRange = 0f..12f,
                        steps = 11,
                        modifier = Modifier.testTag("settings_crossfade_slider")
                    )
                }

                SettingsToggleRow(
                    title = "Gapless Transitions",
                    subtitle = "Seamless continuity between album tracks & BGMs",
                    checked = preferences.gaplessEnabled,
                    onCheckedChange = settingsViewModel::setGaplessEnabled
                )
                SettingsToggleRow(
                    title = "Normalize Loudness",
                    subtitle = "Consistent gain level across indie singles and scores",
                    checked = preferences.normalizeVolumeEnabled,
                    onCheckedChange = settingsViewModel::setNormalizeVolume
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onOpenEqualizer,
                        modifier = Modifier.weight(1f).testTag("settings_open_eq")
                    ) {
                        Icon(Icons.Rounded.Equalizer, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("EQ: $activeEqPreset", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = onOpenSleepTimer,
                        modifier = Modifier.weight(1f).testTag("settings_open_timer")
                    ) {
                        Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        val timerText = playbackSnapshot.sleepTimerRemainingMs?.let { formatDurationMs(it) }
                            ?: if (playbackSnapshot.sleepAtEndOfTrack) "End" else "Sleep Timer"
                        Text(timerText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        // Gestures & Haptics
        item {
            SettingsSectionCard(
                icon = Icons.Rounded.TouchApp,
                title = "Gestures & Haptics"
            ) {
                SettingsToggleRow(
                    title = "Swipe Mini-Player Up/Down",
                    subtitle = "Swipe up to expand player, drag down to minimize",
                    checked = preferences.swipeMiniPlayerEnabled,
                    onCheckedChange = settingsViewModel::setSwipeMiniPlayer
                )
                SettingsToggleRow(
                    title = "Swipe Cover to Skip Track",
                    subtitle = "Swipe left or right on artwork or mini-player",
                    checked = preferences.swipeArtworkEnabled,
                    onCheckedChange = settingsViewModel::setSwipeArtwork
                )
                SettingsToggleRow(
                    title = "Swipe Row Left to Queue",
                    subtitle = "Quick-add any catalog track to Up Next",
                    checked = preferences.swipeRowQueueEnabled,
                    onCheckedChange = settingsViewModel::setSwipeRowQueue
                )
                SettingsToggleRow(
                    title = "Swipe Row Right to Like",
                    subtitle = "Quick-favorite tracks from lists",
                    checked = preferences.swipeRowLikeEnabled,
                    onCheckedChange = settingsViewModel::setSwipeRowLike
                )
                SettingsToggleRow(
                    title = "Double-Tap Edges to Seek ±10s",
                    subtitle = "Double-tap left/right thirds of full player art",
                    checked = preferences.doubleTapSeekEnabled,
                    onCheckedChange = settingsViewModel::setDoubleTapSeek
                )
                SettingsToggleRow(
                    title = "Double-Tap Center to Like",
                    subtitle = "Double-tap center of artwork for instant favorite",
                    checked = preferences.doubleTapLikeEnabled,
                    onCheckedChange = settingsViewModel::setDoubleTapLike
                )
                SettingsToggleRow(
                    title = "Studio Haptic Feedback",
                    subtitle = "Tactile confirmation on gestures and transport",
                    checked = preferences.hapticFeedbackEnabled,
                    onCheckedChange = settingsViewModel::setHapticFeedback
                )
            }
        }

        // Local Storage & Privacy
        item {
            SettingsSectionCard(
                icon = Icons.Rounded.VisibilityOff,
                title = "Local Audio & Privacy"
            ) {
                Text(
                    text = "${linkedAudio.size} of 110 tracks linked via zero-permission Storage Access Framework.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onBulkAutoMatchClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color(0xFF0A0B10)
                        ),
                        modifier = Modifier.weight(1f).testTag("settings_bulk_match")
                    ) {
                        Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Auto-Match", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { showClearConfirm = true },
                        enabled = linkedAudio.isNotEmpty(),
                        modifier = Modifier.weight(1f).testTag("settings_clear_links")
                    ) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Clear Links")
                    }
                }

                SettingsToggleRow(
                    title = "Private Listening Session",
                    subtitle = "Pause cloud play-history logging while active",
                    checked = preferences.privateSessionEnabled,
                    onCheckedChange = settingsViewModel::setPrivateSession,
                    testTag = "settings_private_session_switch"
                )
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear Local Audio Links?") },
            text = { Text("Remove all ${linkedAudio.size} linked audio URIs from Isai Studio? Your audio files stay untouched on your device.") },
            confirmButton = {
                Button(
                    onClick = {
                        settingsViewModel.clearAllLinkedAudio()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.headlineMedium)
            }
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
        )
    }
}
