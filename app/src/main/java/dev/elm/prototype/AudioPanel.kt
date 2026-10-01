package dev.elm.prototype

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.*
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import java.io.File

/** External audio is bound to the pack's recording ID; debug injection remains explicit. */
@Composable
fun AudioPanel(pack: ContentPack, lesson: PackLesson) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("prototype", 0) }
    val store = remember { AudioResumeStore(prefs) }
    val segment = pack.segments.getValue(lesson.segmentRef)
    val recording = pack.recordings.getValue(segment.recordingId)
    val representative = remember { File(context.filesDir, "representative.mp3") }
    var imported by remember(recording.id) { mutableStateOf(store.documentUri(recording.id)) }
    var source by remember(recording.id) { mutableStateOf(store.selected(representative.isFile, recording.id)) }
    var full by remember(recording.id) { mutableStateOf(prefs.getBoolean("full-recording-${recording.id}", false)) }
    var revision by remember { mutableIntStateOf(0) }
    var loadedKey by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var playing by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0) }
    var duration by remember { mutableLongStateOf(0) }
    val player = remember { ExoPlayer.Builder(context).build().apply {
        setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(), true)
        setHandleAudioBecomingNoisy(true)
    } }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                store.chooseDocument(uri.toString(), recording.id)
                imported = uri.toString()
                source = AudioSource.Document
                revision++
                error = null
            } catch (_: Exception) { error = context.getString(R.string.audio_grant_failed) }
        }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
            override fun onPlayerError(e: PlaybackException) { error = context.getString(R.string.audio_failed) }
        }
        player.addListener(listener)
        onDispose { store.save(loadedKey, player.currentPosition); player.removeListener(listener); player.release() }
    }
    val uri = when (source) {
        AudioSource.Tone -> Uri.parse("asset:///${recording.relativePath}")
        AudioSource.Document -> imported?.let(Uri::parse)
        AudioSource.Representative -> if (representative.isFile) Uri.fromFile(representative) else null
    }
    LaunchedEffect(source, imported, full, revision, lesson.segmentRef) {
        store.save(loadedKey, player.currentPosition)
        player.pause()
        val accessible = try {
            when (source) {
                AudioSource.Tone -> { context.assets.open(recording.relativePath).close(); true }
                AudioSource.Document -> uri != null &&
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { true } == true
                AudioSource.Representative -> representative.isFile
            }
        } catch (_: Exception) { false }
        if (uri == null || !accessible) {
            player.clearMediaItems()
            loadedKey = null
            position = 0
            duration = 0
            error = context.getString(R.string.audio_missing)
        } else {
            val technicalOverride = source == AudioSource.Representative ||
                (source == AudioSource.Document && pack.developmentOnly)
            val start = if (technicalOverride) 0L else segment.startMs
            val end = if (technicalOverride) 30_000L else segment.endMs
            val media = MediaItem.Builder().setUri(uri).apply {
                if (!full) setClippingConfiguration(MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(start).setEndPositionMs(end).build())
            }.build()
            val scope = if (full) recording.id else lesson.segmentRef
            val key = store.key(source, uri.toString(), full, scope)
            val resume = store.position(key, uri.toString() + ":" + full, store.key(source, uri.toString(), full))
            player.setMediaItem(media)
            player.prepare()
            player.seekTo(resume)
            loadedKey = key
            store.select(source, recording.id)
            prefs.edit().putBoolean("full-recording-${recording.id}", full).commit()
            error = null
        }
    }
    LaunchedEffect(player) {
        while (true) {
            position = player.currentPosition.coerceAtLeast(0)
            duration = player.duration.coerceAtLeast(0)
            delay(500)
        }
    }
    DisposableEffect(player) {
        val owner = context as ComponentActivity
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                store.save(loadedKey, player.currentPosition)
                player.pause()
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    Panel {
        val label = when (source) {
            AudioSource.Tone -> R.string.tone_label
            AudioSource.Document -> R.string.document_label
            AudioSource.Representative -> R.string.representative_label
        }
        Text(context.getString(label), style = MaterialTheme.typography.titleMedium)
        Note(context.getString(if (pack.developmentOnly) R.string.audio_test_note else R.string.audio_source_note))
        Note(context.getString(R.string.recording_id, recording.id))
        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
        Text("${position / 1000}s / ${duration / 1000}s")
        Action(context.getString(if (playing) R.string.pause_audio else R.string.play_audio),
            loadedKey != null) {
            if (playing) { store.save(loadedKey, player.currentPosition); player.pause() }
            else { if (player.playbackState == Player.STATE_ENDED) player.seekTo(0); player.play() }
        }
        OutlinedButton(onClick = { player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0)) },
            enabled = loadedKey != null, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(context.getString(R.string.replay_audio))
        }
        TextButton(onClick = { source = AudioSource.Tone }) { Text(context.getString(R.string.use_tone)) }
        if (imported != null) TextButton(onClick = { source = AudioSource.Document }) {
            Text(context.getString(R.string.use_document))
        }
        if (representative.isFile) TextButton(onClick = { source = AudioSource.Representative }) {
            Text(context.getString(R.string.use_representative))
        }
        if (loadedKey == null && source != AudioSource.Tone)
            TextButton(onClick = { revision++ }) { Text(context.getString(R.string.retry_recording)) }
        TextButton(onClick = { picker.launch(arrayOf("audio/*")) }) {
            Text(context.getString(R.string.select_recording))
        }
        if (source != AudioSource.Tone) {
            Note(context.getString(if (full) R.string.full_recording_label else R.string.technical_range_label))
            TextButton(onClick = { full = !full }) {
                Text(context.getString(if (full) R.string.technical_range else R.string.full_recording))
            }
        }
    }
}
