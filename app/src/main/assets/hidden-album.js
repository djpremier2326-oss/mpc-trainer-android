/* Three quick taps toggle a normally hidden album; this is a display gesture, not encryption. */
let photosHiddenVisible=false;
function photosRecordVisible(record){return photosHiddenVisible||!mediaFolderList().some(folder=>folder.id===record.folder&&folder.hidden===true)}
function mediaVisibleFolders(){return mediaFolderList().filter(folder=>photosHiddenVisible||folder.hidden!==true).sort((a,b)=>Number(b.hidden===true)-Number(a.hidden===true))}
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
 const view=$('view'),overflow=view.style.overflowY;view.style.overflowY='hidden';renderPhotos();if(photosHiddenVisible)view.scrollTop=0;requestAnimationFrame(()=>{view.style.overflowY=overflow});document.dispatchEvent(new Event('planner-media-changed'));navigator.vibrate?.(25);
}
let down=null,taps=[];
function eligible(target){return tab==='photos'&&(!mediaFolderActive||mediaFolderList().some(f=>f.id===mediaFolderActive&&f.hidden===true))&&!document.querySelector('dialog[open],#notes-media-viewer,#notes-media-sheet')&&target?.closest('#view')&&!target.closest('button,input,textarea,select,a,[contenteditable]')}
document.addEventListener('pointerdown',e=>{down=null;if(!e.isPrimary||e.button!==0||!eligible(e.target)){taps=[];return}down={id:e.pointerId,x:e.clientX,y:e.clientY,time:performance.now()}},{passive:true});
document.addEventListener('pointermove',e=>{if(down&&Math.hypot(e.clientX-down.x,e.clientY-down.y)>18){down=null;taps=[]}},{passive:true});
document.addEventListener('pointercancel',()=>{down=null;taps=[]},{passive:true});
document.addEventListener('pointerup',e=>{const start=down;down=null;if(!start||start.id!==e.pointerId||!eligible(e.target)||performance.now()-start.time>400)return;const now=performance.now(),last=taps[taps.length-1];if(last&&(now-last.time>500||Math.hypot(e.clientX-last.x,e.clientY-last.y)>48))taps=[];taps.push({x:e.clientX,y:e.clientY,time:now});if(taps.length===3){taps=[];toggle()}},{passive:true});
const original=render;render=function(){if(tab!=='photos'&&photosHiddenVisible){photosHiddenVisible=false;const folder=hiddenFolder();if(mediaFolderActive===folder?.id)mediaFolderActive=null;document.dispatchEvent(new Event('planner-media-changed'))}original()};
})();
