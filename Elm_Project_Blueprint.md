# Elm — initial product and development brief

Prepared for Musab Mohammed Ibrahim · 1 October 2026

Status: Android-first scope and the content review participants were confirmed by Musab on 1 October 2026. “Elm” remains a working name. Product mechanics and visual direction below are proposals to validate. No recordings have been inspected or transcribed, and no new lesson summaries or quiz answers have been approved during this planning work.

## Confirmed project decisions

- Launch Android first. Arrange iPhone development after the first Android app launches.
- Musab has most of the recordings on his hard drive.
- Sheikh Abduselam Negash agreed to the project and is participating. He asked to see the work periodically and immediately before launch.
- A close student supplied the complete Nawaqid al-Islam lesson folder with the Sheikh’s permission, as confirmed by Musab. File completeness and ordering still need a technical inventory.
- Musab, his friend Ibrahim, the Sheikh, and some of the Sheikh’s students will participate in content review.

Treat these decisions and the reported permission as established project context. The next practical inputs are the local workspace and audio-folder paths, followed by a small sample for lesson preparation.

## Product purpose

Help Tigrinya-speaking teenagers and young adults learn Islam through Sheikh Abduselam Negash’s existing audio explanations, following the Qur’an and Sunnah according to the understanding of the Salaf as reflected in the approved curriculum. Make lessons easy to begin, understanding easy to check, and earlier knowledge easy to revisit.

Older learners should also be able to use this edition comfortably through readable text, clear audio controls, adjustable text size, and reduced motion. Separate experiences for children and older adults are later product decisions. Keep curriculum and learning records independent of presentation so future editions can reuse approved material.

The principal product outcome is retained understanding. Session completion, return visits, and streaks support that outcome; they do not demonstrate understanding by themselves.

## A daily learning session

Aim initially for a selectable 5–10-minute daily goal. Duration is a design hypothesis to test with learners.

1. Open Today and see one clear next action: continue a lesson or review a few due concepts.
2. Answer an optional recall question about a relevant earlier lesson before seeing the explanation.
3. Listen to one coherent audio section, usually a few minutes. Preserve the explanation’s context even when that requires a longer section.
4. Read or hear the reviewed key points, with Arabic terms and evidence presented clearly where supplied by the source.
5. Answer two to four short questions, receive an explanation, and replay the relevant source passage when useful.
6. See progress, the next lesson, and a brief completion animation.

Learners can also listen to the original complete recording. A study segment always records its original recording and start/end timestamps. Segment boundaries must be checked by a knowledgeable reviewer; a timer must never determine a cut that changes the meaning.

## Initial curriculum map

This is an organizational proposal using the user’s list, not a religious ruling on study order or a claim to have inspected the recordings. A qualified Tigrinya-speaking reviewer should approve prerequisites and sequence.

| Proposed track | User-specified material | Product treatment |
| --- | --- | --- |
| Foundations | Al-Usul al-Thalathah; Al-Qawa’id al-Arba’ | Guided lessons, a terminology glossary, and recall of foundational concepts. |
| Essential practice | Al-Durus al-Muhimmah li-‘Ammatil-Ummah; Bab al-Taharah explained from a work by Sheikh ‘Abd al-Rahman al-Sa‘di | Clear practical explanations and carefully reviewed application questions. Confirm the exact al-Sa‘di book and edition: the chapter title alone is insufficient. |
| Hadith and conduct | Al-Arba‘in al-Nawawiyyah | Hadith-by-hadith study with approved explanation, meaning, and application. |
| Further foundations | Al-Usul al-Sittah; Nawaqid al-Islam | Introduce with reviewer-selected prerequisites and preserve qualifications and explanatory context. |

Start with five to ten short, approved lessons from one compilation. Al-Usul al-Thalathah is a proposed candidate, subject to the reviewer’s recommendation and the quality and availability of source files. Catalog the other series early, but complete one learning experience before importing the full collection.

Use the supplied Nawaqid al-Islam folder as an available candidate for testing the inventory and audio-import workflow. Availability for technical testing does not determine the beginner curriculum order; the review team and Sheikh guide that choice.

## Reviews and retention

Use active recall and spaced review. The Institute of Education Sciences practice guide supports spacing learning and using quizzes to revisit key content [3]. This supports the approach; it does not establish an ideal interval schedule for this specific audience.

Use three review triggers:

- **Scheduled:** concepts become due after a delay. A simple starting schedule could be 1, 3, 7, 14, and 30 days; treat those numbers as tunable product defaults.
- **Related-topic:** a lesson names the earlier concepts it depends on. Offer a short recall prompt and a link to the earlier explanation.
- **Mixed practice:** occasionally include an older learned concept, choosing more often from weaker or overdue material. Keep this inside session boundaries so it does not interrupt the Sheikh mid-explanation.

Store review state per concept, not just per lesson. Record the question version, answer result, hints or answer reveals, timestamp, and next due date. A revealed answer or immediate retry can support learning but should not count as independent evidence of long-term recall. Require later successful recall before showing strong mastery.

Deduplicate concepts when scheduled review and a new lesson’s prerequisite check overlap. Limit the daily review load and provide a gentle catch-up path after an absence. Use alternate approved question forms to reduce memorization of answer positions.

Start with multiple choice, matching terms and meanings, and brief “recall, then reveal” cards. Add ordering exercises only when the source establishes the sequence. Grade source-supported answers; avoid questions that inadvertently reduce a nuanced explanation to an unsupported absolute. Open responses can initially be self-checked against reviewed explanations.

## Gamification

- Award a daily streak for meaningful study: completing a lesson with a check, or completing a short review session. Opening the app is insufficient. Learners can earn participation credit while still making mistakes.
- Keep current streak, longest streak, and total study days. A missed day does not erase course progress or previous achievements.
- Consider a limited, clearly explained grace-day policy. Specify it before implementation and test calendar boundaries.
- Award modest points for study and review, with daily limits that discourage repeating the easiest question for points.
- Use lesson milestones, course completion, and personal learning goals. Progress labels describe learning activity and knowledge checks; they do not score piety or religious worth.
- Make reminders optional, with user-selected timing and quiet hours. Offer a welcoming return after absence.
- Keep individual progress private by default. Social competition is a later decision, not required for the initial release.

## Interface direction

Design for a Tigrinya-first experience from the beginning. Validate Ethiopic glyph coverage, line spacing, long labels, and native reading comfort using real reviewed text. Render Arabic passages with correct right-to-left direction inside the surrounding left-to-right interface. Do not assume a font that looks good in English works equally well for Tigrinya.

Proposed visual direction: warm ivory surfaces, deep ink, a controlled green accent, and restrained geometric detail. Explore a second, more vivid editorial direction before committing. These are design proposals, not finalized branding. Keep the study surface calm and put most celebration at natural completion points.

Core screens:

| Screen | Primary job |
| --- | --- |
| Today | Continue study, show due review, and make the daily goal understandable. |
| Learn | Browse the curriculum and follow the recommended course path. |
| Lesson | Play audio, follow approved notes, bookmark, and open the original recording. |
| Review | Answer a small set of due questions and receive useful correction. |
| Progress | Show study consistency, completed courses, and concepts needing review. |

Downloads and settings should be easy to find without adding unnecessary navigation tabs.

Animation should clarify state changes: play/pause, selecting an answer, a completed lesson, and movement along the course path. All essential actions remain understandable with animations disabled. Sound effects are optional and must never compete with the lesson audio.

Review real screenshots and interaction recordings on a small phone, a larger phone, with enlarged text, and with reduced motion. Check TalkBack labels, touch targets, contrast, text clipping, loading, missing content, interrupted downloads, and offline states. Include a small pilot with Tigrinya-speaking learners before treating the design as finished.

## Design tools researched

| Tool | Verified scope | Proposed use |
| --- | --- | --- |
| Impeccable [1] | Design guidance with critique, animation, accessibility audit, and polish workflows; its documented automated inspection includes web and browser surfaces. | Design direction and refinement; particularly useful for any web prototype or future content dashboard. Native Android quality still needs Android-specific inspection. |
| UI UX Pro Max [2] | Its official repository lists Codex support and platform guidance including Jetpack Compose, Flutter, and React Native. | Lead stack-specific design guidance for the native Android implementation. |

Install and verify the relevant skill in the local Codex workspace. Consult the current official instructions and record the installed version. Neither tool guarantees flawless design. The project’s agreed design system resolves conflicting suggestions.

## Audio and editorial workflow

Musab has confirmed the Sheikh’s agreement to the project and permission for the student-supplied Nawaqid al-Islam collection. Record that authorization, its source, and any conditions communicated by the Sheikh with the affected collection. Do not restart the same permission inquiry or hold local development pending it. Any later conditions should be reflected in the content records.

For each source recording, collect the compilation, original title, speaker, publisher or source owner, source link or original file, duration, order, recording identifier, and permission record. Distinguish the Sheikh’s confirmed participation from approval of a particular edited lesson or app release, which will be recorded when reviewed.

Prepare each lesson through a versioned workflow: draft, language review, religious review, approved, published. Musab, Ibrahim, and participating students can share preparation and review work according to their expertise; a person competent in both areas may perform both reviews. Record who checked each lesson and which version they checked. Keep quotations, translations, and editorial summaries distinguishable. Link every quiz answer and explanation to a reviewed source passage.

Arrange periodic reviews with the Sheikh at useful milestones: the proposed course order and a complete sample lesson; the first usable app and its quiz/review experience; and the release candidate immediately before launch. These are proposed checkpoints implementing his request, not a requirement for him to approve every engineering change. Assemble the actual app build, curriculum list, sample lesson sources, edited text, quiz explanations, and unresolved content questions for each relevant review. Track resulting corrections and his final release approval.

For local intake, keep the original recordings in their existing folder and give local Codex that folder’s path. Inventory only the supplied folder without renaming, moving, or overwriting the originals. Record relative file paths, duration when readable, format, source order, checksums for duplicate detection, and any missing or ambiguous metadata. Keep large original audio files outside Git; commit a portable manifest and content metadata. Inspect one representative recording first, then expand processing once the team has assessed the workflow and transcription quality. Do not send recordings to an external transcription service until Musab chooses that service and the upload scope.

AI can assist with indexing, transcription drafts, segmentation suggestions, and question drafts. Tigrinya speech recognition and translation quality must be measured on a real sample before adoption. Human review determines what is taught and published. Do not assume Tigrinya text-to-speech quality; recorded question audio is a practical alternative if listening support is needed.

Build prototype interactions with clearly labeled neutral fixtures until real teaching material is approved. Design work can progress without inventing religious lessons or a transcript attributed to the Sheikh.

## Technical foundation

Build the Android release with Kotlin and Jetpack Compose, consistent with the proposed stack and Musab’s existing Android work. Compose provides native animation APIs [6]. Use Room for durable local learning records and Media3 for audio playback [4, 5]. Verify versions and toolchain compatibility when setting up the repository.

Make downloaded lessons, quizzes, review scheduling, bookmarks, and progress usable without an internet connection. Persist each meaningful action immediately. The local database supplies the UI; future synchronization updates that local state through repositories, following Android’s offline-first guidance [4].

Begin with a single Android application, documented content format, and small versioned content pack. Avoid creating multiple backend services before the course publishing and synchronization needs are understood. A restricted publishing tool, account sync, and hosted audio can follow the local pilot.

Keep these concepts separate: course, lesson, original recording, audio segment, learning concept, question version, attempt, review state, daily study activity, bookmark, download, and editorial approval. This permits updated lessons without silently rewriting old answers or losing progress.

Record study timestamps and the learner’s study timezone. Define calendar-day behavior explicitly. Offline activity must merge without double-counting when sync is introduced; cross-device conflict handling is part of that later feature’s acceptance criteria.

Offer guest study initially. Clearly explain that local-only progress does not survive clearing app data or uninstalling. Add an explicit backup or account-sync flow before promising reinstall or cross-device recovery.

When accounts and publishing are added, enforce access rules on the server, keep drafts inaccessible to learners, isolate private progress by user, and keep secrets out of the application and Git history. Collect only information needed to operate the learning experience.

iPhone development follows the first Android launch. Keep the content format, review rules, and API contracts portable for that later client. Native Android UI code is not automatically reusable on iOS; avoid adding a cross-platform framework to the initial release solely for that future possibility.

## Delivery milestones

| Milestone | Reviewable outcome |
| --- | --- |
| 0 — Foundation | Record the confirmed Android scope and reviewer team; inspect the local toolchain; document the content format, source inventory, and build instructions. |
| 1 — Design prototype | Render two visual directions for Today, Lesson, and Review; use the recommended direction for a runnable Android demo with audio, a quiz, feedback, and saved completion. |
| 2 — Local learning app | One small course, audio controls, quizzes, review scheduling, streaks, bookmarks, durable progress, and offline use. |
| 3 — Reviewed pilot | Import team-reviewed lessons, verify every source link, review the app periodically with the Sheikh, and test with Tigrinya-speaking learners. |
| 4 — Publication and sync | Add the minimum publishing workflow, hosting, optional account sync, backups, and release process supported by pilot needs; present the release candidate to the Sheikh immediately before launch. |

The first usable build should demonstrate the full sequence: open a course, listen, answer, receive an explanation, finish, close the app, reopen, and find the same saved progress. Due reviews and streak rules must be testable with a controllable clock.

## Collaboration and next actions

Musab supplies product decisions, source recordings, content-review participation, and device feedback. Ibrahim and participating students help prepare and review lessons; the Sheikh provides periodic guidance and the prelaunch review. ChatGPT prepares milestone instructions, reviews design and engineering evidence, and adjusts scope. Local Codex implements and verifies work in the VS Code workspace. Multiple engineering perspectives should produce concrete review evidence, not merely role titles.

Keep durable project documents in Git: product requirements, design system, content policy, architecture decisions, milestone status, and verification notes. These documents support continuity across Codex sessions. Share the repository link and Codex’s actual status report for review; do not assume this conversation can observe an unshared local workspace or private repository.

The next actions are:

1. Create the local workspace, place this blueprint and the updated kickoff prompt in it, and open it in VS Code. A GitHub repository can be connected when ready; it is not required to begin local development.
2. Give local Codex the original audio-folder path. If it is not ready, continue the prototype with clearly labeled development fixtures and record the pending intake step.
3. Run the kickoff through a runnable Android prototype and audio inventory. Share Codex’s milestone report, screenshots, and the repository link when available for review here.
4. Prepare one representative lesson with the content team, then show the course proposal and complete example to the Sheikh at the first content checkpoint.

Name, monetization, and future children’s/adult editions can be decided after the initial learning flow is validated.

## Sources checked on 1 October 2026

1. Impeccable official repository: https://github.com/pbakaus/impeccable
2. UI UX Pro Max official repository: https://github.com/nextlevelbuilder/ui-ux-pro-max-skill
3. Institute of Education Sciences, Organizing Instruction and Study to Improve Student Learning: https://ies.ed.gov/ncee/wwc/PracticeGuide/1
4. Android Developers, Build an offline-first app: https://developer.android.com/topic/architecture/data-layer/offline-first
5. Android Developers, Media3 ExoPlayer: https://developer.android.com/media/media3/exoplayer
6. Android Developers, Animations in Compose: https://developer.android.com/develop/ui/compose/animation/introduction
