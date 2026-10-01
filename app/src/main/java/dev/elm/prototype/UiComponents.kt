package dev.elm.prototype

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected

enum class Direction { Garden, Editorial }

@Composable fun ElmTheme(direction: Direction, content: @Composable () -> Unit) {
    val editorial = direction == Direction.Editorial
    val scheme = lightColorScheme(
        primary = Color(if (editorial) 0xFF2848A0 else 0xFF236247),
        onPrimary = Color.White,
        background = Color(if (editorial) 0xFFF5F2FF else 0xFFFAF7EF),
        surface = Color(if (editorial) 0xFFFFFFFF else 0xFFFFFCF5),
        onSurface = Color(0xFF182E27),
        onBackground = Color(0xFF182E27),
        secondaryContainer = Color(if (editorial) 0xFFFFD66E else 0xFFE4EBDD),
        onSecondaryContainer = Color(0xFF182E27)
    )
    MaterialTheme(colorScheme = scheme,
        typography = Typography(bodyLarge = androidx.compose.ui.text.TextStyle(fontSize = 18.sp, lineHeight = 28.sp)),
        content = content)
}

@Composable fun Title(text: String) {
    Text(text, style = MaterialTheme.typography.headlineLarge, fontFamily = FontFamily.Serif,
        modifier = Modifier.semantics { heading() })
}

@Composable fun Action(text: String, enabled: Boolean = true, click: () -> Unit) {
    Button(onClick = click, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        shape = RoundedCornerShape(16.dp)) {
        Text(text, fontSize = 16.sp, modifier = Modifier.padding(4.dp))
    }
}

@Composable fun Note(text: String) { Text(text, style = MaterialTheme.typography.bodyLarge) }

@Composable fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content)
    }
}

@Composable
fun AnswerChoice(text: String, selected: Boolean, correct: Boolean, feedback: Boolean,
    busy: Boolean, onClick: () -> Unit) {
    val label = when {
        feedback && correct -> stringResource(R.string.correct_choice, text)
        feedback && selected -> stringResource(R.string.your_choice, text)
        selected -> stringResource(R.string.selected_prefix, text)
        else -> text
    }
    if (feedback) {
        Surface(modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            color = if (selected || correct) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
            Text(label, fontSize = 18.sp, modifier = Modifier.padding(16.dp))
        }
    } else {
        OutlinedButton(onClick = onClick, enabled = !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                .semantics { this.selected = selected }) {
            Text(label, fontSize = 18.sp)
        }
    }
}
