package dev.elm.prototype

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class BookMood(val description: String) {
    Idle("waiting"), Welcome("welcoming"), Explain("explaining"), Think("thinking"),
    Encourage("encouraging"), Correct("happy about the answer"),
    Complete("celebrating completion"), Return("welcoming you back")
}

private data class BookPose(
    val leftAngle: Float, val rightAngle: Float, val leftX: Float = 0f,
    val leftY: Float = 0f, val rightX: Float = 0f, val rightY: Float = 0f,
    val pupilX: Float = 0f, val pupilY: Float = 0f
)

private fun pose(mood: BookMood) = when (mood) {
    BookMood.Idle -> BookPose(0f, 0f)
    BookMood.Welcome, BookMood.Return -> BookPose(-22f, 4f, -4f, -9f)
    BookMood.Explain -> BookPose(2f, -24f, rightX = -7f, rightY = -10f)
    BookMood.Think -> BookPose(24f, 0f, leftX = 18f, leftY = -23f,
        pupilX = 2f, pupilY = -2f)
    BookMood.Encourage -> BookPose(0f, -20f, rightX = 5f, rightY = -7f)
    BookMood.Correct -> BookPose(-18f, 18f, leftY = -8f, rightY = -8f)
    BookMood.Complete -> BookPose(-28f, 28f, leftX = -8f, rightX = 8f,
        leftY = -13f, rightY = -13f)
}

/**
 * An original editable layer rig. Each drawing function is a separate physical part.
 * Only a new committed UI event plays a reaction; restoring saved feedback shows its pose.
 */
@Composable
fun BookCompanion(mood: BookMood, eventId: Int = 0, playEvent: Boolean = false,
    reducedMotion: Boolean = false, onEventStarted: () -> Unit = {},
    size: Dp = 132.dp) {
    val context = LocalContext.current
    val lifecycle = (context as? LifecycleOwner)?.lifecycle
    var resumed by remember(lifecycle) {
        mutableStateOf(lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) ?: true)
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            resumed = lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) ?: true
        }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }
    val motion = !reducedMotion && ValueAnimator.areAnimatorsEnabled()
    val target = pose(mood)
    val spec = if (motion) tween<Float>(260) else snap()
    val leftAngle by animateFloatAsState(target.leftAngle, spec, label = "book left hand")
    val rightAngle by animateFloatAsState(target.rightAngle, spec, label = "book right hand")
    val leftX by animateFloatAsState(target.leftX, spec, label = "book left hand x")
    val leftY by animateFloatAsState(target.leftY, spec, label = "book left hand y")
    val rightX by animateFloatAsState(target.rightX, spec, label = "book right hand x")
    val rightY by animateFloatAsState(target.rightY, spec, label = "book right hand y")
    val pupilX by animateFloatAsState(target.pupilX, spec, label = "book gaze x")
    val pupilY by animateFloatAsState(target.pupilY, spec, label = "book gaze y")
    val body = remember { Animatable(0f) }
    val wave = remember { Animatable(0f) }
    val bookmark = remember { Animatable(0f) }
    val feet = remember { Animatable(0f) }
    val eyelids = remember { Animatable(0f) }
    val settle = remember { Animatable(0f) }

    LaunchedEffect(eventId, mood, motion, resumed) {
        body.snapTo(0f); wave.snapTo(0f); bookmark.snapTo(0f); feet.snapTo(0f)
        if (eventId <= 0 || !playEvent || !resumed) return@LaunchedEffect
        onEventStarted()
        if (!motion) return@LaunchedEffect
        when (mood) {
            BookMood.Welcome, BookMood.Return -> {
                repeat(2) {
                    wave.animateTo(-22f, tween(150))
                    wave.animateTo(13f, tween(150))
                }
                wave.animateTo(0f, tween(160))
            }
            BookMood.Explain -> {
                wave.animateTo(-13f, tween(180))
                wave.animateTo(0f, tween(220))
            }
            BookMood.Think -> {
                wave.animateTo(9f, tween(240))
                wave.animateTo(0f, tween(180))
            }
            BookMood.Encourage -> {
                wave.animateTo(-18f, tween(190))
                wave.animateTo(0f, tween(240))
            }
            BookMood.Correct -> {
                launch {
                    body.animateTo(-11f, tween(170))
                    body.animateTo(0f, tween(280))
                }
                launch {
                    wave.animateTo(-14f, tween(190))
                    wave.animateTo(0f, tween(260))
                }
                bookmark.animateTo(11f, tween(170))
                bookmark.animateTo(0f, tween(280))
            }
            BookMood.Complete -> {
                body.animateTo(5f, tween(160))
                launch {
                    feet.animateTo(-6f, tween(250))
                    feet.animateTo(0f, tween(330))
                }
                launch {
                    wave.animateTo(-18f, tween(250))
                    wave.animateTo(0f, tween(450))
                }
                launch {
                    bookmark.animateTo(19f, tween(370))
                    bookmark.animateTo(-7f, tween(310))
                    bookmark.animateTo(0f, tween(320))
                }
                body.animateTo(-26f, tween(310))
                body.animateTo(0f, tween(480))
            }
            BookMood.Idle -> Unit
        }
    }
    LaunchedEffect(mood, motion, resumed) {
        eyelids.snapTo(0f)
        if (!motion || !resumed) return@LaunchedEffect
        while (true) {
            delay(3900)
            eyelids.animateTo(1f, tween(85))
            eyelids.animateTo(0f, tween(120))
        }
    }
    LaunchedEffect(mood, motion, resumed) {
        settle.snapTo(0f)
        if (!motion || !resumed || mood != BookMood.Idle) return@LaunchedEffect
        while (true) {
            delay(5200)
            settle.animateTo(-1.2f, tween(600))
            settle.animateTo(0f, tween(600))
        }
    }
    val cover = Color(0xFF236247)
    val coverDark = Color(0xFF164732)
    val page = Color(0xFFFFF3D9)
    val ink = Color(0xFF18342A)
    Canvas(Modifier.size(size).semantics {
        contentDescription = "Book companion: " + mood.description
    }) {
        val unitScale = minOf(this.size.width, this.size.height) / 160f
        withTransform({ scale(unitScale, unitScale, pivot = Offset.Zero) }) {
            drawBookShadow()
            drawBookFeet(page, ink, feet.value)
            withTransform({ translate(top = body.value + settle.value) }) {
                drawBookPages(page, ink)
                drawBookCover(cover, coverDark)
                drawBookBookmark(Color(0xFFD89A50), bookmark.value)
                drawBookEyes(page, ink, pupilX, pupilY, eyelids.value)
                drawBookMouth(ink, mood)
            }
            drawBookHand(page, ink, 24f + leftX, 99f + leftY + body.value + settle.value,
                leftAngle + wave.value, left = true)
            drawBookHand(page, ink, 136f + rightX, 99f + rightY + body.value + settle.value,
                rightAngle - wave.value, left = false)
        }
    }
}

private fun DrawScope.drawBookShadow() {
    drawOval(Color(0x33283C32), topLeft = Offset(43f, 149f), size = Size(78f, 8f))
}

private fun DrawScope.drawBookFeet(page: Color, ink: Color, kick: Float) {
    listOf(53f, 88f).forEachIndexed { index, x ->
        val shift = if (index == 0) kick else -kick
        drawOval(ink, topLeft = Offset(x - 2f, 140f + shift), size = Size(26f, 13f))
        drawOval(page, topLeft = Offset(x, 139f + shift), size = Size(22f, 11f))
    }
}

private fun DrawScope.drawBookPages(page: Color, ink: Color) {
    drawRoundRect(ink, Offset(44f, 25f), Size(84f, 117f), CornerRadius(14f))
    drawRoundRect(page, Offset(48f, 27f), Size(77f, 112f), CornerRadius(11f))
    for (x in 115..121 step 3)
        drawLine(Color(0xFFDDCEAF), Offset(x.toFloat(), 31f), Offset(x.toFloat(), 134f), 1f)
}

private fun DrawScope.drawBookCover(cover: Color, dark: Color) {
    drawRoundRect(cover, Offset(37f, 20f), Size(84f, 119f), CornerRadius(14f))
    drawRoundRect(dark, Offset(37f, 20f), Size(13f, 119f), CornerRadius(12f))
    drawRoundRect(Color(0xFF4A8768), Offset(52f, 29f), Size(62f, 100f), CornerRadius(8f),
        style = Stroke(width = 1.4f))
    drawCircle(Color(0xFFFFE5A9), 4f, Offset(83f, 112f))
    drawLine(Color(0xFFFFE5A9), Offset(74f, 112f), Offset(92f, 112f), 1.5f)
}

private fun DrawScope.drawBookBookmark(color: Color, angle: Float) {
    withTransform({ rotate(angle, pivot = Offset(107f, 24f)) }) {
        val ribbon = Path().apply {
            moveTo(102f, 22f); lineTo(113f, 22f); lineTo(113f, 51f)
            lineTo(107.5f, 46f); lineTo(102f, 51f); close()
        }
        drawPath(ribbon, color)
    }
}

private fun DrawScope.drawBookEyes(page: Color, ink: Color, lookX: Float,
    lookY: Float, blink: Float) {
    listOf(69f, 97f).forEach { x ->
        drawOval(page, Offset(x - 9f, 59f), Size(18f, 22f))
        drawCircle(ink, 4.5f, Offset(x + lookX, 70f + lookY))
        if (blink > 0f) drawRoundRect(Color(0xFF236247),
            Offset(x - 10f, 57f), Size(20f, 24f * blink), CornerRadius(4f))
    }
}

private fun DrawScope.drawBookMouth(ink: Color, mood: BookMood) {
    if (mood in setOf(BookMood.Welcome, BookMood.Correct, BookMood.Complete)) {
        drawOval(ink, Offset(78f, 90f), Size(13f, 11f))
        drawOval(Color(0xFFE8A08E), Offset(81f, 96f), Size(7f, 3f))
    } else if (mood == BookMood.Explain) {
        drawOval(ink, Offset(81f, 91f), Size(7f, 9f))
    } else if (mood == BookMood.Think) {
        drawOval(ink, Offset(82f, 92f), Size(5f, 4f))
    } else {
        val path = Path().apply {
            moveTo(77f, 92f)
            quadraticBezierTo(84f, 102f, 92f, 92f)
        }
        drawPath(path, ink, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
    }
}

private fun DrawScope.drawBookHand(page: Color, ink: Color, x: Float, y: Float,
    angle: Float, left: Boolean) {
    withTransform({ rotate(angle, pivot = Offset(x, y)) }) {
        drawOval(ink, Offset(x - 12f, y - 7f), Size(24f, 17f))
        drawOval(page, Offset(x - 10f, y - 8f), Size(20f, 15f))
        val direction = if (left) -1f else 1f
        for (finger in -1..1) {
            val fx = x + direction * 6f + finger * 5f
            drawOval(page, Offset(fx - 2f, y - 13f), Size(5f, 10f))
        }
    }
}
