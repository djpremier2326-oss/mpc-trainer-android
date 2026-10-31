from pathlib import Path

path = Path('app/src/main/assets/wishes/wishes.js')
text = path.read_text(encoding='utf-8')
original = text

replacements = [
    ("x.duration<=60", "x.duration<=300", "saved wish duration validation"),
    ("file.size>20000000", "file.size>100000000", "video file size limit"),
    ("Видео должно быть до 20 МБ", "Видео должно быть до 100 МБ", "video file size message"),
    ("video.duration>60", "video.duration>300", "video duration checks"),
    ("Добавь короткое видео до 1 минуты", "Добавь видео до 5 минут", "video duration message"),
]

for old, new, label in replacements:
    count = text.count(old)
    if count == 0:
        raise SystemExit(f'Wishes video patch failed: {label} not found')
    text = text.replace(old, new)
    print(f'{label}: {count} replacement(s)')

if text == original:
    raise SystemExit('Wishes video patch made no changes')

path.write_text(text, encoding='utf-8')
print('Wishes video limit increased to 5 minutes / 100 MB')
