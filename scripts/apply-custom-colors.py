from pathlib import Path
p=Path('app/src/main/assets/index.html')
s=p.read_text(encoding='utf-8')
old="keys.includes(c)"
if s.count(old)!=2:
    raise SystemExit("Expected two ICE palette key checks, found "+str(s.count(old)))
s=s.replace(old,"(keys.includes(c)||/^user_[a-z0-9]{8,24}$/.test(c))",2)
anchor="initializeExtras();\nif(!state.prefs.mainMenu.includes(tab))"
if anchor not in s:
    raise SystemExit("Palette insertion marker missing")
js=r"""
// User-editable, persistent swatches shared by day and task color selectors.
const paletteBase=JSON.parse(JSON.stringify(ICE_COLORS));
function paletteData(){
  if(!state.customPalette||typeof state.customPalette!=='object'||Array.isArray(state.customPalette))state.customPalette={};
  const p=state.customPalette;
  if(!p.items||typeof p.items!=='object'||Array.isArray(p.items))p.items={};
  if(!p.overrides||typeof p.overrides!=='object'||Array.isArray(p.overrides))p.overrides={};
  if(!Array.isArray(p.hidden))p.hidden=[];
  for(const [key,entry] of Object.entries(p.items)){
    if(!/^user_[a-z0-9]{8,24}$/.test(key)||!entry||typeof entry.name!=='string'||!/^#[0-9a-f]{6}$/i.test(entry.hex||''))delete p.items[key];
  }
  for(const [key,entry] of Object.entries(p.overrides)){
    if(!Object.hasOwn(paletteBase,key)||key==='none'||!entry||typeof entry.name!=='string'||!/^#[0-9a-f]{6}$/i.test(entry.hex||''))delete p.overrides[key];
  }
  p.hidden=p.hidden.filter(k=>k!=='none'&&(Object.hasOwn(paletteBase,k)||Object.hasOwn(p.items,k)));
  return p;
}
function paletteInk(hex){
  const rgb=[1,3,5].map(i=>parseInt(hex.slice(i,i+2),16)/255).map(v=>v<=0.04045?v/12.92:Math.pow((v+0.055)/1.055,2.4));
  return rgb[0]*.2126+rgb[1]*.7152+rgb[2]*.0722>.38?'#173047':'#ffffff';
}
function paletteSync(){
  const p=paletteData();
  for(const key of Object.keys(ICE_COLORS))if(!Object.hasOwn(paletteBase,key))delete ICE_COLORS[key];
  for(const [key,entry] of Object.entries(paletteBase))ICE_COLORS[key]=entry.slice();
  for(const [key,entry] of Object.entries(p.overrides))ICE_COLORS[key]=[entry.name.slice(0,40),entry.hex,entry.hex,paletteInk(entry.hex)];
  for(const [key,entry] of Object.entries(p.items))ICE_COLORS[key]=[entry.name.slice(0,40),entry.hex,entry.hex,paletteInk(entry.hex)];
}
paletteSync();
const paletteOriginalActive=activeColors;
activeColors=function(){
  if(state.prefs.design!=='ice')return COLORS;
  const hidden=paletteData().hidden;
  return Object.fromEntries(Object.entries(ICE_COLORS).filter(([k])=>!hidden.includes(k)));
};
function palettePickerRows(type){
  const current=type==='day'?dayColor(dayColorDate):slots()[colorIndex]?.color;
  const entries=Object.entries(activeColors());
  if(current&&!entries.some(([k])=>k===current)&&ICE_COLORS[current])entries.push([current,ICE_COLORS[current]]);
  return entries.map(([key,entry])=>'<button type="button" class="swatch" data-'+(type==='day'?'day-color-pick':'pick-color')+'="'+key+'" aria-pressed="'+(key===current)+'"><i style="background:'+entry[1]+'"></i>'+esc(entry[0])+'</button>').join('')+
    '<button type="button" class="palette-manage-entry" data-palette-manager>＋ Создать свой цвет / Изменить палитру</button>';
}
openColors=function(i){
  colorIndex=i;
  $('palette').innerHTML=palettePickerRows('task');
  $('color-dialog').showModal();
};
openDayColors=function(d){
  if(!validDate(d))return;
  if(FRIEND_SHIFT&&friendShift(d).color!=='none'){toast(friendShiftText(d)+' · по графику');return}
  dayColorDate=d;
  $('day-color-date').textContent=fmt(d,{weekday:'long',day:'numeric',month:'long',year:'numeric'});
  $('day-color-palette').innerHTML=palettePickerRows('day');
  $('day-color-dialog').showModal();
};
const paletteUi=document.createElement('div');
paletteUi.innerHTML='<dialog id="palette-manager-dialog" class="custom-palette-dialog"><div class="dialog-head"><h2>Моя палитра</h2><button type="button" data-palette-close="manager" aria-label="Закрыть">×</button></div><p class="dialog-note">Меняй оттенки, скрывай ненужные и сохраняй свои. Цвета уже созданных заданий не потеряются.</p><div id="palette-manager-list"></div><button type="button" class="btn primary" data-palette-new>＋ Новый цвет</button></dialog>'+
'<dialog id="palette-edit-dialog" class="custom-palette-dialog"><div class="dialog-head"><h2 id="palette-edit-heading">Цвет</h2><button type="button" data-palette-close="edit" aria-label="Назад">×</button></div><label class="palette-field">Название<input id="palette-edit-name" maxlength="40" placeholder="Например, шоколадный"></label><div class="palette-picker-line"><input type="color" id="palette-edit-native" value="#795548" aria-label="Выбрать оттенок"><label class="palette-field">HEX<input id="palette-edit-hex" value="#795548" maxlength="7" spellcheck="false" inputmode="text"></label></div><div class="palette-preview" id="palette-edit-preview">Предпросмотр задания</div><p id="palette-edit-error" class="dialog-note" hidden></p><div class="dialog-actions"><button type="button" class="btn" data-palette-close="edit">Отмена</button><button type="button" class="btn primary" data-palette-save>Сохранить цвет</button></div></dialog>';
document.body.append(...paletteUi.children);
let paletteEditing=null,paletteReturn='task';
function paletteManagerRender(){
  const p=paletteData();
  $('palette-manager-list').innerHTML=Object.entries(ICE_COLORS).filter(([key])=>key!=='none').map(([key,entry])=>{
    const hidden=p.hidden.includes(key),custom=Object.hasOwn(p.items,key);
    return '<div class="palette-manager-row"><span class="palette-sample" style="background:'+entry[1]+'"></span><span class="palette-manager-name">'+esc(entry[0])+(custom?'<small>Мой цвет</small>':'<small>Стандартный</small>')+'</span><button type="button" data-palette-edit="'+key+'">Изменить</button><button type="button" data-palette-toggle="'+key+'">'+(hidden?'Вернуть':'Скрыть')+'</button></div>';
  }).join('');
}
function paletteOpenManager(){
  paletteReturn=$('day-color-dialog').open?'day':'task';
  if($('day-color-dialog').open)$('day-color-dialog').close();
  if($('color-dialog').open)$('color-dialog').close();
  paletteManagerRender();$('palette-manager-dialog').showModal();
}
function paletteOpenEdit(key){
  paletteEditing=key;
  const entry=key?ICE_COLORS[key]:null;
  $('palette-edit-heading').textContent=key?'Изменить цвет':'Создать свой цвет';
  $('palette-edit-name').value=entry?.[0]||'Мой цвет';
  $('palette-edit-native').value=entry?.[1]&&/^#[0-9a-f]{6}$/i.test(entry[1])?entry[1]:'#795548';
  $('palette-edit-hex').value=$('palette-edit-native').value.toUpperCase();
  $('palette-edit-error').hidden=true;
  paletteUpdatePreview();
  $('palette-manager-dialog').close();
  $('palette-edit-dialog').showModal();
}
function paletteUpdatePreview(){
  const hex=$('palette-edit-hex').value;
  if(!/^#[0-9a-f]{6}$/i.test(hex))return;
  $('palette-edit-preview').style.background=hex;
  $('palette-edit-preview').style.color=paletteInk(hex);
}
function paletteRefreshView(){
  paletteSync();
  const paper=document.querySelector('.violet-event-paper'),scroll=paper?.scrollTop;
  if(tab==='day'||tab==='week'||tab==='calendar'||tab==='month')render();
  const replacement=document.querySelector('.violet-event-paper');if(replacement&&scroll!==undefined)requestAnimationFrame(()=>replacement.scrollTop=scroll);
}
function paletteSave(){
  const name=$('palette-edit-name').value.trim().slice(0,40),hex=$('palette-edit-hex').value.trim().toLowerCase();
  if(!name||!/^#[0-9a-f]{6}$/.test(hex)){$('palette-edit-error').hidden=false;$('palette-edit-error').textContent='Укажи название и цвет формата #795548';return}
  const p=paletteData();
  let key=paletteEditing;
  if(!key){key='user_'+Date.now().toString(36)+Math.floor(Math.random()*46656).toString(36).padStart(3,'0');p.items[key]={name,hex}}
  else if(Object.hasOwn(paletteBase,key))p.overrides[key]={name,hex};
  else p.items[key]={name,hex};
  p.hidden=p.hidden.filter(k=>k!==key);
  if(!persist())return;
  paletteRefreshView();
  $('palette-edit-dialog').close();paletteManagerRender();$('palette-manager-dialog').showModal();
  toast('Цвет сохранён');
}
function paletteToggle(key){
  if(!ICE_COLORS[key]||key==='none')return;
  const p=paletteData(),i=p.hidden.indexOf(key);
  if(i>=0)p.hidden.splice(i,1);else p.hidden.push(key);
  if(persist()){paletteManagerRender();toast(i>=0?'Цвет снова в палитре':'Цвет скрыт из палитры')}
}
document.addEventListener('click',e=>{
  const b=e.target.closest('button');if(!b)return;
  if(b.hasAttribute('data-palette-manager')){paletteOpenManager();return}
  if(b.hasAttribute('data-palette-new')){paletteOpenEdit(null);return}
  if(b.dataset.paletteEdit){paletteOpenEdit(b.dataset.paletteEdit);return}
  if(b.dataset.paletteToggle){paletteToggle(b.dataset.paletteToggle);return}
  if(b.hasAttribute('data-palette-save')){paletteSave();return}
  if(b.dataset.paletteClose==='edit'){$('palette-edit-dialog').close();paletteManagerRender();$('palette-manager-dialog').showModal();return}
  if(b.dataset.paletteClose==='manager'){$('palette-manager-dialog').close();if(paletteReturn==='day')openDayColors(dayColorDate);else openColors(colorIndex)}
});
document.getElementById('palette-edit-native').addEventListener('input',e=>{$('palette-edit-hex').value=e.target.value.toUpperCase();paletteUpdatePreview()});
document.getElementById('palette-edit-hex').addEventListener('input',e=>{if(/^#[0-9a-f]{6}$/i.test(e.target.value)){$('palette-edit-native').value=e.target.value;paletteUpdatePreview()}});
"""
css=r"""
<style id="palette-user-styles">
.palette-manage-entry{grid-column:1/-1!important;display:block!important;width:100%!important;min-height:48px!important;padding:12px!important;border:1px dashed #b991db!important;border-radius:12px!important;background:#261a37!important;color:#f7eaff!important;font-weight:700!important}
.custom-palette-dialog{background:#18121f!important;color:#f8efff!important;border:1px solid #ae83d4!important;border-radius:20px!important;width:min(95vw,520px)!important;max-height:85dvh!important;overflow-y:auto!important;padding:18px!important}
.custom-palette-dialog::backdrop{background:#000a!important}
.custom-palette-dialog .dialog-head{display:flex;justify-content:space-between;align-items:center;gap:12px;margin-bottom:10px}
.custom-palette-dialog .dialog-head button{background:transparent;border:0;color:white;font-size:30px;padding:4px 10px}
.custom-palette-dialog .dialog-note{color:#d4bedf!important;line-height:1.35}
#palette-manager-list{display:grid;gap:4px;margin:14px 0;max-height:45dvh;overflow:auto}
.palette-manager-row{display:flex;align-items:center;gap:7px;padding:8px 0;border-bottom:1px solid #ffffff1c}
.palette-sample{flex:0 0 30px;width:30px;height:30px;border-radius:8px;border:1px solid #fff5}
.palette-manager-name{min-width:0;flex:1;font-size:14px;overflow-wrap:anywhere}
.palette-manager-name small{display:block;color:#b8a7c4;font-size:11px}
.palette-manager-row button{padding:9px 6px;border:1px solid #705783;background:#2b2135;color:#f7ecff;border-radius:9px;font-size:12px}
.custom-palette-dialog .btn.primary{background:#c69deb!important;color:#180e20!important}
.custom-palette-dialog [data-palette-new]{width:100%;min-height:46px}
.palette-field{display:flex;flex-direction:column;gap:6px;color:#e8d9f1;font-size:14px;margin:13px 0}
.palette-field input{min-height:45px;background:#2b2135!important;color:#fff!important;border:1px solid #765c89!important;border-radius:10px!important;padding:10px!important;font-size:17px!important;width:100%;box-sizing:border-box}
.palette-picker-line{display:flex;align-items:center;gap:13px}
.palette-picker-line input[type=color]{width:90px;height:84px;border:1px solid #765c89;border-radius:12px;background:#2b2135;padding:6px;flex:none}
.palette-picker-line label{flex:1;min-width:0}
.palette-preview{padding:17px 15px;border-radius:14px;font-weight:700;margin:8px 0;min-height:55px;display:flex;align-items:center}
.custom-palette-dialog .dialog-actions{display:flex;gap:10px;justify-content:flex-end;margin-top:12px}
.custom-palette-dialog .dialog-actions button{min-height:42px;padding:8px 15px}
</style>
"""
s=s.replace(anchor,js+"\ninitializeExtras();\nif(!state.prefs.mainMenu.includes(tab))",1)
s=s.replace("</head>",css+"</head>",1)
p.write_text(s,encoding='utf-8')
print("Editable palette UI installed")
