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

    @Transaction
    @Query("SELECT * FROM sessions WHERE id = :id")
    fun getSessionById(id: Long): Flow<Session?>

    @Transaction
    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<Session>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSession(session: SessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSessionExercises(exercises: List<SessionExerciseEntity>)

    @Delete
    fun deleteSession(session: SessionEntity)

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    fun deleteSessionById(sessionId: Long)

    // for stat screen analysis
    @Query("""SELECT COALESCE(SUM(totalVolume), 0) FROM sessions WHERE startTime >= :startOfMonth """)
    fun getMonthlyVolume(startOfMonth: Long): Flow<Long>

    @Query(""" SELECT COUNT(*) FROM sessions WHERE startTime >= :startOfMonth """)
    fun getMonthlySessionCount(startOfMonth: Long): Flow<Int>

    @Query(""" SELECT COALESCE(SUM(setsCount), 0) FROM sessions WHERE startTime >= :startOfMonth """)
    fun getMonthlyTotalSets(startOfMonth: Long): Flow<Int>

    @Query(""" SELECT COALESCE(SUM(totalVolume), 0) FROM sessions WHERE startTime >= :startOfDay AND startTime < :endOfDay """)
    fun getTodayVolume(startOfDay: Long, endOfDay: Long): Flow<Long>
}
