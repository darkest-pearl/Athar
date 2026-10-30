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
import androidx.compose.ui.platform.LocalConfiguration
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
import kotlinx.coroutines.flow.collect
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
    val conceptSessions = remember(db) { ConceptSessionRepository(db, dao, Clock.systemUTC()) }
    val journeysRepo = remember(db) { JourneyRepository(db, dao, Clock.systemUTC()) }
    val journeySnapshot by remember(dao) { dao.observeLessonJourneys() }
        .collectAsState<List<LessonJourney>, List<LessonJourney>?>(initial = null)
    val journeys = journeySnapshot.orEmpty()
    val pendingSessionSnapshot by remember(dao) { dao.observePendingSessions() }
        .collectAsState<List<PendingSession>, List<PendingSession>?>(initial = null)
    val pendingSessions = pendingSessionSnapshot.orEmpty()
    val studyDaySnapshot by remember(dao) { dao.observeStudyDays() }
        .collectAsState<List<StudyDayCredit>, List<StudyDayCredit>?>(initial = null)
    val studyDays = studyDaySnapshot.orEmpty()
    val reviewClock = remember { Clock.systemUTC() }
    val reviews = remember(db) { ReviewRepository(db, dao, reviewClock) }
    val contentRepo = remember { ContentRepository(context) }
    val initial = remember { runCatching { contentRepo.load() } }
    var pack by remember { mutableStateOf(initial.getOrNull()) }
    val completionSnapshot by remember(dao) { dao.observeLessonCompletions() }
        .collectAsState<List<LessonCompletion>, List<LessonCompletion>?>(initial = null)
    val completions = completionSnapshot.orEmpty()
    val bookmarkSnapshot by remember(dao) { dao.observeBookmarks() }
        .collectAsState<List<Bookmark>, List<Bookmark>?>(initial = null)
    val bookmarks = bookmarkSnapshot.orEmpty()
    val reviewSnapshot by remember(dao) { dao.observeReviewStates() }
        .collectAsState<List<ReviewState>, List<ReviewState>?>(initial = null)
    val reviewStates = reviewSnapshot.orEmpty()
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
        if (reviewSnapshot == null || pendingSessionSnapshot == null) {
            page = "Review"
            return
        }
        if (related.isEmpty() && !returnToLesson) {
            val pending = pendingSessions.firstOrNull { it.courseId == pack?.courseId &&
                it.kind == "review" }
            if (pending != null) {
                reviewSessionId = pending.id
                reviewReturnToLesson = false
                page = "Review"
                return
            }
        }
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
        val current = pack ?: return
        busy = true
        scope.launch {
            try {
                val completed = completions.any { it.courseId == current.courseId &&
                    it.lessonId == lesson.id && it.version == lesson.version }
                val pending = pendingSessions.firstOrNull { it.courseId == current.courseId &&
                    it.lessonId == lesson.id && it.lessonVersion == lesson.version &&
                    it.kind == if (lesson.activities.isEmpty()) "lesson" else "concept" }
                journeysRepo.begin(current.courseId, lesson, repeat = completed && pending == null)
                lessonId = lesson.id
                questionIndex = 0
                selected = -1
                feedback = false
                if (lesson.activities.isNotEmpty()) {
                    sessionId = if (pending != null && conceptSessionMatches(pending, current, lesson))
                        pending.id else UUID.randomUUID().toString().also {
                            conceptSessions.start(current, lesson, it)
                        }
                    page = "Concept"
                } else {
                    sessionId = UUID.randomUUID().toString()
                    page = "Lesson"
                }
                error = null
            } catch (_: Exception) { error = context.getString(R.string.study_save_failed) }
            finally { busy = false }
        }
    }
    val scroll = rememberScrollState()
    // Keep Path's position across tabs, lessons, and rotation.
    // A different course or authored version starts at its own beginning.
    val pathScroll = rememberScrollState()
    var pathPosition by rememberSaveable(pack?.courseId, pack?.contentVersion) {
        mutableIntStateOf(0)
    }
    val orientation = LocalConfiguration.current.orientation
    val pathReady = completionSnapshot != null && journeySnapshot != null &&
        reviewSnapshot != null && bookmarkSnapshot != null
    LaunchedEffect(page, pathScroll) {
        if (page == "Learn") snapshotFlow {
            pathScroll.value to pathScroll.isScrollInProgress
        }.collect { (position, moving) ->
            if (moving) pathPosition = position
        }
    }
    LaunchedEffect(page, orientation, pathReady, pack?.courseId, pack?.contentVersion) {
        if (page == "Learn" && pathReady) {
            withFrameNanos { }
            withFrameNanos { }
            pathScroll.scrollTo(pathPosition)
        }
    }
    val currentLesson = pack?.lessons?.firstOrNull { it.id == lessonId }
    val currentStage = if (currentLesson != null && pendingSessions.any {
            it.kind == "lesson" && it.courseId == pack?.courseId &&
                it.lessonId == currentLesson.id && it.lessonVersion == currentLesson.version
        }) LessonStage.Practice else journeys.firstOrNull {
            it.courseId == pack?.courseId && it.lessonId == lessonId &&
                it.lessonVersion == currentLesson?.version
        }?.stageValue() ?: LessonStage.Intro
    LaunchedEffect(page, lessonId, currentStage, questionIndex, reviewIndex, reviewFeedback) {
        if (page != "Learn") scroll.scrollTo(0)
    }
    BackHandler(page != "Today") {
        page = when (page) {
            "Concept" -> "Learn"
            "Quiz" -> "Lesson"
            "Lesson" -> if (currentLesson != null && currentStage in
                listOf(LessonStage.Takeaway, LessonStage.Listen)) {
                val previous = if (currentStage == LessonStage.Takeaway && currentLesson.segmentRef.isNotEmpty())
                    LessonStage.Listen else LessonStage.Intro
                scope.launch { try {
                    journeysRepo.setStage(pack!!.courseId, currentLesson, previous)
                } catch (_: Exception) { error = context.getString(R.string.study_save_failed) } }
                "Lesson"
            } else "Learn"
            "Review" -> if (reviewReturnToLesson) "Lesson" else "Today"
            else -> "Today"
        }
    }
    ElmTheme(direction) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.safeDrawingPadding()) {
                val focusedStudy = page == "Lesson" || page == "Quiz" ||
                    page == "Concept" || page == "Done"
                val compactHeight = LocalConfiguration.current.screenHeightDp < 500
                val largeText = androidx.compose.ui.platform.LocalDensity.current.fontScale >= 1.3f
                if (!focusedStudy) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp,
                    vertical = 0.dp),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.brand_name), fontFamily = FontFamily.Serif,
                        fontSize = if (compactHeight) 22.sp else 26.sp,
                        fontWeight = FontWeight.Bold)
                    TextButton(onClick = { page = "Settings" }) { Text(stringResource(R.string.nav_settings)) }
                }
                }
                Column(if (focusedStudy) Modifier.weight(1f).fillMaxWidth()
                    else Modifier.weight(1f).fillMaxWidth().verticalScroll(
                        if (page == "Learn") pathScroll else scroll),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                  Column(Modifier.widthIn(max = 640.dp).fillMaxWidth()
                          .then(if (focusedStudy) Modifier.fillMaxHeight()
                          else Modifier.padding(horizontal = 24.dp,
                              vertical = if (compactHeight) 4.dp else 8.dp)),
                      verticalArrangement = if (focusedStudy) Arrangement.Top
                          else Arrangement.spacedBy(if (compactHeight) 8.dp else 12.dp)) {
                    val active = pack
                    if (active == null) {
                        Title(stringResource(R.string.app_name))
                        Note(stringResource(R.string.content_error,
                            initial.exceptionOrNull()?.message ?: "invalid bundled pack"))
                    } else {
                        if (!focusedStudy) Text(stringResource(if (active.developmentOnly) R.string.development_label
                            else R.string.local_pack_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                        if (!focusedStudy && error != null) Text(error!!, color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                        val resolvedLessonId = if (page == "Quiz" || page == "Concept")
                            pendingSessions.firstOrNull { it.id == sessionId &&
                                it.kind == if (page == "Concept") "concept" else "lesson" }
                                ?.lessonId ?: lessonId else lessonId
                        val lesson = active.lessons.firstOrNull { it.id == resolvedLessonId }
                            ?: active.lessons.first()
                        val eligible = eligibleStates()
                        val dueCount = dueReviewCount(eligible, Instant.ofEpochMilli(nowMillis))
                        val streak = streakStats(studyDays,
                            Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate())
                        val studiedToday = studyDays.any { it.studyDay ==
                            Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate().toString() }
                        val localCompletions = completions.filter { it.courseId == active.courseId }
                        val resumable = pendingSessions.firstOrNull { it.courseId == active.courseId }
                        when (page) {
                            "Today" -> {
                                if (completionSnapshot == null || pendingSessionSnapshot == null ||
                                    journeySnapshot == null || studyDaySnapshot == null ||
                                    reviewSnapshot == null || bookmarkSnapshot == null) {
                                    Title(stringResource(R.string.session_loading_title))
                                } else {
                                TodayScreen(active, localCompletions,
                                    journeys.filter { it.courseId == active.courseId },
                                    dueCount, streak, studiedToday, ::openLesson,
                                    openPath = { page = "Learn" }, openReviews = {
                                    val pendingReview = pendingSessions.firstOrNull {
                                        it.courseId == active.courseId && it.kind == "review"
                                    }
                                    if (pendingReview != null) {
                                        reviewSessionId = pendingReview.id
                                        page = "Review"
                                    } else startReviews()
                                }, resumeLesson = if (resumable?.kind == "concept" &&
                                    active.lessons.any { it.id == resumable.lessonId &&
                                        it.version == resumable.lessonVersion &&
                                        conceptSessionMatches(resumable, active, it) }) {{
                                    sessionId = resumable.id
                                    lessonId = resumable.lessonId
                                    page = "Concept"
                                }} else if (resumable?.kind == "lesson") {{
                                    sessionId = resumable.id
                                    lessonId = resumable.lessonId
                                    questionIndex = resumable.cursor
                                    selected = resumable.selected
                                    feedback = resumable.feedback
                                    page = "Quiz"
                                }} else null)
                                }
                            }
                            "Learn" -> if (completionSnapshot == null || journeySnapshot == null ||
                                reviewSnapshot == null || bookmarkSnapshot == null) {
                                Title(stringResource(R.string.session_loading_title))
                            } else CourseScreen(active, localCompletions,
                                bookmarks.filter { it.courseId == active.courseId },
                                journeys.filter { it.courseId == active.courseId },
                                dueCount, ::openLesson) { startReviews() }
                            "Concept" -> {
                                val saved = pendingSessions.firstOrNull {
                                    it.id == sessionId && it.kind == "concept"
                                }
                                if (pendingSessionSnapshot == null) {
                                    Title(stringResource(R.string.session_loading_title))
                                } else if (saved == null ||
                                    !conceptSessionMatches(saved, active, lesson)) {
                                    Title(stringResource(R.string.session_unavailable_title))
                                    Note(stringResource(R.string.session_unavailable_note))
                                    Action(stringResource(R.string.session_close_action)) {
                                        scope.launch {
                                            sessions.abandon(sessionId)
                                            page = "Learn"
                                        }
                                    }
                                } else {
                                    fun save(action: suspend () -> Unit) {
                                        if (busy) return
                                        busy = true
                                        scope.launch {
                                            try {
                                                action()
                                                error = null
                                            } catch (_: Exception) {
                                                error = context.getString(R.string.study_save_failed)
                                            } finally { busy = false }
                                        }
                                    }
                                    ConceptLessonScreen(active, lesson, saved, busy, error,
                                        onExit = { page = "Learn" },
                                        onSelect = { index -> save {
                                            conceptSessions.select(sessionId, index)
                                        } },
                                        onHint = { save { conceptSessions.useHint(sessionId) } },
                                        onReveal = { save {
                                            conceptSessions.reveal(sessionId, active, lesson)
                                        } },
                                        onSource = { save {
                                            conceptSessions.viewSource(sessionId)
                                        } },
                                        onCheck = { save {
                                            conceptSessions.check(sessionId, active, lesson)
                                        } },
                                        onNext = { save {
                                            conceptSessions.advance(sessionId, active, lesson)
                                        } },
                                        onFinish = { save {
                                            study.completeLesson(lesson, sessionId, active.courseId)
                                            runCatching { journeysRepo.setStage(active.courseId, lesson,
                                                LessonStage.Complete) }
                                            page = "Done"
                                        } })
                                }
                            }
                            "Lesson" -> if (journeySnapshot == null || pendingSessionSnapshot == null) {
                                Title(stringResource(R.string.session_loading_title))
                            } else LessonScreen(active, lesson, currentStage,
                                bookmarks.any { it.courseId == active.courseId && it.lessonId == lesson.id }, busy,
                                lesson.prerequisiteConceptIds.count { concept ->
                                    eligible.any { it.conceptId == concept }
                                }, error,
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
                                onStage = { nextStage ->
                                    busy = true
                                    scope.launch {
                                        try {
                                            journeysRepo.setStage(active.courseId, lesson, nextStage)
                                            error = null
                                        } catch (_: Exception) {
                                            error = context.getString(R.string.study_save_failed)
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
                                                journeysRepo.setStage(active.courseId, lesson, LessonStage.Practice)
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
                                }, onExit = { page = "Learn" })
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
                                choice, saved.feedback, busy, error,
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
                                            runCatching { journeysRepo.setStage(active.courseId, lesson,
                                                LessonStage.Complete) }
                                            page = "Done"
                                            error = null
                                        } catch (_: Exception) {
                                            error = context.getString(R.string.completion_failed)
                                        } finally { busy = false }
                                    }
                                }, onExit = { page = "Learn" })
                            }
                            "Done" -> {
                                val next = orderedLessons(active).firstOrNull { candidate ->
                                    candidate.id != lesson.id && !lessonIsComplete(candidate, localCompletions) &&
                                        lessonIsAvailable(active, candidate, localCompletions)
                                }
                                DoneScreen(active, lesson, localCompletions, streak, studiedToday, next,
                                    openNext = { next?.let(::openLesson) },
                                    openProgress = { page = "Progress" },
                                    openPath = { page = "Learn" })
                            }
                            "Review" -> {
                                val savedReview = pendingSessions.firstOrNull {
                                    it.id == reviewSessionId && it.kind == "review"
                                }
                                if (pendingSessionSnapshot == null || reviewSnapshot == null ||
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
                                            onOpenLesson = { openLesson(sourceLesson) })
                                    }
                                }
                            }
                            "Progress" -> if (completionSnapshot == null || studyDaySnapshot == null ||
                                bookmarkSnapshot == null) {
                                Title(stringResource(R.string.session_loading_title))
                            } else ProgressScreen(active,
                                localCompletions,
                                bookmarks.filter { it.courseId == active.courseId }, streak,
                                studiedToday)
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
                if (!focusedStudy) {
                val destinationPage = when (page) {
                    "Lesson", "Quiz", "Done" -> "Learn"
                    else -> page
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp,
                    vertical = if (compactHeight) 0.dp else 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Today", "Learn", "Review", "Progress").forEach { destination ->
                        val label = when (destination) {
                            "Today" -> R.string.nav_today
                            "Learn" -> R.string.nav_learn
                            "Review" -> R.string.nav_review
                            else -> R.string.nav_progress
                        }
                        val activeDestination = destinationPage == destination
                        TextButton(onClick = {
                            if (destination == "Review") startReviews() else page = destination
                        },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = if (activeDestination)
                                    MaterialTheme.colorScheme.secondaryContainer else Color.Transparent),
                            contentPadding = PaddingValues(horizontal = 2.dp),
                            modifier = Modifier.weight(1f).heightIn(min = if (compactHeight) 48.dp else 52.dp)
                                .semantics { this.selected = activeDestination }) {
                            Text(stringResource(label), maxLines = 1, softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = if (largeText) 12.sp else 14.sp))
                        }
                    }
                }
                }
            }
        }
    }
}
