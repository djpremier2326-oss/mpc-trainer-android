from pathlib import Path

INDEX = Path('app/src/main/assets/index.html')
text = INDEX.read_text(encoding='utf-8')

# 1) The Violet time editor gets a real start/end range. Other themes keep the old single-time behavior.
old_dialog = '''<dialog id="main-menu-dialog" aria-labelledby="main-menu-title"><form id="main-menu-form"><div class="dialog-head"><h2 id="main-menu-title">Главное меню</h2><button type="button" data-close="main-menu-dialog" aria-label="Закрыть">×</button></div><p class="dialog-note">Выбери три раздела для нижнего меню. Плюс остаётся третьей кнопкой. Выбор и порядок одинаковы во всех темах.</p><div id="main-menu-preview" class="main-menu-preview" aria-label="Предпросмотр меню"></div><label class="field">Первая кнопка<select name="menuFirst"></select></label><label class="field">Вторая кнопка<select name="menuSecond"></select></label><label class="field">Кнопка справа от плюса<select name="menuThird"></select></label><p id="main-menu-error" class="form-error" role="alert" hidden></p><p class="dialog-note">Скрытие кнопки не удаляет записи. На день и в настройки можно перейти через «+».</p><button class="btn" id="main-menu-reset" type="button">По умолчанию</button><div class="dialog-actions"><button class="btn" type="button" data-close="main-menu-dialog">Отмена</button><button class="btn primary" id="main-menu-save" type="submit">Сохранить</button></div></form></dialog><dialog id="ice-time-dialog" aria-labelledby="ice-time-title"><form id="ice-time-form"><div class="dialog-head"><h2 id="ice-time-title">Время задачи</h2><button type="button" data-close="ice-time-dialog" aria-label="Закрыть">×</button></div><label class="field">Точное время<input name="time" type="time" min="00:00" max="23:59" step="60" required></label><p class="dialog-note">Выбери время с 00:00 до 23:59. Запись займёт своё место в расписании.</p><p id="ice-time-error" class="form-error" role="alert" hidden></p><div class="dialog-actions"><button type="button" class="btn" data-close="ice-time-dialog">Отмена</button><button type="submit" class="btn primary">Готово</button></div></form></dialog>'''
new_dialog = '''<dialog id="main-menu-dialog" aria-labelledby="main-menu-title"><form id="main-menu-form"><div class="dialog-head"><h2 id="main-menu-title">Главное меню</h2><button type="button" data-close="main-menu-dialog" aria-label="Закрыть">×</button></div><p class="dialog-note">Выбери три раздела для нижнего меню. Плюс остаётся третьей кнопкой. Выбор и порядок одинаковы во всех темах.</p><div id="main-menu-preview" class="main-menu-preview" aria-label="Предпросмотр меню"></div><label class="field">Первая кнопка<select name="menuFirst"></select></label><label class="field">Вторая кнопка<select name="menuSecond"></select></label><label class="field">Кнопка справа от плюса<select name="menuThird"></select></label><p id="main-menu-error" class="form-error" role="alert" hidden></p><p class="dialog-note">Скрытие кнопки не удаляет записи. На день и в настройки можно перейти через «+».</p><button class="btn" id="main-menu-reset" type="button">По умолчанию</button><div class="dialog-actions"><button class="btn" type="button" data-close="main-menu-dialog">Отмена</button><button class="btn primary" id="main-menu-save" type="submit">Сохранить</button></div></form></dialog><dialog id="ice-time-dialog" aria-labelledby="ice-time-title"><form id="ice-time-form"><div class="dialog-head"><h2 id="ice-time-title">Время задачи</h2><button type="button" data-close="ice-time-dialog" aria-label="Закрыть">×</button></div><div class="ice-range-fields"><label class="field">С<input name="time" type="time" min="00:00" max="23:59" step="60" required></label><label class="field ice-end-field" hidden>До<input name="endTime" type="time" min="00:00" max="23:59" step="60"></label></div><p class="dialog-note" id="ice-time-help">Выбери время с 00:00 до 23:59. Запись займёт своё место в расписании.</p><p id="ice-time-error" class="form-error" role="alert" hidden></p><div class="dialog-actions"><button type="button" class="btn" data-close="ice-time-dialog">Отмена</button><button type="submit" class="btn primary">Готово</button></div></form></dialog>'''
if old_dialog not in text:
    raise SystemExit('Violet timeline v2: time dialog source not found')
text = text.replace(old_dialog, new_dialog, 1)

# 2) Helpers + dedicated Violet renderer. The rest of the themes still use the existing renderer.
needle = 'function renderIceDay(){const start=monday(selected);'
if needle not in text:
    raise SystemExit('Violet timeline v2: renderIceDay marker not found')

helpers = r'''
const VIOLET_TIMELINE_HOUR=62;
function violetClockMinutes(value){
 const m=/^(\d{2}):(\d{2})$/.exec(value||'');
 if(!m)return 0;
 const h=Number(m[1]),n=Number(m[2]);
 return Math.max(0,Math.min(1440,h*60+n));
}
function violetDefaultEnd(start){
 const mins=violetClockMinutes(start);
 const end=Math.min(1439,mins+60),h=Math.floor(end/60),m=end%60;
 return String(h).padStart(2,'0')+':'+String(m).padStart(2,'0');
}
function violetTaskEnd(row){
 const start=violetClockMinutes(row?.time),raw=row?.endTime;
 if(/^([01][0-9]|2[0-3]):[0-5][0-9]$/.test(raw||'')&&violetClockMinutes(raw)>start)return raw;
 return violetDefaultEnd(row?.time||'00:00');
}
function violetTimelineMetrics(row){
 const start=violetClockMinutes(row?.time),end=Math.max(start+15,violetClockMinutes(violetTaskEnd(row)));
 return {start,end,top:start/60*VIOLET_TIMELINE_HOUR,height:Math.max(34,(end-start)/60*VIOLET_TIMELINE_HOUR)};
}
function violetTimelineInline(row){
 const m=violetTimelineMetrics(row);
 return `--vt-top:${m.top.toFixed(2)}px;--vt-height:${m.height.toFixed(2)}px`;
}
function violetTimelineAxis(){
 return Array.from({length:25},(_,h)=>`<time style="--vt-h:${h}">${String(h).padStart(2,'0')}:00</time>`).join('');
}
function violetTimelineRow(r){
 const i=r.i,a=r.time,b=violetTaskEnd(r),filled=!!r.text.trim(),range=filled?`<button type="button" class="violet-event-range" data-ice-time="${i}" aria-label="Изменить время ${a}–${b}">${a} – ${b}</button>`:'';
 return `<section class="ice-time-block violet-time-block ${filled?'has-entry':''}" data-ice-slot="${i}" data-slot="${i}" data-color="${r.color}" style="${iceRowStyle(r.color)};${violetTimelineInline(r)}"><button type="button" class="ice-hours ice-time-edit" data-ice-time="${i}" aria-label="Изменить время ${a}"><time>${a}</time><span>Изменить</span></button><div class="ice-entry">${range}<label class="sr-only" for="slot-${i}">Дело на ${a}</label><textarea rows="1" id="slot-${i}" data-edit="${i}" aria-label="Дело на ${a}" maxlength="10000" placeholder="${filled?'':'Планы на это время'}" spellcheck="true" autocapitalize="sentences">${esc(r.text)}</textarea><div class="ice-card-actions"><button class="ice-entry-reminder" data-slot-reminder="${i}" aria-label="Напоминание для ${a}">${uiIcon('bell')}</button><button class="ice-color-button" data-color-open="${i}" aria-label="Цвет записи ${a}: ${iceColor(r.color)[0]}"><i></i></button></div><button type="button" class="ice-expand" data-ice-expand="${i}" aria-controls="slot-${i}" aria-expanded="false" aria-label="Развернуть задание" hidden><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="m6 9 6 6 6-6"/></svg></button><button type="button" class="ice-row-done" data-ice-done="${i}">✓ Готово</button></div><button type="button" class="ice-clear-task" data-ice-clear="${i}" aria-label="Очистить запись ${a}" hidden><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true"><path d="M4 7h16M9 7V4h6v3M6 7l1 14h10l1-14M10 10v7m4-7v7"/></svg><span>Очистить</span></button></section>`;
}
function renderVioletTimelineDay(){
 const start=monday(selected),all=slots().map((r,i)=>({...r,i}));
 const filled=all.filter(r=>r.text.trim()).map(r=>({...r,...violetTimelineMetrics(r)}));
 const covered=minute=>filled.some(r=>minute>r.start&&minute<r.end);
 const rows=all.filter(r=>r.text.trim()||!covered(violetClockMinutes(r.time))).sort((a,b)=>a.time.localeCompare(b.time)||a.i-b.i);
 $('view').innerHTML=`<section class="ice-home"><header class="ice-hero ice-tribute"><div class="ice-tribute-glow" aria-hidden="true"></div><div class="ice-tupac-photo"><img src="ice/new-york-city.jpg" alt="" decoding="async"></div><div class="ice-date-line"><span>${esc(fmt(selected,{weekday:'long',day:'numeric',month:'long'}))}${FRIEND_SHIFT?`<small class="friend-shift-title">${friendShiftText(selected)}</small>`:''}</span></div></header><nav class="ice-week" aria-label="Дни недели">${Array.from({length:7},(_,i)=>{const d=addDays(start,i),count=occupied(d)+dayEvents(d).length;return `<button data-date="${d}" class="${i>=5?'ice-sunday':''}" ${d===selected?'aria-current="date"':''} aria-label="${fmt(d,{weekday:'long',day:'numeric',month:'long'})}"><span>${fmt(d,{weekday:'short'})}</span><strong>${dateObj(d).getDate()}</strong><i class="${count?'has-plans':''}"></i></button>`}).join('')}</nav><div class="ice-edit-bar"><span><small>Редактирование</small><strong id="ice-edit-time"></strong></span><button type="button" id="ice-edit-done">Готово</button></div><div class="ice-paper"><div class="ice-day-tools"><button data-day-color-open="${selected}"><i style="background:${iceColor(dayColor(selected))[1]}"></i>Цвет дня</button><button id="copy-open" aria-label="Скопировать день">${uiIcon('copy')}</button><button id="clear-day-open" aria-label="Очистить день" title="Очистить день"><svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M4 7h16M9 7V4h6v3M6 7l1 14h10l1-14M10 10v7m4-7v7"/></svg></button></div><div class="violet-timeline"><div class="violet-time-axis">${violetTimelineAxis()}</div><div class="violet-time-track">${rows.map(violetTimelineRow).join('')}</div></div><div class="ice-day-boundary">Дальше — ${esc(fmt(addDays(selected,1),{day:'numeric',month:'long'}))}</div></div></section>`;
 refreshIceRows();
}
'''
text = text.replace(needle, helpers + '\nfunction renderIceDay(){if(state.prefs.variant===\'violet\')return renderVioletTimelineDay();const start=monday(selected);', 1)

# 3) In Violet the helper range uses the stored end time; other themes keep one-hour rows.
old_range = "function iceTimeRange(i){return slotLabel(i).split('–')}"
new_range = "function iceTimeRange(i){const r=slots()[i];return state.prefs.variant==='violet'?[r.time,violetTaskEnd(r)]:slotLabel(i).split('–')}"
if old_range not in text:
    raise SystemExit('Violet timeline v2: iceTimeRange marker not found')
text = text.replace(old_range, new_range, 1)

# 4) refreshIceRows currently replaces row.style.cssText; preserve the timeline positioning variables.
old_style = "row.dataset.color=color;row.style.cssText=iceRowStyle(color);"
new_style = "row.dataset.color=color;row.style.cssText=iceRowStyle(color)+(state.prefs.variant==='violet'?';'+violetTimelineInline(daySlots[i]):'');"
if old_style not in text:
    raise SystemExit('Violet timeline v2: row style marker not found')
text = text.replace(old_style, new_style, 1)

# 5) Open the range editor. Violet shows both start and end; other themes show only start.
old_open = "document.addEventListener('click',e=>{const b=e.target.closest('[data-ice-time]');if(!b||state.prefs.design!=='ice')return;iceTimeIndex=Number(b.dataset.iceTime);iceTimeDate=selected;$('ice-time-form').elements.time.value=slots()[iceTimeIndex].time;$('ice-time-error').hidden=true;$('ice-time-dialog').showModal()});"
new_open = "document.addEventListener('click',e=>{const b=e.target.closest('[data-ice-time]');if(!b||state.prefs.design!=='ice')return;iceTimeIndex=Number(b.dataset.iceTime);iceTimeDate=selected;const form=$('ice-time-form'),row=slots()[iceTimeIndex],violet=state.prefs.variant==='violet';form.elements.time.value=row.time;form.elements.endTime.value=violetTaskEnd(row);form.querySelector('.ice-end-field').hidden=!violet;$('ice-time-help').textContent=violet?'Укажи начало и конец. Блок займёт этот промежуток на шкале времени.':'Выбери время с 00:00 до 23:59. Запись займёт своё место в расписании.';$('ice-time-error').hidden=true;$('ice-time-dialog').showModal()});"
if old_open not in text:
    raise SystemExit('Violet timeline v2: time dialog open handler not found')
text = text.replace(old_open, new_open, 1)

# 6) Save start/end for Violet and keep reminder time tied to the start.
old_submit = "$('ice-time-form').addEventListener('submit',e=>{e.preventDefault();const time=e.currentTarget.elements.time.value;if(!/^([01][0-9]|2[0-3]):[0-5][0-9]$/.test(time)||!validDate(iceTimeDate)||iceTimeIndex<0||iceTimeIndex>=24){$('ice-time-error').textContent='Укажи время с 00:00 до 23:59.';$('ice-time-error').hidden=false;return}const rows=ensureHourlyDay(iceTimeDate),old=rows[iceTimeIndex].time,changed=[];rows[iceTimeIndex].time=time;state.reminders.filter(r=>r.date===iceTimeDate&&r.slot>=0&&reminderIndex(r,iceTimeDate)===iceTimeIndex).forEach(r=>{changed.push([r,r.time]);r.time=time});if(!persist()){rows[iceTimeIndex].time=old;changed.forEach(([r,value])=>r.time=value);return}$('ice-time-dialog').close();render();const row=document.querySelector('[data-ice-slot=\"'+iceTimeIndex+'\"]');row?.scrollIntoView({block:'nearest'});toast('Время сохранено')});"
new_submit = "$('ice-time-form').addEventListener('submit',e=>{e.preventDefault();const form=e.currentTarget,time=form.elements.time.value,endTime=form.elements.endTime.value,violet=state.prefs.variant==='violet';if(!/^([01][0-9]|2[0-3]):[0-5][0-9]$/.test(time)||!validDate(iceTimeDate)||iceTimeIndex<0||iceTimeIndex>=24){$('ice-time-error').textContent='Укажи время с 00:00 до 23:59.';$('ice-time-error').hidden=false;return}if(violet&&(!/^([01][0-9]|2[0-3]):[0-5][0-9]$/.test(endTime)||violetClockMinutes(endTime)<=violetClockMinutes(time))){$('ice-time-error').textContent='Время «До» должно быть позже времени «С».';$('ice-time-error').hidden=false;return}const rows=ensureHourlyDay(iceTimeDate),oldTime=rows[iceTimeIndex].time,oldEnd=rows[iceTimeIndex].endTime,changed=[];rows[iceTimeIndex].time=time;if(violet)rows[iceTimeIndex].endTime=endTime;state.reminders.filter(r=>r.date===iceTimeDate&&r.slot>=0&&reminderIndex(r,iceTimeDate)===iceTimeIndex).forEach(r=>{changed.push([r,r.time]);r.time=time});if(!persist()){rows[iceTimeIndex].time=oldTime;if(oldEnd===undefined)delete rows[iceTimeIndex].endTime;else rows[iceTimeIndex].endTime=oldEnd;changed.forEach(([r,value])=>r.time=value);return}$('ice-time-dialog').close();render();const row=document.querySelector('[data-ice-slot=\"'+iceTimeIndex+'\"]');row?.scrollIntoView({block:'center'});toast(violet?'Диапазон времени сохранён':'Время сохранено')});"
if old_submit not in text:
    raise SystemExit('Violet timeline v2: time submit handler not found')
text = text.replace(old_submit, new_submit, 1)

INDEX.write_text(text, encoding='utf-8')
print('Violet ranged timeline applied')
