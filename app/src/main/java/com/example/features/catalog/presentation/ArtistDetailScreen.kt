package com.example.features.catalog.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.model.Album
import com.example.core.model.ArtistArtwork
import com.example.core.model.Track
import com.example.core.model.UserPreferences
import com.example.core.player.PlaybackSnapshot
import com.example.features.library.presentation.LibraryViewModel
import com.example.shared.components.TrackTile

@Composable
fun ArtistDetailScreen(
    catalogViewModel: CatalogViewModel,
    libraryViewModel: LibraryViewModel,
    playbackSnapshot: PlaybackSnapshot,
    preferences: UserPreferences,
    onBack: () -> Unit,
    onReplayIntro: () -> Unit,
    onAlbumClick: (Album) -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onTrackLongPress: (Track) -> Unit,
    onSwipeQueue: (Track) -> Unit
) {
    val linkedAudio by catalogViewModel.linkedAudio.collectAsStateWithLifecycle()
    val likedIds by libraryViewModel.likedTrackIds.collectAsStateWithLifecycle()
    val popularTracks = catalogViewModel.allTracks.take(15)

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(310.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.artist_stage_blazer),
                        contentDescription = "Sai Abhyankkar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0x440A0B10), Color(0xAA0A0B10), MaterialTheme.colorScheme.background)
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "VERIFIED COMPOSER • PRODUCER",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Sai Abhyankkar",
                            style = MaterialTheme.typography.displayLarge,
                            color = Color.White
                        )
                        Text(
                            text = "110 Tracks • 48 Songs • 10 Versions • 10 Themes • 42 BGMs",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    onPlayTrack(popularTracks.first(), popularTracks)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color(0xFF0A0B10)
                                )
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Play Top")
                            }
                            OutlinedButton(onClick = onReplayIntro) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Watch 6-Photo Story", color = Color.White)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Studio Gallery",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(ArtistArtwork.introStories) { slide ->
                        Image(
                            painter = painterResource(id = slide.imageRes),
                            contentDescription = slide.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 130.dp, height = 164.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(onClick = onReplayIntro)
                        )
                    }
                }
            }

            item {
                Text(
                    text = "All 12 Releases",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(catalogViewModel.allAlbums, key = { "art_alb_${it.albumKey}" }) { album ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .width(150.dp)
                                .clickable { onAlbumClick(album) }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Image(
                                    painter = painterResource(id = album.artworkRes),
                                    contentDescription = album.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(album.filmOrProject, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                Text("${album.tracksListed} tracks", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Signature Tracks",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            }

            items(popularTracks, key = { "art_trk_${it.id}" }) { track ->
                TrackTile(
                    track = track,
                    isLinked = linkedAudio.containsKey(track.id),
                    isCurrentTrack = playbackSnapshot.currentTrack?.id == track.id,
                    isPlaying = playbackSnapshot.isPlaying,
                    isLiked = likedIds.contains(track.id),
                    swipeQueueEnabled = preferences.swipeRowQueueEnabled,
                    swipeLikeEnabled = preferences.swipeRowLikeEnabled,
                    hapticEnabled = preferences.hapticFeedbackEnabled,
                    onClick = { onPlayTrack(track, popularTracks) },
                    onLongPress = { onTrackLongPress(track) },
                    onSwipeAddToQueue = { onSwipeQueue(track) },
                    onSwipeToggleLike = { libraryViewModel.toggleLike(track) },
                    onToggleLikeClick = { libraryViewModel.toggleLike(track) },
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
            }
        }

        Surface(
            color = Color.Black.copy(alpha = 0.45f),
            shape = CircleShape,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(12.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = Color.White
                )
            }
        }
    }
}
