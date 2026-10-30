# Schema 2: text-first extension

Both validators accept integer schemaVersion 1 or 2. Legacy identities and canonical JSON remain unchanged. Schema 2 requires recordings and segments arrays but permits empty arrays. A lesson may omit/null segmentRef; present audio remains bounded and authorized. Never manufacture audio or timestamps for text.

Schema 2 adds textSources (0–100) and textPassages (0–500). A source has stable id, nonblank bibliographic edition, and authorized permissionRef. A passage has stable id, positive version, known sourceId, nonblank locator and offline text, and wording summary or quotation. Use a reviewed exact paragraph locator for quotations. Broad draft page ranges locate discussions for review; they are not exact quotation anchors. The reference PDF is not an app asset.

Question sourceRef `{kind: text-passage, passageId, contentVersion}` resolves the passage's exact version. Questions optionally declare difficulty recognise/distinguish/apply/recall and a nonblank hint. These are variants of a stable course-scoped concept. Recording and neutral fixture sources remain supported. Production text passages require exact-version language/religious reviews alongside question and lesson reviews. Non-development Android imports enforce publication requirements.

Schema-2 lessons require 1–60 ordered activities, each with stable unique id, kind, and reference. Concept/explanation reference a text passage; choice references a question in that lesson; audio references the lesson segment. Every question must be represented. Matching/order have not been added; validators reject unimplemented kinds. The next engine branch will consume this sequence; this contract branch does not yet activate the fixture by default.

Text source identities are immutable per course; passages per course/id/version. Import rejects rewrites before replacing the previous working pack. Revised passages need new versions and fresh reviews. Editorial Keep is selection only. The English draft and workbook remain separate from bundled neutral fixtures.
