"""Focused emulator smoke check. Coordinates always come from UI XML."""
import argparse,json,pathlib,re,subprocess,time,xml.etree.ElementTree as ET
p=argparse.ArgumentParser();p.add_argument('--adb',default='adb');p.add_argument('--reset',action='store_true');p.add_argument('--start-at-source',action='store_true');a=p.parse_args()
root=pathlib.Path(__file__).resolve().parents[1]; out=root/'evidence';out.mkdir(exist_ok=True)
pkg='dev.elm.prototype'; results=[]
def adb(*args,raw=False):
 return subprocess.check_output([a.adb,*args],encoding=None if raw else 'utf8',errors=None if raw else 'replace')
def tree(name='current'):
 for retry in range(5):
  xml=adb('exec-out','uiautomator','dump','/dev/tty')
  if '<?xml' in xml and '</hierarchy>' in xml: break
  time.sleep(.5)
 else: raise AssertionError('UI dump unavailable: '+xml)
 xml=xml[xml.index('<?xml'):xml.index('</hierarchy>')+len('</hierarchy>')]
 (out/(name+'.xml')).write_text(xml,encoding='utf8');return ET.fromstring(xml)
def shot(name): (out/(name+'.png')).write_bytes(adb('exec-out','screencap','-p',raw=True))
def has(text): return any(text in n.get('text','') for n in tree().iter('node'))
def tap(text):
 for attempt in range(6):
  t=tree()
  matches=[n for n in t.iter('node') if n.get('text')==text and n.get('enabled')=='true']
  if matches:
   b=list(map(int,re.findall(r'\d+',matches[-1].get('bounds'))));
   if b[3]>b[1]: adb('shell','input','tap',str((b[0]+b[2])//2),str((b[1]+b[3])//2));time.sleep(.4);return
  s=next((n for n in t.iter('node') if n.get('scrollable')=='true'),None)
  if s is None: break
  b=list(map(int,re.findall(r'\d+',s.get('bounds')))); x=(b[0]+b[2])//2
  adb('shell','input','swipe',str(x),str(b[3]-70),str(x),str(b[1]+70),'350')
 raise AssertionError('Control not found: '+text)
def check(ok,label):
 if not ok: raise AssertionError(label)
 results.append(label); print('PASS '+label,flush=True)
def pause():
 t=tree()
 match=next((n for n in t.iter('node') if n.get('text')=='Pause audio'),None)
 if match is not None:
  b=list(map(int,re.findall(r'\d+',match.get('bounds'))));adb('shell','input','tap',str((b[0]+b[2])//2),str((b[1]+b[3])//2))
 # A short tone may naturally finish while UI inspection runs.
def top(): adb('shell','input','swipe','180','180','180','600','300')
def launch(): adb('shell','am','start','-n',pkg+'/.MainActivity');time.sleep(1)
def position():
 for n in tree().iter('node'):
  m=re.match(r'(\d+)s / (\d+)s',n.get('text',''))
  if m: return tuple(map(int,m.groups()))
 raise AssertionError('Position display missing')
if not a.start_at_source:
 if a.reset: adb('shell','pm','clear',pkg)
 launch();tree('garden-today');shot('garden-today');tap('Begin practice');tap('Open lesson')
 tree('garden-lesson');shot('garden-lesson');tap('Play audio');time.sleep(2);check(position()[0]>0,'Bundled audio position advances');pause()
 # Quiz correction and durable completion
 tap('Try practice questions');tree('garden-review');shot('garden-review');tap('Settings');tap('Check answer');check(has("Let's look again"),'Incorrect answer shows correction');tree('incorrect-feedback');shot('incorrect-feedback');tap('Next question');tap('Finish lesson');tap('Check answer');tap('Finish lesson');check(has('Completion saved'),'Completion saved after two checks');tap('See progress');check(has('1 lesson completed'),'One durable completion');tree('saved-progress');shot('saved-progress')
 adb('shell','am','force-stop',pkg);launch();tap('Progress');check(has('1 lesson completed'),'Completion survives process restart');tree('restarted-progress');shot('restarted-progress')
 # Repetition is idempotent
 tap('Learn');tap('Open lesson');tap('Try practice questions');tap('Pause audio');tap('Check answer');tap('Next question');tap('Finish lesson');tap('Check answer');tap('Finish lesson');tap('See progress');check(has('1 lesson completed'),'Repeated completion leaves count at one')
 # Alternate direction uses the same live components
 tap('Settings');tap('Compare editorial direction');tap('Back to Today');tree('editorial-today');shot('editorial-today');tap('Revisit practice');tap('Open lesson');tree('editorial-lesson');shot('editorial-lesson');tap('Try practice questions');tree('editorial-review');shot('editorial-review')
 # Network disabled; complete quiz remains available
 adb('shell','cmd','connectivity','airplane-mode','enable');tap('Pause audio');tap('Check answer');tap('Next question');tap('Finish lesson');tap('Check answer');tap('Finish lesson');check(has('Completion saved'),'Quiz and completion work with airplane mode enabled')
 tap('Learn');tap('Open lesson');tap('Play audio');time.sleep(2);check(position()[0]>0,'Bundled playback works in airplane mode');pause()
# Representative original: copy, never edit original; private debug app storage only
adb('shell','cmd','connectivity','airplane-mode','enable')
config=json.loads((root/'.local/audio.json').read_text()); source=pathlib.Path(config['recordingsRoot'])/config['representative']
adb('push',str(source),'/data/local/tmp/athar-representative.mp3');adb('shell','run-as',pkg,'mkdir','-p','files');adb('shell','run-as',pkg,'cp','/data/local/tmp/athar-representative.mp3','files/representative.mp3');adb('shell','rm','/data/local/tmp/athar-representative.mp3');adb('shell','am','force-stop',pkg);launch();tap('Learn');tap('Open lesson')
tap('Development: use injected file');
if has('Test timestamp range'): tap('Test timestamp range')
top();tap('Replay 10 seconds');tap('Play audio');time.sleep(3);check(position()[0]>0 and position()[1]==30,'Original MP3 timestamp range plays offline');pause();tree('source-range');shot('source-range');tap('Open full recording');top();tap('Play audio');time.sleep(2);check(position()[1]>30,'Full original is available');pause();before=position()[0];adb('shell','am','force-stop',pkg);launch();tap('Learn');tap('Open lesson');check(position()[0]>=before,'Original audio position resumes after restart');tree('source-resumed');shot('source-resumed')
# Enlarged text, reduced motion and scripts
adb('shell','settings','put','system','font_scale','1.5')
for key in ['animator_duration_scale','transition_animation_scale','window_animation_scale']: adb('shell','settings','put','global',key,'0')
adb('shell','am','force-stop',pkg);launch();tree('large-text-today');shot('large-text-today');tap('Settings');tree('script-test-large');shot('script-test-large')
if not has('ا ب ت ث ج ح خ'): adb('shell','input','swipe','180','600','180','200','350')
tree('script-test-large');shot('script-test-large');check(has('ሀ ሁ ሂ ሃ ሄ ህ ሆ'),'Ethiopic glyph fixture present in UI');check(has('ا ب ت ث ج ح خ'),'Arabic glyph fixture present in UI')
tap('Back to Today');tap('Revisit practice' if has('Revisit practice') else 'Begin practice');tap('Open lesson');tap('Try practice questions');tap('Pause audio');tap('Check answer');tap('Next question');tap('Finish lesson');tap('Check answer');tap('Finish lesson');check(has('Completion saved'),'Complete flow operable with 150% text and animations disabled');tree('large-text-completed');shot('large-text-completed')
adb('shell','settings','put','system','font_scale','1.0');adb('shell','cmd','connectivity','airplane-mode','disable')
for key in ['animator_duration_scale','transition_animation_scale','window_animation_scale']: adb('shell','settings','put','global',key,'1')
summary={'scope':'source-and-accessibility' if a.start_at_source else 'complete','device':adb('shell','getprop','ro.product.model').strip(),'android':adb('shell','getprop','ro.build.version.release').strip(),'display':adb('shell','wm','size').strip(),'checks':results}
(out/'verification.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2),encoding='utf8');print(json.dumps(summary,ensure_ascii=False),flush=True)
