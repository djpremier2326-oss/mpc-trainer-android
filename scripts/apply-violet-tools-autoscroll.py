from pathlib import Path

path = Path('app/src/main/assets/index.html')
text = path.read_text(encoding='utf-8')
original = text

old = '</nav><div class="ice-paper violet-event-paper">'
new = '''</nav><div class="ice-day-tools violet-day-tools"><button data-day-color-open="${selected}"><i style="background:${iceColor(dayColor(selected))[1]}"></i>Цвет дня</button><button id="copy-open" aria-label="Скопировать день" title="Скопировать день">${uiIcon('copy')}</button><button id="clear-day-open" aria-label="Очистить день" title="Очистить день"><svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M4 7h16M9 7V4h6v3M6 7l1 14h10l1-14M10 10v7m4-7v7"/></svg></button></div><div class="ice-paper violet-event-paper">'''
if old not in text:
    raise SystemExit('Violet tools/autoscroll patch failed: paper marker not found')
text = text.replace(old, new, 1)

old_scroll = "const today=selected===iso(),first=rows[0];const minute=today?(new Date().getHours()*60+new Date().getMinutes()):(first?violetClockMinutes(first.time):8*60);paper.scrollTop=Math.max(0,layout.y(minute)-120)"
new_scroll = "const today=selected===iso(),first=rows[0],now=new Date();const minute=today?(now.getHours()*60):(first?Math.floor(violetClockMinutes(first.time)/60)*60:8*60);paper.scrollTop=Math.max(0,layout.y(minute))"
if old_scroll not in text:
    raise SystemExit('Violet tools/autoscroll patch failed: scroll code not found')
text = text.replace(old_scroll, new_scroll, 1)

style = '''<style id="violet-day-tools-fix">
body[data-design=ice][data-variant=violet] .violet-day-tools{
  margin:0 20px 8px!important;
  display:grid!important;
  grid-template-columns:minmax(0,1fr) 64px 64px!important;
  gap:8px!important;
}
</style>'''
if '</head>' not in text:
    raise SystemExit('Violet tools/autoscroll patch failed: head not found')
text = text.replace('</head>', style + '</head>', 1)

if text == original:
    raise SystemExit('Violet tools/autoscroll patch made no changes')
path.write_text(text, encoding='utf-8')
print('Violet day tools restored and timeline now opens at current hour')
