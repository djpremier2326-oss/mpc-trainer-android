/* UI location lives only in the Android process, never in backups or preferences. */
(()=>{
'use strict';
const bridge=window.AndroidFiles;let restoring=false,scrollTimer=null;
function current(){const folder=(state.mediaFolders||[]).find(f=>f.id===mediaFolderActive);return{tab,date:selected,exercise:workoutActive||'',note:activeNoteId||'',folder:folder&&!folder.hidden?folder.id:'',scroll:$('view')?.scrollTop||0,paper:document.querySelector('.ice-paper')?.scrollTop||0}}
function remember(){if(restoring||!bridge?.rememberRuntimePlace)return;try{bridge.rememberRuntimePlace(JSON.stringify(current()))}catch{}}
let cached=null;try{if(bridge?.runtimePlace)cached=JSON.parse(bridge.runtimePlace()||'null')}catch{}
const oldRender=render;render=function(){oldRender();remember()};
const oldWorkout=renderWorkout;renderWorkout=function(){oldWorkout();remember()};
window.onPlannerPause=remember;
document.addEventListener('visibilitychange',()=>{if(document.hidden)remember()});
document.addEventListener('scroll',()=>{clearTimeout(scrollTimer);scrollTimer=setTimeout(remember,80)},true);
if(cached&&['day','week','month','calendar','notes','photos','workout','savings','events','wishes','betting'].includes(cached.tab)){
 restoring=true;tab=cached.tab;if(validDate(cached.date))selected=cached.date;
 workoutActive=Object.hasOwn(WORKOUT_PLANS,cached.exercise)?cached.exercise:null;
 activeNoteId=(state.notes||[]).some(n=>n.id===cached.note)?cached.note:null;
 mediaFolderActive=(state.mediaFolders||[]).some(f=>f.id===cached.folder&&!f.hidden)?cached.folder:null;
 render();requestAnimationFrame(()=>{$('view').scrollTop=Math.max(0,Number(cached.scroll)||0);const paper=document.querySelector('.ice-paper');if(paper)paper.scrollTop=Math.max(0,Number(cached.paper)||0);restoring=false;remember()});
}else remember();
})();
