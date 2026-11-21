package com.example.shared.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.model.Track
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackTile(
    track: Track,
    isLinked: Boolean,
    isCurrentTrack: Boolean,
    isPlaying: Boolean,
    isLiked: Boolean,
    swipeQueueEnabled: Boolean,
    swipeLikeEnabled: Boolean,
    hapticEnabled: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onSwipeAddToQueue: () -> Unit,
    onSwipeToggleLike: () -> Unit,
    onToggleLikeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val dragOffset = remember { Animatable(0f) }
    val thresholdPx = with(LocalDensity.current) { 78.dp.toPx() }

    val rowContainerColor by animateColorAsState(
        targetValue = if (isCurrentTrack) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0f)
        },
        label = "trackRowColor"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
    ) {
        // Revealed gesture background
        if (dragOffset.value != 0f) {
            val isRight = dragOffset.value > 0f
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        if (isRight) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                        else MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f)
                    )
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isRight) Arrangement.Start else Arrangement.End
            ) {
                Icon(
                    imageVector = if (isRight) Icons.Filled.Favorite else Icons.AutoMirrored.Rounded.PlaylistAdd,
                    contentDescription = null,
                    tint = if (isRight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isRight) "Like" else "Queue",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Surface(
            color = rowContainerColor,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(dragOffset.value.roundToInt(), 0) }
                .then(
                    if (swipeQueueEnabled || swipeLikeEnabled) {
                        Modifier.pointerInput(track.id, swipeQueueEnabled, swipeLikeEnabled) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    val x = dragOffset.value
                                    scope.launch {
                                        if (x > thresholdPx && swipeLikeEnabled) {
                                            if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onSwipeToggleLike()
                                        } else if (x < -thresholdPx && swipeQueueEnabled) {
                                            if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onSwipeAddToQueue()
                                        }
                                        dragOffset.animateTo(0f, spring())
                                    }
                                },
                                onDragCancel = {
                                    scope.launch { dragOffset.animateTo(0f, spring()) }
                                },
                                onHorizontalDrag = { _, delta ->
                                    val next = (dragOffset.value + delta).let { candidate ->
                                        when {
                                            candidate > 0f && !swipeLikeEnabled -> 0f
                                            candidate < 0f && !swipeQueueEnabled -> 0f
                                            else -> candidate.coerceIn(-thresholdPx * 1.4f, thresholdPx * 1.4f)
                                        }
                                    }
                                    scope.launch { dragOffset.snapTo(next) }
                                }
                            )
                        }
                    } else Modifier
                )
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPress()
                    }
                )
                .testTag("track_tile_${track.id}")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(52.dp)) {
                    Image(
                        painter = painterResource(id = track.artworkRes),
                        contentDescription = track.album,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    if (isCurrentTrack) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.55f)),
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedAudioWave(
                                isPlaying = isPlaying,
                                color = MaterialTheme.colorScheme.primary,
                                maxHeight = 18.dp
                            )
                        }
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isCurrentTrack) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isCurrentTrack) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = track.trackType.label.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isLinked) Icons.Rounded.CheckCircle else Icons.Rounded.MusicNote,
                            contentDescription = if (isLinked) "Local MP3 linked" else "Studio audio ready to play",
                            tint = if (isLinked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "${track.filmOrProject} • ${track.subtitle}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onToggleLikeClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("track_like_${track.id}")
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = stringResource(R.string.cd_like),
                        tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onLongPress,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("track_more_${track.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.cd_more_options),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
