from pathlib import Path
p=Path('app/src/main/assets/index.html')
s=p.read_text(encoding='utf-8')
old_down="violetSwipeState={card,wrap:card.closest('.violet-event-wrap'),x:e.clientX,y:e.clientY,moved:false,locked:null,base:card.closest('.violet-event-wrap')?.classList.contains('revealed')?-168:0,lastX:0};card.style.transition='none';card.setPointerCapture?.(e.pointerId)"
new_down="const wrap=card.closest('.violet-event-wrap');if(!e.isPrimary)return;violetSwipeState={card,wrap,id:e.pointerId,x:e.clientX,y:e.clientY,base:wrap.classList.contains('revealed')?-168:0,offset:0,locked:null,moved:false}"
if s.count(old_down)!=1:raise SystemExit('pointerdown marker missing')
s=s.replace(old_down,new_down,1)
prefix="document.addEventListener('pointermove',e=>{const s=violetSwipeState;"
a=s.index(prefix)
b=s.index(" ,{passive:false});",a)+len(" ,{passive:false});")
new_move="document.addEventListener('pointermove',e=>{const s=violetSwipeState;if(!s||s.id!==e.pointerId)return;const dx=e.clientX-s.x,dy=e.clientY-s.y;if(!s.locked){if(Math.abs(dx)<12&&Math.abs(dy)<12)return;if(Math.abs(dy)>Math.abs(dx)*1.15){s.locked='vertical';return}if(Math.abs(dx)>Math.abs(dy)*1.35){s.locked='horizontal';s.card.style.transition='none'}else return}if(s.locked!=='horizontal')return;s.moved=true;s.offset=Math.max(-168,Math.min(0,s.base+dx));s.card.style.transform='translateX('+s.offset+'px)';e.preventDefault()} ,{passive:false});"
s=s[:a]+new_move+s[b:]
prefix="document.addEventListener('pointerup',e=>{const s=violetSwipeState;"
a=s.index(prefix)
b=s.index("violetSwipeState=null});",a)+len("violetSwipeState=null});")
new_up="document.addEventListener('pointerup',e=>{const s=violetSwipeState;if(!s||s.id!==e.pointerId)return;violetSwipeState=null;if(s.locked!=='horizontal'||!s.moved)return;violetIgnoreClick=true;setTimeout(()=>{violetIgnoreClick=false},120);const open=s.offset<=-100;if(open)violetCloseSwipes(s.wrap);s.wrap.classList.toggle('revealed',open);s.card.style.transition='transform .17s ease-out';s.card.style.transform='translateX('+(open?-168:0)+'px)';setTimeout(()=>{if(s.card.isConnected)s.card.style.transition=''},220);});"
s=s[:a]+new_up+s[b:]
s=s.replace(new_up,"document.addEventListener('pointercancel',e=>{const s=violetSwipeState;if(!s||s.id!==e.pointerId)return;violetSwipeState=null;s.card.style.transition='';s.card.style.transform='';});\n"+new_up,1)
p.write_text(s,encoding='utf-8')
print('Standard threshold swipe installed')
