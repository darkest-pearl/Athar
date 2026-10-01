package dev.elm.prototype

import androidx.room.withTransaction
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

data class StreakStats(val current: Int, val longest: Int, val totalDays: Int)

/** Yesterday still counts until the end of today; a missed day never deletes history. */
fun streakStats(days: List<StudyDayCredit>, today: LocalDate): StreakStats {
    val dates = days.map { LocalDate.parse(it.studyDay) }.distinct().sorted()
    if (dates.isEmpty()) return StreakStats(0, 0, 0)
    var longest = 0
    var run = 0
    var previous: LocalDate? = null
    dates.forEach { date ->
        run = if (previous?.plusDays(1) == date) run + 1 else 1
        longest = maxOf(longest, run)
        previous = date
    }
    val latest = dates.lastOrNull { it <= today } ?: return StreakStats(0, longest, dates.size)
    var current = 0
    if (latest == today || latest == today.minusDays(1)) {
        for (index in dates.indices.reversed()) {
            if (dates[index] == latest.minusDays(current.toLong())) current++ else break
        }
    }
    return StreakStats(current, longest, dates.size)
}

class StudyRepository(private val db: LearningDatabase, private val dao: LearningDao,
    private val clock: Clock, private val zone: ZoneId) {
    /** Every question must have saved feedback in this session; score is irrelevant to credit. */
    suspend fun completeLesson(lesson: PackLesson, sessionId: String): Boolean = db.withTransaction {
        val events = dao.sessionAnswers(sessionId).filter { it.kind == "lesson" &&
            it.lessonId == lesson.id }
        require(lesson.questions.all { question -> events.any {
            it.questionId == question.id && it.questionVersion == question.version
        } }) { "Lesson feedback is incomplete" }
        val now = clock.instant()
        val day = studyDay(now, zone)
        dao.insertLessonCompletion(LessonCompletion("${lesson.id}:v${lesson.version}", lesson.id,
            lesson.version, now.toEpochMilli(), day, zone.id, sessionId))
        val inserted = dao.insertStudySession(StudySession("lesson:$sessionId", "lesson",
            "${lesson.id}:v${lesson.version}", now.toEpochMilli(), day, zone.id)) != -1L
        if (inserted) dao.insertStudyDayCredit(StudyDayCredit(day, now.toEpochMilli(), zone.id))
        inserted
    }

    /** Only a finished, nonempty due batch earns a study session. Assisted answers still count. */
    suspend fun completeDueReviewBatch(dueItems: List<ReviewState>, sessionId: String): Boolean =
        db.withTransaction {
            require(dueItems.isNotEmpty()) { "No due questions in this batch" }
            val events = dao.sessionAnswers(sessionId).filter { it.kind == "review" }
            require(dueItems.all { state -> events.any {
                it.lessonId == state.lessonId && it.questionId == state.questionId &&
                    it.questionVersion == state.questionVersion
            } }) { "Review feedback is incomplete" }
            val now = clock.instant()
            val day = studyDay(now, zone)
            val inserted = dao.insertStudySession(StudySession("review:$sessionId", "review",
                dueItems.joinToString(",") { it.conceptId }, now.toEpochMilli(), day, zone.id)) != -1L
            if (inserted) dao.insertStudyDayCredit(StudyDayCredit(day, now.toEpochMilli(), zone.id))
            inserted
        }
}
