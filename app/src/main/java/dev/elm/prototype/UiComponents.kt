package dev.elm.prototype

import android.animation.ValueAnimator
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class Direction { Garden, Editorial }
val LocalAtharReducedMotion = compositionLocalOf { false }
@Composable fun motionAllowed(): Boolean =
    !LocalAtharReducedMotion.current && ValueAnimator.areAnimatorsEnabled()

@Composable fun ElmTheme(direction: Direction, content: @Composable () -> Unit) {
    val editorial = direction == Direction.Editorial
    val scheme = lightColorScheme(
        primary = Color(if (editorial) 0xFF2848A0 else 0xFF2E7C58),
        onPrimary = Color.White,
        background = Color(if (editorial) 0xFFF5F2FF else 0xFFFFFCF5),
        surface = Color.White,
        surfaceVariant = Color(if (editorial) 0xFFE7E5F5 else 0xFFE6F0E8),
        primaryContainer = Color(if (editorial) 0xFFDCE3FF else 0xFFDCF4E5),
        onPrimaryContainer = Color(0xFF183C33),
        outline = Color(if (editorial) 0xFF7E86A6 else 0xFF708879),
        tertiary = Color(0xFF956022),
        tertiaryContainer = Color(0xFFFAEBCB),
        error = Color(0xFFB42318),
        onSurface = Color(0xFF183C33),
        onBackground = Color(0xFF183C33),
        secondaryContainer = Color(if (editorial) 0xFFFFD66E else 0xFFE6F0E8),
        onSecondaryContainer = Color(0xFF183C33))
    MaterialTheme(colorScheme = scheme,
        typography = Typography(
            bodyLarge = androidx.compose.ui.text.TextStyle(fontSize = 18.sp, lineHeight = 28.sp),
            titleMedium = androidx.compose.ui.text.TextStyle(fontSize = 20.sp, lineHeight = 28.sp),
            labelLarge = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, lineHeight = 21.sp)),
        content = content)
}

@Composable fun Title(text: String) {
    Text(text, style = MaterialTheme.typography.headlineLarge,
        modifier = Modifier.semantics { heading() })
}

@Composable fun Action(text: String, enabled: Boolean = true, click: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val offset by animateDpAsState(if (pressed && enabled) 4.dp else 0.dp,
        animationSpec = if (motionAllowed()) tween(if (pressed) 85 else 150) else snap(),
        label = "button press")
    Box(Modifier.fillMaxWidth().height(60.dp)) {
        Surface(Modifier.fillMaxWidth().height(56.dp).offset(y = 4.dp),
            color = if (enabled) Color(0xFF19573B) else Color(0xFFB4C4B8),
            shape = RoundedCornerShape(17.dp)) {}
        Surface(Modifier.fillMaxWidth().height(56.dp).offset(y = offset)
            .clip(RoundedCornerShape(17.dp))
            .clickable(enabled = enabled, interactionSource = interaction,
                indication = null, role = Role.Button, onClick = click),
            color = if (enabled) MaterialTheme.colorScheme.primary else Color(0xFFD9E2DA),
            shape = RoundedCornerShape(17.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = if (enabled) Color.White else Color(0xFF5C7164))
            }
        }
    }
}

@Composable fun Note(text: String) { Text(text, style = MaterialTheme.typography.bodyLarge) }
@Composable fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content)
    }
}

@Composable fun AnswerChoice(text: String, selected: Boolean, correct: Boolean, feedback: Boolean,
    busy: Boolean, onClick: () -> Unit) {
    val label = when {
        feedback && correct -> stringResource(R.string.correct_choice, text)
        feedback && selected -> stringResource(R.string.your_choice, text)
        selected -> stringResource(R.string.selected_prefix, text)
        else -> text
    }
    val fillTarget = when {
        feedback && correct -> Color(0xFFDCF4E5)
        feedback && selected -> Color(0xFFFFE8DE)
        selected -> Color(0xFFE9F2FF)
        else -> Color.White
    }
    val fill by animateColorAsState(fillTarget,
        animationSpec = if (motionAllowed()) tween(170) else snap(), label = "answer fill")
    val border = when {
        feedback && selected && !correct -> Color(0xFFB84D3D)
        feedback && correct -> Color(0xFF2E7C58)
        selected -> Color(0xFF3D70BA)
        else -> Color(0xFFB8C8BD)
    }
    val mark = when {
        feedback && correct -> "✓"
        feedback && selected -> "×"
        selected -> "●"
        else -> "○"
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val offset by animateDpAsState(if (pressed) 4.dp else 0.dp,
        animationSpec = if (motionAllowed()) tween(if (pressed) 85 else 150) else snap(),
        label = "answer press")
    Box(Modifier.fillMaxWidth().heightIn(min = 68.dp)) {
        Surface(Modifier.fillMaxWidth().heightIn(min = 64.dp).offset(y = 4.dp),
            color = border.copy(alpha = 0.55f), shape = RoundedCornerShape(18.dp)) {}
        Surface(Modifier.fillMaxWidth().heightIn(min = 64.dp).offset(y = offset)
            .clip(RoundedCornerShape(18.dp))
            .then(if (feedback) Modifier else Modifier.clickable(enabled = !busy,
                interactionSource = interaction, indication = null, role = Role.RadioButton,
                onClick = onClick))
            .semantics { this.selected = selected },
            color = fill, shape = RoundedCornerShape(18.dp),
            border = BorderStroke(if (selected || feedback && correct) 2.dp else 1.dp, border)) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp).heightIn(min = 44.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(mark, fontSize = 20.sp, color = border)
                Text(label, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
