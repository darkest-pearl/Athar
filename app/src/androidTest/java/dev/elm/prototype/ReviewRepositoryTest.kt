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
class ReviewRepositoryTest {
    private fun clock(value: String) = Clock.fixed(Instant.parse(value), ZoneOffset.UTC)

    @Test fun schedulesAtomicallyAndDoesNotAdvanceAssistedOrDuplicateRecall() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "review-verification.db"
        context.deleteDatabase(name)
        val pack = ContentPackParser.parse(context.assets.open("fixture-pack.json").use { it.readBytes() })
        val lesson = pack.lessons.first()
        val learned = Instant.parse("2026-10-01T20:00:00Z")
        val db = LearningDatabase.open(context, name)
        val dao = db.learningDao()
        dao.insertLessonCompletion(LessonCompletion("controls-fixture:v1", lesson.id, lesson.version,
            learned.toEpochMilli(), "2026-10-02", "Asia/Dubai", "session-learn"))
        val early = ReviewRepository(db, dao, clock("2026-10-02T08:00:00Z"))
        early.ensureSeeded(pack, dao.lessonCompletions())
        val first = dao.reviewState("pause-control")!!
        assertEquals(initialReviewDue(learned).toEpochMilli(), first.dueAt)
        assertEquals(2, dao.reviewStates().size)
        assertEquals(0, dueReviewCount(dao.reviewStates(), Instant.parse("2026-10-02T08:00:00Z")))
        assertEquals(listOf("pause-control"), selectReviewBatch(dao.reviewStates(),
            Instant.parse("2026-10-02T08:00:00Z"), listOf("pause-control","pause-control"))
            .map { it.conceptId })
        val linked = early.submit(first, lesson.questions[0], "link-session", 0, false, false)
        assertTrue(linked.recorded)
        assertFalse(linked.wasDue)
        assertEquals(first.dueAt, dao.reviewState("pause-control")!!.dueAt)
        val due = ReviewRepository(db, dao, clock("2026-10-02T21:00:00Z"))
        val independent = due.submit(first, lesson.questions[0], "review-session", 0, false, false)
        assertTrue(independent.recorded)
        assertEquals(1, independent.state.stage)
        assertEquals(Instant.parse("2026-10-05T21:00:00Z").toEpochMilli(), independent.state.dueAt)
        val duplicate = due.submit(first, lesson.questions[0], "review-session", 1, false, false)
        assertFalse(duplicate.recorded)
        assertEquals(1, dao.reviewState("pause-control")!!.stage)
        val wrong = due.submit(dao.reviewState("completion-control")!!,
            lesson.questions[1], "wrong-session", 0, false, false)
        assertEquals(0, wrong.state.stage)
        assertEquals(Instant.parse("2026-10-03T21:00:00Z").toEpochMilli(), wrong.state.dueAt)
        val nextDay = ReviewRepository(db, dao, clock("2026-10-03T22:00:00Z"))
        val hinted = nextDay.submit(wrong.state, lesson.questions[1],
            "hint-session", 2, true, false)
        assertEquals(0, hinted.state.stage)
        assertEquals(Instant.parse("2026-10-04T22:00:00Z").toEpochMilli(), hinted.state.dueAt)
        val later = ReviewRepository(db, dao, clock("2026-10-05T22:00:00Z"))
        val revealed = later.submit(dao.reviewState("pause-control")!!,
            lesson.questions[0], "reveal-session", -1, false, true)
        assertEquals(0, revealed.state.stage)
        assertEquals(Instant.parse("2026-10-06T22:00:00Z").toEpochMilli(), revealed.state.dueAt)
        db.close()
        val reopened = LearningDatabase.open(context, name)
        assertEquals(5, reopened.learningDao().answerEvents().size)
        assertEquals(0, reopened.learningDao().reviewState("pause-control")!!.stage)
        reopened.close()
        context.deleteDatabase(name)
        Unit
    }
}
