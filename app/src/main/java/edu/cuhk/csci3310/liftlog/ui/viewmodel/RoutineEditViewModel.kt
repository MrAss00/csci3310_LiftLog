package edu.cuhk.csci3310.liftlog.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import edu.cuhk.csci3310.liftlog.data.local.LiftLogDatabase
import edu.cuhk.csci3310.liftlog.data.local.entity.RoutineEntity
import edu.cuhk.csci3310.liftlog.data.local.model.RoutineWorkout
import edu.cuhk.csci3310.liftlog.data.repository.RoutineRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RoutineEditViewState(
    val name: String = "",
    val workouts: List<RoutineWorkout> = emptyList(),
    val isSaved: Boolean = false,
    val message: String? = null,
)

class RoutineEditViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val repository: RoutineRepository

    private val id: Long = savedStateHandle.get<Long>("id") ?: -1L
    val isEditing: Boolean = id > 0

    private val _state = MutableStateFlow(RoutineEditViewState())
    val state: StateFlow<RoutineEditViewState> = _state.asStateFlow()

    init {
        val database = LiftLogDatabase.getInstance(application)
        repository = RoutineRepository(database.routineDao())

        if (isEditing) load()
    }

    private fun load() {
        viewModelScope.launch {
            val routine = repository.getRoutineById(id).firstOrNull()
            if (routine != null) {
                _state.update {
                    it.copy(
                        name = routine.routine.name,
                        workouts = routine.workouts.sortedBy { w -> w.index },
                    )
                }
            } else {
                _state.update { it.copy(message = "routine not found") }
            }
        }
    }

    fun setName(name: String) {
        _state.update { it.copy(name = name) }
    }

    fun addWorkout(workout: RoutineWorkout) {
        _state.update { it.copy(workouts = it.workouts + workout) }
    }

    fun removeWorkout(index: Int) {
        _state.update { it.copy(workouts = it.workouts.toMutableList().apply { removeAt(index) }) }
    }

    fun updateWorkout(index: Int, workout: RoutineWorkout) {
        _state.update {
            val updated = it.workouts.toMutableList()
            updated[index] = workout
            it.copy(workouts = updated)
        }
    }

    fun moveWorkout(fromIndex: Int, toIndex: Int) {
        _state.update {
            val list = it.workouts.toMutableList()
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            it.copy(workouts = list)
        }
    }

    fun save() {
        val state = _state.value
        if (state.name.isBlank()) {
            _state.update { it.copy(message = "routine name cannot be empty") }
            return
        }
        if (state.workouts.isEmpty()) {
            _state.update { it.copy(message = "at least one workout required") }
            return
        }
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val savedRoutineId = if (isEditing) {
                    repository.updateRoutine(
                        RoutineEntity(
                            id = id,
                            name = state.name,
                            updatedAt = now,
                        ),
                    )
                    id
                } else {
                    repository.insertRoutine(
                        RoutineEntity(
                            name = state.name,
                            createdAt = now,
                            updatedAt = now,
                        ),
                    )
                }
                repository.saveWorkoutsForRoutine(
                    savedRoutineId,
                    state.workouts.mapIndexed { index, w ->
                        w.copy(routineId = savedRoutineId, index = index)
                    },
                )
                _state.update { it.copy(isSaved = true) }
            } catch (e: Exception) {
                _state.update { it.copy(message = e.message ?: "failed to save") }
            }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }
}
