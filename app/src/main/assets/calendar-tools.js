/* Calendar-only controls. Schedule is part of the normal saved/exported preferences. */
(()=>{
'use strict';
document.body.dataset.inputMode='pointer';
document.addEventListener('contextmenu',e=>{if(e.target.closest('button'))e.preventDefault()});
document.addEventListener('selectstart',e=>{if(e.target.parentElement?.closest('button')||e.target.closest?.('button'))e.preventDefault()});
document.addEventListener('pointerdown',()=>{document.body.dataset.inputMode='pointer'},true);
document.addEventListener('keydown',e=>{if(['Tab','ArrowUp','ArrowDown','ArrowLeft','ArrowRight'].includes(e.key))document.body.dataset.inputMode='keyboard'},true);
const patterns={'2/2':['work','work','off','off'],'3/3':['work','work','work','off','off','off'],'1/3':['work','off','off','off'],'5/2':['work','work','work','work','work','off','off'],daynight:['day','night','off','off']};
const labels={work:'Рабочая смена',day:'Дневная смена',night:'Ночная смена',off:'Выходной'};
function config(){const c=state.prefs.calendarSchedule;if(!c||!validDate(c.start)||!Array.isArray(c.pattern)||!c.pattern.length||c.pattern.length>62||!c.pattern.every(k=>Object.hasOwn(labels,k))||!c.colors||!Object.keys(labels).every(k=>/^#[0-9a-f]{6}$/i.test(c.colors[k])))return null;return c}
function shift(d,c){const n=(Date.parse(d+'T00:00:00Z')-Date.parse(c.start+'T00:00:00Z'))/86400000;return c.pattern[((n%c.pattern.length)+c.pattern.length)%c.pattern.length]}
const previousMonth=calendarMonthMarkup;
calendarMonthMarkup=function(month){const html=previousMonth(month),c=config();if(!c)return html;const host=document.createElement('div');host.innerHTML=html;host.querySelectorAll('.cal-cell[data-date]').forEach(cell=>{const k=shift(cell.dataset.date,c),color=c.colors[k];cell.classList.add('has-work-shift');cell.style.setProperty('--shift-color',color);if(dayColor(cell.dataset.date)==='none'){cell.style.setProperty('background',color,'important');cell.style.setProperty('color',accentText(color),'important')}cell.setAttribute('aria-label',cell.getAttribute('aria-label')+', '+labels[k]);const tag=document.createElement('small');tag.className='calendar-shift-label';tag.textContent=k==='off'?'Вых.':k==='night'?'Ночь':k==='day'?'День':'Смена';cell.appendChild(tag)});return host.innerHTML};
const previousCalendar=renderCalendar;
renderCalendar=function(){previousCalendar();const jump=document.querySelector('.calendar-jump');if(jump){jump.hidden=true;jump.querySelector("button")?.remove();}document.querySelector('.calendar-hint')?.remove();let bar=$('calendar-top-tools');if(!bar){bar=document.createElement('nav');bar.id='calendar-top-tools';bar.setAttribute('aria-label','Инструменты календаря');bar.innerHTML='<button type="button" id="calendar-schedule-open">'+iceIcon('calendar')+'<span>График</span></button><button type="button" id="calendar-today">'+iceIcon('today')+'<span>Сегодня</span></button>';document.querySelector('main').prepend(bar)}requestAnimationFrame(()=>{const el=$('cal-'+selected.slice(0,7));if(el)$('view').scrollTop=Math.max(0,el.offsetTop-$('view').offsetTop)})};
function syncCalendarChrome(){
 if(tab==='day'){window.AndroidFiles?.statusBarColor?.('#000000',true);return;}
 const view=$('view'),style=getComputedStyle(view);
 const source=style.backgroundColor;
 let rgb=source.match(/\d+(?:\.\d+)?/g);
 if(!rgb||rgb.length>3&&Number(rgb[3])===0){
  const fallback=document.createElement('span');fallback.style.color='var(--app-bg)';document.body.appendChild(fallback);
  rgb=getComputedStyle(fallback).color.match(/\d+/g);fallback.remove();
 }
 const color=rgb&&rgb.length>=3?'#'+rgb.slice(0,3).map(n=>Number(n).toString(16).padStart(2,'0')).join(''):'#e9ddf7';
 document.documentElement.style.setProperty('--section-bg',color);
 document.documentElement.style.setProperty('--section-ink',style.color);
 window.AndroidFiles?.statusBarColor?.(color,accentText(color)==='#ffffff');
}
const calendarRender=render;
render=function(){calendarRender();syncCalendarChrome()};
const calendarApplyTheme=applyTheme;
applyTheme=function(){calendarApplyTheme();syncCalendarChrome()};
const dialog=document.createElement('dialog');dialog.id='calendar-schedule-dialog';dialog.setAttribute('aria-labelledby','calendar-schedule-title');dialog.innerHTML=`<form id="calendar-schedule-form"><div class="dialog-head"><h2 id="calendar-schedule-title">Рабочий график</h2><button type="button" id="calendar-schedule-close" aria-label="Закрыть">×</button></div><label class="field">Чередование смен<select name="pattern"><option value="2/2">2 рабочих / 2 выходных</option><option value="3/3">3 рабочих / 3 выходных</option><option value="1/3">Сутки / трое</option><option value="5/2">5 рабочих / 2 выходных</option><option value="daynight">День / ночь / 2 выходных</option><option value="custom">Свой график</option></select></label><div id="calendar-custom-cycle"><label class="field">Рабочих дней подряд<input name="work" type="number" min="1" max="31" value="2" required></label><label class="field">Выходных подряд<input name="off" type="number" min="1" max="31" value="2" required></label></div><label class="field">Первый день цикла<input name="start" type="date" required></label><p class="dialog-note" id="calendar-cycle-help">Укажи первый рабочий день. Цикл повторяется в обе стороны от этой даты.</p><div class="calendar-shift-colors">${Object.entries(labels).map(([k,v])=>`<label class="field" data-shift-color="${k}">${v}<input name="${k}Color" type="color"></label>`).join('')}</div><p class="dialog-note">Ручной цвет дня сохраняется. На таком дне смена показана цветной полоской и подписью.</p><p id="calendar-schedule-error" role="alert" hidden>Не удалось сохранить график. Освободи место на устройстве и попробуй снова.</p><button type="submit" class="btn calendar-save">Сохранить график</button><button type="button" id="calendar-schedule-remove" class="btn danger">Убрать график</button></form>`;document.body.appendChild(dialog);
const form=$('calendar-schedule-form');
function fields(){const mode=form.elements.pattern.value;$('calendar-custom-cycle').hidden=mode!=='custom';for(const k of Object.keys(labels))dialog.querySelector(`[data-shift-color="${k}"]`).hidden=mode==='daynight'?k==='work':k==='day'||k==='night';$('calendar-cycle-help').textContent=mode==='daynight'?'Укажи первую дневную смену. Затем идут ночь и два выходных.':'Укажи первый рабочий день. Цикл повторяется в обе стороны от этой даты.'}
function open(){const c=config();form.elements.pattern.value=c?.mode||'2/2';form.elements.start.value=c?.start||selected;form.elements.work.value=c?.work||2;form.elements.off.value=c?.off||2;const colors=c?.colors||{work:'#b0f51c',day:'#63d8ef',night:'#b38be8',off:'#f3edf9'};for(const k of Object.keys(labels))form.elements[k+'Color'].value=colors[k];$('calendar-schedule-remove').hidden=!c;$('calendar-schedule-error').hidden=true;fields();dialog.showModal()}
form.elements.pattern.addEventListener('change',fields);
form.addEventListener('submit',e=>{e.preventDefault();if(!form.reportValidity())return;const mode=form.elements.pattern.value,work=Number(form.elements.work.value),off=Number(form.elements.off.value);const c={mode,start:form.elements.start.value,work,off,pattern:mode==='custom'?[...Array(work).fill('work'),...Array(off).fill('off')]:patterns[mode],colors:Object.fromEntries(Object.keys(labels).map(k=>[k,form.elements[k+'Color'].value]))};const old=state.prefs.calendarSchedule;state.prefs.calendarSchedule=c;if(!persist()){state.prefs.calendarSchedule=old;$('calendar-schedule-error').hidden=false;return}dialog.close();render();toast('График сохранён')});
$('calendar-schedule-remove').addEventListener('click',()=>{const old=state.prefs.calendarSchedule;delete state.prefs.calendarSchedule;if(!persist()){state.prefs.calendarSchedule=old;$('calendar-schedule-error').hidden=false;return}dialog.close();render();toast('График убран')});
$('calendar-schedule-close').addEventListener('click',()=>dialog.close());
document.addEventListener('click',e=>{if(e.target.closest('#calendar-schedule-open'))open()});
// Recognize deliberate double taps on empty timeline space, not scroll gestures.
let timelineDown=null,lastTimelineTap=null;
document.addEventListener('pointerdown',e=>{
 const timeline=e.target.closest('.violet-event-timeline');
 if(tab!=='day'||!timeline||e.target.closest('button,input,textarea,[contenteditable],.violet-event-wrap')||!e.isPrimary){timelineDown=null;lastTimelineTap=null;return;}
 timelineDown={id:e.pointerId,x:e.clientX,y:e.clientY,t:performance.now(),timeline,date:selected};
},{passive:true});
document.addEventListener('pointermove',e=>{if(timelineDown&&timelineDown.id===e.pointerId&&Math.hypot(e.clientX-timelineDown.x,e.clientY-timelineDown.y)>10){timelineDown=null;lastTimelineTap=null;}},{passive:true});
document.addEventListener('pointercancel',()=>{timelineDown=null;lastTimelineTap=null;},{passive:true});
document.addEventListener('pointerup',e=>{
 const down=timelineDown;timelineDown=null;
 if(!down||down.id!==e.pointerId||!down.timeline.isConnected||down.date!==selected||tab!=='day'||performance.now()-down.t>300||Math.hypot(e.clientX-down.x,e.clientY-down.y)>10)return;
 const now=performance.now(),last=lastTimelineTap;
 if(last&&now-last.t<350&&last.timeline===down.timeline&&Math.hypot(e.clientX-last.x,e.clientY-last.y)<24){
  lastTimelineTap=null;if(!document.body.classList.contains('violet-editor-open'))$('violet-add-task')?.click();
 }else lastTimelineTap={t:now,x:e.clientX,y:e.clientY,timeline:down.timeline};
},{passive:true});
syncCalendarChrome();
})();
