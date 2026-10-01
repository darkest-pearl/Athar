package dev.elm.prototype

import android.content.Context
import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class ContentAndMigrationTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun fixture() = context.assets.open("fixture-pack.json").use { it.readBytes() }

    @Test fun parserUsesTwoLessonsAndRejectsMalformedCandidates() {
        val bytes = fixture()
        val pack = ContentPackParser.parse(bytes)
        assertEquals(2, pack.lessons.size)
        assertEquals(listOf("controls-fixture", "study-tools-fixture"), pack.lessons.map { it.id })
        val raw = bytes.toString(Charsets.UTF_8)
        val bad = listOf(
            raw.replace("\"schemaVersion\": 1", "\"schemaVersion\": 2"),
            raw.replace("Which control pauses playback?", " "),
            raw.replace("\"contentVersion\": 2", "\"contentVersion\": true"),
            raw.replaceFirst("\"id\": \"q1\"", "\"id\": \"q0\""),
            raw.replaceFirst("\"startMs\": 0", "\"startMs\": \"bad\"")
        )
        bad.forEachIndexed { index, json ->
            val result = runCatching { ContentPackParser.parse(json.toByteArray()) }
            assertTrue("candidate $index was accepted", result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.isNotBlank() == true)
        }
        assertTrue(runCatching {
            ContentPackParser.parse(ByteArray(ContentPackParser.MAX_BYTES + 1))
        }.isFailure)
    }

    @Test fun rejectedImportKeepsWorkingPack() {
        val sandbox = File(context.cacheDir, "content-import-test")
        sandbox.mkdirs()
        val wrapped = object : ContextWrapper(context) { override fun getFilesDir(): File = sandbox }
        val repo = ContentRepository(wrapped)
        val initial = repo.load()
        assertEquals(initial, repo.importBytes(fixture(), initial))
        val changedSameVersion = fixture().toString(Charsets.UTF_8)
            .replace("Learn the controls", "Different title")
        assertTrue(runCatching {
            repo.importBytes(changedSameVersion.toByteArray(), initial)
        }.isFailure)
        assertTrue(runCatching { repo.importBytes("{".toByteArray(), initial) }.isFailure)
        assertEquals(initial.canonicalJson, repo.load().canonicalJson)
        File(sandbox, "active-pack.json").delete()
        File(sandbox, "active-pack.json.bak").delete()
        sandbox.delete()
    }

    @Test fun versionOneDatabaseMigratesCompletionAndFirstAnswers() = runBlocking {
        val name = "migration-v1-verification.db"
        context.deleteDatabase(name)
        val old = context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null)
        old.execSQL("CREATE TABLE Completion (lessonId TEXT NOT NULL, version INTEGER NOT NULL, completedAt INTEGER NOT NULL, studyDay TEXT NOT NULL, studyZone TEXT NOT NULL, PRIMARY KEY(lessonId))")
        old.execSQL("CREATE TABLE Attempt (id TEXT NOT NULL, questionId TEXT NOT NULL, questionVersion INTEGER NOT NULL, selected INTEGER NOT NULL, correct INTEGER NOT NULL, attemptedAt INTEGER NOT NULL, PRIMARY KEY(id))")
        old.execSQL("INSERT INTO Completion VALUES ('controls-fixture',1,1000,'2026-10-01','Asia/Dubai')")
        old.execSQL("INSERT INTO Attempt VALUES ('fixture-v1-q0','q0',1,1,0,900)")
        old.execSQL("INSERT INTO Attempt VALUES ('fixture-v1-q1','q1',1,2,1,950)")
        old.version = 1
        old.close()
        val upgraded = LearningDatabase.open(context, name)
        val dao = upgraded.learningDao()
        assertEquals(1, dao.completions().size)
        assertEquals("controls-fixture", dao.lessonCompletions().single().lessonId)
        assertEquals(listOf("q0", "q1"), dao.answerEvents().map { it.questionId })
        assertEquals(listOf(false, true), dao.answerEvents().map { it.correct })
        upgraded.close()
        val reopened = LearningDatabase.open(context, name)
        assertEquals(2, reopened.learningDao().answerEvents().size)
        reopened.close()
        context.deleteDatabase(name)
        Unit
    }

    @Test fun independentLessonsAndImmutableSubmissionsPersist() = runBlocking {
        val name = "learning-v2-verification.db"
        context.deleteDatabase(name)
        val pack = ContentPackParser.parse(fixture())
        val db = LearningDatabase.open(context, name)
        val repo = LearningRepository(db.learningDao(),
            Clock.fixed(Instant.parse("2026-10-01T20:00:00Z"), ZoneOffset.UTC), ZoneId.of("Asia/Dubai"))
        val first = pack.lessons[0]
        val second = pack.lessons[1]
        assertTrue(repo.submitLessonAnswer(first, first.questions[0], "session-a", 1) >= 0)
        assertEquals(-1L, repo.submitLessonAnswer(first, first.questions[0], "session-a", 0))
        assertTrue(repo.submitLessonAnswer(first, first.questions[0], "session-b", 0) >= 0)
        assertTrue(repo.submitLessonAnswer(second, second.questions[0], "session-c", 0) >= 0)
        assertFalse(db.learningDao().firstLessonAttempt(first.id, "q0", 1)!!.correct)
        assertTrue(repo.completeLesson(first, "session-a") >= 0)
        assertTrue(repo.completeLesson(second, "session-c") >= 0)
        assertEquals(-1L, repo.completeLesson(first, "session-b"))
        repo.bookmark(second.id)
        db.close()
        val reopened = LearningDatabase.open(context, name)
        assertEquals(2, reopened.learningDao().lessonCompletions().size)
        assertEquals(3, reopened.learningDao().answerEvents().size)
        assertEquals(second.id, reopened.learningDao().bookmarks().single().lessonId)
        reopened.close()
        context.deleteDatabase(name)
        Unit
    }
}
