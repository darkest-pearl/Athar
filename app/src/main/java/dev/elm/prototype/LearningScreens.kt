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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color

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
    open: (PackLesson) -> Unit, openPath: () -> Unit, openReviews: () -> Unit,
    resumeLesson: (() -> Unit)? = null) {
    val next = nextAvailableLesson(pack, completions)
    val continuing = next != null && journeys.any { it.courseId == pack.courseId &&
        it.lessonId == next.id && it.lessonVersion == next.version &&
        it.stageValue() in listOf(LessonStage.Listen, LessonStage.Takeaway, LessonStage.Practice) }
    if (LocalConfiguration.current.screenHeightDp >= 500)
        Text(stringResource(R.string.today_title_guided), style = MaterialTheme.typography.headlineMedium,
            fontFamily = FontFamily.Serif, modifier = Modifier.semantics { heading() })
    Surface(color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.today_path_label), style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary)
            Text(next?.title ?: stringResource(R.string.path_all_complete),
                style = MaterialTheme.typography.headlineMedium, fontFamily = FontFamily.Serif,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (next != null) Action(stringResource(if (resumeLesson != null) R.string.session_continue_action
                else if (continuing) R.string.continue_lesson else R.string.start_lesson),
                click = { if (resumeLesson != null) resumeLesson() else open(next) })
            else Action(stringResource(R.string.explore_path), click = openPath)
            val done = pack.lessons.count { lessonIsComplete(it, completions) }
            Text(stringResource(R.string.path_count, done, pack.lessons.size),
                style = MaterialTheme.typography.labelLarge)
            LinearProgressIndicator(progress = { if (pack.lessons.isEmpty()) 0f
                else done.toFloat() / pack.lessons.size }, modifier = Modifier.fillMaxWidth().height(6.dp))
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("◎", style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.daily_goal_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(if (studiedToday) R.string.daily_goal_done else R.string.daily_goal_open),
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
        Surface(color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("✦", style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.tertiary)
                Text(stringResource(R.string.streak_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.streak_current, streak.current),
                    style = MaterialTheme.typography.bodyMedium)
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
    val doneCount = pack.lessons.count { lessonIsComplete(it, completions) }
    val simplePath = LocalDensity.current.fontScale >= 1.3f ||
        LocalConfiguration.current.screenWidthDp < 360
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.path_title), style = MaterialTheme.typography.headlineMedium,
            fontFamily = FontFamily.Serif, modifier = Modifier.semantics { heading() })
        Text(pack.courseTitle, style = MaterialTheme.typography.titleMedium,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (simplePath) Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.course_progress_label), style = MaterialTheme.typography.labelLarge)
            Text(stringResource(R.string.path_count, doneCount, pack.lessons.size),
                style = MaterialTheme.typography.labelLarge)
        } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.course_progress_label), style = MaterialTheme.typography.labelLarge)
            Text(stringResource(R.string.path_count, doneCount, pack.lessons.size),
                style = MaterialTheme.typography.labelLarge)
        }
        LinearProgressIndicator(progress = { if (pack.lessons.isEmpty()) 0f
            else doneCount.toFloat() / pack.lessons.size },
            modifier = Modifier.fillMaxWidth().height(6.dp))
        if (dueCount > 0) Surface(color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(16.dp)) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.due_count, dueCount), modifier = Modifier.weight(1f))
                TextButton(onClick = openReviews) { Text(stringResource(R.string.start_review_batch)) }
            }
        }
        pack.units.forEachIndexed { unitIndex, unit ->
            val lessons = unit.lessonIds.mapNotNull { id -> pack.lessons.firstOrNull { it.id == id } }
            val unitDone = lessons.count { lessonIsComplete(it, completions) }
            Surface(color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.unit_number, unitIndex + 1),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary)
                    Text(unit.title, style = MaterialTheme.typography.titleLarge,
                        fontFamily = FontFamily.Serif, modifier = Modifier.semantics { heading() })
                    Text(stringResource(R.string.path_count, unitDone, lessons.size),
                        style = MaterialTheme.typography.labelMedium)
                    lessons.forEachIndexed { index, lesson ->
                        val number = pack.units.take(unitIndex).sumOf { it.lessonIds.size } + index + 1
                        val done = lessonIsComplete(lesson, completions)
                        val available = lessonIsAvailable(pack, lesson, completions)
                        val isCurrent = lesson.id == current
                        val label = when {
                            done -> stringResource(R.string.path_state_done)
                            !available -> stringResource(R.string.path_state_locked)
                            isCurrent -> stringResource(R.string.path_state_current)
                            else -> stringResource(R.string.path_state_upcoming)
                        }
                        val indent = if (simplePath) 0.dp else if (number % 2 == 0) 20.dp else 0.dp
                        Row(Modifier.fillMaxWidth().padding(start = indent),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val shape = if (done) RoundedCornerShape(16.dp) else CircleShape
                                if (isCurrent) Button(onClick = {
                                    selectedId = if (selectedId == lesson.id) "" else lesson.id
                                },
                                    modifier = Modifier.size(56.dp), shape = shape,
                                    contentPadding = PaddingValues(0.dp)) {
                                    Text(if (done) "✓" else number.toString(), fontSize = 20.sp)
                                } else OutlinedButton(onClick = {
                                    selectedId = if (selectedId == lesson.id) "" else lesson.id
                                },
                                    modifier = Modifier.size(56.dp), shape = shape,
                                    border = BorderStroke(2.dp, if (available)
                                        MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                                    contentPadding = PaddingValues(0.dp)) {
                                    Text(if (done) "✓" else if (available) number.toString() else "•",
                                        fontSize = 20.sp)
                                }
                                if (index < lessons.lastIndex) Surface(
                                    modifier = Modifier.width(3.dp).height(18.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)) {}
                            }
                            Column(Modifier.weight(1f)) {
                                Text(lesson.title, style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(label, style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary)
                                if (bookmarks.any { it.lessonId == lesson.id })
                                    Text(stringResource(R.string.bookmark_saved),
                                        style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        AnimatedVisibility(selectedId == lesson.id,
                            enter = fadeIn(tween(180)), exit = fadeOut(tween(90))) {
                            Surface(color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (!available && !done)
                                        Text(stringResource(R.string.path_prerequisite_note))
                                    else Action(stringResource(when {
                                        done -> R.string.revisit_lesson
                                        journeys.any { it.lessonId == lesson.id &&
                                            it.lessonVersion == lesson.version &&
                                            it.stageValue() != LessonStage.Complete &&
                                            it.stageValue() != LessonStage.Intro } ->
                                            R.string.session_continue_action
                                        else -> R.string.start_lesson
                                    })) { open(lesson) }
                                    Text(lesson.objective.ifBlank { lesson.notes },
                                        style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
            if (unitIndex < pack.units.lastIndex) Surface(
                modifier = Modifier.align(Alignment.CenterHorizontally).width(3.dp).height(14.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)) {}
        }
    }
}

@Composable
fun LessonScreen(pack: ContentPack, lesson: PackLesson, stage: LessonStage,
    bookmarked: Boolean, busy: Boolean, relatedCount: Int, errorMessage: String?,
    onBookmark: () -> Unit, onStage: (LessonStage) -> Unit,
    onQuiz: () -> Unit, onQuickRecall: () -> Unit, onExit: () -> Unit) {
    val actionText = stringResource(when (stage) {
        LessonStage.Intro -> if (lesson.segmentRef.isEmpty()) R.string.continue_to_takeaway else R.string.start_listening
        LessonStage.Listen -> R.string.continue_to_takeaway
        LessonStage.Takeaway -> R.string.try_questions
        LessonStage.Practice -> R.string.session_continue_action
        LessonStage.Complete -> R.string.revisit_lesson
    })
    LessonScaffold(stage, lesson.title, pack.developmentOnly, onExit = onExit,
        actionText = actionText, actionEnabled = !busy, saving = busy,
        errorMessage = errorMessage, onAction = {
            when (stage) {
                LessonStage.Intro -> onStage(if (lesson.segmentRef.isEmpty()) LessonStage.Takeaway else LessonStage.Listen)
                LessonStage.Listen -> onStage(LessonStage.Takeaway)
                LessonStage.Takeaway, LessonStage.Practice -> onQuiz()
                LessonStage.Complete -> onStage(LessonStage.Intro)
            }
        }) {
    AnimatedContent(targetState = stage, label = "lesson stage") { visibleStage ->
      Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { when (visibleStage) {
        LessonStage.Intro -> {
            Title(lesson.title)
            Panel {
                Text(stringResource(R.string.lesson_objective_label),
                    style = MaterialTheme.typography.labelLarge)
                Note(lesson.objective.ifBlank { lesson.notes })
                pack.segments[lesson.segmentRef]?.let { segment ->
                    Text(stringResource(R.string.path_duration,
                        formatAudioTime(segment.endMs - segment.startMs)))
                }
            }
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
            if (lesson.segmentRef.isNotEmpty()) key(lesson.id) { AudioPanel(pack, lesson) }
            Note(stringResource(R.string.listen_stage_note))
        }
        LessonStage.Takeaway -> {
            Title(stringResource(R.string.takeaway_stage_title))
            Panel { Note(lesson.takeaway.ifBlank { lesson.notes }) }
        }
        LessonStage.Practice -> {
            Title(stringResource(R.string.practice_stage_title))
            Note(stringResource(R.string.session_continue_note))
        }
        LessonStage.Complete -> {
            Title(stringResource(R.string.lesson_complete))
        }
    } }
    }
    }
}

@Composable
fun QuizScreen(pack: ContentPack, lesson: PackLesson, index: Int, selectedIndex: Int,
    feedback: Boolean, busy: Boolean, errorMessage: String?,
    onSelect: (Int) -> Unit, onCheck: () -> Unit, onNext: () -> Unit, onFinish: () -> Unit,
    onExit: () -> Unit) {
    val question = lesson.questions[index]
    var sourceVisible by rememberSaveable(question.id, index) { mutableStateOf(false) }
    LessonScaffold(LessonStage.Practice, lesson.title, pack.developmentOnly,
        stepLabel = stringResource(R.string.question_progress, index + 1, lesson.questions.size),
        onExit = onExit,
        actionText = stringResource(if (!feedback) R.string.check_answer
            else if (index + 1 < lesson.questions.size) R.string.next_question
            else R.string.finish_lesson),
        actionEnabled = !busy && (feedback || selectedIndex >= 0), saving = busy,
        errorMessage = errorMessage,
        onAction = { if (!feedback) onCheck() else if (index + 1 < lesson.questions.size)
            onNext() else onFinish() }) {
        Text(question.prompt, style = MaterialTheme.typography.headlineSmall)
        question.choices.forEachIndexed { choiceIndex, choice ->
            AnswerChoice(choice, selectedIndex == choiceIndex,
                choiceIndex == question.correctIndex, feedback, busy) { onSelect(choiceIndex) }
        }
        AnimatedVisibility(visible = feedback, enter = fadeIn(tween(180)),
            exit = fadeOut(tween(90))) {
            Panel {
                Text(stringResource(if (selectedIndex == question.correctIndex)
                    R.string.correct_answer else R.string.incorrect_answer),
                    style = MaterialTheme.typography.titleLarge)
                Note(question.explanation)
            }
        }
        if (feedback) {
            OutlinedButton(onClick = { sourceVisible = !sourceVisible },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(if (sourceVisible) R.string.hide_answer_source
                    else R.string.source_passage_title))
            }
            AnimatedVisibility(sourceVisible) { AnswerSourcePanel(pack, lesson, question) }
        }
    }
}

@Composable
fun DoneScreen(pack: ContentPack, lesson: PackLesson, completions: List<LessonCompletion>,
    streak: StreakStats, studiedToday: Boolean, next: PackLesson?,
    bookEventId: Int, bookPlayEvent: Boolean, reducedMotion: Boolean,
    onBookEventStarted: () -> Unit,
    openNext: () -> Unit, openProgress: () -> Unit, openPath: () -> Unit) {
    val done = pack.lessons.count { lessonIsComplete(it, completions) }
    LessonScaffold(LessonStage.Complete, lesson.title, pack.developmentOnly,
        onExit = openPath,
        actionText = stringResource(if (next != null) R.string.start_next_lesson
            else R.string.see_progress),
        onAction = if (next != null) openNext else openProgress) {
        Surface(color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BookCompanion(BookMood.Complete, bookEventId, bookPlayEvent,
                    reducedMotion, onBookEventStarted, size = 150.dp)
                Text(stringResource(R.string.lesson_complete),
                    style = MaterialTheme.typography.headlineMedium, fontFamily = FontFamily.Serif)
                Text(lesson.title, style = MaterialTheme.typography.titleMedium)
            }
        }
        Text(stringResource(R.string.path_count, done, pack.lessons.size),
            style = MaterialTheme.typography.titleMedium)
        LinearProgressIndicator(progress = { if (pack.lessons.isEmpty()) 0f
            else done.toFloat() / pack.lessons.size }, modifier = Modifier.fillMaxWidth().height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CompletionMetric(stringResource(R.string.daily_goal_title),
                stringResource(if (studiedToday) R.string.daily_goal_done else R.string.daily_goal_open),
                Modifier.weight(1f))
            CompletionMetric(stringResource(R.string.streak_title),
                stringResource(R.string.streak_current, streak.current), Modifier.weight(1f))
        }
        if (next != null) Text(stringResource(R.string.next_lesson_label, next.title),
            style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = openProgress, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.see_progress))
        }
    }
}

@Composable private fun CompletionMetric(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp), modifier = modifier) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
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
    MilestoneBadge("✦", stringResource(R.string.badge_first_lesson), current.isNotEmpty())
    MilestoneBadge("✓", stringResource(R.string.badge_course_path),
        pack.lessons.isNotEmpty() && current.size >= pack.lessons.size)
    MilestoneBadge("◎", stringResource(R.string.badge_three_days), streak.totalDays >= 3)
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

@Composable private fun MilestoneBadge(mark: String, title: String, earned: Boolean) {
    Surface(color = if (earned) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(shape = CircleShape, color = if (earned) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(mark, color = if (earned) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface)
                }
            }
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(stringResource(if (earned) R.string.badge_earned else R.string.badge_pending),
                style = MaterialTheme.typography.labelMedium)
        }
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
    reducedMotion: Boolean, onReducedMotion: (Boolean) -> Unit,
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
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
        .clickable { onReducedMotion(!reducedMotion) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Reduce character motion", style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f))
        Switch(checked = reducedMotion, onCheckedChange = null)
    }
    Note(stringResource(R.string.pending_translation))
    Text(stringResource(R.string.ethiopic_sample), fontSize = 28.sp)
    Text(stringResource(R.string.arabic_sample), fontSize = 28.sp)
    Action(stringResource(R.string.back_to_today), click = onHome)
}
