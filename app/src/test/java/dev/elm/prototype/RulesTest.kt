package dev.elm.prototype
import org.junit.Assert.*
import org.junit.Test
import java.time.*
class RulesTest {
 @Test fun studyDayUsesSavedTimezoneAcrossMidnight() {
  val zone=ZoneId.of("Asia/Dubai")
  assertEquals("2026-10-01",studyDay(Instant.parse("2026-10-01T19:59:59Z"),zone))
  assertEquals("2026-10-02",studyDay(Instant.parse("2026-10-01T20:00:00Z"),zone))
 }
 @Test fun revealedOrWrongRecallDoesNotAdvanceStage() {
  val now=Instant.parse("2026-10-01T00:00:00Z")
  assertEquals(ReviewDecision(0,now.plusSeconds(86400)),nextReview(3,true,true,now))
  assertEquals(ReviewDecision(0,now.plusSeconds(86400)),nextReview(3,false,false,now))
  assertEquals(ReviewDecision(4,now.plusSeconds(30*86400)),nextReview(3,true,false,now))
 }
}
