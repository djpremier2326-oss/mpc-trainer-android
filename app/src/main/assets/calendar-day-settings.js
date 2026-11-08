/* Independent calendar labels/icons; no schedule entries or day colors are replaced. */
(()=>{
'use strict';
const shapes={
 work:['Работа','<rect x="3" y="7" width="18" height="14" rx="2"/><path d="M8 7V3h8v4M3 12h18M10 12v3h4v-3"/>'],
 dumbbell:['Тренировка','<path d="M3 7v10M6 5v14M18 5v14M21 7v10M6 12h12"/>'],
 person:['Человечек','<circle cx="12" cy="4" r="2"/><path d="M12 8v7m0-6-5 4m5-4 5 4m-5 2-4 7m4-7 4 7"/>'],
 hospital:['Больница','<path d="M9 3h6v6h6v6h-6v6H9v-6H3V9h6z"/>'],
 home:['Дом','<path d="m3 11 9-8 9 8M5 10v11h14V10M10 21v-7h4v7"/>'],
 book:['Учёба','<path d="M12 5v16M12 5C8 2 4 3 2 4v15c3-1 6-1 10 2 4-3 7-3 10-2V4c-3-1-6-2-10 1"/>'],
 shopping:['Покупки','<path d="M5 7h14l2 14H3L5 7M8 8V6a4 4 0 0 1 8 0v2"/>'],
 plane:['Поездка','<path d="m22 2-7 20-4-9-9-4L22 2M11 13 22 2"/>'],
 car:['Машина','<path d="m5 6-3 8v6h3v-3h14v3h3v-6l-3-8H5M3 12h18M6 14h1m10 0h1"/>'],
 cake:['Праздник','<path d="M4 12h16v9H4zM4 16c3 3 5-3 8 0s5-3 8 0M8 12V8m4 4V8m4 4V8M8 5V3m4 2V3m4 2V3"/>'],
 heart:['Здоровье','<path d="M12 21 3 12C-3 5 7-2 12 6c5-8 15-1 9 6l-9 9z"/>']
};
const svg=id=>shapes[id]?'<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">'+shapes[id][1]+'</svg>':'';
function note(date){
 const v=state.prefs.calendarDayNotes?.[date];
 if(!v||typeof v.text!=='string'||v.text.length>60)return null;
 const icons=Array.isArray(v.icons)?v.icons:(v.icon?[v.icon]:[]);
 if(icons.length>Object.keys(shapes).length||!icons.every(id=>Object.hasOwn(shapes,id)))return null;
 return {text:v.text,icons:[...new Set(icons)]};
}
function decorate(cell){
 cell.querySelector('.calendar-day-note')?.remove();cell.removeAttribute('aria-description');
 const v=note(cell.dataset.date);if(!v||!v.text&&!v.icons.length)return;
 const el=document.createElement('span');el.className='calendar-day-note';
 if(v.icons.length){const icons=document.createElement('span');icons.className='calendar-day-icons';icons.dataset.count=String(v.icons.length);for(const id of v.icons){const icon=document.createElement('span');icon.className='calendar-day-icon';icon.innerHTML=svg(id);icons.append(icon)}el.append(icons)}
 if(v.text){const text=document.createElement('span');text.className='calendar-day-text';text.textContent=v.text;el.append(text)}
 cell.append(el);cell.setAttribute('aria-description',[v.text,...v.icons.map(id=>shapes[id][0])].filter(Boolean).join(', '));
}
function contrast(){document.querySelectorAll('.calendar-day-icon').forEach(icon=>{const rgb=getComputedStyle(icon.closest('.cal-cell')).color.match(/\d+/g);if(rgb)icon.style.color=rgb.slice(0,3).map(Number).reduce((a,b)=>a+b,0)>382?'#fff':'#000'})}
const originalMonth=calendarMonthMarkup;
calendarMonthMarkup=function(month){const host=document.createElement('div');host.innerHTML=originalMonth(month);host.querySelectorAll('.cal-cell[data-date]').forEach(decorate);return host.innerHTML};
const originalCalendar=renderCalendar;renderCalendar=function(){originalCalendar();contrast()};
$('view').addEventListener('scroll',()=>requestAnimationFrame(contrast),{passive:true});
const dialog=document.createElement('dialog');dialog.id='calendar-day-settings';dialog.setAttribute('aria-labelledby','calendar-day-settings-title');
dialog.innerHTML='<form id="calendar-day-settings-form"><div class="dialog-head"><h2 id="calendar-day-settings-title">День в календаре</h2><button type="button" id="calendar-day-settings-close" aria-label="Закрыть">×</button></div><p id="calendar-day-settings-date" class="dialog-note"></p><label class="field">Короткая запись<input name="text" maxlength="60" placeholder="Например, Больница" autocomplete="off"></label><p class="dialog-note">Цвет дня</p><div id="calendar-day-color-choices" class="calendar-day-color-choices" role="group" aria-label="Цвет дня"></div><p class="dialog-note">Значки</p><div class="calendar-icon-choices" role="group" aria-label="Значки дня"><button type="button" data-calendar-icon="" aria-label="Убрать все значки">Нет</button>'+Object.entries(shapes).map(([id,v])=>'<button type="button" data-calendar-icon="'+id+'" aria-label="'+v[0]+'">'+svg(id)+'<small>'+v[0]+'</small></button>').join('')+'</div><p class="dialog-note">Можно выбрать несколько значков. Повторное нажатие убирает значок. Текст необязателен. Двойной тап по дню открывает расписание.</p><p id="calendar-day-settings-error" role="alert" hidden>Не удалось сохранить. Освободи место на устройстве и попробуй ещё раз.</p><div class="dialog-actions"><button type="button" id="calendar-day-settings-remove" class="btn">Убрать подпись и значки</button><button type="submit" class="btn primary">Сохранить</button></div></form>';
document.body.append(dialog);let editingDate=null,iconChoice=[],colorChoice='none';
function syncIconChoice(){dialog.querySelectorAll('[data-calendar-icon]').forEach(b=>b.setAttribute('aria-pressed',String(b.dataset.calendarIcon?iconChoice.includes(b.dataset.calendarIcon):iconChoice.length===0)))}
function choose(id){if(!id)iconChoice=[];else if(iconChoice.includes(id))iconChoice=iconChoice.filter(value=>value!==id);else iconChoice.push(id);syncIconChoice()}
function chooseColor(key){colorChoice=key;dialog.querySelectorAll('[data-calendar-day-color]').forEach(b=>b.setAttribute('aria-pressed',String(b.dataset.calendarDayColor===key)))}
function open(date){if(!validDate(date))return;editingDate=date;dialog.dataset.date=date;calendarTap=null;$('calendar-preview')?.close();$('calendar-day-settings-date').textContent=fmt(date,{weekday:'long',day:'numeric',month:'long',year:'numeric'});dialog.querySelector('[name=text]').value=note(date)?.text||'';iconChoice=[...(note(date)?.icons||[])];syncIconChoice();$('calendar-day-color-choices').innerHTML=Object.entries(activeColors()).map(([key,[label,color]])=>'<button type="button" data-calendar-day-color="'+key+'"><i style="background:'+color+'"></i><span>'+esc(label)+'</span></button>').join('');chooseColor(dayColor(date));$('calendar-day-settings-error').hidden=true;dialog.showModal()}
window.openCalendarDaySettings=open;
function save(remove=false){const notes={...(state.prefs.calendarDayNotes||{})},old=state.prefs.calendarDayNotes,text=dialog.querySelector('[name=text]').value.trim();if(remove||!text&&!iconChoice.length)delete notes[editingDate];else notes[editingDate]={text,icons:[...iconChoice]};state.prefs.calendarDayNotes=notes;if(!(remove?persist():setDayColor(editingDate,colorChoice))){state.prefs.calendarDayNotes=old;$('calendar-day-settings-error').hidden=false;return}const view=$('view'),top=view.scrollTop;document.querySelectorAll('#calendar-stream .calendar-month').forEach(month=>{month.outerHTML=calendarMonthMarkup(month.dataset.month)});view.scrollTop=top;contrast();dialog.close()}
dialog.addEventListener('click',e=>{const b=e.target.closest('[data-calendar-icon]');if(b)choose(b.dataset.calendarIcon);const color=e.target.closest('[data-calendar-day-color]');if(color)chooseColor(color.dataset.calendarDayColor)});
$('calendar-day-settings-form').addEventListener('submit',e=>{e.preventDefault();save()});$('calendar-day-settings-remove').addEventListener('click',()=>save(true));$('calendar-day-settings-close').addEventListener('click',()=>dialog.close());
let hold=null,suppress=null;
function cancel(){if(hold)clearTimeout(hold.timer);hold=null}
document.addEventListener('pointerdown',e=>{
 cancel();suppress=null;const cell=e.target.closest('.cal-cell[data-date]');if(tab!=='calendar'||!cell||!e.isPrimary||e.button!==0)return;
 const gesture={id:e.pointerId,x:e.clientX,y:e.clientY,date:cell.dataset.date,cell};hold=gesture;
 gesture.timer=setTimeout(()=>{if(hold!==gesture||tab!=='calendar'||!cell.isConnected)return;suppress={date:gesture.date,until:performance.now()+1500};open(gesture.date);},550);
},{passive:true});
document.addEventListener('pointermove',e=>{if(hold&&e.pointerId===hold.id&&Math.hypot(e.clientX-hold.x,e.clientY-hold.y)>10)cancel()},{passive:true});
document.addEventListener('pointerup',cancel,{passive:true});document.addEventListener('pointercancel',cancel,{passive:true});$('view').addEventListener('scroll',cancel,{passive:true});
document.addEventListener('click',e=>{if(suppress&&performance.now()<suppress.until&&e.detail!==0){e.preventDefault();e.stopImmediatePropagation();suppress=null;}},true);
document.addEventListener('keydown',e=>{const c=e.target.closest('.cal-cell[data-date]');if(c&&(e.key==='F2'||e.shiftKey&&e.key==='F10')){e.preventDefault();open(c.dataset.date)}});
})();
