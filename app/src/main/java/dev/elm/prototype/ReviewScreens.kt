package dev.elm.prototype

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ReviewOverview(dueCount: Int, finished: Boolean, start: () -> Unit) {
    Title(stringResource(R.string.nav_review))
    if (finished) Panel { Text(stringResource(R.string.review_batch_done),
        style = MaterialTheme.typography.titleLarge) }
    if (dueCount == 0) Note(stringResource(R.string.review_empty))
    else {
        Note(stringResource(R.string.due_count, dueCount))
        Note(stringResource(if (finished) R.string.review_catchup else R.string.review_intro))
        Action(stringResource(R.string.start_review_batch), click = start)
    }
}

@Composable
fun ReviewQuestionScreen(pack: ContentPack, lesson: PackLesson, question: PackQuestion,
    index: Int, total: Int, selectedIndex: Int, hinted: Boolean, revealed: Boolean,
    feedback: Boolean, busy: Boolean, wasDue: Boolean, sourceVisible: Boolean,
    onSelect: (Int) -> Unit, onHint: () -> Unit, onReveal: () -> Unit,
    onSubmit: () -> Unit, onSource: () -> Unit, onNext: () -> Unit,
    onOpenLesson: () -> Unit) {
    val independent = wasDue && selectedIndex == question.correctIndex && !hinted && !revealed
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.nav_review), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
            Text(stringResource(R.string.review_progress, index + 1, total),
                style = MaterialTheme.typography.labelLarge)
            LinearProgressIndicator(progress = { (index + 1f) / total.coerceAtLeast(1) },
                modifier = Modifier.fillMaxWidth().height(8.dp))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(question.prompt, fontSize = 25.sp, lineHeight = 31.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
            question.choices.forEachIndexed { choiceIndex, choice ->
                AnswerChoice(choice, selectedIndex == choiceIndex,
                    choiceIndex == question.correctIndex, feedback || revealed, busy,
                    firstWrong = feedback && selectedIndex == choiceIndex &&
                        choiceIndex != question.correctIndex,
                    indicator = ('A' + choiceIndex).toString()) {
                    if (!feedback) onSelect(choiceIndex)
                }
            }
            if (!feedback) {
                if (!hinted) TextButton(onClick = onHint, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.show_hint))
                }
                if (hinted) Panel { Note(question.hint.ifBlank { lesson.notes }) }
                if (!revealed) TextButton(onClick = onReveal, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.reveal_answer))
                }
                if (revealed) Note(stringResource(R.string.reveal_note))
            }
            if (feedback && sourceVisible) AnswerSourcePanel(pack, lesson, question, onOpenLesson)
        }
        Surface(shadowElevation = 6.dp, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (feedback) {
                    Text(stringResource(if (!wasDue) R.string.linked_recall
                        else if (independent) R.string.independent_recall else R.string.assisted_recall),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    Text(question.explanation, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = onSource, modifier = Modifier.heightIn(min = 40.dp)) {
                        Text(stringResource(R.string.replay_review_source))
                    }
                }
                Action(if (feedback) stringResource(if (index + 1 < total) R.string.review_next
                    else R.string.review_finish) else stringResource(R.string.submit_review),
                    enabled = if (feedback) !busy else (selectedIndex >= 0 || revealed) && !busy,
                    click = if (feedback) onNext else onSubmit)
            }
        }
    }
}
