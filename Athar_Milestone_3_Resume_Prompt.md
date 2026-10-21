# Athar — resume the interrupted milestone 3

Prepared 2 October 2026. Repository reviewed at `780db35b55db2e6fac1097174814b2d07328f4b6`.

**Latest user direction:** Athar must feel like a guided, gamified learning app in the spirit of Duolingo. The current utility-style experience is too crude. A draggable audio timeline and a substantially improved learning journey are required in this continuation. This direction expands the original milestone's UI/pilot task and takes precedence where the earlier prompt described a smaller polish pass.

## Verified checkpoint

- PR #8, course-scoped/versioned learning state, merged as `2a2b5ec592fb6d8dc0fc636100d73964f0833efb`.
- PR #9, durable lesson/review sessions, merged as `780db35b55db2e6fac1097174814b2d07328f4b6`.
- Both corresponding main CI runs succeeded; latest run: https://github.com/darkest-pearl/Athar/actions/runs/36921978750.
- Remote main is at `780db35`. No source-passage branch or task-3 PR was visible remotely during this review. The report describes unfinished local edits after the usage-limit interruption.
- The merged code includes Room versions 5 and 6, course-scoped records, durable ordered sessions, immutable first-submission feedback, and transactional study finalization. Local connected/emulator verification is documented in the repository; this reviewer did not independently run Android tests or inspect the unfinished local edits.
- Main is still unprotected. Follow the established PR/check workflow and report the actual protection status accurately.

## Prompt for local Codex

Resume Athar from the usage-limit interruption. Read this prompt alongside `Athar_Milestone_3_Codex_Prompt.md`, `AGENTS.md`, and `Athar_Development_Cycle_Prompt.md`; the latest user direction above expands the remaining work. Tasks 1 and 2 are merged. Preserve and continue the local work on Task 3, then build the guided learning experience, prepare the pilot packet, and complete the final handoff. Do not restart initialization or repeat completed work without a concrete reason.

First inspect `git status`, the active branch, recent commits, remotes, and the unfinished diff. Preserve every existing file, including ignored local QA helpers. Check whether the remote has advanced since the reviewed checkpoint. Verify the current branch contains the source-passage edits reported at interruption and resume from their actual state. Keep local-only work clearly distinguished from pushed/merged changes. Stage explicit intended paths; local helper listings in an editor do not imply they belong in Git.

Before completing the milestone, reproduce and address the loading-state issues identified below. Preserve the source-passage work while doing so. If a separate fix PR is needed, use a separate worktree based on verified main; then integrate its merged result into the feature branch without losing the unfinished edits. Continue ordinary authorized commit/push/PR/merge/main-sync actions with required checks and reviews.

### Loading-state findings to reproduce

1. **Review reconciliation can receive a placeholder empty completion list.** At the reviewed commit, `MainActivity.kt` collects `observeLessonCompletions()` with `initial = emptyList()` and immediately calls `ensureSeeded(pack, completions)` from a `LaunchedEffect`. `ReviewRepository.ensureSeeded` deletes active-course states whose concepts are absent from candidates. If this initial empty list reaches reconciliation before the first actual Room result, saved states are deleted and later reseeded at stage 0. This can reset due times and invalidate the schedule snapshot of an unfinished review.

   Reproduce with a completed lesson and a concept already advanced beyond stage 0. Delay the initial data emission deterministically or otherwise control startup ordering. Cold-start/recreate the activity, then compare course, source, stage, dueAt, learnedAt, and lastReviewedAt before and after. Also restart with a partial due batch and verify it remains resumable. A direct DAO reopen test alone does not exercise the Compose loading path.

   Reconciliation must use authoritative, consistently loaded database state. Represent loading separately from a genuinely empty result, or read the qualifying completions inside the reconciliation transaction. A temporary UI placeholder must never drive deletion. Ensure delayed/stale observations cannot mutate a more recent persisted schedule. Keep legitimate content-update reconciliation working and add a focused regression.

2. **Quiz recovery can render before pending-session validation loads.** At the reviewed commit, pending sessions also initially appear as an empty list. The Quiz guard only rejects material when a pending session is found and incompatible; otherwise it renders `QuizScreen`. That screen indexes `lesson.questions[index]` directly using saved UI state. A restored cursor combined with changed or shorter active material can render the wrong lesson or index outside its questions before Room provides the session.

   Reproduce restoration with a saved cursor beyond the replacement lesson's question count and deliberately delayed pending-session loading. Show a loading state until the exact durable session/material is resolved; then restore its committed feedback or show the existing unavailable-material recovery. Never grade a replacement version or index an unvalidated cursor. Cover the initial render after activity/process recreation, not only the repository's missing-source exception.

These are code-level findings with startup timing to verify locally. Report the controlled reproduction and fix; do not present this remote review as an independently executed device test. Keep the changes bounded to loading, reconciliation, and recovery.

### Finish Task 3 — source-passage replay

Continue the existing typed source-reference and answer-source-panel implementation. Re-read Task 3 of the original milestone prompt for its complete acceptance criteria.

Resolve the question's exact text or audio passage in initial quiz feedback and later review. Audio may reference another segment/recording than the lesson's main source. Use the intended original-file start/end positions, stop at the passage boundary, and keep passage playback independent of full-recording mode and lesson resume offsets. Replay passage should start at the passage beginning. Resolve neutral text passages as text.

Verify local recording identity against the source manifest/checksum when supplied, handle wrong-file and missing/revoked access without replacing the last working association, and keep technical overrides explicitly labeled. Keep originals outside Git and external services. Preserve offline use and the documented foreground-only playback behavior.

Complete Python/Android contract validation and regression checks. Test distinguishable neutral passages at nonzero timestamps, another recording, previously enabled full mode, saved positions, text references, and offline reselection. Run affected build/lint/unit/content/device/migration checks; commit and merge only the verified final revision, then verify synchronized main and its CI.

### Required player feature — draggable audio timeline

Add an actual seekable timeline as part of the source/player branch. Musab explicitly requested the familiar interaction found in YouTube and music apps.

- Show elapsed and total time below a horizontal track with a visible thumb; use `mm:ss` and `h:mm:ss` for longer recordings.
- Let the learner drag the thumb backward or forward and preview the target time. Commit the seek when the drag completes; preserve the current playing/paused state unless a clearly documented player rule requires otherwise.
- Maintain separate drag-preview and player-position state so playback polling cannot pull the thumb out from under the user's finger. Reflect normal playback progress continuously when not dragging.
- Use the actual playable duration, handle unknown duration/loading and non-seekable media, and clamp targets safely. Provide accessible seek actions and time announcements, with a usable touch target at enlarged font size.
- A clipped lesson or question passage uses a segment-relative timeline from zero to its playable duration. Full recording uses its own full timeline. Convert correctly when displaying original source timestamps. Seeking must respect the active passage boundary and preserve separate resume positions.
- Keep prominent Play/Pause and skip-back/forward controls together with the track. Verify seeking near the beginning, middle, and end while paused and playing, then verify reopening and switching playback modes.
- A moved audio thumb represents playback position. It must not automatically grant lesson completion, study credit, or claim the learner has understood the material.

Use native Compose/Media3 APIs compatible with the pinned dependencies. Keep this player component reusable for lesson and answer-passage playback; no video subsystem is required for the current audio curriculum.

### Expanded Task 4A — `feat/guided-learning-experience`

Build a learner-facing product flow that makes progress obvious. Use Duolingo's guided course path, short activities, immediate feedback, and integrated review as design references, with Athar's own visual identity and content model. Connect the experience to actual persisted progress, schedules, and sessions.

First update `docs/DESIGN.md` with the intended screens, component states, typography/color/spacing tokens, motion, and learning flow. Use the installed UI UX Pro Max guidance for native Compose, then implement and inspect the actual screens. Resolve routine design choices from the existing Garden direction without waiting for another color preference. The development content can remain neutral while the complete learner journey is built.

**Home and course path**

- Make the first screen answer: What am I learning? What should I do next? How far have I progressed? Include a prominent Start/Continue lesson action, a compact daily goal/streak display, and the current unit/course position.
- Replace the flat course list as the primary journey with a polished vertical path grouped into units. Give completed, current, available upcoming, and review nodes distinct shapes/icons/states with clear labels. Tapping a node shows its title, objective, duration when known, and Start/Continue/Practice action.
- Build units/order/prerequisites from validated content metadata and extend the contract deliberately if needed. Demonstrate the states with enough genuinely implemented neutral lesson/activities; avoid inventing religious curriculum, titles, or segment boundaries.
- Calculate course/unit progress from actual version-matching lesson completion. Current-node continuation uses the durable pending session. Completed nodes remain useful for replay/practice. Any prerequisite restriction must follow explicit content relationships and explain the next available step.
- Surface short due review on the path or Today, with its own clear action; preserve the existing bounded catch-up policy.

**Guided lesson**

- Use a focused sequence: brief objective → listening passage → concise takeaway when supplied → short knowledge checks → answer explanations/source replay → completion and next step. Put a lesson-stage progress indicator at the top so the learner sees how much remains.
- Keep one main action visible for each stage, clear Back/Exit behavior, and a way to continue interrupted work. Keep setup/import/debug controls in the appropriate settings/development surface.
- Make listening a useful learning stage, with the new player and exact source. Continue is deliberate; skipping/seeking audio alone does not establish completion. Persist the additional lesson-stage state needed for reliable recovery using an explicit migration if required.
- Retain the working choice questions and improve their presentation and feedback. Add another short practice interaction only where it helps the available reviewed material, with a bounded neutral fixture to verify it. Religious practice items still require reviewed wording and answers.
- Provide immediate, encouraging feedback and a clear explanation after incorrect answers, with exact source access. Revisit a weak concept using the existing assistance/review rules without overwriting earlier evidence or representing immediate retries as independent recall.

**Motivation and visual quality**

- Show daily goal completion, streak, unit progress, and a few milestone badges derived from real saved study/completion events. Make unit/lesson completion visibly rewarding and point directly to the next learning step. Present these as learning participation and progress.
- Develop a coherent, polished native visual system: expressive but readable typography, purposeful icons, strong hierarchy, richer path/lesson components, thoughtful spacing, and responsive bottom actions/navigation. Keep text and terminology appropriate for teenagers and younger adults, with long Tigrinya and mixed-script content supported when supplied.
- Add smooth transitions for entering a lesson, selected answers, saved feedback, lesson-stage progress, and completing a node. Respect reduced motion and system animation settings, with equivalent static states.
- Derive completed/current states, badges, progress bars, and review counts from real data. Loading, empty course, changed content, missing audio, save failures, and interrupted sessions must have finished user-facing states.

**Acceptance is the actual learning experience**

Capture a continuous emulator walkthrough in which a first-time user can see the course path, start a lesson, scrub audio, work through its stages, answer questions, see explanatory feedback, complete the lesson, watch real path progress update, and continue to the next step or short review. Show the same session resuming after interruption.

Inspect Home/path, lesson intro, player, practice, feedback, completion, and progress at phone sizes, landscape where supported, enlarged text, and reduced motion. Include actual screenshots/recording tied to the verified build. Check that primary actions remain discoverable and reachable as text grows. A compilation result and a color change alone do not demonstrate this requested learning flow.

Merge this coherent feature through the established PR/check/main cycle before the pilot handoff branch.

### Finish Task 4B — first-lesson pilot preparation

Prepare the reviewer packet, interface-copy review sheet, and physical-device checklist described in the original prompt. Use the existing authoring template and established source permission. Fill actual supplied content and reviews only; missing course choice, teaching wording, boundaries, or approvals remain visibly pending. Do not choose the beginner course solely because its recordings are available.

If a reviewed lesson is available, integrate that one lesson and verify its source-linked flow. Otherwise complete the engineering with neutral fixtures and hand over the packet without waiting indefinitely. The current development screenshots do not establish native Tigrinya reading comfort or audible accuracy on a real phone.

### Final handoff

Update the current milestone status, architecture, and evidence around the actual final behavior. End with the APK path and SHA-256, final commit, merged PRs, latest main CI, local/remote synchronization, relevant verification evidence, a recorded guided-learning walkthrough, reviewer packet, physical-device checklist, and concrete remaining human inputs. Preserve the agreed periodic and prelaunch Sheikh review. Complete this milestone before proposing an additional feature milestone.

## Review references

- PR #8: https://github.com/darkest-pearl/Athar/pull/8
- PR #9: https://github.com/darkest-pearl/Athar/pull/9
- UI loading/recovery: https://github.com/darkest-pearl/Athar/blob/780db35b55db2e6fac1097174814b2d07328f4b6/app/src/main/java/dev/elm/prototype/MainActivity.kt
- Review reconciliation: https://github.com/darkest-pearl/Athar/blob/780db35b55db2e6fac1097174814b2d07328f4b6/app/src/main/java/dev/elm/prototype/ReviewRepository.kt
- Quiz rendering: https://github.com/darkest-pearl/Athar/blob/780db35b55db2e6fac1097174814b2d07328f4b6/app/src/main/java/dev/elm/prototype/LearningScreens.kt
- Session repository: https://github.com/darkest-pearl/Athar/blob/780db35b55db2e6fac1097174814b2d07328f4b6/app/src/main/java/dev/elm/prototype/SessionRepository.kt
- Local emulator evidence: https://github.com/darkest-pearl/Athar/blob/780db35b55db2e6fac1097174814b2d07328f4b6/docs/evidence/m3-sessions/README.md
- Duolingo learning-path reference: https://blog.duolingo.com/new-duolingo-home-screen-design/
- Duolingo targeted review reference: https://blog.duolingo.com/how-to-review-lessons-on-duolingo/
- Compose slider reference: https://developer.android.com/develop/ui/compose/components/slider
- Media3 player seeking/duration reference: https://developer.android.com/reference/androidx/media3/common/Player
