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
    /** Reconcile active sources from completed current lessons; answer/completion evidence is immutable. */
    suspend fun ensureSeeded(pack: ContentPack) {
        db.withTransaction {
            // Read authoritative rows in the same transaction as schedule reconciliation.
            val completions = dao.lessonCompletions()
            data class Source(val completion: LessonCompletion, val lesson: PackLesson,
                val question: PackQuestion)
            val candidates = completions.filter { it.courseId == pack.courseId }
                .mapNotNull { completion ->
                    pack.lessons.firstOrNull { it.id == completion.lessonId &&
                        it.version == completion.version }?.let { completion to it }
                }.flatMap { (completion, lesson) ->
                    lesson.questions.map { Source(completion, lesson, it) }
                }.groupBy { it.question.conceptId }
            val oldStates = dao.reviewStates().filter { it.courseId == pack.courseId }
            oldStates.filter { it.conceptId !in candidates }.forEach {
                dao.deleteReviewState(pack.courseId, it.conceptId)
            }
            candidates.forEach { (conceptId, sources) ->
                val old = oldStates.firstOrNull { it.conceptId == conceptId }
                val chosen = sources.sortedWith(compareByDescending<Source> {
                    old != null && it.lesson.id == old.lessonId &&
                        it.question.id == old.questionId && it.question.version == old.questionVersion
                }.thenByDescending { it.completion.completedAt }
                    .thenBy { it.lesson.id }.thenBy { it.question.id }).first()
                val unchanged = old != null && old.lessonId == chosen.lesson.id &&
                    old.questionId == chosen.question.id &&
                    old.questionVersion == chosen.question.version
                val replacement = if (unchanged) old!!.copy(lessonVersion = chosen.lesson.version)
                else ReviewState(conceptId, chosen.lesson.id, chosen.lesson.version,
                    chosen.question.id, chosen.question.version, 0, chosen.completion.completedAt,
                    initialReviewDue(Instant.ofEpochMilli(chosen.completion.completedAt)).toEpochMilli(),
                    null, pack.courseId)
                if (old == null) dao.insertReviewState(replacement)
                else dao.updateReviewState(replacement)
            }
        }
    }

    /** One answer event and its new schedule commit together. Duplicate delivery is inert. */
    suspend fun submit(state: ReviewState, question: PackQuestion, sessionId: String,
        selected: Int, hinted: Boolean, revealed: Boolean): ReviewResult = db.withTransaction {
        require(selected in question.choices.indices || (selected == -1 && revealed))
        val current = dao.reviewState(state.conceptId, state.courseId)
            ?: throw IllegalArgumentException("Review concept is no longer available")
        require(current.lessonId == state.lessonId && current.lessonVersion == state.lessonVersion &&
            current.questionId == question.id && current.questionVersion == question.version &&
            current.conceptId == question.conceptId) { "Review source changed" }
        val now = clock.instant()
        val due = current.dueAt <= now.toEpochMilli()
        val event = AnswerEvent("review:${state.courseId}:$sessionId:${state.conceptId}",
            current.lessonId, current.questionId, current.questionVersion, sessionId,
            selected, selected == question.correctIndex, hinted, revealed,
            now.toEpochMilli(), if (due) "review" else "link", state.courseId)
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
