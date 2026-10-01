import argparse, hashlib, json, pathlib, sys
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1] / '.tools/python'))
from mutagen.mp3 import MP3
p=argparse.ArgumentParser(); p.add_argument('folder'); p.add_argument('--output',default='content/source-manifest.json'); a=p.parse_args()
root=pathlib.Path(a.folder).resolve(); records=[]
for f in sorted(root.rglob('*')):
 if not f.is_file(): continue
 s=f.stat(); h=hashlib.sha256()
 with f.open('rb') as stream:
  for block in iter(lambda:stream.read(1024*1024),b''): h.update(block)
 r={'id':'recording-'+f.stem,'relativePath':f.relative_to(root).as_posix(),'format':f.suffix.lower()[1:],'bytes':s.st_size,'sha256':h.hexdigest(),'permissionRef':'permission-nawaqid-2026-10-01','sourceOrder':None,'issues':['Source order and original title require reviewer confirmation.']}
 import re
 match=re.search(r'(\d+)$',f.stem); r['filenameOrderHint']=int(match.group(1)) if match else None
 try:
  audio=MP3(f); r.update(durationSeconds=round(audio.info.length,3),bitrate=audio.info.bitrate,sampleRate=audio.info.sample_rate,channels=audio.info.channels)
 except Exception as e: r['issues'].append('Unreadable audio metadata: '+str(e)); r['durationSeconds']=None
 if (s.st_size,s.st_mtime_ns)!=(f.stat().st_size,f.stat().st_mtime_ns): raise RuntimeError('Source changed during inventory: '+str(f))
 records.append(r)
seen={}
for r in records:
 if r['sha256'] in seen: r['issues'].append('Duplicate of '+seen[r['sha256']])
 else: seen[r['sha256']]=r['relativePath']
records.sort(key=lambda r:(r['filenameOrderHint'] or 999,r['relativePath']))
manifest={'schemaVersion':1,'collectionId':'nawaqid-al-islam','collectionTitle':'Nawaqid al-Islam','speaker':'Sheikh Abduselam Negash','permission':{'id':'permission-nawaqid-2026-10-01','status':'authorized','reportedBy':'Musab Mohammed Ibrahim','reportedOn':'2026-10-01','source':'Elm_Project_Blueprint.md and user kickoff','scope':'Student-supplied collection; local intake and development','conditions':['Periodic Sheikh review','Sheikh review immediately before launch'],'lessonApproval':'Not implied by source authorization'},'recordings':records,'totalBytes':sum(r['bytes'] for r in records),'totalDurationSeconds':round(sum(r['durationSeconds'] or 0 for r in records),3),'issues':['Filename sequence is a hint, not confirmed lesson order.','Collection completeness is reported by Musab, not independently established.','Source owner, original titles and publication links not supplied.']}
out=pathlib.Path(a.output); out.parent.mkdir(parents=True,exist_ok=True); out.write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
print(json.dumps({'files':len(records),'bytes':manifest['totalBytes'],'durationSeconds':manifest['totalDurationSeconds'],'duplicates':len(records)-len(seen),'unreadable':sum(r['durationSeconds'] is None for r in records)}))
