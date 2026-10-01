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

    @Test fun updatedLessonCanSeedCurrentConceptAfterOldSourceWasCompleted() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "review-upgrade-reproduction.db"
        context.deleteDatabase(name)
        val original = context.assets.open("fixture-pack.json").use { it.readBytes().toString(Charsets.UTF_8) }
        val v1 = ContentPackParser.parse(original.toByteArray())
        val lesson = v1.lessons.first()
        val db = LearningDatabase.open(context, name)
        val dao = db.learningDao()
        val t1 = Instant.parse("2026-10-01T00:00:00Z").toEpochMilli()
        dao.insertLessonCompletion(LessonCompletion("${lesson.id}:v1", lesson.id, 1,
            t1, "2026-10-01", "UTC", "first"))
        val repo = ReviewRepository(db, dao, clock("2026-10-01T00:00:00Z"))
        repo.ensureSeeded(v1, dao.lessonCompletions())
        assertEquals(1, dao.reviewState("pause-control")!!.questionVersion)
        val practiced = ReviewRepository(db, dao, clock("2026-10-02T02:00:00Z"))
            .submit(dao.reviewState("pause-control")!!, lesson.questions[0],
                "first-review", lesson.questions[0].correctIndex, false, false)
        assertEquals(1, practiced.state.stage)
        val revisedJson = original.replaceFirst("\"contentVersion\": 2", "\"contentVersion\": 3")
            .replaceFirst("\"version\": 1", "\"version\": 2")
            .replaceFirst("\"version\": 1", "\"version\": 2")
        val v2 = ContentPackParser.parse(revisedJson.toByteArray())
        val revised = v2.lessons.first()
        dao.insertLessonCompletion(LessonCompletion("${revised.id}:v2", revised.id, 2,
            t1 + 2 * 24 * 3600_000, "2026-10-03", "UTC", "second"))
        repo.ensureSeeded(v2, dao.lessonCompletions())
        assertEquals(0, dao.reviewState("pause-control")!!.stage)
        val eligible = dao.reviewStates().filter { state -> revised.questions.any {
            it.conceptId == state.conceptId && it.id == state.questionId &&
                it.version == state.questionVersion
        } }
        assertTrue("updated concept disappeared", eligible.any { it.conceptId == "pause-control" })
        assertEquals(1, selectReviewBatch(eligible, Instant.parse("2026-10-05T00:00:00Z"))
            .count { it.conceptId == "pause-control" })
        db.close()
        val reopened = LearningDatabase.open(context, name)
        assertEquals(2, reopened.learningDao().reviewState("pause-control")!!.questionVersion)
        assertEquals(2, reopened.learningDao().lessonCompletions().count { it.lessonId == lesson.id })
        reopened.close()
        context.deleteDatabase(name)
        Unit
    }

    @Test fun overlappingCoursesAndSharedConceptsStayScopedAndDeduplicated() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "review-course-scope.db"
        context.deleteDatabase(name)
        val raw = context.assets.open("fixture-pack.json").use { it.readBytes().toString(Charsets.UTF_8) }
        val first = ContentPackParser.parse(raw.toByteArray())
        val second = ContentPackParser.parse(raw.replace("controls-course", "second-course")
            .replace("App controls", "Other controls")
            .replaceFirst("\"conceptId\": \"bookmark-control\"",
                "\"conceptId\": \"pause-control\"").toByteArray())
        val db = LearningDatabase.open(context, name)
        val dao = db.learningDao()
        val at = Instant.parse("2026-10-01T00:00:00Z").toEpochMilli()
        (first.lessons.map { first.courseId to it } + second.lessons.map { second.courseId to it })
            .forEach { (courseId, lesson) ->
                dao.insertLessonCompletion(LessonCompletion("$courseId:${lesson.id}:v1", lesson.id,
                    1, at, "2026-10-01", "UTC", "session-$courseId-${lesson.id}", courseId))
            }
        val repo = ReviewRepository(db, dao, clock("2026-10-01T00:00:00Z"))
        repo.ensureSeeded(first, dao.lessonCompletions())
        repo.ensureSeeded(second, dao.lessonCompletions())
        assertEquals(4, dao.reviewStates().count { it.courseId == first.courseId })
        assertEquals(3, dao.reviewStates().count { it.courseId == second.courseId })
        assertNotNull(dao.reviewState("pause-control", first.courseId))
        assertNotNull(dao.reviewState("pause-control", second.courseId))
        val learning = LearningRepository(dao, clock("2026-10-01T00:00:00Z"), ZoneOffset.UTC)
        learning.bookmark(first.lessons.first().id, first.courseId)
        assertEquals(1, dao.bookmarks().count { it.courseId == first.courseId })
        assertEquals(0, dao.bookmarks().count { it.courseId == second.courseId })
        db.close()
        context.deleteDatabase(name)
        Unit
    }

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
