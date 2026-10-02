package dev.elm.prototype

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/** The source is the question's validated reference, not the surrounding lesson player. */
@Composable
fun AnswerSourcePanel(pack: ContentPack, lesson: PackLesson, question: PackQuestion,
    onOpenLesson: (() -> Unit)? = null) {
    when (val ref = question.sourceRef) {
        is PackSourceRef.FixtureText -> {
            Panel {
                Text(stringResource(R.string.source_passage_title),
                    style = MaterialTheme.typography.titleMedium)
                Note(stringResource(R.string.fixture_text_source_label))
                Note(when (ref.passage) {
                    "notes" -> lesson.notes
                    "interface-contract" -> stringResource(R.string.fixture_interface_contract)
                    else -> error("Unvalidated fixture passage")
                })
            }
        }
        is PackSourceRef.RecordingPassage -> {
            val segment = pack.segments.getValue(ref.segmentId)
            val recording = pack.recordings.getValue(segment.recordingId)
            Text(stringResource(R.string.source_passage_title),
                style = MaterialTheme.typography.titleMedium)
            Note(recording.title.ifBlank { recording.id })
            Note(stringResource(R.string.source_passage_range,
                formatAudioTime(ref.startMs), formatAudioTime(ref.endMs)))
            AudioPanel(pack, lesson, ref)
        }
    }
    if (onOpenLesson != null) TextButton(onClick = onOpenLesson,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(stringResource(R.string.open_surrounding_lesson))
    }
}

fun formatAudioTime(milliseconds: Long): String {
    val total = milliseconds.coerceAtLeast(0) / 1000
    val seconds = total % 60
    val minutes = (total / 60) % 60
    val hours = total / 3600
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(total / 60, seconds)
}
