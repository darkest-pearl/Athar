"""Create small, deterministic non-speech source-passage QA fixtures."""

import hashlib
import json
import math
import pathlib
import struct
import wave

ROOT = pathlib.Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
CONTENT = ROOT / "content/qa-passage-pack.json"
RATE = 8000
DURATION = 12


def cue(name, bands):
    path = ASSETS / f"neutral-cue-{name}.wav"
    with wave.open(str(path), "wb") as out:
        out.setnchannels(1)
        out.setsampwidth(2)
        out.setframerate(RATE)
        samples = bytearray()
        for frame in range(RATE * DURATION):
            second = frame / RATE
            frequency = next((hz for start, end, hz in bands if start <= second < end), 0)
            amplitude = int(9000 * math.sin(2 * math.pi * frequency * second)) if frequency else 0
            samples += struct.pack("<h", amplitude)
        out.writeframes(samples)
    data = path.read_bytes()
    return {
        "id": f"cue-{name}", "relativePath": path.name,
        "title": f"Neutral cue {name.upper()}", "durationMs": DURATION * 1000,
        "permissionRef": "neutral-fixture", "sha256": hashlib.sha256(data).hexdigest(),
        "bytes": len(data),
    }


def main():
    a = cue("a", [(2, 4, 440), (6, 8, 660)])
    b = cue("b", [(2, 4, 880), (6, 8, 523)])
    pack = {
        "schemaVersion": 1, "contentVersion": 1, "developmentOnly": True,
        "language": "en", "course": {"id": "audio-qa-course", "title": "Audio source QA",
            "curriculumStatus": "fixture"},
        "permissions": [{"id": "neutral-fixture", "status": "authorized",
            "source": "Developer-authored non-speech tones"}],
        "recordings": [a, b],
        "segments": [
            {"id": "cue-a-lesson", "recordingId": "cue-a", "startMs": 2000,
             "endMs": 8000, "boundaryReviewed": False},
            {"id": "cue-b-source", "recordingId": "cue-b", "startMs": 2000,
             "endMs": 8000, "boundaryReviewed": False},
        ],
        "concepts": [{"id": "first-cue"}, {"id": "other-cue"}],
        "lessons": [{
            "id": "audio-source-check", "version": 1,
            "title": "Test passage controls", "language": "en", "state": "draft",
            "segmentRef": "cue-a-lesson", "permissionRef": "neutral-fixture",
            "contentType": "audio-fixture", "notes": "Cue A changes pitch at six seconds. This is a technical audio test, not teaching content.",
            "reviews": [], "questions": [
                {"id": "cue-a-question", "version": 1, "conceptId": "first-cue",
                 "prompt": "Which cue belongs to this lesson?",
                 "choices": ["Cue A", "Cue B"], "correctIndex": 0,
                 "explanation": "The lesson segment uses cue A; the answer passage is its later tone.",
                 "sourceRef": {"kind": "recording-passage", "segmentId": "cue-a-lesson",
                     "startMs": 6000, "endMs": 8000, "contentVersion": 1}, "reviews": []},
                {"id": "cue-b-question", "version": 1, "conceptId": "other-cue",
                 "prompt": "Which cue is the second source?",
                 "choices": ["Cue A", "Cue B"], "correctIndex": 1,
                 "explanation": "This answer deliberately points to a different recording, cue B.",
                 "sourceRef": {"kind": "recording-passage", "segmentId": "cue-b-source",
                     "startMs": 2000, "endMs": 4000, "contentVersion": 1}, "reviews": []},
            ],
        }],
    }
    CONTENT.write_text(json.dumps(pack, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
