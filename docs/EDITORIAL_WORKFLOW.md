# Draft concept and question selection

Musab makes content-selection decisions in the root `Athar_English_Course_Review.xlsx` workbook. The workbook remains an authoring surface, separate from the learner app and publication approval. It currently has 45 concept rows and 90 question rows, all **Undecided**.

## Controls

| Action | Course sheet | Questions sheet | Effect |
| --- | --- | --- | --- |
| Keep, Edit, Remove, Defer, Undecided | Decision column C | Decision column D | Keep and Edit identify draft candidates. Remove excludes an item. Defer preserves it for later. Undecided does not select it. |
| Change order | Your order column B | Your order column E | Positive whole number, unique among selected items of that type. Blank keeps proposed order. |
| Edit text and record reasons | Teaching card G; notes J | Prompt F; choices G-J; answer K; explanation L; hint M; notes P | Keep the stable ID. Changed wording or answer needs a new content version and fresh applicable review before import. |

Use `python scripts/editorial_review.py` to validate the workbook and write an ignored `.local/editorial-selection.json` report. The report lists selected draft concepts and questions with stable IDs, effective order, source/page context, editable material, and a fingerprint. It is deliberately **not** a content pack, has no `schemaVersion`, and sets `publication_approved` to false. A selected question whose concept is not selected remains in the workbook but does not enter the candidate list. The tool never edits the workbook or the installed learner pack.

The authored source information in the workbook is a review locator, not an exact quotation anchor. Before creating a publishable pack, reviewers must confirm the named edition, exact passage boundaries and wording, answer and distractor accuracy, English and future Tigrinya wording, and applicable religious review for that content version. Source permission is already confirmed; selection and content approval are separate. The English draft file stays labelled draft until those decisions are versioned. Neutral engineering fixtures remain the only active concept course for this implementation slice.
