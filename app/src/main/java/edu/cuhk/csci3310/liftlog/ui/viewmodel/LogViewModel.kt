package edu.cuhk.csci3310.liftlog.ui.viewmodel

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import edu.cuhk.csci3310.liftlog.data.local.LiftLogDatabase
import edu.cuhk.csci3310.liftlog.data.local.model.Routine
import edu.cuhk.csci3310.liftlog.data.local.model.Session
import edu.cuhk.csci3310.liftlog.data.repository.RoutineRepository
import edu.cuhk.csci3310.liftlog.data.repository.SessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@RequiresApi(Build.VERSION_CODES.O)
data class LogViewState(
    val selectedDate: LocalDate = LocalDate.now(),
    val currentMonth: YearMonth = YearMonth.now(),
    val dottedDays: Set<Int> = emptySet(),
    val sessions: List<Session> = emptyList(),
    val routines: List<Routine> = emptyList(),
)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalCoroutinesApi::class)
class LogViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionRepository: SessionRepository
    private val routineRepository: RoutineRepository

    private val _state = MutableStateFlow(LogViewState())
    val state: StateFlow<LogViewState> = _state.asStateFlow()

    init {
        val database = LiftLogDatabase.getInstance(application)
        sessionRepository = SessionRepository(database.sessionDao())
        routineRepository = RoutineRepository(database.routineDao())

        // fetch sessions when selected date changes
        viewModelScope.launch {
            _state.map { it.selectedDate }.distinctUntilChanged().flatMapLatest { date ->
                val startOfDay =
                    date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endOfDay =
                    date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                sessionRepository.getSessionsForDay(startOfDay, endOfDay)
            }.collect { sessions ->
                _state.update { it.copy(sessions = sessions) }
            }
        }

        // fetch days with sessions when current month changes
        viewModelScope.launch {
            _state.map { it.currentMonth }.distinctUntilChanged().flatMapLatest { month ->
                val startOfMonth =
                    month.atDay(1).atStartOfDay(ZoneId.systemDefault())
                        .toInstant().toEpochMilli()
                val endOfMonth =
                    month.plusMonths(1).atDay(1).atStartOfDay(ZoneId.systemDefault())
                        .toInstant().toEpochMilli()
                sessionRepository.getSessionTimestampsInRange(startOfMonth, endOfMonth)
            }.collect { timestamps ->
                val days = timestamps.map { timestamp ->
                    Instant.ofEpochMilli(timestamp)
                        .atZone(ZoneId.systemDefault())
                        .dayOfMonth
                }
                _state.update { it.copy(dottedDays = days.toSet()) }
            }
        }

        // fetch all routines for the picker
        viewModelScope.launch {
            routineRepository.getAllRoutines().collect { routines ->
                _state.update { it.copy(routines = routines) }
            }
        }
    }

    fun onDateSelected(date: LocalDate) {
        _state.update {
            it.copy(
                selectedDate = date,
                currentMonth = YearMonth.from(date),
            )
        }
    }

    fun goToPreviousMonth() {
        _state.update { it.copy(currentMonth = it.currentMonth.minusMonths(1)) }
    }

    fun goToNextMonth() {
        _state.update { it.copy(currentMonth = it.currentMonth.plusMonths(1)) }
    }

    fun deleteSession(session: Session) {
        viewModelScope.launch {
            sessionRepository.deleteSession(session)
        }
    }

    fun getRoutine(id: Long): Routine? = _state.value.routines.find { it.routine.id == id }
}
