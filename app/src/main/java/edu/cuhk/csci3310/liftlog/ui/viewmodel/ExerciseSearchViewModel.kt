package edu.cuhk.csci3310.liftlog.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import edu.cuhk.csci3310.liftlog.data.remote.model.Exercise
import edu.cuhk.csci3310.liftlog.data.repository.ExerciseRepository
import edu.cuhk.csci3310.liftlog.data.repository.SettingsRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

data class ExerciseSearchViewState(
    val query: String = "",
    val exercises: List<Exercise> = emptyList(),
    val bodyParts: List<String> = emptyList(),
    val bodyPart: String? = null,
    val isLoading: Boolean = false,
    val hasMore: Boolean = true,
    val message: String? = null,
)

@OptIn(FlowPreview::class)
class ExerciseSearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ExerciseRepository(application)
    private val settingsRepository = SettingsRepository(application)

    private val _state = MutableStateFlow(ExerciseSearchViewState())
    val state: StateFlow<ExerciseSearchViewState> = _state.asStateFlow()

    // reflects the current data-source preference
    private val _useRemote = MutableStateFlow(false)

    private var offset = 0
    private val size = 20

    init {
        // observe the setting; reload everything when it changes
        viewModelScope.launch {
            settingsRepository.useRemoteExercises
                .distinctUntilChanged()
                .collect { useRemote ->
                    _useRemote.value = useRemote
                    loadBodyParts()
                    loadExercises()
                }
        }

        // re-fetch when query / body-part filter changes (debounced)
        viewModelScope.launch {
            state.map { it.query to it.bodyPart }
                .debounce(300.milliseconds)
                .distinctUntilChanged()
                .collect { loadExercises() }
        }
    }

    private fun loadBodyParts() {
        viewModelScope.launch {
            val result = repository.getBodyParts(useRemote = _useRemote.value)
            result.onSuccess { parts ->
                _state.update { it.copy(bodyParts = parts) }
            }
        }
    }

    fun loadMoreExercises() {
        if (_state.value.hasMore) {
            loadExercises(append = true)
        }
    }

    private fun loadExercises(append: Boolean = false) {
        if (_state.value.isLoading) return

        val current = if (append) offset else 0

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, message = null) }

            val result = fetchExercises(offset = current)
            result.onSuccess { exercises ->
                _state.update {
                    it.copy(
                        exercises = if (append) it.exercises + exercises else exercises,
                        isLoading = false,
                        hasMore = exercises.size >= size,
                    )
                }
                offset = if (append) offset + exercises.size else exercises.size
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        message = error.message ?: "failed to load exercises",
                    )
                }
            }
        }
    }

    private suspend fun fetchExercises(offset: Int): Result<List<Exercise>> {
        val state = _state.value
        val useRemote = _useRemote.value
        return if (state.bodyPart != null) {
            repository.listExercisesByBodyPart(
                bodyPart = state.bodyPart,
                offset = offset,
                limit = size,
                useRemote = useRemote,
            )
        } else {
            repository.searchExercises(
                query = state.query,
                offset = offset,
                limit = size,
                useRemote = useRemote,
            )
        }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query, bodyPart = null) }
    }

    fun onBodyPartSelected(bodyPart: String?) {
        _state.update { it.copy(bodyPart = bodyPart) }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }
}
