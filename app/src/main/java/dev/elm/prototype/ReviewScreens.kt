package dev.elm.prototype

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
    onSubmit: () -> Unit, onSource: () -> Unit, onNext: () -> Unit) {
    Title(stringResource(R.string.nav_review))
    Note(stringResource(R.string.review_progress, index + 1, total))
    Note(question.prompt)
    question.choices.forEachIndexed { choiceIndex, choice ->
        OutlinedButton(onClick = { onSelect(choiceIndex) }, enabled = !feedback && !revealed && !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                .semantics { selected = selectedIndex == choiceIndex }) {
            Text(if (selectedIndex == choiceIndex) stringResource(R.string.selected_prefix, choice)
                else choice, fontSize = 18.sp)
        }
    }
    if (!feedback) {
        if (!hinted) TextButton(onClick = onHint) { Text(stringResource(R.string.show_hint)) }
        if (hinted) Panel { Note(lesson.notes) }
        if (!revealed) TextButton(onClick = onReveal) { Text(stringResource(R.string.reveal_answer)) }
        if (revealed) Panel {
            Note(stringResource(R.string.revealed_answer, question.choices[question.correctIndex]))
        }
        Action(stringResource(R.string.submit_review), (selectedIndex >= 0 || revealed) && !busy, onSubmit)
    } else {
        Panel {
            Text(stringResource(if (!wasDue) R.string.linked_recall
                else if (selectedIndex == question.correctIndex && !hinted && !revealed)
                    R.string.independent_recall else R.string.assisted_recall),
                style = MaterialTheme.typography.titleLarge)
            Note(question.explanation)
            Note(stringResource(R.string.review_source))
        }
        TextButton(onClick = onSource) { Text(stringResource(R.string.replay_review_source)) }
        if (sourceVisible) androidx.compose.runtime.key(question.conceptId) { AudioPanel(pack, lesson) }
        Action(stringResource(if (index + 1 < total) R.string.review_next else R.string.review_finish),
            click = onNext)
    }
}
