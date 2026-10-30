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
class ConceptSessionTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val clock = Clock.fixed(Instant.parse("2026-10-03T12:00:00Z"), ZoneOffset.UTC)

    @Test fun authoredOrderChoiceMappingCorrectionAndCompletionSurviveReopen() = runBlocking {
        val name = "concept-session-verification.db"
        context.deleteDatabase(name)
        val pack = ContentPackParser.parse(context.assets.open("concept-fixture-pack.json")
            .use { it.readBytes() })
        val lesson = pack.lessons.first()
        val db = LearningDatabase.open(context, name)
        val repo = ConceptSessionRepository(db, db.learningDao(), clock)
        val started = repo.start(pack, lesson, "stable-session")
        assertEquals(lesson.activities.map { it.id },
            started.conceptActivities().map { it.id })
        val firstOrder = started.conceptActivities()[1].choiceOrder
        assertEquals(listOf(0, 1, 2), firstOrder.sorted())
        repo.viewSource(started.id)
        repo.advance(started.id, pack, lesson)
        repo.useHint(started.id)
        repo.select(started.id, firstOrder.indexOf(1))
        repo.check(started.id, pack, lesson)
        assertTrue(db.learningDao().pendingSession(started.id)!!.feedback)
        db.close()

        val reopened = LearningDatabase.open(context, name)
        val later = ConceptSessionRepository(reopened, reopened.learningDao(), clock)
        val saved = reopened.learningDao().pendingSession(started.id)!!
        assertEquals(firstOrder, saved.conceptActivities()[1].choiceOrder)
        assertEquals(1, saved.conceptActivities()[1].firstSelected)
        assertEquals(-1, saved.selected)
        assertTrue(saved.hinted)
        assertTrue(saved.feedback)
        assertTrue(saved.conceptActivities()[0].sourceViewed)
        assertFalse(reopened.learningDao().answerEvents().single().correct)
        try {
            later.check(started.id, pack, lesson)
            fail("Duplicate first submission must not create a new answer")
        } catch (_: IllegalArgumentException) { }
        assertEquals(1, reopened.learningDao().answerEvents().size)
        val correct = lesson.questions[0].correctIndex
        later.select(started.id, firstOrder.indexOf(correct))
        later.check(started.id, pack, lesson)
        assertTrue(reopened.learningDao().pendingSession(started.id)!!
            .conceptActivities()[1].corrected)
        assertEquals(setOf("lesson", "correction"),
            reopened.learningDao().answerEvents().map { it.kind }.toSet())
        later.advance(started.id, pack, lesson)
        later.advance(started.id, pack, lesson)
        val secondOrder = reopened.learningDao().pendingSession(started.id)!!
            .conceptActivities()[3].choiceOrder
        later.select(started.id, secondOrder.indexOf(lesson.questions[1].correctIndex))
        later.check(started.id, pack, lesson)
        val study = StudyRepository(reopened, reopened.learningDao(), clock, ZoneOffset.UTC)
        val replay = reopened.learningDao().pendingSession(started.id)!!
            .conceptActivities().last()
        assertEquals("replay", replay.kind)
        assertEquals(lesson.questions[0].id, replay.reference)
        try {
            study.completeLesson(lesson, started.id, pack.courseId)
            fail("Mistake replay must finish before lesson completion")
        } catch (_: IllegalArgumentException) { }
        later.advance(started.id, pack, lesson)
        later.select(started.id, replay.choiceOrder.indexOf(lesson.questions[0].correctIndex))
        later.check(started.id, pack, lesson)
        assertEquals(1, reopened.learningDao().answerEvents().count { it.kind == "replay" })
        assertTrue(study.completeLesson(lesson, started.id, pack.courseId))
        assertFalse(study.completeLesson(lesson, started.id, pack.courseId))
        assertEquals(1, reopened.learningDao().lessonCompletions().size)
        assertTrue(reopened.learningDao().pendingSession(started.id)!!.finalized)
        ReviewRepository(reopened, reopened.learningDao(), clock).ensureSeeded(pack)
        val review = reopened.learningDao().reviewState("map-key", pack.courseId)!!
        assertEquals(lesson.questions[1].id, review.questionId)
        assertEquals(0, review.stage)
        reopened.close()
        context.deleteDatabase(name)
        Unit
    }
}
