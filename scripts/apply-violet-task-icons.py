from pathlib import Path

INDEX = Path('app/src/main/assets/index.html')
text = INDEX.read_text(encoding='utf-8')

marker = "function violetEventMarkup(r,layout){"
if marker not in text:
    raise SystemExit('Violet task icons: event renderer not found')

helper = r'''
function violetTaskIcon(text){
 const t=(text||'').toLowerCase();
 const svg=(body)=>`<svg class="violet-task-icon-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${body}</svg>`;
 if(/подъ[её]м|проснуться|встать|будильник/.test(t)) return svg('<circle cx="12" cy="13" r="7"/><path d="M12 10v4l3 2M5 4 2.5 6.5M19 4l2.5 2.5M7 20l-1 2M17 20l1 2"/>');
 if(/сон|спать|отбой|ложиться|лечь спать/.test(t)) return svg('<path d="M20 15.5A8 8 0 0 1 8.5 4 8.5 8.5 0 1 0 20 15.5Z"/>');
 if(/баскетбол|баскет/.test(t)) return svg('<circle cx="12" cy="12" r="9"/><path d="M3.5 9.5c4.5.7 8.4 4.6 9.1 11M20.5 14.5c-4.5-.7-8.4-4.6-9.1-11M7.2 4.4c2.8 2.5 7.8 2.5 10.6 0M6.2 18.9c2.9-2.5 8.7-2.5 11.6 0"/>');
 if(/завтрак|обед|ужин|перекус|поесть|еда/.test(t)) return svg('<path d="M7 3v8M4.5 3v5.5A2.5 2.5 0 0 0 7 11a2.5 2.5 0 0 0 2.5-2.5V3M7 11v10M15 3v18M15 3c3.5 1.5 4.5 5 2.5 8H15"/>');
 if(/работа|работать|смена|офис|проект/.test(t)) return svg('<rect x="3" y="7" width="18" height="12" rx="2"/><path d="M8 7V5a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2M3 12h18M10 12v2h4v-2"/>');
 if(/тренировка|тренироваться|спортзал|\bзал\b|бег|подтяг|отжим/.test(t)) return svg('<path d="M3 9v6M6 7v10M18 7v10M21 9v6M6 12h12"/>');
 if(/музыка|бит|биты|битмейк|студия|рэп|запись|клавиш|пианино/.test(t)) return svg('<path d="M9 18V5l10-2v13M9 8l10-2M6 21a3 3 0 1 0 0-6 3 3 0 0 0 0 6Zm10-2a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z"/>');
 if(/свободное время|отдых|отдохнуть|прогулка|погулять|кино|расслаб/.test(t)) return svg('<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>');
 return '';
}
'''
text = text.replace(marker, helper + '\n' + marker, 1)

old = "function violetEventMarkup(r,layout){const m=layout.positions[r.i],black=r.color==='black',rem=linkedReminder(selected,r.i),bell=rem?.enabled?'●':'';return `"
new = "function violetEventMarkup(r,layout){const m=layout.positions[r.i],black=r.color==='black',rem=linkedReminder(selected,r.i),bell=rem?.enabled?'●':'',taskIcon=violetTaskIcon(r.text);return `"
if old not in text:
    raise SystemExit('Violet task icons: event function signature not found')
text = text.replace(old, new, 1)

old_card = "<strong>${esc(r.text)}</strong>${bell?`<i class=\"violet-reminder-mark\" aria-label=\"Уведомление включено\">${bell}</i>`:''}</button>"
new_card = "<strong>${esc(r.text)}</strong>${taskIcon?`<span class=\"violet-task-auto-icon\">${taskIcon}</span>`:''}${bell?`<i class=\"violet-reminder-mark\" aria-label=\"Уведомление включено\">${bell}</i>`:''}</button>"
if old_card not in text:
    raise SystemExit('Violet task icons: card body not found')
text = text.replace(old_card, new_card, 1)

style = r'''<style id="violet-task-auto-icons">
body[data-design=ice][data-variant=violet] .violet-event-card{padding-right:46px!important;position:absolute!important}
body[data-design=ice][data-variant=violet] .violet-task-auto-icon{position:absolute;right:12px;top:50%;transform:translateY(-50%);width:25px;height:25px;display:grid;place-items:center;color:currentColor;opacity:.9;pointer-events:none}
body[data-design=ice][data-variant=violet] .violet-task-icon-svg{width:25px;height:25px;display:block}
body[data-design=ice][data-variant=violet] .violet-event-card.is-short{padding-right:38px!important}
body[data-design=ice][data-variant=violet] .violet-event-card.is-short .violet-task-auto-icon{right:9px;width:20px;height:20px}
body[data-design=ice][data-variant=violet] .violet-event-card.is-short .violet-task-icon-svg{width:20px;height:20px}
body[data-design=ice][data-variant=violet] .violet-event-card.is-black .violet-task-auto-icon{color:#fff}
</style>'''
if '</head>' not in text:
    raise SystemExit('Violet task icons: head not found')
text = text.replace('</head>', style + '</head>', 1)

INDEX.write_text(text, encoding='utf-8')
print('Violet automatic task icons applied')
