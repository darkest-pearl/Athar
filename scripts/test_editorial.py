"""Workbook decision controls stay draft-only and preserve independent choices."""
from pathlib import Path
import tempfile
import unittest
import xml.etree.ElementTree as ET
from zipfile import ZipFile

from editorial_review import NS, WORKBOOK, review_report


def workbook_with_changes(destination, changes):
    with ZipFile(WORKBOOK) as source, ZipFile(destination, 'w') as output:
        for info in source.infolist():
            data = source.read(info.filename)
            if info.filename in changes:
                root = ET.fromstring(data)
                for address, value in changes[info.filename].items():
                    cell = root.find(f".//x:c[@r='{address}']", NS)
                    if cell is None:
                        raise AssertionError(f'Missing workbook cell {address}')
                    text = cell.find('.//x:t', NS)
                    if text is None:
                        text = ET.SubElement(ET.SubElement(cell,
                            '{%s}is' % NS['x']), '{%s}t' % NS['x'])
                    text.text = value
                data = ET.tostring(root, encoding='utf8', xml_declaration=True)
            output.writestr(info, data)


class EditorialWorkbookTest(unittest.TestCase):
    def test_original_workbook_selects_nothing(self):
        report = review_report()
        self.assertFalse(report['publication_approved'])
        self.assertEqual({'Undecided': 45}, report['concepts']['decisions'])
        self.assertEqual({'Undecided': 90}, report['questions']['decisions'])
        self.assertEqual([], report['concepts']['selected'])
        self.assertEqual([], report['questions']['selected'])

    def test_keep_and_order_are_independent_and_still_draft(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / 'review.xlsx'
            workbook_with_changes(path, {
                'xl/worksheets/sheet1.xml': {'C9': 'Keep', 'B9': '7', 'C10': 'Remove'},
                'xl/worksheets/sheet2.xml': {'D9': 'Keep', 'E9': '8', 'D11': 'Keep'},
            })
            report = review_report(path)
            self.assertEqual(['u3-concept-001'],
                [item['id'] for item in report['concepts']['selected']])
            self.assertEqual(['u3-question-001-learn'],
                [item['id'] for item in report['questions']['selected']])
            self.assertEqual(7, report['concepts']['selected'][0]['effective_order'])
            self.assertEqual(8, report['questions']['selected'][0]['effective_order'])
            self.assertFalse(report['publication_approved'])

    def test_invalid_order_is_rejected(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / 'review.xlsx'
            workbook_with_changes(path, {'xl/worksheets/sheet1.xml': {'B9': '0'}})
            with self.assertRaisesRegex(ValueError, 'Invalid positive order'):
                review_report(path)

    def test_edit_and_defer_remain_distinct_from_approval(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / 'review.xlsx'
            workbook_with_changes(path, {
                'xl/worksheets/sheet1.xml': {'C9': 'Edit', 'C10': 'Defer'},
                'xl/worksheets/sheet2.xml': {'D9': 'Edit', 'D10': 'Defer', 'D11': 'Keep'},
            })
            report = review_report(path)
            self.assertEqual(['u3-concept-001'],
                [item['id'] for item in report['concepts']['selected']])
            self.assertEqual(['u3-question-001-learn'],
                [item['id'] for item in report['questions']['selected']])
            self.assertEqual('Edit', report['questions']['selected'][0]['decision'])
            self.assertEqual(1, report['questions']['decisions']['Defer'])
            self.assertFalse(report['publication_approved'])

    def test_excel_shared_string_decision_is_read(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / 'review.xlsx'
            with ZipFile(WORKBOOK) as source, ZipFile(path, 'w') as output:
                shared = ET.fromstring(source.read('xl/sharedStrings.xml'))
                index = len(shared.findall('x:si', NS))
                ET.SubElement(ET.SubElement(shared, '{%s}si' % NS['x']),
                    '{%s}t' % NS['x']).text = 'Keep'
                for info in source.infolist():
                    data = source.read(info.filename)
                    if info.filename == 'xl/worksheets/sheet1.xml':
                        root = ET.fromstring(data)
                        cell = root.find(".//x:c[@r='C9']", NS)
                        for child in list(cell):
                            cell.remove(child)
                        cell.set('t', 's')
                        ET.SubElement(cell, '{%s}v' % NS['x']).text = str(index)
                        data = ET.tostring(root, encoding='utf8', xml_declaration=True)
                    elif info.filename == 'xl/sharedStrings.xml':
                        data = ET.tostring(shared, encoding='utf8', xml_declaration=True)
                    output.writestr(info, data)
            report = review_report(path)
            self.assertEqual(['u3-concept-001'],
                [item['id'] for item in report['concepts']['selected']])


if __name__ == '__main__':
    unittest.main()
