package dev.elm.prototype

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Keeps the next study action reachable while long content and source panels scroll. */
@Composable
fun LessonScaffold(stage: LessonStage, lessonTitle: String, developmentOnly: Boolean,
    stepLabel: String? = null,
    onExit: () -> Unit, actionText: String, actionEnabled: Boolean = true,
    saving: Boolean = false, errorMessage: String? = null,
    onAction: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val compactHeight = LocalConfiguration.current.screenHeightDp < 500
    val stageText = stringResource(R.string.lesson_stage_progress, stage.ordinal + 1,
        LessonStage.entries.size)
    val fixtureText = stringResource(if (developmentOnly) R.string.development_label
        else R.string.local_pack_label)
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp,
            vertical = if (compactHeight) 0.dp else 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onExit, modifier = Modifier.heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 0.dp)) {
                    Text(stringResource(R.string.exit_to_path))
                }
                Spacer(Modifier.width(12.dp))
                Text(lessonTitle, style = MaterialTheme.typography.titleMedium,
                    maxLines = if (compactHeight) 1 else 2,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (compactHeight) Text(stepLabel ?: stageText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
            }
            if (!compactHeight) Row(Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stageText, style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
                if (stepLabel != null) Text(stepLabel, style = MaterialTheme.typography.labelLarge)
            }
            val progress by animateFloatAsState((stage.ordinal + 1f) / LessonStage.entries.size,
                label = "lesson scaffold progress")
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth()
                .height(6.dp), color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant)
            Text(fixtureText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary)
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = if (compactHeight) 8.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
        Surface(color = MaterialTheme.colorScheme.background,
            shadowElevation = 4.dp) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp,
                vertical = if (compactHeight) 4.dp else 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (errorMessage != null) Text(errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                if (compactHeight) Button(onClick = onAction, enabled = actionEnabled,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(if (saving) stringResource(R.string.study_saving) else actionText)
                } else Action(if (saving) stringResource(R.string.study_saving) else actionText,
                    actionEnabled, onAction)
            }
        }
    }
}
