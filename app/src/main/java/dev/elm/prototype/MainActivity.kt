package dev.elm.prototype

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.semantics.*
import androidx.media3.common.*
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.time.Clock
import java.time.ZoneId

class MainActivity : ComponentActivity() {
 private lateinit var db: LearningDatabase
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true; db = LearningDatabase.open(this); setContent { ElmApp(db) } }
 override fun onDestroy() { super.onDestroy(); db.close() }
}
enum class Direction { Garden, Editorial }
@Composable fun ElmTheme(direction: Direction, content: @Composable () -> Unit) {
 val editorial = direction == Direction.Editorial
 val scheme = lightColorScheme(primary = Color(if(editorial) 0xFF2848A0 else 0xFF236247), onPrimary = Color.White,
  background = Color(if(editorial) 0xFFF5F2FF else 0xFFFAF7EF), surface = Color(if(editorial) 0xFFFFFFFF else 0xFFFFFCF5),
  onSurface = Color(0xFF182E27), onBackground = Color(0xFF182E27), secondaryContainer = Color(if(editorial) 0xFFFFD66E else 0xFFE4EBDD), onSecondaryContainer = Color(0xFF182E27))
 MaterialTheme(colorScheme = scheme, typography = Typography(bodyLarge = androidx.compose.ui.text.TextStyle(fontSize = 18.sp, lineHeight = 28.sp)), content = content)
}
@Composable fun Title(text: String) { Text(text, style = MaterialTheme.typography.headlineLarge, fontFamily = FontFamily.Serif, modifier = Modifier.semantics { heading() }) }
@Composable fun Action(text: String, enabled: Boolean = true, click: () -> Unit) { Button(onClick=click, enabled=enabled, modifier=Modifier.fillMaxWidth().heightIn(min=56.dp), shape=RoundedCornerShape(16.dp)) { Text(text, fontSize=16.sp, modifier=Modifier.padding(4.dp)) } }
@Composable fun Note(text: String) { Text(text, style=MaterialTheme.typography.bodyLarge) }
@Composable fun Panel(content: @Composable ColumnScope.() -> Unit) { Surface(color=MaterialTheme.colorScheme.secondaryContainer, shape=RoundedCornerShape(24.dp)) { Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement=Arrangement.spacedBy(16.dp), content=content) } }

@Composable fun ElmApp(db: LearningDatabase) {
 val context=androidx.compose.ui.platform.LocalContext.current
 val prefs=remember { context.getSharedPreferences("prototype",0) }
 val scope=rememberCoroutineScope()
 val repo=remember { LearningRepository(db.learningDao(), Clock.systemUTC(), ZoneId.of(prefs.getString("study-zone", null) ?: ZoneId.systemDefault().id.also { prefs.edit().putString("study-zone",it).commit() })) }
 val completionFlow=remember(db) { db.learningDao().observeCompletions() }
 val completions by completionFlow.collectAsState(initial=emptyList())
 var page by rememberSaveable { mutableStateOf("Today") }
 var direction by rememberSaveable { mutableStateOf(Direction.Garden) }
 var q by rememberSaveable { mutableIntStateOf(0) }
 var selected by rememberSaveable { mutableIntStateOf(-1) }
 var feedback by rememberSaveable { mutableStateOf(false) }
 var busy by remember { mutableStateOf(false) }
 var error by remember { mutableStateOf<String?>(null) }
 val sourceFile=File(context.filesDir,"representative.mp3")
 val audioStore=remember { AudioResumeStore(prefs) }
 var imported by remember { mutableStateOf(audioStore.documentUri()) }
 var activeSource by rememberSaveable { mutableStateOf(audioStore.selected(sourceFile.exists())) }
 var fullRecording by rememberSaveable { mutableStateOf(prefs.getBoolean("full-recording", false)) }
 val sourceAudio=activeSource != AudioSource.Tone
 var loadedKey by remember { mutableStateOf<String?>(null) }
 var selectionRevision by remember { mutableIntStateOf(0) }
 val player=remember { ExoPlayer.Builder(context).build().apply { setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(), true); setHandleAudioBecomingNoisy(true) } }
 var playing by remember { mutableStateOf(false) }
 var position by remember { mutableLongStateOf(0L) }
 var duration by remember { mutableLongStateOf(0L) }
 val sourceUri=when(activeSource) {
  AudioSource.Tone -> Uri.parse("asset:///neutral-tone.wav")
  AudioSource.Document -> imported?.let(Uri::parse)
  AudioSource.Representative -> if(sourceFile.exists()) Uri.fromFile(sourceFile) else null
 }
 val launcher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if(uri!=null) { try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); audioStore.chooseDocument(uri.toString()); imported=uri.toString(); activeSource=AudioSource.Document; selectionRevision++; error=null } catch(e:Exception) { error="Audio access failed. Select the file again." } } }
 DisposableEffect(player) { val listener=object: Player.Listener {
  override fun onIsPlayingChanged(value:Boolean) { playing=value }
  override fun onPlayerError(e: PlaybackException) { error="Audio could not be opened. Re-select the local recording or use the bundled test tone." }
 }; player.addListener(listener); onDispose { player.removeListener(listener); player.release() } }
 LaunchedEffect(activeSource, imported, fullRecording, selectionRevision) {
  audioStore.save(loadedKey, player.currentPosition)
  player.pause()
  // Resolve only the explicit selection. A missing grant/file must never play another source.
  val uri=sourceUri
  val accessible=try {
   when(activeSource) {
    AudioSource.Tone -> true
    AudioSource.Document -> uri!=null && context.contentResolver.openFileDescriptor(uri,"r")?.use { true } == true
    AudioSource.Representative -> sourceFile.isFile
   }
  } catch(_:Exception) { false }
  if(uri==null || !accessible) {
   player.clearMediaItems(); loadedKey=null; position=0; duration=0
   error="Selected recording is unavailable. Select the local recording again."
  } else {
   val item=MediaItem.Builder().setUri(uri).apply {
    if(sourceAudio && !fullRecording) setClippingConfiguration(
     MediaItem.ClippingConfiguration.Builder().setStartPositionMs(0).setEndPositionMs(30000).build())
   }.build()
   val key=audioStore.key(activeSource,uri.toString(),fullRecording)
   val resume=audioStore.position(key,uri.toString()+":"+fullRecording)
   player.setMediaItem(item); player.prepare(); player.seekTo(resume)
   loadedKey=key; error=null; audioStore.select(activeSource)
   prefs.edit().putBoolean("full-recording",fullRecording).commit()
  }
 }
 LaunchedEffect(player) { while(true) { position=player.currentPosition; duration=player.duration.coerceAtLeast(0); delay(500) } }
 DisposableEffect(Unit) { val owner=context as ComponentActivity
  val obs=androidx.lifecycle.LifecycleEventObserver { _,event -> if(event==androidx.lifecycle.Lifecycle.Event.ON_STOP) { audioStore.save(loadedKey,player.currentPosition); player.pause() } }
  owner.lifecycle.addObserver(obs); onDispose { owner.lifecycle.removeObserver(obs) }
 }
 val scroll=rememberScrollState()
 LaunchedEffect(page,q) { scroll.scrollTo(0) }
 BackHandler(page!="Today") { page=when(page) { "Review" -> "Lesson"; "Lesson" -> "Learn"; else -> "Today" } }
 ElmTheme(direction) { Surface(Modifier.fillMaxSize(), color=MaterialTheme.colorScheme.background) {
 Column(Modifier.safeDrawingPadding()) {
  Row(Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=8.dp), horizontalArrangement=Arrangement.SpaceBetween) {
   Text("Athar", fontFamily=FontFamily.Serif, fontSize=32.sp, fontWeight=FontWeight.Bold)
   TextButton(onClick={page="Settings"}) { Text("Settings") }
  }
  Column(Modifier.weight(1f).verticalScroll(scroll).padding(horizontal=24.dp,vertical=16.dp), verticalArrangement=Arrangement.spacedBy(24.dp)) {
   Text("DEVELOPMENT DEMO · English fixtures", style=MaterialTheme.typography.labelMedium, color=MaterialTheme.colorScheme.primary)
   if(error!=null) Text(error!!, color=MaterialTheme.colorScheme.error, modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite })
   when(page) {
    "Today" -> TodayScreen(completions.isNotEmpty()) { page="Learn" }
    "Learn" -> { Title("A small first step"); Note("Prototype course · 1 neutral lesson"); Panel { Text("01 / Learn the controls",style=MaterialTheme.typography.titleLarge); Note("Try audio, answer two practice questions, and save your completion. This teaches app controls only."); Action("Open lesson") { page="Lesson" } }; Note("Nawaqid al-Islam is available for audio intake testing. Beginner course order awaits the review team.") }
    "Lesson" -> {
     LessonHeader()
     Panel {
      Text(if(activeSource==AudioSource.Document) "Selected local recording · technical test" else if(activeSource==AudioSource.Representative) "Injected development file · technical test" else "Bundled test tone · no speech",style=MaterialTheme.typography.titleMedium)
      Note(if(sourceAudio) "Local source for player testing. No transcript or quiz is derived from this recording." else "A locally generated tone for offline player testing. No teaching content.")
      Text("${position/1000}s / ${duration/1000}s")
      Action(if(playing) "Pause audio" else "Play audio", loadedKey!=null) { if(playing) { audioStore.save(loadedKey,player.currentPosition); player.pause() } else { if(player.playbackState==Player.STATE_ENDED) player.seekTo(0); player.play() } }
      OutlinedButton(onClick={player.seekTo((player.currentPosition-10000).coerceAtLeast(0))},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) { Text("Replay 10 seconds") }
      TextButton(onClick={activeSource=AudioSource.Tone}) { Text("Use bundled test tone") }
      if(imported!=null) TextButton(onClick={activeSource=AudioSource.Document}) { Text("Use selected local recording") }
      if(sourceFile.isFile) TextButton(onClick={activeSource=AudioSource.Representative}) { Text("Development: use injected file") }
      if(loadedKey==null && sourceAudio) TextButton(onClick={selectionRevision++}) { Text("Retry selected recording") }
      TextButton(onClick={launcher.launch(arrayOf("audio/*"))}) { Text("Select local recording") }
      if(sourceAudio) { Note(if(fullRecording) "Full selected recording" else "Technical range 00:00–00:30 · boundary unreviewed; not a study segment."); TextButton(onClick={fullRecording=!fullRecording}) { Text(if(fullRecording) "Test timestamp range" else "Open full recording") } }
     }
     Note("Fixture note: Play starts audio. Pause stops it temporarily. Replay moves back ten seconds.")
     Action("Try practice questions") { q=0;selected=-1;feedback=false;page="Review" }
    }
    "Review" -> {
     Title("Recall the controls"); Note("Practice ${q+1} of 2 · neutral fixture")
     val options=if(q==0) listOf("Pause audio","Settings","Finish lesson") else listOf("Opening the app","Selecting a recording","Finish lesson")
     val correct=if(q==0) 0 else 2
     Note(if(q==0) "Which control pauses playback?" else "Which action saves lesson completion?")
     options.forEachIndexed { i,text -> OutlinedButton(onClick={selected=i},enabled=!feedback&&!busy,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp).semantics { this.selected=selected==i }) { Text((if(selected==i) "Selected: " else "")+text,fontSize=18.sp) } }
     if(!feedback) Action("Check answer", selected>=0&&!busy) { busy=true; scope.launch { try { repo.answer(q,selected,selected==correct); feedback=true } catch(e:Exception){error="Answer could not be saved. Try again."} finally {busy=false} } }
     else { Panel { Text(if(selected==correct) "Correct · practice only" else "Let's look again",style=MaterialTheme.typography.titleLarge); Note(if(q==0) "Pause audio temporarily stops playback. You can resume using Play audio." else "Finish lesson saves your completed lesson on this device. Opening the app alone does not complete it."); Note("Source: reviewed-by-developer neutral controls fixture v1. No religious content. No mastery awarded.") }
      Action(if(q==0) "Next question" else if(busy) "Saving…" else "Finish lesson",!busy) { if(q==0) {q=1;selected=-1;feedback=false} else {busy=true;scope.launch {try {repo.finish("controls-fixture");page="Done"}catch(e:Exception){error="Completion could not be saved. Try again."}finally{busy=false}}} }
     }
    }
    "Done" -> { Title("A step completed"); Panel { Text("Completion saved", style=MaterialTheme.typography.titleLarge); Note("Your neutral practice lesson is recorded on this device. Close and reopen Athar to find the same progress.") }; Note("Participation is separate from knowledge mastery. This demo does not assess religious understanding."); Action("See progress") {page="Progress"} }
    "Progress" -> { Title("Your learning record"); Panel { Text("${completions.size} lesson completed",style=MaterialTheme.typography.headlineMedium); Note(if(completions.isEmpty()) "Complete the practice lesson to begin." else "Learn the controls · saved"); completions.forEach { Note("Study day: ${it.studyDay}\nTimezone: ${it.studyZone}") } }; Note("Guest progress stays on this device. Clearing app data or uninstalling removes it. Backup and sync are not available.") }
    "Settings" -> { Title("Make it comfortable"); Note("Visual direction · provisional"); Action(if(direction==Direction.Garden) "Compare editorial direction" else "Use garden direction") {direction=if(direction==Direction.Garden) Direction.Editorial else Direction.Garden}; Note("Text size follows Android settings. There are no custom motion effects in this prototype."); Note("Script test only · alphabet glyphs, not translated lesson text"); Text("ሀ ሁ ሂ ሃ ሄ ህ ሆ",fontSize=28.sp,lineHeight=40.sp); Text("ا ب ت ث ج ح خ",fontSize=28.sp,style=androidx.compose.ui.text.TextStyle(textDirection=TextDirection.Rtl),textAlign=TextAlign.Right,modifier=Modifier.fillMaxWidth()); Note("Tigrinya translation and native reading review remain pending."); Action("Back to Today") {page="Today"} }
   }
  }
  Row(Modifier.fillMaxWidth().padding(8.dp),horizontalArrangement=Arrangement.SpaceEvenly) { listOf("Today","Learn","Progress").forEach { name -> TextButton(onClick={page=name},modifier=Modifier.heightIn(min=48.dp).semantics { this.selected=page==name }) {Text(name)} } }
 }
 } }
}
@Composable fun TodayScreen(done:Boolean=false, open:()->Unit={}) { Title(if(done) "Keep your momentum" else "Room to learn"); Note("One calm step, at your pace."); Panel { Text("YOUR NEXT STEP",style=MaterialTheme.typography.labelLarge); Text("Learn the controls",style=MaterialTheme.typography.headlineMedium); Note("A two-question practice session. Explore before the reviewed course arrives."); Action(if(done) "Revisit practice" else "Begin practice",click=open) }; Note(if(done) "1 practice lesson completed · saved on device" else "Your progress begins with a completed session.") }
@Composable fun LessonHeader() { Title("Learn the controls"); Note("Listen, try, then check. This is development-only practice.") }
@Composable fun ReviewPreview() { Title("Recall the controls"); Note("Which control pauses playback?"); Action("Pause audio") {}; Panel {Note("Pause audio temporarily stops playback. Practice only; no mastery awarded.")} }
@Composable fun PreviewSurface(direction:Direction, screen:@Composable ()->Unit) {ElmTheme(direction) {Surface(color=MaterialTheme.colorScheme.background) {Column(Modifier.fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.spacedBy(24.dp)){ screen() }}}}
@Preview(name="Garden Today",showBackground=true) @Composable fun GardenToday()=PreviewSurface(Direction.Garden){TodayScreen()}
@Preview(name="Editorial Today",showBackground=true) @Composable fun EditorialToday()=PreviewSurface(Direction.Editorial){TodayScreen()}
@Preview(name="Garden Lesson",showBackground=true) @Composable fun GardenLesson()=PreviewSurface(Direction.Garden){LessonHeader();Panel{Action("Play audio") {}}}
@Preview(name="Editorial Lesson",showBackground=true) @Composable fun EditorialLesson()=PreviewSurface(Direction.Editorial){LessonHeader();Panel{Action("Play audio") {}}}
@Preview(name="Garden Review",showBackground=true) @Composable fun GardenReview()=PreviewSurface(Direction.Garden){ReviewPreview()}
@Preview(name="Editorial Review",showBackground=true) @Composable fun EditorialReview()=PreviewSurface(Direction.Editorial){ReviewPreview()}
