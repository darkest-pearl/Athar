# Physical Android device pilot checklist

Use a disposable/test app profile for destructive resets. Install the same APK as [the pilot packet](FIRST_LESSON_PACKET.md) with `adb install -r` or Android's ordinary update flow, without clearing app data. Keep the original recordings local and unchanged. Record the device, Android version, build hash, reviewer and date before checking. Mark each row pass/fail/not run and write observed behavior or a defect reference; blank means **not run**, never pass.

| Field | Entry |
| --- | --- |
| Device model / Android version | |
| Tester / date / APK SHA-256 | |
| Local pack version / recording identity checked | |

| Check | Expected behavior | Result and observed behavior / defect |
| --- | --- | --- |
| Update over an installed app without clearing data | Existing lesson answers, bookmarks, review state, study days and a pending session survive Room migration. | |
| Airplane mode / offline launch | Bundled or imported pack, saved progress and locally selected audio remain usable without network. | |
| Select the verified local recording | Checksum/size identity matches the manifest; a wrong file is rejected while the prior working choice remains. | |
| Listen to lesson and exact answer passage | Sound is audible and intelligible; passage begins/stops at reviewed full-file boundaries and replay restarts there. | |
| Open full recording, seek and return | Drag preview and committed seek agree; passage/lesson/full positions remain separate. | |
| Background and screen off | Document actual foreground-only pause and resume behavior; note any mismatch, interruption or lost position. | |
| Headset, unplug, call and audio focus | Record expected versus heard pause/resume and output route; log any defect. | |
| Rotate during an unfinished review | Same batch, cursor, saved feedback, hints/reveals and due qualification remain. | |
| Force-stop/reopen after a partial lesson | Continue study restores the same stage and committed answer without changing its grade. | |
| Finish one lesson or due-review batch | Study day credits once; path and next step update from saved state. | |
| 150% or larger text; long supplied Tigrinya/Arabic | Primary action reachable; text not clipped; a native reader judges wording and reading comfort. | |
| TalkBack | Control names, focus order, answer feedback and audio seek value are understandable. | |
| Missing/revoked recording access | Clear recovery message, no silent fallback to unrelated audio. | |

Emulator coverage and its limits are recorded in [guided evidence](../evidence/m3-guided/README.md). Actual teaching wording, answer correctness, segment boundaries and native-language comfort require the named human reviewers in the [packet](FIRST_LESSON_PACKET.md); a checked device cannot substitute for editorial approval.
