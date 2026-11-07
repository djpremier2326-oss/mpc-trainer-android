#!/usr/bin/env python3
"""Attach calendar-first assets after legacy theme build patches."""
from pathlib import Path
file = Path('app/src/main/assets/index.html')
html = file.read_text(encoding='utf-8')
css = '<link rel="stylesheet" href="calendar-first/calendar.css">'
js = '<script src="calendar-first/calendar.js"></script>'
if css not in html:
    assert '</head>' in html, 'Calendar CSS anchor missing'
    html = html.replace('</head>', css + '</head>', 1)
if js not in html:
    assert '</body>' in html, 'Calendar JavaScript anchor missing'
    html = html.replace('</body>', js + '</body>', 1)
file.write_text(html, encoding='utf-8')
print('Calendar-first assets attached after theme transformations')
