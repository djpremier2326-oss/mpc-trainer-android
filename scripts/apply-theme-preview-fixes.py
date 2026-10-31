from pathlib import Path

path = Path('app/src/main/assets/index.html')
text = path.read_text(encoding='utf-8')
old = "${id==='rhythm'?'theme-previews/rhythm-day.svg':'theme-previews/'+id+'-day.webp'}"
new = "${(['ice','rhythm'].includes(id)?'theme-previews/'+id+'-day.svg':'theme-previews/'+id+'-day.webp')}"
if old not in text:
    raise SystemExit('theme preview patch target not found')
text = text.replace(old, new, 1)
link = '<link rel="stylesheet" href="blueberry/targeted-fixes.css">'
if link not in text:
    text = text.replace('</head>', link + '</head>', 1)
path.write_text(text, encoding='utf-8')
print('Theme preview cards patched; Blueberry targeted fixes linked')
