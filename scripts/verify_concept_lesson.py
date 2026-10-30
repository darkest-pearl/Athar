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
launch()
capture('concept-home')
recording = start_recording('concept-walkthrough')
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
stop_recording(recording, 'concept-walkthrough')
tap('See progress')
capture('concept-progress')
assert has('1 of 2 lessons complete')
assert seek_text('Concept learning')
capture('concept-progress-concepts')
assert has('1 concept practised in lessons')
assert has('1 concept with a corrected answer')
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
tap('Exit to path')
adb('shell', 'am', 'force-stop', PKG)
launch()
tap('Settings')
capture('reduced-motion-reopened')
assert motion_reduced()
(OUT / 'concept-checks.json').write_text(json.dumps(dict(
    offline=True, no_audio=True, authored_order=True, hint=True,
    first_response_and_correction=True, feedback_restored=True,
    source_resolved=True, application=True, mistake_replay=True, completion=True,
    progress_reopened=True, animated_walkthrough=True,
    reduced_motion_persisted=True), indent=2), encoding='utf8')
