from pathlib import Path

INDEX = Path('app/src/main/assets/index.html')
index = INDEX.read_text(encoding='utf-8')
link = '<link rel="stylesheet" href="violet/timeline.css">'

if link not in index:
    anchor = '<link rel="stylesheet" href="violet/fixes2.css">'
    if anchor not in index:
        raise SystemExit('Missing Violet fixes stylesheet link')
    index = index.replace(anchor, anchor + link, 1)

INDEX.write_text(index, encoding='utf-8')
print('Violet timeline task style enabled')
