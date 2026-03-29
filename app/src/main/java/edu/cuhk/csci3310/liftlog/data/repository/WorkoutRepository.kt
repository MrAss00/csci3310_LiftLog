package edu.cuhk.csci3310.liftlog.data.repository

import edu.cuhk.csci3310.liftlog.data.local.dao.WorkoutDao
import kotlinx.coroutines.flow.Flow

class WorkoutRepository(private val workoutDao: WorkoutDao) {

    fun getMonthlyVolume(startOfMonth: Long): Flow<Long> {
        return workoutDao.getMonthlyVolume(startOfMonth)
    }

    fun getMonthlySessionCount(startOfMonth: Long): Flow<Int> {
        return workoutDao.getMonthlySessionCount(startOfMonth)
    }

    fun getMonthlyTotalSets(startOfMonth: Long): Flow<Int> {
        return workoutDao.getMonthlyTotalSets(startOfMonth)
    }
}