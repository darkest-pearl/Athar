package dev.elm.prototype

import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject
import java.time.Clock
import kotlin.random.Random

/** Activity state is stored with the session, so rotation and process death keep the same choices. */
data class ConceptActivityState(
    val id: String, val kind: String, val reference: String, val choiceOrder: List<Int>,
    val firstSelected: Int = -2, val firstCorrect: Boolean = false,
    val corrected: Boolean = false,
    val sourceViewed: Boolean = false, val usedHint: Boolean = false
)

fun PendingSession.conceptActivities(): List<ConceptActivityState> {
    require(kind == "concept") { "Not a concept session" }
    val array = JSONArray(itemsJson)
    return (0 until array.length()).map { index ->
        val item = array.getJSONObject(index)
        val order = item.getJSONArray("choiceOrder")
        ConceptActivityState(item.getString("id"), item.getString("kind"),
            item.getString("reference"), (0 until order.length()).map(order::getInt),
            item.optInt("firstSelected", -2), item.optBoolean("firstCorrect"),
            item.optBoolean("corrected"),
            item.optBoolean("sourceViewed"), item.optBoolean("usedHint"))
    }
}

private fun encodeConceptActivities(items: List<ConceptActivityState>): String =
    JSONArray().apply {
        items.forEach { item ->
            put(JSONObject().apply {
                put("id", item.id)
                put("kind", item.kind)
                put("reference", item.reference)
                put("choiceOrder", JSONArray(item.choiceOrder))
                put("firstSelected", item.firstSelected)
                put("firstCorrect", item.firstCorrect)
                put("corrected", item.corrected)
                put("sourceViewed", item.sourceViewed)
                put("usedHint", item.usedHint)
            })
        }
    }.toString()

fun conceptSessionMatches(session: PendingSession, pack: ContentPack, lesson: PackLesson): Boolean {
    if (session.kind != "concept" || session.courseId != pack.courseId ||
        session.contentVersion != pack.contentVersion || session.lessonId != lesson.id ||
        session.lessonVersion != lesson.version || session.finalized) return false
    return runCatching {
        val saved = session.conceptActivities()
        saved.size == lesson.activities.size && session.cursor in saved.indices &&
            saved.zip(lesson.activities).all { (item, authored) ->
                item.id == authored.id && item.kind == authored.kind &&
                    item.reference == authored.reference &&
                    (if (item.kind == "choice") {
                        val size = lesson.questions.first { it.id == item.reference }.choices.size
                        item.choiceOrder.sorted() == (0 until size).toList()
                    } else item.choiceOrder.isEmpty())
            }
    }.getOrDefault(false)
}

/** Uses the existing Room session table; legacy audio sessions retain their original format. */
class ConceptSessionRepository(private val db: LearningDatabase, private val dao: LearningDao,
    private val clock: Clock) {
    suspend fun start(pack: ContentPack, lesson: PackLesson, id: String): PendingSession {
        require(lesson.activities.isNotEmpty())
        val random = Random(id.hashCode())
        val items = lesson.activities.map { activity ->
            val order = if (activity.kind == "choice") {
                val question = lesson.questions.first { it.id == activity.reference }
                question.choices.indices.shuffled(random)
            } else emptyList()
            ConceptActivityState(activity.id, activity.kind, activity.reference, order)
        }
        val now = clock.millis()
        val session = PendingSession(id, pack.courseId, "concept", pack.contentVersion,
            lesson.id, lesson.version, encodeConceptActivities(items), 0, -1,
            false, false, false, false, now, now)
        dao.insertPendingSession(session)
        return dao.pendingSession(id)!!
    }

    suspend fun select(id: String, presentedIndex: Int): PendingSession = change(id) { old, items ->
        val current = items[old.cursor]
        require(current.kind == "choice" && presentedIndex in current.choiceOrder.indices)
        require(!old.feedback || (current.firstSelected >= -1 &&
            !current.firstCorrect && !current.corrected))
        old.copy(selected = presentedIndex)
    }

    suspend fun useHint(id: String): PendingSession = change(id) { old, items ->
        require(items[old.cursor].kind == "choice" && !old.feedback)
        old.copy(hinted = true,
            itemsJson = encodeConceptActivities(items.replaceAt(old.cursor) {
                it.copy(usedHint = true)
            }))
    }

    suspend fun viewSource(id: String): PendingSession = change(id) { old, items ->
        old.copy(itemsJson = encodeConceptActivities(items.replaceAt(old.cursor) {
            it.copy(sourceViewed = true)
        }))
    }

    /** First response is immutable. A corrected response is a separate event. */
    suspend fun check(id: String, pack: ContentPack, lesson: PackLesson): PendingSession =
        db.withTransaction {
            val old = active(id, pack, lesson)
            val items = old.conceptActivities()
            val item = items[old.cursor]
            require(item.kind == "choice")
            val question = lesson.questions.first { it.id == item.reference }
            val chosen = item.choiceOrder.getOrNull(old.selected)
                ?: throw IllegalArgumentException("Select an answer")
            val eventId = "concept:${old.courseId}:$id:${item.id}:first"
            val existing = dao.answerEvent(eventId)
            val next = if (existing == null) {
                val event = AnswerEvent(eventId, lesson.id, question.id, question.version,
                    id, chosen, chosen == question.correctIndex, old.hinted, old.revealed,
                    clock.millis(), "lesson", old.courseId)
                dao.insertAnswerEvent(event)
                old.copy(feedback = true,
                    selected = if (event.correct) old.selected else -1,
                    itemsJson = encodeConceptActivities(items.replaceAt(old.cursor) {
                        it.copy(firstSelected = chosen,
                            firstCorrect = chosen == question.correctIndex,
                            usedHint = old.hinted)
                    }))
            } else {
                require(item.firstSelected >= -1)
                require(!item.corrected && !existing.correct) { "Already answered" }
                require(chosen == question.correctIndex) { "Choose the explained answer" }
                dao.insertAnswerEvent(AnswerEvent(
                    "concept:${old.courseId}:$id:${item.id}:correction", lesson.id,
                    question.id, question.version, id, chosen, true, true, true,
                    clock.millis(), "correction", old.courseId))
                old.copy(itemsJson = encodeConceptActivities(items.replaceAt(old.cursor) {
                    it.copy(corrected = true)
                }))
            }
            next.copy(updatedAt = clock.millis()).also { dao.updatePendingSession(it) }
        }

    suspend fun reveal(id: String, pack: ContentPack, lesson: PackLesson): PendingSession =
        db.withTransaction {
            val old = active(id, pack, lesson)
            require(!old.feedback)
            val items = old.conceptActivities()
            val item = items[old.cursor]
            require(item.kind == "choice")
            val question = lesson.questions.first { it.id == item.reference }
            dao.insertAnswerEvent(AnswerEvent(
                "concept:${old.courseId}:$id:${item.id}:first", lesson.id, question.id,
                question.version, id, -1, false, old.hinted, true, clock.millis(),
                "lesson", old.courseId))
            old.copy(feedback = true, revealed = true, selected = -1,
                itemsJson = encodeConceptActivities(items.replaceAt(old.cursor) {
                    it.copy(firstSelected = -1, usedHint = old.hinted)
                }), updatedAt = clock.millis()).also { dao.updatePendingSession(it) }
        }

    suspend fun advance(id: String, pack: ContentPack, lesson: PackLesson): PendingSession =
        db.withTransaction {
            val old = active(id, pack, lesson)
            val items = old.conceptActivities()
            require(old.cursor + 1 < items.size)
            val current = items[old.cursor]
            if (current.kind == "choice") {
                require(old.feedback)
                require(current.firstCorrect || current.corrected) {
                    "Correct the answer before continuing"
                }
            }
            old.copy(cursor = old.cursor + 1, selected = -1, hinted = false,
                revealed = false, feedback = false, updatedAt = clock.millis())
                .also { dao.updatePendingSession(it) }
        }

    private suspend fun active(id: String, pack: ContentPack, lesson: PackLesson): PendingSession =
        (dao.pendingSession(id) ?: throw IllegalArgumentException("Session unavailable")).also {
            require(conceptSessionMatches(it, pack, lesson)) { "Session material changed" }
        }

    private suspend fun change(id: String,
        transform: (PendingSession, List<ConceptActivityState>) -> PendingSession): PendingSession =
        db.withTransaction {
            val old = dao.pendingSession(id) ?: throw IllegalArgumentException("Session unavailable")
            require(old.kind == "concept" && !old.finalized)
            transform(old, old.conceptActivities()).copy(updatedAt = clock.millis())
                .also { dao.updatePendingSession(it) }
        }
}

private fun <T> List<T>.replaceAt(index: Int, transform: (T) -> T): List<T> =
    mapIndexed { at, item -> if (at == index) transform(item) else item }
