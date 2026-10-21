# First Tigrinya pilot: learner-facing copy priority

Use [the versioned interface copy sheet](interface-copy.csv) for exact English source strings and reviewer columns. This list sets review order only; it supplies no translation or approval. Validate spoken and written Tigrinya on a physical phone before a teaching release.

1. **The uninterrupted first lesson path:** `nav_today`, `nav_learn`, `today_title_guided`, `today_path_label`, `path_title`, `path_state_current`, `start_lesson`, `session_continue_action`, `exit_to_path`, `start_listening`, `listen_stage_title`, `continue_to_takeaway`, `check_answer`, `finish_lesson`, `lesson_complete`, `start_next_lesson`. These labels determine whether a new learner can find and finish a lesson without English help.
2. **Correction and trust:** `correct_answer`, `incorrect_answer`, `source_passage_title`, `path_state_done`, `path_state_locked`, `daily_goal_title`, `streak_title`. Review the full explanation and source metadata from the approved sample lesson alongside these UI labels; none of the neutral QA explanations are teaching copy.
3. **Return and review:** `nav_review`, `nav_progress`, `continue_lesson`, `badge_first_lesson`, plus the remaining Progress, due-review, storage, and audio-error rows in the sheet. Keep placeholders and formatting markers exactly as documented in the CSV.

Language reviewer, religious reviewer, Sheikh checkpoint, corrected text, and reviewed version remain blank until the team supplies the first approved sample. The current APK is a development demo with English neutral fixtures.