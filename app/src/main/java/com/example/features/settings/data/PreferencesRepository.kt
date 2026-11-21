package com.example.features.settings.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.core.local.studioLibraryDataStore
import com.example.core.model.UserPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PreferencesRepository(context: Context) {
    private val dataStore = context.applicationContext.studioLibraryDataStore
    private val json = Json { ignoreUnknownKeys = true }
    private val prefsKey = stringPreferencesKey("studio_user_preferences_json")

    fun observePreferences(): Flow<UserPreferences> =
        dataStore.data.map { prefs ->
            val raw = prefs[prefsKey]
            if (raw.isNullOrBlank()) {
                UserPreferences()
            } else {
                runCatching { json.decodeFromString<UserPreferences>(raw) }.getOrDefault(UserPreferences())
            }
        }

    suspend fun savePreferences(prefsValue: UserPreferences): Result<Unit> {
        return try {
            dataStore.edit { prefs ->
                prefs[prefsKey] = json.encodeToString(prefsValue)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
