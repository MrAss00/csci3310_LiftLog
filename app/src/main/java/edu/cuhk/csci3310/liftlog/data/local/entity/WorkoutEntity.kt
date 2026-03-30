package edu.cuhk.csci3310.liftlog.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val date: Long,
    val name: String,
    val totalVolume: Long = 0,
    val setsCount: Int = 0,
    val notes: String? = null
)