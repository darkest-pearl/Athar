# First teaching lesson - reviewer packet

Prepared 21 October 2026. This packet is for the content and language reviewers. The app engineering uses a **neutral development fixture**; no teaching lesson is approved or installed as reviewed content yet. The beginner course must be chosen by the Sheikh/content team, not inferred from which recordings happen to be available. Existing collection/source permission was confirmed by Musab on 1 October 2026 and is recorded in [CONTENT.md](../CONTENT.md); it does not approve edited lesson content or public release.

Fill [the sample-lesson authoring template](../SAMPLE_LESSON_AUTHORING_TEMPLATE.md) for one self-contained lesson. Approximately 2-4 source-linked questions are suggested if appropriate to its passage. Keep unknown entries blank rather than guessing. The requested inputs are:

| Input still needed | Team entry / review record |
| --- | --- |
| Beginner course and lesson order decision; course ID/title and first unit | **Pending** |
| Recording manifest ID, verified file SHA-256/duration, and speaker attribution if known | **Pending** |
| Lesson title, objective, teaching notes and concise takeaway in reviewed Tigrinya | **Pending** |
| Lesson segment and question-specific original-file start/end boundaries, with boundary reviewer/date/result | **Pending** |
| Question prompts, 2-6 choices each, zero-based answer index, rationale and explanation | **Pending** |
| Exact source passages per answer, distinguishing quotations, translation and summary | **Pending** |
| Language reviewer, date, version, result and corrections | **Pending** |
| Religious reviewer, date, version, result and corrections; Sheikh periodic checkpoint | **Pending** |

Stable course/lesson/question/concept IDs and positive versions must follow [the content contract](../../content/CONTRACT.md). A correction to prompt, answer, explanation or source requires an appropriate new version and fresh review. The JSON review fields record supplied claims but cannot authenticate a reviewer. The strict `python scripts/validate_content.py <pack> --publish` gate is for an actually reviewed candidate; development validity alone is insufficient.

The local debug APK at `app/build/outputs/apk/debug/app-debug.apk` can demonstrate the import/audio flow with neutral material. Its SHA-256 is `B75472B68ABE8FFFFAD07A17516F34F3A4B0E3E5228C8AA1912E0872DEF46219`. Use [the emulator evidence](../evidence/m3-guided/README.md) and [interaction specification](../design/athar-learning-experience.md) to evaluate flow. The APK is a generated local build output and is intentionally outside Git. No original recording should be uploaded for this packet; local Android document selection is the current test path.

The [interface-copy review sheet](interface-copy.csv) exports resource keys and English strings with blank Tigrinya and reviewer fields. The [physical-device checklist](PHYSICAL_DEVICE_CHECKLIST.md) records hardware checks that engineering could not claim from the silent emulator. The Sheikh's agreed periodic content reviews and prelaunch checkpoint remain required.
