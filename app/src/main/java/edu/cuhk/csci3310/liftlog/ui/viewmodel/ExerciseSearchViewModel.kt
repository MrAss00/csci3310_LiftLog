package edu.cuhk.csci3310.liftlog.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.cuhk.csci3310.liftlog.data.remote.RetrofitInstance
import edu.cuhk.csci3310.liftlog.data.remote.model.Exercise
import edu.cuhk.csci3310.liftlog.data.repository.ExerciseRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExerciseSearchState(
    val query: String = "",
    val exercises: List<Exercise> = emptyList(),
    val bodyParts: List<String> = emptyList(),
    val bodyPart: String? = null,
    val loading: Boolean = false,
    val more: Boolean = true,
    val message: String? = null
)

class ExerciseSearchViewModel : ViewModel() {

    private val repository = ExerciseRepository(RetrofitInstance.api)

    private val _state = MutableStateFlow(ExerciseSearchState())
    val state: StateFlow<ExerciseSearchState> = _state.asStateFlow()

    private var searchJob: Job? = null
    private var offset = 0
    private val size = 20

    init {
        loadBodyParts()
        searchExercises(debounce = false)
    }

    private fun loadBodyParts() {
        viewModelScope.launch {
            val result = repository.getBodyParts()
            result.onSuccess { parts ->
                _state.update { it.copy(bodyParts = parts) }
            }
        }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        searchExercises(debounce = true)
    }

    fun onBodyPartSelected(bodyPart: String?) {
        _state.update { it.copy(bodyPart = bodyPart) }
        searchExercises(debounce = false)
    }

    private fun searchExercises(debounce: Boolean) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (debounce) delay(400)

            offset = 0
            _state.update { it.copy(loading = true, message = null) }

            val result = fetchExercises(offset = 0)
            result.onSuccess { exercises ->
                _state.update {
                    it.copy(
                        exercises = exercises,
                        loading = false,
                        more = exercises.size >= size
                    )
                }
                offset = exercises.size
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        loading = false,
                        message = error.message ?: "failed to load exercises"
                    )
                }
            }
        }
    }

    fun loadMore() {
        val state = _state.value
        if (state.loading || !state.more) return

        viewModelScope.launch {
            _state.update { it.copy(loading = true) }

            val result = fetchExercises(offset = offset)
            result.onSuccess { exercises ->
                _state.update {
                    it.copy(
                        exercises = it.exercises + exercises,
                        loading = false,
                        more = exercises.size >= size
                    )
                }
                offset += exercises.size
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        loading = false,
                        message = error.message ?: "failed to load more"
                    )
                }
            }
        }
    }

    private suspend fun fetchExercises(offset: Int): Result<List<Exercise>> {
        val state = _state.value
        return if (state.bodyPart != null) {
            repository.getExercisesByBodyPart(
                bodyPart = state.bodyPart,
                offset = offset,
                limit = size
            )
        } else {
            repository.searchExercises(
                query = state.query,
                offset = offset,
                limit = size
            )
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }
}
