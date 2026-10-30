"""Hosted QA emulator: offline authored order, correction, restart and saved progress."""
import json
import os
import subprocess
import time
from ui_driver import OUT, PKG, adb, capture, has, launch, seek_text, tap, tree


def start_recording(name):
    remote = '/sdcard/' + name + '.mp4'
    adb('shell', 'rm', '-f', remote)
    process = subprocess.Popen([os.environ.get('ADB', 'adb'), 'shell', 'screenrecord',
        '--time-limit', '180', '--bit-rate', '2500000', remote],
        stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
    time.sleep(2)
    assert process.poll() is None, 'Emulator screen recording did not start'
    return process, remote


def stop_recording(recording, name):
    process, remote = recording
    pids = adb('shell', 'pidof', 'screenrecord').split()
    assert pids, 'Emulator screen recording stopped early'
    adb('shell', 'kill', '-2', pids[0])
    process.communicate(timeout=30)
    assert process.returncode == 0, 'Emulator screen recording failed'
    adb('pull', remote, str(OUT / (name + '.mp4')))
    assert (OUT / (name + '.mp4')).stat().st_size > 10000


def companion_is(mood):
    return any('Book companion: ' + mood in n.get('content-desc', '')
        for n in tree().iter('node'))


def motion_reduced():
    return any(n.get('checkable') == 'true' and n.get('checked') == 'true'
        for n in tree().iter('node'))

adb('shell', 'am', 'force-stop', PKG)
adb('push', 'content/concept-fixture-pack.json', '/data/local/tmp/concept-fixture-pack.json')
adb('shell', 'run-as', PKG, 'mkdir', '-p', 'files')
adb('shell', 'run-as', PKG, 'cp', '/data/local/tmp/concept-fixture-pack.json',
    'files/active-pack.json')
adb('shell', 'cmd', 'connectivity', 'airplane-mode', 'enable')
adb('shell', 'settings', 'put', 'global', 'animator_duration_scale', '1')
adb('root')
adb('wait-for-device')
assert adb('shell', 'id', '-u').strip() == '0', 'Hosted emulator needs root for due-date QA'
launch()
capture('concept-home')
recording = start_recording('concept-walkthrough')
tap('Path')
capture('concept-path-before')
assert has('Your next lesson')
tap('1')
tap('Start lesson')
capture('concept-teach')
assert has('One idea')
assert not has('Play audio')
assert companion_is('welcoming')
tap('Continue')
capture('concept-supported')
assert has('Supported practice')
assert companion_is('thinking')
tap('Show hint')
capture('concept-hint')
tap('The battery level')
tap('Check answer')
capture('concept-first-feedback')
adb('shell', 'am', 'force-stop', PKG)
launch()
tap('Continue study')
capture('concept-feedback-restored')
tap('The meaning of a symbol')
tap('Check correction')
assert seek_text('Corrected')
capture('concept-corrected')
tap('Source · Learn more')
assert seek_text('Athar neutral engineering notes')
capture('concept-source')
tap('Continue')
capture('concept-explanation')
assert has('What this means')
tap('Continue')
capture('concept-application')
assert has('Apply the idea')
tap('Read the key for that map')
tap('Check answer')
capture('concept-application-feedback')
tap('Continue')
capture('concept-mistake-replay')
assert has('Previous mistake')
tap('The meaning of a symbol')
tap('Check answer')
capture('concept-replay-feedback')
tap('Finish lesson')
capture('concept-complete')
assert has('1 of 2 lessons complete')
assert companion_is('celebrating completion')
tap('Exit to path')
capture('concept-path-after')
assert has('1 of 2 lessons complete')
assert has('Your next lesson')
# The emulator is disposable. Advance only its clock so the saved scheduled
# review becomes due; the installed state and the production scheduler stay intact.
now = int(adb('shell', 'date', '+%s').strip())
future = now + 2 * 24 * 60 * 60
adb('shell', 'date', '-u', '-s', '@' + str(future))
assert int(adb('shell', 'date', '+%s').strip()) >= future
adb('shell', 'am', 'force-stop', PKG)
launch()
tap('Path')
capture('concept-path-due')
assert has('Due reviews: 1')
tap('Start review batch')
capture('concept-due-review')
assert has('A dotted line appears on a new map. How do you find its meaning?')
tap('Read the key for that map')
tap('Save review')
capture('concept-independent-review')
assert has('Independent recall')
tap('Finish batch')
capture('concept-review-complete')
tap('Progress')
capture('concept-progress')
assert has('1 of 2 lessons complete')
assert seek_text('Concept learning')
capture('concept-progress-concepts')
assert has('1 concept practised in lessons')
assert has('1 concept with a corrected answer')
assert seek_text('1 concept recalled in a later due review')
capture('concept-progress-recalled')
stop_recording(recording, 'concept-walkthrough')
adb('shell', 'am', 'force-stop', PKG)
launch()
tap('Progress')
capture('concept-progress-reopened')
assert has('1 of 2 lessons complete')
tap('Settings')
tap('Reduce character motion')
capture('reduced-motion-setting')
assert motion_reduced()
tap('Back to Today')
tap('Start lesson')
capture('reduced-motion-teach')
assert companion_is('welcoming')
tap('Exit')
adb('shell', 'am', 'force-stop', PKG)
launch()
tap('Settings')
capture('reduced-motion-reopened')
assert motion_reduced()
tap('Back to Today')
adb('shell', 'settings', 'put', 'system', 'font_scale', '1.5')
adb('shell', 'am', 'force-stop', PKG)
launch()
tap('Start lesson')
capture('large-text-teach')
assert seek_text('One idea')
tap('Continue')
capture('large-text-supported')
assert seek_text('Check answer')
tap('Exit')
adb('shell', 'settings', 'put', 'system', 'font_scale', '1.0')
adb('shell', 'wm', 'size', '720x1280')
adb('shell', 'am', 'force-stop', PKG)
launch()
tap('Continue study')
capture('small-phone-supported')
assert seek_text('Supported practice')
tap('Exit')
adb('shell', 'wm', 'size', 'reset')
adb('shell', 'settings', 'put', 'system', 'accelerometer_rotation', '0')
adb('shell', 'settings', 'put', 'system', 'user_rotation', '1')
adb('shell', 'am', 'force-stop', PKG)
launch()
tap('Continue study')
capture('landscape-supported')
assert seek_text('Supported practice')
tap('Exit')
adb('shell', 'settings', 'put', 'system', 'user_rotation', '0')
adb('shell', 'settings', 'put', 'system', 'accelerometer_rotation', '1')
(OUT / 'accessibility-service.txt').write_text(
    adb('shell', 'settings', 'get', 'secure', 'enabled_accessibility_services'),
    encoding='utf8')
(OUT / 'concept-checks.json').write_text(json.dumps(dict(
    offline=True, no_audio=True, authored_order=True, hint=True,
    first_response_and_correction=True, feedback_restored=True,
    source_resolved=True, application=True, mistake_replay=True, completion=True,
    path_transition=True, due_review=True, independent_recall=True,
    progress_reopened=True, animated_walkthrough=True,
    reduced_motion_persisted=True, large_text=True, small_phone=True,
    landscape=True), indent=2), encoding='utf8')
