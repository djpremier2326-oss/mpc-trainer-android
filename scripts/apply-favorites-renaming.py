from pathlib import Path

INDEX = Path('app/src/main/assets/index.html')
WISHES = Path('app/src/main/assets/wishes/wishes.js')
CSS = Path('app/src/main/assets/wishes/wishes.css')


def replace_required(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise SystemExit(f'Missing expected text for {label}: {old!r}')
    return text.replace(old, new)

# Global menus / shared UI (all themes)
index = INDEX.read_text(encoding='utf-8')
index = replace_required(
    index,
    "['wishes','speaker','Мои желания','То, ради чего ты стараешься']",
    "['wishes','speaker','Избранное','']",
    'more-options favorite label',
)
index = replace_required(
    index,
    '<span><strong>${title}</strong><small>${desc}</small></span>',
    "<span><strong>${title}</strong>${desc?'<small>'+desc+'</small>':''}</span>",
    'hide empty menu descriptions',
)
index = index.replace(
    'Текущие записи, заметки, желания и напоминания будут заменены.',
    'Текущие записи, заметки, избранное и напоминания будут заменены.',
)
INDEX.write_text(index, encoding='utf-8')

# Favorites section: neutral wording only, no motivational copy.
wishes = WISHES.read_text(encoding='utf-8')
wishes = replace_required(wishes, "<h2>Моё желание</h2>", "<h2>Избранное</h2>", 'editor title')
wishes = wishes.replace('placeholder="Что изменится в моей жизни, когда это появится?"', 'placeholder=""')
wishes = replace_required(wishes, "$('title').textContent='Мои желания'", "$('title').textContent='Избранное'", 'page title')
wishes = replace_required(wishes, "$('subtitle').textContent='Помни, ради чего ты стараешься'", "$('subtitle').textContent=''", 'page subtitle')
wishes = replace_required(wishes, '<h2>Всё начинается с мечты</h2>', '', 'motivational heading')
wishes = replace_required(
    wishes,
    '<div class="wish-empty"><strong>Представь, что это уже твоё</strong><p>Добавь фото или короткое видео своей мечты. Подпись — по желанию.</p><button class="btn primary" data-wish-add>Добавить первое желание</button></div>',
    '<div class="wish-empty"><button class="btn primary" data-wish-add>+ Добавить</button></div>',
    'empty-state motivation',
)
wishes = wishes.replace("<small>${x.done?'ИСПОЛНЕНО':'МОЁ ЖЕЛАНИЕ'}</small>", '')
wishes = wishes.replace("${x.done?'Вернуть в желания':'✓ Исполнено'}", "${x.done?'Вернуть':'✓ Готово'}")
wishes = wishes.replace(
    "<button class=\"btn primary\" data-close=\"wish-detail\">${item.done?'У меня получилось':'Я к этому иду'}</button>",
    '<button class="btn primary" data-close="wish-detail">Закрыть</button>',
)
wishes = wishes.replace('Не удалось прочитать файл желания', 'Не удалось прочитать файл')
wishes = wishes.replace('В копии отсутствуют фото желаний', 'В копии отсутствуют файлы избранного')
WISHES.write_text(wishes, encoding='utf-8')

# Remove the now-empty subtitle without leaving vertical spacing.
css = CSS.read_text(encoding='utf-8')
rule = '\nbody[data-view="wishes"] #subtitle{display:none!important}\n'
if rule.strip() not in css:
    css += rule
CSS.write_text(css, encoding='utf-8')

print('Favorites naming and neutral copy applied')
