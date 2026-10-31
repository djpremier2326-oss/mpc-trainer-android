from pathlib import Path

path = Path('app/src/main/assets/index.html')
text = path.read_text(encoding='utf-8')
link = '<link rel="stylesheet" href="blueberry/fixes.css">'
if link not in text:
    text = text.replace('</head>', link + '</head>', 1)
path.write_text(text, encoding='utf-8')
print('Blueberry fixes stylesheet linked')
