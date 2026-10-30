"""Generate neutral schema-2 authoring/Android parity fixture, never religious teaching."""
import json
from pathlib import Path


def make_pack():
    concepts = [
        ('map-key', 'Read a map key', 'A map key explains what a symbol means. A blue line in this practice map means a river.',
         'What does a map key explain?', ['The meaning of a symbol', 'The battery level', 'The age of the paper'], 0,
         'A dotted line appears on a new map. How do you find its meaning?',
         ['Assume every line means a river', 'Read the key for that map', 'Measure the paper'], 1,
         'Use the key belonging to that map; symbols can differ between maps.'),
        ('route-plan', 'Plan a route', 'A route connects a start and a destination. First locate both, then follow the connecting path.',
         'What does a route connect?', ['Two random symbols', 'A start and a destination', 'A title and a date'], 1,
         'You found the library on a map. What else do you need to plan your walk?',
         ['Your starting point and a connecting path', 'Only the map title', 'Only the colour of the library symbol'], 0,
         'A destination alone does not describe a route. Locate your start and the path between them.')]
    pack = dict(schemaVersion=2, contentVersion=1, developmentOnly=True, language='en',
                course={'id': 'concept-lab', 'title': 'Map skills · neutral development fixture'},
                permissions=[{'id': 'fixture-permission', 'status': 'authorized'}], recordings=[], segments=[],
                concepts=[{'id': x[0]} for x in concepts], textSources=[{
                    'id': 'map-lab', 'edition': 'Athar neutral engineering notes, edition 1',
                    'permissionRef': 'fixture-permission'}], textPassages=[], lessons=[], units=[])
    for n, (cid, title, text, prompt, choices, answer, later, alternatives, later_answer, reason) in enumerate(concepts):
        pack['textPassages'].append(dict(id=cid, version=1, sourceId='map-lab',
            locator=f'Engineering note {n + 1}, complete authored paragraph', text=text, wording='summary'))
        questions = [dict(id=f'{cid}-{suffix}', version=1, conceptId=cid, prompt=p,
            choices=c, correctIndex=a, explanation=reason, difficulty=d, hint=text,
            sourceRef=dict(kind='text-passage', passageId=cid, contentVersion=1))
            for suffix, p, c, a, d in [('learn', prompt, choices, answer, 'recognise'),
                                      ('apply', later, alternatives, later_answer, 'apply')]]
        pack['lessons'].append(dict(id=f'{cid}-lesson', version=1, title=title, language='en', state='draft',
            permissionRef='fixture-permission', notes=text, objective=title, takeaway=reason,
            prerequisiteConceptIds=[] if n == 0 else ['map-key'], questions=questions,
            activities=[dict(id=f'{cid}-teach', kind='concept', reference=cid),
                        dict(id=f'{cid}-supported', kind='choice', reference=questions[0]['id']),
                        dict(id=f'{cid}-explain', kind='explanation', reference=cid),
                        dict(id=f'{cid}-later', kind='choice', reference=questions[1]['id'])]))
    pack['units'] = [dict(id='maps', title='Map skills', lessonIds=[x['id'] for x in pack['lessons']])]
    return pack


if __name__ == '__main__':
    data = json.dumps(make_pack(), ensure_ascii=False, indent=2) + '\n'
    for path in ['content/concept-fixture-pack.json', 'app/src/main/assets/concept-fixture-pack.json']:
        Path(path).write_text(data, encoding='utf8')
