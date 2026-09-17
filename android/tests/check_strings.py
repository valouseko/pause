"""Validate translation coverage and format arguments before building the APK."""
from collections import Counter
from pathlib import Path
import re
import xml.etree.ElementTree as ET

source = Path(__file__).resolve().parents[1] / 'app/src/main'

def load(locale):
    nodes = ET.parse(source / 'res' / locale / 'strings.xml').getroot()
    values = {node.attrib['name']: node.text or '' for node in nodes}
    assert len(values) == len(nodes), f'Duplicate key in {locale}'
    assert all(values.values()), f'Empty translation in {locale}'
    return values

english, czech = load('values'), load('values-cs')
assert english.keys() == czech.keys(), 'Missing translation keys'
for key in english:
    placeholders = lambda text: Counter(re.findall(r'%\d+\$[sd]', text))
    assert placeholders(english[key]) == placeholders(czech[key]), f'Format mismatch: {key}'
for file in (source / 'java').rglob('*.kt'):
    for key in re.findall(r'R\.string\.(\w+)', file.read_text(encoding='utf-8')):
        assert key in english, f'Unknown resource {key} in {file.name}'
print(f'PASS: {len(english)} complete EN/CZ translations with matching format arguments')
