package dev.elm.prototype

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class StudyRepositoryTest {
    private val zone = ZoneId.of("Asia/Dubai")
    private fun clock(value: String) = Clock.fixed(Instant.parse(value), zone)

    @Test fun distinctCompletedSessionsShareDailyCreditAndSurviveRestart() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "study-verification.db"
        context.deleteDatabase(name)
        val pack = ContentPackParser.parse(context.assets.open("fixture-pack.json").use { it.readBytes() })
        val lesson = pack.lessons.first()
        val db = LearningDatabase.open(context, name)
        val dao = db.learningDao()
        val day1 = clock("2026-01-01T10:00:00Z")
        val answers = LearningRepository(dao, day1, zone)
        val study = StudyRepository(db, dao, day1, zone)
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { study.completeLesson(lesson, "incomplete") }
        }
        lesson.questions.forEach { answers.submitLessonAnswer(lesson, it, "session-1",
            (it.correctIndex + 1) % it.choices.size) }
        assertTrue(study.completeLesson(lesson, "session-1"))
        assertFalse(study.completeLesson(lesson, "session-1"))
        assertEquals(1, dao.studyDays().size)
        assertEquals(1, dao.lessonCompletions().size)
        val second = StudyRepository(db, dao, clock("2026-01-01T18:00:00Z"), zone)
        val lateAnswers = LearningRepository(dao, clock("2026-01-01T18:00:00Z"), zone)
        lesson.questions.forEach { lateAnswers.submitLessonAnswer(lesson, it, "session-2", it.correctIndex) }
        assertTrue(second.completeLesson(lesson, "session-2"))
        assertEquals(2, dao.studySessions().size)
        assertEquals(1, dao.studyDays().size)
        assertEquals(1, dao.lessonCompletions().size)

        val reviewClock = clock("2026-01-02T10:00:00Z")
        val item = ReviewState("pause-control", lesson.id, lesson.version,
            lesson.questions[0].id, lesson.questions[0].version, 0, day1.millis(),
            reviewClock.millis(), null)
        dao.insertReviewState(item)
        val review = ReviewRepository(db, dao, reviewClock)
        review.submit(item, lesson.questions[0], "batch-1", -1, false, true)
        val reviewStudy = StudyRepository(db, dao, reviewClock, zone)
        assertTrue(reviewStudy.completeDueReviewBatch(listOf(item), "batch-1"))
        assertFalse(reviewStudy.completeDueReviewBatch(listOf(item), "batch-1"))
        assertEquals(2, dao.studyDays().size)
        assertEquals(StreakStats(2, 2, 2), streakStats(dao.studyDays(),
            java.time.LocalDate.parse("2026-01-03")))
        db.close()
        val reopened = LearningDatabase.open(context, name)
        assertEquals(3, reopened.learningDao().studySessions().size)
        assertEquals(2, reopened.learningDao().studyDays().size)
        reopened.close()
        context.deleteDatabase(name)
        Unit
    }
}
