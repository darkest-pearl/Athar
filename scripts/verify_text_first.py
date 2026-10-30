"""Isolated CI emulator only: text-only completion, offline source, durable feedback.

Does not clear app data. Use only on a QA installation: the active pack is replaced.
"""
import json
from ui_driver import OUT, PKG, adb, capture, has, launch, seek_text, tap

adb('shell', 'am', 'force-stop', PKG)
adb('push', 'content/concept-fixture-pack.json', '/data/local/tmp/concept-fixture-pack.json')
adb('shell', 'run-as', PKG, 'mkdir', '-p', 'files')
adb('shell', 'run-as', PKG, 'cp', '/data/local/tmp/concept-fixture-pack.json', 'files/active-pack.json')
adb('shell', 'cmd', 'connectivity', 'airplane-mode', 'enable')
launch()
capture('text-home')
tap('Start lesson')
capture('text-intro')
assert not has('Play audio')
tap('Continue to takeaway')
capture('text-takeaway')
tap('Try practice questions')
tap('The battery level')
tap('Check answer')
capture('text-correction')
assert has('Let’s look again')
adb('shell', 'am', 'force-stop', PKG)
launch()
tap('Continue study')
assert has('Let’s look again')
capture('text-feedback-restored')
tap('Answer source')
assert seek_text('Athar neutral engineering notes')
capture('text-source')
tap('Next question')
tap('Read the key for that map')
tap('Check answer')
capture('text-application')
tap('Finish lesson')
capture('text-complete')
tap('See progress')
capture('text-progress')
adb('shell', 'am', 'force-stop', PKG)
launch()
tap('Progress')
capture('text-progress-reopened')
(OUT / 'text-checks.json').write_text(json.dumps(dict(
    offline=True, no_audio=True, feedback_restored=True, source_resolved=True,
    completion=True, progress_reopened=True), indent=2), encoding='utf8')
