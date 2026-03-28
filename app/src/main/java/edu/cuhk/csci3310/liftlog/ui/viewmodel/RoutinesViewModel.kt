package edu.cuhk.csci3310.liftlog.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import edu.cuhk.csci3310.liftlog.data.local.LiftLogDatabase
import edu.cuhk.csci3310.liftlog.data.local.model.RoutineWithWorkouts
import edu.cuhk.csci3310.liftlog.data.repository.RoutineRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class RoutinesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RoutineRepository

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val routines: StateFlow<List<RoutineWithWorkouts>>

    init {
        val database = LiftLogDatabase.getInstance(application)
        repository = RoutineRepository(database.routineDao())

        routines = _query
            .flatMapLatest { query ->
                if (query.isBlank()) {
                    repository.getAllRoutines()
                } else {
                    repository.searchRoutines(query)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun onQueryChange(query: String) {
        _query.value = query
    }

    fun deleteRoutine(routineWithWorkouts: RoutineWithWorkouts) {
        viewModelScope.launch { repository.deleteRoutine(routineWithWorkouts.routine) }
    }
}
