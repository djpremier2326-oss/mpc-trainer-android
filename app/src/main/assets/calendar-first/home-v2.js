'use strict';
/* Calendar-first v0.2: spacious grid, short headlines, custom shift cycles, daily notes/icons. */
const CFV2_DEFAULT_COLORS={day:'#28c6e5',night:'#b49bff',off:'#a8e27c'};
const CFV2_TAGS=[['🏋','Тренировка'],['💰','Деньги'],['🏥','Больница'],['📌','Важное'],['🎂','Праздник'],['🚗','Поездка']];
const CFV2_TOKENS={'Д':'day','Н':'night','В':'off'};
const cfV2OriginalDayText=cfDayText;
function cfV2Title(date){return (state.cfHeadlines?.[date]||cfV2OriginalDayText(date)||'').trim().split('\n')[0].slice(0,75)}
function cfV2ShiftCycle(value){const chars=String(value||'').toUpperCase().replace(/[\s,;.-]/g,'').split('');return chars.length>0&&chars.length<=31&&chars.every(ch=>CFV2_TOKENS[ch])?chars.map(ch=>CFV2_TOKENS[ch]):null}
cfShift=function(date){
  const plan=state.shiftPlan;
  if(!plan||!validDate(plan.start))return null;
  let cycle=plan.pattern==='custom'?cfV2ShiftCycle(plan.cycle):CF_PATTERNS[plan.pattern];
  if(!cycle||!cycle.length)return null;
  const from=Date.parse(plan.start+'T00:00:00Z'),to=Date.parse(date+'T00:00:00Z');
  const offset=Math.round((to-from)/86400000),kind=cycle[((offset%cycle.length)+cycle.length)%cycle.length];
  return Object.assign({kind:kind},CF_SHIFT_INFO[kind]);
};
function cfV2HexColor(hex){return /^#[0-9a-f]{6}$/i.test(hex||'')?hex:null}
function cfV2Ink(hex){const rgb=[1,3,5].map(i=>parseInt(hex.slice(i,i+2),16)/255);
  const l=rgb.map(v=>v<=0.04045?v/12.92:Math.pow((v+0.055)/1.055,2.4));
  return (l[0]*0.2126+l[1]*0.7152+l[2]*0.0722)>0.33?'#09213a':'#ffffff';
}
function cfV2Palette(date,shift){
  const manual=dayColor(date);
  if(manual!=='none'){
    if(state.prefs.design==='ice'){
      const c=iceColor(manual);
      return {kind:'manual',hex:c[2],ink:c[3]};
    }
    const t=DAY_TINTS[manual],c=activeColors()[manual];
    const hex=cfV2HexColor(t?.[0])||cfV2HexColor(c?.[1])||'#a4ccef';
    return {kind:'manual',hex:hex,ink:cfV2Ink(hex)};
  }
  if(shift){
    const hex=cfV2HexColor(state.shiftPlan?.colors?.[shift.kind])||CFV2_DEFAULT_COLORS[shift.kind];
    return {kind:shift.kind,hex:hex,ink:cfV2Ink(hex)};
  }
  return null;
}
function cfV2Icons(date){
  const chosen=Array.isArray(state.cfBadges?.[date])?state.cfBadges[date].filter(x=>CFV2_TAGS.some(t=>t[0]===x)):[];
  const actual=[cfWorkouts(date).length?'🏋':'',cfDeposits(date).length?'💰':'',cfPhotoRows(date).length?'📷':''].filter(Boolean);
  return [...new Set([...chosen,...actual])];
}
function cfV2MonthMarkup(month){
  const first=month+'-01',start=monday(first),date=dateObj(first);
  const count=new Date(date.getFullYear(),date.getMonth()+1,0).getDate();
  const length=Math.ceil((((date.getDay()+6)%7)+count)/7)*7;
  const names=['Пн','Вт','Ср','Чт','Пт','Сб','Вс'];
  return '<section class="calendar calendar-month cf-v2-month" data-month="'+month+'" id="cal-'+month+'"><h2>'+esc(fmt(first,{month:'long',year:'numeric'}))+'</h2><div class="cal-grid">'+
    names.map((n,i)=>'<div class="weekday'+(i>=5?' cf-weekend':'')+'">'+n+'</div>').join('')+
    Array.from({length},(_,i)=>{
      const d=addDays(start,i),shift=cfShift(d),color=cfV2Palette(d,shift),label=cfV2Title(d),icons=cfV2Icons(d),outside=d.slice(0,7)!==month;
      const vars=color?'--day-tint:'+color.hex+';--day-edge:'+color.hex+';--day-ink:'+color.ink+';':'';
      const info=[fmt(d,{day:'numeric',month:'long',year:'numeric'}),shift?.label,label,...icons].filter(Boolean).join(', ');
      return '<button type="button" class="cal-cell cf-v2-cell'+(outside?' outside':'')+(d===iso()?' today':'')+(d===selected?' chosen':'')+
        '" data-date="'+d+'" data-day-color="'+(color?'cf-v2':'none')+'" data-cf-filled="'+(color?color.kind:'none')+
        '" style="'+esc(vars)+'" aria-label="'+esc(info)+'" aria-pressed="'+String(d===selected)+'">'+
        '<span class="cf-v2-number">'+Number(d.slice(-2))+'</span>'+
        (shift?'<small class="cf-shift-tag">'+esc(shift.short)+'</small>':'')+
        (label?'<small class="cf-day-title">'+esc(label)+'</small>':'')+
        (icons.length?'<span class="cf-badges" aria-hidden="true">'+icons.slice(0,3).join('')+'</span>':'')+
        '</button>';
    }).join('')+'</div></section>';
}
calendarMonthMarkup=cfV2MonthMarkup;
renderCalendar=function(){
  cfOriginalRenderCalendar();
  const bar=document.querySelector('.calendar-jump');
  if(bar){
    const month=selected.slice(0,7);
    bar.innerHTML='<input type="month" id="calendar-month" value="'+month+'" hidden aria-label="Месяц">'+
      '<button type="button" class="cf-v2-topbtn" data-cf-v2-jump>▦ Месяц</button>'+
      '<button type="button" class="cf-v2-topbtn cf-v2-shiftbtn" data-cf-shifts>⚙ График смен</button>'+
      '<button type="button" class="cf-v2-topbtn cf-v2-gear" data-cf-v2-settings aria-label="Настройки приложения">⋮</button>';
  }
  document.querySelector('.calendar-hint')?.remove();
  document.querySelector('.cf-shift-legend')?.remove();
  alignCalendarMonth(selected.slice(0,7));
};
function cfV2RefreshCalendar(){
  if(tab!=='calendar')return;
  const view=$('view'),pos=view.scrollTop;
  [...view.querySelectorAll('.calendar-month[data-month]')].forEach(el=>{
    const tmp=document.createElement('div');tmp.innerHTML=cfV2MonthMarkup(el.dataset.month);
    el.replaceWith(tmp.firstElementChild);
  });
  view.scrollTop=pos;
}
tapCalendarDate=function(date){
  if(!validDate(date))return;
  calendarTap=null;selected=date;$('date').value=date;
  document.querySelectorAll('.cal-cell').forEach(el=>{
    const yes=el.dataset.date===date;el.classList.toggle('chosen',yes);el.setAttribute('aria-pressed',String(yes));
  });
  openCalendarPreview(date);
};
function cfV2OpenPreview(date){
  cfReleaseUrls();cfPhotoToken++;
  let dialog=$('calendar-preview');
  if(!dialog){dialog=document.createElement('dialog');dialog.id='calendar-preview';dialog.setAttribute('aria-label','Дела на день');document.body.append(dialog);dialog.addEventListener('close',cfReleaseUrls)}
  const lines=slots(date).filter(r=>r.text.trim()).map(r=>({time:r.time||'',text:r.text}));
  for(const ev of dayEvents(date))if(ev.slot<0)lines.push({time:ev.time||'',text:ev.title||''});
  lines.sort((a,b)=>a.time.localeCompare(b.time));
  const shift=cfShift(date),work=cfWorkouts(date),paid=cfDeposits(date),photos=cfPhotoRows(date);
  const tags=state.cfBadges?.[date]||[],headline=state.cfHeadlines?.[date]||'',details=state.cfDetails?.[date]||'';
  dialog.innerHTML=
    '<div class="dialog-head"><h2>'+esc(fmt(date,{weekday:'long',day:'numeric',month:'long',year:'numeric'}))+'</h2><button data-close="calendar-preview" aria-label="Закрыть">×</button></div>'+
    (shift?'<p class="cf-v2-shiftline">📅 '+esc(shift.label)+'</p>':'')+
    '<label class="cf-v2-field">Главное на день <input id="cf-v2-headline" maxlength="75" value="'+esc(headline)+'" placeholder="Например, больница" autocomplete="off"></label>'+
    '<label class="cf-v2-field">Подробности <textarea id="cf-v2-details" rows="3" maxlength="10000" placeholder="Время, адрес, что взять с собой…">'+esc(details)+'</textarea></label>'+
    '<p class="cf-v2-tag-title">Значки на календаре</p><div class="cf-v2-tags">'+CFV2_TAGS.map(([icon,title])=>'<button type="button" class="cf-v2-tag" data-cf-v2-tag="'+date+'" data-cf-v2-emoji="'+icon+'" aria-pressed="'+String(tags.includes(icon))+'" title="'+esc(title)+'"><span>'+icon+'</span><small>'+esc(title)+'</small></button>').join('')+'</div>'+
    '<button class="btn primary cf-v2-save" data-cf-v2-save="'+date+'">✓ Сохранить день</button>'+
    (lines.length?'<h3 class="cf-v2-list-title">Записи из расписания</h3><div class="calendar-preview-list">'+lines.map(x=>'<div class="calendar-preview-row"><time>'+esc(x.time)+'</time><p>'+esc(x.text)+'</p></div>').join('')+'</div>':'')+
    (work.length||paid.length?'<div class="cf-preview-meta">'+(work.length?'🏋 '+work.length+' тренировок ':'')+(paid.length?'💰 '+rub(paid.reduce((s,e)=>s+e.amount,0)):'')+'</div>':'')+
    '<div id="cf-photo-list">'+(photos.length?'<p class="dialog-note">Фото этого дня</p><div class="cf-photo-grid" id="cf-photo-grid"></div>':'')+'</div>'+
    '<div class="cf-preview-actions"><button class="btn cf-open-full" data-cf-open="'+date+'">Открыть подробное расписание</button>'+
    '<button class="btn" data-cf-color="'+date+'">🎨 Цвет дня</button><button class="btn" data-cf-photo="'+date+'">📷 Добавить фото</button>'+
    '<button class="btn" data-cf-event="'+date+'">🔔 Напоминание</button><button class="btn" data-cf-workout="'+date+'">🏋 Тренировки</button>'+
    '<button class="btn" data-cf-savings="'+date+'">💰 Накопления</button></div>';
  cfRenderPhotos(date,cfPhotoToken);
  if(!dialog.open)dialog.show();
}
openCalendarPreview=cfV2OpenPreview;
function cfV2SaveDay(date){
  if(!validDate(date))return;
  const title=$('cf-v2-headline').value.trim().slice(0,75),detail=$('cf-v2-details').value.trim().slice(0,10000);
  const beforeTitle=state.cfHeadlines?.[date],beforeDetail=state.cfDetails?.[date];
  state.cfHeadlines=state.cfHeadlines||{};state.cfDetails=state.cfDetails||{};
  if(title)state.cfHeadlines[date]=title;else delete state.cfHeadlines[date];
  if(detail)state.cfDetails[date]=detail;else delete state.cfDetails[date];
  if(!persist()){
    if(beforeTitle===undefined)delete state.cfHeadlines[date];else state.cfHeadlines[date]=beforeTitle;
    if(beforeDetail===undefined)delete state.cfDetails[date];else state.cfDetails[date]=beforeDetail;
    return;
  }
  cfV2RefreshCalendar();toast('Запись сохранена',950);
}
function cfV2ToggleTag(date,icon,button){
  if(!validDate(date)||!CFV2_TAGS.some(t=>t[0]===icon))return;
  state.cfBadges=state.cfBadges||{};
  const prev=state.cfBadges[date]||[],next=prev.includes(icon)?prev.filter(x=>x!==icon):[...prev,icon];
  if(next.length)state.cfBadges[date]=next;else delete state.cfBadges[date];
  if(!persist()){if(prev.length)state.cfBadges[date]=prev;else delete state.cfBadges[date];return}
  button.setAttribute('aria-pressed',String(next.includes(icon)));cfV2RefreshCalendar();
}
function cfV2SetupSettings(){
  const form=$('cf-shift-form');
  form.elements.pattern.insertAdjacentHTML('beforeend','<option value="custom">Своя схема</option>');
  const actions=form.querySelector('.dialog-actions');
  actions.insertAdjacentHTML('beforebegin',
    '<label class="field" id="cf-v2-cycle-label" hidden>Своя схема (Д = день, Н = ночь, В = выходной)<input name="cycle" maxlength="64" placeholder="ДДВВ" autocomplete="off"><small>Например: ДДВВ — два рабочих, два выходных. НВВВ — сутки через трое.</small></label>'+
    '<p class="cf-v2-color-head">Цвета смен</p><div class="cf-v2-colors">'+
    '<label>Дневная <input type="color" name="dayColor"></label>'+
    '<label>Ночная <input type="color" name="nightColor"></label>'+
    '<label>Выходной <input type="color" name="offColor"></label></div>');
  form.elements.pattern.addEventListener('change',()=>{$('cf-v2-cycle-label').hidden=form.elements.pattern.value!=='custom'});
  form.onsubmit=e=>{
    e.preventDefault();
    const f=e.target,start=f.elements.start.value,pattern=f.elements.pattern.value;
    if(!validDate(start)||(!Object.hasOwn(CF_PATTERNS,pattern)&&pattern!=='custom'))return;
    const cycle=pattern==='custom'?cfV2ShiftCycle(f.elements.cycle.value):null;
    if(pattern==='custom'&&!cycle){toast('Укажи схему буквами Д, Н, В (например ДДВВ)');return}
    const previous=state.shiftPlan,colors={
      day:f.elements.dayColor.value,night:f.elements.nightColor.value,off:f.elements.offColor.value
    };
    if(!Object.values(colors).every(cfV2HexColor))return;
    state.shiftPlan={start,pattern,cycle:pattern==='custom'?f.elements.cycle.value.toUpperCase().replace(/[\s,;.-]/g,''):'',colors};
    if(!persist()){state.shiftPlan=previous;return}
    $('cf-shift-dialog').close();render();toast('График смен сохранён',1100);
  };
  cfOpenShifts=function(){
    const f=$('cf-shift-form'),plan=state.shiftPlan||{};
    f.elements.start.value=plan.start||selected;
    f.elements.pattern.value=plan.pattern||'none';
    f.elements.cycle.value=plan.cycle||'ДДВВ';
    for(const kind of ['day','night','off']){
      const name=kind==='day'?'dayColor':kind==='night'?'nightColor':'offColor';
      f.elements[name].value=cfV2HexColor(plan.colors?.[kind])||CFV2_DEFAULT_COLORS[kind];
    }
    $('cf-v2-cycle-label').hidden=f.elements.pattern.value!=='custom';
    $('cf-shift-dialog').showModal();
  };
  const settings=$('settings').querySelector('.settings-buttons');
  if(settings&&!$('cf-v2-open-settings')){
    const button=document.createElement('button');button.className='btn';button.id='cf-v2-open-settings';button.textContent='Настройки календаря и рабочих смен';
    settings.insertBefore(button,$('main-menu-open'));
  }
  const jump=document.createElement('dialog');jump.id='cf-v2-month-dialog';
  jump.innerHTML='<form id="cf-v2-month-form"><div class="dialog-head"><h2>Перейти к месяцу</h2><button type="button" data-close="cf-v2-month-dialog">×</button></div><label class="cf-v2-field">Месяц <input name="month" type="month" required></label><button type="submit" class="btn primary cf-v2-save">Перейти</button></form>';
  document.body.append(jump);
  $('cf-v2-month-form').onsubmit=e=>{
    e.preventDefault();const value=e.target.elements.month.value;
    if(!/^\d{4}-(0[1-9]|1[0-2])$/.test(value))return;
    jump.close();navigate(value+'-01','calendar');
  };
}
document.addEventListener('click',e=>{
  const b=e.target.closest('[data-cf-v2-save],[data-cf-v2-tag],[data-cf-v2-jump],[data-cf-v2-settings],#cf-v2-open-settings');
  if(!b)return;
  if(b.dataset.cfV2Save){cfV2SaveDay(b.dataset.cfV2Save);return}
  if(b.dataset.cfV2Tag){cfV2ToggleTag(b.dataset.cfV2Tag,b.dataset.cfV2Emoji,b);return}
  if(b.dataset.cfV2Jump!==undefined){
    $('cf-v2-month-form').elements.month.value=selected.slice(0,7);$('cf-v2-month-dialog').showModal();return;
  }
  if(b.dataset.cfV2Settings!==undefined){$('settings-open').click();return}
  if(b.id==='cf-v2-open-settings'){$('settings').close();cfOpenShifts()}
});
const cfV2OriginalSyncNavigation=syncDesignNavigation;
syncDesignNavigation=function(){
  cfV2OriginalSyncNavigation();
  const nav=document.querySelector('.tabs');if(!nav)return;
  if(state.prefs.design==='ice'){
    const cal=nav.querySelector('[data-tab=calendar]'),today=nav.querySelector('[data-ice-action=today]');
    if(cal&&today){
      const dayLabel=today.querySelector('span');if(dayLabel)dayLabel.textContent='День';
      nav.insertBefore(cal,nav.firstElementChild);
    }
  }else{
    const cal=nav.querySelector('[data-tab=calendar]');
    if(cal){cal.style.order='0';for(const other of nav.children){
      if(other===cal)continue;
      if(other.id==='impulse-add')other.style.order='2';
      else if(other.dataset.tab)other.style.order=other.dataset.tab===state.prefs.mainMenu[0]?'1':'3';
    }}
  }
};
cfV2SetupSettings();
const cfV2Menu=Array.isArray(state.prefs.mainMenu)?state.prefs.mainMenu.filter(x=>x!=='calendar'):[];
if(state.prefs.mainMenu?.[0]!=='calendar'){
  state.prefs.mainMenu=['calendar',...cfV2Menu].slice(0,3);
  if(state.prefs.mainMenu.length<3)state.prefs.mainMenu=['calendar','day','savings'];
  persist();
}
tab='calendar';
render();
