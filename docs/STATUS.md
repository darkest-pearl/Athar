# Milestone 1 status · 1 October 2026

## Outcome
First runnable Athar native Android prototype implemented and verified. App source commit c9a994ff779b998d12bfaed0addbf20696a67e12; CI setup fix 933f741. PR #1 (https://github.com/darkest-pearl/Athar/pull/1) merged as c49c015940c22a0c33d774a22319ca945c61600f; main CI run 36826407912 passed. This section records the milestone 1 snapshot. Current milestone 2 work is described in docs/MILESTONE2_STATUS.md.

Baseline main ff63889 was reviewed and pushed to darkest-pearl/Athar. The remote was initially empty. Configured Git identity was unchanged. No main protection or active branch rules reported by GitHub API. Workflow is persisted in AGENTS.md and Athar_Development_Cycle_Prompt.md. Athar is the confirmed name; original Elm briefs are preserved historical inputs. dev.elm.prototype remains a development package placeholder.

## Changed files
app/: Compose Today/course/Lesson/quiz/feedback/completion/Progress/Settings, Media3 playback, Room completion/attempt storage, two rule tests and one device persistence test. Six Compose previews and Settings compare Garden and Editorial; Garden recommended. content/: portable source inventory and neutral fixture schema. scripts/: inventory, fixture generator, publication validator and checks, UI-tree-driven emulator verifier. docs/: requirements, design, architecture, content policy and status. Gradle wrapper, pinned configuration, .gitignore and .github/workflows/android.yml supply local and CI builds. evidence/: actual PNG/XML captures, test summaries and final-diff review. No APK or large originals tracked.

## Source inventory
21 readable MP3s, 803,585,077 bytes, 50,205.353 seconds (13h 56m 45s). SHA-256 duplicates: 0. Filename number hints: 1–21, without gaps; sourceOrder remains null. Completeness is reported by Musab, not independently established from filenames. All 21 checksums were compared after intake and match; representative recording 1 checked again after playback and matches. No original renamed, moved, edited, uploaded or transcribed. Local absolute path stays in ignored .local/audio.json. Collection permission from Musab is recorded and inherited by recordings.

## Commands and actual results
- gradlew.bat assembleDebug lintDebug testDebugUnitTest: PASS. Two rule tests; lint 19 warnings, zero errors. APK app/build/outputs/apk/debug/app-debug.apk (11,731,757 bytes), SHA-256 in evidence/build-summary.json.
- gradlew.bat connectedDebugAndroidTest: PASS, one Room reopen/uniqueness/first-attempt test. Initial inferred-Boolean test signature failure fixed to Unit before passing rerun.
- python scripts/test_content.py: PASS, four tests. Development validation passes; fixtures and missing/stale authorization/reviews/source metadata are rejected for publication as tested. No actual religious approval represented by test records.
- python scripts/inventory.py <supplied-folder>, then a second inventory to .local/post-inventory.json and checksum comparison: PASS, 21 originals unchanged.
- adb install -r <debug-APK> / am start: PASS on ElmPrototype Android 16/API 36 x86_64, 360×800, 160dpi.
- python scripts/verify_prototype.py --adb <local-adb> --reset: core flow observed passing across runs: audio position advances, incorrect correction, two-question completion, process restart, no duplicate completion, offline quiz and bundled playback. Initial runs stopped on automation readiness and a short tone naturally ending; app behavior was not falsely called failed. Script corrected; screenshot evidence and observed results are in evidence/verification-flow.json.
- Same script --start-at-source: PASS, six source/accessibility checks. Original 1 plays in labeled 00:00–00:30 technical range offline; full original available; position preserved after restart; Ethiopic/Arabic alphabet fixtures render; full completion flow usable at 150% text with all animation scales zero. Emulator settings restored afterward. No semantic segment-boundary approval is implied.
- GitHub run 36825108072 at 933f741: PASS build/lint/unit/content and APK artifact. First CI run failed on obsolete SDK tools package; corrected to platform-tools without dropping checks. Latest PR and post-merge main CI are checked in the handoff.

Screenshots cover Today/Lesson/Review in both directions, correction, saved/restarted progress, original source player, enlarged text and script samples. Real 45-second emulator screen recording is local at .local/athar-prototype-flow.mp4. Emulator ran with -no-audio; decoder/playback position verified, audible sound quality not checked. Read-only document picker import is implemented but not exercised by this private-file emulator test.

## Scope and limitations
Milestone 1 is complete; this is a development prototype, not a reviewed release. No approved teaching lessons, Tigrinya translation, mastery, streak UI, bookmarks, downloads or sync. nextReview is a tested proposal helper, not integrated review UI. Runtime content-pack import remains milestone 2; fixture literals mirror the documented pack. Guest progress does not survive data clear/uninstall. Real reviewed Tigrinya readability, TalkBack spoken behavior, audio interruptions/headphones, physical hardware, tablet/landscape and wider device sizes remain unverified. Build/target versions are conservative; production upgrades and lint warning refinements are documented in evidence/REVIEW.md. No cloud services or public launch configured.

## Next action
The review team selects the first beginner course and prepares one complete, versioned source-linked sample lesson for language/religious review and the Sheikh's periodic checkpoint. Beginner curriculum and public package decisions remain distinct from the supplied Nawaqid intake test. Continue on a new task branch only for the next approved task after main is verified/synchronized.
