package edu.cuhk.csci3310.liftlog.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import edu.cuhk.csci3310.liftlog.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    val speechRecognitionEnabled: StateFlow<Boolean> = repository.speechRecognitionEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true,
        )

    fun setSpeechRecognitionEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setSpeechRecognitionEnabled(enabled) }
    }

    val useRemoteExercises: StateFlow<Boolean> = repository.useRemoteExercises
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    fun setUseRemoteExercises(enabled: Boolean) {
        viewModelScope.launch { repository.setUseRemoteExercises(enabled) }
    }
}
