# Athar Android prototype
Confirmed name: Athar. Local development demo; no approved religious lessons. Open this directory in Android Studio or VS Code.

## Build
JDK 17 and Android SDK 35 are required. Set JAVA_HOME and create ignored local.properties with sdk.dir pointing at your SDK.

```powershell
$env:JAVA_HOME='path-to-jdk-17'
.\gradlew.bat assembleDebug testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
python scripts/test_content.py
python scripts/validate_content.py content/fixture-pack.json
```
APK: app/build/outputs/apk/debug/app-debug.apk. Install with adb install -r followed by adb shell am start -n dev.elm.prototype/.MainActivity.

## Try the complete flow
Today → Begin practice → Open lesson → Play audio / Pause → Try practice questions → select an answer → Check answer → read feedback → Next question → Check answer → Finish lesson → See progress. Force-stop the app and reopen; Progress should still show one completion. Repeating the lesson must leave the count at one.

The 12-second bundled test tone contains no speech. To test an original, copy one recording to your test device and use Select local recording. The app requests read-only persisted document access. Original audio is independent of the neutral app-control quiz. The 00:00–00:30 technical range is explicitly unreviewed; Open full original preserves context. Playback pauses when the app backgrounds; position is saved for reopening the same media.

Settings compares two visual directions. Compose previews cover Today, Lesson and Review in each. System text scale controls text size; all content scrolls and no custom animations are used.

## Audio inventory
Originals stay outside Git. Local path is in ignored .local/audio.json. Portable source manifest: content/source-manifest.json. Reproduce without editing sources:

```powershell
python -m pip install --target .tools/python mutagen==1.48.1
python scripts/inventory.py 'your-recordings-folder'
```
No recording was uploaded or transcribed. SHA-256, durations and filename sequence hints are recorded. Order is not treated as curriculum approval.

See docs/STATUS.md for actual results, docs/CONTENT.md for the publication gate, and docs/ARCHITECTURE.md for platform decisions.

## Verification evidence
Actual checks, limitations and screenshots: docs/STATUS.md and evidence/. The script can be resumed for source/accessibility checks with --start-at-source. The full --reset flow clears only the isolated prototype app data on the connected test device; do not use it on a device whose guest progress you wish to retain. A real interaction recording remains in ignored .local/athar-prototype-flow.mp4.
