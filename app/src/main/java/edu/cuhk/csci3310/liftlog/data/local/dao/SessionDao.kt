package edu.cuhk.csci3310.liftlog.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import edu.cuhk.csci3310.liftlog.data.local.entity.SessionEntity
import edu.cuhk.csci3310.liftlog.data.local.entity.SessionExerciseEntity
import edu.cuhk.csci3310.liftlog.data.local.model.Session
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Transaction
    @Query("SELECT * FROM sessions WHERE startTime >= :startOfDay AND startTime < :endOfDay ORDER BY startTime DESC")
    fun getSessionsForDay(startOfDay: Long, endOfDay: Long): Flow<List<Session>>

    @Query("SELECT DISTINCT startTime FROM sessions WHERE startTime >= :startOfMonth AND startTime < :endOfMonth")
    fun getSessionTimestampsInRange(startOfMonth: Long, endOfMonth: Long): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionExercises(exercises: List<SessionExerciseEntity>)

    @Delete
    suspend fun deleteSession(session: SessionEntity)

    @Query("""SELECT COALESCE(SUM(totalVolume), 0) FROM sessions WHERE startTime >= :startOfMonth """)
    fun getMonthlyVolume(startOfMonth: Long): Flow<Long>

    @Query(""" SELECT COUNT(*) FROM sessions WHERE startTime >= :startOfMonth """)
    fun getMonthlySessionCount(startOfMonth: Long): Flow<Int>

    @Query(""" SELECT COALESCE(SUM(setsCount), 0) FROM sessions WHERE startTime >= :startOfMonth """)
    fun getMonthlyTotalSets(startOfMonth: Long): Flow<Int>

    @Query(""" SELECT COALESCE(SUM(totalVolume), 0) FROM sessions WHERE startTime >= :startOfDay AND startTime < :endOfDay """)
    fun getTodayVolume(startOfDay: Long, endOfDay: Long): Flow<Long>

    @Query(
        """
        SELECT (startTime / 86400000) * 86400000 AS day,
               COALESCE(SUM(totalVolume), 0) AS totalVolume
        FROM sessions
        WHERE startTime >= :start AND startTime < :end
        GROUP BY day
        ORDER BY day ASC
    """,
    )
    fun getVolumePerDay(start: Long, end: Long): Flow<List<DayVolume>>

    @Query(
        """
        SELECT exerciseName, SUM(sets) AS totalSets
        FROM session_exercises
        GROUP BY exerciseName
        ORDER BY totalSets DESC
        LIMIT 5
    """,
    )
    fun getTopExercisesBySets(): Flow<List<ExerciseSetCount>>

    @Query(
        """
        SELECT exerciseName, MAX(weight) AS maxWeight
        FROM session_exercises
        GROUP BY exerciseName
        ORDER BY maxWeight DESC
        LIMIT 5
    """,
    )
    fun getPersonalRecords(): Flow<List<ExerciseBest>>

    @Query(
        """
        SELECT COALESCE(AVG(endTime - startTime), 0)
        FROM sessions
        WHERE startTime >= :startOfMonth
    """,
    )
    fun getAverageSessionDuration(startOfMonth: Long): Flow<Long>
}

data class DayVolume(val day: Long, val totalVolume: Long)
data class ExerciseSetCount(val exerciseName: String, val totalSets: Int)
data class ExerciseBest(val exerciseName: String, val maxWeight: Double)
