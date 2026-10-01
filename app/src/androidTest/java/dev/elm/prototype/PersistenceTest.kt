package dev.elm.prototype
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import java.time.*
@RunWith(AndroidJUnit4::class)
class PersistenceTest {
 @Test fun completionAndAttemptSurviveReopenWithoutDuplicates()=runBlocking {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val name="verification.db"; context.deleteDatabase(name)
  val db=LearningDatabase.open(context,name)
  val repo=LearningRepository(db.learningDao(),Clock.fixed(Instant.parse("2026-10-01T20:00:00Z"),ZoneOffset.UTC),ZoneId.of("Asia/Dubai"))
  Assert.assertTrue(repo.finish("fixture")!=-1L)
  Assert.assertEquals(-1L,repo.finish("fixture"))
  repo.answer(0,1,false); Assert.assertEquals(-1L,repo.answer(0,0,true))
  db.close()
  val reopened=LearningDatabase.open(context,name)
  Assert.assertEquals(1,reopened.learningDao().completions().size)
  Assert.assertEquals("2026-10-02",reopened.learningDao().completions().single().studyDay)
  Assert.assertEquals(false,reopened.learningDao().attempts().single().correct)
  reopened.close(); context.deleteDatabase(name); Unit
 }
}
