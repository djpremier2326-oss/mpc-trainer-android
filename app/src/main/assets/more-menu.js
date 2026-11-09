/* All sections remain reachable. Only the bottom-panel duplicates are filtered. */
(()=>{
'use strict';
const defaults=['wishes','workout','calculator','photos','notes','savings','day','week','calendar','events','settings'];
const icons={wishes:'speaker',workout:'workout',calculator:'calculator',betting:'calculator',notes:'notes',savings:'savings',day:'today',week:'week',calendar:'calendar',settings:'gear'};
const titles={...MAIN_SECTIONS,calculator:'Калькулятор',betting:'Работа',settings:'Настройки'};
const list=$('ice-tools-list'),dialog=$('ice-menu-dialog');
function order(){const saved=state.prefs.moreMenuOrder;return [...new Set([...(Array.isArray(saved)?saved.filter(id=>defaults.includes(id)):[]),...defaults])]}
function visible(){const bottom=state.prefs.mainMenu||[];return order().filter(id=>!bottom.includes(id))}
function draw(){list.innerHTML=visible().map(id=>'<button type="button" data-more-menu="'+id+'" aria-label="'+titles[id]+'"><span>'+(id==='photos'?photosIcon():id==='events'?uiIcon('bell'):iceIcon(icons[id]))+'</span><span><strong>'+titles[id]+'</strong></span><b aria-hidden="true">≡</b></button>').join('')}
openIceMenu=function(){if(!$('more-menu-hint')){const hint=document.createElement('p');hint.id='more-menu-hint';hint.className='dialog-note';hint.textContent='Зажми раздел и перетащи, чтобы изменить порядок.';list.before(hint)}draw();if(!dialog.open)dialog.showModal()};
let pending=null,drag=null,blockClick=false;
function stopPending(){if(pending)clearTimeout(pending.timer);pending=null}
function start(p){if(!p.row.isConnected||!dialog.open)return;blockClick=true;const b=p.row.getBoundingClientRect(),ghost=p.row.cloneNode(true);ghost.className='more-menu-drag-ghost';ghost.setAttribute('aria-hidden','true');ghost.style.width=b.width+'px';ghost.style.left=b.left+'px';ghost.style.top=b.top+'px';dialog.append(ghost);drag={...p,ghost,initial:[...list.children].map(b=>b.dataset.moreMenu),top:b.top};p.row.classList.add('more-menu-drag-placeholder');p.row.setPointerCapture?.(p.id);navigator.vibrate?.(15)}
function finish(save){stopPending();const d=drag;if(!d)return;drag=null;d.ghost.remove();d.row.classList.remove('more-menu-drag-placeholder');if(d.row.hasPointerCapture?.(d.id))d.row.releasePointerCapture(d.id);
 if(save){const visibleOrder=[...list.children].map(b=>b.dataset.moreMenu),available=new Set(visibleOrder),previous=state.prefs.moreMenuOrder;let i=0;state.prefs.moreMenuOrder=order().map(id=>available.has(id)?visibleOrder[i++]:id);if(!persist()){state.prefs.moreMenuOrder=previous;draw();toast('Не удалось сохранить порядок меню')}}else draw();
}
list.addEventListener('pointerdown',e=>{if(!e.isPrimary||e.button!==0)return;stopPending();blockClick=false;const row=e.target.closest('[data-more-menu]');if(!row)return;const p={row,id:e.pointerId,x:e.clientX,y:e.clientY};p.timer=setTimeout(()=>{if(pending===p){pending=null;start(p)}},450);pending=p},{passive:true});
document.addEventListener('pointermove',e=>{
 if(pending&&pending.id===e.pointerId&&Math.hypot(e.clientX-pending.x,e.clientY-pending.y)>10)stopPending();
 const d=drag;if(!d||e.pointerId!==d.id)return;e.preventDefault();d.ghost.style.transform='translateY('+(e.clientY-d.y)+'px)';
 const others=[...list.children].filter(row=>row!==d.row),before=others.find(row=>e.clientY<row.getBoundingClientRect().top+row.getBoundingClientRect().height/2);list.insertBefore(d.row,before||null);
 const box=dialog.getBoundingClientRect();if(e.clientY<box.top+65)dialog.scrollTop-=12;else if(e.clientY>box.bottom-45)dialog.scrollTop+=12;
},{passive:false});
// Prevent native touch scrolling only after a deliberate hold activates dragging.
list.addEventListener('touchmove',e=>{if(drag)e.preventDefault()},{passive:false});
document.addEventListener('pointerup',e=>{if(drag&&drag.id===e.pointerId)finish(true);else stopPending()},{passive:true});
document.addEventListener('pointercancel',()=>{stopPending();finish(false)},{passive:true});
dialog.addEventListener('close',()=>{stopPending();finish(false)});dialog.addEventListener('scroll',()=>{if(!drag)stopPending()},{passive:true});
document.addEventListener('click',e=>{const b=e.target.closest('[data-more-menu]');if(!b)return;e.preventDefault();e.stopImmediatePropagation();if(blockClick&&e.detail!==0){blockClick=false;return}const id=b.dataset.moreMenu;dialog.close();if(id==='settings')toggleSettings();else if(id==='calculator')toggleNoteCalculator(true,'full');else{if(id==='workout')workoutActive=null;navigate(id==='day'?iso():selected,id)}},true);
list.addEventListener('keydown',e=>{const row=e.target.closest('[data-more-menu]');if(!row||!e.altKey||!['ArrowUp','ArrowDown'].includes(e.key))return;e.preventDefault();const other=e.key==='ArrowUp'?row.previousElementSibling:row.nextElementSibling;if(!other)return;list.insertBefore(row,e.key==='ArrowUp'?other:other.nextElementSibling);const sequence=[...list.children].map(b=>b.dataset.moreMenu),set=new Set(sequence),old=state.prefs.moreMenuOrder;let i=0;state.prefs.moreMenuOrder=order().map(id=>set.has(id)?sequence[i++]:id);if(!persist()){state.prefs.moreMenuOrder=old;draw()}else row.focus()});
})();
