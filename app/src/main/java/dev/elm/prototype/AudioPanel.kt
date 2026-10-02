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
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.*
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import kotlin.math.roundToLong

/** True only after reading all local bytes. The URI hash used for resume keys is unrelated. */
fun verifyRecordingBytes(input: InputStream, recording: PackRecording): Boolean {
    val expected = recording.sha256 ?: return false
    val size = recording.bytes ?: return false
    val digest = MessageDigest.getInstance("SHA-256")
    var count = 0L
    val buffer = ByteArray(64 * 1024)
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        count += read
        if (count > size) return false
        digest.update(buffer, 0, read)
    }
    return count == size && digest.digest().joinToString("") { "%02x".format(it) } == expected
}

/** External audio is bound to the pack's recording ID; debug injection remains explicit. */
@Composable
fun AudioPanel(pack: ContentPack, lesson: PackLesson,
    passage: PackSourceRef.RecordingPassage? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("prototype", 0) }
    val store = remember { AudioResumeStore(prefs) }
    val segment = pack.segments.getValue(passage?.segmentId ?: lesson.segmentRef)
    val recording = pack.recordings.getValue(segment.recordingId)
    val recordingKey = "${pack.courseId}:${recording.id}"
    val representative = remember { File(context.filesDir, "representative.mp3") }
    var imported by remember(recordingKey) { mutableStateOf(store.documentUri(recordingKey)) }
    var source by remember(recordingKey) { mutableStateOf(store.selected(representative.isFile, recordingKey)) }
    var full by remember(recordingKey, passage) { mutableStateOf(if (passage != null) false else
        prefs.getBoolean("full-recording-$recordingKey",
        if (pack.courseId == "controls-course") prefs.getBoolean("full-recording-${recording.id}", false)
        else false)) }
    var revision by remember { mutableIntStateOf(0) }
    var loadedKey by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var playing by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    var verifying by remember { mutableStateOf(false) }
    var controlsExpanded by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0) }
    var duration by remember { mutableLongStateOf(0) }
    var dragPreview by remember { mutableStateOf<Float?>(null) }
    val player = remember { ExoPlayer.Builder(context).build().apply {
        setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(), true)
        setHandleAudioBecomingNoisy(true)
    } }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            verifying = true
            scope.launch {
                try {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    val valid = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            if (recording.sha256 != null) verifyRecordingBytes(input, recording)
                            else pack.developmentOnly
                        } == true
                    }
                    if (!valid) {
                        error = context.getString(if (recording.sha256 == null)
                            R.string.audio_identity_missing else R.string.audio_wrong_file)
                        if (uri.toString() != imported) runCatching {
                            context.contentResolver.releasePersistableUriPermission(uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    } else {
                        store.chooseDocument(uri.toString(), recordingKey)
                        imported = uri.toString()
                        source = AudioSource.Document
                        revision++
                        error = null
                    }
                } catch (_: Exception) { error = context.getString(R.string.audio_grant_failed) }
                finally { verifying = false }
            }
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
    LaunchedEffect(recordingKey, source, imported, full, revision, lesson.segmentRef, passage) {
        store.save(loadedKey, player.currentPosition)
        player.pause()
        loaded = false
        dragPreview = null
        val access = withContext(Dispatchers.IO) { try {
            when (source) {
                AudioSource.Tone -> context.assets.open(recording.relativePath).use { input ->
                    if (recording.sha256 != null)
                        if (verifyRecordingBytes(input, recording)) 1 else 2
                    else if (pack.developmentOnly) 1 else 3
                }
                AudioSource.Document -> if (uri == null) 0 else
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        if (recording.sha256 != null)
                            if (verifyRecordingBytes(input, recording)) 1 else 2
                        else if (pack.developmentOnly) 1 else 3
                    } ?: 0
                AudioSource.Representative -> if (passage != null) 2
                    else if (pack.developmentOnly && representative.isFile) 1 else 0
            }
        } catch (_: Exception) { 0 } }
        if (uri == null || access != 1) {
            player.clearMediaItems()
            loadedKey = null
            loaded = false
            position = 0
            duration = 0
            error = context.getString(when (access) {
                2 -> R.string.audio_wrong_file
                3 -> R.string.audio_identity_missing
                else -> R.string.audio_missing
            })
        } else {
            val start = passage?.startMs ?: if (source == AudioSource.Representative) 0L else segment.startMs
            val end = passage?.endMs ?: if (source == AudioSource.Representative) 30_000L else segment.endMs
            val media = MediaItem.Builder().setUri(uri).apply {
                if (!full) setClippingConfiguration(MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(start).setEndPositionMs(end).build())
            }.build()
            val resumeScope = "${pack.courseId}:${if (full) recording.id else lesson.segmentRef}"
            val key = if (passage != null && !full) null
                else store.key(source, uri.toString(), full, resumeScope)
            val resume = if (key == null) 0L else store.position(key,
                if (pack.courseId == "controls-course") uri.toString() + ":" + full else "",
                if (pack.courseId == "controls-course")
                    store.key(source, uri.toString(), full, if (full) recording.id else lesson.segmentRef)
                else null)
            player.setMediaItem(media)
            player.prepare()
            player.seekTo(resume)
            loadedKey = key
            loaded = true
            store.select(source, recordingKey)
            if (passage == null) prefs.edit().putBoolean("full-recording-$recordingKey", full).commit()
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
            AudioSource.Document -> if (pack.developmentOnly) R.string.document_label
                else R.string.verified_document_label
            AudioSource.Representative -> R.string.representative_label
        }
        Text(context.getString(label), style = MaterialTheme.typography.titleMedium)
        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
        if (verifying) Note(context.getString(R.string.audio_verifying))
        if (source == AudioSource.Document && recording.sha256 == null && pack.developmentOnly)
            Note(context.getString(R.string.audio_unverified_fixture))
        val canSeek = loaded && duration > 0 && player.isCurrentMediaItemSeekable
        val shownPosition = dragPreview?.let { (it * duration).roundToLong().coerceIn(0, duration) }
            ?: position.coerceIn(0, duration.coerceAtLeast(0))
        Slider(value = dragPreview ?: if (duration > 0) position.toFloat()
            .div(duration).coerceIn(0f, 1f) else 0f,
            onValueChange = { dragPreview = it },
            onValueChangeFinished = {
                dragPreview?.let { fraction ->
                    player.seekTo((fraction * duration).roundToLong().coerceIn(0, duration))
                }
                dragPreview = null
            },
            enabled = canSeek,
            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .semantics {
                    stateDescription = context.getString(R.string.audio_timeline_announcement,
                        formatAudioTime(shownPosition),
                        if (duration > 0) formatAudioTime(duration) else context.getString(R.string.audio_unknown_time))
                })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatAudioTime(shownPosition))
            Text(if (duration > 0) formatAudioTime(duration) else context.getString(R.string.audio_unknown_time))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0)) },
                enabled = canSeek, modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    .semantics { contentDescription = context.getString(R.string.audio_skip_back_accessible) },
                contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text(context.getString(R.string.audio_skip_back))
            }
            Button(onClick = {
                if (playing) { store.save(loadedKey, player.currentPosition); player.pause() }
                else { if (player.playbackState == Player.STATE_ENDED) player.seekTo(0); player.play() }
            }, enabled = loaded && !verifying,
                modifier = Modifier.weight(1.25f).heightIn(min = 56.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text(context.getString(if (playing) R.string.pause_audio else if (passage != null && !full)
                    R.string.play_passage else R.string.play_audio))
            }
            OutlinedButton(onClick = { player.seekTo((player.currentPosition + 10_000)
                .coerceAtMost(duration)) }, enabled = canSeek,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    .semantics { contentDescription = context.getString(R.string.audio_skip_forward_accessible) },
                contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text(context.getString(R.string.audio_skip_forward))
            }
        }
        if (passage != null && !full) TextButton(onClick = { player.seekTo(0) },
            enabled = canSeek, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(context.getString(R.string.replay_passage))
        }
        if (passage != null && source != AudioSource.Tone) TextButton(onClick = { full = !full },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(context.getString(if (full) R.string.return_to_passage else R.string.full_recording))
        }
        if (!loaded && source != AudioSource.Tone)
            TextButton(onClick = { revision++ }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(context.getString(R.string.retry_recording))
            }
        if (!loaded || controlsExpanded) TextButton(onClick = { picker.launch(arrayOf("audio/*")) },
            enabled = !verifying, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(context.getString(R.string.select_recording))
        }
        if (source != AudioSource.Tone && passage == null)
            Note(context.getString(if (full) R.string.full_recording_label else R.string.technical_range_label))
        TextButton(onClick = { controlsExpanded = !controlsExpanded },
            modifier = Modifier.heightIn(min = 48.dp)) {
            Text(context.getString(if (controlsExpanded) R.string.audio_hide_controls
                else R.string.audio_more_controls))
        }
        if (controlsExpanded) {
            Note(context.getString(if (pack.developmentOnly) R.string.audio_test_note else R.string.audio_source_note))
            Note(context.getString(R.string.recording_id, recording.id))
            TextButton(onClick = { source = AudioSource.Tone },
                modifier = Modifier.heightIn(min = 48.dp)) { Text(context.getString(R.string.use_tone)) }
            if (imported != null) TextButton(onClick = { source = AudioSource.Document },
                modifier = Modifier.heightIn(min = 48.dp)) {
                Text(context.getString(R.string.use_document))
            }
            if (representative.isFile) TextButton(onClick = { source = AudioSource.Representative },
                modifier = Modifier.heightIn(min = 48.dp)) {
                Text(context.getString(R.string.use_representative))
            }
            if (source != AudioSource.Tone && passage == null) TextButton(onClick = { full = !full },
                modifier = Modifier.heightIn(min = 48.dp)) {
                Text(context.getString(if (full) R.string.technical_range else R.string.full_recording))
            }
        }
    }
}
