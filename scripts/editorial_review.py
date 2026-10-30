"""Inspect Musab's workbook decisions without activating teaching content.

Uses only the Python standard library. Output is a draft review report, not an
Android content pack or publication approval.
"""
import argparse
from collections import Counter
from hashlib import sha256
import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
WORKBOOK = ROOT / 'Athar_English_Course_Review.xlsx'
NS = {'x': 'http://schemas.openxmlformats.org/spreadsheetml/2006/main'}
DECISIONS = {'Undecided', 'Keep', 'Edit', 'Remove', 'Defer'}


def rows_from_sheet(archive, sheet_number, shared_strings):
    root = ET.fromstring(archive.read(f'xl/worksheets/sheet{sheet_number}.xml'))
    rows = []
    for row in root.findall('.//x:sheetData/x:row', NS):
        cells = {}
        for cell in row.findall('x:c', NS):
            col = re.match(r'[A-Z]+', cell.get('r', '')).group()
            inline = cell.find('x:is', NS)
            value = cell.find('x:v', NS)
            raw = (''.join(inline.itertext()) if inline is not None else
                value.text if value is not None and value.text else '')
            cells[col] = (shared_strings[int(raw)] if cell.get('t') == 's' and raw
                else raw).strip()
        rows.append((int(row.get('r')), cells))
    return rows


def read_workbook(path=WORKBOOK):
    with ZipFile(path) as archive:
        if archive.testzip() is not None:
            raise ValueError('Workbook ZIP checksum failed')
        shared_strings = []
        if 'xl/sharedStrings.xml' in archive.namelist():
            shared_root = ET.fromstring(archive.read('xl/sharedStrings.xml'))
            shared_strings = [''.join(item.itertext()) for item in
                shared_root.findall('x:si', NS)]
        concepts = []
        for row_number, c in rows_from_sheet(archive, 1, shared_strings):
            if row_number < 9 or not c.get('D'):
                continue
            concepts.append(dict(id=c['D'], proposed_order=int(c['A']),
                your_order=c.get('B', ''), decision=c.get('C', ''), unit=c.get('E', ''),
                title=c.get('F', ''), teaching_card=c.get('G', ''),
                printed_pages=c.get('H', ''), evidence=c.get('I', ''),
                notes=c.get('J', ''), editorial_note=c.get('K', '')))
        questions = []
        for row_number, c in rows_from_sheet(archive, 2, shared_strings):
            if row_number < 9 or not c.get('A'):
                continue
            choices = [c.get(col, '') for col in 'GHIJ']
            while choices and not choices[-1]:
                choices.pop()
            questions.append(dict(id=c['A'], concept_id=c.get('B', ''),
                phase=c.get('C', ''), decision=c.get('D', ''), your_order=c.get('E', ''),
                proposed_order=row_number - 8, prompt=c.get('F', ''),
                choices=choices,
                correct_letter=c.get('K', ''), explanation=c.get('L', ''),
                hint=c.get('M', ''), printed_pages=c.get('N', ''),
                evidence=c.get('O', ''), notes=c.get('P', ''),
                editorial_note=c.get('Q', '')))
    return concepts, questions


def review_report(path=WORKBOOK):
    concepts, questions = read_workbook(path)
    if len(concepts) != 45 or len(questions) != 90:
        raise ValueError('Expected 45 concept rows and 90 question rows; review the workbook structure')
    for label, items in [('concept', concepts), ('question', questions)]:
        ids = [item['id'] for item in items]
        if len(ids) != len(set(ids)):
            raise ValueError(f'Duplicate {label} ID')
        for item in items:
            if item['decision'] not in DECISIONS:
                raise ValueError(f"Invalid {label} decision: {item['id']}")
            order = item['your_order']
            if order and (not order.isdigit() or int(order) < 1):
                raise ValueError(f"Invalid positive order: {item['id']}")
            item['effective_order'] = int(order) if order else item['proposed_order']
            authored = {key: value for key, value in item.items() if key not in
                ('decision', 'your_order', 'effective_order', 'proposed_order', 'notes')}
            item['draft_fingerprint'] = sha256(json.dumps(authored, sort_keys=True,
                ensure_ascii=False).encode('utf8')).hexdigest()
        selected = [item for item in items if item['decision'] in ('Keep', 'Edit')]
        ranks = [item['effective_order'] for item in selected]
        if len(ranks) != len(set(ranks)):
            raise ValueError(f'Duplicate selected {label} order')
    by_concept = {item['id']: item for item in concepts}
    for q in questions:
        if q['concept_id'] not in by_concept:
            raise ValueError(f"Unknown concept for question: {q['id']}")
        if (q['correct_letter'] not in 'ABCD' or
            ord(q['correct_letter']) - ord('A') >= len(q['choices']) or
            len(q['choices']) not in (3, 4) or any(not value for value in q['choices'])):
            raise ValueError(f"Invalid correct choice: {q['id']}")
    selected_concepts = sorted((item for item in concepts if item['decision'] in ('Keep', 'Edit')),
        key=lambda item: item['effective_order'])
    selected_questions = sorted((item for item in questions if item['decision'] in ('Keep', 'Edit')
        and by_concept[item['concept_id']]['decision'] in ('Keep', 'Edit')),
        key=lambda item: item['effective_order'])
    return dict(status='draft-selection-only', publication_approved=False,
        workbook=str(Path(path).name), concepts=dict(decisions=dict(Counter(
            item['decision'] for item in concepts)), selected=selected_concepts),
        questions=dict(decisions=dict(Counter(item['decision'] for item in questions)),
            selected=selected_questions),
        requirements=['Musab selects and orders candidates',
            'Edited wording receives a new content version and fresh review',
            'Exact source locators, language and religious review remain required',
            'Only a separately validated reviewed pack may be imported into Athar'])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--workbook', type=Path, default=WORKBOOK)
    parser.add_argument('--output', type=Path, default=ROOT / '.local/editorial-selection.json')
    args = parser.parse_args()
    report = review_report(args.workbook)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2, ensure_ascii=False) + '\n', encoding='utf8')
    print('Draft editorial report:', args.output)
    print('Concepts:', report['concepts']['decisions'])
    print('Questions:', report['questions']['decisions'])
    print('Selected:', len(report['concepts']['selected']), 'concepts,',
        len(report['questions']['selected']), 'questions; publication remains blocked')


if __name__ == '__main__':
    main()
