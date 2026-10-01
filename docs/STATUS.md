# Milestone status · 1 October 2026

## Current task
Branch feat/android-prototype; acceptance: native lesson/audio/quiz/feedback/completion/restart flow, source inventory, two visual directions, reviewable debug APK and proportional verification. Athar is confirmed by the development-cycle document; older Elm briefs remain preserved historical inputs.

Baseline main ff63889 was reviewed and pushed to darkest-pearl/Athar. Initial remote was empty. Local Git identity was used unchanged. Implementation remains on task branch until verification and normal PR integration. No protected main branch reported by API; other rules/checks are inspected before merge.

## Implemented
Compose Today, course overview, Lesson, two fixture questions, correction feedback, completion, Progress and Settings. Garden recommended; Editorial available through Settings and six Compose previews. Room completion/attempt persistence with duplicate guards. Media3 bundled tone, read-only local recording selection, timestamp technical test and full-original option. Content/authorization manifest and development/publication validator. Durable requirements, architecture, content policy, design guidance and workflow. Minimal build/lint/unit/content CI.

Inventory: 21 readable MP3s, 803,585,077 bytes, 50,205.353 seconds (13h 56m 45s); SHA-256 duplicates 0. Numeric filename hints 1–21 without gaps. All sourceOrder fields remain null pending content review. Originals unchanged; not uploaded; audio ignored by Git. Representative recording 1 tests playback only. No transcript or religious content generated.

## Verification in progress
Build and two unit tests passed before naming/UI refinement. Four Python content tests pass. Initial Android test failed because Kotlin inferred a Boolean return; corrected to Unit and rerun pending. Final build/lint, device flow, enlarged text/reduced motion, original playback and persistence checks are running. Emulator Android 16 / API 36, x86_64, 360×800 dp. First startup exceeded disk capacity; local partition reduced from 10G to 2G.

## Scope limits / next milestone
No approved teaching lessons, Tigrinya translation, mastery, streak UI, bookmarks, downloads or sync. Review scheduling has isolated proposal tests only, no learner UI integration. Real Tigrinya readability, TalkBack speech, interruptions/headphones, real hardware and small/large device sweep remain pending. Dev fixture literals mirror the documented pack; runtime import is not implemented yet. Local guest storage does not survive uninstall/data clear.

Next most useful content action: the review team selects the initial beginner course and prepares one source-linked, versioned approved sample lesson. Sheikh periodic/prelaunch review remains in the content/release workflow. No public launch performed.
