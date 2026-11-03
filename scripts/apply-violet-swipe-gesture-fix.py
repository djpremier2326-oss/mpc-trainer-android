from pathlib import Path

path = Path('app/src/main/assets/index.html')
text = path.read_text(encoding='utf-8')
original = text

old_down = "document.addEventListener('pointerdown',e=>{if(state.prefs?.variant!=='violet'||tab!=='day')return;const del=e.target.closest('[data-violet-delete]');if(del){e.preventDefault();e.stopPropagation();violetSwipeState=null;violetIgnoreClick=false;violetDeleteTask(Number(del.dataset.violetDelete));return}const card=e.target.closest('.violet-event-card');if(!card)return;violetSwipeState={card,wrap:card.closest('.violet-event-wrap'),x:e.clientX,y:e.clientY,moved:false};card.setPointerCapture?.(e.pointerId)});"
new_down = "document.addEventListener('pointerdown',e=>{if(state.prefs?.variant!=='violet'||tab!=='day')return;const del=e.target.closest('[data-violet-delete]');if(del){e.preventDefault();e.stopPropagation();violetSwipeState=null;violetIgnoreClick=false;violetDeleteTask(Number(del.dataset.violetDelete));return}const card=e.target.closest('.violet-event-card');if(!card)return;violetSwipeState={card,wrap:card.closest('.violet-event-wrap'),x:e.clientX,y:e.clientY,moved:false,locked:null,base:card.closest('.violet-event-wrap')?.classList.contains('revealed')?-168:0,lastX:0};card.style.transition='none';card.setPointerCapture?.(e.pointerId)});"
if old_down not in text:
    raise SystemExit('Swipe fix: pointerdown marker not found')
text = text.replace(old_down, new_down, 1)

old_move = "document.addEventListener('pointermove',e=>{const s=violetSwipeState;if(!s)return;const dx=e.clientX-s.x,dy=e.clientY-s.y;if(Math.abs(dx)>8&&Math.abs(dx)>Math.abs(dy)){s.moved=true;const base=s.wrap.classList.contains('revealed')?-168:0;const x=Math.max(-168,Math.min(0,base+dx));s.card.style.transform=`translateX(${x}px)`;e.preventDefault()}} ,{passive:false});"
new_move = "document.addEventListener('pointermove',e=>{const s=violetSwipeState;if(!s)return;const dx=e.clientX-s.x,dy=e.clientY-s.y;if(!s.locked){if(Math.abs(dx)<18&&Math.abs(dy)<18)return;if(Math.abs(dy)>Math.abs(dx)*1.25){s.locked='vertical';s.card.style.transition='';s.card.style.transform='';return}if(Math.abs(dx)>Math.abs(dy)*1.25)s.locked='horizontal';else return}if(s.locked!=='horizontal')return;s.moved=true;s.lastX=Math.max(-168,Math.min(0,s.base+dx));s.card.style.transform=`translateX(${s.lastX}px)`;e.preventDefault()} ,{passive:false});"
if old_move not in text:
    raise SystemExit('Swipe fix: pointermove marker not found')
text = text.replace(old_move, new_move, 1)

old_up = "document.addEventListener('pointerup',e=>{const s=violetSwipeState;if(!s)return;const dx=e.clientX-s.x;s.card.style.transform='';if(s.moved){violetIgnoreClick=true;if((s.wrap.classList.contains('revealed')&&dx>45)||(!s.wrap.classList.contains('revealed')&&dx>-45))s.wrap.classList.remove('revealed');else{violetCloseSwipes(s.wrap);s.wrap.classList.add('revealed')}}violetSwipeState=null});"
new_up = "document.addEventListener('pointerup',e=>{const s=violetSwipeState;if(!s)return;const dx=e.clientX-s.x;s.card.style.transition='';if(s.locked!=='horizontal'||!s.moved){s.card.style.transform='';violetSwipeState=null;return}violetIgnoreClick=true;const revealed=s.wrap.classList.contains('revealed');const shouldOpen=revealed?dx<-80:dx<-96;const shouldClose=revealed&&dx>80;if(shouldOpen){violetCloseSwipes(s.wrap);s.wrap.classList.add('revealed')}else if(shouldClose){s.wrap.classList.remove('revealed')}s.card.style.transform='';violetSwipeState=null});"
if old_up not in text:
    raise SystemExit('Swipe fix: pointerup marker not found')
text = text.replace(old_up, new_up, 1)

if text == original:
    raise SystemExit('Swipe fix made no changes')
path.write_text(text, encoding='utf-8')
print('Violet swipe now follows the finger, ignores vertical scrolling, and requires a deliberate horizontal drag')
