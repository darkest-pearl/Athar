import argparse
import datetime
import json
import pathlib
import re

ID = re.compile(r"^[a-z][a-z0-9-]{0,63}$")
STATES = {"draft", "in-review", "approved", "published"}


def validate(pack, publish=False):
    errors = []

    def check(ok, message):
        if not ok:
            errors.append(message)
        return ok

    def obj(value, where):
        if not isinstance(value, dict):
            errors.append(f"{where}: expected object")
            return {}
        return value

    def nonblank(value, where):
        return check(isinstance(value, str) and bool(value.strip()) and len(value) <= 4096, f"{where}: nonblank string required")

    def integer(value, where, minimum=0):
        return check(type(value) is int and value >= minimum, f"{where}: integer >= {minimum} required")

    def identifier(value, where):
        return check(isinstance(value, str) and bool(ID.fullmatch(value)), f"{where}: invalid stable ID")

    def member(value, mapping):
        return isinstance(value, str) and value in mapping

    def collection(parent, key, where="pack"):
        value = parent.get(key)
        if not isinstance(value, list) or not value:
            errors.append(f"{where}.{key}: nonempty collection required")
            return []
        maximum = 20 if key == "questions" else {
            "permissions": 100, "recordings": 100, "segments": 500,
            "concepts": 500, "lessons": 100
        }.get(key, 100)
        check(len(value) <= maximum, f"{where}.{key}: at most {maximum} items")
        return value

    def index(items, where):
        result = {}
        for n, raw in enumerate(items):
            item = obj(raw, f"{where}[{n}]")
            value = item.get("id")
            if identifier(value, f"{where}[{n}].id"):
                if value in result:
                    errors.append(f"{where}: duplicate ID {value}")
                else:
                    result[value] = item
        return result

    pack = obj(pack, "pack")
    check(type(pack.get("schemaVersion")) is int and pack.get("schemaVersion") == 1,
          "pack.schemaVersion: unsupported schema version (expected 1)")
    integer(pack.get("contentVersion"), "pack.contentVersion", 1)
    nonblank(pack.get("language"), "pack.language")
    check(type(pack.get("developmentOnly")) is bool, "pack.developmentOnly: boolean required")
    if publish:
        check(pack.get("developmentOnly") is False, "Development fixtures cannot be published")
    course = obj(pack.get("course"), "pack.course")
    identifier(course.get("id"), "pack.course.id")
    nonblank(course.get("title"), "pack.course.title")
    permissions = index(collection(pack, "permissions"), "permissions")
    recordings = index(collection(pack, "recordings"), "recordings")
    segments = index(collection(pack, "segments"), "segments")
    concepts = index(collection(pack, "concepts"), "concepts")
    lessons = index(collection(pack, "lessons"), "lessons")

    def authorized(ref, where):
        check(member(ref, permissions) and permissions[ref].get("status") == "authorized",
              f"{where}: collection authorization missing or revoked")

    def reviewed(item, where):
        roles = set()
        reviews = item.get("reviews", [])
        if not isinstance(reviews, list):
            errors.append(f"{where}.reviews: collection required")
            reviews = []
        for n, raw in enumerate(reviews):
            review = obj(raw, f"{where}.reviews[{n}]")
            try:
                date = review.get("date")
                if not isinstance(date, str):
                    raise ValueError()
                datetime.date.fromisoformat(date)
            except ValueError:
                errors.append(f"{where}.reviews[{n}].date: invalid ISO date")
                continue
            if (nonblank(review.get("reviewer"), f"{where}.reviews[{n}].reviewer")
                    and review.get("version") == item.get("version")
                    and review.get("result") == "approved"):
                roles.add(review.get("role")) if isinstance(review.get("role"), str) else None
        check({"language", "religious"}.issubset(roles),
              f"{where}: current-version language and religious reviews required")

    for pid, permission in permissions.items():
        check(member(permission.get("status"), {"authorized", "pending", "revoked"}),
              f"permissions.{pid}.status: invalid status")
    for rid, recording in recordings.items():
        path = recording.get("relativePath")
        if nonblank(path, f"recordings.{rid}.relativePath"):
            check(not path.startswith("/") and ".." not in path and "\\" not in path,
                  f"recordings.{rid}.relativePath: unsafe path")
        integer(recording.get("durationMs"), f"recordings.{rid}.durationMs", 1)
        checksum, byte_count = recording.get("sha256"), recording.get("bytes")
        check((checksum is None) == (byte_count is None),
              f"recordings.{rid}: sha256 and bytes must be supplied together")
        if checksum is not None:
            check(isinstance(checksum, str) and bool(re.fullmatch(r"[0-9a-f]{64}", checksum)),
                  f"recordings.{rid}.sha256: lowercase SHA-256 required")
            integer(byte_count, f"recordings.{rid}.bytes", 1)
        if "title" in recording and recording["title"] is not None:
            nonblank(recording["title"], f"recordings.{rid}.title")
        authorized(recording.get("permissionRef"), f"recordings.{rid}")
    for sid, segment in segments.items():
        recording = recordings.get(segment.get("recordingId")) if isinstance(segment.get("recordingId"), str) else None
        check(recording is not None, f"segments.{sid}: unknown recording")
        start, end = segment.get("startMs"), segment.get("endMs")
        valid = integer(start, f"segments.{sid}.startMs") & integer(end, f"segments.{sid}.endMs", 1)
        if valid and recording and type(recording.get("durationMs")) is int:
            check(start < end <= recording["durationMs"], f"segments.{sid}: invalid source timestamps")
        if publish:
            check(segment.get("boundaryReviewed") is True, f"segments.{sid}: boundary unreviewed")
    for lid, lesson in lessons.items():
        where = f"lessons.{lid}"
        integer(lesson.get("version"), f"{where}.version", 1)
        nonblank(lesson.get("title"), f"{where}.title")
        nonblank(lesson.get("language"), f"{where}.language")
        check(member(lesson.get("state"), STATES), f"{where}.state: invalid state")
        check(member(lesson.get("segmentRef"), segments), f"{where}: unknown source segment")
        authorized(lesson.get("permissionRef"), where)
        if "notes" in lesson and lesson["notes"] is not None:
            nonblank(lesson["notes"], f"{where}.notes")
        prerequisites = lesson.get("prerequisiteConceptIds", [])
        if not isinstance(prerequisites, list):
            errors.append(f"{where}.prerequisiteConceptIds: array required")
            prerequisites = []
        check(len(prerequisites) <= 20, f"{where}.prerequisiteConceptIds: too many items")
        if len([x for x in prerequisites if isinstance(x, str)]) != len(set(x for x in prerequisites if isinstance(x, str))):
            errors.append(f"{where}.prerequisiteConceptIds: duplicate ID")
        for ref_id in prerequisites:
            check(member(ref_id, concepts), f"{where}: unknown prerequisite concept")
        if publish:
            check(member(lesson.get("state"), {"approved", "published"}), f"{where}: not approved")
            reviewed(lesson, where)
        questions = index(collection(lesson, "questions", where), f"{where}.questions")
        for qid, question in questions.items():
            qwhere = f"{where}.questions.{qid}"
            integer(question.get("version"), f"{qwhere}.version", 1)
            nonblank(question.get("prompt"), f"{qwhere}.prompt")
            nonblank(question.get("explanation"), f"{qwhere}.explanation")
            check(member(question.get("conceptId"), concepts), f"{qwhere}: unknown concept")
            choices = question.get("choices")
            if not isinstance(choices, list) or not 2 <= len(choices) <= 6:
                errors.append(f"{qwhere}.choices: 2-6 choices required")
                choices = []
            for n, choice in enumerate(choices):
                nonblank(choice, f"{qwhere}.choices[{n}]")
            strings = [x.strip() for x in choices if isinstance(x, str)]
            if len(strings) != len(set(strings)):
                errors.append(f"{qwhere}.choices: duplicate choices")
            answer = question.get("correctIndex")
            if integer(answer, f"{qwhere}.correctIndex"):
                check(answer < len(choices), f"{qwhere}.correctIndex: out of range")
            ref = obj(question.get("sourceRef"), f"{qwhere}.sourceRef")
            if publish:
                segment = segments.get(ref.get("segmentId")) if isinstance(ref.get("segmentId"), str) else None
                start, end = ref.get("startMs"), ref.get("endMs")
                valid = segment is not None and type(start) is int and type(end) is int
                check(valid and type(segment.get("startMs")) is int and type(segment.get("endMs")) is int
                      and segment["startMs"] <= start < end <= segment["endMs"]
                      and ref.get("contentVersion") == pack.get("contentVersion"),
                      f"{qwhere}: versioned source passage required")
                reviewed(question, qwhere)
            else:
                if ref.get("kind") == "fixture-text":
                    check(ref.get("lessonId") == lid, f"{qwhere}: invalid fixture source lesson")
                    integer(ref.get("contentVersion"), f"{qwhere}.sourceRef.contentVersion", 1)
                    nonblank(ref.get("passage"), f"{qwhere}.sourceRef.passage")
                    check(ref.get("passage") in {"notes", "interface-contract"},
                          f"{qwhere}: unknown fixture passage")
                    if ref.get("passage") == "notes":
                        nonblank(lesson.get("notes"), f"{qwhere}: notes passage")
                else:
                    check(ref.get("kind") in (None, "recording-passage"),
                          f"{qwhere}: invalid source kind")
                    segment = segments.get(ref.get("segmentId")) if isinstance(ref.get("segmentId"), str) else None
                    check(segment is not None, f"{qwhere}: source reference missing or invalid")
                    start, end = ref.get("startMs"), ref.get("endMs")
                    if integer(start, f"{qwhere}.sourceRef.startMs") & integer(end, f"{qwhere}.sourceRef.endMs", 1) and segment:
                        check(segment["startMs"] <= start < end <= segment["endMs"],
                              f"{qwhere}: invalid source passage")
                    integer(ref.get("contentVersion"), f"{qwhere}.sourceRef.contentVersion", 1)
    return errors


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("pack")
    parser.add_argument("--publish", action="store_true")
    args = parser.parse_args()
    try:
        pack = json.loads(pathlib.Path(args.pack).read_text(encoding="utf8"))
        errors = validate(pack, args.publish)
    except (OSError, json.JSONDecodeError) as exc:
        errors = [f"Unable to read content pack: {exc}"]
    print("\n".join(errors) if errors else "Content valid for " + ("publication" if args.publish else "development"))
    raise SystemExit(bool(errors))
