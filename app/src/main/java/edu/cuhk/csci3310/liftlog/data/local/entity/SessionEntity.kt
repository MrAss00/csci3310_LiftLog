package edu.cuhk.csci3310.liftlog.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["routineId"]),
        Index(value = ["startTime"]),
    ],
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val routineId: Long?,  // nullable in case routine is deleted
    val routineName: String,  // snapshot of routine name at time of session
    val startTime: Long,
    val endTime: Long,
    val notes: String? = null,
    // for stat screen
    val totalVolume: Long = 0,
    val totalSets: Int = 0,
) {
    val duration: Long // in milliseconds
        get() = endTime - startTime
}
