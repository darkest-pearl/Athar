# Athar — continuation after the first Android prototype

Prepared 1 October 2026. Repository reviewed: darkest-pearl/Athar at c49c015940c22a0c33d774a22319ca945c61600f.

## Review context

PR #1 is merged and main CI run 36826407912 completed successfully. The tracked tree contains the generated neutral WAV and no original MP3 recordings. The source manifest lists 21 recordings, 803,585,077 bytes, and 50,205.353 seconds. These are repository and manifest observations; the reviewer did not inspect the original hard-drive files or independently rerun the Android emulator session.

The code and screenshots support milestone 1 as a runnable development prototype. The runtime lessons and questions are still hardcoded in MainActivity.kt. The nextReview function is not connected to a review queue. Streaks, bookmarks, real Tigrinya localization, and reviewed teaching lessons are pending. Garden and Editorial currently share the same layout and mainly differ in color tokens. Main is unprotected at this snapshot.

Additional checks against a scratch copy of scripts/validate_content.py reproduced these gaps. Using the same synthetic publication-test setup as the existing unit test, validation returns no errors for a missing question prompt, all-blank answer choices, and duplicate question IDs. A string in segment.startMs raises an uncaught TypeError. Synthetic test review records were used only to exercise the validator; they are not actual editorial approvals.

Static inspection also identified a source-selection issue: MainActivity.kt lines 73–75 prefer filesDir/representative.mp3 whenever it exists, even after the document picker supplies another recording. This condition affects the injected-file testing setup. Confirm and fix it with two distinguishable local audio fixtures.

This review did not modify the repository, PR, or settings.

## Prompt to execute

Continue Athar from the existing implementation. The first prototype milestone is complete. Build the next local learning milestone in the bounded branches below, following Athar_Development_Cycle_Prompt.md and AGENTS.md. Implement, verify, commit, push, merge eligible work, synchronize main, and then create the next branch. Continue through the listed scope without repeatedly requesting permission for routine implementation and version-control operations.

First inspect current main and any ongoing work. The review above refers to c49c015; if main has advanced, assess the actual changes and avoid repeating completed work. Preserve user changes and existing saved progress. Read docs/STATUS.md, docs/ARCHITECTURE.md, docs/CONTENT.md, docs/PRODUCT.md, and docs/DESIGN.md. Record PR #1’s actual merged status in the next relevant documentation update. Correct the stale claim in docs/PRODUCT.md that no remote repository has been chosen. Keep the original Elm briefs as historical inputs while using Athar in current product documentation.

Engineering can continue while Musab, Ibrahim, the Sheikh, and participating students prepare the first approved lesson. Use clearly labeled neutral content for development. Source-recording permission is already established. Do not invent Tigrinya translations, religious answers, speaker transcripts, reviewer approvals, or a beginner curriculum decision.

1. Branch: fix/content-validation

Strengthen the existing validator before adding runtime content ingestion. Write focused regression tests for the reproduced cases, then implement the fixes. Define required fields, types, accepted states, and identifier scopes clearly.

Require nonempty lesson titles, question prompts, applicable explanations, and meaningful answer-choice strings. Validate choice count and answer indices. Enforce stable identifiers and reject duplicates within their defined scope; do not let dictionary construction silently overwrite duplicate entities. Reject booleans where actual integers are required, invalid versions, malformed timestamps, invalid references, and missing required collections. Distinguish optional fields from required ones deliberately.

Malformed input must produce useful validation errors without an unhandled exception. Preserve the existing checks for authorization inheritance, reviewed segment boundaries, source passages, and current-version language/religious reviews. Development fixtures remain usable for development and fail publication validation. Do not label JSON review metadata as independently authenticated human approval.

Make the validation contract reusable by the upcoming Android loader. Shared positive and negative fixture files can verify consistent rules across Python tooling and Android parsing; do not attempt to run Python inside the Android application. Keep publication validation distinct from development preview.

Acceptance: all existing content tests pass; the reproduced malformed packs fail with meaningful diagnostics; duplicate references cannot silently replace content; unsupported schema versions fail clearly. Commit and merge this branch before the importer depends on it.

2. Branch: fix/local-audio-selection

Reproduce the injected representative-file precedence issue. Track the active audio source explicitly so a user-selected recording plays when selected. Make the injected test source a deliberate development option. Do not silently substitute a different recording when the selected source fails.

Verify the actual document-picker path, persisted read access, reopening after process restart, missing/revoked access, and graceful reselection. Maintain separate resume positions for distinct recordings and modes. Document how full-recording timestamps map to segment-relative player positions, and prevent switching media from restoring another source’s offset.

Keep full-original access and truthful technical-segment labels. Preserve the original files. Test with two short, distinguishable neutral audio files, including the case where representative.mp3 is also present. If user-provided originals are used for verification, keep them outside Git and external services.

This branch repairs source selection and resume behavior. Record the existing foreground-only playback behavior accurately; background media service work can be planned separately. Do not claim physical-device audio quality or interruption handling has been verified without performing those checks.

3. Branch: feat/content-driven-lessons

Replace the hardcoded lesson, question, answer, and explanation literals with typed, validated content loaded through a repository. The portable pack must become the source of truth used by the running app. Keep UI copy in Android string resources so reviewed Tigrinya wording can be added later without searching through composables.

Support a bundled development pack and local content-pack selection through Android’s document picker. Validate completely before activating an imported pack. Bound input size and collection sizes, handle malformed JSON and unsupported versions cleanly, and leave existing content and progress usable after any failed import. Use Android document access rather than requesting broad storage permissions. Audio remains associated through explicit recording identities and local references.

Ship at least two neutral lessons with different stable IDs and multiple concepts to demonstrate that course navigation and progress are no longer fixture-specific. Keep development labeling concise and visible. The absence of approved religious content must not prevent exercising the complete engineering flow.

Introduce versioned persistence for courses/lessons, questions, concepts, attempts, completion, and bookmarks as needed. IDs must not collide between different lessons. Distinguish immutable answer events from derived first-attempt results: later review sessions need additional evidence while repeated delivery of the same submission must remain idempotent. Store enough session, question-version, time, and hint/reveal information for the review engine.

Export the Room schema and implement an explicit migration from the installed prototype database. Preserve its completion and first-answer evidence with correct fixture identities. Do not use destructive migration or clear app data to make upgrades pass. Reject conflicting content that changes under the same immutable version, or require a documented new version. Explain how a new lesson version affects prior completion; do not silently overwrite history.

Refactor the growing MainActivity into focused screens, screen state/ViewModels where useful, and content/learning/player components. Keep this refactor tied to the feature. Retain a single application and avoid an unnecessary multi-module or backend rewrite.

Acceptance: both fixture lessons render from pack data; changing the fixture data updates the correct UI; local import succeeds offline; invalid imports retain the working pack; different lessons record independent progress; rapid repeat submissions do not duplicate events; bookmarks persist; the upgrade preserves the previously installed prototype’s data.

4. Branch: feat/spaced-review

Connect concept-level review state to a real Review area and Today’s due-review count. Persist scheduling and show empty, due, completed, and catch-up states. A learner must be able to complete a lesson now and revisit its due concepts later without retaking the entire course.

Document the initial rule precisely. A reasonable development default is first review after one day, then successful independent reviews at increasing intervals of 3, 7, 14, and 30 days. These are tunable defaults, not a claim of an optimized learning algorithm. Specify stage transitions explicitly so the first review is not accidentally skipped. Inject the clock for deterministic verification and calculate due times consistently.

Wrong answers, hints, answer reveals, and immediate retries must not advance long-term recall as though they were successful independent retrieval. Keep participation separate. Record each real session while making duplicate taps/retries of the same stored submission harmless. Update the attempt and resulting schedule atomically.

Support lesson prerequisites or related-concept links that can offer a short recall check before a relevant new lesson. Deduplicate these against scheduled reviews. Any mixed older question must come from already learned concepts and the same scheduling/attempt system. Do not interrupt audio in the middle of an explanation to insert a quiz.

Keep review batches short, with a documented configurable limit. A long absence must not force the learner through an unbounded queue before they can use the app. Provide reviewed explanations/source replay where available, and neutral equivalents in fixtures.

Acceptance: correct scheduling on first learning and later independent recall; wrong/revealed answers do not falsely increase mastery; due reviews persist after restart and work offline; one concept is not counted twice when it appears through two review triggers; duplicate submissions cannot advance a schedule twice.

5. Branch: feat/study-streaks

Track meaningful study-day events independently from one-time lesson completion. Completing an eligible lesson session or a defined due-review batch can count; opening a screen cannot. A low quiz score does not prevent participation credit when the learner completes the study and feedback flow.

Use the existing saved study timezone and an injectable clock. Persist UTC timestamps plus the derived study date and timezone. Make daily credit unique and transactional, while allowing several legitimate study sessions on one day. Immediate repetition must not manufacture extra days, completion counts, or rewards.

Show current streak, longest streak, and total study days. Define current streak so yesterday’s valid streak remains available during today until today is missed; do not reset it merely because the learner opens the app before studying. Missing a day does not remove saved lessons or achievements. Handle days with fewer due items using an explicit review-batch completion rule.

Keep the first policy simple and explain it clearly. Leave grace-day or freeze mechanics as a documented later decision unless already agreed; do not invent hidden exemptions. Add restrained completion and streak feedback that respects reduced motion. Do not represent points or streaks as a measure of piety.

Acceptance: ordinary midnight, month/year boundaries, repeated sessions, missed days, offline study, restart, and timezone stability are covered. Show independently verified streak results with a test clock; keep test-only controls out of normal learner navigation.

6. Branch: feat/learning-flow-polish, then handoff

Use Garden as the provisional direction unless Musab has supplied a different decision. Consult the already installed design skill and current design document. Make Today, course navigation, Lesson, Review, and Progress reflect the actual implemented state, with clear actions, coherent spacing, persistent navigation, useful errors, and accessible selection feedback.

The existing screenshots establish readability; they are not evidence of a finished animated design. Add purposeful state transitions where they help the new flows, then inspect actual emulator output. Keep content meaningful and compact as text grows. Put technical diagnostics and fixture-source controls in appropriate development surfaces while preserving honest development-content labels.

Run the relevant build, lint, unit, content-validation, migration, and emulator gates for each branch. Add regressions for the concrete risks above rather than creating a large speculative suite. Use the existing source-preservation and offline checks where affected. Test the upgrade from an installed earlier APK without clearing data. Run destructive verifier reset modes only in an isolated test profile/device whose data may be cleared; do not reset Musab’s personal phone.

Capture the actual multi-lesson flow, due review, streak states, invalid import recovery, selected-file playback, and upgraded progress. Check enlarged text and reduced motion and report which screen sizes/devices were used. Verify audible playback on a real device if available; otherwise mark it pending and provide short manual steps. Native Tigrinya wording and readability remain a human review step until the team supplies the text.

After every branch, verify the latest PR revision and main CI before continuing according to the established cycle. Main was unprotected in the reviewed snapshot: report its actual current status and recommend requiring PRs, the build check, and protection against force pushes/deletion. Keep repository-policy changes separate from code implementation; do not claim protection is enabled unless verified.

End with the updated debug APK/artifact location, merged PRs and commits, main synchronization, verification evidence, known limitations, and a short sample-lesson authoring template for the review team. That template should capture source recording and timestamps, objective, reviewed Tigrinya wording, questions/answers/explanations, and review/version fields without filling unknown teaching material with generated assertions.

Complete this scope and hand it back for review. Cloud accounts, hosted downloads, public publication, a full curriculum import, and iOS are subsequent milestones. The Sheikh’s periodic and prelaunch checkpoints remain as agreed.

## Repository references

- Merged PR: https://github.com/darkest-pearl/Athar/pull/1
- Main CI: https://github.com/darkest-pearl/Athar/actions/runs/36826407912
- Reviewed status: https://github.com/darkest-pearl/Athar/blob/c49c015940c22a0c33d774a22319ca945c61600f/docs/STATUS.md
- Validator: https://github.com/darkest-pearl/Athar/blob/c49c015940c22a0c33d774a22319ca945c61600f/scripts/validate_content.py
- Source selection and current UI: https://github.com/darkest-pearl/Athar/blob/c49c015940c22a0c33d774a22319ca945c61600f/app/src/main/java/dev/elm/prototype/MainActivity.kt
