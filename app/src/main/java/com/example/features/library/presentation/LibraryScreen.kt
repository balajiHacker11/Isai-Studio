package com.example.features.library.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.model.ArtistArtwork
import com.example.core.model.LinkedAudioEntry
import com.example.core.model.Track
import com.example.core.model.UiState
import com.example.core.model.UserPlaylist
import com.example.core.model.UserPreferences
import com.example.core.player.PlaybackSnapshot
import com.example.features.catalog.presentation.CatalogViewModel
import com.example.features.settings.presentation.SettingsViewModel
import com.example.shared.components.TrackListShimmer
import com.example.shared.components.TrackTile

private enum class LibraryTab(val label: String) {
    PLAYLISTS("Playlists"),
    LIKED("Liked Songs"),
    LINKED("My MP3s"),
    HISTORY("History")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    catalogViewModel: CatalogViewModel,
    libraryViewModel: LibraryViewModel,
    settingsViewModel: SettingsViewModel,
    playbackSnapshot: PlaybackSnapshot,
    preferences: UserPreferences,
    onTrackClick: (Track, List<Track>) -> Unit,
    onTrackLongPress: (Track) -> Unit,
    onSwipeQueue: (Track) -> Unit,
    onBulkAutoMatchClick: () -> Unit
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var selectedPlaylistId by rememberSaveable { mutableStateOf<String?>(null) }

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var editingPlaylist by remember { mutableStateOf<UserPlaylist?>(null) }
    var deletingPlaylist by remember { mutableStateOf<UserPlaylist?>(null) }
    var showClearLinksConfirm by remember { mutableStateOf(false) }

    val playlistsState by libraryViewModel.playlistsState.collectAsStateWithLifecycle()
    val likedTracksState by libraryViewModel.likedTracksState.collectAsStateWithLifecycle()
    val likedIds by libraryViewModel.likedTrackIds.collectAsStateWithLifecycle()
    val historyState by libraryViewModel.historyState.collectAsStateWithLifecycle()
    val linkedAudio by settingsViewModel.linkedAudio.collectAsStateWithLifecycle()
    val combinedTracks by catalogViewModel.combinedTracks.collectAsStateWithLifecycle()
    val isRefreshing by libraryViewModel.isRefreshing.collectAsStateWithLifecycle()

    val tracksById = remember(combinedTracks) {
        combinedTracks.associateBy { it.id }
    }

    val activePlaylist = (playlistsState as? UiState.Success)?.data
        ?.firstOrNull { it.id == selectedPlaylistId }

    if (selectedPlaylistId != null && activePlaylist != null) {
        BackHandler { selectedPlaylistId = null }
        PlaylistDetailView(
            playlist = activePlaylist,
            tracks = activePlaylist.trackIds.mapNotNull { tracksById[it.toInt()] },
            linkedAudio = linkedAudio,
            playbackSnapshot = playbackSnapshot,
            likedIds = likedIds,
            preferences = preferences,
            onBack = { selectedPlaylistId = null },
            onPlayAll = { list ->
                if (list.isNotEmpty()) onTrackClick(list.first(), list)
            },
            onTrackClick = onTrackClick,
            onTrackLongPress = onTrackLongPress,
            onToggleLike = libraryViewModel::toggleLike,
            onSwipeQueue = onSwipeQueue,
            onMoveTrack = { from, to -> libraryViewModel.reorderPlaylistTrack(activePlaylist, from, to) },
            onRemoveTrack = { trackId -> libraryViewModel.removeTrackFromPlaylist(activePlaylist, trackId) },
            onEditPlaylist = { editingPlaylist = activePlaylist },
            onDeletePlaylist = { deletingPlaylist = activePlaylist }
        )
    } else {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = libraryViewModel::refresh,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 18.dp, top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "YOUR COLLECTION",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Library",
                                style = MaterialTheme.typography.displayMedium
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onBulkAutoMatchClick,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("library_bulk_match_button")
                            ) {
                                Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Link", style = MaterialTheme.typography.labelLarge)
                            }
                            Button(
                                onClick = { showCreatePlaylistDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color(0xFF0A0B10)
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("create_playlist_button")
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("New Mix", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(LibraryTab.entries.size) { index ->
                            val tab = LibraryTab.entries[index]
                            FilterChip(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                label = { Text(tab.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color(0xFF0A0B10)
                                ),
                                modifier = Modifier.testTag("library_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                when (LibraryTab.entries[selectedTabIndex]) {
                    LibraryTab.PLAYLISTS -> {
                        when (val state = playlistsState) {
                            is UiState.Loading -> {
                                item {
                                    Box(modifier = Modifier.padding(horizontal = 18.dp)) {
                                        TrackListShimmer(count = 4)
                                    }
                                }
                            }
                            is UiState.Error -> {
                                item {
                                    LibraryErrorCard(
                                        message = state.message,
                                        onRetry = libraryViewModel::refresh
                                    )
                                }
                            }
                            is UiState.Success -> {
                                if (state.data.isEmpty()) {
                                    item {
                                        LibraryEmptyCard(
                                            icon = Icons.Rounded.QueueMusic,
                                            title = "No Playlists Yet",
                                            body = stringResource(R.string.empty_playlists),
                                            ctaLabel = "Create Playlist",
                                            onCtaClick = { showCreatePlaylistDialog = true }
                                        )
                                    }
                                } else {
                                    items(state.data, key = { it.id }) { playlist ->
                                        PlaylistCardRow(
                                            playlist = playlist,
                                            onClick = { selectedPlaylistId = playlist.id },
                                            onPlay = {
                                                val list = playlist.trackIds.mapNotNull { tracksById[it.toInt()] }
                                                if (list.isNotEmpty()) {
                                                    onTrackClick(list.first(), list)
                                                } else {
                                                    selectedPlaylistId = playlist.id
                                                }
                                            },
                                            onEdit = { editingPlaylist = playlist },
                                            onDelete = { deletingPlaylist = playlist }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    LibraryTab.LIKED -> {
                        when (val state = likedTracksState) {
                            is UiState.Loading -> {
                                item {
                                    Box(modifier = Modifier.padding(horizontal = 18.dp)) {
                                        TrackListShimmer(count = 5)
                                    }
                                }
                            }
                            is UiState.Error -> {
                                item {
                                    LibraryErrorCard(
                                        message = state.message,
                                        onRetry = libraryViewModel::refresh
                                    )
                                }
                            }
                            is UiState.Success -> {
                                val likedTracks = state.data.mapNotNull { tracksById[it.trackNumber.toInt()] }
                                if (likedTracks.isEmpty()) {
                                    item {
                                        LibraryEmptyCard(
                                            icon = Icons.Rounded.Favorite,
                                            title = "No Liked Tracks",
                                            body = stringResource(R.string.empty_likes)
                                        )
                                    }
                                } else {
                                    item {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 18.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${likedTracks.size} Liked Songs",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Button(
                                                onClick = { onTrackClick(likedTracks.first(), likedTracks) },
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Play All")
                                            }
                                        }
                                    }
                                    items(likedTracks, key = { "liked_${it.id}" }) { track ->
                                        TrackTile(
                                            track = track,
                                            isLinked = linkedAudio.containsKey(track.id),
                                            isCurrentTrack = playbackSnapshot.currentTrack?.id == track.id,
                                            isPlaying = playbackSnapshot.isPlaying,
                                            isLiked = true,
                                            swipeQueueEnabled = preferences.swipeRowQueueEnabled,
                                            swipeLikeEnabled = preferences.swipeRowLikeEnabled,
                                            hapticEnabled = preferences.hapticFeedbackEnabled,
                                            onClick = { onTrackClick(track, likedTracks) },
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

                    LibraryTab.LINKED -> {
                        val linkedTracks = linkedAudio.keys.mapNotNull { tracksById[it] }
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${linkedTracks.size} Playable Track(s)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (linkedTracks.isNotEmpty()) {
                                    TextButton(onClick = { showClearLinksConfirm = true }) {
                                        Text("Clear All", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                        if (linkedTracks.isEmpty()) {
                            item {
                                LibraryEmptyCard(
                                    icon = Icons.Rounded.AudioFile,
                                    title = "No Custom MP3s Added",
                                    body = "All 24 bundled Studio MP3s play right away on tap. Optionally add your Music folder or custom MP3/AAC files to play your own recordings too.",
                                    ctaLabel = "Add Music Folder or Files",
                                    onCtaClick = onBulkAutoMatchClick
                                )
                            }
                        } else {
                            items(linkedTracks, key = { "linked_${it.id}" }) { track ->
                                val entry = linkedAudio[track.id]
                                LinkedTrackRow(
                                    track = track,
                                    entry = entry,
                                    isCurrentTrack = playbackSnapshot.currentTrack?.id == track.id,
                                    onPlay = { onTrackClick(track, linkedTracks) },
                                    onUnlink = { settingsViewModel.unlinkTrackAudio(track.id) }
                                )
                            }
                        }
                    }

                    LibraryTab.HISTORY -> {
                        when (val state = historyState) {
                            is UiState.Loading -> {
                                item {
                                    Box(modifier = Modifier.padding(horizontal = 18.dp)) {
                                        TrackListShimmer(count = 5)
                                    }
                                }
                            }
                            is UiState.Error -> {
                                item {
                                    LibraryErrorCard(
                                        message = state.message,
                                        onRetry = libraryViewModel::refresh
                                    )
                                }
                            }
                            is UiState.Success -> {
                                val historyTracks = state.data.mapNotNull { tracksById[it.trackNumber.toInt()] }
                                if (historyTracks.isEmpty()) {
                                    item {
                                        LibraryEmptyCard(
                                            icon = Icons.Rounded.History,
                                            title = "No Recent Plays",
                                            body = stringResource(R.string.empty_history)
                                        )
                                    }
                                } else {
                                    itemsIndexed(state.data, key = { index, item -> "${item.id}_$index" }) { _, entry ->
                                        val track = tracksById[entry.trackNumber.toInt()]
                                        if (track != null) {
                                            TrackTile(
                                                track = track,
                                                isLinked = linkedAudio.containsKey(track.id),
                                                isCurrentTrack = playbackSnapshot.currentTrack?.id == track.id,
                                                isPlaying = playbackSnapshot.isPlaying,
                                                isLiked = likedIds.contains(track.id),
                                                swipeQueueEnabled = preferences.swipeRowQueueEnabled,
                                                swipeLikeEnabled = preferences.swipeRowLikeEnabled,
                                                hapticEnabled = preferences.hapticFeedbackEnabled,
                                                onClick = { onTrackClick(track, historyTracks.distinctBy { it.id }) },
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
                    }
                }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        PlaylistEditorDialog(
            title = "Create Playlist",
            initialName = "",
            initialDescription = "",
            initialCoverKey = ArtistArtwork.portraits.first().first,
            confirmLabel = "Create",
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = { name, desc, coverKey ->
                libraryViewModel.createPlaylist(name, desc, coverKey)
                showCreatePlaylistDialog = false
            }
        )
    }

    editingPlaylist?.let { playlist ->
        PlaylistEditorDialog(
            title = "Edit Playlist",
            initialName = playlist.name,
            initialDescription = playlist.description,
            initialCoverKey = playlist.coverKey,
            confirmLabel = "Save",
            onDismiss = { editingPlaylist = null },
            onConfirm = { name, desc, _ ->
                libraryViewModel.renamePlaylist(playlist, name, desc)
                editingPlaylist = null
            }
        )
    }

    deletingPlaylist?.let { playlist ->
        AlertDialog(
            onDismissRequest = { deletingPlaylist = null },
            title = { Text("Delete Playlist?") },
            text = { Text("Remove \"${playlist.name}\" from your library? Your catalog tracks will remain intact.") },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedPlaylistId == playlist.id) selectedPlaylistId = null
                        libraryViewModel.deletePlaylist(playlist)
                        deletingPlaylist = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingPlaylist = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showClearLinksConfirm) {
        AlertDialog(
            onDismissRequest = { showClearLinksConfirm = false },
            title = { Text("Clear Local Audio Links?") },
            text = { Text("This unlinks your device audio URIs from tracks in Isai Studio. The original files on your device will not be deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        settingsViewModel.clearAllLinkedAudio()
                        showClearLinksConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearLinksConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun PlaylistCardRow(
    playlist: UserPlaylist,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .clickable(onClick = onClick)
            .testTag("playlist_card_${playlist.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = ArtistArtwork.drawableForKey(playlist.coverKey)),
                contentDescription = playlist.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (playlist.description.isNotBlank()) {
                    Text(
                        text = playlist.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "${playlist.trackIds.size} tracks",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.minimumInteractiveComponentSize()) {
                Icon(Icons.Rounded.Edit, contentDescription = "Rename playlist")
            }
            IconButton(onClick = onDelete, modifier = Modifier.minimumInteractiveComponentSize()) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete playlist")
            }
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier.size(42.dp)
            ) {
                IconButton(onClick = onPlay) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play ${playlist.name}",
                        tint = Color(0xFF0A0B10)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistDetailView(
    playlist: UserPlaylist,
    tracks: List<Track>,
    linkedAudio: Map<Int, LinkedAudioEntry>,
    playbackSnapshot: PlaybackSnapshot,
    likedIds: Set<Int>,
    preferences: UserPreferences,
    onBack: () -> Unit,
    onPlayAll: (List<Track>) -> Unit,
    onTrackClick: (Track, List<Track>) -> Unit,
    onTrackLongPress: (Track) -> Unit,
    onToggleLike: (Track) -> Unit,
    onSwipeQueue: (Track) -> Unit,
    onMoveTrack: (Int, Int) -> Unit,
    onRemoveTrack: (Int) -> Unit,
    onEditPlaylist: () -> Unit,
    onDeletePlaylist: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("playlist_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                }
                Row {
                    IconButton(onClick = onEditPlaylist) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit playlist")
                    }
                    IconButton(onClick = onDeletePlaylist) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete playlist")
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = ArtistArtwork.drawableForKey(playlist.coverKey)),
                    contentDescription = playlist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(164.dp)
                        .clip(RoundedCornerShape(22.dp))
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.headlineLarge
                )
                if (playlist.description.isNotBlank()) {
                    Text(
                        text = playlist.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { onPlayAll(tracks) },
                    enabled = tracks.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color(0xFF0A0B10)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Play Playlist (${tracks.size})", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        if (tracks.isEmpty()) {
            item {
                LibraryEmptyCard(
                    icon = Icons.Rounded.QueueMusic,
                    title = "Playlist is Empty",
                    body = "Long-press any track or tap its three-dot menu to add tracks to \"${playlist.name}\"."
                )
            }
        } else {
            itemsIndexed(tracks, key = { idx, t -> "${t.id}_$idx" }) { index, track ->
                Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                    TrackTile(
                        track = track,
                        isLinked = linkedAudio.containsKey(track.id),
                        isCurrentTrack = playbackSnapshot.currentTrack?.id == track.id,
                        isPlaying = playbackSnapshot.isPlaying,
                        isLiked = likedIds.contains(track.id),
                        swipeQueueEnabled = preferences.swipeRowQueueEnabled,
                        swipeLikeEnabled = preferences.swipeRowLikeEnabled,
                        hapticEnabled = preferences.hapticFeedbackEnabled,
                        onClick = { onTrackClick(track, tracks) },
                        onLongPress = { onTrackLongPress(track) },
                        onSwipeAddToQueue = { onSwipeQueue(track) },
                        onSwipeToggleLike = { onToggleLike(track) },
                        onToggleLikeClick = { onToggleLike(track) }
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (index > 0) {
                            IconButton(
                                onClick = { onMoveTrack(index, index - 1) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move up", modifier = Modifier.size(16.dp))
                            }
                        }
                        if (index < tracks.lastIndex) {
                            IconButton(
                                onClick = { onMoveTrack(index, index + 1) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move down", modifier = Modifier.size(16.dp))
                            }
                        }
                        IconButton(
                            onClick = { onRemoveTrack(track.id) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove from playlist", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkedTrackRow(
    track: Track,
    entry: LinkedAudioEntry?,
    isCurrentTrack: Boolean,
    onPlay: () -> Unit,
    onUnlink: () -> Unit
) {
    Surface(
        color = if (isCurrentTrack) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .clickable(onClick = onPlay)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = track.artworkRes),
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = entry?.displayName ?: "Linked local audio",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onUnlink,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(Icons.Rounded.LinkOff, contentDescription = "Unlink audio")
            }
        }
    }
}

@Composable
private fun PlaylistEditorDialog(
    title: String,
    initialName: String,
    initialDescription: String,
    initialCoverKey: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String, coverKey: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var desc by remember { mutableStateOf(initialDescription) }
    var selectedCover by remember { mutableStateOf(initialCoverKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.headlineMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("playlist_dialog_name_input")
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description (optional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Choose Cover Portrait",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ArtistArtwork.portraits) { (key, res) ->
                        val isSelected = selectedCover == key
                        Image(
                            painter = painterResource(id = res),
                            contentDescription = key,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .then(
                                    if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                                    else Modifier
                                )
                                .clickable { selectedCover = key }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, desc, selectedCover) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("playlist_dialog_confirm")
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun LibraryEmptyCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    ctaLabel: String? = null,
    onCtaClick: (() -> Unit)? = null
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(text = title, style = MaterialTheme.typography.headlineMedium)
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (ctaLabel != null && onCtaClick != null) {
                Button(onClick = onCtaClick) {
                    Text(ctaLabel)
                }
            }
        }
    }
}

@Composable
private fun LibraryErrorCard(
    message: String,
    onRetry: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Sync Error", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
            Text(message, style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}
