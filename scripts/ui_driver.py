"""ADB UI inspection helpers. Tap coordinates come from the current accessibility tree."""
import os
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET

OUT = Path('.local/qa')
OUT.mkdir(parents=True, exist_ok=True)
PKG = 'dev.elm.prototype'


def adb(*args, raw=False):
    return subprocess.check_output([os.environ.get('ADB', 'adb'), *args],
        encoding=None if raw else 'utf8', errors=None if raw else 'replace', timeout=45)


def tree(name='current'):
    for _ in range(5):
        xml = adb('exec-out', 'uiautomator', 'dump', '/dev/tty')
        if '<?xml' in xml and '</hierarchy>' in xml:
            xml = xml[xml.index('<?xml'):xml.index('</hierarchy>') + len('</hierarchy>')]
            (OUT / (name + '.xml')).write_text(xml, encoding='utf8')
            return ET.fromstring(xml)
        time.sleep(1)
    raise AssertionError('UI tree unavailable')


def capture(name):
    tree(name)
    (OUT / (name + '.png')).write_bytes(adb('exec-out', 'screencap', '-p', raw=True))


def has(text):
    return any(text in n.get('text', '') for n in tree().iter('node'))


def seek_text(text):
    """Find text in a scrollable Compose screen, including content below the fold."""
    for _ in range(8):
        root = tree()
        if any(text in n.get('text', '') for n in root.iter('node')):
            return True
        scroll = next((n for n in root.iter('node') if n.get('scrollable') == 'true'), None)
        if scroll is None:
            return False
        b = list(map(int, re.findall(r'\d+', scroll.get('bounds', ''))))
        if len(b) != 4:
            return False
        x = str((b[0] + b[2]) // 2)
        adb('shell', 'input', 'swipe', x, str(b[3] - 50), x, str(b[1] + 50), '300')
        time.sleep(.5)
    return False


def tap(text):
    for _ in range(8):
        root = tree()
        matches = [n for n in root.iter('node') if n.get('text') == text and n.get('enabled') == 'true']
        if matches:
            b = list(map(int, re.findall(r'\d+', matches[-1].get('bounds', ''))))
            if len(b) == 4 and b[2] > b[0] and b[3] > b[1]:
                adb('shell', 'input', 'tap', str((b[0]+b[2])//2), str((b[1]+b[3])//2))
                time.sleep(.5)
                return
        scroll = next((n for n in root.iter('node') if n.get('scrollable') == 'true'), None)
        if scroll is not None:
            b = list(map(int, re.findall(r'\d+', scroll.get('bounds'))))
            x = str((b[0]+b[2])//2)
            adb('shell', 'input', 'swipe', x, str(b[3]-50), x, str(b[1]+50), '300')
        else:
            time.sleep(.8)
    capture('missing-control')
    raise AssertionError('Missing control: ' + text)


def launch():
    adb('shell', 'am', 'start', '-n', PKG + '/.MainActivity')
    time.sleep(2)
