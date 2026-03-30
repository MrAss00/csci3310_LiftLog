package edu.cuhk.csci3310.liftlog.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "routine_workouts",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["routineId"])],
)
data class RoutineWorkoutEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val routineId: Long,
    val exerciseId: String,
    val exerciseName: String,
    val exerciseGifUrl: String,
    val sets: Int,
    val reps: Int,
    val weight: Double,
    val interval: Int,
    val index: Int,
)
