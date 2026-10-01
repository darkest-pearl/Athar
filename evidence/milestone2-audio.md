# Milestone 2 audio selection verification · 1 October 2026

Branch: fix/local-audio-selection. Isolated ElmPrototype Android 16/API 36 x86_64 emulator, 360×800, 160 dpi. The emulator runs without audible output; decoder/timeline behavior was checked, not sound quality.

- `assembleDebug lintDebug testDebugUnitTest connectedDebugAndroidTest` passed. The committed connected test checks explicit Document selection, restart persistence of the stored mode/URI, and distinct position keys for representative/document A/document B and full/range modes.
- Local-only generated neutral 440 Hz and 660 Hz WAV files, each six seconds and 192,044 bytes, were put in emulator Downloads. The injected `representative.mp3` was also present in private app storage (copied from the inventoried source, never edited or committed). Through Android OpenDocument, choosing a-440.wav showed `0s / 6s` rather than the representative's 30-second technical clip. Choosing b-660.wav persisted a `content://.../b-660.wav` URI and active mode `Document`; playback advanced to `2s / 6s`.
- After force-stop/reopen, the selected document reopened and its position showed `2s / 6s`. This exercises persisted read access and source-specific resume. Switching between source identities is separately covered by the connected test.
- Removing only the generated b-660.wav emulator copy and restarting produced `Selected recording is unavailable. Select the local recording again.` and `0s / 0s`; no representative fallback occurred. Restoring the neutral file and choosing a-440.wav through the picker cleared the error.
- A temporary isolated-emulator instrumentation test released the persisted read grant and confirmed it was absent from `persistedUriPermissions` (3 tests passed, 0 skipped). It was removed afterward because routine tests must not revoke a learner's file. End-to-end UI after grant revocation was not observed because the connected test runner uninstalled its target app. The runtime uses the same open-file failure path exercised by the missing-file case.
- The original source recording folder was read/copied for test injection only. No original was modified, moved, committed, or uploaded. The neutral WAVs and temporary automation script remain ignored under `.local`.

Foreground-only playback and unreviewed technical clipping remain explicit. Physical-device sound quality, interruptions, and headphones remain unverified.

The updated non-resetting source/accessibility verifier passed all six checks after making range mode and fresh-profile navigation explicit. Its machine-readable result is evidence/verification.json; source and large-text captures are in evidence/. Emulator settings were restored by the verifier.
