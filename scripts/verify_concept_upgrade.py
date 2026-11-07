"""Disposable hosted emulator: real APK replacement keeps legacy work and course choice."""
import json
import os
from pathlib import Path
from ui_driver import OUT, PKG, adb, capture, has, launch, tap

assert os.environ.get('ATHAR_ISOLATED_QA') == '1', (
    'Upgrade QA may clear data only on the disposable CI emulator')
baseline = Path(os.environ['ATHAR_BASELINE_APK'])
candidate = Path('app/build/outputs/apk/debug/app-debug.apk')
assert baseline.is_file() and candidate.is_file()
assert 'Success' in adb('install', '-r', str(baseline))
adb('shell', 'pm', 'clear', PKG)
launch()
assert has('Learn the controls'), 'Baseline normal launch did not open its legacy lesson'
capture('upgrade-baseline-today')
tap('Start lesson')
tap('Start listening')
tap('Continue to takeaway')
tap('Try practice questions')
tap('Pause audio')
tap('Check answer')
tap('Next question')
tap('Finish lesson')
tap('Check answer')
tap('Finish lesson')
assert has('1 of 2 lessons complete')
capture('upgrade-baseline-complete')
tap('Start next lesson')
tap('Start listening')
tap('Continue to takeaway')
tap('Try practice questions')
tap('Bookmark')
assert has('Selected: Bookmark')
capture('upgrade-baseline-pending')
adb('shell', 'am', 'force-stop', PKG)

assert 'Success' in adb('install', '-r', str(candidate)), 'In-place APK update failed'
launch()
assert has('Map skills'), 'New default did not appear after legacy upgrade'
capture('upgrade-new-default')
tap('Choose course')
tap('Select course: App controls')
assert has('1 of 2 lessons complete'), 'Saved legacy completion disappeared'
capture('upgrade-selected-legacy')
tap('Continue study')
tap('Continue study')
assert has('Selected: Bookmark'), 'Pending legacy quiz answer did not restore'
capture('upgrade-restored-pending')
tap('Exit to path')
adb('shell', 'am', 'force-stop', PKG)
launch()
assert has('App controls') and has('1 of 2 lessons complete'), (
    'Selected legacy course or completion did not persist across restart')
tap('Choose course')
tap('Select course: Map skills · neutral development fixture')
tap('Start lesson')
assert has('One idea'), 'Visible course switch did not open concept teaching'
capture('upgrade-concept-teaching')
(OUT / 'upgrade-checks.json').write_text(json.dumps({
    'same_package_in_place_upgrade': True,
    'legacy_completion_retained': True,
    'legacy_pending_choice_retained': True,
    'selected_course_restored_after_restart': True,
    'concept_demo_reachable_through_visible_selection': True,
}, indent=2), encoding='utf8')
