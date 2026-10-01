# Sample lesson handoff template

Leave unknown fields blank. The content team supplies and reviews teaching wording; engineering does not infer it from filenames or generate religious answers. Keep the source recording local until an agreed transfer path exists.

| Field | Team entry |
|---|---|
| Proposed course ID, title, curriculum decision | |
| Pack `contentVersion`, lesson stable ID and positive `version` | |
| Lesson state (`draft`, `in-review`, `approved`, `published`) | |
| Learning objective in reviewed Tigrinya | |
| Lesson title and teaching notes in reviewed Tigrinya | |
| Source recording manifest ID, speaker if known, permission reference | |
| Source file SHA-256 and duration for matching, not for Git | |
| Segment ID; source startMs and endMs on the full recording | |
| Boundary reviewer, date, and result | |
| Exact source passage or quotation; language; translation/summary kept separate | |
| Earlier learned concept IDs for optional prerequisite recall | |

For each question, copy this blank row. Use 2–6 distinct, meaningful choices and a zero-based `correctIndex`. A question ID is unique within its lesson; concept IDs are stable across lessons. A corrected prompt, answer, explanation, or source passage requires a new applicable version.

| Question field | Team entry |
|---|---|
| Question ID, positive version, concept ID | |
| Reviewed Tigrinya prompt | |
| Reviewed Tigrinya choices in display order | |
| `correctIndex` and answer rationale | |
| Reviewed Tigrinya explanation; lesson notes used for the current optional review hint | |
| `sourceRef` segment ID, passage startMs/endMs, source contentVersion | |
| Language review: reviewer, date, version, result, corrections | |
| Religious review: reviewer, date, version, result, corrections | |

Record the lesson-level language and religious reviews, permission status, and Sheikh checkpoint separately, with the reviewed version and any corrections. JSON review fields document editorial claims; they do not authenticate a person. Run the development validator while drafting, then the strict publication validator only after actual current-version approvals and reviewed source boundaries exist. See [the portable content contract](../content/CONTRACT.md) for exact field rules and bounds.
