package edu.cuhk.csci3310.liftlog.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Query("""
        SELECT COALESCE(SUM(totalVolume), 0) 
        FROM workout 
        WHERE date >= :startOfMonth
    """)
    fun getMonthlyVolume(startOfMonth: Long): Flow<Long>

    @Query("""
        SELECT COUNT(*) 
        FROM workout 
        WHERE date >= :startOfMonth
    """)
    fun getMonthlySessionCount(startOfMonth: Long): Flow<Int>

    @Query("""
        SELECT COALESCE(SUM(setsCount), 0) 
        FROM workout 
        WHERE date >= :startOfMonth
    """)
    fun getMonthlyTotalSets(startOfMonth: Long): Flow<Int>
}