from pathlib import Path

p=Path("app/src/main/assets/index.html")
s=p.read_text(encoding="utf-8")
old_preview="""'<div class="notes-media-item"><div class="notes-media-preview">'+(video?'<video src="'+url+'" controls preload="metadata" playsinline></video>':'<img src="'+url+'" loading="lazy" alt="'+esc(item.name)+'">')+'</div><div class="notes-media-info">"""
new_preview="""'<div class="notes-media-item"><button type="button" class="notes-media-preview media-open-preview" data-media-preview="'+esc(item.id)+'" aria-label="Открыть '+esc(item.name)+'">'+(video?'<video src="'+url+'" muted preload="metadata" playsinline></video><span class="media-video-play" aria-hidden="true">▶</span>':'<img src="'+url+'" loading="lazy" alt="'+esc(item.name)+'">')+'</button><div class="notes-media-info">"""
assert s.count(old_preview)==1,"Media preview markup missing"
s=s.replace(old_preview,new_preview,1)

old_start=s.index("function mediaMenuFolder(id){")
old_end=s.index("document.addEventListener('click',e=>{",old_start)
replacement=r"""
let mediaOverlayURL=null,mediaSheetContext=null;
function mediaOverlayClose(){
 const overlay=document.getElementById('notes-media-viewer');
 if(overlay){const video=overlay.querySelector('video');if(video)video.pause();overlay.remove()}
 if(mediaOverlayURL){URL.revokeObjectURL(mediaOverlayURL);mediaOverlayURL=null}
 document.body.classList.remove('notes-media-viewing');
}
async function mediaOpenPreview(id){
 try{
  const folder=mediaFolderActive,items=await mediaQuery(folder);
  const item=items.find(x=>x.id===id);
  if(!item||mediaFolderActive!==folder)return;
  mediaOverlayClose();
  mediaOverlayURL=URL.createObjectURL(item.blob);
  const video=item.type.startsWith('video/');
  const name=esc(item.name);
  document.body.insertAdjacentHTML('beforeend','<div id="notes-media-viewer" class="notes-media-viewer" role="dialog" aria-modal="true" aria-label="Просмотр файла"><header class="media-viewer-head"><span>'+name+'</span><button type="button" data-media-viewer-close aria-label="Закрыть">✕</button></header><div class="media-viewer-stage">'+(video?'<video src="'+mediaOverlayURL+'" controls autoplay playsinline></video>':'<img src="'+mediaOverlayURL+'" alt="'+name+'">')+'</div></div>');
  document.body.classList.add('notes-media-viewing');
 }catch(err){toast('Не удалось открыть файл: '+err.message)}
}
function mediaSheetClose(){
 document.getElementById('notes-media-sheet')?.remove();
 mediaSheetContext=null;
}
function mediaSheetDisplay(title,content){
 mediaSheetClose();
 document.body.insertAdjacentHTML('beforeend','<div id="notes-media-sheet" class="notes-media-sheet-cover"><div class="notes-media-sheet-backdrop" data-media-sheet-dismiss></div><section class="notes-media-sheet" role="dialog" aria-modal="true" aria-label="'+esc(title)+'"><div class="notes-media-sheet-handle"></div><div class="notes-media-sheet-head"><strong>'+esc(title)+'</strong><button type="button" data-media-sheet-dismiss aria-label="Закрыть">✕</button></div><div class="notes-media-sheet-content">'+content+'</div></section></div>');
}
function mediaSheetAction(label,action,danger=false){
 return '<button type="button" class="notes-media-sheet-action'+(danger?' danger':'')+'" data-media-action="'+action+'">'+esc(label)+'</button>';
}
function mediaShowCreate(){
 mediaSheetDisplay('Новая папка','<label class="media-sheet-label" for="media-folder-name">Название папки</label><input class="media-sheet-input" id="media-folder-name" type="text" maxlength="80" placeholder="Например, Фото" value=""><button type="button" class="notes-media-sheet-primary" data-media-action="create-save">Создать папку</button>');
 document.getElementById('media-folder-name')?.focus();
}
function mediaMenuFolder(id){
 const f=mediaFolderList().find(x=>x.id===id);if(!f)return;
 mediaSheetDisplay(f.name,mediaSheetAction('Переименовать','folder-rename')+mediaSheetAction('Удалить папку','folder-delete',true));
 mediaSheetContext={kind:'folder',id};
}
async function mediaMenuFile(id){
 try{
  const items=await mediaQuery(mediaFolderActive),item=items.find(x=>x.id===id);if(!item)return;
  mediaSheetDisplay(item.name,mediaSheetAction('Переместить в другую папку','file-move')+mediaSheetAction('Удалить файл','file-delete',true));
  mediaSheetContext={kind:'file',id};
 }catch(err){toast('Не удалось открыть настройки: '+err.message)}
}
function mediaSheetForm(title,label,value,action){
 mediaSheetDisplay(title,'<label class="media-sheet-label" for="media-folder-name">'+esc(label)+'</label><input class="media-sheet-input" id="media-folder-name" maxlength="80" value="'+esc(value)+'" type="text"><button type="button" class="notes-media-sheet-primary" data-media-action="'+action+'">Сохранить</button>');
 document.getElementById('media-folder-name')?.focus();
}
async function mediaSheetDo(action){
 const ctx=mediaSheetContext;
 if(action==='create-save'){
  const name=document.getElementById('media-folder-name')?.value.trim().slice(0,80);if(!name){toast('Введи название папки');return}
  const folders=mediaFolderList();
  folders.push({id:'mf_'+Date.now().toString(36)+Math.random().toString(36).slice(2,8),name});
  if(!persist())folders.pop();
  mediaSheetClose();renderNotes();return;
 }
 if(!ctx)return;
 const folder=mediaFolderList().find(x=>x.id===ctx.id);
 if(action==='folder-rename'&&folder){
  mediaSheetForm('Переименовать папку','Название папки',folder.name,'folder-rename-save');
  mediaSheetContext=ctx;return;
 }
 if(action==='folder-rename-save'&&folder){
  const name=document.getElementById('media-folder-name')?.value.trim().slice(0,80);if(!name){toast('Введи название папки');return}
  const old=folder.name;folder.name=name;
  if(!persist())folder.name=old;
  mediaSheetClose();renderNotes();return;
 }
 if(action==='folder-delete'&&folder){
  mediaSheetDisplay('Удалить папку?','<p class="media-sheet-warning">Папка «'+esc(folder.name)+'» и все фото и видео в ней будут удалены без возможности восстановления.</p>'+mediaSheetAction('Удалить папку и файлы','folder-delete-confirm',true)+mediaSheetAction('Отмена','cancel'));
  mediaSheetContext=ctx;return;
 }
 if(action==='folder-delete-confirm'&&folder){
  try{
   const items=await mediaQuery(ctx.id);
   for(const item of items)await mediaRemove(item.id);
   state.mediaFolders=mediaFolderList().filter(x=>x.id!==ctx.id);
   persist();mediaSheetClose();renderNotes();toast('Папка удалена');
  }catch(err){toast('Не удалось удалить папку: '+err.message)}
  return;
 }
 if(action==='file-move'&&ctx.kind==='file'){
  const other=mediaFolderList().filter(x=>x.id!==mediaFolderActive);
  if(!other.length){toast('Сначала создай ещё одну папку');return}
  mediaSheetDisplay('Переместить в папку',other.map(f=>'<button type="button" class="notes-media-sheet-action" data-media-move-target="'+esc(f.id)+'">'+esc(f.name)+'</button>').join(''));
  mediaSheetContext=ctx;return;
 }
 if(action==='file-delete'&&ctx.kind==='file'){
  mediaSheetDisplay('Удалить файл?','<p class="media-sheet-warning">Фото или видео будет удалено из папки без возможности восстановления.</p>'+mediaSheetAction('Удалить файл','file-delete-confirm',true)+mediaSheetAction('Отмена','cancel'));
  mediaSheetContext=ctx;return;
 }
 if(action==='file-delete-confirm'&&ctx.kind==='file'){
  try{await mediaRemove(ctx.id);mediaSheetClose();mediaFolderRender();toast('Файл удалён')}
  catch(err){toast('Ошибка удаления: '+err.message)}
 }
}
document.addEventListener('click',e=>{
 const viewer=e.target.closest('[data-media-viewer-close]');if(viewer){mediaOverlayClose();return}
 const dismiss=e.target.closest('[data-media-sheet-dismiss]');if(dismiss){mediaSheetClose();return}
 const preview=e.target.closest('[data-media-preview]');if(preview){mediaOpenPreview(preview.dataset.mediaPreview);return}
 const b=e.target.closest('button');if(!b)return;
 if(b.dataset.mediaAction){
  if(b.dataset.mediaAction==='cancel')mediaSheetClose();
  else mediaSheetDo(b.dataset.mediaAction);
  return;
 }
 if(b.dataset.mediaMoveTarget){
  const id=b.dataset.mediaMoveTarget,ctx=mediaSheetContext;
  if(!ctx||ctx.kind!=='file')return;
  (async()=>{try{
    const items=await mediaQuery(mediaFolderActive),item=items.find(x=>x.id===ctx.id);
    if(!item)return;item.folder=id;await mediaPut(item);
    mediaSheetClose();mediaFolderRender();toast('Перемещено');
   }catch(err){toast('Не удалось переместить: '+err.message)}
  })();
 }
});
const mediaOldBackHandler=window.handlePlannerBack;
window.handlePlannerBack=function(){
 if(document.getElementById('notes-media-viewer')){mediaOverlayClose();return true}
 if(document.getElementById('notes-media-sheet')){mediaSheetClose();return true}
 return mediaOldBackHandler?.()||false;
};
"""
s=s[:old_start]+replacement+s[old_end:]
old_create="""if(b.hasAttribute('data-media-new-folder')){
  const name=prompt('Название новой папки','Фото')?.trim().slice(0,80);if(!name)return;
  mediaFolderList().push({id:'mf_'+Date.now().toString(36)+Math.random().toString(36).slice(2,8),name});
  if(persist())renderNotes();
 }"""
assert s.count(old_create)==1,"Old create folder prompt not found"
s=s.replace(old_create,"if(b.hasAttribute('data-media-new-folder'))mediaShowCreate();",1)
style=r"""
<style id="notes-media-polish">
.notes-media-preview.media-open-preview{position:relative;width:100%;height:auto;padding:0;border:0;cursor:pointer;overflow:hidden}
.media-video-play{position:absolute;inset:0;display:grid;place-items:center;color:white;font-size:32px;text-shadow:0 1px 10px #000;pointer-events:none}
.notes-media-viewer{position:fixed;inset:0;z-index:20000;background:#07070a;display:flex;flex-direction:column;color:white}
.media-viewer-head{flex:none;display:flex;align-items:center;gap:10px;padding:14px 16px;background:#151319;min-height:60px}
.media-viewer-head span{flex:1;min-width:0;font-weight:600;font-size:14px;overflow:hidden;white-space:nowrap;text-overflow:ellipsis}
.media-viewer-head button{border:0;background:#333039;color:#fff;border-radius:50%;width:38px;height:38px;font-size:20px}
.media-viewer-stage{display:flex;align-items:center;justify-content:center;flex:1;min-height:0;overflow:hidden}
.media-viewer-stage img,.media-viewer-stage video{display:block;width:100%;height:100%;object-fit:contain}
.notes-media-sheet-cover{position:fixed;inset:0;z-index:19000;display:flex;align-items:flex-end;justify-content:center}
.notes-media-sheet-backdrop{position:absolute;inset:0;background:rgba(0,0,0,.58)}
.notes-media-sheet{position:relative;width:100%;max-width:560px;max-height:85vh;overflow:auto;border:1px solid var(--border);border-radius:24px 24px 0 0;padding:8px 18px max(30px,env(safe-area-inset-bottom));background:var(--card);color:var(--text);box-shadow:0 -8px 28px #0005}
.notes-media-sheet-handle{width:40px;height:5px;border-radius:6px;background:var(--muted);opacity:.45;margin:0 auto 14px}
.notes-media-sheet-head{display:flex;align-items:center;gap:12px;margin-bottom:16px}
.notes-media-sheet-head strong{flex:1;font-size:19px;line-height:1.25;overflow-wrap:anywhere}
.notes-media-sheet-head button{width:34px;height:34px;border:0;border-radius:10px;background:var(--control);color:var(--text);font-size:19px}
.notes-media-sheet-content{display:grid;gap:10px}
.notes-media-sheet-action{width:100%;min-height:52px;text-align:left;border:1px solid var(--border);border-radius:13px;background:var(--control);color:var(--text);font-size:15px;font-weight:600;padding:12px 16px}
.notes-media-sheet-action.danger{color:#e4444e}
.media-sheet-label{font-size:13px;font-weight:600;color:var(--muted)}
.media-sheet-input{width:100%;box-sizing:border-box;min-height:50px;border:1px solid var(--border);border-radius:12px;background:var(--app-bg);color:var(--text);padding:10px 14px;font:inherit;font-size:16px}
.notes-media-sheet-primary{width:100%;min-height:50px;background:var(--accent);border:0;border-radius:12px;color:var(--on-accent);font-weight:800;font-size:15px}
.media-sheet-warning{margin:0 0 8px;line-height:1.5;font-size:14px}
</style>
"""
assert "</head>" in s
s=s.replace("</head>",style+"</head>",1)
p.write_text(s,encoding="utf-8")
print("Gallery opens full-screen; folder/file management uses native-looking action sheet")
