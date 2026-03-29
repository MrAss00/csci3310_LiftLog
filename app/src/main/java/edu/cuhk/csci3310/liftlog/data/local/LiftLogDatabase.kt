package edu.cuhk.csci3310.liftlog.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import edu.cuhk.csci3310.liftlog.data.local.dao.RoutineDao
import edu.cuhk.csci3310.liftlog.data.local.entity.RoutineEntity
import edu.cuhk.csci3310.liftlog.data.local.entity.RoutineWorkoutEntity

import edu.cuhk.csci3310.liftlog.data.local.dao.WorkoutDao
import edu.cuhk.csci3310.liftlog.data.local.entity.WorkoutEntity

@Database(
    entities = [RoutineEntity::class, RoutineWorkoutEntity::class, WorkoutEntity::class],
    version = 3,
    exportSchema = false
)
abstract class LiftLogDatabase : RoomDatabase() {

    abstract fun routineDao(): RoutineDao
    abstract fun workoutDao(): WorkoutDao

    companion object {
        @Volatile
        private var INSTANCE: LiftLogDatabase? = null

        fun getInstance(context: Context): LiftLogDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LiftLogDatabase::class.java,
                    "liftlog_database"
                )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
