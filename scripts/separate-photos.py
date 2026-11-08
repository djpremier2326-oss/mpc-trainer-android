"""Move the existing media UI into its own section without migrating any blobs."""
from pathlib import Path
p=Path('app/src/main/assets/index.html');s=p.read_text()
s=s.replace("events:'Напоминания',wishes:'Избранное',workout:'Тренировка'}", "events:'Напоминания',wishes:'Избранное',workout:'Тренировка',photos:'Фото'}")
s=s.replace("'events','wishes','workout'].includes(x)","'events','wishes','workout','photos'].includes(x)")
s=s.replace("if(tab==='notes'){$('title')", "if(tab==='photos'){renderPhotos();return}if(tab==='notes'){$('title')")
s=s.replace("classList.toggle('notes-view',tab==='notes')", "classList.toggle('notes-view',tab==='notes'||tab==='photos')")
icon=next(line for line in Path('app/src/main/assets/photos-section.js').read_text().splitlines() if line.startswith('function photosIcon('))
s=s.replace('const MAIN_SECTIONS=',icon+'\nconst MAIN_SECTIONS=',1)
s=s.replace("function mainMenuIcon(id){", "function mainMenuIcon(id){if(id==='photos')return photosIcon();")
s=s.replace("id==='events'?uiIcon('bell'):iceIcon(iconFor(id))", "id==='photos'?photosIcon():id==='events'?uiIcon('bell'):iceIcon(iconFor(id))")
a=s.index('// Standalone photo/video folders inside Notes');b=s.index('initializeExtras();',a)
media=s[a:b]
x=media.index('const mediaBaseRenderNotes=renderNotes;');y=media.index('const mediaBaseHandleBack=',x)
media=media[:x]+media[y:]
media=media.replace('const other=mediaFolderList().filter', 'const other=mediaVisibleFolders().filter')
media=media.replace("tab==='notes'","tab==='photos'").replace("tab!=='notes'","tab!=='photos'").replace('renderNotes()', 'renderPhotos()').replace('‹ Заметки','‹ Альбомы')
s=s[:a]+media+s[b:]
assert "if(tab==='photos'){renderPhotos();return}" in s
assert "photos:'Фото'" in s
p.write_text(s)
