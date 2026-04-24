package edu.cuhk.csci3310.liftlog.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import edu.cuhk.csci3310.liftlog.data.local.entity.SessionEntity
import edu.cuhk.csci3310.liftlog.data.local.entity.SessionExerciseEntity

data class Session(
    @Embedded val session: SessionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId",
    )
    val exercises: List<SessionExerciseEntity>,
) {
    // delegate commonly used fields so call-sites stay concise
    val id: Long get() = session.id
    val routineName: String get() = session.routineName
    val startTime: Long get() = session.startTime
    val duration: Long get() = session.duration
    val notes: String? get() = session.notes
}
