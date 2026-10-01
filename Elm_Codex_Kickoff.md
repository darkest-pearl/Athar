# Elm — local Codex kickoff prompt

Place `Elm_Project_Blueprint.md` in the local workspace alongside this file. Paste the prompt below into Codex in VS Code. Android-first development, the Sheikh’s participation, and the review team are confirmed. This prompt covers local preparation and a runnable Android prototype; launch follows the Sheikh’s requested prelaunch review.

---

We are starting Elm, a working-name learning app for Tigrinya-speaking teenagers and young adults. Read `Elm_Project_Blueprint.md` before making product or architecture decisions. Treat it as a working proposal: explicit decisions I provide in this conversation take precedence.

Platform: Android first, using Kotlin and Jetpack Compose, with Room for durable local state and Media3 for audio playback. iPhone development follows the first Android launch. Proceed with Android implementation; do not ask me to confirm this decision again.

Confirmed source and review context: I have most recordings on my hard drive. Sheikh Abduselam Negash agreed to the project and asked to review it periodically and immediately before launch. A close student supplied the complete Nawaqid al-Islam collection with the Sheikh’s permission. I, my friend Ibrahim, the Sheikh, and participating students will prepare and review the content. Record this authorization and involvement as confirmed by me; do not turn them into unresolved blockers. New edited lessons, translations, and quiz answers still need our content review before publication.

Act as the implementation lead, applying mobile engineering, UX design, data modeling, security, testing, and release-engineering judgment. I will coordinate product and curriculum decisions with ChatGPT. Work in small, reviewable milestones with concrete evidence.

## Inspect and preserve

Inspect the current directory, applicable AGENTS.md instructions, repository status, existing files, and available development tools. Do not assume this is an empty workspace. Preserve existing work. Never overwrite or discard unrelated changes.

Use commands appropriate to the actual operating system; my normal environment is Windows with PowerShell and VS Code. Inspect Java, Android SDK, the Gradle wrapper if present, and emulator/device availability. Select mutually compatible stable versions from official documentation and pin them. Do not expose environment secrets. Report missing prerequisites accurately and continue independent work. If I have not connected a remote Git repository yet, continue locally.

Do not choose a permanent public app name or package identifier without a confirmed decision. Use clearly documented development placeholders where necessary. Do not create cloud resources or claim a service is configured when it is not.

## Prepare durable project context

Produce concise, internally consistent documents for:

- Product requirements: audience, learning loop, initial scope, and success criteria.
- Design system: typography, script handling, colors, spacing, controls, states, and motion principles.
- Content policy: sources, attribution, permission status, review responsibilities, and publishing states.
- Architecture decisions: selected platform and why; local storage and audio approach; unresolved decisions.
- Implementation plan and status: milestones, completed work, evidence, blockers, and next actions.
- A repository AGENTS.md with the agreed implementation and verification conventions, without claiming authority to override my instructions.

Keep documentation proportional to the initial pilot. Avoid copying the same requirements into numerous files. Record decisions and their status so later sessions can distinguish confirmed requirements from proposals.

## Design preparation

Check whether Impeccable or UI UX Pro Max is already installed and usable. Consult current official installation instructions before proposing or performing setup. Record which version and which applicable guidance are used. Do not treat a web-only detector as Android verification.

Official sources:

- https://github.com/pbakaus/impeccable
- https://github.com/nextlevelbuilder/ui-ux-pro-max-skill

For native Android, prioritize the applicable Jetpack Compose guidance and Android platform behavior. Use one coherent project design system. Tool suggestions must serve the product’s actual audience, script requirements, and platform.

Prepare two visually distinct directions for Today, Lesson, and Review using Compose components and previews. Recommend one, explain why it fits the audience, and use it provisionally for the connected prototype; keep the alternative easy to compare without duplicating the application. Do not pause functional development just to wait for a cosmetic choice. If rendering tools are unavailable, document the limitation without pretending screenshots or an interactive preview exist.

Tigrinya uses Ethiopic script. Verify glyph coverage and layout using supplied or reviewed text; mark any provisional translation. Arabic passages require their own correct direction and alignment. Plan for enlarged text, accessible controls, reduced motion, small screens, and clear loading/error/offline states.

Keep the lesson surface focused. Motion should explain state changes and celebrate completion without interfering with playback or reading. Do not use unreviewed Arabic calligraphy or generated religious quotations as decorative assets.

## Content preparation

Create a minimal documented content schema and validation approach for course, lesson, source recording, timestamped segment, learning concept, question, explanation, source reference, language, version, permission status, and editorial approval.

Questions must reference the relevant source passage. A publication validator should reject lessons or questions lacking required review and authorization metadata. Populate the confirmed collection-level authorization from my statement and allow recordings to inherit that record; do not require repetitive new permission entries for every file. Record reviewers, review dates, the reviewed content version, and corrections. Periodic Sheikh review and final release approval belong in the workflow; do not require him to approve every code change.

If I provide a local recordings-folder path, inventory only that folder without modifying originals. Produce a portable manifest containing relative paths, format, duration where readable, source lesson order when established, and checksums for duplicate detection. Flag unreadable files, ambiguous order, and possible gaps for the team; do not guess missing metadata. Keep large original recordings outside Git and keep machine-specific absolute paths in local configuration.

The complete Nawaqid al-Islam folder is available as a candidate for intake testing; it does not automatically become the first beginner course. Use a single representative recording to validate playback and timestamped segments. If the folder path is not supplied yet, proceed with neutral fixtures and request the path in the milestone handoff. Do not scan unrelated drives or upload recordings to external services without my direction.

Use clearly labeled neutral test fixtures until approved teaching material is supplied. Do not invent a transcript, hadith, religious ruling, translation, or endorsement. Do not attribute synthesized audio or sample text to Sheikh Abduselam Negash.

The compilation list is in the blueprint. Preserve the original recording’s lesson order unless a curriculum reviewer approves changes. Confirm the exact al-Sa‘di source book before assigning that chapter to a book record.

## Define the first implementation milestone

The first milestone is a runnable native Android prototype: open a lesson, play available local audio, answer a small fixture quiz, see explanatory feedback, finish, restart the app, and find completion preserved. Use the recommended visual direction provisionally. Produce a debug APK when the toolchain permits and document installation and verification. Do not stop after writing planning documents if implementation can continue.

The following local-learning milestone expands that complete flow. Keep its components simple enough to inspect and extend:

1. Today, course overview, lesson playback, question feedback, review, and progress flows.
2. A documented local content pack with neutral fixtures initially, followed by a small approved real course.
3. Durable progress, bookmarks, review state, and daily study records.
4. Deterministic review scheduling behind a replaceable interface.
5. Meaningful study streaks using a documented study timezone and an injectable clock.
6. Offline playback of available downloaded/bundled content and complete offline quiz/review behavior.

Keep participation points separate from knowledge mastery. A hinted or immediately repeated correct answer is not sufficient evidence for long-term mastery. Do not award duplicate completion or streak credit after rapid taps, restarts, or replaying an already-recorded completion event.

Prefer guest use for the initial pilot. Explain its storage limits accurately. Design future synchronization boundaries without building unnecessary services. Do not promise recovery after uninstall until backup or sync actually exists.

## Verification expectations

Verify what is built; do not create a large speculative test suite before there is implementation. Focus meaningful automated tests on review scheduling, study-day boundaries, duplicate submissions, persistence, content validation, and later synchronization conflicts.

For the initial runnable prototype, verify the connected lesson/quiz/completion flow and saved progress after restart. Before declaring the expanded local-learning milestone complete, also demonstrate:

- A learner can complete a lesson and questions, receive an explanation, and find preserved progress after restarting the app.
- Downloaded or bundled learning content remains usable with the network disabled.
- An incorrect answer offers useful correction and affects the future review schedule.
- Answer reveal/hints and immediate retries do not falsely raise mastery.
- Repeated completion actions do not duplicate rewards or daily credit.
- Streak behavior works across midnight and follows the documented timezone policy.
- Audio resumes correctly; a quiz or return reminder does not unexpectedly interrupt speech.
- The screens remain usable with enlarged text and reduced motion.
- All displayed teaching content is reviewed or clearly marked as development-only placeholder material.

Use emulator or device screenshots and interaction checks when available. State which device, OS, and scenarios were actually checked. Compilation success alone is not proof of correct UI behavior. If the environment prevents a check, provide precise reproduction steps and mark it unverified.

## Working method and handoff

Do not attempt every future feature in one pass. Complete the current milestone, fix verified defects, and update the status document. Do not claim a test passed without running it or fabricate logs, screenshots, source material, or permissions.

Start now with inspection, concise foundation documents, content intake when the source path is supplied, design directions, and the runnable Android prototype. Use neutral development fixtures where the team has not supplied reviewed teaching content. Continue reversible work without repeated permission requests; surface only decisions or blockers that materially prevent progress. Preserve the distinction between a working development prototype and a reviewed release candidate.

End the milestone report with the outcome, changed files, commands and checks actually run, unresolved issues, and the single most useful next decision or action. Keep repository and local-workspace status explicit so ChatGPT can review your report without assuming direct access to my machine.
