package dev.elm.prototype

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

class MainActivity : ComponentActivity() {
    private lateinit var db: LearningDatabase
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = true
        db = LearningDatabase.open(this)
        setContent { AtharApp(db) }
    }
    override fun onDestroy() { super.onDestroy(); db.close() }
}

@Composable
fun AtharApp(db: LearningDatabase) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("prototype", 0) }
    val scope = rememberCoroutineScope()
    val dao = remember(db) { db.learningDao() }
    val zone = remember {
        ZoneId.of(prefs.getString("study-zone", null)
            ?: ZoneId.systemDefault().id.also { prefs.edit().putString("study-zone", it).commit() })
    }
    val repo = remember(dao) { LearningRepository(dao, Clock.systemUTC(), zone) }
    val study = remember(db, zone) { StudyRepository(db, dao, Clock.systemUTC(), zone) }
    val sessions = remember(db) { SessionRepository(db, dao, Clock.systemUTC()) }
    val pendingSessionSnapshot by remember(dao) { dao.observePendingSessions() }
        .collectAsState<List<PendingSession>, List<PendingSession>?>(initial = null)
    val pendingSessions = pendingSessionSnapshot.orEmpty()
    val studyDays by remember(dao) { dao.observeStudyDays() }
        .collectAsState(initial = emptyList())
    val reviewClock = remember { Clock.systemUTC() }
    val reviews = remember(db) { ReviewRepository(db, dao, reviewClock) }
    val contentRepo = remember { ContentRepository(context) }
    val initial = remember { runCatching { contentRepo.load() } }
    var pack by remember { mutableStateOf(initial.getOrNull()) }
    val completionSnapshot by remember(dao) { dao.observeLessonCompletions() }
        .collectAsState<List<LessonCompletion>, List<LessonCompletion>?>(initial = null)
    val completions = completionSnapshot.orEmpty()
    val bookmarks by remember(dao) { dao.observeBookmarks() }
        .collectAsState(initial = emptyList())
    val reviewStates by remember(dao) { dao.observeReviewStates() }
        .collectAsState(initial = emptyList())
    var nowMillis by remember { mutableLongStateOf(reviewClock.millis()) }
    LaunchedEffect(Unit) { while (true) { nowMillis = reviewClock.millis(); delay(60_000) } }
    LaunchedEffect(pack, completionSnapshot) {
        // Observations trigger refresh only; the repository reads completions atomically.
        if (completionSnapshot != null) pack?.let { reviews.ensureSeeded(it) }
    }
    var page by rememberSaveable { mutableStateOf("Today") }
    var direction by rememberSaveable { mutableStateOf(Direction.Garden) }
    var lessonId by rememberSaveable { mutableStateOf("") }
    var questionIndex by rememberSaveable { mutableIntStateOf(0) }
    var selected by rememberSaveable { mutableIntStateOf(-1) }
    var feedback by rememberSaveable { mutableStateOf(false) }
    var sessionId by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var importStatus by remember { mutableStateOf<String?>(null) }
    var reviewBatch by remember { mutableStateOf<List<ReviewState>>(emptyList()) }
    var reviewDueItems by remember { mutableStateOf<List<ReviewState>>(emptyList()) }
    var reviewIndex by remember { mutableIntStateOf(0) }
    var reviewChoice by remember { mutableIntStateOf(-1) }
    var reviewHinted by remember { mutableStateOf(false) }
    var reviewRevealed by remember { mutableStateOf(false) }
    var reviewFeedback by remember { mutableStateOf(false) }
    var reviewWasDue by remember { mutableStateOf(true) }
    var reviewSourceVisible by remember { mutableStateOf(false) }
    var reviewFinished by remember { mutableStateOf(false) }
    var reviewReturnToLesson by remember { mutableStateOf(false) }
    var reviewSessionId by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }
    LaunchedEffect(page, sessionId, reviewSessionId, pendingSessions) {
        if (page == "Quiz") pendingSessions.firstOrNull { it.id == sessionId && it.kind == "lesson" }
            ?.let { saved ->
                lessonId = saved.lessonId
                questionIndex = saved.cursor
                selected = saved.selected
                feedback = saved.feedback
            }
        if (page == "Review") pendingSessions.firstOrNull { it.id == reviewSessionId &&
            it.kind == "review" }?.let { saved ->
            val items = saved.items()
            reviewBatch = items.map { it.reviewState(saved.courseId) }
            reviewDueItems = items.filter { it.wasDue }.map { it.reviewState(saved.courseId) }
            reviewIndex = saved.cursor
            reviewChoice = saved.selected
            reviewHinted = saved.hinted
            reviewRevealed = saved.revealed
            reviewFeedback = saved.feedback
            reviewWasDue = items.getOrNull(saved.cursor)?.wasDue == true
            reviewReturnToLesson = saved.lessonId.isNotBlank()
            if (saved.lessonId.isNotBlank()) lessonId = saved.lessonId
        }
    }
    fun eligibleStates(): List<ReviewState> {
        val current = pack ?: return emptyList()
        return reviewStates.filter { state ->
            state.courseId == current.courseId &&
            current.lessons.any { lesson ->
                lesson.id == state.lessonId && lesson.version == state.lessonVersion &&
                    lesson.questions.any { it.id == state.questionId &&
                        it.version == state.questionVersion && it.conceptId == state.conceptId }
            }
        }
    }
    fun startReviews(related: List<String> = emptyList(), returnToLesson: Boolean = false) {
        val batch = selectReviewBatch(eligibleStates(), Instant.ofEpochMilli(nowMillis), related)
        val current = pack ?: return
        if (batch.isEmpty()) { page = "Review"; return }
        busy = true
        scope.launch {
            try {
                val id = UUID.randomUUID().toString()
                sessions.startReview(current, batch, id,
                    if (returnToLesson) lessonId else "")
                reviewSessionId = id
                reviewBatch = batch
                reviewDueItems = batch.filter { it.dueAt <= nowMillis }
                reviewIndex = 0
                reviewChoice = -1
                reviewHinted = false
                reviewRevealed = false
                reviewFeedback = false
                reviewSourceVisible = false
                reviewFinished = false
                reviewReturnToLesson = returnToLesson
                page = "Review"
            } catch (_: Exception) { error = context.getString(R.string.study_save_failed) }
            finally { busy = false }
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val current = pack
        if (uri != null && current != null) {
            busy = true
            scope.launch {
                try {
                    val next = withContext(Dispatchers.IO) { contentRepo.import(uri, current) }
                    pack = next
                    lessonId = ""
                    reviewBatch = emptyList()
                    reviewDueItems = emptyList()
                    page = "Learn"
                    error = null
                    importStatus = context.getString(R.string.import_success)
                } catch (e: Exception) {
                    importStatus = context.getString(R.string.import_failed,
                        e.message ?: context.getString(R.string.invalid_document))
                } finally { busy = false }
            }
        }
    }
    fun openLesson(lesson: PackLesson) {
        lessonId = lesson.id
        questionIndex = 0
        selected = -1
        feedback = false
        sessionId = UUID.randomUUID().toString()
        page = "Lesson"
    }
    val scroll = rememberScrollState()
    LaunchedEffect(page, lessonId, questionIndex, reviewIndex, reviewFeedback) { scroll.scrollTo(0) }
    BackHandler(page != "Today") {
        page = when (page) {
            "Quiz" -> "Lesson"
            "Lesson" -> "Learn"
            "Review" -> if (reviewReturnToLesson) "Lesson" else "Today"
            else -> "Today"
        }
    }
    ElmTheme(direction) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.safeDrawingPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.brand_name), fontFamily = FontFamily.Serif, fontSize = 32.sp,
                        fontWeight = FontWeight.Bold)
                    TextButton(onClick = { page = "Settings" }) { Text(stringResource(R.string.nav_settings)) }
                }
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                  Column(Modifier.widthIn(max = 640.dp).fillMaxWidth()
                      .padding(horizontal = 24.dp, vertical = 16.dp),
                      verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    val active = pack
                    if (active == null) {
                        Title(stringResource(R.string.app_name))
                        Note(stringResource(R.string.content_error,
                            initial.exceptionOrNull()?.message ?: "invalid bundled pack"))
                    } else {
                        Text(stringResource(if (active.developmentOnly) R.string.development_label
                            else R.string.local_pack_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                        val resolvedLessonId = if (page == "Quiz")
                            pendingSessions.firstOrNull { it.id == sessionId && it.kind == "lesson" }
                                ?.lessonId ?: lessonId else lessonId
                        val lesson = active.lessons.firstOrNull { it.id == resolvedLessonId }
                            ?: active.lessons.first()
                        val eligible = eligibleStates()
                        val dueCount = dueReviewCount(eligible, Instant.ofEpochMilli(nowMillis))
                        val streak = streakStats(studyDays,
                            Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate())
                        val resumable = pendingSessions.firstOrNull { it.courseId == active.courseId }
                        when (page) {
                            "Today" -> {
                                if (resumable != null) Panel {
                                    Text(stringResource(R.string.session_continue_title),
                                        style = MaterialTheme.typography.titleMedium)
                                    Note(stringResource(R.string.session_continue_note))
                                    Action(stringResource(R.string.session_continue_action)) {
                                        if (resumable.kind == "lesson") {
                                            sessionId = resumable.id
                                            lessonId = resumable.lessonId
                                            questionIndex = resumable.cursor
                                            selected = resumable.selected
                                            feedback = resumable.feedback
                                            page = "Quiz"
                                        } else {
                                            reviewSessionId = resumable.id
                                            reviewReturnToLesson = false
                                            page = "Review"
                                        }
                                    }
                                }
                                TodayScreen(active, completions.filter { it.courseId == active.courseId },
                                    dueCount, streak, ::openLesson) {
                                    val pendingReview = pendingSessions.firstOrNull {
                                        it.courseId == active.courseId && it.kind == "review"
                                    }
                                    if (pendingReview != null) {
                                        reviewSessionId = pendingReview.id
                                        page = "Review"
                                    } else startReviews()
                                }
                            }
                            "Learn" -> CourseScreen(active, completions.filter { it.courseId == active.courseId },
                                bookmarks.filter { it.courseId == active.courseId }, ::openLesson)
                            "Lesson" -> LessonScreen(active, lesson,
                                bookmarks.any { it.courseId == active.courseId && it.lessonId == lesson.id }, busy,
                                lesson.prerequisiteConceptIds.count { concept ->
                                    eligible.any { it.conceptId == concept }
                                },
                                onBookmark = {
                                    busy = true
                                    scope.launch {
                                        try {
                                            if (bookmarks.any { it.courseId == active.courseId && it.lessonId == lesson.id })
                                                repo.unbookmark(lesson.id, active.courseId)
                                            else repo.bookmark(lesson.id, active.courseId)
                                        } catch (_: Exception) {
                                            error = context.getString(R.string.bookmark_failed)
                                        } finally { busy = false }
                                    }
                                },
                                onQuiz = {
                                    val pendingLesson = pendingSessions.firstOrNull {
                                        it.courseId == active.courseId && it.kind == "lesson" &&
                                            it.lessonId == lesson.id && it.lessonVersion == lesson.version
                                    }
                                    if (pendingLesson != null) {
                                        sessionId = pendingLesson.id
                                        questionIndex = pendingLesson.cursor
                                        selected = pendingLesson.selected
                                        feedback = pendingLesson.feedback
                                        page = "Quiz"
                                    } else {
                                        busy = true
                                        scope.launch {
                                            try {
                                                val id = UUID.randomUUID().toString()
                                                sessions.startLesson(active, lesson, id)
                                                sessionId = id
                                                questionIndex = 0; selected = -1; feedback = false
                                                page = "Quiz"
                                            } catch (_: Exception) {
                                                error = context.getString(R.string.study_save_failed)
                                            } finally { busy = false }
                                        }
                                    }
                                },
                                onQuickRecall = {
                                    startReviews(lesson.prerequisiteConceptIds, true)
                                })
                            "Quiz" -> if (pendingSessionSnapshot == null) {
                                Title(stringResource(R.string.session_loading_title))
                            } else if (pendingSessions.firstOrNull { it.id == sessionId && it.kind == "lesson" }
                                ?.let { saved -> saved.courseId != active.courseId ||
                                    saved.lessonId != lesson.id || saved.lessonVersion != lesson.version ||
                                    saved.cursor !in lesson.questions.indices ||
                                    saved.items().size != lesson.questions.size ||
                                    saved.items().withIndex().any { (index, item) ->
                                        val question = lesson.questions[index]
                                        item.questionId != question.id ||
                                            item.questionVersion != question.version ||
                                            item.conceptId != question.conceptId
                                    } } != false) {
                                Title(stringResource(R.string.session_unavailable_title))
                                Note(stringResource(R.string.session_unavailable_note))
                                Action(stringResource(R.string.session_close_action)) {
                                    scope.launch { sessions.abandon(sessionId); page = "Today" }
                                }
                            } else {
                                val saved = pendingSessions.first { it.id == sessionId && it.kind == "lesson" }
                                var choice by remember(saved.id, saved.cursor) {
                                    mutableIntStateOf(saved.selected)
                                }
                                QuizScreen(active, lesson, saved.cursor,
                                choice, saved.feedback, busy,
                                onSelect = {
                                    choice = it
                                    scope.launch { runCatching { sessions.updateInput(sessionId, selected = it) } }
                                },
                                onCheck = {
                                    busy = true
                                    val question = lesson.questions[saved.cursor]
                                    scope.launch {
                                        try {
                                            sessions.updateInput(sessionId, selected = choice)
                                            val result = sessions.submitLesson(sessionId, lesson, question)
                                            choice = result.event.selected
                                            feedback = true
                                            error = null
                                        } catch (_: Exception) {
                                            error = context.getString(R.string.answer_save_failed)
                                        } finally { busy = false }
                                    }
                                },
                                onNext = {
                                    busy = true
                                    scope.launch {
                                        try {
                                            val next = sessions.advance(sessionId)
                                            questionIndex = next.cursor
                                            selected = -1
                                            feedback = false
                                        } catch (_: Exception) {
                                            error = context.getString(R.string.study_save_failed)
                                        } finally { busy = false }
                                    }
                                },
                                onFinish = {
                                    busy = true
                                    scope.launch {
                                        try {
                                            study.completeLesson(lesson, sessionId, active.courseId)
                                            page = "Done"
                                            error = null
                                        } catch (_: Exception) {
                                            error = context.getString(R.string.completion_failed)
                                        } finally { busy = false }
                                    }
                                })
                            }
                            "Done" -> DoneScreen(streak) { page = "Progress" }
                            "Review" -> {
                                val savedReview = pendingSessions.firstOrNull {
                                    it.id == reviewSessionId && it.kind == "review"
                                }
                                if (pendingSessionSnapshot == null ||
                                    (savedReview != null && (reviewBatch.map { it.conceptId } !=
                                        savedReview.items().map { it.conceptId } ||
                                        reviewIndex != savedReview.cursor))) {
                                    Title(stringResource(R.string.session_loading_title))
                                } else if (reviewBatch.isEmpty() || reviewIndex >= reviewBatch.size) {
                                    ReviewOverview(dueCount, reviewFinished) { startReviews() }
                                } else {
                                    val state = reviewBatch[reviewIndex]
                                    val sourceLesson = active.lessons.firstOrNull {
                                        it.id == state.lessonId && it.version == state.lessonVersion
                                    }
                                    val sourceQuestion = sourceLesson?.questions?.firstOrNull {
                                        it.id == state.questionId && it.version == state.questionVersion &&
                                            it.conceptId == state.conceptId
                                    }
                                    val currentSource = eligible.firstOrNull { it.conceptId == state.conceptId &&
                                        it.courseId == state.courseId }
                                    val sourceChanged = !reviewFeedback && (currentSource == null ||
                                        currentSource.lessonId != state.lessonId ||
                                        currentSource.lessonVersion != state.lessonVersion ||
                                        currentSource.questionId != state.questionId ||
                                        currentSource.questionVersion != state.questionVersion ||
                                        currentSource.stage != state.stage || currentSource.dueAt != state.dueAt)
                                    fun advanceReview() {
                                        if (reviewIndex + 1 < reviewBatch.size) {
                                            busy = true
                                            scope.launch {
                                                try {
                                                    val next = sessions.advance(reviewSessionId)
                                                    reviewIndex = next.cursor
                                                    reviewChoice = -1
                                                    reviewHinted = false
                                                    reviewRevealed = false
                                                    reviewFeedback = false
                                                    reviewSourceVisible = false
                                                } catch (_: Exception) {
                                                    error = context.getString(R.string.study_save_failed)
                                                } finally { busy = false }
                                            }
                                        } else {
                                            busy = true
                                            scope.launch {
                                                try {
                                                    if (reviewDueItems.isNotEmpty())
                                                        study.completeDueReviewBatch(reviewDueItems, reviewSessionId,
                                                            active.courseId)
                                                    else sessions.abandon(reviewSessionId)
                                                    reviewBatch = emptyList()
                                                    reviewDueItems = emptyList()
                                                    reviewFinished = true
                                                    error = null
                                                    if (reviewReturnToLesson) page = "Lesson"
                                                } catch (_: Exception) {
                                                    error = context.getString(R.string.study_save_failed)
                                                } finally { busy = false }
                                            }
                                        }
                                    }
                                    if (sourceLesson == null || sourceQuestion == null || sourceChanged) {
                                        Title(stringResource(R.string.session_unavailable_title))
                                        Note(stringResource(R.string.session_unavailable_note))
                                        Action(stringResource(R.string.session_close_action)) {
                                            scope.launch {
                                                sessions.abandon(reviewSessionId)
                                                reviewBatch = emptyList()
                                                page = "Today"
                                            }
                                        }
                                    } else {
                                        ReviewQuestionScreen(active, sourceLesson, sourceQuestion,
                                            reviewIndex, reviewBatch.size, reviewChoice, reviewHinted,
                                            reviewRevealed, reviewFeedback, busy, reviewWasDue,
                                            reviewSourceVisible,
                                            onSelect = {
                                                reviewChoice = it
                                                scope.launch { runCatching {
                                                    sessions.updateInput(reviewSessionId, selected = it)
                                                } }
                                            },
                                            onHint = {
                                                reviewHinted = true
                                                scope.launch { runCatching {
                                                    sessions.updateInput(reviewSessionId, hinted = true)
                                                } }
                                            },
                                            onReveal = {
                                                reviewRevealed = true; reviewChoice = -1
                                                scope.launch { runCatching {
                                                    sessions.updateInput(reviewSessionId,
                                                        selected = -1, revealed = true)
                                                } }
                                            },
                                            onSubmit = {
                                                busy = true
                                                scope.launch {
                                                    try {
                                                        sessions.updateInput(reviewSessionId,
                                                            selected = reviewChoice,
                                                            hinted = reviewHinted,
                                                            revealed = reviewRevealed)
                                                        val result = sessions.submitReview(reviewSessionId,
                                                            sourceQuestion)
                                                        reviewChoice = result.event.selected
                                                        reviewHinted = result.event.hinted
                                                        reviewRevealed = result.event.revealed
                                                        reviewWasDue = result.wasDue
                                                        reviewFeedback = true
                                                        error = null
                                                    } catch (_: Exception) {
                                                        error = context.getString(R.string.review_error)
                                                    } finally { busy = false }
                                                }
                                            },
                                            onSource = { reviewSourceVisible = !reviewSourceVisible },
                                            onNext = ::advanceReview,
                                            onOpenLesson = {
                                                lessonId = sourceLesson.id
                                                page = "Lesson"
                                            })
                                    }
                                }
                            }
                            "Progress" -> ProgressScreen(active,
                                completions.filter { it.courseId == active.courseId },
                                bookmarks.filter { it.courseId == active.courseId }, streak)
                            "Settings" -> SettingsScreen(active, direction, importStatus,
                                onDirection = {
                                    direction = if (direction == Direction.Garden) Direction.Editorial
                                    else Direction.Garden
                                },
                                onImport = { if (!busy) importer.launch(arrayOf("*/*")) },
                                onHome = { page = "Today" })
                        }
                    }
                  }
                }
                val destinationPage = when (page) {
                    "Lesson", "Quiz", "Done" -> "Learn"
                    else -> page
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Today", "Learn", "Review", "Progress").forEach { destination ->
                        val label = when (destination) {
                            "Today" -> R.string.nav_today
                            "Learn" -> R.string.nav_learn
                            "Review" -> R.string.nav_review
                            else -> R.string.nav_progress
                        }
                        val activeDestination = destinationPage == destination
                        TextButton(onClick = { page = destination },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = if (activeDestination)
                                    MaterialTheme.colorScheme.secondaryContainer else Color.Transparent),
                            contentPadding = PaddingValues(horizontal = 2.dp),
                            modifier = Modifier.weight(1f).heightIn(min = 52.dp)
                                .semantics { this.selected = activeDestination }) {
                            Text(stringResource(label), maxLines = 1, softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}
