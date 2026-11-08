/* Album browsing and a cancellable multi-file selection; originals stay in IndexedDB. */
(()=>{
'use strict';
let urls=[],selected=new Set(),selectionFolder=null,pending=null,suppressUntil=0,busy=false;
const oldClose=mediaOverlayClose;
mediaOverlayClose=function(){document.querySelectorAll('#notes-media-viewer video').forEach(video=>video.pause());oldClose();urls.forEach(url=>URL.revokeObjectURL(url));urls=[]};
mediaOpenPreview=async function(id){
 const folder=mediaFolderActive;
 try{
  const items=(await mediaQuery(folder)).sort((a,b)=>b.created-a.created),index=items.findIndex(item=>item.id===id);
  if(index<0||mediaFolderActive!==folder)return;
  mediaOverlayClose();
  const viewer=document.createElement('div');viewer.id='notes-media-viewer';viewer.className='notes-media-viewer';viewer.setAttribute('role','dialog');viewer.setAttribute('aria-modal','true');viewer.setAttribute('aria-label','Фотографии альбома');
  viewer.innerHTML='<header class="media-viewer-head"><span id="album-gallery-count" aria-live="polite"></span><button type="button" data-media-viewer-close aria-label="Закрыть">✕</button></header><div id="album-gallery-track" class="media-viewer-stage" tabindex="0" aria-label="Листай влево и вправо"></div>';
  const track=viewer.querySelector('#album-gallery-track');
  for(const item of items){const page=document.createElement('div');page.className='album-gallery-page';const video=item.type.startsWith('video/'),media=document.createElement(video?'video':'img'),url=URL.createObjectURL(item.blob);urls.push(url);media.src=url;if(video){media.controls=true;media.playsInline=true;media.preload='metadata'}else{media.alt='Фотография';media.draggable=false}page.append(media);track.append(page)}
  document.body.append(viewer);document.body.classList.add('notes-media-viewing');
  function caption(){const i=Math.max(0,Math.min(items.length-1,Math.round(track.scrollLeft/(track.clientWidth||1))));viewer.querySelector('#album-gallery-count').textContent=(i+1)+' / '+items.length;track.querySelectorAll('video').forEach(video=>{if(video.parentElement!==track.children[i])video.pause()})}
  track.scrollLeft=index*track.clientWidth;caption();track.addEventListener('scroll',caption,{passive:true});
  viewer.addEventListener('keydown',e=>{if(e.key==='Escape'){e.preventDefault();mediaOverlayClose()}else if(['ArrowLeft','ArrowRight'].includes(e.key)){e.preventDefault();track.scrollBy({left:track.clientWidth*(e.key==='ArrowRight'?1:-1),behavior:'smooth'})}});
 }catch(error){mediaOverlayClose();toast('Не удалось открыть файл: '+error.message)}
};
function active(){return selectionFolder!==null&&selectionFolder===mediaFolderActive&&tab==='notes'}
function clear(){selected.clear();selectionFolder=null;paint()}
function paint(){
 let bar=$('album-selection-bar');const grid=$('notes-media-grid');
 if(!grid)return;
 if(!bar){bar=document.createElement('div');bar.id='album-selection-bar';bar.innerHTML='<strong id="album-selection-count"></strong><button type="button" class="btn" data-album-select-all>Все</button><button type="button" class="btn" data-album-delete>Удалить</button><button type="button" class="btn" data-album-cancel>Отмена</button>';grid.before(bar)}
 bar.hidden=!active();$('album-selection-count').textContent='Выбрано: '+selected.size;
 bar.querySelectorAll('button').forEach(b=>b.disabled=busy||b.hasAttribute('data-album-delete')&&!selected.size);
 grid.classList.toggle('album-selecting',active());grid.querySelectorAll('[data-media-preview]').forEach(button=>{const checked=active()&&selected.has(button.dataset.mediaPreview);button.classList.toggle('album-selected',checked);if(active()){button.setAttribute('aria-pressed',String(checked));button.setAttribute('aria-label',checked?'Снять выбор':'Выбрать файл')}else button.removeAttribute('aria-pressed')});
}
function choose(id){if(busy)return;if(!active()){selected.clear();selectionFolder=mediaFolderActive}if(selected.has(id))selected.delete(id);else selected.add(id);paint()}
function cancelHold(){if(pending)clearTimeout(pending.timer);pending=null}
document.addEventListener('pointerdown',e=>{cancelHold();if(!e.isPrimary||e.button!==0)return;if(active()){suppressUntil=0;return;}const button=e.target.closest('#notes-media-grid [data-media-preview]');if(!button)return;const p={id:e.pointerId,x:e.clientX,y:e.clientY,button};p.timer=setTimeout(()=>{if(pending!==p||!button.isConnected)return;pending=null;suppressUntil=performance.now()+800;choose(button.dataset.mediaPreview);navigator.vibrate?.(25)},500);pending=p},{passive:true});
document.addEventListener('pointermove',e=>{if(pending&&pending.id===e.pointerId&&Math.hypot(e.clientX-pending.x,e.clientY-pending.y)>10)cancelHold()},{passive:true});
document.addEventListener('pointerup',cancelHold,{passive:true});document.addEventListener('pointercancel',cancelHold,{passive:true});
document.addEventListener('contextmenu',e=>{if(e.target.closest('#notes-media-grid [data-media-preview]'))e.preventDefault()});
async function removeSelection(){
 if(busy||!active()||!selected.size)return;
 const ids=[...selected],folder=selectionFolder;if(!confirm('Удалить выбранные файлы ('+ids.length+')?'))return;
 busy=true;paint();
 try{const db=await mediaDB();await new Promise((resolve,reject)=>{const tx=db.transaction('media','readwrite'),store=tx.objectStore('media');for(const id of ids){const request=store.get(id);request.onsuccess=()=>{if(request.result?.folder!==folder){tx.abort();return}store.delete(id)}}tx.oncomplete=resolve;tx.onabort=tx.onerror=()=>reject(tx.error||Error('Не удалось удалить выбранные файлы'))});document.dispatchEvent(new Event('planner-media-changed'));clear();if(mediaFolderActive===folder)mediaFolderRender();toast('Удалено файлов: '+ids.length)}catch(error){toast(error.message)}finally{busy=false;paint()}
}
document.addEventListener('click',e=>{
 const button=e.target.closest('button');if(!button)return;
 if(button.hasAttribute('data-album-select-all')){e.stopImmediatePropagation();const ids=[...document.querySelectorAll('#notes-media-grid [data-media-preview]')].map(b=>b.dataset.mediaPreview);selected=selected.size===ids.length?new Set():new Set(ids);paint();return}
 if(button.hasAttribute('data-album-delete')){e.stopImmediatePropagation();removeSelection();return}
 if(button.hasAttribute('data-album-cancel')){e.stopImmediatePropagation();clear();return}
 if(button.matches('#notes-media-grid [data-media-preview]')&&(active()||performance.now()<suppressUntil)){e.preventDefault();e.stopImmediatePropagation();if(performance.now()>=suppressUntil)choose(button.dataset.mediaPreview);return}
 if(active()&&button.closest('#notes-media-grid .notes-media-info')){e.preventDefault();e.stopImmediatePropagation();return}
 if(button.hasAttribute('data-media-back')||button.hasAttribute('data-tab'))clear();
},true);
const oldBack=window.handlePlannerBack;window.handlePlannerBack=function(){if(active()&&!$('notes-media-viewer')){if(!busy)clear();return true}return oldBack?.()||false};
new MutationObserver(records=>{if(!records.some(r=>r.target===$('view')||r.target.id==='notes-media-grid'))return;if(selectionFolder!==null&&!active()){selected.clear();selectionFolder=null}paint()}).observe($('view'),{childList:true,subtree:true});
})();
