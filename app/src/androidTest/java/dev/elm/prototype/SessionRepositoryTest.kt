package dev.elm.prototype

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class SessionRepositoryTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val pack get() = ContentPackParser.parse(
        context.assets.open("fixture-pack.json").use { it.readBytes() })
    private val clock = Clock.fixed(Instant.parse("2026-10-03T12:00:00Z"), ZoneOffset.UTC)

    @Test fun reviewBatchSurvivesReopenAndFinalSaveFinalizesOnce() = runBlocking {
        val name = "pending-review-verification.db"
        context.deleteDatabase(name)
        val content = pack
        val lesson = content.lessons.first()
        val db = LearningDatabase.open(context, name)
        val dao = db.learningDao()
        val learned = Instant.parse("2026-10-01T12:00:00Z").toEpochMilli()
        val batch = lesson.questions.map { question -> ReviewState(question.conceptId,
            lesson.id, lesson.version, question.id, question.version, 0,
            learned, learned + 86_400_000, null, content.courseId) }
        batch.forEach { dao.insertReviewState(it) }
        val sessions = SessionRepository(db, dao, clock)
        sessions.startReview(content, batch, "review-resume")
        sessions.updateInput("review-resume", hinted = true)
        db.close()

        val reopened = LearningDatabase.open(context, name)
        val next = SessionRepository(reopened, reopened.learningDao(), clock)
        val saved = reopened.learningDao().pendingSession("review-resume")!!
        assertEquals(2, saved.items().size)
        assertTrue(saved.items().all { it.wasDue })
        assertTrue(saved.hinted)
        next.updateInput(saved.id, selected = lesson.questions[0].correctIndex)
        val first = next.submitReview(saved.id, lesson.questions[0])
        assertTrue(first.wasDue)
        assertTrue(first.event.hinted)
        assertEquals(0, reopened.learningDao().reviewState(batch[0].conceptId)!!.stage)
        next.updateInput(saved.id, selected = 1)
        val retry = next.submitReview(saved.id, lesson.questions[0])
        assertEquals(first.event.selected, retry.event.selected)
        assertTrue(retry.event.hinted)
        next.advance(saved.id)
        next.updateInput(saved.id, selected = lesson.questions[1].correctIndex)
        next.submitReview(saved.id, lesson.questions[1])
        reopened.close()

        val afterFinalSave = LearningDatabase.open(context, name)
        val pending = afterFinalSave.learningDao().pendingSession("review-resume")!!
        assertEquals(1, pending.cursor)
        assertTrue(pending.feedback)
        val study = StudyRepository(afterFinalSave, afterFinalSave.learningDao(), clock, ZoneOffset.UTC)
        assertTrue(study.completeDueReviewBatch(pending.items().filter { it.wasDue }
            .map { it.reviewState(content.courseId) }, pending.id, content.courseId))
        assertFalse(study.completeDueReviewBatch(pending.items().filter { it.wasDue }
            .map { it.reviewState(content.courseId) }, pending.id, content.courseId))
        assertTrue(afterFinalSave.learningDao().pendingSession(pending.id)!!.finalized)
        assertEquals(1, afterFinalSave.learningDao().studyDays().size)
        assertEquals(2, afterFinalSave.learningDao().answerEvents().size)
        afterFinalSave.close()
        context.deleteDatabase(name)
        Unit
    }

    @Test fun partialLessonAndIntentionalRepeatKeepFirstSavedFeedback() = runBlocking {
        val name = "pending-lesson-verification.db"
        context.deleteDatabase(name)
        val content = pack
        val lesson = content.lessons.first()
        val db = LearningDatabase.open(context, name)
        val sessions = SessionRepository(db, db.learningDao(), clock)
        sessions.startLesson(content, lesson, "lesson-one")
        sessions.updateInput("lesson-one", selected = 1)
        db.close()
        val reopened = LearningDatabase.open(context, name)
        val next = SessionRepository(reopened, reopened.learningDao(), clock)
        assertEquals(1, reopened.learningDao().pendingSession("lesson-one")!!.selected)
        val wrong = next.submitLesson("lesson-one", lesson, lesson.questions[0])
        assertFalse(wrong.event.correct)
        next.updateInput("lesson-one", selected = 0)
        val retry = next.submitLesson("lesson-one", lesson, lesson.questions[0])
        assertEquals(1, retry.event.selected)
        assertFalse(retry.event.correct)
        next.advance("lesson-one")
        assertEquals(1, reopened.learningDao().pendingSession("lesson-one")!!.cursor)
        next.startLesson(content, lesson, "lesson-repeat")
        next.updateInput("lesson-repeat", selected = lesson.questions[0].correctIndex)
        assertTrue(next.submitLesson("lesson-repeat", lesson, lesson.questions[0]).event.correct)
        assertEquals(2, reopened.learningDao().answerEvents().size)
        reopened.close()
        context.deleteDatabase(name)
        Unit
    }

    @Test fun originalDueQualificationAndUnavailableSourceArePreserved() = runBlocking {
        val name = "pending-due-snapshot.db"
        context.deleteDatabase(name)
        val content = pack
        val lesson = content.lessons.first()
        val question = lesson.questions.first()
        val db = LearningDatabase.open(context, name)
        val dao = db.learningDao()
        val dueAt = Instant.parse("2026-10-03T12:00:00Z").toEpochMilli()
        val state = ReviewState(question.conceptId, lesson.id, lesson.version,
            question.id, question.version, 0, dueAt - 86_400_000,
            dueAt, null, content.courseId)
        dao.insertReviewState(state)
        val early = SessionRepository(db, dao,
            Clock.fixed(Instant.parse("2026-10-03T11:00:00Z"), ZoneOffset.UTC))
        early.startReview(content, listOf(state), "before-due")
        val late = SessionRepository(db, dao,
            Clock.fixed(Instant.parse("2026-10-03T13:00:00Z"), ZoneOffset.UTC))
        late.updateInput("before-due", selected = question.correctIndex)
        val submitted = late.submitReview("before-due", question)
        assertFalse(submitted.wasDue)
        assertEquals("link", submitted.event.kind)
        assertEquals(dueAt, dao.reviewState(question.conceptId)!!.dueAt)
        late.startReview(content, listOf(state), "source-removed")
        dao.deleteReviewState(content.courseId, question.conceptId)
        late.updateInput("source-removed", selected = question.correctIndex)
        assertTrue(runCatching { late.submitReview("source-removed", question) }.isFailure)
        assertNull(dao.answerEvent("review:${content.courseId}:source-removed:${question.conceptId}"))
        assertFalse(dao.pendingSession("source-removed")!!.finalized)
        late.abandon("source-removed")
        assertTrue(dao.pendingSession("source-removed")!!.finalized)
        db.close()
        context.deleteDatabase(name)
        Unit
    }
}
