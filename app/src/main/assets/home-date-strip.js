/* A native horizontal scroller; taps keep the normal day navigation. */
(()=>{
'use strict';
const originalHome=renderVioletTimelineDay;
function markup(date){const count=occupied(date)+dayEvents(date).length,weekend=dateObj(date).getDay()%6===0;return '<button type="button" data-date="'+date+'" class="'+(weekend?'ice-sunday':'')+'" '+(date===selected?'aria-current="date"':'')+' aria-label="'+esc(fmt(date,{weekday:'long',day:'numeric',month:'long',year:'numeric'}))+'"><span>'+fmt(date,{weekday:'short'})+'</span><strong>'+dateObj(date).getDate()+'</strong><i class="'+(count?'has-plans':'')+'"></i></button>'}
renderVioletTimelineDay=function(){
 originalHome();const nav=document.querySelector('.ice-week');if(!nav)return;
 nav.classList.add('home-date-strip');nav.setAttribute('aria-label','Дни: листай влево и вправо');
 let first=addDays(monday(selected),-14),last=addDays(first,34),extending=false;
 nav.innerHTML=Array.from({length:35},(_,i)=>markup(addDays(first,i))).join('');
 nav.scrollLeft=nav.clientWidth*2;
 nav.addEventListener('scroll',()=>{
  if(extending||!nav.isConnected)return;extending=true;
  try{
   const width=nav.clientWidth/7;
   if(nav.scrollLeft<width*7&&first>'1900-02-01'){
    const old=nav.scrollLeft;const next=addDays(first,-14);
    nav.insertAdjacentHTML('afterbegin',Array.from({length:14},(_,i)=>markup(addDays(next,i))).join(''));first=next;nav.scrollLeft=old+width*14;
   }
   if(nav.scrollWidth-nav.scrollLeft-nav.clientWidth<width*7&&last<'2199-12-01'){
    nav.insertAdjacentHTML('beforeend',Array.from({length:14},(_,i)=>markup(addDays(last,i+1))).join(''));last=addDays(last,14);
   }
  }finally{extending=false}
 },{passive:true});
};
})();
