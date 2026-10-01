import copy,json,pathlib,unittest
from validate_content import validate
class ContentTests(unittest.TestCase):
 def setUp(self): self.pack=json.loads(pathlib.Path('content/fixture-pack.json').read_text())
 def test_fixture_valid_only_in_development(self):
  self.assertEqual([],validate(self.pack)); self.assertTrue(validate(self.pack,True))
 def test_bad_timestamp_rejected(self):
  self.pack['segments'][0]['endMs']=999999; self.assertTrue(validate(self.pack))
 def test_revoked_authorization_rejected(self):
  self.pack['permissions'][0]['status']='revoked'; self.assertTrue(validate(self.pack))
 def test_publication_requires_matching_reviews_and_passage(self):
  p=self.pack; p['developmentOnly']=False; p['segments'][0]['boundaryReviewed']=True
  lesson=p['lessons'][0]; lesson['state']='approved'
  for item in [lesson]+lesson['questions']:
   item['reviews']=[{'role':role,'reviewer':'test-reviewer','date':'2026-10-01','version':1,'result':'approved'} for role in ['language','religious']]
  for q in lesson['questions']: q['sourceRef']={'segmentId':'tone-all','startMs':0,'endMs':1000,'contentVersion':1}
  self.assertEqual([],validate(p,True)); lesson['questions'][0]['reviews'][0]['version']=0; self.assertTrue(validate(p,True))


class RegressionTests(unittest.TestCase):
 def setUp(self): self.pack=json.loads(pathlib.Path('content/fixture-pack.json').read_text(encoding='utf8'))
 def assert_bad(self, expected):
  errors=validate(self.pack)
  self.assertTrue(any(expected in error for error in errors), errors)
 def test_required_text(self):
  for field in ('title','language'):
   with self.subTest(field=field):
    pack=copy.deepcopy(self.pack); pack['lessons'][0][field]=' '; self.assertTrue(validate(pack))
  for field in ('prompt','explanation'):
   with self.subTest(field=field):
    pack=copy.deepcopy(self.pack); pack['lessons'][0]['questions'][0][field]=' '; self.assertTrue(validate(pack))
 def test_choices(self):
  for choices in ([], [' ', ' '], ['same','same'], ['one']):
   with self.subTest(choices=choices):
    pack=copy.deepcopy(self.pack); pack['lessons'][0]['questions'][0]['choices']=choices; self.assertTrue(validate(pack))
 def test_duplicate_ids_and_scopes(self):
  self.pack['lessons'][0]['questions'][1]['id']='q0'; self.assert_bad('duplicate ID q0')
  self.pack['lessons'].append(copy.deepcopy(self.pack['lessons'][0])); self.assert_bad('duplicate ID controls-fixture')
 def test_boolean_integers(self):
  for path in ('contentVersion','startMs','correctIndex','version'):
   pack=copy.deepcopy(self.pack)
   if path=='contentVersion': pack[path]=True
   elif path=='startMs': pack['segments'][0][path]=True
   elif path=='correctIndex': pack['lessons'][0]['questions'][0][path]=False
   else: pack['lessons'][0][path]=True
   with self.subTest(path=path): self.assertTrue(validate(pack))
 def test_malformed_timestamps_do_not_raise(self):
  self.pack['segments'][0]['startMs']='bad'; self.assert_bad('startMs')
  self.pack['segments'][0]['startMs']=1; self.pack['segments'][0]['endMs']=1; self.assert_bad('invalid source timestamps')
 def test_missing_collections_and_schema(self):
  for field in ('permissions','recordings','segments','concepts','lessons'):
   pack=copy.deepcopy(self.pack); del pack[field]
   with self.subTest(field=field): self.assertTrue(validate(pack))
  self.pack['schemaVersion']=2; self.assert_bad('unsupported schema')
 def test_invalid_references_and_state(self):
  self.pack['lessons'][0]['state']='invented'; self.assert_bad('invalid state')
  self.pack['lessons'][0]['questions'][0]['conceptId']='missing'; self.assert_bad('unknown concept')
 def test_malformed_input(self):
  self.assertTrue(validate(None))
  self.pack['lessons']=[42]; self.assertTrue(validate(self.pack))

if __name__ == '__main__': unittest.main()
