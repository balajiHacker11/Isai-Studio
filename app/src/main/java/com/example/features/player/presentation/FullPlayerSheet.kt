package com.example.features.player.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.example.R
import com.example.core.model.Track
import com.example.core.model.UserPreferences
import com.example.shared.components.LrcEditorDialog
import com.example.shared.components.formatDurationMs
import com.example.shared.components.shareTrackIntent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun FullPlayerSheet(
    playerViewModel: PlayerViewModel,
    isLiked: Boolean,
    preferences: UserPreferences,
    onToggleLike: (Track) -> Unit,
    onSaveEqToCloud: (String, List<Int>) -> Unit,
    onMinimize: () -> Unit
) {
    BackHandler { onMinimize() }

    val snapshot by playerViewModel.snapshot.collectAsStateWithLifecycle()
    val eqState by playerViewModel.eqState.collectAsStateWithLifecycle()
    val activeEqPreset by playerViewModel.activeEqPreset.collectAsStateWithLifecycle()
    val parsedLyrics by playerViewModel.parsedLyrics.collectAsStateWithLifecycle()
    val rawLrc by playerViewModel.activeTrackRawLrc.collectAsStateWithLifecycle()

    val track = snapshot.currentTrack ?: return
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var showQueueSheet by remember { mutableStateOf(false) }
    var showLyricsSheet by remember { mutableStateOf(false) }
    var showLrcEditor by remember { mutableStateOf(false) }
    var showEqSheet by remember { mutableStateOf(false) }
    var showSleepSheet by remember { mutableStateOf(false) }

    var showLikeBurst by remember { mutableStateOf(false) }
    var seekBurstText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(showLikeBurst) {
        if (showLikeBurst) {
            delay(700)
            showLikeBurst = false
        }
    }
    LaunchedEffect(seekBurstText) {
        if (seekBurstText != null) {
            delay(650)
            seekBurstText = null
        }
    }

    val verticalDragOffset = remember { Animatable(0f) }
    var horizontalArtDrag by remember { mutableFloatStateOf(0f) }

    val topBg = if (preferences.dynamicTintEnabled) snapshot.dominantColor else Color(0xFF1B1F30)
    val accentColor = if (preferences.dynamicTintEnabled) snapshot.vibrantColor else MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, verticalDragOffset.value.roundToInt().coerceAtLeast(0)) }
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to topBg,
                        0.55f to topBg.copy(alpha = 0.72f),
                        1.0f to Color(0xFF090A0F)
                    )
                )
            )
            .then(
                if (preferences.swipeMiniPlayerEnabled) {
                    Modifier.pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                scope.launch {
                                    if (verticalDragOffset.value > 220f) {
                                        onMinimize()
                                    } else {
                                        verticalDragOffset.animateTo(0f, spring())
                                    }
                                }
                            },
                            onDragCancel = {
                                scope.launch { verticalDragOffset.animateTo(0f, spring()) }
                            },
                            onVerticalDrag = { _, dragAmount ->
                                val next = (verticalDragOffset.value + dragAmount).coerceAtLeast(0f)
                                scope.launch { verticalDragOffset.snapTo(next) }
                            }
                        )
                    }
                } else Modifier
            )
            .testTag("full_player_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 22.dp, vertical = 10.dp)
                .widthIn(max = 520.dp)
                .align(Alignment.Center),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onMinimize,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("player_minimize_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.cd_minimize),
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PLAYING FROM ALBUM",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = track.filmOrProject,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = { shareTrackIntent(context, track) },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = "Share track",
                        tint = Color.White
                    )
                }
            }

            // Interactive Artwork Stage (Swipe L/R skip, Double-Tap center Like, Double-Tap L/R seek +/-10s)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(vertical = 12.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(26.dp))
                    .then(
                        if (preferences.swipeArtworkEnabled) {
                            Modifier.pointerInput(track.id) {
                                detectHorizontalDragGestures(
                                    onDragStart = { horizontalArtDrag = 0f },
                                    onDragEnd = {
                                        if (horizontalArtDrag < -90f) {
                                            if (preferences.hapticFeedbackEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            playerViewModel.skipNext()
                                        } else if (horizontalArtDrag > 90f) {
                                            if (preferences.hapticFeedbackEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            playerViewModel.skipPrevious()
                                        }
                                        horizontalArtDrag = 0f
                                    },
                                    onHorizontalDrag = { _, delta -> horizontalArtDrag += delta }
                                )
                            }
                        } else Modifier
                    )
                    .pointerInput(track.id, preferences.doubleTapLikeEnabled, preferences.doubleTapSeekEnabled) {
                        detectTapGestures(
                            onDoubleTap = { offset ->
                                val width = size.width
                                when {
                                    offset.x < width * 0.30f && preferences.doubleTapSeekEnabled -> {
                                        if (preferences.hapticFeedbackEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        playerViewModel.seekBy(-10_000L)
                                        seekBurstText = "-10s"
                                    }
                                    offset.x > width * 0.70f && preferences.doubleTapSeekEnabled -> {
                                        if (preferences.hapticFeedbackEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        playerViewModel.seekBy(10_000L)
                                        seekBurstText = "+10s"
                                    }
                                    preferences.doubleTapLikeEnabled -> {
                                        if (preferences.hapticFeedbackEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onToggleLike(track)
                                        showLikeBurst = true
                                    }
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = track.artworkRes),
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                androidx.compose.animation.AnimatedVisibility(
                    visible = showLikeBurst,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = CircleShape,
                        modifier = Modifier.size(86.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(46.dp)
                            )
                        }
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = seekBurstText != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.72f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = seekBurstText ?: "",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp)
                        )
                    }
                }
            }

            // Track Metadata + Like
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = track.subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = {
                        if (preferences.hapticFeedbackEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleLike(track)
                    },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("player_like_button")
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = stringResource(R.string.cd_like),
                        tint = if (isLiked) accentColor else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Scrubber
            Column(modifier = Modifier.fillMaxWidth()) {
                val maxDur = snapshot.durationMs.coerceAtLeast(1L).toFloat()
                Slider(
                    value = snapshot.positionMs.toFloat().coerceIn(0f, maxDur),
                    onValueChange = { playerViewModel.seekTo(it.toLong()) },
                    valueRange = 0f..maxDur,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = accentColor,
                        inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDurationMs(snapshot.positionMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.78f)
                    )
                    Text(
                        text = formatDurationMs(snapshot.durationMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.78f)
                    )
                }
            }

            // Primary Transport Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(
                    onClick = playerViewModel::toggleShuffle,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("player_shuffle_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = stringResource(R.string.cd_shuffle),
                        tint = if (snapshot.shuffleEnabled) accentColor else Color.White.copy(alpha = 0.7f)
                    )
                }
                IconButton(
                    onClick = {
                        if (preferences.hapticFeedbackEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        playerViewModel.skipPrevious()
                    },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("player_prev_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = stringResource(R.string.cd_previous),
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Surface(
                    color = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(68.dp)
                ) {
                    IconButton(
                        onClick = playerViewModel::togglePlayPause,
                        modifier = Modifier.fillMaxSize().testTag("player_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (snapshot.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(if (snapshot.isPlaying) R.string.cd_pause else R.string.cd_play),
                            tint = Color(0xFF0A0B10),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                IconButton(
                    onClick = {
                        if (preferences.hapticFeedbackEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        playerViewModel.skipNext()
                    },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("player_next_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = stringResource(R.string.cd_next),
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                IconButton(
                    onClick = playerViewModel::cycleRepeatMode,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("player_repeat_button")
                ) {
                    Icon(
                        imageVector = if (snapshot.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        contentDescription = stringResource(R.string.cd_repeat),
                        tint = if (snapshot.repeatMode != Player.REPEAT_MODE_OFF) accentColor else Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Bottom Console Tool Strip: Queue, Lyrics, EQ, Sleep Timer
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PlayerToolPill(
                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                    label = "Queue",
                    active = false,
                    accent = accentColor,
                    onClick = { showQueueSheet = true },
                    testTag = "player_queue_pill"
                )
                PlayerToolPill(
                    icon = Icons.Rounded.Lyrics,
                    label = "Lyrics",
                    active = parsedLyrics.isNotEmpty(),
                    accent = accentColor,
                    onClick = { showLyricsSheet = true },
                    testTag = "player_lyrics_pill"
                )
                PlayerToolPill(
                    icon = Icons.Rounded.Equalizer,
                    label = activeEqPreset,
                    active = activeEqPreset != "Flat",
                    accent = accentColor,
                    onClick = { showEqSheet = true },
                    testTag = "player_eq_pill"
                )
                PlayerToolPill(
                    icon = Icons.Rounded.Timer,
                    label = snapshot.sleepTimerRemainingMs?.let { formatDurationMs(it) }
                        ?: if (snapshot.sleepAtEndOfTrack) "End" else "Timer",
                    active = snapshot.sleepTimerRemainingMs != null || snapshot.sleepAtEndOfTrack,
                    accent = accentColor,
                    onClick = { showSleepSheet = true },
                    testTag = "player_timer_pill"
                )
            }
        }
    }

    if (showQueueSheet) {
        QueueBottomSheet(
            queue = snapshot.queue,
            currentIndex = snapshot.currentIndex,
            hapticEnabled = preferences.hapticFeedbackEnabled,
            onDismiss = { showQueueSheet = false },
            onJumpToIndex = { idx -> playerViewModel.jumpToQueueIndex(idx) },
            onMoveItem = { from, to -> playerViewModel.moveQueueItem(from, to) },
            onRemoveItem = { idx -> playerViewModel.removeQueueItem(idx) }
        )
    }

    if (showLyricsSheet) {
        val activeIdx = playerViewModel.activeLyricIndex(parsedLyrics, snapshot.positionMs)
        LyricsBottomSheet(
            track = track,
            lines = parsedLyrics,
            activeIndex = activeIdx,
            onDismiss = { showLyricsSheet = false },
            onSeekToLine = { ms -> playerViewModel.seekTo(ms) },
            onEditLyrics = { showLyricsSheet = false; showLrcEditor = true }
        )
    }

    if (showLrcEditor) {
        LrcEditorDialog(
            track = track,
            initialLrc = rawLrc,
            onDismiss = { showLrcEditor = false },
            onSave = { content -> playerViewModel.saveLyrics(track.id, content) }
        )
    }

    if (showEqSheet) {
        EqualizerBottomSheet(
            eqState = eqState,
            presets = playerViewModel.eqPresets,
            activePreset = activeEqPreset,
            onDismiss = {
                showEqSheet = false
                onSaveEqToCloud(activeEqPreset, eqState.bandLevelsMb)
            },
            onSelectPreset = { preset ->
                playerViewModel.selectEqPreset(preset)
                onSaveEqToCloud(preset.name, preset.bandsMb)
            },
            onBandChanged = { idx, level -> playerViewModel.setEqBand(idx, level) }
        )
    }

    if (showSleepSheet) {
        SleepTimerBottomSheet(
            remainingMs = snapshot.sleepTimerRemainingMs,
            sleepAtEndOfTrack = snapshot.sleepAtEndOfTrack,
            onDismiss = { showSleepSheet = false },
            onSelectMinutes = playerViewModel::startSleepTimer,
            onSelectEndOfTrack = playerViewModel::enableSleepAtEndOfTrack,
            onCancelTimer = playerViewModel::cancelSleepTimer
        )
    }
}

@Composable
private fun PlayerToolPill(
    icon: ImageVector,
    label: String,
    active: Boolean,
    accent: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        color = if (active) accent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.1f),
        shape = RoundedCornerShape(14.dp),
        onClick = onClick,
        modifier = Modifier.minimumInteractiveComponentSize().testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) accent else Color.White,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (active) accent else Color.White,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}
