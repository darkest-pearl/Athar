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
    return process, remote, time.monotonic()


def mp4_duration(path):
    """Read the MP4 movie clock so tap markers use video, not host, seconds."""
    data = path.read_bytes()
    def boxes(start, end):
        while start + 8 <= end:
            size = int.from_bytes(data[start:start + 4], 'big')
            header = 8
            if size == 1:
                size = int.from_bytes(data[start + 8:start + 16], 'big')
                header = 16
            elif size == 0:
                size = end - start
            if size < header or start + size > end:
                raise AssertionError('Invalid MP4 box size')
            yield data[start + 4:start + 8], start + header, start + size
            start += size
    for kind, begin, end in boxes(0, len(data)):
        if kind == b'moov':
            for child, pos, _ in boxes(begin, end):
                if child == b'mvhd':
                    version = data[pos]
                    offset = pos + (12 if version == 0 else 20)
                    width = 4 if version == 0 else 8
                    timescale = int.from_bytes(data[offset:offset + 4], 'big')
                    duration = int.from_bytes(data[offset + 4:offset + 4 + width], 'big')
                    assert timescale > 0
                    return duration / timescale
    raise AssertionError('MP4 movie clock missing')


def stop_recording(recording, name):
    process, remote, started = recording
    elapsed = time.monotonic() - started
    (OUT / 'walkthrough-duration.txt').write_text(f'{elapsed:.1f} seconds\n', encoding='utf8')
    pid_lookup = subprocess.run([os.environ.get('ADB', 'adb'), 'shell', 'pidof',
        'screenrecord'], capture_output=True, text=True, timeout=45)
    pids = pid_lookup.stdout.split()
    if pids:
        adb('shell', 'kill', '-2', pids[0])
    process.communicate(timeout=30)
    path = OUT / (name + '.mp4')
    adb('pull', remote, str(path))
    assert path.stat().st_size > 10000
    assert pids and process.returncode == 0, (
        f'Emulator recording ended at its 180-second limit after {elapsed:.1f}s; inspect video')
    return mp4_duration(path) - elapsed


def companion_is(mood):
    return any('Book companion: ' + mood in n.get('content-desc', '')
        for n in tree().iter('node'))


def motion_reduced():
    return any(n.get('checkable') == 'true' and n.get('checked') == 'true'
        for n in tree().iter('node'))

assert os.environ.get('ATHAR_ISOLATED_QA') == '1', (
    'This fresh-install walkthrough clears only the disposable CI emulator app')
adb('shell', 'cmd', 'connectivity', 'airplane-mode', 'enable')
adb('root')
adb('wait-for-device')
assert adb('shell', 'id', '-u').strip() == '0', 'Hosted emulator needs root for due-date QA'
motion_scales = ('window_animation_scale', 'transition_animation_scale',
    'animator_duration_scale')
for scale in motion_scales:
    adb('shell', 'settings', 'put', 'global', scale, '1')
observed_scales = {scale: adb('shell', 'settings', 'get', 'global', scale).strip()
    for scale in motion_scales}
assert all(float(value) == 1.0 for value in observed_scales.values()), observed_scales
(OUT / 'motion-scales.json').write_text(json.dumps(observed_scales, indent=2), encoding='utf8')
adb('shell', 'pm', 'clear', PKG)
recording = start_recording('concept-walkthrough')
motion_events = []


def timed_tap(label, event):
    def mark():
        motion_events.append(dict(event=event,
            seconds=round(time.monotonic() - recording[2], 2)))
    tap(label, before_input=mark)


launch()
capture('concept-home')
assert has('Map skills')
assert has('DEVELOPMENT DEMO')
capture('concept-path-before')
timed_tap('Start lesson', 'start_lesson_press_and_welcome')
capture('concept-teach')
assert has('One idea')
assert not has('Play audio')
assert companion_is('welcoming')
timed_tap('Continue', 'teaching_to_supported_activity')
capture('concept-supported')
assert has('Supported practice')
assert companion_is('thinking')
tap('Show hint')
timed_tap('The battery level', 'answer_selection')
timed_tap('Check answer', 'incorrect_feedback_and_reassurance')
capture('concept-first-feedback')
adb('shell', 'am', 'force-stop', PKG)
launch()
tap('Continue study')
capture('concept-feedback-restored')
tap('The meaning of a symbol')
timed_tap('Check correction', 'corrected_answer_feedback')
assert seek_text('Corrected')
capture('concept-corrected')
tap('Source · Learn more')
assert seek_text('Athar neutral engineering notes')
capture('concept-source')
tap('Close source')
timed_tap('Continue', 'feedback_to_explanation')
assert has('What this means')
tap('Continue')
capture('concept-application')
assert has('Apply the idea')
tap('Read the key for that map')
timed_tap('Check answer', 'correct_application_feedback')
tap('Continue')
capture('concept-mistake-replay')
assert has('Previous mistake')
tap('The meaning of a symbol')
tap('Check answer')
timed_tap('Finish lesson', 'completion_celebration')
capture('concept-complete')
assert has('1 of 2 lessons complete')
assert companion_is('celebrating completion')
timed_tap('Exit to path', 'path_update')
capture('concept-path-after')
assert has('1 of 2 lessons complete')
# The emulator is disposable. Advance only its clock so the saved scheduled
# review becomes due; the installed state and the production scheduler stay intact.
now = int(adb('shell', 'date', '+%s').strip())
future = now + 2 * 24 * 60 * 60
adb('shell', 'date', '-u', '-s', '@' + str(future))
assert int(adb('shell', 'date', '+%s').strip()) >= future
adb('shell', 'am', 'force-stop', PKG)
launch()
capture('concept-path-due')
assert has('Due reviews: 1')
tap('Start review batch')
assert has('A dotted line appears on a new map. How do you find its meaning?')
tap('Read the key for that map')
tap('Save review')
capture('concept-independent-review')
assert has('Independent recall')
tap('Finish batch')
tap('Progress')
assert has('1 of 2 lessons complete')
assert seek_text('1 concept recalled in a later due review')
capture('concept-progress-recalled')
assert has('1 concept practised in lessons')
assert has('1 concept with a corrected answer')
video_clock_offset = stop_recording(recording, 'concept-walkthrough')
assert len(motion_events) == 9, motion_events
for event in motion_events:
    event['seconds'] = round(event['seconds'] + video_clock_offset, 2)
(OUT / 'motion-events.json').write_text(json.dumps(motion_events, indent=2), encoding='utf8')
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
tap('Reduce character motion')
assert not motion_reduced()
tap('Back to Today')
for scale in motion_scales:
    adb('shell', 'settings', 'put', 'global', scale, '0')
assert all(float(adb('shell', 'settings', 'get', 'global', scale).strip()) == 0.0
    for scale in motion_scales)
adb('shell', 'am', 'force-stop', PKG)
launch()
tap('Continue study')
assert seek_text('One idea')
capture('system-disabled-motion-teach')
tap('Exit to path')
for scale in motion_scales:
    adb('shell', 'settings', 'put', 'global', scale, '1')
adb('shell', 'settings', 'put', 'system', 'font_scale', '1.5')
adb('shell', 'am', 'force-stop', PKG)
launch()
tap('Continue study')
capture('large-text-teach')
assert seek_text('One idea')
tap('Continue')
assert seek_text('A start and a destination')
capture('large-text-supported')
tap('Exit to path')
adb('shell', 'settings', 'put', 'system', 'font_scale', '1.0')
adb('shell', 'wm', 'size', '720x1280')
adb('shell', 'am', 'force-stop', PKG)
launch()
tap('Continue study')
assert seek_text('A start and a destination')
capture('small-phone-supported')
tap('Exit to path')
adb('shell', 'wm', 'size', 'reset')
adb('shell', 'settings', 'put', 'system', 'accelerometer_rotation', '0')
adb('shell', 'settings', 'put', 'system', 'user_rotation', '1')
adb('shell', 'am', 'force-stop', PKG)
launch()
tap('Continue study')
assert seek_text('A start and a destination')
capture('landscape-supported')
tap('Exit to path')
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
    progress_reopened=True, normal_motion_scales_enabled=True,
    reduced_motion_persisted=True, system_motion_disabled=True,
    motion_events_recorded=len(motion_events), large_text=True, small_phone=True,
    landscape=True), indent=2), encoding='utf8')
