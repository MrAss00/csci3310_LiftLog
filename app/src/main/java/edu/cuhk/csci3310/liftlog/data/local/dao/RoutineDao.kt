package edu.cuhk.csci3310.liftlog.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import edu.cuhk.csci3310.liftlog.data.local.entity.RoutineEntity
import edu.cuhk.csci3310.liftlog.data.local.entity.RoutineWorkoutEntity
import edu.cuhk.csci3310.liftlog.data.local.model.Routine
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {

    // --- Routines ---

    @Transaction
    @Query("SELECT * FROM routines ORDER BY updatedAt DESC")
    fun getAllRoutines(): Flow<List<Routine>>

    @Transaction
    @Query("SELECT * FROM routines WHERE name LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchRoutines(query: String): Flow<List<Routine>>

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :id")
    fun getRoutineById(id: Long): Flow<Routine?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineEntity): Long

    @Update
    suspend fun updateRoutine(routine: RoutineEntity)

    @Delete
    suspend fun deleteRoutine(routine: RoutineEntity)

    // --- Workout ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkouts(workouts: List<RoutineWorkoutEntity>)

    @Query("DELETE FROM routine_workouts WHERE routineId = :routineId")
    suspend fun deleteWorkoutsForRoutine(routineId: Long)
}
