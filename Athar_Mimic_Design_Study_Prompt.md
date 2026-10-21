# Athar — use Mimic to study and implement a richer learning experience

Prepared 21 October 2026.

## Repository and fit

The Mimic repository identified for this plan is https://github.com/sumanrox/mimic, reviewed at `a2e1f6c3316bf10b6e94e8898981d6fef768e670`. Musab referred to a public repository called Mimic without an owner/link; this is an explicit working assumption. If he supplies a different repository, inspect that repository and adjust the tooling choice.

Mimic's README and `skill/SKILL.md` describe a Claude Code skill for observing website layouts, component states, typography, interaction, and motion with browser tools. Its pattern-extraction and design-adaptation modes fit this work. Its scripts inspect browser surfaces; they do not generate native Android Compose components or recover Duolingo's private learning algorithms. Codex will need to adapt the workflow and implement the resulting specification in Athar's existing native stack. Successful installation or a measured web page alone does not establish a successful Android learning experience.

Athar main was `a70a8f025e4c8849ef8d0fd2894fa9cc17664c50` during this check. Inspect the actual current branch and working tree before work; do not repeat features already implemented since earlier milestones.

## Prompt for Codex

Use Mimic's observation and pattern-extraction approach to study Duolingo's learner experience, then implement an original Athar experience with comparable clarity, polish, progression, and feedback. This extends Musab's previously approved guided-learning and draggable-audio requirements. Keep the current Android app, learning data, source audio, and agreed editorial workflow.

First read `AGENTS.md`, `Athar_Development_Cycle_Prompt.md`, the latest project status/architecture/design/content documents, and active milestone instructions. Inspect local changes and ongoing branches. Preserve unfinished work and continue the established branch → verification → commit/push → PR/checks/review → merge → synchronized main cycle. Do not restart the project or change technology stacks.

### 1. Inspect and adapt Mimic

Read the reference repository's README, `skill/SKILL.md`, relevant capture/motion guides, and scripts before executing them. Use the reviewed revision or record the different revision actually selected. Keep this design tooling in a separate local checkout; it is a development aid, not an Android runtime dependency.

Select pattern extraction and redesign/adaptation for this request. Adjust the Claude-specific browser/tool instructions to Codex's available browser tools. Verify dependencies and Windows compatibility before using individual scripts. Do not assume its Claude installation command installs a working Codex integration. Use available equivalent inspection tools if needed and report exactly which Mimic helpers or methods were used. Retain the relevant MIT notice for any reused tool code.

### 2. Study the actual learning flow

Use Duolingo's mobile web learner interface and public product demonstrations/documentation as observable references. Distinguish its marketing site from its course/lesson experience. If Android-specific captures are available, label them separately; mobile browser measurements do not establish Android implementation details.

Study a bounded journey: course path → lesson introduction → an exercise → selected answer → correct/incorrect feedback → lesson result → updated course position → targeted review. Include daily-goal/streak presentation where observable.

For each observed screen/state record:

- The learner's current goal and the primary next action.
- State transitions, disabled/loading states, Back/Exit behavior, and what happens after a mistake.
- Hierarchy, spacing, touch targets, component dimensions, text lengths, and progress indicators.
- Motion timing, easing/spring behavior, sequencing, and reduced-motion behavior where observable.
- How the learner understands completion, repetition, next-step availability, and returning later.

Use permitted interactive access for any signed-in surface. If a screen is inaccessible, use an official public demonstration or label the gap and continue the accessible study. Do not turn a login/access gap into invented observations. Keep private session data and credentials out of the repository and reports.

Create `docs/research/duolingo-experience-study.md`. Label entries OBSERVED, SOURCE-DOCUMENTED, INFERRED, or UNVERIFIED, with source/date and platform. Use screenshots/frame references when available; collect only the bounded evidence needed to understand the interactions. Record measured timings only when actually measured. Do not claim the internal scheduler, scoring formula, or personalization has been recovered from visible screens.

### 3. Turn observations into an Athar specification

Create `docs/design/athar-learning-experience.md` and update the current design document to point to it. For every adopted pattern explain the learner benefit, Athar behavior, required persisted state, and acceptance check.

| Reference pattern | Athar implementation target |
| --- | --- |
| Visible course path and a clear current position | Course → authored units → short lessons; distinct completed/current/upcoming/review states driven by actual progress |
| Focused lessons with visible progress | Objective, listening, reviewed takeaway, short practice, explanation, and a clear completion/next action |
| Fast answer feedback | Readable selected/correct/incorrect states, purposeful motion, explanation, and exact source passage access |
| Practice integrated into progression | Existing due/prerequisite recall surfaced at useful points using the persisted concept scheduler |
| Daily motivation and milestones | Real study-day goal, streak, unit completion, and milestone feedback derived from legitimate saved events |
| Coherent component and motion language | Original Athar tokens, icons, buttons, path nodes, progress components, and transitions implemented in Compose |

Define complete component states and navigation behavior before a broad visual rewrite. Include interrupted-session recovery, missing audio, changed content, save errors, long text, and empty/loading states.

Keep Athar's own branding, original visual assets, and Tigrinya/audio-led teaching model. Design for teenagers and younger adults. Use supplied reviewed wording for teaching material; neutral fixtures can exercise the full engineering journey. Unit order, lesson objectives, passage boundaries, and religious question answers remain authored/reviewed content. Do not cut a long lecture into arbitrary timed fragments and call those approved micro-lessons.

Model learning mechanics separately from appearance. Use the current documented schedule unless an explicit, justified change is part of the task. Preserve immutable answers, hint/reveal evidence, idempotence, meaningful study credit, and data migrations. A streak or badge expresses study participation. Any new points/reward rule needs a simple explicit policy tied to real persisted events and safe retry behavior.

### 4. Deliver one complete learning slice

Implement one polished unit journey before rolling components across the entire app. Reuse existing features that satisfy the specification. Use a neutral fixture unit if reviewed teaching material is not yet supplied. Build enough real activity states to demonstrate completed, current, upcoming, and review behavior; do not paint simulated progress into the learner UI.

Include the required draggable audio seek bar. Show elapsed/total time, preview the target while dragging, preserve play/pause, and seek correctly within the active lesson/passage/full-recording timeline. Playback polling must not fight the drag. Keep audio position, lesson-stage progress, course completion, and study credit as distinct truthful values.

Use native Compose layout, interaction, accessibility, and animation. Resolve additional lesson-stage persistence with an explicit migration when needed. Keep imported content/progress usable after an update. Preserve original MP3s and existing source permission; original recordings remain local and outside Git/external services.

The slice should let a new user find the next lesson, listen and seek, answer practice, understand a correction, finish, see real path progress, and move to the next lesson or review. Continuing after interruption must restore the same saved work.

### 5. Validate and roll out

Run relevant build/lint/unit/content/migration checks and device verification. Inspect actual emulator output at the existing phone size plus a second useful size, enlarged text, landscape where supported, and reduced motion. Check long/mixed-script content using supplied text; glyph rendering alone does not establish native reading comfort.

Capture a continuous Athar walkthrough of the implemented slice. Assess hierarchy, action discoverability, feedback, motion, audio seeking, path updates, and restored progress against the adopted specification. For any direct component comparison, distinguish intentional Athar adaptations from implementation defects. Mimic's DOM/CSS web verifier is not a native Android UI test; use Android screenshots, interaction checks, and appropriate animation evidence.

Fix observable problems in the slice, then extend the verified components to the relevant Home/path, lesson, review, completion, and progress screens in coherent task branches. Keep the Sheikh/content team's agreed periodic and prelaunch review and the first-lesson reviewer packet in the handoff.

Finish with the observation report, adopted interaction specification, actual APK path/hash, walkthrough, merged PRs/commit, latest main CI, synchronization status, and remaining human content/device checks. Separate observed behavior, measured fidelity, and intentional adaptation. Do not claim a Duolingo-equivalent retention outcome from matching its visual patterns.

## Primary references

- Mimic repository: https://github.com/sumanrox/mimic
- Reviewed skill: https://github.com/sumanrox/mimic/blob/a2e1f6c3316bf10b6e94e8898981d6fef768e670/skill/SKILL.md
- Duolingo's explanation of its guided path and integrated practice: https://blog.duolingo.com/new-duolingo-home-screen-design/
- Duolingo's explanation of section navigation and targeted review: https://blog.duolingo.com/how-to-review-lessons-on-duolingo/
- Athar repository: https://github.com/darkest-pearl/Athar
