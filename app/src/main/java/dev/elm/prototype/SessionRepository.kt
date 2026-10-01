package dev.elm.prototype

import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject
import java.time.Clock

/** A compact, durable reference to material; the active pack remains the source of text. */
data class SessionItem(
    val lessonId: String, val lessonVersion: Int, val questionId: String,
    val questionVersion: Int, val conceptId: String, val wasDue: Boolean = false,
    val stage: Int = 0, val learnedAt: Long = 0, val dueAt: Long = 0,
    val lastReviewedAt: Long? = null
) {
    fun reviewState(courseId: String) = ReviewState(conceptId, lessonId, lessonVersion,
        questionId, questionVersion, stage, learnedAt, dueAt, lastReviewedAt, courseId)
}

fun PendingSession.items(): List<SessionItem> {
    val array = JSONArray(itemsJson)
    return (0 until array.length()).map { index ->
        val item = array.getJSONObject(index)
        SessionItem(item.getString("lessonId"), item.getInt("lessonVersion"),
            item.getString("questionId"), item.getInt("questionVersion"),
            item.getString("conceptId"), item.optBoolean("wasDue", false),
            item.optInt("stage", 0), item.optLong("learnedAt", 0),
            item.optLong("dueAt", 0),
            if (item.isNull("lastReviewedAt")) null else item.getLong("lastReviewedAt"))
    }
}

private fun encode(items: List<SessionItem>): String = JSONArray().apply {
    items.forEach { item -> put(JSONObject().apply {
        put("lessonId", item.lessonId)
        put("lessonVersion", item.lessonVersion)
        put("questionId", item.questionId)
        put("questionVersion", item.questionVersion)
        put("conceptId", item.conceptId)
        put("wasDue", item.wasDue)
        put("stage", item.stage)
        put("learnedAt", item.learnedAt)
        put("dueAt", item.dueAt)
        put("lastReviewedAt", item.lastReviewedAt ?: JSONObject.NULL)
    }) }
}.toString()

data class SessionSubmission(val event: AnswerEvent, val wasDue: Boolean)

class SessionRepository(private val db: LearningDatabase, private val dao: LearningDao,
    private val clock: Clock) {
    suspend fun startLesson(pack: ContentPack, lesson: PackLesson, id: String): PendingSession {
        val now = clock.millis()
        val items = lesson.questions.map { SessionItem(lesson.id, lesson.version,
            it.id, it.version, it.conceptId) }
        val session = PendingSession(id, pack.courseId, "lesson", pack.contentVersion,
            lesson.id, lesson.version, encode(items), 0, -1, false, false,
            false, false, now, now)
        dao.insertPendingSession(session)
        return dao.pendingSession(id)!!
    }

    suspend fun startReview(pack: ContentPack, batch: List<ReviewState>, id: String,
        returnLessonId: String = ""): PendingSession {
        val now = clock.millis()
        val items = batch.map { state -> SessionItem(state.lessonId, state.lessonVersion,
            state.questionId, state.questionVersion, state.conceptId,
            state.dueAt <= now, state.stage, state.learnedAt, state.dueAt, state.lastReviewedAt) }
        val session = PendingSession(id, pack.courseId, "review", pack.contentVersion,
            returnLessonId, 0, encode(items), 0, -1, false, false, false, false, now, now)
        dao.insertPendingSession(session)
        return dao.pendingSession(id)!!
    }

    suspend fun updateInput(id: String, selected: Int? = null, hinted: Boolean? = null,
        revealed: Boolean? = null): PendingSession = db.withTransaction {
        val old = requireActive(id)
        if (old.feedback) return@withTransaction old
        val next = old.copy(selected = selected ?: old.selected,
            hinted = hinted ?: old.hinted, revealed = revealed ?: old.revealed,
            updatedAt = clock.millis())
        dao.updatePendingSession(next)
        next
    }

    suspend fun advance(id: String): PendingSession = db.withTransaction {
        val old = requireActive(id)
        require(old.feedback && old.cursor + 1 < old.items().size) { "No next question" }
        val next = old.copy(cursor = old.cursor + 1, selected = -1,
            hinted = false, revealed = false, feedback = false, updatedAt = clock.millis())
        dao.updatePendingSession(next)
        next
    }

    suspend fun submitLesson(id: String, lesson: PackLesson, question: PackQuestion): SessionSubmission =
        db.withTransaction {
            val session = requireActive(id)
            require(session.kind == "lesson")
            val item = session.items().getOrNull(session.cursor)
                ?: throw IllegalArgumentException("Session item missing")
            require(item.lessonId == lesson.id && item.lessonVersion == lesson.version &&
                item.questionId == question.id && item.questionVersion == question.version &&
                item.conceptId == question.conceptId) { "Lesson material changed" }
            val eventId = "${session.courseId}:$id:${lesson.id}:v${lesson.version}:${question.id}:v${question.version}:lesson"
            val existing = dao.answerEvent(eventId)
            val event = existing ?: run {
                require(session.selected in question.choices.indices)
                AnswerEvent(eventId, lesson.id, question.id, question.version, id,
                    session.selected, session.selected == question.correctIndex,
                    session.hinted, session.revealed, clock.millis(), "lesson", session.courseId)
                    .also { dao.insertAnswerEvent(it) }
            }
            dao.updatePendingSession(session.copy(selected = event.selected,
                hinted = event.hinted, revealed = event.revealed,
                feedback = true, updatedAt = clock.millis()))
            SessionSubmission(event, false)
        }

    suspend fun submitReview(id: String, question: PackQuestion): SessionSubmission =
        db.withTransaction {
            val session = requireActive(id)
            require(session.kind == "review")
            val item = session.items().getOrNull(session.cursor)
                ?: throw IllegalArgumentException("Session item missing")
            require(item.questionId == question.id && item.questionVersion == question.version &&
                item.conceptId == question.conceptId) { "Review material changed" }
            val eventId = "review:${session.courseId}:$id:${item.conceptId}"
            val existing = dao.answerEvent(eventId)
            val event = existing ?: run {
                require(session.selected in question.choices.indices ||
                    (session.selected == -1 && session.revealed))
                val current = dao.reviewState(item.conceptId, session.courseId)
                    ?: throw IllegalArgumentException("Review source unavailable")
                require(current.lessonId == item.lessonId &&
                    current.lessonVersion == item.lessonVersion &&
                    current.questionId == item.questionId &&
                    current.questionVersion == item.questionVersion &&
                    current.stage == item.stage && current.dueAt == item.dueAt) {
                    "Review source or schedule changed"
                }
                val now = clock.instant()
                AnswerEvent(eventId, item.lessonId, item.questionId, item.questionVersion,
                    id, session.selected, session.selected == question.correctIndex,
                    session.hinted, session.revealed, now.toEpochMilli(),
                    if (item.wasDue) "review" else "link", session.courseId).also {
                    dao.insertAnswerEvent(it)
                    if (item.wasDue) {
                        val decision = nextReview(current.stage, it.correct, it.revealed,
                            now, it.hinted)
                        dao.updateReviewState(current.copy(stage = decision.stage,
                            dueAt = decision.due.toEpochMilli(), lastReviewedAt = now.toEpochMilli()))
                    }
                }
            }
            dao.updatePendingSession(session.copy(selected = event.selected,
                hinted = event.hinted, revealed = event.revealed,
                feedback = true, updatedAt = clock.millis()))
            SessionSubmission(event, event.kind == "review")
        }

    suspend fun abandon(id: String) = db.withTransaction {
        dao.pendingSession(id)?.let { dao.updatePendingSession(it.copy(finalized = true)) }
    }

    private suspend fun requireActive(id: String): PendingSession =
        (dao.pendingSession(id) ?: throw IllegalArgumentException("Session unavailable")).also {
            require(!it.finalized) { "Session already finished" }
        }
}
