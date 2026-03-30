package edu.cuhk.csci3310.liftlog.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import edu.cuhk.csci3310.liftlog.data.local.entity.RoutineEntity
import edu.cuhk.csci3310.liftlog.data.local.entity.RoutineWorkoutEntity

data class Routine(
    @Embedded val routine: RoutineEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "routineId",
    )
    val workouts: List<RoutineWorkout>,
)

typealias RoutineWorkout = RoutineWorkoutEntity
