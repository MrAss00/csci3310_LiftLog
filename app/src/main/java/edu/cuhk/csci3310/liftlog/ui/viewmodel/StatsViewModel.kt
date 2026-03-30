package edu.cuhk.csci3310.liftlog.ui.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import edu.cuhk.csci3310.liftlog.data.local.LiftLogDatabase
import edu.cuhk.csci3310.liftlog.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

@RequiresApi(Build.VERSION_CODES.O)
class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = LiftLogDatabase.getInstance(application)
    private val repository = SessionRepository(database.sessionDao())

    private val prefs = application.getSharedPreferences("liftlog_prefs", Context.MODE_PRIVATE)

    private val _monthlyGoal = MutableStateFlow(prefs.getLong("monthly_goal_kg", 100_000L))
    val monthlyGoal: StateFlow<Long> = _monthlyGoal.asStateFlow()

    private val _monthlyVolume = MutableStateFlow(0L)
    val monthlyVolume: StateFlow<Long> = _monthlyVolume.asStateFlow()

    private val _monthlySessions = MutableStateFlow(0)
    val monthlySessions: StateFlow<Int> = _monthlySessions.asStateFlow()

    private val _monthlyTotalSets = MutableStateFlow(0)
    val monthlyTotalSets: StateFlow<Int> = _monthlyTotalSets.asStateFlow()

    private val _monthlyProgress = MutableStateFlow(0f)
    val monthlyProgress: StateFlow<Float> = _monthlyProgress.asStateFlow()

    // Refresh goal when returning from Settings
    fun refreshMonthlyGoal() {
        _monthlyGoal.value = prefs.getLong("monthly_goal_kg", 100_000L)
    }

    init {
        viewModelScope.launch {
            val startOfMonth = LocalDate.now()
                .withDayOfMonth(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            combine(
                repository.getMonthlyVolume(startOfMonth),
                repository.getMonthlySessionCount(startOfMonth),
                repository.getMonthlyTotalSets(startOfMonth)
            ) { volume, sessions, sets ->
                _monthlyVolume.value = volume
                _monthlySessions.value = sessions
                _monthlyTotalSets.value = sets

                val goal = _monthlyGoal.value
                _monthlyProgress.value = if (goal > 0) {
                    (volume.toFloat() / goal.toFloat()).coerceAtMost(1f)
                } else 0f
            }.collect { }
        }
    }
}