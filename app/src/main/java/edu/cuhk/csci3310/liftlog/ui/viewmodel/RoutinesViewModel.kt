package edu.cuhk.csci3310.liftlog.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import edu.cuhk.csci3310.liftlog.data.local.LiftLogDatabase
import edu.cuhk.csci3310.liftlog.data.local.model.Routine
import edu.cuhk.csci3310.liftlog.data.repository.RoutineRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RoutinesViewState(
    val query: String = "",
    val routines: List<Routine> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class RoutinesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RoutineRepository

    private val _state = MutableStateFlow(RoutinesViewState())
    val state: StateFlow<RoutinesViewState> = _state.asStateFlow()

    init {
        val database = LiftLogDatabase.getInstance(application)
        repository = RoutineRepository(database.routineDao())

        viewModelScope.launch {
            state.map { it.query }.distinctUntilChanged().collect { loadRoutines() }
        }
    }

    private fun loadRoutines() {
        val state = _state.value
        viewModelScope.launch {
            if (state.query.isBlank()) {
                repository.getAllRoutines()
            } else {
                repository.searchRoutines(state.query)
            }
                .collect { routines ->
                    _state.update { it.copy(routines = routines) }
                }
        }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
    }

    fun deleteRoutine(routine: Routine) {
        viewModelScope.launch { repository.deleteRoutine(routine.routine) }
    }
}
