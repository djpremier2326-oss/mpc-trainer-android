from pathlib import Path
p=Path('app/src/main/assets/index.html')
s=p.read_text(encoding='utf-8')
anchor="initializeExtras();\nif(!state.prefs.mainMenu.includes(tab))"
assert s.count(anchor)==1, "Cannot locate notes gallery insertion"
script=r"""
// Standalone photo/video folders inside Notes, leaving existing notes untouched.
let mediaFolderActive=null,mediaObjectURLs=[],mediaDBPromise=null;
function mediaFolderList(){return Array.isArray(state.mediaFolders)?state.mediaFolders:state.mediaFolders=[]}
function mediaDB(){
 if(!mediaDBPromise)mediaDBPromise=new Promise((resolve,reject)=>{
  if(!window.indexedDB){reject(Error('Хранилище файлов недоступно'));return}
  const request=indexedDB.open('dayplan-note-media',1);
  request.onupgradeneeded=()=>{
   const db=request.result,store=db.createObjectStore('media',{keyPath:'id'});
   store.createIndex('folder','folder',{unique:false});
  };
  request.onsuccess=()=>resolve(request.result);
  request.onerror=()=>reject(request.error||Error('Не удалось открыть хранилище'));
 });
 return mediaDBPromise;
}
async function mediaQuery(folder){
 const db=await mediaDB();
 return new Promise((resolve,reject)=>{
  const tx=db.transaction('media','readonly');
  const request=folder?tx.objectStore('media').index('folder').getAll(folder):tx.objectStore('media').getAll();
  request.onsuccess=()=>resolve(request.result);
  request.onerror=()=>reject(request.error||Error('Не удалось прочитать файлы'));
 });
}
async function mediaPut(record){
 const db=await mediaDB();
 return new Promise((resolve,reject)=>{
  const tx=db.transaction('media','readwrite');
  tx.objectStore('media').put(record);
  tx.oncomplete=()=>resolve();
  tx.onerror=()=>reject(tx.error||Error('Недостаточно места для файла'));
  tx.onabort=()=>reject(tx.error||Error('Не удалось сохранить файл'));
 });
}
async function mediaRemove(id){
 const db=await mediaDB();
 return new Promise((resolve,reject)=>{
  const tx=db.transaction('media','readwrite');tx.objectStore('media').delete(id);
  tx.oncomplete=resolve;tx.onerror=()=>reject(tx.error);
 });
}
function mediaDispose(){
 for(const url of mediaObjectURLs)URL.revokeObjectURL(url);
 mediaObjectURLs=[];
}
function mediaFolderCards(){
 const folders=mediaFolderList();
 if(!folders.length)return '';
 return '<div class="notes-media-folders"><div class="notes-media-heading">Мои папки</div><div class="notes-media-folder-grid">'+folders.map(f=>
 '<div class="notes-media-folder"><button type="button" class="notes-media-folder-open" data-media-folder-open="'+esc(f.id)+'"><span class="notes-folder-symbol">▣</span><strong>'+esc(f.name)+'</strong></button><button type="button" class="notes-media-folder-menu" data-media-folder-menu="'+esc(f.id)+'" aria-label="Действия с папкой">⋯</button></div>'
 ).join('')+'</div></div>';
}
function mediaInstallListUI(){
 if(tab!=='notes'||activeNoteId||mediaFolderActive)return;
 const tools=document.querySelector('.notes-tools');
 if(!tools)return;
 if(!document.querySelector('[data-media-new-folder]'))tools.insertAdjacentHTML('beforeend','<button type="button" class="btn" data-media-new-folder>＋ Папка</button>');
 const list=$('notes-list');if(list)list.insertAdjacentHTML('beforebegin',mediaFolderCards());
}
function mediaFolderRender(){
 if(tab!=='notes'||!mediaFolderActive)return;
 const f=mediaFolderList().find(x=>x.id===mediaFolderActive);
 if(!f){mediaFolderActive=null;renderNotes();return}
 mediaDispose();document.body.classList.remove('note-open');
 $('view').innerHTML='<div class="notes-media-gallery"><div class="notes-media-toolbar"><button type="button" class="btn" data-media-back>‹ Заметки</button><strong>'+esc(f.name)+'</strong><button type="button" class="btn primary" data-media-upload>＋ Фото/видео</button></div><div id="notes-media-grid" class="notes-media-grid"><p class="notes-media-empty">Открываем папку…</p></div><input type="file" id="notes-media-picker" accept="image/*,video/*" multiple hidden></div>';
 mediaQuery(f.id).then(items=>{
  if(mediaFolderActive!==f.id)return;
  const host=$('notes-media-grid');if(!host)return;
  host.innerHTML=items.length?items.sort((a,b)=>b.created-a.created).map(item=>{
   const url=URL.createObjectURL(item.blob);
   mediaObjectURLs.push(url);
   const video=item.type.startsWith('video/');
   return '<div class="notes-media-item"><div class="notes-media-preview">'+(video?'<video src="'+url+'" controls preload="metadata" playsinline></video>':'<img src="'+url+'" loading="lazy" alt="'+esc(item.name)+'">')+'</div><div class="notes-media-info"><small title="'+esc(item.name)+'">'+esc(item.name)+'</small><button type="button" data-media-file-menu="'+esc(item.id)+'" aria-label="Действия с файлом">⋯</button></div></div>'
  }).join(''):'<p class="notes-media-empty">В этой папке пока пусто. Нажми «＋ Фото/видео».</p>';
 }).catch(err=>{const h=$('notes-media-grid');if(h)h.textContent='Не удалось открыть файлы: '+err.message});
}
const mediaBaseRenderNotes=renderNotes;
renderNotes=function(){
 if(tab==='notes'&&mediaFolderActive){mediaFolderRender();return}
 mediaBaseRenderNotes();
 if(tab==='notes'&&!activeNoteId)mediaInstallListUI();
};
const mediaBaseHandleBack=window.handlePlannerBack;
window.handlePlannerBack=function(){
 if(tab==='notes'&&mediaFolderActive){mediaFolderActive=null;mediaDispose();renderNotes();return true}
 return mediaBaseHandleBack?.()||false;
};
function mediaMenuFolder(id){
 const f=mediaFolderList().find(x=>x.id===id);if(!f)return;
 const action=prompt('Папка «'+f.name+'»\n1 — переименовать\n2 — удалить папку','1');
 if(action==='1'){
  const name=prompt('Новое название папки',f.name)?.trim().slice(0,80);
  if(!name)return;
  f.name=name;persist();renderNotes();
 }else if(action==='2'){
  if(!confirm('Удалить папку «'+f.name+'» и все фото и видео внутри неё?'))return;
  mediaQuery(id).then(async items=>{
   for(const file of items)await mediaRemove(file.id);
   state.mediaFolders=mediaFolderList().filter(x=>x.id!==id);
   persist();renderNotes();toast('Папка удалена');
  }).catch(e=>toast('Не удалось удалить файлы: '+e.message));
 }
}
async function mediaMenuFile(id){
 let items;try{items=await mediaQuery(mediaFolderActive)}catch(e){toast(e.message);return}
 const item=items.find(x=>x.id===id);if(!item)return;
 const action=prompt('Файл «'+item.name+'»\n1 — переместить в другую папку\n2 — удалить файл','1');
 if(action==='1'){
  const other=mediaFolderList().filter(x=>x.id!==mediaFolderActive);
  if(!other.length){toast('Создай ещё одну папку для переноса');return}
  const choice=prompt(other.map((x,i)=>(i+1)+' — '+x.name).join('\n'),'1');
  const dest=other[Number(choice)-1];if(!dest)return;
  item.folder=dest.id;try{await mediaPut(item);mediaFolderRender();toast('Файл перемещён')}catch(e){toast('Не удалось переместить файл')}
 }else if(action==='2'){
  if(!confirm('Удалить '+item.name+'?'))return;
  try{await mediaRemove(id);mediaFolderRender();toast('Файл удалён')}catch(e){toast('Ошибка удаления')}
 }
}
document.addEventListener('click',e=>{
 const b=e.target.closest('button');if(!b||tab!=='notes')return;
 if(b.hasAttribute('data-media-new-folder')){
  const name=prompt('Название новой папки','Фото')?.trim().slice(0,80);if(!name)return;
  mediaFolderList().push({id:'mf_'+Date.now().toString(36)+Math.random().toString(36).slice(2,8),name});
  if(persist())renderNotes();
 }
 if(b.dataset.mediaFolderOpen){mediaFolderActive=b.dataset.mediaFolderOpen;mediaFolderRender()}
 if(b.dataset.mediaFolderMenu)mediaMenuFolder(b.dataset.mediaFolderMenu);
 if(b.hasAttribute('data-media-back')){mediaFolderActive=null;mediaDispose();renderNotes()}
 if(b.hasAttribute('data-media-upload'))document.getElementById('notes-media-picker')?.click();
 if(b.dataset.mediaFileMenu)mediaMenuFile(b.dataset.mediaFileMenu);
});
document.addEventListener('change',async e=>{
 const picker=e.target;if(picker.id!=='notes-media-picker'||!mediaFolderActive)return;
 const folder=mediaFolderActive,files=Array.from(picker.files||[]);
 if(!files.length)return;
 let saved=0;
 for(const file of files){
  if(!/^image\/|^video\//.test(file.type)){toast('Можно добавлять только фото и видео');continue}
  try{
   await mediaPut({id:'mi_'+Date.now().toString(36)+Math.random().toString(36).slice(2,11),folder,name:file.name.slice(0,150),type:file.type,blob:file,created:Date.now()});
   saved++;
  }catch(err){toast('Не удалось сохранить файл: '+err.message);break}
 }
 picker.value='';
 if(mediaFolderActive===folder)mediaFolderRender();
 if(saved)toast('Сохранено файлов: '+saved);
});
"""
css=r"""
<style id="notes-media-folder-style">
.notes-tools{flex-wrap:wrap}
.notes-media-folders{margin:14px 0 18px}
.notes-media-heading{font-weight:800;margin:5px 2px 10px}
.notes-media-folder-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:10px}
.notes-media-folder{display:flex;align-items:stretch;border:1px solid var(--border);background:var(--card);color:var(--text);border-radius:12px;overflow:hidden;min-height:76px}
.notes-media-folder-open{border:0;background:none;color:inherit;display:flex;align-items:center;gap:9px;flex:1;min-width:0;text-align:left;padding:12px;font:inherit}
.notes-media-folder-open strong{overflow-wrap:anywhere;font-size:13px}
.notes-folder-symbol{font-size:25px;color:var(--accent)}
.notes-media-folder-menu{border:0;background:transparent;color:var(--text);font-size:25px;padding:0 11px}
.notes-media-gallery{min-height:100%;padding:12px 8px 24px}
.notes-media-toolbar{display:flex;align-items:center;gap:8px;flex-wrap:wrap;margin-bottom:15px}
.notes-media-toolbar strong{flex:1;font-size:17px;overflow-wrap:anywhere}
.notes-media-toolbar .btn{font-size:12px;padding:8px}
.notes-media-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:11px}
.notes-media-item{min-width:0;background:var(--card);border:1px solid var(--border);border-radius:12px;overflow:hidden}
.notes-media-preview{aspect-ratio:1;background:#101010;display:flex;align-items:center;justify-content:center}
.notes-media-preview img,.notes-media-preview video{width:100%;height:100%;object-fit:contain}
.notes-media-info{display:flex;align-items:center;gap:6px;min-height:40px;padding:5px 8px}
.notes-media-info small{flex:1;white-space:nowrap;text-overflow:ellipsis;overflow:hidden;font-size:11px}
.notes-media-info button{color:var(--text);border:0;background:none;font-size:23px}
.notes-media-empty{grid-column:1/-1;color:var(--muted);text-align:center;margin:40px 0}
</style>
"""
s=s.replace(anchor,script+"\n"+anchor,1)
s=s.replace("</head>",css+"</head>",1)
p.write_text(s,encoding='utf-8')
print("Independent note media folders installed")
