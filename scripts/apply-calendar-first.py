#!/usr/bin/env python3
"""Attach calendar-first assets after legacy theme build patches."""
from pathlib import Path
file = Path('app/src/main/assets/index.html')
html = file.read_text(encoding='utf-8')
styles = (
    '<link rel="stylesheet" href="calendar-first/calendar.css">'
    '<link rel="stylesheet" href="calendar-first/home-v2.css">'
)
scripts = (
    '<script src="calendar-first/calendar.js"></script>'
    '<script src="calendar-first/home-v2.js"></script>'
)
if 'calendar-first/calendar.css' not in html:
    assert '</head>' in html, 'Calendar CSS anchor missing'
    html = html.replace('</head>', styles + '</head>', 1)
if 'calendar-first/calendar.js' not in html:
    assert '</body>' in html, 'Calendar JavaScript anchor missing'
    html = html.replace('</body>', scripts + '</body>', 1)
file.write_text(html, encoding='utf-8')
print('Calendar-first v0.2 assets attached after theme transformations')
