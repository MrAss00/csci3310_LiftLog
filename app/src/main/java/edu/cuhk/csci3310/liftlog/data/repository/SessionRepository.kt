package edu.cuhk.csci3310.liftlog.data.repository

import edu.cuhk.csci3310.liftlog.data.local.dao.SessionDao
import edu.cuhk.csci3310.liftlog.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

class SessionRepository(private val dao: SessionDao) {

    fun getSessionsForDay(startOfDay: Long, endOfDay: Long): Flow<List<SessionEntity>> =
        dao.getSessionsForDay(startOfDay, endOfDay)

    fun getSessionTimestampsInRange(startOfMonth: Long, endOfMonth: Long): Flow<List<Long>> =
        dao.getSessionTimestampsInRange(startOfMonth, endOfMonth)

    fun getSessionById(id: Long): Flow<SessionEntity?> =
        dao.getSessionById(id)

    fun getAllSessions(): Flow<List<SessionEntity>> =
        dao.getAllSessions()

    suspend fun insertSession(session: SessionEntity): Long =
        dao.insertSession(session)

    suspend fun deleteSession(session: SessionEntity) =
        dao.deleteSession(session)

    suspend fun deleteSessionById(sessionId: Long) =
        dao.deleteSessionById(sessionId)

    // for stat screen
    fun getMonthlyVolume(startOfMonth: Long): Flow<Long> =
        dao.getMonthlyVolume(startOfMonth)

    fun getMonthlySessionCount(startOfMonth: Long): Flow<Int> =
        dao.getMonthlySessionCount(startOfMonth)

    fun getMonthlyTotalSets(startOfMonth: Long): Flow<Int> =
        dao.getMonthlyTotalSets(startOfMonth)

    fun getTodayVolume(startOfDay: Long, endOfDay: Long): Flow<Long> =
        dao.getTodayVolume(startOfDay, endOfDay)
}
