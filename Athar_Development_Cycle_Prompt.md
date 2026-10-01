# Athar — Git setup and recurring development cycle

Paste the instructions below into the active Codex conversation. They apply during the current initialization and continue through the approved project milestones. The project was previously called Elm; Athar is the confirmed app name.

---

Adopt this development workflow now, while you are reading the project files and initializing the workspace. Preserve the existing project requirements and continue the current task. You are authorized to initialize local version control, create branches, make commits, push to the intended configured repository, create pull requests, merge eligible completed work, and continue with the next approved task. Do not repeatedly ask permission for these routine steps. Respect the environment’s actual permissions and the repository’s access controls.

Use this cycle for each coherent change: establish a current main baseline, create a task branch, implement, verify, commit, push when connected, review the final change, merge when ready, verify the resulting main, refresh the local main, then create a new branch for the next task.

1. Inspect before initializing.

Read the applicable AGENTS.md instructions and project documents. Inspect the actual workspace, existing files, Git status, repository root, remotes, and any worktrees. Check whether this folder already belongs to a parent repository before creating a new repository. Never reinitialize or replace existing history just because Git setup has not yet been discussed.

Use the current operating system’s shell and the project’s configured Git identity. Do not invent an author identity or change global Git settings. Resolve any in-progress Git operation before starting another one. Preserve all existing files and other people’s changes.

2. Establish the repository and baseline.

If this is a new standalone project without Git, initialize it with main as the default branch. First add an appropriate Android .gitignore and inspect exactly what will be tracked. Exclude credentials, private signing keys, machine-specific configuration, build output, APKs, and large original audio recordings. Keep required source files, sanitized examples, relevant lockfiles, and the Gradle wrapper tracked.

Make one meaningful initial baseline commit containing the reviewed project documents and any valid starter configuration already present. Label this as an initialization baseline; do not claim the app builds if there is no app yet. This initial commit is the setup exception to the rule that subsequent implementation happens on task branches.

Use the intended repository URL only when it has been supplied or reliably established from the existing configuration. Verify the destination before pushing. If a known remote already contains history, base work on that history and preserve local files; do not overwrite the remote or automatically combine unrelated histories.

If no GitHub repository or authentication is available yet, continue local commits, branches, checks, and merges. Request the missing repository URL or authentication once at a useful checkpoint. Clearly mark remote publishing as pending. When connecting later, inspect the remote history before pushing; stop only the unsafe integration if histories require a migration decision. Never fabricate successful pushes or PRs.

3. Start one branch per coherent task.

Use main as the stable integration branch. If an existing repository uses another default branch, use that branch consistently without renaming it as a side effect.

Before each new task, fetch the intended remote when connected and fast-forward the clean local main to its current remote counterpart. Use explicit fast-forward-only synchronization; unexpected divergence requires inspection and preservation of local commits, not a hard reset. Without a remote, start from the latest verified local main.

Create a descriptive branch such as feat/audio-player, feat/lesson-quizzes, fix/review-scheduling, chore/android-ci, or docs/content-review. Define a small outcome and acceptance criteria before editing. Keep related code, tests, and documentation together. Avoid unrelated refactoring or new features outside the approved backlog.

If initialization has already produced uncommitted implementation work, preserve it on an appropriate task branch. Use a separate worktree when needed to avoid disturbing unrelated work. Do not indiscriminately stash, stage, or commit someone else’s changes.

4. Implement with useful checkpoints.

Make focused, coherent changes and commit at meaningful checkpoints. Inspect the staged diff before each commit. Stage specific files or hunks; avoid blindly committing everything in the workspace. Follow existing commit conventions, or use clear messages such as feat(player): resume lesson playback and fix(review): prevent duplicate completion credit.

Push useful branch checkpoints when connected. Work in progress may be pushed to a branch, but it remains unmerged until the branch meets its acceptance criteria. Preserve published history; use additional commits rather than rewriting shared branches. Never force-push main or use destructive cleanup to make a check appear clean.

Update relevant documentation and project status within the task branch. Record implemented and verified work accurately; record the actual merge result after it happens in the handoff or PR. Avoid creating unrelated direct commits on main merely to announce a merge.

5. Verify proportionately.

Discover the Gradle wrapper and actual available tasks. For Android code changes, run the relevant build, lint, and unit-test gates, plus focused emulator/device checks when behavior requires them. Inspect affected screens for UI changes, including Tigrinya text, Arabic direction, enlarged text, and reduced motion where relevant.

Add meaningful regression tests for logic fixes, especially review scheduling, streak dates, persistence, duplicate actions, and migrations. Do not add tests that merely repeat the implementation. A documentation-only change needs an appropriate review and any required repository checks; it does not automatically need every Android test.

Record commands, results, and the commit checked. Distinguish passed, failed, unavailable, and not applicable. Fix defects and repeat the affected checks. Never claim checks ran when they did not, weaken tests to obtain green results, or skip required gates. Continue independent work when a local tool is unavailable, but keep unmet merge requirements explicit.

Inspect existing CI. If none exists, establish a small CI workflow for pull requests and main in an appropriate setup branch once the project can build. Prefer build, lint, and relevant tests with compatible pinned tooling and useful dependency caching. Do not make workflow failures disappear by disabling verification or giving the workflow unnecessary write permissions.

6. Review and prepare the branch for integration.

Review the complete diff against the latest main for correctness, missing error states, accidental deletions, credentials, large binaries, dependency changes, persistence risks, and unrelated edits. Fix concrete findings on the same branch. Keep the review proportional to what changed.

If main advanced, integrate the relevant updated base into the feature branch and repeat affected checks. Resolve conflicts deliberately by understanding both sides. Do not blindly choose ours or theirs. Any new change after verification requires the relevant verification again.

7. Push and open a pull request when GitHub is connected.

Push the task branch and create one PR for the coherent change, targeting the intended default branch. Reuse an existing PR for that branch. The description should explain the problem, resulting behavior, checks actually run, relevant screenshots, and any material limitation or migration consideration. Use a body file or structured tool argument for multiline text.

Use draft status for unfinished work. Mark the PR ready when its acceptance criteria are satisfied. Monitor the actual checks for the latest pushed revision. If another contributor changes the branch, inspect that update before merging.

8. Merge only completed, eligible work.

Merge automatically when the change is ready and all applicable repository conditions are satisfied. Honor required reviews, checks, signing requirements, and merge queues. Use the repository’s established merge method; for a new repository, prefer squash merging self-contained PRs unless a different strategy is required.

On GitHub, use the normal PR merge mechanism. When supported, match the PR head to the revision you reviewed, for example through the GitHub CLI’s --match-head-commit option. If a merge queue is required, use it and follow its result. Enabling auto-merge or entering a queue is not confirmation that a merge happened.

Never use administrator bypass, disable protections, manufacture approvals, or falsify checks. If a required human review or external service blocks merging, leave the branch and PR intact, report the exact blocker, and continue independent work that is safe. Do not treat unmerged dependent work as the next main baseline.

When operating only locally, use an ordinary merge commit after the same applicable local checks and diff review. Direct remote main pushes are only an integration fallback when PR tooling is unavailable and the repository demonstrably permits them. Missing access is not a reason to bypass protections.

9. Verify main and clean up safely.

Confirm the actual merge result and record its commit identifier. After a GitHub merge, fetch the remote and fast-forward the clean local main; the server-side merge has already updated remote main. Do not create or push a duplicate local merge.

Confirm the merged result is present in main. When nobody else has advanced it, local main and remote main should match; if others have advanced it, verify the merged result is included and synchronize to the newer baseline. Preserve any unexpectedly divergent local work.

Check the applicable main CI result and perform a focused smoke check when it addresses an integration risk. Do not repeat a large suite without a reason when the required result already verifies the same code. If main is broken, prioritize recovery before the next dependent feature.

Delete only the task branch that has been verified as merged, with no later unique commits or uncommitted work. Squash merges do not preserve the original branch’s ancestry in main, so verify the PR’s merged source revision before deleting it. Retain branches or worktrees when deletion safety is uncertain. Never delete the default branch or another contributor’s work.

10. Continue through the approved work.

After successful integration, start a new branch from the updated main for the next approved task. Serialize integration so later work starts from a known baseline. Do not reuse the merged branch or quietly base a feature on an unmerged dependency.

Repeat until the current approved milestone or backlog is complete, I redirect you, or a genuine blocker prevents safe progress. Do not invent additional features to keep the loop running. At a session boundary, preserve a useful local checkpoint, push it when possible, and record the exact resume state.

11. Recover and report accurately.

For a regression, create a focused fix branch. When reverting is the safest recovery, create a new revert commit through the same workflow rather than rewriting main. Inspect dependent changes and the parent selection if reverting a merge commit. Explain the affected functionality and verify the recovery.

After each integration, report the completed task, branch, PR link if one exists, merged commit, relevant verification results, main synchronization state, unresolved issues, and next task. If a push or merge is still pending, say so explicitly.

Persist this workflow concisely in the existing repository guidance and development documentation through the same branch process. Source-code integration can proceed using clearly labeled development fixtures. Publishing religious teaching content and launching the app still follow the agreed content reviews and the Sheikh’s prelaunch checkpoint.

Begin with repository inspection and initialization now, then continue the Android milestone already assigned. Do not stop after presenting a plan when the work can proceed.

---

Official references checked on 1 October 2026:

- https://git-scm.com/docs/git-pull
- https://git-scm.com/docs/git-worktree
- https://cli.github.com/manual/gh_pr_merge
- https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches
- https://git-scm.com/docs/git-revert
