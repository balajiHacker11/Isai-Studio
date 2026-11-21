package com.example.features.catalog.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.domain.SearchCatalogUseCase
import com.example.core.local.LocalAudioStore
import com.example.core.model.Album
import com.example.core.model.LinkedAudioEntry
import com.example.core.model.Track
import com.example.core.model.TrackType
import com.example.features.catalog.data.CatalogRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class HomeFilter(val label: String) {
    ALL("All"),
    LOCAL_MP3("My MP3s"),
    DEVICE("Device Music"),
    SONGS("Songs"),
    BGMS("Original BGMs"),
    THEMES("Themes"),
    VERSIONS("Versions")
}

data class SearchUiState(
    val query: String = "",
    val selectedType: TrackType? = null,
    val selectedLanguage: String? = null,
    val linkedOnly: Boolean = false,
    val results: List<Track> = emptyList()
)

class CatalogViewModel(
    private val catalogRepository: CatalogRepository,
    private val localAudioStore: LocalAudioStore,
    private val searchCatalogUseCase: SearchCatalogUseCase
) : ViewModel() {

    val allAlbums: List<Album> = catalogRepository.albums
    val allTracks: List<Track> = catalogRepository.tracks

    val customTracks: StateFlow<List<Track>> = localAudioStore.customTracksFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val combinedTracks: StateFlow<List<Track>> = customTracks
        .combine(MutableStateFlow(allTracks)) { custom, catalog ->
            catalog + custom
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = allTracks
        )

    val linkedAudio: StateFlow<Map<Int, LinkedAudioEntry>> = localAudioStore.linkedAudioFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyMap()
        )

    private val _selectedHomeFilter = MutableStateFlow(HomeFilter.ALL)
    val selectedHomeFilter: StateFlow<HomeFilter> = _selectedHomeFilter.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _searchType = MutableStateFlow<TrackType?>(null)
    private val _searchLanguage = MutableStateFlow<String?>(null)
    private val _searchLinkedOnly = MutableStateFlow(false)

    val filteredHomeTracks: StateFlow<List<Track>> = combine(
        _selectedHomeFilter,
        linkedAudio,
        combinedTracks
    ) { filter, linked, mergedTracks ->
        when (filter) {
            HomeFilter.ALL -> mergedTracks
            HomeFilter.LOCAL_MP3 -> mergedTracks.filter { linked.containsKey(it.id) }
            HomeFilter.DEVICE -> mergedTracks.filter { it.albumKey == "device_local" }
            HomeFilter.SONGS -> mergedTracks.filter { it.trackType == TrackType.SONG }
            HomeFilter.BGMS -> mergedTracks.filter { it.trackType == TrackType.BGM }
            HomeFilter.THEMES -> mergedTracks.filter { it.trackType == TrackType.THEME }
            HomeFilter.VERSIONS -> mergedTracks.filter { it.trackType == TrackType.VERSION }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = allTracks
    )

    val searchUiState: StateFlow<SearchUiState> = combine(
        _searchQuery,
        _searchType,
        _searchLanguage,
        _searchLinkedOnly,
        linkedAudio,
        combinedTracks
    ) { values ->
        val query = values[0] as String
        val type = values[1] as TrackType?
        val lang = values[2] as String?
        val linkedOnly = values[3] as Boolean
        @Suppress("UNCHECKED_CAST")
        val linked = values[4] as Map<Int, LinkedAudioEntry>
        @Suppress("UNCHECKED_CAST")
        val mergedTracks = values[5] as List<Track>

        val results = searchCatalogUseCase(
            tracks = mergedTracks,
            query = query,
            trackTypeFilter = type,
            languageFilter = lang,
            linkedOnly = linkedOnly,
            linkedAudio = linked
        )
        SearchUiState(
            query = query,
            selectedType = type,
            selectedLanguage = lang,
            linkedOnly = linkedOnly,
            results = results
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = SearchUiState(results = allTracks)
    )

    fun selectHomeFilter(filter: HomeFilter) {
        _selectedHomeFilter.value = filter
    }

    fun refreshCatalog() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(450L)
            _isRefreshing.value = false
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onSearchTypeSelected(type: TrackType?) {
        _searchType.update { if (it == type) null else type }
    }

    fun onSearchLanguageSelected(language: String?) {
        _searchLanguage.update { if (it == language) null else language }
    }

    fun onToggleSearchLinkedOnly() {
        _searchLinkedOnly.update { !it }
    }

    fun albumByKey(albumKey: String): Album? = catalogRepository.albumByKey(albumKey)

    fun tracksForAlbum(albumKey: String): List<Track> = catalogRepository.tracksForAlbum(albumKey)
}
