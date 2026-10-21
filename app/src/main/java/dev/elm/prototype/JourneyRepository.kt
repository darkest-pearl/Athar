package dev.elm.prototype

import androidx.room.withTransaction
import java.time.Clock

enum class LessonStage { Intro, Listen, Takeaway, Practice, Complete }

fun LessonJourney.stageValue(): LessonStage =
    LessonStage.entries.firstOrNull { it.name == stage } ?: LessonStage.Intro

class JourneyRepository(private val db: LearningDatabase, private val dao: LearningDao,
    private val clock: Clock) {
    suspend fun begin(courseId: String, lesson: PackLesson, repeat: Boolean = false) =
        db.withTransaction {
            val old = dao.lessonJourney(courseId, lesson.id, lesson.version)
            if (old == null || repeat)
                dao.putLessonJourney(LessonJourney(courseId, lesson.id, lesson.version,
                    LessonStage.Intro.name, clock.millis()))
        }

    suspend fun setStage(courseId: String, lesson: PackLesson, stage: LessonStage) =
        db.withTransaction {
            dao.putLessonJourney(LessonJourney(courseId, lesson.id, lesson.version,
                stage.name, clock.millis()))
        }
}
