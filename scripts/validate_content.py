import argparse, datetime, json, pathlib

def validate(pack, publish=False):
 errors=[]
 def check(ok,msg):
  if not ok: errors.append(msg)
 check(pack.get('schemaVersion')==1,'Unsupported schema version')
 check(isinstance(pack.get('contentVersion'),int),'Missing content version')
 check(bool(pack.get('language')),'Missing language')
 if publish: check(not pack.get('developmentOnly',False),'Development fixtures cannot be published')
 permissions={p['id']:p for p in pack.get('permissions',[])}
 recordings={r['id']:r for r in pack.get('recordings',[])}
 segments={s['id']:s for s in pack.get('segments',[])}
 concepts={c['id'] for c in pack.get('concepts',[])}
 def authorized(ref): return permissions.get(ref,{}).get('status')=='authorized'
 def reviewed(item):
  roles=set()
  for r in item.get('reviews',[]):
   try: datetime.date.fromisoformat(r.get('date',''))
   except ValueError: continue
   if r.get('reviewer') and r.get('version')==item.get('version') and r.get('result')=='approved': roles.add(r.get('role'))
  return {'language','religious'}.issubset(roles)
 for r in recordings.values(): check(authorized(r.get('permissionRef')),'Recording lacks collection authorization: '+r['id'])
 for s in segments.values():
  recording=recordings.get(s.get('recordingId'),{})
  check(bool(recording),'Unknown segment recording: '+s['id'])
  check(0<=s.get('startMs',-1)<s.get('endMs',-1)<=recording.get('durationMs',-1),'Invalid source timestamps: '+s['id'])
  if publish: check(s.get('boundaryReviewed',False),'Segment boundary unreviewed: '+s['id'])
 for l in pack.get('lessons',[]):
  check(bool(l.get('language')) and isinstance(l.get('version'),int),'Lesson language/version missing')
  check(l.get('segmentRef') in segments,'Unknown lesson source segment')
  check(authorized(l.get('permissionRef')),'Lesson authorization missing')
  if publish: check(l.get('state') in ['approved','published'] and reviewed(l),'Lesson lacks reviews of current version')
  for q in l.get('questions',[]):
   check(q.get('conceptId') in concepts,'Unknown question concept')
   check(isinstance(q.get('version'),int),'Question version missing')
   check(0<=q.get('correctIndex',-1)<len(q.get('choices',[])),'Invalid answer index')
   check(bool(q.get('explanation')),'Explanation missing')
   ref=q.get('sourceRef',{})
   if publish:
    seg=segments.get(ref.get('segmentId'),{})
    check(bool(seg) and seg.get('startMs',0)<=ref.get('startMs',-1)<ref.get('endMs',-1)<=seg.get('endMs',-1) and ref.get('contentVersion')==pack.get('contentVersion'),'Answer lacks versioned source passage')
    check(reviewed(q),'Question lacks reviews of current version')
   else: check(bool(ref),'Question source missing')
 check(bool(pack.get('lessons')),'No lessons')
 return errors

if __name__=='__main__':
 p=argparse.ArgumentParser(); p.add_argument('pack'); p.add_argument('--publish',action='store_true'); a=p.parse_args()
 errors=validate(json.loads(pathlib.Path(a.pack).read_text(encoding='utf8')),a.publish)
 print('\n'.join(errors) if errors else 'Content valid for '+('publication' if a.publish else 'development'))
 raise SystemExit(bool(errors))
