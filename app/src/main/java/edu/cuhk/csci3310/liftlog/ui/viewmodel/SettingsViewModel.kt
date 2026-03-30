package edu.cuhk.csci3310.liftlog.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("liftlog_prefs", Context.MODE_PRIVATE)

    private val _monthlyGoal = MutableStateFlow(prefs.getLong("monthly_goal_kg", 100_000L))
    val monthlyGoal: StateFlow<Long> = _monthlyGoal.asStateFlow()

    fun updateMonthlyGoal(newGoal: Long) {
        viewModelScope.launch {
            _monthlyGoal.value = newGoal
            prefs.edit { putLong("monthly_goal_kg", newGoal) }
        }
    }
}
