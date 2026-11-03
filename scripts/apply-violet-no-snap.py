from pathlib import Path
p=Path("app/src/main/assets/index.html")
s=p.read_text(encoding="utf-8")
old_down="violetSwipeState={card,wrap:card.closest('.violet-event-wrap'),x:e.clientX,y:e.clientY,moved:false,locked:null,base:card.closest('.violet-event-wrap')?.classList.contains('revealed')?-168:0,lastX:0};card.style.transition='none';card.setPointerCapture?.(e.pointerId)"
new_down="violetSwipeState={card,wrap:card.closest('.violet-event-wrap'),x:e.clientX,y:e.clientY,moved:false,locked:null,base:Number(card.closest('.violet-event-wrap')?.dataset.violetOffset||0),lastX:0,id:e.pointerId};card.style.transition='none'"
if old_down not in s:raise SystemExit("drag down marker missing")
s=s.replace(old_down,new_down,1)
old_move="if(Math.abs(dy)>Math.abs(dx)*1.25){s.locked='vertical';s.card.style.transition='';s.card.style.transform='';return}"
new_move="if(Math.abs(dy)>Math.abs(dx)*1.25){s.locked='vertical';return}"
if old_move not in s:raise SystemExit("vertical move marker missing")
s=s.replace(old_move,new_move,1)
old_up="document.addEventListener('pointerup',e=>{const s=violetSwipeState;if(!s)return;const dx=e.clientX-s.x;s.card.style.transition='';if(s.locked!=='horizontal'||!s.moved){s.card.style.transform='';violetSwipeState=null;return}violetIgnoreClick=true;const revealed=s.wrap.classList.contains('revealed');const shouldOpen=revealed?dx<-80:dx<-96;const shouldClose=revealed&&dx>80;if(shouldOpen){violetCloseSwipes(s.wrap);s.wrap.classList.add('revealed')}else if(shouldClose){s.wrap.classList.remove('revealed')}s.card.style.transform='';violetSwipeState=null});"
new_up="document.addEventListener('pointerup',e=>{const s=violetSwipeState;if(!s||s.id!==e.pointerId)return;if(s.locked==='horizontal'&&s.moved){violetIgnoreClick=true;s.wrap.dataset.violetOffset=String(s.lastX);s.card.style.transition='none';s.card.style.transform='translateX('+s.lastX+'px)'}violetSwipeState=null});"
if old_up not in s:raise SystemExit("release marker missing")
s=s.replace(old_up,new_up,1)
old_close="function violetCloseSwipes(except=null){document.querySelectorAll('.violet-event-wrap.revealed').forEach(el=>{if(el!==except)el.classList.remove('revealed')})}"
new_close="function violetCloseSwipes(except=null){document.querySelectorAll('.violet-event-wrap').forEach(el=>{if(el===except)return;el.classList.remove('revealed');el.dataset.violetOffset='0';const card=el.querySelector('.violet-event-card');if(card){card.style.transition='none';card.style.transform='translateX(0px)'}})}"
if old_close not in s:raise SystemExit("close swipe marker missing")
s=s.replace(old_close,new_close,1)
# Without CSS transition, not even a 180ms finish animation can occur.
style="<style id=\"violet-no-snap\">body[data-design=ice][data-variant=violet] .violet-event-card{transition:none!important}body[data-design=ice][data-variant=violet] .violet-event-wrap.revealed .violet-event-card{transform:none}</style>"
if "</head>" not in s:raise SystemExit("head missing")
s=s.replace("</head>",style+"</head>",1)
p.write_text(s,encoding="utf-8")
print("Violet swipe uses exact finger location; no snap/auto-completion")
