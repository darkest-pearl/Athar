# Athar: concept-based learning, Duolingo study, and animated book companions

Prepared 30 October 2026. Implementation brief for the existing native Android project, not a completed app change.

## The agreed product direction

Build Athar around concepts. A course such as The Three Fundamental Principles contains ordered units, short concept lessons, supported questions, explanations, progressively less supported questions, and spaced review. Audio is an optional way to explore a concept. Playing a whole recording is not a prerequisite for learning or progress.

The learner should understand an idea, practise it successfully, and recall it later. A correct response to an obvious question immediately after reading the answer is useful supported practice. It is not evidence of independent retention.

Use Duolingo's applicable learning and interaction patterns. Athar has its own name, palette, artwork, book companions, and reviewed Islamic content. Reproduce the functional quality of the learning loop, screen hierarchy, feedback, transitions, and animation, rather than stopping after adding rounded cards.

Musab decides the content, order, and questions to retain. The accompanying workbook contains 45 concept sections and 90 starter questions, all Undecided. Do not silently approve, publish, or install the entire bank as approved teaching material.

## What was actually studied with Mimic

The actual [Mimic skill](https://github.com/sumanrox/mimic/blob/master/skill/SKILL.md) from [sumanrox/mimic](https://github.com/sumanrox/mimic) was read through GitHub, together with its preflight, feasibility, capture, motion, and access-wall instructions. Skill-file blob SHA: `ea18a3ff4048d498987860365fd413630eff12ff`. Use modes E, pattern extraction, and F, adaptation. This is not a claim of a full pixel-identical clone.

Selected files were staged as temporary study tools. Preflight ran. Node, npm, Playwright, ImageMagick, jq, and Python were present. The local Playwright Chromium binary was absent. Preflight was rerun in its documented `--lenient` mode, and the available cloud browser was used for live interaction. The standalone Mimic pixel verifier was not run.

A live, anonymous Duolingo Spanish guest walkthrough was opened through the public Get Started flow. No account, email address, credentials, or paid subscription was needed for the observed exercises. Measurements below were obtained from the rendered page at a desktop viewport of 1363 x 936 CSS pixels. They are observations of that web session, not measurements of the native Android app or of every Duolingo experiment.

An adaptation of Mimic's `measure.js` ran using the browser's read-only DOM and computed-style interface. It recorded geometry, text styles, colors, radii, and declared transitions. Font inventory was omitted because that browser interface does not expose the full native page API.

Mimic's declared-animation probe was attempted. It failed with `TypeError: document.getAnimations is not a function` in the read-only browser interface. The original motion sampler also needs MutationObserver, timing, and programmatic scrolling that are not available through that evaluation surface. It was not run. Screenshots showed animated transitions, but no exact easing, spring curve, frame rate, or animation duration was recovered. Empty or unavailable animation output must not be described as proof of no animation.

### Observed learner states

| Surface | Direct observation | Athar application |
| --- | --- | --- |
| Welcome | Mascot and a short speech bubble, progress, a persistent Continue action | Original book welcomes the learner briefly. Avoid a long setup sequence before the first useful lesson. |
| Setup | One question per screen, selected radio cards, disabled Continue until a selection, responsive mascot copy | Clear single decisions and immediate selected state. Do not require Duolingo's marketing or attribution questions. |
| First exercise | New Word label, one prompt, three illustrated choices, Check disabled initially | Introduce a concept explicitly, then provide a small supported question. Use evidence and concept visuals rather than forced pictures for abstract beliefs. |
| Selected answer | Blue-tinted selected card, distinct outline/depth, enabled Check | Selection and correctness are different states. Do not mark an option correct merely because it is selected. |
| Incorrect answer | Choices become disabled, correct solution is shown, red correction area, Continue | Explain the correct concept and the relevant distinction. Use an encouraging book expression. |
| Heart tutorial | One deliberate mistake reduced hearts from five to four and opened a tutorial overlay | This is observed, but blocking practice with hearts is outside this Athar milestone. Corrections remain available. |
| Correct answer | Selected choice gains green confirmation and a sparkle treatment; a green feedback area and Continue appear | Show confirmation with text and an icon, a short explanation, a restrained book reaction, and an explicit next action. |
| Matching | Previously encountered terms return in matching pairs; matched pairs disable | Add meaningfully authored concept-to-definition matching where it helps. An equivalent tap interaction must be available. |
| Later questions | Dog and water reappear as text-only choices after earlier illustrated questions; cat also returns | Reduce support and vary the retrieval demand. Do not claim a private adaptive rule was recovered. |
| Mixed progression | New material, matching, text-only recall, and brief encouragement are interleaved | Alternate short teaching, practice, application, and return to earlier concepts. |
| Mid-lesson encouragement | A mascot message appears between groups of exercises | Give brief, honest encouragement, without inferring sincerity or moral worth from answers. |
| Explicit mistake replay | Near the end, a message announces review of a missed exercise; a Previous Mistake label precedes the original illustrated cat question | Return to an error after intervening work. Record this correction separately from delayed independent retention. |
| Lesson complete | After the corrected replay, progress reaches its end; an animated character celebration, Lesson Complete message, animated counter, Review Lesson, and Continue appear | Replace the human/owl artwork with the original book rig. Save the lesson first, show a truthful result, and offer a clear next action. |

The live sequence supports a design inference: removing cues and revisiting concepts can create a stronger learning journey than an audio catalogue. It does not prove that Duolingo uses Athar's proposed difficulty policy, intervals, mastery thresholds, or scheduler.

### Measured examples from the guest web session

| Element | Observed value | Interpretation |
| --- | --- | --- |
| Exercise prompt | 600 px wide, 40 px high, 32 px, weight 700 | A dominant prompt with limited competing chrome |
| Illustrated answer | Approximately 195 x 254 px, 12 px radius, 19 px text | Large selectable targets in the desktop exercise |
| Bottom Check/Skip | 150 x 50 px, 16 px radius, 17 px weight 700 | A stable, high-emphasis action area |
| Lesson progress track | Approximately 886 x 16 px | Visible position throughout the activity |
| Onboarding choice | Approximately 364 x 78 px, 12 px radius | A compact, readable selection card |
| Font family | `duolingo-sans, sans-serif` | An observed proprietary family, not an Athar asset |
| Main page colors | White; text near rgb(60,60,60); active green rgb(88,204,2) | Reference observations, not a requirement to copy the palette |
| Declared CSS example | The close button had a `filter 0.2s` transition | One computed declaration, not a measured character animation |

Use these as evidence of hierarchy and state design. Convert the design to native dp/sp and responsive Compose layouts; do not copy desktop pixels into a phone layout. Mobile web breakpoints, native Duolingo screens, exact motion curves, and signed-in personalization were not measured here.

### Official sources used alongside live observation

- [Duolingo's guided path explanation](https://blog.duolingo.com/new-duolingo-home-screen-design/): ordered progress and integrated practice. This describes the 2022 path redesign, not a claim about every current screen.
- [Duolingo character shape language](https://blog.duolingo.com/shape-language-duolingos-art-style/): clear silhouettes, exaggerated expressions, and directing attention through art and motion.
- [How Duolingo animates its world characters](https://blog.duolingo.com/world-character-visemes/): character animation states and mouth-shape blending. Athar does not require speech synthesis or lip-sync in this milestone.
- [Duolingo's math engineering explanation](https://blog.duolingo.com/developing-math/): knowledge components can take different visual/exercise forms, and Rive can respond to programmatic inputs.
- [Compose animation guidance](https://developer.android.com/develop/ui/compose/animation/quick-guide) and the [official Rive Android runtime](https://github.com/rive-app/rive-android): implementation references, not proof that either runtime has already been added to Athar.

## The Athar learning loop

1. Enter the course path and see a clear next lesson plus any due review.
2. Open a compact lesson with a close control and progress bar. App-wide tabs and Settings do not compete with the activity.
3. Read one short concept card. The book may point to the key sentence. Optional source explanation and audio are available.
4. Answer a supported question with three clear choices. Check remains disabled until a choice exists.
5. Receive explanatory feedback. A mistake produces a correction and encouragement. Continue is an explicit learner action.
6. Meet another related concept or activity before revisiting the first concept. Changing the position of a choice is not the only increase in difficulty.
7. Answer a less supported variant with plausible distractors, changed wording, or a short application scenario. A hint remains available, and using it is recorded as supported practice.
8. Finish the lesson, see saved progress, and return to the same path position. The next node and scheduled review are clear.
9. Revisit the concept later through due review. Mix concepts while preserving prerequisites and source context.

Suggested first vertical slice: concept sections 1-4 in two small lessons, followed by a review checkpoint. These are candidate teaching drafts until Musab selects and reviews them. Engineering must remain runnable with clearly labelled neutral fixtures while that decision is pending.

Aim initially for about 3-5 minutes per lesson, containing 1-2 small concepts and roughly 6-10 meaningful interactions. This is a design target to test with learners, not a quota or a researched universal optimum. Broad sections in the course outline may need several lessons.

### Difficulty progression

| Stage | Support | Question design | What an answer demonstrates |
| --- | --- | --- | --- |
| Teach | Short insight visible, source available | Explain the concept first | Exposure |
| Recognise | Obvious contrast, three choices, hint available | Clear affirmative wording | Supported recognition |
| Distinguish | Teaching card hidden, closer alternatives | Contrast concepts that could actually be confused | Discrimination |
| Apply | New short situation | Apply the same principle without adding an unreviewed ruling | Understanding in a new context |
| Recall later | Different session and variant; hint use recorded | Revisit the concept after a gap | Evidence of retention |

Never make a question difficult through obscure English, ambiguous alternatives, unequal theological precision, a hidden exception, or an unreviewed new claim. Do not reward speed for reading evidence. Use feedback after correct as well as incorrect answers. Correct misleading distractors promptly.

Store guided correctness separately from independent retention. Preserve the first submitted answer and any later correction. A completed lesson, a streak, and retained concepts are different measures.

Starting review intervals may be about 1, 3, 7, 14, and 30 days, with a same-session revisit. These are proposed tunable defaults. Prefer the existing tested scheduler where appropriate, and document any policy change. A single easy or hinted answer cannot advance a concept to retained. A possible pilot rule is two unassisted successful variants on different days before showing Recalled; validate its usefulness rather than labelling it a scientific certainty.

## The book companions

Use original cartoon books instead of human-like characters. The primary companion is a friendly hardcover book with expressive eyes and mouth, two floating hands, and two floating feet. Use the floating-limb option Musab suggested; arms and legs are unnecessary for the first character.

The cover forms the body. A cream page block, rounded cover corners, and a bookmark provide a strong silhouette. Keep the design readable at small sizes. A quiet green cover with warm cream pages is a proposed fit with Athar's existing Garden direction. Keep this as a generic learning book with an original cover treatment.

The book is a learning companion. It does not impersonate a scholar, prophet, angel, or sacred text. Religious statements come from reviewed learning content and visible sources. Add secondary book characters only when they serve a clear teaching role.

### Required character states

| State | Pose and motion | Trigger |
| --- | --- | --- |
| Idle | Relaxed floating hands, occasional blink, very small settling motion | Waiting for a learner action |
| Welcome | Friendly wave and open expression | First course/lesson entry |
| Explain | One hand points toward the concept card, attentive mouth shape | Teaching or expanding an explanation |
| Think | Thoughtful eyes, hand near the cover, no distressed expression | Optional hint or a difficult question |
| Correct | Quick pleased expression, small bounce, hands lift | A correct answer has been committed |
| Encourage | Warm reassuring expression and open hand | Incorrect answer and readable correction |
| Complete | A short celebratory hop, hands spread, bookmark follows through | Lesson completion has been saved |
| Return | A brief welcoming wave | Resuming an interrupted lesson |

A generated pose concept is reference artwork. It is not a sprite animation, a rig, or an interactive Rive state machine. Implement and verify actual transitions in the APK.

Use an editable layered rig: cover, page block, left/right eyes, eyelids, mouth, hands, feet, bookmark, and shadow. Keep the book identity and limb count consistent. Prefer a genuine original Rive asset if an editable `.riv` file can be authored and tested. Otherwise use a maintainable Compose vector/layer rig with explicit animated transforms and expression shapes. Do not block completion on a non-existent Rive asset or describe a static image crossfade as a full character rig.

## UI requirements

- **Home/path:** compact course title, current unit, visible nearby nodes, clear start/continue action. Show completed, current, upcoming, and review states with shape/labels as well as color. Preserve the existing course-scoped scroll restoration.
- **Concept card:** one idea, clear key term, concise original explanation, Source / Learn more expansion, optional Listen. No file picker or source setup in the everyday learner flow.
- **Question:** one prompt, 3-4 generous answer targets, stable positions for the current item, a visible selected state, and a persistent Check action.
- **Feedback:** correct/needs-correction text, the intended answer, a short reason, exact authored source reference, and Continue. Keep it readable until the learner advances.
- **Matching/order:** use only when the content benefits from relationships or sequence. Provide a tap/accessible alternative to drag. They are additional authored activity types, not automatically fabricated from arbitrary paragraphs.
- **Completion:** a short book animation, truthful lesson result, saved concepts/progress, and a clear next action. Summarise first-attempt performance accurately if shown.
- **Progress:** distinguish lessons completed, concepts needing review, concepts recalled later, and study-day participation. Avoid presenting app points as a measure of religious virtue or sincerity.
- **Editorial review:** a separate author/reviewer surface with stable IDs, Keep/Edit/Remove/Defer, order, source, correct answer, explanation, and version. Do not expose these fields in the learner activity.

Preserve English-first authoring, the existing ability to render Arabic and Ethiopic scripts, and future reviewed Tigrinya localization. Layout must allow longer text. Use 48 dp minimum interactive targets, text/icon indicators in addition to color, and readable contrast.

## Motion requirements

All numbers in this table are proposed Athar timings. They are not measured Duolingo timings. Tune them by inspecting the actual emulator recording.

| Interaction | Proposed motion | Target range | Reduced-motion behavior |
| --- | --- | --- | --- |
| Button press | Move the top face into its depth, then release | 60-100 ms press, 120-180 ms release | Immediate clear pressed state |
| Choice selection | Border/fill transition and small settle | 140-200 ms | Immediate selected state |
| Concept/activity transition | Short directional slide or page-turn cue with opacity | 220-300 ms | Immediate change or short opacity transition |
| Progress fill | Smooth change to committed progress | 220-320 ms | Immediate correct value |
| Correct answer | One book bounce, expression change, restrained sparkle | 350-550 ms | Static happy pose with text/icon |
| Correction | Gentle open-hand reassurance; feedback panel settles in | 250-450 ms | Static encouraging pose and feedback |
| Node unlock | Node gains completed/current state with a short connecting-path update | 400-700 ms | Immediate final state |
| Lesson complete | Small anticipatory dip, hop, hands spread, bookmark follow-through | 900-1400 ms | Static completion pose |

Required quality details:

- Motion must communicate the current state, not play randomly on taps.
- Character and UI reactions must follow committed learning events. Recomposition, rotation, reopening feedback, or restoring a session cannot award another completion or replay rewards as new achievements.
- Do not wait for an animation to write learning data. Write the result first, then animate it. Continue may advance while a cosmetic reaction settles.
- When the learner is reading, avoid large perpetual movement near the text. Suspend decorative loops when backgrounded or offscreen.
- Disable bobbing, shaking, hopping, particles, and rapid page turns when reduced motion or the system animation scale requires it. Preserve all information and interaction.
- Optional haptics and brief feedback sounds need user controls. Neither is required to understand a result.
- Use a predictable event/animation controller. Rapid taps cancel or coalesce cosmetic transitions without submitting a second answer.

## Required content and persistence changes

The inspected current `content/CONTRACT.md` is schema 1. It requires nonempty recordings and segments and a `segmentRef` for lessons. Its production source references are recording passages; `fixture-text` is a neutral development path. Therefore text-first teaching requires an explicit content-contract extension. Do not create a silent audio file, a fake timestamp, or a fake recording to satisfy old validation.

Implement a versioned schema with backward compatibility:

1. Lessons may contain ordered typed activities: concept card, choice, matching/order where authored, explanation, and optional audio.
2. Audio metadata and bounded recording references remain validated when present, and are optional when the lesson is entirely text based.
3. Introduce a real production text-passage/source reference. It resolves an immutable authored passage, edition/source identity, version, and locator; supports offline reading of the permitted authored explanation; and distinguishes original summaries from source quotations. Do not relabel neutral `fixture-text` as approved religious material.
4. Record bibliographic evidence such as the named English edition and printed pages. If an exact paragraph is needed, establish and review that paragraph ID during content preparation. Broad draft page ranges must not be presented as exact quotation anchors.
5. A concept has a stable course-scoped ID. Questions are separately versioned variants linked to that concept. Difficulty and support are metadata, not new concept identities.
6. Store ordered session activity IDs, presented choice order, current activity, first response, correction/hint/source-view status, and finalization state. Restore the same question and answer mapping after recreation or restart.
7. Only independent review attempts qualify for the applicable retention step. Immediate corrections, visible-answer retries, and hinted attempts do not masquerade as independent recall.
8. Preserve installed learner data with explicit Room migrations if tables change. Keep historical completions/answers and the tested course/version isolation, reconciliation, and startup-loading protections.
9. Python and Android validators must agree on both new and legacy packs. Invalid imports leave the previous working pack active.
10. Keep editorial selection separate from publication approval. Changed stems, distractors, answers, hints, explanations, or source passages require a content version and fresh applicable review.

The accompanying files are authoring material, not a schema-1 import pack. Keep `developmentOnly`/draft status honest. The reference book PDF is not to be bundled or republished as an app asset.

## Implementation branches and acceptance

Read the project's current AGENTS.md and development-cycle instructions first. Inspect the current branch, clean/dirty state, latest main, pending PRs, and existing work. The latest supplied handoff reported main at `de7f2c8`; preserve anything newer. Do not reset to that old checkpoint.

Complete one reviewed vertical slice before extending it. Suggested branch sequence:

1. **Text-first content and variants.** Version the contract, align both validators, and support text-only lessons plus legacy audio packs. Verify invalid text references, changed immutable content, optional audio, and old-pack progress.
2. **Concept lesson engine.** Add the short-card -> supported-choice -> feedback -> reduced-support/application sequence, independent retention classification, and durable session recovery. Verify choice order and no duplicate submissions after rotation/restart.
3. **Animated book companion and lesson UI.** Implement the actual layered character, required states, original visual language, and motion controller. Check normal/reduced motion and background lifecycle behavior.
4. **Path, practice, and editorial controls.** Connect next lesson and due review to genuine saved results. Add the author/reviewer workflow or an explicit reviewed import workflow with selection/order control. Do not accidentally turn all Undecided questions into active lessons.
5. **End-to-end refinement.** Inspect the actual APK and recording. Resolve visual, persistence, accessibility, and content-state mismatches before declaring completion.

For every branch: implement, inspect the intended diff, run relevant local checks, make focused commits, push, open/review the PR, wait for checks on its final revision, merge when eligible, synchronize main, and verify the merge. Respect branch protections and required reviews. Distinguish local-only work from pushed and merged work. Preserve original recordings and keep large audio/video/APK evidence outside Git.

### Required verification

- A text-only concept lesson works offline with no audio file selected or present.
- A legacy audio lesson still loads, seeks, resumes, and resolves its exact source passage.
- The first supported question feels easy because it was taught clearly. A later variant tests the same concept with fewer cues; no ambiguous or multiple-correct choices are introduced.
- Feedback explains why, including after a correct answer. It remains on screen until Continue.
- A wrong answer, correction, hint, source view, later independent success, and delayed success produce the intended distinct records and progress.
- Matching/order activities, if added, have an accessible non-drag route.
- Normal animation shows actual hand, face, body, and bookmark changes. Reduced motion provides the same states without the movement. A static pose sheet alone is not acceptance evidence.
- Rotation, background process death, and force-stop/reopen restore the correct current activity, choice order, committed feedback, and session. Completion and study-day credit are not duplicated.
- Return to the same path position after an unfinished or completed lesson; tab switching and deliberate course changes follow the existing tested rules.
- Test a small phone, landscape, large text, TalkBack focus/announcements, and reduced motion. Report actual physical-device gaps rather than declaring them passed from an emulator.
- Run build, lint, relevant JVM/connected tests, Python content checks, and CI according to the repository's workflow. Add regressions for the new schema, attempt classification, and persistence boundaries.
- Record one uninterrupted walkthrough: course path -> concept card -> supported answer -> correction -> explanation/source -> later variant -> completion -> updated path -> due-review/Progress. Inspect the recording through its final states.
- Tie APK hash, source commit, test results, screenshots, and recording notes to the same final build. Keep motion timing claims labelled proposed until measured on that build.

## Deliverables from Codex

Return the updated APK/hash, merged PRs and main commit, schema/authoring documentation, the editable character source or layered-rig implementation, the inspected walkthrough location, representative screenshots, regression results, and an honest list of content/hardware reviews still pending. Do not call this milestone complete if animation is only described, the text-only flow needs fake audio, or the complete walkthrough stops before its final states.

## Sources for the teaching draft and learning design

- Muhammad ibn Salih al-Uthaymeen, *Explanation of the Three Fundamental Principles of Islaam*, Abu Talhah Dawud Burbank translation, Al-Hidaayah, 1997. [Reference edition](https://darpdfs.org/storage/2023/12/Exp.-of-the-Three-Fundamental-Principles-of-Islaam-Sh.-al-Uthaymeen-compressed.pdf). Printed page references appear with each concept. Questions and teaching cards are original English draft wording.
- Muhammad ibn Abd al-Wahhab, [underlying Arabic matn](https://risala.prh.gov.sa/ar/content/489).
- Butler & Roediger (2008), [multiple-choice feedback study](https://pubmed.ncbi.nlm.nih.gov/18491500/). Feedback reduced later use of incorrect alternatives and improved retention in the experiments. This supports corrective feedback, not a guarantee of outcomes in Athar.
- Rawson & Dunlosky (2011), [spaced relearning of conceptual material](https://pubmed.ncbi.nlm.nih.gov/21707204/). Supports repeated retrieval across sessions; does not establish Athar's exact proposed schedule.

