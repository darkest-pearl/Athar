package dev.elm.prototype

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.heading

fun orderedLessons(pack: ContentPack): List<PackLesson> = pack.units.flatMap { unit ->
    unit.lessonIds.mapNotNull { id -> pack.lessons.firstOrNull { it.id == id } }
}

fun lessonIsComplete(lesson: PackLesson, completions: List<LessonCompletion>) =
    completions.any { it.lessonId == lesson.id && it.version == lesson.version }

fun lessonIsAvailable(pack: ContentPack, lesson: PackLesson,
    completions: List<LessonCompletion>): Boolean = lesson.prerequisiteConceptIds.all { concept ->
    pack.lessons.any { source ->
        source.questions.any { it.conceptId == concept } && lessonIsComplete(source, completions)
    }
}

fun nextAvailableLesson(pack: ContentPack, completions: List<LessonCompletion>): PackLesson? =
    orderedLessons(pack).firstOrNull { !lessonIsComplete(it, completions) &&
        lessonIsAvailable(pack, it, completions) }

@Composable private fun CourseProgress(done: Int, total: Int, label: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        val fraction = if (total == 0) 0f else done.toFloat() / total
        val animated by animateFloatAsState(fraction, label = "course progress")
        LinearProgressIndicator(progress = { animated },
            modifier = Modifier.fillMaxWidth().height(10.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant)
        Text(stringResource(R.string.path_count, done, total))
    }
}

@Composable fun LessonStageHeader(stage: LessonStage, title: String,
    onExit: (() -> Unit)? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.lesson_stage_progress, stage.ordinal + 1,
                LessonStage.entries.size), style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary)
            if (onExit != null) TextButton(onClick = onExit,
                modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.exit_to_path))
            }
        }
        val animated by animateFloatAsState((stage.ordinal + 1f) / LessonStage.entries.size,
            label = "lesson stage progress")
        LinearProgressIndicator(progress = { animated },
            modifier = Modifier.fillMaxWidth().height(10.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant)
    }
}

@Composable
fun TodayScreen(pack: ContentPack, completions: List<LessonCompletion>,
    journeys: List<LessonJourney>, dueCount: Int, streak: StreakStats, studiedToday: Boolean,
    open: (PackLesson) -> Unit, openPath: () -> Unit, openReviews: () -> Unit) {
    val next = nextAvailableLesson(pack, completions)
    val continuing = next != null && journeys.any { it.courseId == pack.courseId &&
        it.lessonId == next.id && it.lessonVersion == next.version &&
        it.stageValue() in listOf(LessonStage.Listen, LessonStage.Takeaway, LessonStage.Practice) }
    Title(stringResource(R.string.today_title_guided))
    Note(pack.courseTitle)
    Panel {
        Text(stringResource(R.string.today_path_label), style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary)
        Text(next?.title ?: stringResource(R.string.path_all_complete),
            style = MaterialTheme.typography.headlineMedium, fontFamily = FontFamily.Serif)
        if (next != null) Action(stringResource(if (continuing) R.string.continue_lesson
            else R.string.start_lesson), click = { open(next) })
        else Action(stringResource(R.string.explore_path), click = openPath)
        CourseProgress(pack.lessons.count { lessonIsComplete(it, completions) },
            pack.lessons.size, stringResource(R.string.course_progress_label))
        Note(next?.objective?.ifBlank { next.notes }
            ?: stringResource(R.string.path_all_complete_note))
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.daily_goal_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(if (studiedToday) R.string.daily_goal_done else R.string.daily_goal_open))
            }
        }
        Surface(color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.streak_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.streak_current, streak.current))
            }
        }
    }
    if (dueCount > 0) Panel {
        Text(stringResource(R.string.due_count, dueCount), style = MaterialTheme.typography.titleLarge)
        Note(stringResource(R.string.review_intro))
        Action(stringResource(R.string.start_review_batch), click = openReviews)
    }
    TextButton(onClick = openPath, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(stringResource(R.string.explore_path))
    }
}

@Composable
fun CourseScreen(pack: ContentPack, completions: List<LessonCompletion>, bookmarks: List<Bookmark>,
    journeys: List<LessonJourney>, dueCount: Int, open: (PackLesson) -> Unit,
    openReviews: () -> Unit) {
    var selectedId by rememberSaveable(pack.courseId) { mutableStateOf("") }
    val current = nextAvailableLesson(pack, completions)?.id
    LaunchedEffect(current) { if (current != null) selectedId = current }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Title(stringResource(R.string.path_title))
    Note(pack.courseTitle)
    CourseProgress(pack.lessons.count { lessonIsComplete(it, completions) },
        pack.lessons.size, stringResource(R.string.course_progress_label))
    if (dueCount > 0) Panel {
        Text(stringResource(R.string.path_review_title), style = MaterialTheme.typography.titleLarge)
        Note(stringResource(R.string.due_count, dueCount))
        Action(stringResource(R.string.start_review_batch), click = openReviews)
    }
    var number = 0
    pack.units.forEachIndexed { unitIndex, unit ->
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val lessons = unit.lessonIds.mapNotNull { id -> pack.lessons.firstOrNull { it.id == id } }
        Text(stringResource(R.string.unit_number, unitIndex + 1),
            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(unit.title, style = MaterialTheme.typography.headlineSmall, fontFamily = FontFamily.Serif,
            modifier = Modifier.semantics { heading() })
        CourseProgress(lessons.count { lessonIsComplete(it, completions) }, lessons.size,
            stringResource(R.string.unit_progress_label))
        lessons.forEachIndexed { index, lesson ->
            number++
            val done = lessonIsComplete(lesson, completions)
            val available = lessonIsAvailable(pack, lesson, completions)
            val isCurrent = lesson.id == current
            val label = when {
                done -> stringResource(R.string.path_state_done)
                !available -> stringResource(R.string.path_state_locked)
                isCurrent -> stringResource(R.string.path_state_current)
                else -> stringResource(R.string.path_state_upcoming)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val shape = if (done) RoundedCornerShape(18.dp) else CircleShape
                    val nodeText = if (done) "✓" else number.toString()
                    if (isCurrent) Button(onClick = { selectedId = lesson.id },
                        modifier = Modifier.size(64.dp), shape = shape,
                        contentPadding = PaddingValues(0.dp)) { Text(nodeText, fontSize = 22.sp) }
                    else OutlinedButton(onClick = { selectedId = lesson.id },
                        modifier = Modifier.size(64.dp), shape = shape,
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(0.dp)) { Text(nodeText, fontSize = 22.sp) }
                    if (index + 1 < lessons.size) Surface(
                        modifier = Modifier.width(3.dp).height(16.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)) {}
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(lesson.title, style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal)
                    Text(label, color = MaterialTheme.colorScheme.primary)
                    if (bookmarks.any { it.lessonId == lesson.id })
                        Text(stringResource(R.string.bookmark_saved), style = MaterialTheme.typography.labelMedium)
                }
            }
            AnimatedVisibility(visible = selectedId == lesson.id,
                enter = fadeIn(tween(180)), exit = fadeOut(tween(90))) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!available && !done) Note(stringResource(R.string.path_prerequisite_note))
                        else Action(stringResource(when {
                            done -> R.string.revisit_lesson
                            journeys.any { it.lessonId == lesson.id && it.lessonVersion == lesson.version &&
                                it.stageValue() != LessonStage.Complete && it.stageValue() != LessonStage.Intro } ->
                                R.string.session_continue_action
                            else -> R.string.start_lesson
                        })) { open(lesson) }
                        Note(lesson.objective.ifBlank { lesson.notes })
                        val segment = pack.segments.getValue(lesson.segmentRef)
                        Text(stringResource(R.string.path_duration,
                            formatAudioTime(segment.endMs - segment.startMs)),
                            style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
      }
    }
    }
}

@Composable
fun LessonScreen(pack: ContentPack, lesson: PackLesson, stage: LessonStage,
    bookmarked: Boolean, busy: Boolean, relatedCount: Int,
    onBookmark: () -> Unit, onStage: (LessonStage) -> Unit,
    onQuiz: () -> Unit, onQuickRecall: () -> Unit, onExit: () -> Unit) {
    LessonStageHeader(stage, lesson.title, onExit)
    AnimatedContent(targetState = stage, label = "lesson stage") { visibleStage ->
      Column(verticalArrangement = Arrangement.spacedBy(20.dp)) { when (visibleStage) {
        LessonStage.Intro -> {
            Panel {
                Text(stringResource(R.string.lesson_objective_label),
                    style = MaterialTheme.typography.labelLarge)
                Note(lesson.objective.ifBlank { lesson.notes })
                val segment = pack.segments.getValue(lesson.segmentRef)
                Text(stringResource(R.string.path_duration,
                    formatAudioTime(segment.endMs - segment.startMs)))
            }
            Action(stringResource(R.string.start_listening), !busy) { onStage(LessonStage.Listen) }
            OutlinedButton(onClick = onBookmark, enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(if (bookmarked) R.string.bookmark_remove else R.string.bookmark_add))
            }
            if (relatedCount > 0) Panel {
                Note(stringResource(R.string.related_recall_note))
                Action(stringResource(R.string.related_recall), click = onQuickRecall)
            }
        }
        LessonStage.Listen -> {
            Title(stringResource(R.string.listen_stage_title))
            Note(stringResource(R.string.listen_stage_note))
            Action(stringResource(R.string.continue_to_takeaway), !busy) {
                onStage(LessonStage.Takeaway)
            }
            key(lesson.id) { AudioPanel(pack, lesson) }
        }
        LessonStage.Takeaway -> {
            Title(stringResource(R.string.takeaway_stage_title))
            Panel { Note(lesson.takeaway.ifBlank { lesson.notes }) }
            Action(stringResource(R.string.try_questions), !busy) { onQuiz() }
        }
        LessonStage.Practice -> {
            Title(stringResource(R.string.practice_stage_title))
            Note(stringResource(R.string.session_continue_note))
            Action(stringResource(R.string.session_continue_action), !busy) { onQuiz() }
        }
        LessonStage.Complete -> {
            Title(stringResource(R.string.lesson_complete))
            Action(stringResource(R.string.revisit_lesson), !busy) { onStage(LessonStage.Intro) }
        }
    } }
    }
}

@Composable
fun QuizScreen(pack: ContentPack, lesson: PackLesson, index: Int, selectedIndex: Int,
    feedback: Boolean, busy: Boolean,
    onSelect: (Int) -> Unit, onCheck: () -> Unit, onNext: () -> Unit, onFinish: () -> Unit) {
    val question = lesson.questions[index]
    LessonStageHeader(LessonStage.Practice, lesson.title)
    Note(stringResource(R.string.question_progress, index + 1, lesson.questions.size))
    Note(question.prompt)
    question.choices.forEachIndexed { choiceIndex, choice ->
        AnswerChoice(choice, selectedIndex == choiceIndex, choiceIndex == question.correctIndex,
            feedback, busy) { onSelect(choiceIndex) }
    }
    if (!feedback) Action(stringResource(R.string.check_answer), selectedIndex >= 0 && !busy, onCheck)
    AnimatedVisibility(visible = feedback, enter = fadeIn(tween(180)), exit = fadeOut(tween(90))) {
        Panel {
            Text(stringResource(if (selectedIndex == question.correctIndex) R.string.correct_answer
                else R.string.incorrect_answer), style = MaterialTheme.typography.titleLarge)
            Note(question.explanation)
        }
    }
    if (feedback) {
        AnswerSourcePanel(pack, lesson, question)
        if (index + 1 < lesson.questions.size)
            Action(stringResource(R.string.next_question), !busy, onNext)
        else Action(stringResource(R.string.finish_lesson), !busy, onFinish)
    }
}

@Composable
fun DoneScreen(lesson: PackLesson, streak: StreakStats, next: PackLesson?, openNext: () -> Unit,
    openProgress: () -> Unit) {
    LessonStageHeader(LessonStage.Complete, lesson.title)
    Title(stringResource(R.string.lesson_complete))
    if (next != null) {
        Note(stringResource(R.string.next_lesson_label, next.title))
        Action(stringResource(R.string.start_next_lesson), click = openNext)
    } else Action(stringResource(R.string.see_progress), click = openProgress)
    Panel { Note(stringResource(R.string.completion_note)) }
    StreakPanel(streak, compact = true)
    if (next != null) TextButton(onClick = openProgress,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(stringResource(R.string.see_progress))
    }
}

@Composable
fun ProgressScreen(pack: ContentPack, completions: List<LessonCompletion>, bookmarks: List<Bookmark>,
    streak: StreakStats, studiedToday: Boolean) {
    val current = completions.filter { completion ->
        pack.lessons.any { it.id == completion.lessonId && it.version == completion.version }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Title(stringResource(R.string.progress_title))
    CourseProgress(current.size, pack.lessons.size, stringResource(R.string.course_progress_label))
    Panel {
        Text(stringResource(R.string.daily_goal_title), style = MaterialTheme.typography.titleLarge)
        Note(stringResource(if (studiedToday) R.string.daily_goal_done else R.string.daily_goal_open))
    }
    Title(stringResource(R.string.milestones_title))
    if (current.isNotEmpty()) Panel { Note(stringResource(R.string.badge_first_lesson)) }
    if (current.size >= pack.lessons.size) Panel { Note(stringResource(R.string.badge_course_path)) }
    if (streak.totalDays >= 3) Panel { Note(stringResource(R.string.badge_three_days)) }
    if (current.isEmpty() && streak.totalDays < 3) Note(stringResource(R.string.milestones_empty))
    StreakPanel(streak)
    Panel {
        Text(stringResource(R.string.progress_count, current.size), style = MaterialTheme.typography.headlineMedium)
        current.forEach { completion ->
            val title = pack.lessons.first { it.id == completion.lessonId }.title
            Note("$title · ${completion.studyDay}")
        }
    }
    Title(stringResource(R.string.progress_bookmarks))
    if (bookmarks.isEmpty()) Note(stringResource(R.string.no_bookmarks))
    bookmarks.forEach { bookmark ->
        val title = pack.lessons.firstOrNull { it.id == bookmark.lessonId }?.title ?: bookmark.lessonId
        Note(title)
    }
    Note(stringResource(R.string.progress_guest))
    }
}

@Composable
fun StreakPanel(streak: StreakStats, compact: Boolean = false) {
    Panel {
        Text(stringResource(R.string.streak_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.streak_current, streak.current))
        if (!compact) {
            Text(stringResource(R.string.streak_longest, streak.longest))
            Text(stringResource(R.string.streak_total, streak.totalDays))
            Note(stringResource(R.string.streak_note))
        }
    }
}

@Composable
fun SettingsScreen(pack: ContentPack, direction: Direction, importStatus: String?,
    onDirection: () -> Unit, onImport: () -> Unit, onHome: () -> Unit) {
    Title(stringResource(R.string.settings_title))
    Note(pack.courseTitle)
    Note(stringResource(R.string.pack_version, pack.contentVersion))
    Note(stringResource(if (pack.developmentOnly) R.string.development_label
        else R.string.local_pack_label))
    Action(stringResource(R.string.import_pack), click = onImport)
    if (importStatus != null) Text(importStatus, style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    Action(stringResource(if (direction == Direction.Garden) R.string.compare_editorial
        else R.string.use_garden), click = onDirection)
    Note(stringResource(R.string.pending_translation))
    Text(stringResource(R.string.ethiopic_sample), fontSize = 28.sp)
    Text(stringResource(R.string.arabic_sample), fontSize = 28.sp)
    Action(stringResource(R.string.back_to_today), click = onHome)
}
