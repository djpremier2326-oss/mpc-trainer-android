/* Album covers use the existing Notes media records; originals are never modified. */
(()=>{
'use strict';
let generation=0,coverURLs=[];
function dispose(){generation++;coverURLs.forEach(url=>URL.revokeObjectURL(url));coverURLs=[]}
const placeholder='<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true"><path d="M3 6h7l2 2h9v12H3zM3 6V4h7l2 2"/></svg>';
mediaFolderCards=function(){const folders=mediaVisibleFolders();if(!folders.length)return '';return '<div class="notes-media-folders"><div class="notes-media-heading">Мои папки</div><div class="notes-media-folder-grid">'+folders.map(folder=>'<div class="notes-media-folder notes-album" data-album-id="'+esc(folder.id)+'"><button type="button" class="notes-media-folder-open" data-media-folder-open="'+esc(folder.id)+'" aria-label="Открыть папку '+esc(folder.name)+'" title="'+esc(folder.name)+'"><span class="notes-album-cover">'+placeholder+'</span><strong class="notes-album-name">'+esc(folder.name)+'</strong><small class="notes-album-count">…</small></button><button type="button" class="notes-media-folder-menu" data-media-folder-menu="'+esc(folder.id)+'" aria-label="Действия с папкой '+esc(folder.name)+'">⋯</button></div>').join('')+'</div></div>'};
async function covers(){
 const grid=document.querySelector('.notes-media-folder-grid');if(!grid)return;const token=generation;
 try{
  const records=await mediaQuery();if(token!==generation||!grid.isConnected)return;
  const groups=new Map();for(const record of records){if(!groups.has(record.folder))groups.set(record.folder,[]);groups.get(record.folder).push(record)}
  grid.querySelectorAll('.notes-album').forEach(card=>{
   const items=groups.get(card.dataset.albumId)||[],count=card.querySelector('.notes-album-count');count.textContent=String(items.length);
   const item=[...items].sort((a,b)=>(b.created||0)-(a.created||0)).find(r=>r.blob&&(r.type?.startsWith('image/')||r.type?.startsWith('video/')));
   if(!item)return;const url=URL.createObjectURL(item.blob);coverURLs.push(url);const cover=card.querySelector('.notes-album-cover'),video=item.type.startsWith('video/'),media=document.createElement(video?'video':'img');cover.querySelector('svg')?.remove();
   if(video){media.muted=true;media.playsInline=true;media.preload='metadata';media.addEventListener('loadedmetadata',()=>{if(Number.isFinite(media.duration)&&media.duration>0)media.currentTime=Math.min(.1,media.duration/2)});const play=document.createElement('span');play.className='notes-album-video';play.textContent='▶';cover.append(play)}else{media.alt='';media.loading='lazy';media.decoding='async'}
   media.addEventListener('error',()=>{media.remove()},{once:true});media.src=url;cover.prepend(media);
  });
 }catch(error){if(grid.isConnected)grid.querySelectorAll('.notes-album-count').forEach(label=>{label.textContent='Не удалось загрузить'})}
}
const originalNotes=renderPhotos;renderPhotos=function(){dispose();originalNotes();covers()};
// Release decoded covers when navigating away, not only when Notes rerenders.
const originalRender=render;render=function(){if(tab!=='photos')dispose();originalRender()};
})();
