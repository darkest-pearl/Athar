"""Export Android string resources for human interface-copy review."""

import csv
from pathlib import Path
import re
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "app/src/main/res/values/strings.xml"
DESTINATION = ROOT / "docs/review/interface-copy.csv"


def context_for(key: str) -> str:
    if key.startswith(("audio_", "play_", "pause_", "replay_", "full_recording", "source_", "recording_")):
        return "Audio and answer source"
    if key.startswith(("review_", "due_", "nothing_due", "start_review", "linked_", "assisted_", "independent_")):
        return "Spaced review"
    if key.startswith(("path_", "unit_", "course_", "nav_learn", "explore_path")):
        return "Course path"
    if key.startswith(("lesson_", "listen_", "takeaway_", "practice_", "question_", "check_answer", "correct_", "incorrect_", "next_question", "finish_lesson")):
        return "Guided lesson and feedback"
    if key.startswith(("progress_", "streak_", "study_", "daily_goal", "milestone_", "badge_")):
        return "Progress and participation"
    if key.startswith(("settings_", "import_", "local_pack", "use_", "technical_", "development_")):
        return "Settings and development"
    if key.startswith("session_"):
        return "Session recovery"
    if key.startswith("nav_"):
        return "Navigation"
    return "General learner interface"


def main() -> None:
    resources = ET.parse(SOURCE).getroot()
    rows = []
    for item in resources.findall("string"):
        key = item.attrib["name"]
        value = "".join(item.itertext())
        placeholders = ", ".join(dict.fromkeys(re.findall(r"%(?:\d+\$)?[ds]", value)))
        rows.append([key, context_for(key), value, placeholders, "", "", "", "", ""])
    DESTINATION.parent.mkdir(parents=True, exist_ok=True)
    with DESTINATION.open("w", encoding="utf-8-sig", newline="") as output:
        writer = csv.writer(output)
        writer.writerow(["resource_key", "context", "english", "placeholders_preserve",
                         "tigrinya", "language_reviewer", "review_date", "reviewed_version", "result_corrections"])
        writer.writerows(rows)
    print(f"Exported {len(rows)} strings to {DESTINATION.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
