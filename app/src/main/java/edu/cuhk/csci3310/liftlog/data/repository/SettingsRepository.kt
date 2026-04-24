package edu.cuhk.csci3310.liftlog.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        private val KEY_SPEECH_RECOGNITION_ENABLED =
            booleanPreferencesKey("speech_recognition_enabled")
        private val KEY_USE_REMOTE_EXERCISES =
            booleanPreferencesKey("use_remote_exercises")
    }

    val speechRecognitionEnabled: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[KEY_SPEECH_RECOGNITION_ENABLED] ?: true }

    suspend fun setSpeechRecognitionEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SPEECH_RECOGNITION_ENABLED] = enabled
        }
    }

    val useRemoteExercises: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[KEY_USE_REMOTE_EXERCISES] ?: false }

    suspend fun setUseRemoteExercises(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USE_REMOTE_EXERCISES] = enabled
        }
    }
}
