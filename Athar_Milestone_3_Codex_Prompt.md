# Athar — reliable sessions and first-lesson pilot

Prepared 1 October 2026. Reviewed repository: `darkest-pearl/Athar` at `09d55e41361ff9eb8914a20eb96c107faedb21cd`.

## Review outcome

GitHub confirms that PRs #2–7 merged and main CI run 36858610755 completed successfully for the reviewed commit. The repository contains the content importer, two neutral lessons, review scheduling, study-day streaks, explicit database migrations, and verification evidence. The screenshots show a readable provisional Garden interface.

The review examined code, repository metadata, documentation, and screenshots. It did not independently run the Android application, inspect Musab's original recordings, or verify audible playback. GitHub CI currently runs the build, lint, JVM tests, and Python content tests; connected Android tests are documented as local checks. Main was unprotected at this snapshot.

Three concrete findings guide this milestone:

1. **Updated lessons can lose their review queue.** `ReviewState` has only `conceptId` as its primary key. `ensureSeeded` uses insert-with-ignore, while `eligibleStates` requires the active lesson and question versions. After a lesson version changes, the old state becomes ineligible and blocks insertion of its replacement. Re-completing that concept can therefore fail to restore its scheduled reviews. Persistence also lacks course scope even though an imported pack can change courses.
2. **An unfinished review session is lost on activity recreation.** The batch, due-item snapshot, cursor, assistance flags, and review session ID use `remember`. Answer events and schedule changes are persisted separately, while study credit is finalized after the last Next/Finish action. Activity recreation can discard the information needed to finish the original batch; already answered items have moved out of the due queue. The manifest does not suppress ordinary orientation recreation. This is a code-level finding requiring device reproduction.
3. **Question-specific source replay is unfinished.** `PackQuestion.sourceRef` is retained as serialized JSON, but `ReviewQuestionScreen` opens `AudioPanel(pack, lesson)` without resolving it. Playback uses the lesson segment and saved full-recording mode. A question referring to another segment or a narrower answer passage will not select that passage. Fixture-text references also currently lead to the audio player.

The rest of this document is an implementation prompt for local Codex. It is a bounded continuation, not a new application specification.

## Operating instructions

Continue Athar through the four tasks below. First inspect actual current main and any active work; this review refers to `09d55e4`, and later fixes may already exist. Preserve local changes and saved learner data. Read `AGENTS.md`, `Athar_Development_Cycle_Prompt.md`, `docs/STATUS.md`, `docs/MILESTONE2_STATUS.md`, `docs/ARCHITECTURE.md`, `docs/CONTENT.md`, `docs/DESIGN.md`, and `content/CONTRACT.md`.

Use one coherent task branch at a time. Reproduce the relevant issue, implement the fix, run the affected gates, review the final diff, commit, push, open/update a PR, wait for checks on its latest revision, merge when eligible, synchronize main, and confirm main CI before starting the next branch. Existing authorization covers these routine development and version-control actions. Respect required reviews and repository protections; do not bypass them or change repository settings as part of these code tasks. Clearly distinguish local commits, pushed changes, and merged PRs.

Keep Kotlin, Compose, Room, and Media3. Refactor screen state and repositories where required by these behaviors. Use the installed design guidance when polishing affected screens, and the Android QA skill for device checks. Check official documentation against the pinned dependency versions before selecting APIs; a toolchain upgrade is not an objective of this milestone.

Continue engineering with neutral fixtures while the content team prepares the first lesson. Collection permission is already established. Preserve original audio and keep recordings, device paths, credentials, and build outputs out of Git. Actual teaching text, translations, source boundaries, answers, and reviews must come from the agreed content workflow. Do not manufacture approval metadata or mark a development fixture as approved teaching content.

## Task 1 — `fix/versioned-learning-state`

Reproduce the review disappearance before changing the schema:

- Complete a fixture lesson at version 1 and confirm its concepts are seeded.
- Import a valid higher content version with a higher lesson/question version and the same concept IDs.
- Complete the updated lesson, advance an injected clock, and observe whether its concepts can be reviewed.

Implement explicit course and version identity across the affected persistence and lookup paths. Lessons, question attempts, bookmarks, review state, and recording associations must not inherit another course's data because short IDs happen to match. Study-day credit remains learner-wide, so studying two courses on one date still earns one day.

Separate historical answer/completion evidence from the active review source. Define a deterministic reconciliation policy for an updated or removed question, a revised lesson, and the same concept taught in more than one lesson. Keep one active scheduled item per concept within its documented scope. An obsolete source must not permanently block a valid current source. Preserve historical events and completion versions. A material correction must not silently retain an unsupported mastery claim; unchanged material may retain its schedule through an explicit, tested policy. Document the policy and its user-visible effect.

Define what the existing importer supports when replacing the active course. Implement scoped identities for supported course replacement; if any case remains unsupported, reject it clearly before activation instead of accepting a pack that aliases existing progress. Keep a small local record of previously accepted immutable identities/fingerprints if needed to prevent conflicts after switching away and back. A full multi-course catalog UI is outside this task.

Use an explicit Room migration from version 4. Preserve known legacy fixture identities as `controls-course`. For older imported data whose course cannot be inferred reliably, preserve the records and report the ambiguity rather than inventing an attribution. Update any relevant SharedPreferences/audio-key migration deliberately. Prevent lesson/question content changes under an already accepted immutable identity and version.

Acceptance evidence:

- The lesson-upgrade reproduction now yields a valid current review, including after reopening the app.
- Two neutral courses with deliberately overlapping lesson/question/concept/recording IDs do not share progress or selected audio.
- Teaching a concept in two lessons does not create duplicate due items.
- Historical answers and completions survive; repeat submissions remain idempotent.
- An installed milestone-2 database upgrades without uninstalling or clearing data. Unknown legacy data is preserved.

## Task 2 — `fix/resumable-study-sessions`

Reproduce review loss with activity recreation, including after the final answer is saved but before the batch is finalized. Cover a partially completed lesson quiz too.

Move session ownership out of transient composable variables. Persist enough information to resume the same logical lesson/review session: identity, course/content references, ordered item identities, the original due-item qualification, saved answers, hint/reveal usage, and finalization status. Keep lightweight presentation state in an appropriate screen state holder. Restore from durable session/event data after process restart; storing a whole content pack in an Android saved-state Bundle is unnecessary.

The UI must render the committed answer result. Re-entering a question, returning with Back, double tapping, or retrying a save must not show a new result while the database silently keeps a different first submission. Starting a deliberate repeat lesson creates a distinct legitimate session.

Preserve assistance history for an unfinished question. A learner who reveals or hints an answer and then rotates/reopens must not have the same session counted as independent recall. Keep review schedule changes, persisted session progress, and finalization consistent. Define when a finished feedback flow qualifies for study credit and make that finalization safely retryable. If a crash interrupts the final action, recovery must be able to finish once without awarding extra days or reapplying the schedule change.

Retain the original batch across rotation or process recreation even when answered items are no longer due. Provide a clear Continue action for an unfinished session. Handle content changes during a pending session explicitly: preserve its evidence, identify unavailable material, and offer a safe recovery path. Do not silently grade against a different content version.

Acceptance evidence:

- Rotate/recreate before answering, after saving an answer, after hint/reveal, and on final feedback; continue the same session correctly.
- Restart during a partial batch and finish it without replaying saved schedule updates.
- Recover after the final save/finalization boundary with one session and one daily credit.
- Navigate away/back and deliberately repeat a lesson; feedback agrees with persisted events.
- Missing or replaced content does not crash recovery or attach answers to the wrong version.

Use focused repository tests and actual activity/device checks. Distinguish activity recreation, system process recreation, and force-stop/reopen in the evidence; they are different checks. Any test that changes the clock/database or clears data must use an isolated test app/profile, not Musab's personal installation.

## Task 3 — `feat/source-passage-replay`

Resolve `sourceRef` into a typed source model shared by lesson feedback and review feedback. Handle both neutral text references and recording passages.

For recording passages, select the reference's recording through its segment, use its validated original-file start/end positions, and offer an explicit Play passage action. The referenced segment may differ from the lesson's main segment. Passage playback must stop at the intended boundary and must not inherit a full-recording preference, another lesson's offset, or a previous passage's position. A new Replay passage action starts at the passage's beginning. Preserve normal lesson/full-recording resume positions separately.

Show understandable source information: recording/title where supplied, passage time range, and a way to open the surrounding lesson or full recording. Display elapsed time as minutes/seconds and hours when appropriate. Keep learner controls compact, with accessible seeking where useful. Put technical fixture selection in the development surface while keeping legitimate source/context access easy to find.

For fixture-text references, display the correct neutral passage. Do not represent a test tone as evidence for a textual answer. Resolve existing passage identifiers explicitly, or evolve the fixture schema with its required version update and tests.

Make the local recording association truthful. The pilot needs to detect an accidentally selected wrong file before treating it as the answer source. Use the source manifest's identity/checksum when available, validate locally off the UI thread, and preserve the previous working association when selection fails. Distinguish content fingerprints from URI identity: the current resume-store hash is a preference key, not verification of audio bytes. Preserve explicit technical testing as a clearly labeled separate mode. Do not upload or alter the originals.

Keep Python validation, Android parsing, and the documented contract aligned for any added fields. Reject invalid ranges and missing references cleanly. Runtime preview remains distinct from editorial publication approval; JSON review records do not authenticate a reviewer.

Acceptance evidence:

- Use distinguishable neutral audio fixtures containing at least two identifiable passages at nonzero timestamps.
- Demonstrate lesson segment, question passage, another-recording passage, and full recording selecting the correct source/range.
- Repeat with full-recording mode previously enabled and with saved positions in other modes.
- Demonstrate offline operation, missing/revoked document access, wrong-file selection, and reselection without silent fallback.
- Verify source text and source audio from both initial quiz feedback and later review feedback.
- Human listening remains pending unless someone actually hears and checks the output; player timestamps alone do not prove audible accuracy.

Foreground-only playback remains the current documented behavior for this bounded milestone. Record screen-off/app-background results honestly. Background and lock-screen media playback can be the next media feature after these fixes.

## Task 4 — `feat/first-lesson-pilot`, then handoff

Prepare the smallest end-to-end teaching pilot: one course selected by the Sheikh/content team, one self-contained lesson, and approximately 2–4 reviewed questions if appropriate to the passage. Recording availability alone must not choose the beginner curriculum.

Reuse `docs/SAMPLE_LESSON_AUTHORING_TEMPLATE.md`. Prepare a reviewer-friendly packet listing only the inputs still needed: course/order decision, recording ID and verified identity, lesson boundaries, objective, Tigrinya title/notes, question/choice/answer/explanation text, exact source passages, and actual language/religious reviewers with version/date/result. Record existing permission instead of requesting it again. Keep blanks visibly pending.

Also export a concise interface-copy review sheet from the actual string resources, with stable keys, context, English text, empty Tigrinya text/review fields, and preservation instructions for placeholders/plurals. Use supplied, reviewed text when available. Choose typography and layout for real supplied content; Unicode glyph availability alone is not language validation.

If approved material is already present, integrate that one lesson and run the existing editorial gate before describing it as approved. Prepare a private reviewer build using the established local import/audio flow. If approvals or teaching text are absent, finish the engineering with neutral material and hand off the prepared packet. Report the teaching pilot as awaiting content; do not mark it completed or hold unrelated fixes indefinitely.

Polish the affected learner flow using actual state: Continue study, lesson/passages, answer feedback, review, and saved progress. The current Garden direction remains provisional. Include purposeful feedback/motion that respects reduced motion, compact player controls, clear loading/recovery states, and readable long text. Keep one dominant action per step and explain study streaks as participation. Evaluate native wording and reading comfort with Musab, Ibrahim, or another Tigrinya reviewer when available.

Create a short physical-device checklist: install/update without clearing data; run offline; listen to passage/full recording; seek and return; rotate during review; reopen and continue; complete a study day; inspect enlarged text; use TalkBack; check headset/audio-focus and screen-off behavior against documented expectations. Record device/Android version, who checked, expected versus observed results, and outstanding defects. Give the checklist to Musab rather than claiming unavailable hardware tests passed.

## Completion and evidence

For each branch run the relevant build/lint, unit, Python content, Room migration, and connected-device checks. Add regressions for the concrete failures above. A full emulator suite in GitHub CI is not required merely to increase test counts; accurately identify which checks ran locally and which ran remotely.

Update status and architecture documents to describe the actual final state, merged PRs, schema/content changes, session recovery policy, and remaining limitations. Keep screenshots and short recordings tied to the exact build under review. Do not include originals, personal paths, or unapproved teaching content in public evidence.

End with the APK location and SHA-256, final commit, merged PRs, latest main CI result, clean/synchronized status, test evidence, reviewer packet, and physical-device checklist. Mention the actual repository-protection status without claiming it is configured. Finish this bounded scope and return for review; public release, cloud accounts/sync, a full curriculum, and iOS remain later milestones. The Sheikh's agreed periodic and prelaunch reviews still apply.

## References

- Reviewed main CI: https://github.com/darkest-pearl/Athar/actions/runs/36858610755
- Review state/schema: https://github.com/darkest-pearl/Athar/blob/09d55e41361ff9eb8914a20eb96c107faedb21cd/app/src/main/java/dev/elm/prototype/LearningStore.kt
- Review seeding/submission: https://github.com/darkest-pearl/Athar/blob/09d55e41361ff9eb8914a20eb96c107faedb21cd/app/src/main/java/dev/elm/prototype/ReviewRepository.kt
- Active eligibility/session state: https://github.com/darkest-pearl/Athar/blob/09d55e41361ff9eb8914a20eb96c107faedb21cd/app/src/main/java/dev/elm/prototype/MainActivity.kt
- Study finalization: https://github.com/darkest-pearl/Athar/blob/09d55e41361ff9eb8914a20eb96c107faedb21cd/app/src/main/java/dev/elm/prototype/StudyRepository.kt
- Source replay UI: https://github.com/darkest-pearl/Athar/blob/09d55e41361ff9eb8914a20eb96c107faedb21cd/app/src/main/java/dev/elm/prototype/ReviewScreens.kt
- Audio source and clipping: https://github.com/darkest-pearl/Athar/blob/09d55e41361ff9eb8914a20eb96c107faedb21cd/app/src/main/java/dev/elm/prototype/AudioPanel.kt
- Content identity contract: https://github.com/darkest-pearl/Athar/blob/09d55e41361ff9eb8914a20eb96c107faedb21cd/content/CONTRACT.md
- Android state preservation: https://developer.android.com/develop/ui/compose/state-saving
- Media3 passage clipping: https://developer.android.com/media/media3/exoplayer/media-items
