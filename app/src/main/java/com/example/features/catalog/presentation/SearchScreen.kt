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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.model.Album
import com.example.core.model.Track
import com.example.core.model.TrackType
import com.example.core.model.UserPreferences
import com.example.core.player.PlaybackSnapshot
import com.example.features.library.presentation.LibraryViewModel
import com.example.shared.components.TrackTile

@Composable
fun SearchScreen(
    catalogViewModel: CatalogViewModel,
    libraryViewModel: LibraryViewModel,
    playbackSnapshot: PlaybackSnapshot,
    preferences: UserPreferences,
    onTrackClick: (Track, List<Track>) -> Unit,
    onTrackLongPress: (Track) -> Unit,
    onSwipeQueue: (Track) -> Unit,
    onAlbumClick: (Album) -> Unit
) {
    val searchState by catalogViewModel.searchUiState.collectAsStateWithLifecycle()
    val linkedAudio by catalogViewModel.linkedAudio.collectAsStateWithLifecycle()
    val likedIds by libraryViewModel.likedTrackIds.collectAsStateWithLifecycle()

    val languages = listOf("Tamil", "Telugu", "Malayalam", "Instrumental")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 16.dp)
            ) {
                Text(
                    text = "Search & Explore",
                    style = MaterialTheme.typography.displayMedium
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = searchState.query,
                    onValueChange = catalogViewModel::onSearchQueryChanged,
                    placeholder = { Text(stringResource(R.string.search_placeholder)) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchState.query.isNotEmpty()) {
                            IconButton(onClick = { catalogViewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_input")
                )
            }
        }

        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = searchState.linkedOnly,
                        onClick = catalogViewModel::onToggleSearchLinkedOnly,
                        label = { Text("My MP3s") }
                    )
                }
                items(TrackType.entries) { type ->
                    FilterChip(
                        selected = searchState.selectedType == type,
                        onClick = { catalogViewModel.onSearchTypeSelected(type) },
                        label = { Text(type.label) }
                    )
                }
                items(languages) { lang ->
                    FilterChip(
                        selected = searchState.selectedLanguage == lang,
                        onClick = { catalogViewModel.onSearchLanguageSelected(lang) },
                        label = { Text(lang) }
                    )
                }
            }
        }

        if (searchState.query.isBlank() && searchState.selectedType == null && searchState.selectedLanguage == null && !searchState.linkedOnly) {
            item {
                Text(
                    text = "Browse by Release",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
                )
            }
            val albumPairs = catalogViewModel.allAlbums.chunked(2)
            items(albumPairs) { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pair.forEach { album ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(110.dp)
                                .clickable { onAlbumClick(album) }
                        ) {
                            Box {
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
                                                listOf(Color(0x550A0B10), Color(0xDD0A0B10))
                                            )
                                        )
                                )
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = album.filmOrProject,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${album.language} • ${album.tracksListed} tracks",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                    if (pair.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        } else {
            item {
                Text(
                    text = "${searchState.results.size} matching track(s)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            }
            items(searchState.results, key = { "search_${it.id}" }) { track ->
                TrackTile(
                    track = track,
                    isLinked = linkedAudio.containsKey(track.id),
                    isCurrentTrack = playbackSnapshot.currentTrack?.id == track.id,
                    isPlaying = playbackSnapshot.isPlaying,
                    isLiked = likedIds.contains(track.id),
                    swipeQueueEnabled = preferences.swipeRowQueueEnabled,
                    swipeLikeEnabled = preferences.swipeRowLikeEnabled,
                    hapticEnabled = preferences.hapticFeedbackEnabled,
                    onClick = { onTrackClick(track, searchState.results) },
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
