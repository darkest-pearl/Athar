package dev.elm.prototype

import androidx.room.withTransaction
import java.time.Clock
import java.time.Duration
import java.time.Instant

/** UTC instants, with whole-day intervals from the actual review time. */
data class ReviewDecision(val stage: Int, val due: Instant)
private val reviewIntervals = listOf(1L, 3L, 7L, 14L, 30L)
const val REVIEW_BATCH_LIMIT = 5

fun initialReviewDue(learnedAt: Instant): Instant = learnedAt.plus(Duration.ofDays(1))

fun nextReview(stage: Int, correct: Boolean, revealed: Boolean, now: Instant,
    hinted: Boolean = false): ReviewDecision {
    require(stage in 0..4)
    val nextStage = if (correct && !revealed && !hinted) (stage + 1).coerceAtMost(4) else 0
    return ReviewDecision(nextStage, now.plus(Duration.ofDays(reviewIntervals[nextStage])))
}

fun dueReviewCount(states: List<ReviewState>, now: Instant): Int =
    states.count { it.dueAt <= now.toEpochMilli() }

fun selectReviewBatch(states: List<ReviewState>, now: Instant,
    relatedConceptIds: List<String> = emptyList(), limit: Int = REVIEW_BATCH_LIMIT): List<ReviewState> {
    require(limit in 1..20)
    val byId = states.associateBy { it.conceptId }
    val related = relatedConceptIds.mapNotNull(byId::get)
    val due = states.filter { it.dueAt <= now.toEpochMilli() }.sortedWith(
        compareBy<ReviewState> { it.dueAt }.thenBy { it.conceptId })
    return (related + due).distinctBy { it.conceptId }.take(limit)
}

data class ReviewResult(val recorded: Boolean, val wasDue: Boolean, val state: ReviewState)

class ReviewRepository(private val db: LearningDatabase, private val dao: LearningDao,
    private val clock: Clock) {
    /** Seed migrated and new completions from the validated current pack without replacing history. */
    suspend fun ensureSeeded(pack: ContentPack, completions: List<LessonCompletion>) {
        db.withTransaction {
            completions.sortedBy { it.completedAt }.forEach { completion ->
                val lesson = pack.lessons.firstOrNull {
                    it.id == completion.lessonId && it.version == completion.version
                } ?: return@forEach
                lesson.questions.forEach { question ->
                    dao.insertReviewState(ReviewState(question.conceptId, lesson.id, lesson.version,
                        question.id, question.version, 0, completion.completedAt,
                        initialReviewDue(Instant.ofEpochMilli(completion.completedAt)).toEpochMilli(), null))
                }
            }
        }
    }

    /** One answer event and its new schedule commit together. Duplicate delivery is inert. */
    suspend fun submit(state: ReviewState, question: PackQuestion, sessionId: String,
        selected: Int, hinted: Boolean, revealed: Boolean): ReviewResult = db.withTransaction {
        require(selected in question.choices.indices || (selected == -1 && revealed))
        val current = dao.reviewState(state.conceptId)
            ?: throw IllegalArgumentException("Review concept is no longer available")
        require(current.questionId == question.id && current.questionVersion == question.version &&
            current.conceptId == question.conceptId) { "Review question version changed" }
        val now = clock.instant()
        val due = current.dueAt <= now.toEpochMilli()
        val event = AnswerEvent("review:$sessionId:${state.conceptId}",
            current.lessonId, current.questionId, current.questionVersion, sessionId,
            selected, selected == question.correctIndex, hinted, revealed,
            now.toEpochMilli(), if (due) "review" else "link")
        if (dao.insertAnswerEvent(event) == -1L)
            return@withTransaction ReviewResult(false, due, current)
        val updated = if (due) {
            val decision = nextReview(current.stage, event.correct, revealed, now, hinted)
            current.copy(stage = decision.stage, dueAt = decision.due.toEpochMilli(),
                lastReviewedAt = now.toEpochMilli()).also { dao.updateReviewState(it) }
        } else current
        ReviewResult(true, due, updated)
    }
}
