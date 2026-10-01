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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Clock
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
    val contentRepo = remember { ContentRepository(context) }
    val initial = remember { runCatching { contentRepo.load() } }
    var pack by remember { mutableStateOf(initial.getOrNull()) }
    val completions by remember(dao) { dao.observeLessonCompletions() }
        .collectAsState(initial = emptyList())
    val bookmarks by remember(dao) { dao.observeBookmarks() }
        .collectAsState(initial = emptyList())
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
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val current = pack
        if (uri != null && current != null) {
            busy = true
            scope.launch {
                try {
                    val next = withContext(Dispatchers.IO) { contentRepo.import(uri, current) }
                    pack = next
                    lessonId = ""
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
    LaunchedEffect(page, lessonId, questionIndex) { scroll.scrollTo(0) }
    BackHandler(page != "Today") {
        page = when (page) {
            "Quiz" -> "Lesson"
            "Lesson" -> "Learn"
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
                Column(Modifier.weight(1f).verticalScroll(scroll)
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
                        val lesson = active.lessons.firstOrNull { it.id == lessonId } ?: active.lessons.first()
                        when (page) {
                            "Today" -> TodayScreen(active, completions, ::openLesson)
                            "Learn" -> CourseScreen(active, completions, bookmarks, ::openLesson)
                            "Lesson" -> LessonScreen(active, lesson,
                                bookmarks.any { it.lessonId == lesson.id }, busy,
                                onBookmark = {
                                    busy = true
                                    scope.launch {
                                        try {
                                            if (bookmarks.any { it.lessonId == lesson.id }) repo.unbookmark(lesson.id)
                                            else repo.bookmark(lesson.id)
                                        } catch (_: Exception) {
                                            error = context.getString(R.string.bookmark_failed)
                                        } finally { busy = false }
                                    }
                                },
                                onQuiz = {
                                    questionIndex = 0; selected = -1; feedback = false; page = "Quiz"
                                })
                            "Quiz" -> QuizScreen(lesson, questionIndex, selected, feedback, busy,
                                onSelect = { selected = it },
                                onCheck = {
                                    busy = true
                                    val question = lesson.questions[questionIndex]
                                    scope.launch {
                                        try {
                                            repo.submitLessonAnswer(lesson, question, sessionId, selected)
                                            feedback = true
                                            error = null
                                        } catch (_: Exception) {
                                            error = context.getString(R.string.answer_save_failed)
                                        } finally { busy = false }
                                    }
                                },
                                onNext = {
                                    questionIndex++
                                    selected = -1
                                    feedback = false
                                },
                                onFinish = {
                                    busy = true
                                    scope.launch {
                                        try {
                                            repo.completeLesson(lesson, sessionId)
                                            page = "Done"
                                            error = null
                                        } catch (_: Exception) {
                                            error = context.getString(R.string.completion_failed)
                                        } finally { busy = false }
                                    }
                                })
                            "Done" -> DoneScreen { page = "Progress" }
                            "Review" -> { Title(stringResource(R.string.nav_review)); Note(stringResource(R.string.nothing_due)) }
                            "Progress" -> ProgressScreen(active, completions, bookmarks)
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
                Row(Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("Today", "Learn", "Review", "Progress").forEach { destination ->
                        val label = when (destination) {
                            "Today" -> R.string.nav_today
                            "Learn" -> R.string.nav_learn
                            "Review" -> R.string.nav_review
                            else -> R.string.nav_progress
                        }
                        TextButton(onClick = { page = destination },
                            modifier = Modifier.heightIn(min = 48.dp)
                                .semantics { this.selected = page == destination }) {
                            Text(stringResource(label))
                        }
                    }
                }
            }
        }
    }
}
