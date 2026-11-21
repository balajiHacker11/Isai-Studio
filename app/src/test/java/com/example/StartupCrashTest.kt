package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.core.di.AppContainer
import com.example.core.model.AccentTheme
import com.example.core.model.UserPreferences
import com.example.features.catalog.data.CatalogRepository
import com.example.features.library.data.LibraryRepository
import com.example.features.player.data.LyricsRepository
import com.example.features.settings.data.PreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StartupCrashTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun catalogJsonLoadsCleanly() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = CatalogRepository(context)
        assertEquals(12, repo.albums.size)
        assertEquals(110, repo.tracks.size)
    }

    @Test
    fun mainActivityLaunchesIntroAndEntersStudioHomeWithoutLogin() {
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag("intro_skip_button").fetchSemanticsNode()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag("intro_skip_button").assertIsDisplayed().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag("nav_home").fetchSemanticsNode()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag("nav_home").assertIsDisplayed()
    }

    @Test
    fun localRepositoriesPersistLibraryLyricsAndPreferences() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AppContainer(context)
        val sampleTrack = container.catalogRepository.tracks.first()

        val libRepo: LibraryRepository = container.libraryRepository
        assertTrue(libRepo.toggleLike(sampleTrack, currentlyLiked = false).isSuccess)
        assertTrue(libRepo.observeLikedTracks().first().any { it.trackNumber == sampleTrack.id.toLong() })

        val playlistResult = libRepo.createPlaylist(
            name = "Drive Mix",
            description = "Late night bangers",
            initialTrackIds = listOf(sampleTrack.id)
        )
        assertTrue(playlistResult.isSuccess)
        val playlistId = playlistResult.getOrThrow()
        assertTrue(libRepo.observePlaylists().first().any { it.id == playlistId && it.name == "Drive Mix" })
        assertTrue(libRepo.deletePlaylist(playlistId).isSuccess)

        assertTrue(libRepo.recordPlayHistory(sampleTrack).isSuccess)
        assertTrue(libRepo.observePlayHistory().first().any { it.trackNumber == sampleTrack.id.toLong() })

        val lyricsRepo: LyricsRepository = container.lyricsRepository
        assertTrue(lyricsRepo.saveTrackLyrics(sampleTrack.id, "[00:01.00]Katchi Sera").isSuccess)
        val savedLyrics = lyricsRepo.observeTrackLyrics(sampleTrack.id).first()
        assertNotNull(savedLyrics)
        assertEquals("[00:01.00]Katchi Sera", savedLyrics?.lrcContent)

        val prefsRepo: PreferencesRepository = container.preferencesRepository
        val customPrefs = UserPreferences(
            darkThemeEnabled = false,
            accentKey = AccentTheme.CYAN.key,
            crossfadeSeconds = 6L
        )
        assertTrue(prefsRepo.savePreferences(customPrefs).isSuccess)
        assertEquals(customPrefs, prefsRepo.observePreferences().first())
    }
}
