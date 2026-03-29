package edu.cuhk.csci3310.liftlog.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val date: Long,                    // timestamp in milliseconds
    val name: String,                  // e.g. "Push Day"
    val totalVolume: Long = 0,         // total kg lifted in the session
    val setsCount: Int = 0,            // total number of sets
    val notes: String? = null
)