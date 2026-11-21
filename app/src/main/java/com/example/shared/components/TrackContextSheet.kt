package com.example.shared.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.model.LinkedAudioEntry
import com.example.core.model.Track
import com.example.core.model.UserPlaylist

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackContextBottomSheet(
    track: Track,
    linkedEntry: LinkedAudioEntry?,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onLinkAudio: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onEditLyrics: () -> Unit,
    onGoToAlbum: () -> Unit
) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = track.artworkRes),
                    contentDescription = track.album,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${track.album} • ${track.language}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (linkedEntry != null) {
                        Text(
                            text = "Linked: ${linkedEntry.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )

            SheetActionItem(
                icon = Icons.Rounded.AudioFile,
                label = if (linkedEntry == null) "Link Local Audio (MP3/AAC)" else "Replace Local Audio File",
                onClick = { onDismiss(); onLinkAudio() },
                testTag = "context_link_audio"
            )
            SheetActionItem(
                icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                label = "Play Next",
                onClick = { onDismiss(); onPlayNext() },
                testTag = "context_play_next"
            )
            SheetActionItem(
                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                label = "Add to Queue",
                onClick = { onDismiss(); onAddToQueue() },
                testTag = "context_add_queue"
            )
            SheetActionItem(
                icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                label = "Add to Playlist",
                onClick = { onDismiss(); onAddToPlaylist() },
                testTag = "context_add_playlist"
            )
            SheetActionItem(
                icon = Icons.Rounded.Lyrics,
                label = "Attach / Edit Synced LRC Lyrics",
                onClick = { onDismiss(); onEditLyrics() },
                testTag = "context_edit_lyrics"
            )
            SheetActionItem(
                icon = Icons.Rounded.Album,
                label = "Go to Album",
                onClick = { onDismiss(); onGoToAlbum() },
                testTag = "context_go_album"
            )
            SheetActionItem(
                icon = Icons.Rounded.Share,
                label = "Share Track",
                onClick = {
                    onDismiss()
                    shareTrackIntent(context, track)
                },
                testTag = "context_share"
            )
        }
    }
}

@Composable
private fun SheetActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun LinkAudioPromptDialog(
    track: Track,
    onDismiss: () -> Unit,
    onPickFolder: () -> Unit,
    onBulkAutoMatch: () -> Unit,
    onPickSingleFile: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Music to Play",
                style = MaterialTheme.typography.headlineMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "\"${track.title}\" includes built-in Studio MP3 audio. You can also import your own music folder or custom MP3 files anytime.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Matching filenames replace or link into the catalog, and any extra audio files are added to Device Music so nothing is skipped.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onDismiss(); onPickFolder() },
                        modifier = Modifier.fillMaxWidth().testTag("dialog_add_music_folder")
                    ) {
                        Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add Music Folder (All Songs)")
                    }
                    OutlinedButton(
                        onClick = { onDismiss(); onBulkAutoMatch() },
                        modifier = Modifier.fillMaxWidth().testTag("dialog_bulk_match")
                    ) {
                        Icon(Icons.Rounded.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Select Multiple Audio Files")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onDismiss(); onPickSingleFile() },
                modifier = Modifier.testTag("dialog_pick_single_audio")
            ) {
                Text("Pick 1 File for This Track")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddMusicMethodDialog(
    onDismiss: () -> Unit,
    onPickFolder: () -> Unit,
    onSelectFiles: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Your Music",
                style = MaterialTheme.typography.headlineMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Import all your MP3, AAC, M4A, or FLAC files at once. Matching filenames link to the Studio catalog and any other songs are added to Device Music.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { onDismiss(); onPickFolder() },
                    modifier = Modifier.fillMaxWidth().testTag("add_music_folder_button")
                ) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Choose Music Folder")
                }
                OutlinedButton(
                    onClick = { onDismiss(); onSelectFiles() },
                    modifier = Modifier.fillMaxWidth().testTag("add_music_files_button")
                ) {
                    Icon(Icons.Rounded.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Select Audio Files")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddToPlaylistDialog(
    track: Track,
    playlists: List<UserPlaylist>,
    onDismiss: () -> Unit,
    onSelectPlaylist: (UserPlaylist) -> Unit,
    onCreateNewPlaylist: (name: String, description: String) -> Unit
) {
    var showCreateFields by remember { mutableStateOf(playlists.isEmpty()) }
    var newName by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (showCreateFields) "New Playlist" else "Add to Playlist",
                style = MaterialTheme.typography.headlineMedium
            )
        },
        text = {
            if (showCreateFields) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Adding \"${track.title}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Playlist Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_playlist_name_input")
                    )
                    OutlinedTextField(
                        value = newDesc,
                        onValueChange = { newDesc = it },
                        label = { Text("Description (optional)") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(playlists, key = { it.id }) { pl ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectPlaylist(pl); onDismiss() }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(pl.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text("${pl.trackIds.size} tracks", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.Rounded.Add, contentDescription = null)
                            }
                        }
                    }
                    item {
                        TextButton(
                            onClick = { showCreateFields = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Create New Playlist")
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (showCreateFields) {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            onCreateNewPlaylist(newName, newDesc)
                            onDismiss()
                        }
                    },
                    enabled = newName.isNotBlank()
                ) {
                    Text("Create & Add")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun LrcEditorDialog(
    track: Track,
    initialLrc: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember(initialLrc) {
        mutableStateOf(
            initialLrc.ifBlank {
                "[00:00.00]${track.title} — ${track.composer}\n[00:08.00]Album: ${track.album}\n[00:16.00]Add timestamped [mm:ss.xx] lyrics lines here"
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Synced LRC Lyrics", style = MaterialTheme.typography.headlineMedium)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Paste standard [mm:ss.xx] LRC lines for \"${track.title}\". Lines sync automatically with ExoPlayer.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .testTag("lrc_editor_input"),
                    textStyle = MaterialTheme.typography.labelMedium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onSave(text)
                        onDismiss()
                    }
                },
                enabled = text.isNotBlank(),
                modifier = Modifier.testTag("lrc_save_button")
            ) {
                Text("Save Lyrics")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
