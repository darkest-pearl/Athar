"""Verify ordinary document-picker import and course switching on disposable CI."""
import json
import os
import time
from pathlib import Path
from ui_driver import OUT, PKG, adb, capture, has, launch, tap, tree

assert os.environ.get('ATHAR_ISOLATED_QA') == '1'
source = json.loads(Path('content/concept-fixture-pack.json').read_text(encoding='utf8'))
bundled_title = source['course']['title']
source['course']['id'] = 'imported-neutral-map-check'
source['course']['title'] = 'Imported map study'
local = OUT / 'athar-import-check.json'
local.write_text(json.dumps(source), encoding='utf8')
adb('push', str(local), '/sdcard/Download/athar-import-check.json')
adb('shell', 'pm', 'clear', PKG)
launch()
capture('import-fresh-concept')
assert has('Map skills')
tap('Settings')
tap('Select local content pack')
capture('import-document-picker')

def visible(value):
    return any(n.get('text') == value or n.get('content-desc') == value
        for n in tree().iter('node'))

if not visible('athar-import-check.json'):
    for drawer in ('Show roots', 'Browse', 'Show navigation drawer'):
        if visible(drawer):
            tap(drawer)
            break
    if visible('Downloads'):
        tap('Downloads')
capture('import-downloads')
tap('athar-import-check.json')
for _ in range(10):
    if has('Imported map study'):
        break
    time.sleep(1)
assert has('Imported map study'), 'Visible document import did not select the course'
capture('imported-course-selected')
tap('Start lesson')
assert has('One idea')
tap('Continue')
tap('The meaning of a symbol')
tap('Check answer')
tap('Continue')
tap('Continue')
tap('Read the key for that map')
tap('Check answer')
tap('Finish lesson')
tap('Exit to path')
assert has('1 of 2 lessons complete')
capture('imported-course-progress')
tap('Choose course')
tap('Select course: ' + bundled_title)
assert has('0 of 2 lessons complete')
capture('import-switched-to-bundled')
tap('Choose course')
tap('Select course: Imported map study')
assert has('1 of 2 lessons complete')
adb('shell', 'am', 'force-stop', PKG)
launch()
assert has('Imported map study') and has('1 of 2 lessons complete')
capture('import-restored-after-restart')
(OUT / 'import-checks.json').write_text(json.dumps({
    'selected_with_system_document_picker': True,
    'imported_completion_kept_in_course': True,
    'bundled_demo_progress_stayed_separate': True,
    'imported_bytes_and_selection_survived_switch_and_restart': True,
}, indent=2), encoding='utf8')
