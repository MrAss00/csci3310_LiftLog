package edu.cuhk.csci3310.liftlog.widget

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import edu.cuhk.csci3310.liftlog.data.local.LiftLogDatabase
import edu.cuhk.csci3310.liftlog.data.repository.RoutineRepository
import edu.cuhk.csci3310.liftlog.ui.components.RoutinePickerDialog
import edu.cuhk.csci3310.liftlog.ui.theme.LiftLogTheme

class RoutinePickerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = RoutineRepository(
            LiftLogDatabase.getInstance(applicationContext).routineDao(),
        )

        setContent {
            LiftLogTheme {
                val routines by repository.getAllRoutines().collectAsState(initial = emptyList())
                RoutinePickerDialog(
                    routines = routines,
                    onRoutineSelected = { routine ->
                        val uri = "liftlog://spotter/${routine.routine.id}".toUri()
                        startActivity(
                            Intent(Intent.ACTION_VIEW, uri).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                            },
                        )
                        finish()
                    },
                    onDismiss = { finish() },
                )
            }
        }
    }
}
