# Milestone 3 session recovery verification

Build under review: local debug APK SHA-256 `8236FE72F4BDEAC7199A2165EE1751AC8D2B65E3DA059A9DA7B60F3FA75E8185`, built from `fix/resumable-study-sessions` before PR commit. All screenshots show neutral interface fixtures on the isolated `ElmPrototype` Android 16/API 36 emulator (360×800 portrait); no personal recording or teaching content appears.

Before the fix, completing the first fixture lesson and entering its linked review showed `Review 1 of 1`. Changing orientation to landscape recreated the activity and displayed `Nothing is due` in Review. The linked batch and session ID had existed only in Compose `remember` state.

With this build:

- The second quiz question's committed feedback survived force-stop/reopen through `Continue study` and showed the original correct answer. [Feedback before restart](lesson-final-feedback-before-force-stop.png).
- The linked review stayed at `Review 1 of 1` through landscape and portrait recreation, including after showing its lesson note. [Review before rotation](review-before-rotation.png).
- After its answer was saved but before Finish, force-stop/reopen offered `Continue study` and restored the saved linked-feedback message. [Feedback before restart](review-final-feedback-before-force-stop.png). Finishing returned to the second lesson, `Save and return`.
- A separate background `am kill dev.elm.prototype` removed PID 5296; reopening restored the first question of the second lesson with its unsubmitted `Selected: Bookmark` choice. This simulates process recreation but is not an observed natural low-memory reclaim.

Connected Android tests use isolated databases and cover Room 5→6 through the registered migration chain, ordered due-batch recovery after database reopen, assistance persistence, first-submission feedback on retry, finalization idempotence, one daily credit, unchanged original due qualification, and unavailable-source handling. The branch passed `assembleDebug lintDebug testDebugUnitTest connectedDebugAndroidTest` (16 connected tests) and 14 Python content tests. Device QA used the debug install on the emulator; no physical Android device was available. Natural low-memory reclaim and hardware behavior remain to be checked separately.
