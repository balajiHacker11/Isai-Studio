package com.example.features.catalog.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.model.Track
import com.example.core.model.UserPreferences
import com.example.core.player.PlaybackSnapshot
import com.example.features.library.presentation.LibraryViewModel
import com.example.shared.components.TrackTile

@Composable
fun AlbumDetailScreen(
    albumKey: String,
    catalogViewModel: CatalogViewModel,
    libraryViewModel: LibraryViewModel,
    playbackSnapshot: PlaybackSnapshot,
    preferences: UserPreferences,
    onBack: () -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onTrackLongPress: (Track) -> Unit,
    onSwipeQueue: (Track) -> Unit,
    onBulkAutoMatch: () -> Unit
) {
    val album = catalogViewModel.albumByKey(albumKey)
    val tracks = catalogViewModel.tracksForAlbum(albumKey)
    val linkedAudio by catalogViewModel.linkedAudio.collectAsStateWithLifecycle()
    val likedIds by libraryViewModel.likedTrackIds.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val showStickyTitle by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 260
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                if (album != null) {
                    Box(modifier = Modifier.fillMaxWidth().height(310.dp)) {
                        Image(
                            painter = painterResource(id = album.artworkRes),
                            contentDescription = album.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0.0f to Color(0x660A0B10),
                                            0.55f to Color(0xAA0A0B10),
                                            1.0f to MaterialTheme.colorScheme.background
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "${album.albumType.uppercase()} • ${album.language.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = album.title,
                                style = MaterialTheme.typography.headlineLarge,
                                color = Color.White
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Isai Collective • ${tracks.size} MP3 tracks" +
                                    (album.releaseDate?.let { " • $it" } ?: ""),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val firstPlayable = tracks.firstOrNull()
                                if (firstPlayable != null) onPlayTrack(firstPlayable, tracks)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color(0xFF0A0B10)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Play All")
                        }
                        OutlinedButton(
                            onClick = {
                                val shuffled = tracks.shuffled()
                                val firstPlayable = shuffled.firstOrNull()
                                if (firstPlayable != null) onPlayTrack(firstPlayable, shuffled)
                            },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Shuffle")
                        }
                        OutlinedButton(
                            onClick = onBulkAutoMatch,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.FolderOpen, contentDescription = "Link audio", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            items(tracks, key = { "album_${it.id}" }) { track ->
                TrackTile(
                    track = track,
                    isLinked = linkedAudio.containsKey(track.id),
                    isCurrentTrack = playbackSnapshot.currentTrack?.id == track.id,
                    isPlaying = playbackSnapshot.isPlaying,
                    isLiked = likedIds.contains(track.id),
                    swipeQueueEnabled = preferences.swipeRowQueueEnabled,
                    swipeLikeEnabled = preferences.swipeRowLikeEnabled,
                    hapticEnabled = preferences.hapticFeedbackEnabled,
                    onClick = { onPlayTrack(track, tracks) },
                    onLongPress = { onTrackLongPress(track) },
                    onSwipeAddToQueue = { onSwipeQueue(track) },
                    onSwipeToggleLike = { libraryViewModel.toggleLike(track) },
                    onToggleLikeClick = { libraryViewModel.toggleLike(track) },
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
            }
        }

        // Sticky Collapsing Header Top Bar
        Surface(
            color = if (showStickyTitle) MaterialTheme.colorScheme.surface.copy(alpha = 0.96f) else Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (showStickyTitle) Color.Transparent else Color.Black.copy(alpha = 0.45f),
                    shape = CircleShape
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = if (showStickyTitle) MaterialTheme.colorScheme.onSurface else Color.White
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = album?.title ?: "Album",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.alpha(if (showStickyTitle) 1f else 0f)
                )
            }
        }
    }
}
