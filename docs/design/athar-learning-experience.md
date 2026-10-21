# Athar learning interaction specification · 21 October 2026

Reference and evidence labels: [bounded public study](../research/duolingo-experience-study.md). This specifies Athar behavior. Public Duolingo descriptions informed the interaction pattern; numbers, tokens and transitions below are Athar design choices to verify on Android, not measured Duolingo values.

## One neutral unit first

The engineering slice uses two tiny, explicitly non-teaching WAVs. `audio-qa-course` has an authored first unit, **Listen and find a source**, followed by **Practice playback controls**. Its three actual lessons exercise an initial/current/completed/locked path, two source-linked audio questions, passage replay from another recording, and a later text-source correction. The first lesson's older version remains unchanged to preserve accepted identity; optional objective/takeaway fields use title/notes fallbacks. These are technical controls, never approved religious lessons or boundaries.

The learner route is Today → Path/current node → Objective → Listen and scrub → Takeaway → Practice/choice → saved feedback and exact source → Completion → updated Path/next node or Review. A playback seek changes only playback position. A completion appears only after saved answers and transactional study finalization. A repeat preserves the first completion and immutable first-answer evidence. Reviews keep the existing 1/3/7/14/30-day development schedule and bounded batch of five.

## Pattern-to-behavior contract

| Pattern and learner benefit | Athar state and action | Persisted source of truth | Acceptance check |
| --- | --- | --- | --- |
| Visible ordered path: know where to go | Authored units, round numbered nodes; filled current, checked completed, outlined upcoming, restricted prerequisite, separate due-review card. Selecting reveals objective, duration, and Start/Continue/Replay. | Validated pack unit order; version-matched `LessonCompletion`; exact `PendingSession`; `ReviewState`. | Fresh path shows first current, later nodes restricted. Completing first changes its node and makes the next current after Room emission. |
| Focused stages: avoid losing place | Objective → Listen → Takeaway → Practice → Completion, with five-stage header and one dominant action; Exit returns to Path, Back reverses a pre-quiz stage. | `LessonJourney` keyed by course/lesson/version (Room v7); quiz cursor/feedback in v6 `PendingSession`. | Leave at Listen/Takeaway, kill/reopen, Continue restores stage; quiz restores exact committed cursor and feedback. |
| Useful listening: connect audio to source | Source title and verified local identity; segment-relative seekable track, preview on drag, paused/playing preserved, full and answer passage separate. Missing/revoked URI asks for reselection. | Source pack manifest, persisted URI grant, source/mode-specific resume key. | Seek at start/middle/end, switch modes, reopen; no seek creates completion/study credit. |
| Immediate correction: understand mistakes | Before Check, a selection is highlighted. During save, choices/submit disable. After commit, selected and correct choices are labeled in text; feedback explains why and offers exact text/audio source and Next/Finish. | Immutable `AnswerEvent`, pending feedback, typed `sourceRef`. | Wrong and right paths show saved explanation; restart at feedback preserves selection; no regrading of changed material. |
| Integrated recall: revisit earlier ideas | Due review card on Today/Path; prerequisite-related recall optional before later lesson; completed nodes permit repeat. | Existing concept schedule and review session snapshot. | Due count agrees with eligible saved states; a linked before-due attempt never advances long-term stage or earns due-review credit. |
| Truthful motivation: show participation | Today's goal (one meaningful credited day), streak, current-version unit/course fraction, first lesson/three-day/course badges and completion panel. No XP or religious-worth score. | `StudyDayCredit`, `StudySession`, current-version completions. | Opening/seeking/abandoning does not earn a day; saved lesson/review does; one day remains one credit. |

## Component and navigation states

- **Today:** course title, next available lesson and Start/Continue, current course fraction, one-day participation/streak, due review. Empty course/all-complete use explanatory action to Path or Review. Data loads as loading, never as fake zero progress.
- **Path:** unit title/fraction, completed/current/upcoming/restricted/review node. A selected node shows objective or notes, playable duration if known, prerequisite reason, and primary action. Long titles wrap. Saved partial stage says Continue. No painted placeholder progress.
- **Lesson:** top progress and Exit; main stage action near its content; Listen includes track, Play/Pause, skips, source/access errors; Takeaway preserves supplied text. Back reverses Intro/Listen/Takeaway or returns from Practice to saved Lesson, with an Exit to Path. Stage changes save before UI success.
- **Practice:** options are unselected/selected/saving/correct/incorrect/committed. Check disabled until selection. Feedback carries explanation and exact source, then Next/Finish. Save failure leaves the learner on the same action with an inline retryable error; changed material offers close of pending session without grading it.
- **Completion and Progress:** celebrate only after Room completion, point to next available step; bars/badges use current version and real study days. Replaying completed material does not inflate unique completion or day count.
- **Review:** no-due overview, due/related queue, assistance and reveal, saved feedback, source replay, interrupted exact-session resume, unavailable-material recovery. Preserve existing scheduler.

## Visual and motion contract

Garden: ivory `#FAF7EF`, ink `#182E27`, deep green `#236247`, soft green `#E4EBDD`, amber milestone accent. Serif headings and 18sp/28sp body; 24dp phone gutters, 16/24dp section gaps, 48dp minimum targets, 56dp primary actions, 640dp maximum reading measure. Shape, icon/number, state text, and fill together explain each node. Use an Android slider with spoken elapsed/total and skip actions. No borrowed Duolingo artwork or character assets.

Short stage/card/feedback transitions and animated progress may run at the system animation scale. At scale zero, every state and action remains immediately visible with equal meaning. No autoplay, reward sound, or animation required to proceed. Timing/easing are Athar implementation choices, to be assessed by emulator recording; no Duolingo timing was measured.

## Boundary and verification

Android Room v6→v7 explicitly adds stage rows and preserves prior records. Legacy packs without `units` synthesize one ordered unit; imported current-version completions remain usable. Unknown/missing audio must not silently switch source. Long mixed-script fixtures can verify layout/glyph rendering, but native Tigrinya reading comfort requires a human reviewer. Real religious objectives, unit order, source boundaries, translations, and answers require versioned language/religious review and the Sheikh's periodic and prelaunch review.

Verify the first unit at 360×800, a second phone size, enlarged text, landscape where supported, and animation scale zero. Capture actual Home/Path/Objective/Listen/Practice/feedback/completion/updated Path/Progress screens plus a continuous walkthrough with interruption/reopen. Record the APK hash, device settings, failures and fixes; the screenshots establish Athar behavior only.

## Focused lesson scaffold - 22 October 2026

Objective, Listen, Takeaway and Practice now use a focused three-part scaffold: compact Exit/title/stage/fixture strip, a scrollable task body, and a visible primary action below the task. The full Athar/Settings header and four-tab navigation stay on main surfaces, while the saved lesson and quiz screens give that space to study. The footer is outside the scrolling content, so long explanations and expanded source playback do not displace Next/Finish. At a screen height below 500dp, the stage label shares the title row, the body padding shrinks, and the footer uses a 48dp action; Play is reachable with one landscape scroll. At 150% text, the short fixture title wraps in the header and the body scrolls without clipping the action.

The Listen body presents source title/status, prominent Play/Pause, the same segment-relative seekable timeline and elapsed/total times, then skip and source controls. Continue is deliberately after listening in the action hierarchy. No playback logic, source clipping or resume policy changed. Quiz choices have an animated selected fill and stronger border with a checked radio affordance; saved correct and incorrect choices use separate green/amber treatments plus text. Saving changes the disabled action label to "Saving…". Committed feedback stays readable, while exact answer source opens on demand in a vertically grouped panel. Stage, answer and completion transitions still follow Android's animation scale and have static zero-scale states. See [actual emulator evidence](../evidence/visual-polish/README.md).
