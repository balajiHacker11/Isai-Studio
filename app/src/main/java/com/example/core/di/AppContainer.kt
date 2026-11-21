package com.example.core.di

import android.content.Context
import com.example.core.domain.AutoMatchAudioFilesUseCase
import com.example.core.domain.ParseLrcUseCase
import com.example.core.domain.SearchCatalogUseCase
import com.example.core.local.LocalAudioStore
import com.example.core.player.PlaybackCoordinator
import com.example.features.catalog.data.CatalogRepository
import com.example.features.library.data.LibraryRepository
import com.example.features.player.data.LyricsRepository
import com.example.features.settings.data.PreferencesRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val catalogRepository: CatalogRepository by lazy {
        CatalogRepository(appContext)
    }

    val localAudioStore: LocalAudioStore by lazy {
        LocalAudioStore(appContext)
    }

    val playbackCoordinator: PlaybackCoordinator by lazy {
        PlaybackCoordinator(appContext, catalogRepository, localAudioStore)
    }

    val libraryRepository: LibraryRepository by lazy {
        LibraryRepository(appContext)
    }

    val lyricsRepository: LyricsRepository by lazy {
        LyricsRepository(appContext)
    }

    val preferencesRepository: PreferencesRepository by lazy {
        PreferencesRepository(appContext)
    }

    val searchCatalogUseCase: SearchCatalogUseCase by lazy { SearchCatalogUseCase() }
    val autoMatchAudioFilesUseCase: AutoMatchAudioFilesUseCase by lazy { AutoMatchAudioFilesUseCase() }
    val parseLrcUseCase: ParseLrcUseCase by lazy { ParseLrcUseCase() }
}
