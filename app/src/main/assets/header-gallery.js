/* Swiping only the artwork changes the header; date navigation remains independent. */
(()=>{
'use strict';
const originalPictures=[{id:'swoosh',title:'Свуш'},...(window.plannerHeaderImages||[]).filter(item=>item&&typeof item.id==='string'&&typeof item.title==='string'&&typeof item.src==='string')];
function aurora(){return ['aurora','indigo'].includes(state.prefs.colorTheme)&&state.prefs.variant==='violet'}
function pictureList(){return aurora()?(window.plannerAuroraHeaderImages||[]):originalPictures}
function pictureKey(){return aurora()?'auroraHeaderArtwork':'headerArtwork'}
const originalHome=renderVioletTimelineDay;
let cachedPath=null,cachedImage=null;
function localArtwork(path){if(!window.AndroidFiles?.headerArtwork)return path;if(cachedPath===path&&cachedImage)return cachedImage;try{const inline=AndroidFiles.headerArtwork(path);if(inline){cachedPath=path;cachedImage=inline;return inline}}catch(error){}return path}
function artwork(hero){const pictures=pictureList(),key=pictureKey(),id=state.prefs[key]||(aurora()?pictures[0]?.id:'swoosh'),picture=pictures.find(item=>item.id===id)||pictures[0];if(!picture)return;const image=hero.querySelector('.header-artwork-image');hero.dataset.headerArtwork=picture.id==='swoosh'?'swoosh':'image';image.hidden=picture.id==='swoosh';image.dataset.edgeFade=aurora()?'none':picture.edgeFade||'none';image.dataset.artworkId=picture.id;if(picture.src){image.src=localArtwork(picture.src);image.alt=picture.title}const hit=hero.querySelector('.header-artwork-swipe');hit.setAttribute('aria-label',picture.title+'. Листай влево или вправо, чтобы сменить изображение')}
function change(hero,direction){const pictures=pictureList(),key=pictureKey(),old=state.prefs[key],index=Math.max(0,pictures.findIndex(item=>item.id===(old||(aurora()?pictures[0]?.id:'swoosh')))),next=pictures[(index+direction+pictures.length)%pictures.length];if(pictures.length<2)return;state.prefs[key]=next.id;if(!persist()){if(old===undefined)delete state.prefs[key];else state.prefs[key]=old;toast('Не удалось сохранить изображение');return}artwork(hero)}
renderVioletTimelineDay=function(){originalHome();if(state.prefs.variant!=='violet')return;const hero=document.querySelector('.ice-tribute');if(!hero)return;const image=document.createElement('img');image.className='header-artwork-image';image.draggable=false;hero.prepend(image);const hit=document.createElement('div');hit.className='header-artwork-swipe';hit.tabIndex=0;hit.setAttribute('role','group');hero.append(hit);artwork(hero);let down=null;
 hit.addEventListener('pointerdown',e=>{if(e.isPrimary&&e.button===0)down={id:e.pointerId,x:e.clientX,y:e.clientY,time:performance.now()};},{passive:true});
 hit.addEventListener('pointermove',e=>{if(down&&e.pointerId===down.id&&Math.abs(e.clientX-down.x)>Math.abs(e.clientY-down.y)+10)e.preventDefault()},{passive:false});
 hit.addEventListener('pointercancel',()=>{down=null},{passive:true});
 hit.addEventListener('pointerup',e=>{const start=down;down=null;if(!start||start.id!==e.pointerId||!hero.isConnected||performance.now()-start.time>1500)return;const dx=e.clientX-start.x,dy=e.clientY-start.y;if(Math.abs(dx)>=45&&Math.abs(dx)>Math.abs(dy)*1.4)change(hero,dx<0?1:-1)},{passive:true});
 hit.addEventListener('keydown',e=>{if(e.key==='ArrowLeft'||e.key==='ArrowRight'){e.preventDefault();change(hero,e.key==='ArrowRight'?1:-1)}});
};
})();
