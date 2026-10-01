package dev.elm.prototype

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.*

@Entity data class Completion(@PrimaryKey val lessonId: String, val version: Int, val completedAt: Long, val studyDay: String, val studyZone: String)
@Entity data class Attempt(@PrimaryKey val id: String, val questionId: String, val questionVersion: Int, val selected: Int, val correct: Boolean, val attemptedAt: Long)
@Dao interface LearningDao {
 @Query("SELECT * FROM Completion") fun observeCompletions(): Flow<List<Completion>>
 @Query("SELECT * FROM Completion") suspend fun completions(): List<Completion>
 @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertCompletion(value: Completion): Long
 @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAttempt(value: Attempt): Long
 @Query("SELECT * FROM Attempt") suspend fun attempts(): List<Attempt>
}
@Database(entities = [Completion::class, Attempt::class], version = 1, exportSchema = false)
abstract class LearningDatabase : RoomDatabase() {
 abstract fun learningDao(): LearningDao
 companion object { fun open(context: Context, name: String = "elm-learning.db") = Room.databaseBuilder(context, LearningDatabase::class.java, name).build() }
}
class LearningRepository(private val dao: LearningDao, private val clock: Clock, private val zone: ZoneId) {
 suspend fun finish(lessonId: String) = dao.insertCompletion(Completion(lessonId, 1, clock.millis(), studyDay(clock.instant(), zone), zone.id))
 suspend fun answer(question: Int, selected: Int, correct: Boolean) = dao.insertAttempt(Attempt("fixture-v1-q$question", "q$question", 1, selected, correct, clock.millis()))
}
fun studyDay(instant: Instant, zone: ZoneId): String = instant.atZone(zone).toLocalDate().toString()
// Proposed defaults only; integration belongs to milestone 2. A reveal cannot advance recall stage.
data class ReviewDecision(val stage: Int, val due: Instant)
fun nextReview(stage: Int, correct: Boolean, revealed: Boolean, now: Instant): ReviewDecision {
 val intervals = listOf(1L, 3L, 7L, 14L, 30L)
 val next = if (!correct || revealed) 0 else (stage + 1).coerceAtMost(4)
 return ReviewDecision(next, now.plusSeconds(intervals[next] * 86400))
}
