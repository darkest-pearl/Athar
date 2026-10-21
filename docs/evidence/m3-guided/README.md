# Guided learning verification - 21 October 2026

This evidence corresponds to the guided-learning feature branch build before its PR. The development pack contains neutral test material only. The original Nawaqid recordings were not used or changed.

## Executed checks

- `gradlew.bat :app:assembleDebug :app:lintDebug :app:testDebugUnitTest :app:connectedDebugAndroidTest --console=plain`: passed. The Android 16/API 36 `niqdah_qa_small_api36` emulator ran 21 connected tests, including Room 1-to-7 migration, guided-unit parser, prerequisite ordering, and journey-stage reopen/repeat checks. Lint reported warnings and no errors.
- `python scripts/test_content.py`: 17 passed.
- `python scripts/validate_content.py content/qa-passage-pack.json`: valid as **development** content. This does not pass editorial publication review.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`, SHA-256 `B75472B68ABE8FFFFAD07A17516F34F3A4B0E3E5228C8AA1912E0872DEF46219`.
- Continuous emulator screen video: ignored local `.local/athar-guided-final.mp4`, 3,943,402 bytes, SHA-256 `B581AA9A4EA97214E5992AFC858D58156839AB5FDEF135416EFD829DB15155AA`, duration 87.3922 seconds. MP4 structure has a video track and no audio track. The video remains outside Git.

## Observed Android journey

The 360x640dp emulator showed the real 0/3 course path, first-unit intro, listening player, takeaway, two practice questions, saved wrong-answer correction, exact source passage replay, completion, 1/3 path update, first-unit 1/1 state, a study-day credit, and the next lesson action. Paused drag seeking changed the lesson player's position from 0:00 to 0:02 of 0:06 without finishing the lesson. Source A replay used original-file 0:06-0:08 and source B used 0:02-0:04. Force-stop/reopen restored the same saved incorrect feedback. The second lesson was completed manually and the third opened through the real next action. No due review existed in this first-day fixture state.

Screenshots: `path-before.png`, `lesson-intro.png`, `lesson-player-before-seek.png`, `lesson-player-scrubbed.png`, `lesson-takeaway.png`, `practice-question.png`, `practice-selected.png`, `feedback-incorrect.png`, `feedback-source-a.png`, `resume-card.png`, `feedback-resumed.png`, `feedback-source-b.png`, `lesson-complete.png`, `path-after.png`, `progress-after.png`. The screenshot sequence and local continuous video were inspected against [the interaction specification](../../design/athar-learning-experience.md).

Responsive checks: `short-home.png` and `short-progress.png` show 360x640dp; the temporary 360x800dp emulator size was also inspected. At 150% font, `large-text-home.png` retains the Home action, and `large-text-intro.png` reaches Continue after one scroll. In 1280x720 landscape, `landscape-intro.png`, `landscape-intro-scrolled.png`, and `landscape-action.png` show the action reachable after two scrolls. With system animation scales at zero, `reduced-motion-listen.png` shows the immediately rendered listening state. Emulator size, density, font scale, orientation, and animation settings were restored after QA.

The emulator ran with `-no-audio`: player position and passage bounds were checked, audible fidelity was not. No physical-phone, TalkBack speech, headset/audio-focus, or native Tigrinya reading review was performed. The walkthrough video has no sound track. A short phone can require scrolling in Path; landscape intro requires two scrolls. These are visible reachability limits, not claims of native-language usability.
