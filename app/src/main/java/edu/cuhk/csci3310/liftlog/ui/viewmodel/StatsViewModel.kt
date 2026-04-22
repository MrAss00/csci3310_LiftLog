package edu.cuhk.csci3310.liftlog.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import edu.cuhk.csci3310.liftlog.data.local.LiftLogDatabase
import edu.cuhk.csci3310.liftlog.data.local.dao.ExerciseBest
import edu.cuhk.csci3310.liftlog.data.local.dao.ExerciseSetCount
import edu.cuhk.csci3310.liftlog.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class HeatmapDay(val day: Long, val volume: Long)

data class StatsViewState(
    val monthlySessions: Int = 0,
    val monthlyVolume: Long = 0L,
    val monthlyTotalSets: Int = 0,
    val averageSessionDuration: Long = 0L,
    val heatmapData: List<HeatmapDay> = emptyList(),
    val topExercises: List<ExerciseSetCount> = emptyList(),
    val personalRecords: List<ExerciseBest> = emptyList(),
)

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = LiftLogDatabase.getInstance(application)
    private val repository = SessionRepository(database.sessionDao())

    private val _state = MutableStateFlow(StatsViewState())
    val state: StateFlow<StatsViewState> = _state.asStateFlow()

    init {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()

        val startOfMonth = today.withDayOfMonth(1)
            .atStartOfDay(zone).toInstant().toEpochMilli()

        val heatmapStart = startOfMonth
        val heatmapEnd = today.withDayOfMonth(1).plusMonths(1)
            .atStartOfDay(zone).toInstant().toEpochMilli()

        viewModelScope.launch {
            combine(
                repository.getMonthlyVolume(startOfMonth),
                repository.getMonthlySessionCount(startOfMonth),
                repository.getMonthlyTotalSets(startOfMonth),
            ) { volume, sessions, sets ->
                Triple(volume, sessions, sets)
            }.collect { (volume, sessions, sets) ->
                _state.update {
                    it.copy(
                        monthlyVolume = volume,
                        monthlySessions = sessions,
                        monthlyTotalSets = sets,
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.getAverageSessionDuration(startOfMonth).collect { ms ->
                _state.update { it.copy(averageSessionDuration = ms) }
            }
        }

        viewModelScope.launch {
            repository.getVolumePerDay(heatmapStart, heatmapEnd).collect { rows ->
                val map = rows.associate { it.day to it.totalVolume }
                val daysInMonth = today.lengthOfMonth()
                val days = (1..daysInMonth).map { dayNum ->
                    val date = today.withDayOfMonth(dayNum)
                    val epochMs = date.atStartOfDay(zone).toInstant().toEpochMilli()
                    HeatmapDay(epochMs, map[date.toString()] ?: 0L)
                }
                _state.update { it.copy(heatmapData = days) }
            }
        }

        viewModelScope.launch {
            repository.getTopExercisesBySets().collect { list ->
                _state.update { it.copy(topExercises = list) }
            }
        }

        viewModelScope.launch {
            repository.getPersonalRecords().collect { list ->
                _state.update { it.copy(personalRecords = list) }
            }
        }
    }
}
