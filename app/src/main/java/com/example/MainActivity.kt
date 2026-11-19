package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.core.di.AppContainer
import com.example.core.model.AccentTheme
import com.example.core.model.Track
import com.example.core.model.UiState
import com.example.core.navigation.AppRoute
import com.example.features.auth.presentation.AuthScreen
import com.example.features.auth.presentation.AuthViewModel
import com.example.features.catalog.presentation.AlbumDetailScreen
import com.example.features.catalog.presentation.ArtistDetailScreen
import com.example.features.catalog.presentation.CatalogViewModel
import com.example.features.catalog.presentation.HomeScreen
import com.example.features.catalog.presentation.SearchScreen
import com.example.features.intro.InteractiveIntroScreen
import com.example.features.library.presentation.LibraryScreen
import com.example.features.library.presentation.LibraryViewModel
import com.example.features.player.presentation.EqualizerBottomSheet
import com.example.features.player.presentation.FullPlayerSheet
import com.example.features.player.presentation.MiniPlayerBar
import com.example.features.player.presentation.PlayerViewModel
import com.example.features.player.presentation.SleepTimerBottomSheet
import com.example.features.settings.presentation.SettingsScreen
import com.example.features.settings.presentation.SettingsViewModel
import com.example.shared.components.AddToPlaylistDialog
import com.example.shared.components.LinkAudioPromptDialog
import com.example.shared.components.LrcEditorDialog
import com.example.shared.components.TrackContextBottomSheet
import com.example.ui.theme.IsaiStudioTheme
import com.example.ui.theme.ObsidianCanvas
import com.google.firebase.auth.FirebaseUser

private data class TopLevelNavItem(
    val route: AppRoute,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as IsaiStudioApplication).container

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val authViewModel: AuthViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        AuthViewModel(
                            authRepository = container.authRepository,
                            localAudioStore = container.localAudioStore
                        )
                    }
                }
            )

            val introCompleted by authViewModel.introCompleted.collectAsStateWithLifecycle()
            val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()

            when {
                introCompleted == null -> {
                    IsaiStudioTheme(darkTheme = true, accentTheme = AccentTheme.CORAL) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(ObsidianCanvas)
                        )
                    }
                }

                introCompleted == false -> {
                    IsaiStudioTheme(darkTheme = true, accentTheme = AccentTheme.CORAL) {
                        InteractiveIntroScreen(
                            onFinishIntro = { authViewModel.completeIntro() }
                        )
                    }
                }

                currentUser == null -> {
                    IsaiStudioTheme(darkTheme = true, accentTheme = AccentTheme.CORAL) {
                        AuthScreen(
                            authViewModel = authViewModel,
                            onReplayIntro = { authViewModel.replayIntro() }
                        )
                    }
                }

                else -> {
                    val user = currentUser!!
                    key(user.uid) {
                        AuthenticatedStudioApp(
                            user = user,
                            container = container,
                            windowSizeClass = windowSizeClass,
                            onSignOut = { authViewModel.signOut(this) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedStudioApp(
    user: FirebaseUser,
    container: AppContainer,
    windowSizeClass: WindowSizeClass,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current

    val catalogViewModel: CatalogViewModel = viewModel(
        key = "catalog_${user.uid}",
        factory = viewModelFactory {
            initializer {
                CatalogViewModel(
                    catalogRepository = container.catalogRepository,
                    localAudioStore = container.localAudioStore,
                    searchCatalogUseCase = container.searchCatalogUseCase
                )
            }
        }
    )

    val libraryViewModel: LibraryViewModel = viewModel(
        key = "library_${user.uid}",
        factory = viewModelFactory {
            initializer {
                LibraryViewModel(
                    repository = container.libraryRepository,
                    currentUserId = user.uid
                )
            }
        }
    )

    val playerViewModel: PlayerViewModel = viewModel(
        key = "player_${user.uid}",
        factory = viewModelFactory {
            initializer {
                PlayerViewModel(
                    playbackCoordinator = container.playbackCoordinator,
                    localAudioStore = container.localAudioStore,
                    libraryRepository = container.libraryRepository,
                    lyricsRepository = container.lyricsRepository,
                    parseLrcUseCase = container.parseLrcUseCase,
                    currentUserId = user.uid
                )
            }
        }
    )

    val settingsViewModel: SettingsViewModel = viewModel(
        key = "settings_${user.uid}",
        factory = viewModelFactory {
            initializer {
                SettingsViewModel(
                    preferencesRepository = container.preferencesRepository,
                    localAudioStore = container.localAudioStore,
                    catalogRepository = container.catalogRepository,
                    autoMatchAudioFilesUseCase = container.autoMatchAudioFilesUseCase,
                    currentUserId = user.uid
                )
            }
        }
    )

    val preferences by settingsViewModel.preferences.collectAsStateWithLifecycle()
    val snapshot by playerViewModel.snapshot.collectAsStateWithLifecycle()
    val isFullPlayerExpanded by playerViewModel.isFullPlayerExpanded.collectAsStateWithLifecycle()
    val unlinkedTrackPrompt by playerViewModel.unlinkedTrackPrompt.collectAsStateWithLifecycle()
    val eqState by playerViewModel.eqState.collectAsStateWithLifecycle()
    val activeEqPreset by playerViewModel.activeEqPreset.collectAsStateWithLifecycle()
    val likedIds by libraryViewModel.likedTrackIds.collectAsStateWithLifecycle()
    val playlistsState by libraryViewModel.playlistsState.collectAsStateWithLifecycle()
    val linkedAudio by settingsViewModel.linkedAudio.collectAsStateWithLifecycle()

    LaunchedEffect(preferences) {
        playerViewModel.setPrivateSession(preferences.privateSessionEnabled)
        playerViewModel.applyAudioSettings(
            crossfadeSec = preferences.crossfadeSeconds.toInt(),
            normalizeVolume = preferences.normalizeVolumeEnabled,
            preset = preferences.eqPreset,
            eqBands = preferences.eqBands
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(libraryViewModel) {
        libraryViewModel.snackbarMessages.collect { msg -> snackbarHostState.showSnackbar(msg) }
    }
    LaunchedEffect(playerViewModel) {
        playerViewModel.snackbarMessages.collect { msg -> snackbarHostState.showSnackbar(msg) }
    }
    LaunchedEffect(settingsViewModel) {
        settingsViewModel.messages.collect { msg -> snackbarHostState.showSnackbar(msg) }
    }
    LaunchedEffect(snapshot.playbackError) {
        val error = snapshot.playbackError
        if (error != null) {
            snackbarHostState.showSnackbar(error)
            playerViewModel.clearPlaybackError()
        }
    }

    // SAF Audio Linking State
    var pendingLinkTrack by remember { mutableStateOf<Track?>(null) }
    var pendingPlayContext by remember { mutableStateOf<List<Track>?>(null) }

    val singleAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val track = pendingLinkTrack
        val contextTracks = pendingPlayContext
        pendingLinkTrack = null
        pendingPlayContext = null
        if (uri != null && track != null) {
            settingsViewModel.linkSingleTrackAudio(track, uri) {
                if (contextTracks != null) {
                    playerViewModel.playTrack(track, contextTracks)
                }
            }
        }
    }

    val bulkAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            settingsViewModel.autoMatchMultipleAudioFiles(uris)
        }
    }

    var askedNotificationPermission by rememberSaveable { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* playback works regardless of notification denial */ }

    fun ensureNotificationPermissionOnce() {
        if (!askedNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            askedNotificationPermission = true
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Sheet & Dialog State
    var contextMenuTrack by remember { mutableStateOf<Track?>(null) }
    var addToPlaylistTrack by remember { mutableStateOf<Track?>(null) }
    var editingLyricsTrack by remember { mutableStateOf<Track?>(null) }
    var showSettingsEqSheet by remember { mutableStateOf(false) }
    var showSettingsSleepSheet by remember { mutableStateOf(false) }
    var showIntroReplay by rememberSaveable { mutableStateOf(false) }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val topLevelItems = remember {
        listOf(
            TopLevelNavItem(
                route = AppRoute.Home,
                labelRes = R.string.nav_home,
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home,
                testTag = "nav_home"
            ),
            TopLevelNavItem(
                route = AppRoute.Search,
                labelRes = R.string.nav_search,
                selectedIcon = Icons.Filled.Search,
                unselectedIcon = Icons.Outlined.Search,
                testTag = "nav_search"
            ),
            TopLevelNavItem(
                route = AppRoute.Library,
                labelRes = R.string.nav_library,
                selectedIcon = Icons.Filled.LibraryMusic,
                unselectedIcon = Icons.Outlined.LibraryMusic,
                testTag = "nav_library"
            ),
            TopLevelNavItem(
                route = AppRoute.Settings,
                labelRes = R.string.nav_settings,
                selectedIcon = Icons.Filled.Tune,
                unselectedIcon = Icons.Outlined.Tune,
                testTag = "nav_settings"
            )
        )
    }

    fun navigateTopLevel(route: AppRoute) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val playTrackAction: (Track, List<Track>) -> Unit = { track, contextList ->
        ensureNotificationPermissionOnce()
        pendingPlayContext = contextList
        playerViewModel.playTrack(track, contextList)
    }

    val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact

    IsaiStudioTheme(
        darkTheme = preferences.darkThemeEnabled,
        accentTheme = AccentTheme.fromKey(preferences.accentKey)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    if (isCompact) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            if (snapshot.currentTrack != null) {
                                MiniPlayerBar(
                                    snapshot = snapshot,
                                    isLiked = likedIds.contains(snapshot.currentTrack?.id),
                                    preferences = preferences,
                                    onExpand = playerViewModel::expandPlayer,
                                    onTogglePlayPause = playerViewModel::togglePlayPause,
                                    onSkipNext = playerViewModel::skipNext,
                                    onSkipPrevious = playerViewModel::skipPrevious,
                                    onToggleLike = {
                                        snapshot.currentTrack?.let { libraryViewModel.toggleLike(it) }
                                    }
                                )
                            }
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                windowInsets = WindowInsets.navigationBars
                            ) {
                                topLevelItems.forEach { item ->
                                    val selected = currentDestination?.hasRoute(item.route::class) == true
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateTopLevel(item.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = stringResource(item.labelRes)
                                            )
                                        },
                                        label = { Text(stringResource(item.labelRes)) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                        ),
                                        modifier = Modifier.testTag(item.testTag)
                                    )
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (!isCompact) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            topLevelItems.forEach { item ->
                                val selected = currentDestination?.hasRoute(item.route::class) == true
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(item.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = stringResource(item.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(item.labelRes)) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    modifier = Modifier.testTag(item.testTag)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = AppRoute.Home,
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 940.dp)
                        ) {
                            composable<AppRoute.Home> {
                                HomeScreen(
                                    catalogViewModel = catalogViewModel,
                                    libraryViewModel = libraryViewModel,
                                    playbackSnapshot = snapshot,
                                    preferences = preferences,
                                    onTrackClick = playTrackAction,
                                    onTrackLongPress = { contextMenuTrack = it },
                                    onSwipeQueue = { playerViewModel.addToQueue(it) },
                                    onAlbumClick = { album ->
                                        navController.navigate(AppRoute.AlbumDetail(album.albumKey))
                                    },
                                    onArtistClick = {
                                        navController.navigate(AppRoute.ArtistDetail)
                                    },
                                    onReplayIntroClick = { showIntroReplay = true },
                                    onBulkAutoMatchClick = { bulkAudioLauncher.launch(arrayOf("audio/*")) }
                                )
                            }

                            composable<AppRoute.Search> {
                                SearchScreen(
                                    catalogViewModel = catalogViewModel,
                                    libraryViewModel = libraryViewModel,
                                    playbackSnapshot = snapshot,
                                    preferences = preferences,
                                    onTrackClick = playTrackAction,
                                    onTrackLongPress = { contextMenuTrack = it },
                                    onSwipeQueue = { playerViewModel.addToQueue(it) },
                                    onAlbumClick = { album ->
                                        navController.navigate(AppRoute.AlbumDetail(album.albumKey))
                                    }
                                )
                            }

                            composable<AppRoute.Library> {
                                LibraryScreen(
                                    catalogViewModel = catalogViewModel,
                                    libraryViewModel = libraryViewModel,
                                    settingsViewModel = settingsViewModel,
                                    playbackSnapshot = snapshot,
                                    preferences = preferences,
                                    onTrackClick = playTrackAction,
                                    onTrackLongPress = { contextMenuTrack = it },
                                    onSwipeQueue = { playerViewModel.addToQueue(it) },
                                    onBulkAutoMatchClick = { bulkAudioLauncher.launch(arrayOf("audio/*")) }
                                )
                            }

                            composable<AppRoute.Settings> {
                                SettingsScreen(
                                    currentUser = user,
                                    settingsViewModel = settingsViewModel,
                                    playbackSnapshot = snapshot,
                                    activeEqPreset = activeEqPreset,
                                    onOpenEqualizer = { showSettingsEqSheet = true },
                                    onOpenSleepTimer = { showSettingsSleepSheet = true },
                                    onBulkAutoMatchClick = { bulkAudioLauncher.launch(arrayOf("audio/*")) },
                                    onReplayIntroClick = { showIntroReplay = true },
                                    onSignOutClick = onSignOut
                                )
                            }

                            composable<AppRoute.AlbumDetail> { entry ->
                                val args = entry.toRoute<AppRoute.AlbumDetail>()
                                AlbumDetailScreen(
                                    albumKey = args.albumKey,
                                    catalogViewModel = catalogViewModel,
                                    libraryViewModel = libraryViewModel,
                                    playbackSnapshot = snapshot,
                                    preferences = preferences,
                                    onBack = { navController.popBackStack() },
                                    onPlayTrack = playTrackAction,
                                    onTrackLongPress = { contextMenuTrack = it },
                                    onSwipeQueue = { playerViewModel.addToQueue(it) },
                                    onBulkAutoMatch = { bulkAudioLauncher.launch(arrayOf("audio/*")) }
                                )
                            }

                            composable<AppRoute.ArtistDetail> {
                                ArtistDetailScreen(
                                    catalogViewModel = catalogViewModel,
                                    libraryViewModel = libraryViewModel,
                                    playbackSnapshot = snapshot,
                                    preferences = preferences,
                                    onBack = { navController.popBackStack() },
                                    onReplayIntro = { showIntroReplay = true },
                                    onAlbumClick = { album ->
                                        navController.navigate(AppRoute.AlbumDetail(album.albumKey))
                                    },
                                    onPlayTrack = playTrackAction,
                                    onTrackLongPress = { contextMenuTrack = it },
                                    onSwipeQueue = { playerViewModel.addToQueue(it) }
                                )
                            }
                        }

                        if (!isCompact && snapshot.currentTrack != null) {
                            MiniPlayerBar(
                                snapshot = snapshot,
                                isLiked = likedIds.contains(snapshot.currentTrack?.id),
                                preferences = preferences,
                                onExpand = playerViewModel::expandPlayer,
                                onTogglePlayPause = playerViewModel::togglePlayPause,
                                onSkipNext = playerViewModel::skipNext,
                                onSkipPrevious = playerViewModel::skipPrevious,
                                onToggleLike = {
                                    snapshot.currentTrack?.let { libraryViewModel.toggleLike(it) }
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .widthIn(max = 680.dp)
                                    .padding(bottom = 12.dp)
                            )
                        }
                    }
                }
            }

            // Full-Screen Dynamic Player Sheet
            AnimatedVisibility(
                visible = isFullPlayerExpanded && snapshot.currentTrack != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                FullPlayerSheet(
                    playerViewModel = playerViewModel,
                    isLiked = likedIds.contains(snapshot.currentTrack?.id),
                    preferences = preferences,
                    onToggleLike = libraryViewModel::toggleLike,
                    onSaveEqToCloud = settingsViewModel::saveEqualizerSettings,
                    onMinimize = playerViewModel::minimizePlayer
                )
            }

            // Replayable 6-Photo Interactive Story Intro
            AnimatedVisibility(
                visible = showIntroReplay,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                InteractiveIntroScreen(
                    onFinishIntro = { showIntroReplay = false },
                    isReplay = true
                )
            }
        }

        // Context Action Sheet
        contextMenuTrack?.let { track ->
            TrackContextBottomSheet(
                track = track,
                linkedEntry = linkedAudio[track.id],
                onDismiss = { contextMenuTrack = null },
                onPlayNext = { playerViewModel.playNext(track) },
                onAddToQueue = { playerViewModel.addToQueue(track) },
                onLinkAudio = {
                    pendingLinkTrack = track
                    pendingPlayContext = null
                    singleAudioLauncher.launch(arrayOf("audio/*"))
                },
                onAddToPlaylist = { addToPlaylistTrack = track },
                onEditLyrics = { editingLyricsTrack = track },
                onGoToAlbum = {
                    navController.navigate(AppRoute.AlbumDetail(track.albumKey))
                }
            )
        }

        // Unlinked Track Prompt (when tapping a catalog track that has no audio file yet)
        unlinkedTrackPrompt?.let { track ->
            LinkAudioPromptDialog(
                track = track,
                onDismiss = {
                    pendingPlayContext = null
                    playerViewModel.dismissUnlinkedPrompt()
                },
                onPickSingleFile = {
                    pendingLinkTrack = track
                    singleAudioLauncher.launch(arrayOf("audio/*"))
                },
                onBulkAutoMatch = {
                    pendingPlayContext = null
                    bulkAudioLauncher.launch(arrayOf("audio/*"))
                }
            )
        }

        // Add to Playlist Dialog
        addToPlaylistTrack?.let { track ->
            val currentPlaylists = (playlistsState as? UiState.Success)?.data ?: emptyList()
            AddToPlaylistDialog(
                track = track,
                playlists = currentPlaylists,
                onDismiss = { addToPlaylistTrack = null },
                onSelectPlaylist = { playlist -> libraryViewModel.addTrackToPlaylist(playlist, track) },
                onCreateNewPlaylist = { name, desc ->
                    libraryViewModel.createPlaylist(
                        name = name,
                        description = desc,
                        coverKey = track.artworkKey,
                        initialTrackId = track.id
                    )
                }
            )
        }

        // Synced LRC Lyrics Editor (from track context menu)
        editingLyricsTrack?.let { track ->
            val rawLrc by playerViewModel.activeTrackRawLrc.collectAsStateWithLifecycle()
            val initial = if (snapshot.currentTrack?.id == track.id) rawLrc else ""
            LrcEditorDialog(
                track = track,
                initialLrc = initial,
                onDismiss = { editingLyricsTrack = null },
                onSave = { content -> playerViewModel.saveLyrics(track.id, content) }
            )
        }

        // Equalizer Sheet (triggered from Studio Settings)
        if (showSettingsEqSheet) {
            EqualizerBottomSheet(
                eqState = eqState,
                presets = playerViewModel.eqPresets,
                activePreset = activeEqPreset,
                onDismiss = {
                    showSettingsEqSheet = false
                    settingsViewModel.saveEqualizerSettings(activeEqPreset, eqState.bandLevelsMb)
                },
                onSelectPreset = { preset ->
                    playerViewModel.selectEqPreset(preset)
                    settingsViewModel.saveEqualizerSettings(preset.name, preset.bandsMb)
                },
                onBandChanged = { idx, level -> playerViewModel.setEqBand(idx, level) }
            )
        }

        // Sleep Timer Sheet (triggered from Studio Settings)
        if (showSettingsSleepSheet) {
            SleepTimerBottomSheet(
                remainingMs = snapshot.sleepTimerRemainingMs,
                sleepAtEndOfTrack = snapshot.sleepAtEndOfTrack,
                onDismiss = { showSettingsSleepSheet = false },
                onSelectMinutes = playerViewModel::startSleepTimer,
                onSelectEndOfTrack = playerViewModel::enableSleepAtEndOfTrack,
                onCancelTimer = playerViewModel::cancelSleepTimer
            )
        }
    }
}
