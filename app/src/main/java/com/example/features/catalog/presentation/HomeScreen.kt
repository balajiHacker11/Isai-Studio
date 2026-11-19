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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.model.Album
import com.example.core.model.PlayHistoryEntry
import com.example.core.model.Track
import com.example.core.model.TrackType
import com.example.core.model.UiState
import com.example.core.model.UserPreferences
import com.example.core.player.PlaybackSnapshot
import com.example.features.library.presentation.LibraryViewModel
import com.example.shared.components.TrackTile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    catalogViewModel: CatalogViewModel,
    libraryViewModel: LibraryViewModel,
    playbackSnapshot: PlaybackSnapshot,
    preferences: UserPreferences,
    onTrackClick: (Track, List<Track>) -> Unit,
    onTrackLongPress: (Track) -> Unit,
    onSwipeQueue: (Track) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onArtistClick: () -> Unit,
    onReplayIntroClick: () -> Unit,
    onBulkAutoMatchClick: () -> Unit
) {
    val selectedFilter by catalogViewModel.selectedHomeFilter.collectAsStateWithLifecycle()
    val filteredTracks by catalogViewModel.filteredHomeTracks.collectAsStateWithLifecycle()
    val linkedAudio by catalogViewModel.linkedAudio.collectAsStateWithLifecycle()
    val isRefreshing by catalogViewModel.isRefreshing.collectAsStateWithLifecycle()
    val likedIds by libraryViewModel.likedTrackIds.collectAsStateWithLifecycle()
    val historyState by libraryViewModel.historyState.collectAsStateWithLifecycle()

    val recentTracks = when (val st = historyState) {
        is UiState.Success -> st.data
            .distinctBy { it.trackNumber }
            .mapNotNull { entry -> catalogViewModel.allTracks.firstOrNull { it.id == entry.trackNumber.toInt() } }
            .take(8)
        else -> emptyList()
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            catalogViewModel.refreshCatalog()
            libraryViewModel.refresh()
        },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Top Brand Bar & Linked Audio Pill
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, top = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "ISAI STUDIO",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Sai Abhyankkar",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape,
                        modifier = Modifier
                            .clickable(onClick = onBulkAutoMatchClick)
                            .testTag("home_bulk_match_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = "Link audio",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${linkedAudio.size}/110 Linked",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Hero Artist Spotlight Card
            item {
                ArtistSpotlightCard(
                    linkedCount = linkedAudio.size,
                    onOpenArtist = onArtistClick,
                    onReplayIntro = onReplayIntroClick,
                    onBulkAutoMatch = onBulkAutoMatchClick
                )
            }

            // Recently Played Horizontal Shelf (if user has play history)
            if (recentTracks.isNotEmpty()) {
                item {
                    SectionHeader(title = "Recently Played", subtitle = "Synced across your account")
                    Spacer(Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recentTracks, key = { "recent_${it.id}" }) { track ->
                            MiniTrackShelfCard(
                                track = track,
                                isLinked = linkedAudio.containsKey(track.id),
                                onClick = { onTrackClick(track, recentTracks) }
                            )
                        }
                    }
                }
            }

            // Albums & Soundtracks Horizontal Shelf
            item {
                SectionHeader(
                    title = "Albums & Original Scores",
                    subtitle = "${catalogViewModel.allAlbums.size} curated releases"
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(catalogViewModel.allAlbums, key = { it.albumKey }) { album ->
                        AlbumShelfCard(
                            album = album,
                            onClick = { onAlbumClick(album) }
                        )
                    }
                }
            }

            // Made For You: Indie & Anthem Highlights
            item {
                val highlights = catalogViewModel.allTracks.filter {
                    it.albumKey == "indie" || it.trackType == TrackType.THEME
                }.take(10)
                SectionHeader(
                    title = "Think Indie & Promo Themes",
                    subtitle = "Signature singles & universe cues"
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(highlights, key = { "hl_${it.id}" }) { track ->
                        MiniTrackShelfCard(
                            track = track,
                            isLinked = linkedAudio.containsKey(track.id),
                            onClick = { onTrackClick(track, highlights) }
                        )
                    }
                }
            }

            // Filter Chips + Full Discography Track List
            item {
                Column(modifier = Modifier.padding(horizontal = 18.dp)) {
                    Text(
                        text = "Complete Discography",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(8.dp))
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(HomeFilter.entries) { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { catalogViewModel.selectHomeFilter(filter) },
                            label = { Text(filter.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color(0xFF0A0B10)
                            ),
                            modifier = Modifier.testTag("home_filter_${filter.name.lowercase()}")
                        )
                    }
                }
            }

            if (filteredTracks.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "No local audio files linked yet",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Select MP3 or AAC files from your device to auto-match against the 110-track catalog.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = onBulkAutoMatchClick) {
                                Text("Auto-Match Local Files")
                            }
                        }
                    }
                }
            } else {
                items(filteredTracks, key = { it.id }) { track ->
                    TrackTile(
                        track = track,
                        isLinked = linkedAudio.containsKey(track.id),
                        isCurrentTrack = playbackSnapshot.currentTrack?.id == track.id,
                        isPlaying = playbackSnapshot.isPlaying,
                        isLiked = likedIds.contains(track.id),
                        swipeQueueEnabled = preferences.swipeRowQueueEnabled,
                        swipeLikeEnabled = preferences.swipeRowLikeEnabled,
                        hapticEnabled = preferences.hapticFeedbackEnabled,
                        onClick = { onTrackClick(track, filteredTracks) },
                        onLongPress = { onTrackLongPress(track) },
                        onSwipeAddToQueue = { onSwipeQueue(track) },
                        onSwipeToggleLike = { libraryViewModel.toggleLike(track) },
                        onToggleLikeClick = { libraryViewModel.toggleLike(track) },
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistSpotlightCard(
    linkedCount: Int,
    onOpenArtist: () -> Unit,
    onReplayIntro: () -> Unit,
    onBulkAutoMatch: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .clickable(onClick = onOpenArtist)
            .testTag("artist_spotlight_card")
    ) {
        Box(modifier = Modifier.height(215.dp)) {
            Image(
                painter = painterResource(id = R.drawable.artist_mic_studio),
                contentDescription = "Sai Abhyankkar Studio",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colorStops = arrayOf(
                                0.0f to Color(0xEE0A0B10),
                                0.58f to Color(0xBA0A0B10),
                                1.0f to Color(0x330A0B10)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                    shape = CircleShape
                ) {
                    Text(
                        text = "COMPOSER ARCHIVE • $linkedCount LINKED",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Column {
                    Text(
                        text = "110 Songs, Themes & BGMs",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Explore Think Indie, Dude, Karuppu, Balti, Baththa & OM.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 2
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onBulkAutoMatch,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color(0xFF0A0B10)
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Link MP3s", style = MaterialTheme.typography.labelLarge)
                        }
                        OutlinedButton(
                            onClick = onReplayIntro,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("home_replay_story_button")
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("6-Photo Story", style = MaterialTheme.typography.labelLarge, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(horizontal = 18.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AlbumShelfCard(
    album: Album,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .width(164.dp)
            .clickable(onClick = onClick)
            .testTag("album_card_${album.albumKey}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Image(
                painter = painterResource(id = album.artworkRes),
                contentDescription = album.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(142.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = album.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${album.language} • ${album.tracksListed} tracks",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MiniTrackShelfCard(
    track: Track,
    isLinked: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(152.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box {
                Image(
                    painter = painterResource(id = track.artworkRes),
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(126.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Surface(
                    color = if (isLinked) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.6f),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Play ${track.title}",
                            tint = if (isLinked) Color(0xFF0A0B10) else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.filmOrProject,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
