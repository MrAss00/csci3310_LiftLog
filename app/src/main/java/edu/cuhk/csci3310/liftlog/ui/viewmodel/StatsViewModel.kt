package edu.cuhk.csci3310.liftlog.ui.viewmodel

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import edu.cuhk.csci3310.liftlog.data.local.LiftLogDatabase
import edu.cuhk.csci3310.liftlog.data.repository.WorkoutRepository
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
    private val repository = WorkoutRepository(database.workoutDao())

    private val _monthlyVolume = MutableStateFlow(0L)
    val monthlyVolume: StateFlow<Long> = _monthlyVolume.asStateFlow()

    private val _monthlySessions = MutableStateFlow(0)
    val monthlySessions: StateFlow<Int> = _monthlySessions.asStateFlow()

    private val _monthlyTotalSets = MutableStateFlow(0)
    val monthlyTotalSets: StateFlow<Int> = _monthlyTotalSets.asStateFlow()

    private val _monthlyProgress = MutableStateFlow(0f)   // 0.0 - 1.0
    val monthlyProgress: StateFlow<Float> = _monthlyProgress.asStateFlow()

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

                // Monthly goal = 100,000 kg (you can change this later)
                _monthlyProgress.value = (volume.toFloat() / 100_000f).coerceAtMost(1f)
            }.collect { }
        }
    }
}