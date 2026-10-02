package dev.elm.prototype

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion

@Composable
fun TodayScreen(pack: ContentPack, completions: List<LessonCompletion>,
    dueCount: Int, streak: StreakStats, open: (PackLesson) -> Unit, openReviews: () -> Unit) {
    val next = pack.lessons.firstOrNull { lesson ->
        completions.none { it.lessonId == lesson.id && it.version == lesson.version }
    } ?: pack.lessons.first()
    Title(stringResource(R.string.today_title))
    Note(stringResource(R.string.today_subtitle))
    if (dueCount > 0) Panel {
        Text(stringResource(R.string.due_count, dueCount), style = MaterialTheme.typography.titleLarge)
        Note(stringResource(R.string.review_intro))
        Action(stringResource(R.string.start_review_batch), click = openReviews)
    }
    Panel {
        Text(stringResource(R.string.your_next_step), style = MaterialTheme.typography.labelLarge)
        Text(next.title, style = MaterialTheme.typography.headlineMedium)
        Action(stringResource(if (completions.any { it.lessonId == next.id && it.version == next.version })
            R.string.revisit_lesson else R.string.open_lesson)) { open(next) }
    }
    StreakPanel(streak, compact = true)
}

@Composable
fun CourseScreen(pack: ContentPack, completions: List<LessonCompletion>, bookmarks: List<Bookmark>,
    open: (PackLesson) -> Unit) {
    Title(stringResource(R.string.course_title))
    Note(pack.courseTitle)
    Note(stringResource(R.string.course_count, pack.lessons.size))
    pack.lessons.forEachIndexed { index, lesson ->
        val done = completions.any { it.lessonId == lesson.id && it.version == lesson.version }
        Panel {
            Text("${index + 1}. ${lesson.title}", style = MaterialTheme.typography.titleLarge)
            if (done) Text(stringResource(R.string.lesson_complete))
            if (bookmarks.any { it.lessonId == lesson.id }) Text(stringResource(R.string.bookmark_saved))
            Text(lesson.notes, style = MaterialTheme.typography.bodyLarge,
                maxLines = 3, overflow = TextOverflow.Ellipsis)
            Action(stringResource(if (done) R.string.revisit_lesson else R.string.open_lesson)) { open(lesson) }
        }
    }
}

@Composable
fun LessonScreen(pack: ContentPack, lesson: PackLesson, bookmarked: Boolean, busy: Boolean,
    relatedCount: Int, onBookmark: () -> Unit, onQuiz: () -> Unit,
    onQuickRecall: () -> Unit) {
    Title(lesson.title)
    Note(lesson.notes)
    OutlinedButton(onClick = onBookmark, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(stringResource(if (bookmarked) R.string.bookmark_remove else R.string.bookmark_add))
    }
    if (relatedCount > 0) Panel {
        Note(stringResource(R.string.related_recall_note))
        Action(stringResource(R.string.related_recall), click = onQuickRecall)
    }
    androidx.compose.runtime.key(lesson.id) { AudioPanel(pack, lesson) }
    Action(stringResource(R.string.try_questions)) { onQuiz() }
}

@Composable
fun QuizScreen(pack: ContentPack, lesson: PackLesson, index: Int, selectedIndex: Int,
    feedback: Boolean, busy: Boolean,
    onSelect: (Int) -> Unit, onCheck: () -> Unit, onNext: () -> Unit, onFinish: () -> Unit) {
    val question = lesson.questions[index]
    Title(lesson.title)
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
fun DoneScreen(streak: StreakStats, openProgress: () -> Unit) {
    Title(stringResource(R.string.completion_saved))
    Panel { Note(stringResource(R.string.completion_note)) }
    StreakPanel(streak, compact = true)
    Action(stringResource(R.string.see_progress), click = openProgress)
}

@Composable
fun ProgressScreen(pack: ContentPack, completions: List<LessonCompletion>, bookmarks: List<Bookmark>,
    streak: StreakStats) {
    val current = completions.filter { completion ->
        pack.lessons.any { it.id == completion.lessonId && it.version == completion.version }
    }
    Title(stringResource(R.string.progress_title))
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
