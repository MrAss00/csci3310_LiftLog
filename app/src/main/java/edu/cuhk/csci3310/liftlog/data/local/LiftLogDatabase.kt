package edu.cuhk.csci3310.liftlog.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import edu.cuhk.csci3310.liftlog.data.local.dao.RoutineDao
import edu.cuhk.csci3310.liftlog.data.local.dao.SessionDao
import edu.cuhk.csci3310.liftlog.data.local.entity.RoutineEntity
import edu.cuhk.csci3310.liftlog.data.local.entity.RoutineWorkoutEntity
import edu.cuhk.csci3310.liftlog.data.local.entity.SessionEntity
import edu.cuhk.csci3310.liftlog.data.local.entity.SessionExerciseEntity

@Database(
    version = 9,
    exportSchema = false,
    entities = [
        RoutineEntity::class,
        RoutineWorkoutEntity::class,
        SessionEntity::class,
        SessionExerciseEntity::class,
    ],
)
abstract class LiftLogDatabase : RoomDatabase() {

    abstract fun routineDao(): RoutineDao
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var INSTANCE: LiftLogDatabase? = null

        fun getInstance(context: Context): LiftLogDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LiftLogDatabase::class.java,
                    name = "liftlog",
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
