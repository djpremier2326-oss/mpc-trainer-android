from pathlib import Path

path = Path('app/src/main/assets/index.html')
text = path.read_text(encoding='utf-8')
old = "${id==='rhythm'?'theme-previews/rhythm-day.svg':'theme-previews/'+id+'-day.webp'}"
new = "${(['ice','rhythm'].includes(id)?'theme-previews/'+id+'-day.svg':'theme-previews/'+id+'-day.webp')}"
if old not in text:
    raise SystemExit('theme preview patch target not found')
path.write_text(text.replace(old, new, 1), encoding='utf-8')
print('Theme preview cards patched')
