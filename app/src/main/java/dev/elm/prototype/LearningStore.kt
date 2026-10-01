package dev.elm.prototype

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import java.time.*

/** Prototype v1 tables remain readable after migration as original evidence. */
@Entity data class Completion(@PrimaryKey val lessonId: String, val version: Int, val completedAt: Long, val studyDay: String, val studyZone: String)
@Entity data class Attempt(@PrimaryKey val id: String, val questionId: String, val questionVersion: Int, val selected: Int, val correct: Boolean, val attemptedAt: Long)

@Entity data class LessonCompletion(
    @PrimaryKey val key: String, val lessonId: String, val version: Int,
    val completedAt: Long, val studyDay: String, val studyZone: String, val sessionId: String,
    val courseId: String = "controls-course"
)
@Entity data class AnswerEvent(
    @PrimaryKey val id: String, val lessonId: String, val questionId: String,
    val questionVersion: Int, val sessionId: String, val selected: Int,
    val correct: Boolean, val hinted: Boolean, val revealed: Boolean,
    val answeredAt: Long, val kind: String, val courseId: String = "controls-course"
)
@Entity(primaryKeys = ["courseId", "lessonId"])
data class Bookmark(val lessonId: String, val createdAt: Long, val courseId: String = "controls-course")
@Entity(primaryKeys = ["courseId", "conceptId"])
data class ReviewState(
    val conceptId: String, val lessonId: String, val lessonVersion: Int,
    val questionId: String, val questionVersion: Int, val stage: Int,
    val learnedAt: Long, val dueAt: Long, val lastReviewedAt: Long?,
    val courseId: String = "controls-course"
)
@Entity data class StudySession(
    @PrimaryKey val id: String, val kind: String, val reference: String,
    val completedAt: Long, val studyDay: String, val studyZone: String
)
@Entity data class StudyDayCredit(
    @PrimaryKey val studyDay: String, val firstAt: Long, val studyZone: String
)

@Dao interface LearningDao {
    @Query("SELECT * FROM Completion") fun observeCompletions(): Flow<List<Completion>>
    @Query("SELECT * FROM Completion") suspend fun completions(): List<Completion>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertCompletion(value: Completion): Long
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAttempt(value: Attempt): Long
    @Query("SELECT * FROM Attempt") suspend fun attempts(): List<Attempt>

    @Query("SELECT * FROM LessonCompletion ORDER BY completedAt DESC") fun observeLessonCompletions(): Flow<List<LessonCompletion>>
    @Query("SELECT * FROM LessonCompletion") suspend fun lessonCompletions(): List<LessonCompletion>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertLessonCompletion(value: LessonCompletion): Long
    @Query("SELECT * FROM AnswerEvent ORDER BY answeredAt ASC, id ASC") suspend fun answerEvents(): List<AnswerEvent>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAnswerEvent(value: AnswerEvent): Long
    @Query("SELECT * FROM AnswerEvent WHERE courseId=:courseId AND lessonId=:lessonId AND questionId=:questionId AND questionVersion=:version AND kind='lesson' ORDER BY answeredAt ASC, id ASC LIMIT 1")
    suspend fun firstLessonAttempt(lessonId: String, questionId: String, version: Int,
        courseId: String = "controls-course"): AnswerEvent?
    @Query("SELECT * FROM Bookmark ORDER BY createdAt DESC") fun observeBookmarks(): Flow<List<Bookmark>>
    @Query("SELECT * FROM Bookmark") suspend fun bookmarks(): List<Bookmark>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertBookmark(value: Bookmark): Long
    @Query("DELETE FROM Bookmark WHERE courseId=:courseId AND lessonId=:lessonId")
    suspend fun deleteBookmark(lessonId: String, courseId: String = "controls-course"): Int
    @Query("SELECT * FROM ReviewState ORDER BY dueAt ASC, conceptId ASC")
    fun observeReviewStates(): Flow<List<ReviewState>>
    @Query("SELECT * FROM ReviewState ORDER BY dueAt ASC, conceptId ASC")
    suspend fun reviewStates(): List<ReviewState>
    @Query("SELECT * FROM ReviewState WHERE courseId=:courseId AND conceptId=:conceptId LIMIT 1")
    suspend fun reviewState(conceptId: String, courseId: String = "controls-course"): ReviewState?
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertReviewState(value: ReviewState): Long
    @Update suspend fun updateReviewState(value: ReviewState): Int
    @Query("DELETE FROM ReviewState WHERE courseId=:courseId AND conceptId=:conceptId")
    suspend fun deleteReviewState(courseId: String, conceptId: String): Int
    @Query("SELECT * FROM AnswerEvent WHERE courseId=:courseId AND sessionId=:sessionId")
    suspend fun sessionAnswers(sessionId: String, courseId: String = "controls-course"): List<AnswerEvent>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertStudySession(value: StudySession): Long
    @Query("SELECT * FROM StudySession ORDER BY completedAt ASC") suspend fun studySessions(): List<StudySession>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertStudyDayCredit(value: StudyDayCredit): Long
    @Query("SELECT * FROM StudyDayCredit ORDER BY studyDay ASC") fun observeStudyDays(): Flow<List<StudyDayCredit>>
    @Query("SELECT * FROM StudyDayCredit ORDER BY studyDay ASC") suspend fun studyDays(): List<StudyDayCredit>
}

@Database(entities = [Completion::class, Attempt::class, LessonCompletion::class,
    AnswerEvent::class, Bookmark::class, ReviewState::class,
    StudySession::class, StudyDayCredit::class], version = 5, exportSchema = true)
abstract class LearningDatabase : RoomDatabase() {
    abstract fun learningDao(): LearningDao
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS LessonCompletion (
                    `key` TEXT NOT NULL, lessonId TEXT NOT NULL, version INTEGER NOT NULL,
                    completedAt INTEGER NOT NULL, studyDay TEXT NOT NULL, studyZone TEXT NOT NULL,
                    sessionId TEXT NOT NULL, PRIMARY KEY(`key`))""")
                db.execSQL("""CREATE TABLE IF NOT EXISTS AnswerEvent (
                    id TEXT NOT NULL, lessonId TEXT NOT NULL, questionId TEXT NOT NULL,
                    questionVersion INTEGER NOT NULL, sessionId TEXT NOT NULL,
                    selected INTEGER NOT NULL, correct INTEGER NOT NULL, hinted INTEGER NOT NULL,
                    revealed INTEGER NOT NULL, answeredAt INTEGER NOT NULL, kind TEXT NOT NULL,
                    PRIMARY KEY(id))""")
                db.execSQL("""CREATE TABLE IF NOT EXISTS Bookmark (
                    lessonId TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(lessonId))""")
                db.execSQL("""INSERT OR IGNORE INTO LessonCompletion
                    SELECT lessonId || ':v' || version, lessonId, version, completedAt,
                    studyDay, studyZone, 'prototype-v1' FROM Completion""")
                db.execSQL("""INSERT OR IGNORE INTO AnswerEvent
                    SELECT 'legacy:' || id, 'controls-fixture', questionId, questionVersion,
                    'prototype-v1', selected, correct, 0, 0, attemptedAt, 'lesson'
                    FROM Attempt""")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS ReviewState (
                    conceptId TEXT NOT NULL, lessonId TEXT NOT NULL, lessonVersion INTEGER NOT NULL,
                    questionId TEXT NOT NULL, questionVersion INTEGER NOT NULL, stage INTEGER NOT NULL,
                    learnedAt INTEGER NOT NULL, dueAt INTEGER NOT NULL, lastReviewedAt INTEGER,
                    PRIMARY KEY(conceptId))""")
                // Content-to-concept mapping lives in validated packs, not the v2 database.
                // ReviewRepository seeds existing completed lessons after pack load.
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS StudySession (
                    id TEXT NOT NULL, kind TEXT NOT NULL, reference TEXT NOT NULL,
                    completedAt INTEGER NOT NULL, studyDay TEXT NOT NULL, studyZone TEXT NOT NULL,
                    PRIMARY KEY(id))""")
                db.execSQL("""CREATE TABLE IF NOT EXISTS StudyDayCredit (
                    studyDay TEXT NOT NULL, firstAt INTEGER NOT NULL, studyZone TEXT NOT NULL,
                    PRIMARY KEY(studyDay))""")
                // Completed lessons in installed v3 apps are legitimate earlier study days.
                db.execSQL("""INSERT OR IGNORE INTO StudySession
                    SELECT 'legacy:' || `key`, 'lesson', lessonId, completedAt, studyDay, studyZone
                    FROM LessonCompletion""")
                db.execSQL("""INSERT OR IGNORE INTO StudyDayCredit
                    SELECT studyDay, MIN(completedAt), MIN(studyZone)
                    FROM LessonCompletion GROUP BY studyDay""")
            }
        }
        const val LEGACY_UNATTRIBUTED = "legacy-unattributed"
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // The two bundled fixture lessons are known. Imported v4 pack history cannot
                // be inferred from short IDs, so keep it visible as unattributed evidence.
                val course = "CASE WHEN lessonId IN ('controls-fixture','study-tools-fixture') " +
                    "THEN 'controls-course' ELSE 'legacy-unattributed' END"
                db.execSQL("""CREATE TABLE LessonCompletion_new (`key` TEXT NOT NULL, lessonId TEXT NOT NULL,
                    version INTEGER NOT NULL, completedAt INTEGER NOT NULL, studyDay TEXT NOT NULL,
                    studyZone TEXT NOT NULL, sessionId TEXT NOT NULL, courseId TEXT NOT NULL,
                    PRIMARY KEY(`key`))""")
                db.execSQL("""INSERT INTO LessonCompletion_new SELECT ($course) || ':' || `key`, lessonId, version,
                    completedAt, studyDay, studyZone, sessionId, $course FROM LessonCompletion""")
                db.execSQL("DROP TABLE LessonCompletion")
                db.execSQL("ALTER TABLE LessonCompletion_new RENAME TO LessonCompletion")
                db.execSQL("""CREATE TABLE AnswerEvent_new (id TEXT NOT NULL, lessonId TEXT NOT NULL,
                    questionId TEXT NOT NULL, questionVersion INTEGER NOT NULL, sessionId TEXT NOT NULL,
                    selected INTEGER NOT NULL, correct INTEGER NOT NULL, hinted INTEGER NOT NULL,
                    revealed INTEGER NOT NULL, answeredAt INTEGER NOT NULL, kind TEXT NOT NULL,
                    courseId TEXT NOT NULL, PRIMARY KEY(id))""")
                db.execSQL("""INSERT INTO AnswerEvent_new SELECT id, lessonId, questionId,
                    questionVersion, sessionId, selected, correct, hinted, revealed, answeredAt,
                    kind, $course FROM AnswerEvent""")
                db.execSQL("DROP TABLE AnswerEvent")
                db.execSQL("ALTER TABLE AnswerEvent_new RENAME TO AnswerEvent")
                db.execSQL("""CREATE TABLE Bookmark_new (lessonId TEXT NOT NULL, createdAt INTEGER NOT NULL,
                    courseId TEXT NOT NULL, PRIMARY KEY(courseId, lessonId))""")
                db.execSQL("INSERT INTO Bookmark_new SELECT lessonId, createdAt, $course FROM Bookmark")
                db.execSQL("DROP TABLE Bookmark")
                db.execSQL("ALTER TABLE Bookmark_new RENAME TO Bookmark")
                db.execSQL("""CREATE TABLE ReviewState_new (conceptId TEXT NOT NULL,
                    lessonId TEXT NOT NULL, lessonVersion INTEGER NOT NULL, questionId TEXT NOT NULL,
                    questionVersion INTEGER NOT NULL, stage INTEGER NOT NULL, learnedAt INTEGER NOT NULL,
                    dueAt INTEGER NOT NULL, lastReviewedAt INTEGER, courseId TEXT NOT NULL,
                    PRIMARY KEY(courseId, conceptId))""")
                db.execSQL("""INSERT INTO ReviewState_new SELECT conceptId, lessonId, lessonVersion,
                    questionId, questionVersion, stage, learnedAt, dueAt, lastReviewedAt,
                    $course FROM ReviewState""")
                db.execSQL("DROP TABLE ReviewState")
                db.execSQL("ALTER TABLE ReviewState_new RENAME TO ReviewState")
            }
        }
        fun open(context: Context, name: String = "elm-learning.db") =
            Room.databaseBuilder(context, LearningDatabase::class.java, name)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build()
    }
}

class LearningRepository(private val dao: LearningDao, private val clock: Clock, private val zone: ZoneId) {
    // Keep the v1 entry points for existing evidence and compatibility tests.
    suspend fun finish(lessonId: String) =
        dao.insertCompletion(Completion(lessonId, 1, clock.millis(), studyDay(clock.instant(), zone), zone.id))
    suspend fun answer(question: Int, selected: Int, correct: Boolean) =
        dao.insertAttempt(Attempt("fixture-v1-q$question", "q$question", 1, selected, correct, clock.millis()))

    suspend fun submitLessonAnswer(
        lesson: PackLesson, question: PackQuestion, sessionId: String, selected: Int,
        hinted: Boolean = false, revealed: Boolean = false,
        courseId: String = "controls-course"
    ): Long {
        require(selected in question.choices.indices)
        val id = "$courseId:$sessionId:${lesson.id}:v${lesson.version}:${question.id}:v${question.version}:lesson"
        return dao.insertAnswerEvent(AnswerEvent(id, lesson.id, question.id, question.version,
            sessionId, selected, selected == question.correctIndex, hinted, revealed,
            clock.millis(), "lesson", courseId))
    }
    suspend fun completeLesson(lesson: PackLesson, sessionId: String,
        courseId: String = "controls-course"): Long {
        val instant = clock.instant()
        return dao.insertLessonCompletion(LessonCompletion("$courseId:${lesson.id}:v${lesson.version}",
            lesson.id, lesson.version, instant.toEpochMilli(), studyDay(instant, zone), zone.id,
            sessionId, courseId))
    }
    suspend fun bookmark(lessonId: String, courseId: String = "controls-course"): Long =
        dao.insertBookmark(Bookmark(lessonId, clock.millis(), courseId))
    suspend fun unbookmark(lessonId: String, courseId: String = "controls-course"): Int =
        dao.deleteBookmark(lessonId, courseId)
}

fun studyDay(instant: Instant, zone: ZoneId): String = instant.atZone(zone).toLocalDate().toString()
