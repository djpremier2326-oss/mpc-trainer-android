/* A circle toggles a normally hidden album; this is a display gesture, not encryption. */
let photosHiddenVisible=false;
function photosRecordVisible(record){return photosHiddenVisible||!mediaFolderList().some(folder=>folder.id===record.folder&&folder.hidden===true)}
function mediaVisibleFolders(){return mediaFolderList().filter(folder=>photosHiddenVisible||folder.hidden!==true)}
(()=>{
'use strict';
function hiddenFolder(){return mediaFolderList().find(folder=>folder.hidden===true)}
function toggle(){
 if(!photosHiddenVisible&&!hiddenFolder()){
  const folder={id:'mf_hidden_'+Date.now().toString(36),name:'Скрытая',hidden:true};mediaFolderList().push(folder);
  if(!persist()){state.mediaFolders=mediaFolderList().filter(item=>item!==folder);toast('Не удалось сохранить папку');return}
 }
 photosHiddenVisible=!photosHiddenVisible;
 if(mediaFolderList().some(folder=>folder.id===mediaFolderActive&&folder.hidden===true))mediaFolderActive=null;
 const view=$('view'),overflow=view.style.overflowY;view.style.overflowY='hidden';renderPhotos();requestAnimationFrame(()=>{view.style.overflowY=overflow});document.dispatchEvent(new Event('planner-media-changed'));navigator.vibrate?.(25);
}
function circle(points,duration){
 if(points.length<14||duration<250||duration>3000)return false;
 const xs=points.map(p=>p.x),ys=points.map(p=>p.y),w=Math.max(...xs)-Math.min(...xs),h=Math.max(...ys)-Math.min(...ys);
 if(w<60||h<60||w>420||h>420||w/h<.55||w/h>1.8)return false;
 const cx=(Math.max(...xs)+Math.min(...xs))/2,cy=(Math.max(...ys)+Math.min(...ys))/2,rx=w/2,ry=h/2;
 if(Math.hypot(points[0].x-points.at(-1).x,points[0].y-points.at(-1).y)>Math.min(w,h)*.4)return false;
 let angle=0,length=0,previous=Math.atan2((points[0].y-cy)/ry,(points[0].x-cx)/rx),radial=0;
 for(let i=0;i<points.length;i++){
  const p=points[i],r=Math.hypot((p.x-cx)/rx,(p.y-cy)/ry);radial+=Math.abs(r-1);
  if(i){const current=Math.atan2((p.y-cy)/ry,(p.x-cx)/rx);let delta=current-previous;if(delta>Math.PI)delta-=2*Math.PI;if(delta< -Math.PI)delta+=2*Math.PI;angle+=delta;previous=current;length+=Math.hypot(p.x-points[i-1].x,p.y-points[i-1].y)}
 }
 return Math.abs(angle)>5.1&&Math.abs(angle)<7.7&&radial/points.length<.3&&length>Math.max(w,h)*2.3;
}
let stroke=null,suppressUntil=0;
function eligible(){return tab==='photos'&&(!mediaFolderActive||mediaFolderList().some(f=>f.id===mediaFolderActive&&f.hidden===true))&&!document.querySelector('dialog[open],#notes-media-viewer,#notes-media-sheet')}
document.addEventListener('touchstart',e=>{stroke=null;suppressUntil=0;if(!eligible()||!e.target.closest('#view')||e.touches.length!==1||e.target.closest('.notes-tools,.notes-media-toolbar,input,textarea'))return;const t=e.touches[0];stroke={id:t.identifier,start:performance.now(),points:[{x:t.clientX,y:t.clientY}]};},{passive:true});
document.addEventListener('touchmove',e=>{if(!stroke||!eligible()||e.touches.length!==1){stroke=null;return}const t=[...e.touches].find(t=>t.identifier===stroke.id);if(!t)return;const p=stroke.points,last=p.at(-1);if(Math.hypot(t.clientX-last.x,t.clientY-last.y)>=3)p.push({x:t.clientX,y:t.clientY});const first=p[0];if(Math.abs(t.clientX-first.x)>Math.abs(t.clientY-first.y)*1.2)stroke.captured=true;if(e.cancelable&&stroke.captured)e.preventDefault();},{passive:false});
document.addEventListener('touchend',e=>{const s=stroke;stroke=null;if(!s||!eligible())return;const t=[...e.changedTouches].find(t=>t.identifier===s.id);if(t)s.points.push({x:t.clientX,y:t.clientY});if(circle(s.points,performance.now()-s.start)){suppressUntil=performance.now()+600;toggle()}},{passive:false});
document.addEventListener('touchcancel',()=>{stroke=null},{passive:true});
document.addEventListener('click',e=>{if(performance.now()<suppressUntil&&e.target.closest('[data-media-folder-open],[data-media-preview]')){e.preventDefault();e.stopImmediatePropagation()}},true);
const original=render;render=function(){if(tab!=='photos'&&photosHiddenVisible){photosHiddenVisible=false;const folder=hiddenFolder();if(mediaFolderActive===folder?.id)mediaFolderActive=null;document.dispatchEvent(new Event('planner-media-changed'))}original()};
})();
