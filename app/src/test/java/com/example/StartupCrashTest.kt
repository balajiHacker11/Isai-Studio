package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.core.di.AppContainer
import com.example.core.model.UserPreferences
import com.example.features.auth.data.AuthRepository
import com.example.features.catalog.data.CatalogRepository
import com.example.features.library.data.LibraryRepository
import com.example.features.player.data.LyricsRepository
import com.example.features.settings.data.PreferencesRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    fun mainActivityLaunchesAndNavigatesIntroToAuthWithoutCrash() {
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
                composeRule.onNodeWithTag("google_sign_in_button").fetchSemanticsNode()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
    }

    @Test
    fun repositoriesDegradeSafelyWhenFirebaseIsUnavailable() = runTest {
        val authRepo = AuthRepository(db = null, auth = null)
        assertFalse(authRepo.isFirebaseConfigured)
        assertNull(authRepo.currentUser())
        assertNull(authRepo.authStateFlow().first())

        val libRepo = LibraryRepository(db = null, auth = null)
        assertTrue(libRepo.observeLikedTracks("u1").first().isEmpty())
        assertTrue(libRepo.observePlaylists("u1").first().isEmpty())
        assertTrue(libRepo.observePlayHistory("u1").first().isEmpty())

        val lyricsRepo = LyricsRepository(db = null, auth = null)
        assertNull(lyricsRepo.observeTrackLyrics("u1", 1).first())

        val prefsRepo = PreferencesRepository(db = null, auth = null)
        assertEquals(UserPreferences(userId = "u1"), prefsRepo.observePreferences("u1").first())
    }

    @Test
    fun appContainerInitializesFirebaseRepositoriesFromGoogleServicesJson() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        FirebaseApp.getApps(context).forEach { it.delete() }

        val webClientResId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        assertTrue("Expected default_web_client_id generated from app/google-services.json", webClientResId != 0)
        assertTrue(context.getString(webClientResId).isNotBlank())

        val container = AppContainer(context)
        assertTrue("Expected FirebaseApp to initialize from google-services.json resources", container.isFirebaseConfigured)
        assertNotNull(container.firestore)
        assertNotNull(container.firebaseAuth)
        assertTrue(container.authRepository.isFirebaseConfigured)
    }
}
