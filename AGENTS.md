# Elm implementation conventions
User instructions take precedence. Read Elm_Project_Blueprint.md, docs/STATUS.md and docs/CONTENT.md before changing scope.
- Android first: Kotlin/Compose, Room durable state, Media3 playback. Confirmed app name: Athar. Development identifier dev.elm.prototype remains a placeholder.
- Never modify originals in Nawaqid_Al_Islam. Never add large audio, local paths, SDKs, secrets or generated build files to Git.
- Neutral development fixtures are allowed. Religious text, translations, source segment boundaries and quiz answers need versioned human review before publication.
- Preserve existing work. Use small reviewable changes and update actual verification evidence in docs/STATUS.md.
- Run relevant checks: gradlew.bat assembleDebug testDebugUnitTest; python scripts/test_content.py; device persistence and UI checks when behavior changes. Compilation alone is not UI verification.
- Use PowerShell on Windows. Keep absolute machine paths in ignored local.properties and .local/audio.json.
- Do not upload recordings, provision services or publish a release without user direction. Source authorization is already confirmed; edited content and prelaunch review remain separate.

## Development cycle
Read Athar_Development_Cycle_Prompt.md for the complete authorized workflow. Use main as stable baseline. For each approved coherent task: fetch and fast-forward main, create a task branch, implement, run build/lint/relevant tests and device checks, review staged diff, commit, push, open PR, review final head, merge only when required checks/reviews/protections permit, then fetch/fast-forward main and verify synchronization. Use normal PR merges with matching reviewed head; never force-push or bypass protection. CI is required once established. Preserve unrelated files; stage explicit paths. Report local versus remote status and actual merge SHA. Baseline commit is the only initialization exception. Continue approved work only; do not invent a next task.
