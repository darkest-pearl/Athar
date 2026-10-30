package dev.elm.prototype

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Executes the authored order. The saved session, rather than composition, owns answers. */
@Composable
fun ConceptLessonScreen(pack: ContentPack, lesson: PackLesson, session: PendingSession,
    busy: Boolean, errorMessage: String?, bookMood: BookMood, bookEventId: Int,
    bookPlayEvent: Boolean, reducedMotion: Boolean, onBookEventStarted: () -> Unit,
    onExit: () -> Unit, onSelect: (Int) -> Unit,
    onHint: () -> Unit, onReveal: () -> Unit, onSource: () -> Unit,
    onCheck: () -> Unit, onNext: () -> Unit, onFinish: () -> Unit) {
    val activities = session.conceptActivities()
    val item = activities[session.cursor]
    val isLast = session.cursor == activities.lastIndex
    val question = if (item.kind == "choice")
        lesson.questions.first { it.id == item.reference } else null
    val firstCorrect = question != null && item.firstSelected == question.correctIndex
    val needsCorrection = question != null && session.feedback && !firstCorrect && !item.corrected
    val canCheck = question != null && session.selected in item.choiceOrder.indices
    val canCorrect = needsCorrection && canCheck &&
        item.choiceOrder[session.selected] == question?.correctIndex
    val actionText = when {
        question != null && !session.feedback -> "Check answer"
        needsCorrection -> "Check correction"
        isLast -> "Finish lesson"
        else -> "Continue"
    }
    val actionEnabled = !busy && when {
        question != null && !session.feedback -> canCheck
        needsCorrection -> canCorrect
        else -> true
    }
    var sourceOpen by rememberSaveable(session.id, item.id) {
        mutableStateOf(item.sourceViewed)
    }
    val passage = pack.textPassages[item.reference]
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onExit, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Exit to path")
                }
                Spacer(Modifier.width(12.dp))
                Text(lesson.title, style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f))
            }
            Text("Step ${session.cursor + 1} of ${activities.size}",
                style = MaterialTheme.typography.labelLarge)
            LinearProgressIndicator(progress = { (session.cursor + 1f) / activities.size },
                modifier = Modifier.fillMaxWidth().height(6.dp))
            Text(if (pack.developmentOnly) "DEVELOPMENT DEMO · draft fixture"
                else "Local reviewed lesson", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary)
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                BookCompanion(bookMood, bookEventId, bookPlayEvent, reducedMotion,
                    onBookEventStarted, size = if (question == null) 116.dp else 88.dp)
            }
            when (item.kind) {
                "concept" -> {
                    Text(lesson.objective, style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { heading() })
                    Panel {
                        Text("One idea", style = MaterialTheme.typography.labelLarge)
                        Note(passage?.text ?: lesson.notes)
                    }
                    Text("Read the idea, then try a supported question.",
                        style = MaterialTheme.typography.bodyMedium)
                }
                "explanation" -> {
                    Text("What this means", style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { heading() })
                    Panel { Note(lesson.takeaway.ifBlank { passage?.text ?: lesson.notes }) }
                    Text("Next, use the idea in a new situation with fewer cues.",
                        style = MaterialTheme.typography.bodyMedium)
                }
                "choice" -> {
                    Text(when (question!!.difficulty) {
                        "recognise" -> "Supported practice"
                        "apply" -> "Apply the idea"
                        "distinguish" -> "Tell the ideas apart"
                        else -> "Recall the idea"
                    }, style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                    Text(question.prompt, style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.semantics { heading() })
                    item.choiceOrder.forEachIndexed { presented, original ->
                        AnswerChoice(question.choices[original],
                            session.selected == presented, original == question.correctIndex,
                            session.feedback && !needsCorrection, busy) {
                            if (!session.feedback || needsCorrection) onSelect(presented)
                        }
                    }
                    if (!session.feedback && question.hint.isNotBlank()) {
                        TextButton(onClick = onHint, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text(if (session.hinted) "Hint shown" else "Show hint")
                        }
                        if (session.hinted) Panel { Note(question.hint) }
                    }
                    if (!session.feedback) TextButton(onClick = onReveal,
                        modifier = Modifier.heightIn(min = 48.dp)) { Text("Show answer") }
                    if (session.feedback) {
                        Panel {
                            Text(if (firstCorrect) "Correct" else if (item.corrected)
                                "Corrected" else "Let's look again",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.semantics {
                                    liveRegion = LiveRegionMode.Polite
                                })
                            Text("Answer: ${question.choices[question.correctIndex]}",
                                style = MaterialTheme.typography.titleMedium)
                            if (!firstCorrect) Text(if (item.firstSelected >= 0)
                                "Your first answer: ${question.choices[item.firstSelected]}"
                                else "You revealed the answer",
                                style = MaterialTheme.typography.bodyMedium)
                            Note(question.explanation)
                            if (!firstCorrect) Text(if (item.corrected)
                                "You corrected this answer. Try it again later without a hint."
                                else "Choose the explained answer to continue.",
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                "audio" -> {
                    Text("Listen", style = MaterialTheme.typography.headlineMedium)
                    AudioPanel(pack, lesson)
                }
            }
            if (item.kind == "concept" || item.kind == "explanation" ||
                (item.kind == "choice" && session.feedback)) {
                OutlinedButton(onClick = {
                    sourceOpen = !sourceOpen
                    if (sourceOpen) onSource()
                }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(if (sourceOpen) "Hide source" else "Source · Learn more")
                }
                if (sourceOpen) {
                    if (question != null) AnswerSourcePanel(pack, lesson, question)
                    else if (passage != null) Panel {
                        Text("Source", style = MaterialTheme.typography.titleMedium)
                        Note(pack.textSources.getValue(passage.sourceId).edition)
                        Note("${passage.locator} · ${passage.wording} · v${passage.version}")
                        Note(passage.text)
                    }
                }
            }
        }
        Surface(shadowElevation = 4.dp) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
                if (errorMessage != null) Text(errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                Action(if (busy) "Saving…" else actionText, actionEnabled) {
                    when {
                        question != null && (!session.feedback || needsCorrection) -> onCheck()
                        isLast -> onFinish()
                        else -> onNext()
                    }
                }
            }
        }
    }
}
