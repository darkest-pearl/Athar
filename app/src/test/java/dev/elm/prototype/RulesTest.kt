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
 @Test fun firstDueAndIndependentStagesAreExplicit() {
  val learned=Instant.parse("2026-10-01T12:00:00Z")
  assertEquals(learned.plusSeconds(86400),initialReviewDue(learned))
  val intervals=listOf(3L,7L,14L,30L,30L)
  for(stage in 0..4) {
   val result=nextReview(stage,true,false,learned)
   assertEquals((stage+1).coerceAtMost(4),result.stage)
   assertEquals(learned.plusSeconds(intervals[stage]*86400),result.due)
  }
  assertEquals(ReviewDecision(0,learned.plusSeconds(86400)),
   nextReview(3,true,false,learned,hinted=true))
 }
 @Test fun dueBatchIsBoundedAndRelatedConceptsAreDeduplicated() {
  val now=Instant.parse("2026-10-05T00:00:00Z")
  val states=(0..7).map { i -> ReviewState("concept-$i","lesson",1,"q$i",1,0,
   now.minusSeconds(86400).toEpochMilli(),now.minusSeconds(10L+i).toEpochMilli(),null) }
  val batch=selectReviewBatch(states,now,listOf("concept-0","concept-0"),5)
  assertEquals(5,batch.size)
  assertEquals(1,batch.count { it.conceptId=="concept-0" })
  assertEquals(8,dueReviewCount(states,now))
  assertEquals(0,dueReviewCount(states,now.minusSeconds(86400)))
 }

}
