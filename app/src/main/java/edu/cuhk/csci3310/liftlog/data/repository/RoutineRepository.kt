package edu.cuhk.csci3310.liftlog.data.repository

import edu.cuhk.csci3310.liftlog.data.local.dao.RoutineDao
import edu.cuhk.csci3310.liftlog.data.local.entity.RoutineEntity
import edu.cuhk.csci3310.liftlog.data.local.entity.RoutineWorkoutEntity
import edu.cuhk.csci3310.liftlog.data.local.model.Routine
import kotlinx.coroutines.flow.Flow

class RoutineRepository(private val dao: RoutineDao) {

    fun getAllRoutines(): Flow<List<Routine>> =
        dao.getAllRoutines()

    fun searchRoutines(query: String): Flow<List<Routine>> =
        dao.searchRoutines(query)

    fun getRoutineById(id: Long): Flow<Routine?> =
        dao.getRoutineById(id)

    suspend fun insertRoutine(routine: RoutineEntity): Long =
        dao.insertRoutine(routine)

    suspend fun updateRoutine(routine: RoutineEntity) =
        dao.updateRoutine(routine)

    suspend fun deleteRoutine(routine: RoutineEntity) =
        dao.deleteRoutine(routine)

    suspend fun saveWorkoutsForRoutine(routineId: Long, workouts: List<RoutineWorkoutEntity>) {
        dao.deleteWorkoutsForRoutine(routineId)
        dao.insertWorkouts(
            workouts.mapIndexed { index, workout ->
                workout.copy(routineId = routineId, index = index)
            },
        )
    }
}
