package dev.elm.prototype

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** The saved session owns every answer and cursor. Animation only presents committed state. */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ConceptLessonScreen(pack: ContentPack, lesson: PackLesson, session: PendingSession,
    busy: Boolean, errorMessage: String?, bookMood: BookMood, bookEventId: Int,
    bookPlayEvent: Boolean, reducedMotion: Boolean, onBookEventStarted: () -> Unit,
    onExit: () -> Unit, onSelect: (Int) -> Unit,
    onHint: () -> Unit, onReveal: () -> Unit, onSource: () -> Unit,
    onCheck: () -> Unit, onNext: () -> Unit, onFinish: () -> Unit) {
    val activities = session.conceptActivities()
    val item = activities[session.cursor]
    val question = if (item.kind in setOf("choice", "replay"))
        lesson.questions.first { it.id == item.reference } else null
    val firstCorrect = question != null && item.firstSelected == question.correctIndex
    val needsCorrection = question != null && session.feedback && !firstCorrect && !item.corrected
    val canCheck = question != null && session.selected in item.choiceOrder.indices
    val canCorrect = needsCorrection && canCheck &&
        item.choiceOrder[session.selected] == question?.correctIndex
    val authored = activities.count { it.kind != "replay" }.coerceAtLeast(1)
    val completedAuthored = activities.take(session.cursor + 1).count { it.kind != "replay" }
    val fraction = completedAuthored.toFloat() / authored
    val progress by animateFloatAsState(fraction,
        animationSpec = if (motionAllowed()) tween(280) else snap(),
        label = "lesson progress")
    val isLast = session.cursor == activities.lastIndex
    val actionText = when {
        question != null && !session.feedback -> stringResource(R.string.concept_check)
        needsCorrection -> stringResource(R.string.concept_check_correction)
        isLast -> stringResource(R.string.concept_finish)
        else -> stringResource(R.string.concept_continue)
    }
    val actionEnabled = !busy && when {
        question != null && !session.feedback -> canCheck
        needsCorrection -> canCorrect
        else -> true
    }
    var sourceOpen by rememberSaveable(session.id, item.id) { mutableStateOf(false) }
    val animateActivity = motionAllowed()
    var activityShown by remember(item.id, animateActivity) { mutableStateOf(!animateActivity) }
    LaunchedEffect(item.id, animateActivity) { activityShown = true }
    val passage = pack.textPassages[item.reference]
    val selectedRequester = remember(item.id) { BringIntoViewRequester() }
    LaunchedEffect(item.id, session.feedback, session.selected) {
        if (question != null && session.feedback && session.selected >= 0) {
            delay(320)
            selectedRequester.bringIntoView()
        }
    }
    if (sourceOpen) ModalBottomSheet(onDismissRequest = { sourceOpen = false }) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.concept_source_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f))
                TextButton(onClick = { sourceOpen = false },
                    modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.concept_close_source))
                }
            }
            Column(Modifier.fillMaxWidth()
                .heightIn(max = (LocalConfiguration.current.screenHeightDp - 180)
                    .coerceAtLeast(160).dp)
                .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (question != null) {
                Text(stringResource(R.string.concept_full_explanation),
                    style = MaterialTheme.typography.titleMedium)
                Note(question.explanation)
                AnswerSourcePanel(pack, lesson, question)
            }
            else if (passage != null) {
                Text(pack.textSources.getValue(passage.sourceId).edition)
                Text("${passage.locator} · ${passage.wording} · v${passage.version}")
                Note(passage.text)
            }
            }
        }
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onExit, modifier = Modifier.heightIn(min = 48.dp)
                .semantics { contentDescription = "Exit to path" }) {
                Text(stringResource(R.string.concept_exit))
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(lesson.title, style = MaterialTheme.typography.labelLarge,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (item.kind == "replay") stringResource(R.string.concept_replay_step)
                    else stringResource(R.string.concept_step, completedAuthored, authored),
                    style = MaterialTheme.typography.labelSmall)
            }
        }
        LinearProgressIndicator(progress = { progress },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(8.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AnimatedVisibility(activityShown,
                enter = if (animateActivity) fadeIn(tween(240)) +
                    slideInHorizontally(tween(240)) { it / 10 } else fadeIn(tween(0))) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (item.kind == "concept" || item.kind == "explanation") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BookCompanion(bookMood, bookEventId, bookPlayEvent, reducedMotion,
                        onBookEventStarted, size = 132.dp)
                    Text(if (item.kind == "concept") stringResource(R.string.concept_one_idea)
                        else stringResource(R.string.concept_what_it_means),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f).semantics { heading() })
                }
                Panel {
                    Text(if (item.kind == "concept") lesson.objective
                        else lesson.takeaway.ifBlank { lesson.objective },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Note(if (item.kind == "concept") passage?.text ?: lesson.notes
                        else lesson.takeaway.ifBlank { passage?.text ?: lesson.notes })
                }
                Text(if (item.kind == "concept") stringResource(R.string.concept_then_practice)
                    else stringResource(R.string.concept_then_apply),
                    style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { sourceOpen = true; onSource() },
                    modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.concept_view_source))
                }
            } else if (question != null) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BookCompanion(bookMood, bookEventId, bookPlayEvent, reducedMotion,
                        onBookEventStarted, size = 76.dp)
                    Column {
                        Text(when {
                            item.kind == "replay" -> stringResource(R.string.concept_replay)
                            question.difficulty == "recognise" -> stringResource(R.string.concept_supported)
                            else -> stringResource(R.string.concept_apply)
                        }, style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary)
                        if (item.kind == "replay") Text(stringResource(R.string.concept_replay_note),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text(question.prompt, fontSize = 25.sp, lineHeight = 31.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() })
                item.choiceOrder.forEachIndexed { presented, original ->
                    AnswerChoice(question.choices[original],
                        session.selected == presented, original == question.correctIndex,
                        session.feedback, busy,
                        firstWrong = session.feedback && !firstCorrect &&
                            item.firstSelected == original,
                        correctionAllowed = needsCorrection,
                        indicator = ('A' + presented).toString(),
                        modifier = if (session.selected == presented)
                            Modifier.bringIntoViewRequester(selectedRequester) else Modifier) {
                        if (!session.feedback || needsCorrection) onSelect(presented)
                    }
                }
                if (session.feedback) Spacer(Modifier.height(28.dp))
                if (!session.feedback) {
                    if (question.hint.isNotBlank()) {
                        TextButton(onClick = onHint, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text(if (session.hinted) stringResource(R.string.concept_hint_shown)
                                else stringResource(R.string.concept_show_hint))
                        }
                        if (session.hinted) Panel { Note(question.hint) }
                    }
                    TextButton(onClick = onReveal, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.reveal_answer))
                    }
                }
            } else if (item.kind == "audio") {
                Text(stringResource(R.string.listen_stage_title),
                    style = MaterialTheme.typography.headlineMedium)
                AudioPanel(pack, lesson)
            }
            }
            }
        }
        Surface(shadowElevation = 6.dp, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AnimatedVisibility(question != null && session.feedback,
                    enter = if (motionAllowed()) fadeIn(tween(260)) + expandVertically(tween(260))
                        else fadeIn(tween(0)),
                    exit = if (motionAllowed()) fadeOut(tween(150)) + shrinkVertically(tween(150))
                        else fadeOut(tween(0))) {
                    if (question != null) Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val outcome = when {
                        firstCorrect -> stringResource(R.string.concept_correct)
                        item.corrected -> stringResource(R.string.concept_corrected)
                        else -> stringResource(R.string.concept_try_again)
                    }
                    Text(outcome, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (firstCorrect || item.corrected)
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    if (!firstCorrect) Text(if (item.firstSelected >= 0)
                        stringResource(R.string.concept_first_answer,
                            question.choices[item.firstSelected])
                        else stringResource(R.string.concept_revealed),
                        style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.concept_answer_why,
                        question.choices[question.correctIndex], question.explanation),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3, overflow = TextOverflow.Ellipsis)
                    TextButton(onClick = { sourceOpen = true; onSource() },
                        modifier = Modifier.heightIn(min = 40.dp)) {
                        Text(stringResource(R.string.concept_view_source))
                    }
                    }
                }
                if (errorMessage != null) Text(errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                Action(if (busy) stringResource(R.string.study_saving) else actionText,
                    actionEnabled) {
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
