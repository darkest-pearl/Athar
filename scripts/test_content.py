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
if __name__=='__main__': unittest.main()
