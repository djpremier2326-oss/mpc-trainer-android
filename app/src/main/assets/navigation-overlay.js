// Measure the real navigation, including safe-area padding and compact screens.
// The scroll area fills the window; its trailing padding keeps the last item reachable.
(()=>{
 const nav=document.querySelector('main>.tabs');if(!nav)return;
 let height=-1;
 const measure=()=>{
  const next=Math.ceil(nav.getBoundingClientRect().height);
  if(next===height)return;
  height=next;document.documentElement.style.setProperty('--planner-nav-height',height+'px');
 };
 measure();
 new ResizeObserver(measure).observe(nav);
 window.addEventListener('resize',measure,{passive:true});
})();
