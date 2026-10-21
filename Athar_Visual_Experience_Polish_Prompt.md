# Athar — finish the learner-facing visual experience

Review date: 22 October 2026. Reviewed main: `0c216f30adeb632b0a80a4aa5c665d07bdfe48e8`.

## Review outcome

GitHub confirms PR #12 and PR #13 merged. Main CI run 39666177755 subsequently completed successfully for the final documentation merge, resolving the missing-run item in the handoff. Main is currently unprotected.

The code, interaction specification, and emulator screenshots support a functional guided-learning prototype: authored units, lesson stages, draggable audio seeking, source-linked feedback, saved progress, and recovery. Device-test execution and the silent walkthrough are documented local evidence; this review inspected repository code and screenshots and did not independently execute Android or view the local MP4.

The visual experience remains below Musab's requested polished, gamified learning experience. Current screenshots show:

- A tall shared brand/settings header and development banner occupying substantial space on every learner screen.
- Course and unit headings/counts taking most of the Path viewport before the current lesson. The path still presents as a vertical text list with a numbered button and an expanded detail block.
- Listen's Continue action appearing above the player, weakening the intended listening-first hierarchy.
- Quiz choices distinguished mainly by their labels; persistent global navigation competes with the lesson task.
- Completion dominated by text and storage/participation explanations, with little visual sense of achievement.

The research report correctly states that live Duolingo browser capture, Mimic measurements, and motion sampling were unavailable and not run. Its output is a documented pattern adaptation. Do not claim measured Duolingo fidelity. Actual Athar animation quality still needs review of its local walkthrough.

## Prompt for local Codex

Continue Athar from the existing implementation. Preserve the working learning system and installed progress. Treat the guided engineering milestone as complete, and finish this focused learner-facing design pass. Inspect actual current main and local changes first; do not redo later fixes.

Read `AGENTS.md`, the established development-cycle prompt, `docs/DESIGN.md`, `docs/design/athar-learning-experience.md`, the bounded research study, and the guided evidence. Use the installed native design guidance. Implement a coherent original Athar visual system drawing on the requested clarity and playful progression of Duolingo. Keep the existing reviewed-content policy and neutral fixture labels truthful and compact.

Work through two coherent branches: `feat/learning-screen-polish` for shared layout/player/lesson/practice, then `feat/path-progress-polish` for Home/Path/completion/progress. Adjust names if these already exist. Follow branch → implement → verify → commit/push → final revision review/checks → eligible PR merge → synchronize main and verify main CI. Preserve unrelated work. Routine version-control actions remain authorized.

### Focused lesson and practice layout

Introduce a reusable lesson scaffold. During Objective, Listen, Takeaway, Practice, feedback, and completion, use a compact exit/back control plus useful lesson/question progress. Keep full Home/Path/Review/Progress navigation and Settings in the main app surfaces. Retain a small development/preview indicator so fixture content is never mistaken for reviewed teaching.

Use the available screen height deliberately. Establish a clear task area and a bottom primary-action area respecting system insets. Let long text/feedback scroll without obscuring controls. At enlarged text or short landscape heights, adapt the arrangement so all content and actions remain reachable; a tall fixed footer must not consume the reading area. Avoid repeating course/lesson titles and progress counts when they communicate the same thing.

On Listen, make the audio player the central component before the next-stage action. Show useful source information, a prominent Play/Pause control, the draggable timeline with elapsed/total time, and skip controls. Put Continue to takeaway after the listening task in the reading/action hierarchy. Preserve existing passage/full-recording boundaries, drag preview, play state, and resume behavior. A deliberate Continue remains available; seeking does not award learning credit.

Use answer cards with unmistakable selected state: fill/border, a selection icon, and accessible selected semantics. Correct, incorrect, saving, and committed states must each be visually legible and explained with text. Use concise feedback presentation with clear explanation and exact source access; Next/Finish remains easy to find. Optional source playback may expand without pushing the only continuation action out of reach.

Add purposeful selection, feedback, and progress transitions. Use restrained spring/scale/slide where it improves understanding, and keep equivalent static states at animation scale zero. Make the lesson feel responsive on ordinary hardware. Preserve immutable first-answer evidence, assistance flags, recovery, and save-before-success behavior.

### Course path and motivation

Make Home show the current learning step, a prominent Start/Continue action, a compact daily goal/streak summary, and useful course progress. Give the current task greater visual emphasis than administrative information.

Build a visually connected path grouped into compact unit sections. Give current, completed, available-upcoming, restricted, and review nodes different intentional treatments. Use original icons/shapes and connectors; allow a gentle staggered path on ordinary phone widths, with a simpler accessible arrangement when large text or narrow space requires it. Keep lesson names and state meanings available to screen readers.

Put course/unit progress in a compact header rather than repeated large stacked labels and bars. Show the current lesson and surrounding path context early in the viewport. Selecting a node should reveal compact details through an anchored panel or adaptive sheet instead of leaving a large default-open block between nodes. Current-node discovery and returning to it should be straightforward.

Keep authored units/order, prerequisites, actual completion versions, due review, and pending sessions as the source of truth. Long courses need efficient scrolling and preserved position. A lesson/version update must not manufacture progress, and a cosmetic node animation must not create a completion.

Use a distinctive completion treatment: a small original achievement visual, concise lesson-finished message, real course/unit progress change, daily-goal/streak result, and a prominent next learning action. Move engineering explanations about local storage and participation/mastery policy to the appropriate informational surface. Keep everyday learner wording warm and concise. Keep original artwork/icons appropriate for the existing target age group.

Make earned milestone badges visually recognizable, with clear earned/pending states and short labels. Reuse real saved study/completion data; there is no need to add a currency, backend, or new mastery algorithm for this design pass.

### Acceptance evidence

Update the interaction specification to describe the actual new layouts. Capture before/after views of Home, Path, Listen, selected answer, incorrect feedback with source, completion, and Progress. Inspect actual screenshots for hierarchy, text density, contrast, touch targets, and primary-action reachability. At normal phone text size the main task and next action should be discoverable immediately; larger text may require scrolling without losing information or controls.

Record a continuous emulator walkthrough showing a new user selecting a path node, starting, seeking audio, answering incorrectly, reading/replaying the explanation, finishing, seeing real progress, and continuing. Include interruption/reopen. Inspect motion in the video locally and report the observed transitions. Keep the walkthrough local, as in the existing handoff.

Run the existing relevant build/lint/unit/content/migration gates. Perform device checks affected by the layout/navigation changes, including short and normal phone heights, landscape, 150% font, reduced motion, source replay, save error/retry where feasible, and continued progress after restart. Add a regression only for a concrete behavioral defect introduced or discovered; layout-only changes do not require implementation-mirroring tests.

Keep physical audible quality, TalkBack speech, headset/audio focus, and native Tigrinya reading marked pending until actually checked. Use the existing phone checklist and reviewer packet; this design work can proceed while the content team prepares one reviewed lesson. Provide a short priority list of learner-facing strings for the first Tigrinya pilot from the existing copy sheet, without inventing translations or approvals.

End with final screenshots/walkthrough location, APK/hash, merged PRs/commit, main CI and synchronization, verification evidence, and outstanding human inputs. Report the visual outcome candidly. Finish this bounded pass before adding further features.

## Reviewed references

- Final main CI: https://github.com/darkest-pearl/Athar/actions/runs/39666177755
- Guided feature: https://github.com/darkest-pearl/Athar/pull/12
- Reviewer packet: https://github.com/darkest-pearl/Athar/pull/13
- Screenshots and device evidence: https://github.com/darkest-pearl/Athar/tree/0c216f30adeb632b0a80a4aa5c665d07bdfe48e8/docs/evidence/m3-guided
- Interaction specification: https://github.com/darkest-pearl/Athar/blob/0c216f30adeb632b0a80a4aa5c665d07bdfe48e8/docs/design/athar-learning-experience.md
- Study limitations: https://github.com/darkest-pearl/Athar/blob/0c216f30adeb632b0a80a4aa5c665d07bdfe48e8/docs/research/duolingo-experience-study.md
