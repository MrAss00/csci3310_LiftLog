package edu.cuhk.csci3310.liftlog.data.repository

import edu.cuhk.csci3310.liftlog.data.local.dao.DayVolume
import edu.cuhk.csci3310.liftlog.data.local.dao.ExerciseBest
import edu.cuhk.csci3310.liftlog.data.local.dao.ExerciseSetCount
import edu.cuhk.csci3310.liftlog.data.local.dao.SessionDao
import edu.cuhk.csci3310.liftlog.data.local.entity.SessionEntity
import edu.cuhk.csci3310.liftlog.data.local.entity.SessionExerciseEntity
import edu.cuhk.csci3310.liftlog.data.local.model.Session
import kotlinx.coroutines.flow.Flow

class SessionRepository(private val dao: SessionDao) {

    fun getSessionsForDay(startOfDay: Long, endOfDay: Long): Flow<List<Session>> =
        dao.getSessionsForDay(startOfDay, endOfDay)

    fun getSessionTimestampsInRange(startOfMonth: Long, endOfMonth: Long): Flow<List<Long>> =
        dao.getSessionTimestampsInRange(startOfMonth, endOfMonth)

    suspend fun insertSession(session: SessionEntity): Long =
        dao.insertSession(session)

    suspend fun insertSessionExercises(exercises: List<SessionExerciseEntity>) =
        dao.insertSessionExercises(exercises)

    suspend fun deleteSession(session: Session) =
        dao.deleteSession(session.session)

    fun getMonthlyVolume(startOfMonth: Long): Flow<Long> =
        dao.getMonthlyVolume(startOfMonth)

    fun getMonthlySessionCount(startOfMonth: Long): Flow<Int> =
        dao.getMonthlySessionCount(startOfMonth)

    fun getMonthlyTotalSets(startOfMonth: Long): Flow<Int> =
        dao.getMonthlyTotalSets(startOfMonth)

    fun getVolumePerDay(start: Long, end: Long): Flow<List<DayVolume>> =
        dao.getVolumePerDay(start, end)

    fun getTopExercisesBySets(): Flow<List<ExerciseSetCount>> =
        dao.getTopExercisesBySets()

    fun getPersonalRecords(): Flow<List<ExerciseBest>> =
        dao.getPersonalRecords()

    fun getAverageSessionDuration(startOfMonth: Long): Flow<Long> =
        dao.getAverageSessionDuration(startOfMonth)
}
