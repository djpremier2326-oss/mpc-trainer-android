'use strict';
/* Calendar-first edition: original notes, timer, reminders and savings remain untouched. */
const CF_PATTERNS={
  none:[],
  twoTwo:['day','day','off','off'],
  dayNight:['day','night','off','off'],
  fiveTwo:['day','day','day','day','day','off','off'],
  oneThree:['night','off','off','off']
};
const CF_SHIFT_INFO={
  day:{label:'Дневная смена',short:'День',color:'cyan'},
  night:{label:'Ночная смена',short:'Ночь',color:'purple'},
  off:{label:'Выходной',short:'Вых.',color:'green'}
};
let cfPhotoDate='',cfPhotoToken=0,cfPhotoUrls=[];
function cfShift(date){
  const plan=state.shiftPlan;
  if(!plan||!validDate(plan.start)||!Object.hasOwn(CF_PATTERNS,plan.pattern))return null;
  const pattern=CF_PATTERNS[plan.pattern];if(!pattern.length)return null;
  const offset=Math.round((Date.parse(date+'T00:00:00Z')-Date.parse(plan.start+'T00:00:00Z'))/86400000);
  return CF_SHIFT_INFO[pattern[((offset%pattern.length)+pattern.length)%pattern.length]];
}
function cfWorkouts(date){return (state.workout?.history||[]).filter(h=>Number.isFinite(h.at)&&iso(new Date(h.at))===date)}
function cfDeposits(date){return (state.savings?.entries||[]).filter(e=>e.paid&&e.paidOn===date)}
function cfPhotoRows(date){const rows=state.calendarPhotos?.[date];return Array.isArray(rows)?rows:[]}
function cfDayText(date){
  const important=dayEvents(date).filter(e=>e.enabled!==false).map(e=>e.title?.trim()).filter(Boolean);
  const tasks=slots(date).map(s=>s.text.trim()).filter(Boolean);
  return (important[0]||tasks[0]||'').split('\n')[0].slice(0,75);
}
function cfEffectiveColor(date){
  const chosen=dayColor(date);
  if(chosen!=='none')return chosen;
  return cfShift(date)?.color||'none';
}
function cfColorVariables(key){if(key==='none')return '';const c=iceColor(key);return '--day-tint:'+c[2]+';--day-edge:'+c[1]+';--day-ink:'+c[3]}
function cfCalendarMonthMarkup(month){
  const first=month+'-01',start=monday(first),d=dateObj(first);
  const count=new Date(d.getFullYear(),d.getMonth()+1,0).getDate();
  const length=Math.ceil((((d.getDay()+6)%7)+count)/7)*7;
  return '<section class="calendar calendar-month" data-month="'+month+'" id="cal-'+month+'"><h2>'+esc(fmt(first,{month:'long',year:'numeric'}))+'</h2><div class="cal-grid">'+
  ['Пн','Вт','Ср','Чт','Пт','Сб','Вс'].map(x=>'<div class="weekday">'+x+'</div>').join('')+
  Array.from({length},(_,i)=>{
    const date=addDays(start,i),shift=cfShift(date),color=cfEffectiveColor(date),label=cfDayText(date);
    const badges=(cfWorkouts(date).length?'🏋':'')+(cfDeposits(date).length?'💰':'')+(cfPhotoRows(date).length?'📷':'');
    const accessible=[fmt(date,{day:'numeric',month:'long',year:'numeric'}),shift?.label,label,cfWorkouts(date).length?'тренировка':'',cfDeposits(date).length?'взнос':'',cfPhotoRows(date).length?'фото':''].filter(Boolean).join(', ');
    return '<button class="cal-cell '+(date.slice(0,7)!==month?'outside ':'')+(date===iso()?'today ':'')+(date===selected?'chosen':'')+
      '" data-date="'+date+'" data-day-color="'+color+'" style="'+esc(cfColorVariables(color))+'" aria-label="'+esc(accessible)+'">'+
      '<span>'+Number(date.slice(-2))+'</span>'+(shift?'<small class="cf-shift-tag">'+esc(shift.short)+'</small>':'')+
      (label?'<small class="cf-day-title">'+esc(label)+'</small>':'')+
      (badges?'<span class="cf-badges" aria-hidden="true">'+badges+'</span>':'')+'</button>';
  }).join('')+'</div></section>';
}
calendarMonthMarkup=cfCalendarMonthMarkup;
const cfOriginalRenderCalendar=renderCalendar;
renderCalendar=function(){
  cfOriginalRenderCalendar();
  const bar=document.querySelector('.calendar-jump');
  if(bar&&!bar.querySelector('.cf-shift-open'))bar.insertAdjacentHTML('beforeend','<button class="btn cf-shift-open" data-cf-shifts>Смены ⚙</button>');
  const stream=$('calendar-stream');
  if(stream)stream.insertAdjacentHTML('beforebegin','<div class="cf-shift-legend"><span>☀ Дневная</span><span>🌙 Ночная</span><span>✓ Выходной</span><span>🏋 Тренировка</span><span>💰 Взнос</span><span>📷 Фото</span></div>');
  const help=document.querySelector('.calendar-hint');
  if(help)help.textContent='Нажми на дату: планы, смены и фото. Открыть день можно целиком.';
};
function cfReleaseUrls(){for(const url of cfPhotoUrls)URL.revokeObjectURL(url);cfPhotoUrls=[]}
function cfPhotoKey(id){return 'calendar-day-'+id}
function cfSetup(){
  const shifts=document.createElement('dialog');shifts.id='cf-shift-dialog';
  shifts.innerHTML='<form id="cf-shift-form"><div class="dialog-head"><h2>Рабочий график</h2><button type="button" data-close="cf-shift-dialog" aria-label="Закрыть">×</button></div>'+
  '<p class="dialog-note">Выбери начало цикла. Смены автоматически окрасят дни, но твои ручные цвета имеют приоритет.</p>'+
  '<label class="field">Начало цикла<input type="date" name="start" required></label>'+
  '<label class="field">Тип графика<select name="pattern"><option value="none">Без смен</option><option value="twoTwo">2 через 2</option><option value="dayNight">День / ночь / 2 выходных</option><option value="fiveTwo">5 через 2</option><option value="oneThree">Сутки / 3 выходных</option></select></label>'+
  '<div class="dialog-actions"><button class="btn primary" type="submit">Применить</button></div></form>';
  document.body.append(shifts);
  const photo=document.createElement('input');photo.type='file';photo.accept='image/*';photo.id='cf-photo-input';photo.hidden=true;document.body.append(photo);
  photo.addEventListener('change',cfAddPhoto);
  const viewer=document.createElement('dialog');viewer.id='cf-image-dialog';viewer.innerHTML='<div class="dialog-head"><h2>Фото дня</h2><button data-close="cf-image-dialog">×</button></div><img id="cf-full-image" alt="Фото" style="display:block;width:100%;height:auto;max-height:74dvh;object-fit:contain">';document.body.append(viewer);
  $('cf-shift-form').onsubmit=e=>{
    e.preventDefault();const form=e.target,start=form.elements.start.value,pattern=form.elements.pattern.value;
    if(!validDate(start)||!Object.hasOwn(CF_PATTERNS,pattern))return;
    const previous=state.shiftPlan;state.shiftPlan={start,pattern};
    if(!persist()){state.shiftPlan=previous;return}
    shifts.close();render();toast('График смен сохранён',900);
  };
  $('calendar-preview')?.addEventListener('close',cfReleaseUrls);
}
function cfOpenPreview(date){
  cfReleaseUrls();cfPhotoToken++;
  let dialog=$('calendar-preview');
  if(!dialog){dialog=document.createElement('dialog');dialog.id='calendar-preview';document.body.append(dialog);dialog.addEventListener('close',cfReleaseUrls)}
  const lines=slots(date).filter(r=>r.text.trim()).map(r=>({time:r.time||'',text:r.text}));
  for(const ev of dayEvents(date))if(ev.slot<0)lines.push({time:ev.time||'',text:ev.title||''});
  lines.sort((a,b)=>a.time.localeCompare(b.time));
  const shift=cfShift(date),work=cfWorkouts(date),deposits=cfDeposits(date),photos=cfPhotoRows(date);
  dialog.innerHTML='<div class="dialog-head"><h2>'+esc(fmt(date,{weekday:'long',day:'numeric',month:'long',year:'numeric'}))+'</h2><button data-close="calendar-preview" aria-label="Закрыть">×</button></div>'+
  '<div class="cf-preview-meta">'+(shift?esc(shift.label)+' · ':'')+lines.length+' дел'+(work.length?' · 🏋 '+work.length+' тренировок':'')+(deposits.length?' · 💰 '+rub(deposits.reduce((s,e)=>s+e.amount,0)):'')+'</div>'+
  '<div class="calendar-preview-list">'+(lines.map(x=>'<div class="calendar-preview-row"><time>'+esc(x.time)+'</time><p>'+esc(x.text)+'</p></div>').join('')||'<p class="dialog-note">Пока записей нет</p>')+'</div>'+
  '<div id="cf-photo-list">'+(photos.length?'<p class="dialog-note">Фотографии этого дня</p><div class="cf-photo-grid" id="cf-photo-grid"></div>':'')+'</div>'+
  '<div class="cf-preview-actions"><button class="btn cf-open-full" data-cf-open="'+date+'">Открыть день целиком</button>'+
  '<button class="btn" data-cf-color="'+date+'">🎨 Цвет дня</button><button class="btn" data-cf-photo="'+date+'">📷 Добавить фото</button>'+
  '<button class="btn" data-cf-event="'+date+'">🔔 Событие</button><button class="btn" data-cf-workout="'+date+'">🏋 Тренировки</button>'+
  '<button class="btn" data-cf-savings="'+date+'">💰 Накопления</button></div>';
  cfRenderPhotos(date,cfPhotoToken);
  if(!dialog.open)dialog.show();
}
openCalendarPreview=cfOpenPreview;
async function cfRenderPhotos(date,token){
  const list=$('cf-photo-grid');if(!list)return;
  for(const row of cfPhotoRows(date)){
    try{
      const blob=await wishPhoto('get',cfPhotoKey(row.id));
      if(!blob||token!==cfPhotoToken||!list.isConnected)return;
      const url=blob instanceof Blob?URL.createObjectURL(blob):blob;
      if(blob instanceof Blob)cfPhotoUrls.push(url);
      const tile=document.createElement('div');tile.className='cf-photo-tile';
      const button=document.createElement('button');button.type='button';button.textContent='×';button.title='Удалить фото';button.dataset.cfRemovePhoto=row.id;button.dataset.cfDate=date;
      const image=document.createElement('img');image.src=url;image.alt=row.name||'Фото дня';image.dataset.cfViewPhoto=row.id;image.dataset.cfDate=date;image.style.cursor='pointer';
      tile.append(image,button);list.append(tile);
    }catch(e){toast('Не удалось прочитать фотографию')}
  }
}
async function cfAddPhoto(e){
  const file=e.target.files?.[0];e.target.value='';
  const date=cfPhotoDate;if(!file||!validDate(date))return;
  if(!file.type.startsWith('image/')||file.size>8000000){toast('Нужно фото до 8 МБ');return}
  if(cfPhotoRows(date).length>=12){toast('Не больше 12 фото в день');return}
  const id=crypto.randomUUID(),row={id,name:String(file.name||'Фото').slice(0,160)};
  try{
    await wishPhoto('put',cfPhotoKey(id),file);
    state.calendarPhotos=state.calendarPhotos||{};
    state.calendarPhotos[date]=[...cfPhotoRows(date),row];
    if(!persist())throw Error('Не удалось сохранить запись');
    if($('calendar-preview')?.open)cfOpenPreview(date);
    toast('Фото добавлено',800);
  }catch(err){
    state.calendarPhotos[date]=(state.calendarPhotos[date]||[]).filter(x=>x.id!==id);
    try{await wishPhoto('delete',cfPhotoKey(id))}catch(_){}
    toast('Не удалось сохранить фото');
  }
}
async function cfRemovePhoto(date,id){
  if(!confirm('Удалить фото с этой даты?'))return;
  const old=cfPhotoRows(date).slice();
  state.calendarPhotos[date]=old.filter(x=>x.id!==id);
  if(!persist()){state.calendarPhotos[date]=old;return}
  try{await wishPhoto('delete',cfPhotoKey(id))}catch(_){}
  if($('calendar-preview')?.open)cfOpenPreview(date);
}
function cfOpenShifts(){
  const f=$('cf-shift-form');f.elements.start.value=state.shiftPlan?.start||selected;
  f.elements.pattern.value=state.shiftPlan?.pattern||'none';$('cf-shift-dialog').showModal();
}
document.addEventListener('click',e=>{
  const b=e.target.closest('[data-cf-shifts],[data-cf-open],[data-cf-color],[data-cf-photo],[data-cf-event],[data-cf-workout],[data-cf-savings],[data-cf-remove-photo],[data-cf-view-photo]');
  if(!b)return;const d=b.dataset;
  if(d.cfShifts!==undefined){cfOpenShifts();return}
  if(d.cfOpen){navigate(d.cfOpen,'day');return}
  if(d.cfColor){$('calendar-preview')?.close();openDayColors(d.cfColor);return}
  if(d.cfPhoto){cfPhotoDate=d.cfPhoto;$('cf-photo-input').click();return}
  if(d.cfEvent){$('calendar-preview')?.close();selected=d.cfEvent;openReminderDialog?.();return}
  if(d.cfWorkout){$('calendar-preview')?.close();navigate(d.cfWorkout,'workout');return}
  if(d.cfSavings){$('calendar-preview')?.close();navigate(d.cfSavings,'savings');return}
  if(d.cfRemovePhoto){cfRemovePhoto(d.cfDate,d.cfRemovePhoto);return}
  if(d.cfViewPhoto){const image=e.target;const full=$('cf-full-image');full.src=image.src;$('cf-image-dialog').showModal()}
});
// A marked savings deposit is linked to the day that was selected in the calendar.
document.addEventListener('click',e=>{
  const b=e.target.closest('button[data-deposit]');if(!b)return;
  const entry=state.savings?.entries?.find(x=>x.id===b.dataset.deposit);if(!entry)return;
  if(entry.paid)delete entry.paidOn;else entry.paidOn=selected;
},true);
function cfBlobDataURL(blob){return new Promise((resolve,reject)=>{const f=new FileReader();f.onload=()=>resolve(f.result);f.onerror=()=>reject(f.error);f.readAsDataURL(blob)})}
async function cfBackupPhotos(){
  const out={};
  for(const [date,rows] of Object.entries(state.calendarPhotos||{})){
    if(!validDate(date)||!Array.isArray(rows))continue;
    for(const row of rows){const file=await wishPhoto('get',cfPhotoKey(row.id));if(!file)throw Error('Не найдено фото за '+date);out[row.id]=file instanceof Blob?await cfBlobDataURL(file):file}
  }
  return out;
}
$('export').onclick=async()=>{
  try{
    const json=JSON.stringify({...state,wishes:await wishesForBackup(),calendarPhotoFiles:await cfBackupPhotos()},null,2);
    if(new Blob([json]).size>49000000)throw Error('Копия с фото больше 49 МБ. Уменьши количество фотографий.');
    const name='calendar-backup-'+iso()+'.json';
    if(window.AndroidFiles){AndroidFiles.saveBackup(name,json);return}
    const url=URL.createObjectURL(new Blob([json],{type:'application/json'})),a=document.createElement('a');a.href=url;a.download=name;a.click();setTimeout(()=>URL.revokeObjectURL(url),1500);
    toast('Копия с фото подготовлена',1000);
  }catch(err){toast(err.message||'Не удалось сохранить копию')}
};
const cfOriginalImport=$('file').onchange;
$('file').onchange=async event=>{
  const source=event.target.files?.[0];if(!source)return;
  try{
    const raw=await source.text(),obj=JSON.parse(raw),photos=obj.calendarPhotoFiles||{};
    if(!obj.calendarPhotoFiles)return cfOriginalImport(event);
    if(!photos||typeof photos!=='object'||Array.isArray(photos)||Object.keys(photos).length>2000)throw Error();
    for(const [id,data] of Object.entries(photos))if(!/^[a-f0-9-]{12,50}$/i.test(id)||typeof data!=='string'||!/^data:image\//.test(data)||data.length>13000000)throw Error();
    delete obj.calendarPhotoFiles;
    const token='cf-'+crypto.randomUUID();obj.__calendarImportToken=token;
    const copy=JSON.stringify(obj);
    const proxy={target:{files:[{name:source.name,size:new Blob([copy]).size,text:async()=>copy}],value:''}};
    await cfOriginalImport(proxy);
    if(state.__calendarImportToken!==token)return;
    delete state.__calendarImportToken;
    for(const [id,data] of Object.entries(photos)){const blob=await(await fetch(data)).blob();await wishPhoto('put',cfPhotoKey(id),blob)}
    persist();toast('Копия и фотографии восстановлены',1400);
  }catch(err){toast('Не удалось прочитать фотографии из копии')}
  finally{event.target.value=''}
};
cfSetup();
tab='calendar';render();
