const {test,before,after}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');
const root=path.resolve('app/build/generated/plannerAssets');
let browser,server,origin;
before(async()=>{
 server=http.createServer((req,res)=>{
  const file=path.resolve(root,'.'+decodeURIComponent(req.url.split('?')[0]));
  if(!file.startsWith(root+path.sep)){res.writeHead(403).end();return}
  try{res.setHeader('Content-Type',({'.html':'text/html; charset=utf-8','.js':'text/javascript','.css':'text/css','.svg':'image/svg+xml'})[path.extname(file)]||'application/octet-stream');res.end(fs.readFileSync(file))}catch{res.writeHead(404).end()}
 });
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));origin=`http://127.0.0.1:${server.address().port}`;
 browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH||undefined,headless:true,args:['--no-sandbox']});
});
after(async()=>{await browser?.close();await new Promise(resolve=>server?.close(resolve))});
async function open(){
 const context=await browser.newContext({viewport:{width:393,height:851},isMobile:true,hasTouch:true,timezoneId:'Europe/Moscow'});
 const page=await context.newPage();page.setDefaultTimeout(8000);const errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.addInitScript(()=>{
  window.nativeColors=[];window.statusColors=[];
  window.AndroidFiles={friendSchedule:()=>false,statusBarColor:(color,dark)=>statusColors.push({color,dark}),themeColors:(bg,dark)=>nativeColors.push({bg,dark}),startupMetrics:()=>{},hideKeyboard:()=>{},syncReminders:()=>true,notificationStatus:()=>'{"supported":true,"notifications":true,"exact":true}'};
 });
 await page.goto(origin+'/index.html');await page.waitForFunction(()=>document.documentElement.dataset.appReady==='true');
 await page.evaluate(()=>window.onAndroidReady?.());assert.deepEqual(errors,[]);
 return {page,context,errors};
}
const palettes={ice:'#03192e',graphite:'#8d8d8d',blueberry:'#a5afbb',rhythm:'#f7f6f1',violet:'#171126'};
for(const [variant,bg] of Object.entries(palettes)){
 test(`saved ${variant} theme boots directly into its own ready page`,async()=>{
  const {page,context,errors}=await open();
  try{
   await page.evaluate(variant=>{state.prefs.variant=variant;applyTheme();persist();render()},variant);
   await page.reload();await page.waitForFunction(()=>document.documentElement.dataset.appReady==='true');
   const boot=await page.evaluate(()=>({startup:window.__plannerStartupVariant,variant:document.body.dataset.variant,view:document.body.dataset.view,colors:nativeColors,overlays:document.querySelectorAll('#notes-media-viewer,#notes-media-sheet').length,visible:getComputedStyle(document.querySelector('main')).visibility}));
   assert.equal(boot.startup,variant);assert.equal(boot.variant,variant);assert.equal(boot.view,'day');assert.equal(boot.visible,'visible');assert.equal(boot.overlays,0);
   assert.ok(boot.colors.length>0);assert.ok(boot.colors.every(c=>c.bg===bg),JSON.stringify(boot.colors));
   assert.deepEqual(errors,[]);
  }finally{await context.close()}
 });
}
test('text notes and selected appearance survive relaunch',async()=>{
 const {page,context,errors}=await open();
 try{
  await page.evaluate(()=>{state.prefs.variant='violet';applyTheme();navigate(selected,'notes')});
  await page.locator('#note-add').tap();await page.locator('#note-title').fill('Не потерять');await page.locator('#note-rich').fill('Проверка сохранённой заметки');await page.locator('#note-save').tap();
  await page.reload();await page.waitForFunction(()=>document.documentElement.dataset.appReady==='true');
  const saved=await page.evaluate(()=>({variant:state.prefs.variant,title:state.notes[0].title,text:state.notes[0].text}));
  assert.deepEqual(saved,{variant:'violet',title:'Не потерять',text:'Проверка сохранённой заметки'});assert.deepEqual(errors,[]);
 }finally{await context.close()}
});
test('resuming the page keeps its current editor and does not show media overlays',async()=>{
 const {page,context,errors}=await open();
 try{
  await page.evaluate(()=>navigate(selected,'notes'));await page.locator('#note-add').tap();await page.locator('#note-rich').fill('Открытая запись');
  await page.evaluate(()=>{window.savedEditor=document.getElementById('note-rich');window.onPlannerResume();window.onPlannerResume()});
  await page.waitForTimeout(100);
  assert.equal(await page.evaluate(()=>document.getElementById('note-rich')===window.savedEditor),true);
  assert.equal(await page.locator('#note-rich').innerText(),'Открытая запись');assert.equal(await page.locator('#notes-media-viewer').count(),0);assert.deepEqual(errors,[]);
 }finally{await context.close()}
});
test('latest media folder menus and full-screen photo viewer survive relaunch',async()=>{
 const {page,context,errors}=await open();
 try{
  await page.evaluate(()=>navigate(selected,'notes'));
  await page.locator('[data-media-new-folder]').tap();await page.locator('#media-folder-name').fill('Мои фото');await page.locator('[data-media-action="create-save"]').tap();
  await page.locator('[data-media-folder-menu]').tap();await page.locator('[data-media-action="folder-rename"]').tap();await page.locator('#media-folder-name').fill('Сохранённые фото');await page.locator('[data-media-action="folder-rename-save"]').tap();
  await page.locator('[data-media-folder-open]').tap();
  const original=await page.evaluate(()=>{const canvas=document.createElement('canvas');canvas.width=20;canvas.height=10;return canvas.toDataURL('image/png').split(',')[1]});
  const bytes=Buffer.from(original,'base64');
  await page.locator('#notes-media-picker').setInputFiles({name:'original.png',mimeType:'image/png',buffer:bytes});
  await page.locator('[data-media-preview]').waitFor({state:'visible'});await page.locator('[data-media-preview]').tap();
  await page.waitForFunction(()=>document.querySelector('.media-viewer-stage img')?.naturalWidth===20);
  await page.locator('[data-media-viewer-close]').tap();
  await page.reload();await page.waitForFunction(()=>document.documentElement.dataset.appReady==='true');await page.evaluate(()=>navigate(selected,'notes'));
  assert.match(await page.locator('[data-media-folder-open]').innerText(),/Сохранённые фото/);await page.locator('[data-media-folder-open]').tap();
  await page.locator('[data-media-preview]').waitFor({state:'visible'});await page.locator('[data-media-preview]').tap();await page.waitForFunction(()=>document.querySelector('.media-viewer-stage img')?.naturalWidth===20);
  const stored=await page.evaluate(async()=>{const item=(await mediaQuery(mediaFolderActive))[0];return {name:item.name,size:item.blob.size,type:item.type}});
  assert.deepEqual(stored,{name:'original.png',size:bytes.length,type:'image/png'});assert.deepEqual(errors,[]);
 }finally{await context.close()}
});
test('calendar top panel, shift cycles, colors, persistence and removal',async()=>{
 const {page,context,errors}=await open();
 try{
  await page.evaluate(()=>{state.prefs.variant='violet';applyTheme();state.iceMarks=state.iceMarks||{};state.iceMarks.days={'2026-11-09':'purple'};persist();navigate('2026-11-08','calendar')});
  assert.equal(await page.locator('.calendar-jump').isVisible(),false);
  assert.equal(await page.locator('.app-heading').isVisible(),false);
  await page.locator('#calendar-schedule-open').tap();
  await page.locator('[name=start]').fill('2026-11-08');
  await page.locator('[name=workColor]').fill('#12ab34');
  await page.locator('#calendar-schedule-form [type=submit]').tap();
  const label=d=>page.locator(`#cal-2026-11 .cal-cell[data-date="${d}"] .calendar-shift-label`).innerText();
  assert.equal(await label('2026-11-08'),'Смена');assert.equal(await label('2026-11-09'),'Смена');assert.equal(await page.evaluate(()=>dayColor('2026-11-09')),'purple');assert.notEqual(await page.locator('#cal-2026-11 [data-date="2026-11-09"]').evaluate(el=>getComputedStyle(el).backgroundColor),'rgb(18, 171, 52)');assert.equal(await label('2026-11-10'),'Вых.');assert.equal(await label('2026-11-07'),'Вых.');
  assert.equal(await page.locator('#cal-2026-11 [data-date="2026-11-08"]').evaluate(el=>getComputedStyle(el).backgroundColor),'rgb(18, 171, 52)');
  await page.reload();await page.waitForFunction(()=>document.documentElement.dataset.appReady==='true');await page.evaluate(()=>navigate('2026-11-08','calendar'));assert.equal(await label('2026-11-08'),'Смена');
  for(const mode of ['3/3','daynight','custom']){
   await page.locator('#calendar-schedule-open').tap();await page.locator('[name=pattern]').selectOption(mode);
   if(mode==='custom'){await page.locator('[name=work]').fill('4');await page.locator('[name=off]').fill('1')}
   await page.locator('#calendar-schedule-form [type=submit]').tap();
   assert.equal(await label('2026-11-09'),mode==='daynight'?'Ночь':'Смена');
   assert.equal(await label('2026-11-11'),mode==='custom'?'Смена':'Вых.');
  }
  await page.screenshot({path:'/workspace/.planner-startup-review/calendar-2.3.105.png'});
  await page.locator('#calendar-schedule-open').tap();await page.locator('#calendar-schedule-remove').tap();assert.equal(await page.locator('.calendar-shift-label').count(),0);assert.equal(await page.evaluate(()=>state.iceMarks.days['2026-11-09']),'purple');
  await page.evaluate(()=>navigate('2027-04-05','calendar'));await page.locator('#calendar-today').tap();assert.equal(await page.evaluate(()=>selected),await page.evaluate(()=>iso()));
  await page.evaluate(()=>navigate(selected,'day'));assert.equal(await page.locator('#calendar-top-tools').isVisible(),false);assert.deepEqual(errors,[]);
 }finally{await context.close()}
});

test('flat calendar controls and status bar follow page background across themes',async()=>{
 const {page,context,errors}=await open();
 try{
  for(const variant of Object.keys(palettes)){
   await page.evaluate(v=>{state.prefs.variant=v;applyTheme();navigate(selected,'calendar')},variant);
   const chrome=await page.evaluate(()=>({height:document.getElementById('calendar-top-tools').getBoundingClientRect().height,top:getComputedStyle(document.getElementById('calendar-top-tools')).backgroundColor,page:getComputedStyle(document.getElementById('view')).backgroundColor,status:statusColors.at(-1)}));
   assert.ok(chrome.height<=52,JSON.stringify(chrome));assert.equal(chrome.top,'rgba(0, 0, 0, 0)');assert.match(chrome.status.color,/^#[0-9a-f]{6}$/);if(variant==='violet')assert.equal(chrome.status.color,'#e9ddf7');
   await page.evaluate(()=>navigate(selected,'events'));assert.match(await page.evaluate(()=>statusColors.at(-1).color),/^#[0-9a-f]{6}$/);
  }
  assert.deepEqual(errors,[]);
 }finally{await context.close()}
});

test('home stays black; notes have flat controls with working search and actions',async()=>{
 const {page,context,errors}=await open();
 try{
  assert.equal(await page.evaluate(()=>statusColors.at(-1).color),'#000000');
  await page.evaluate(()=>{state.prefs.variant='violet';applyTheme();navigate(selected,'notes')});
  assert.equal(await page.locator('.app-heading').isVisible(),false);
  assert.equal(await page.locator('.notes-tools').evaluate(el=>getComputedStyle(el).backgroundColor),'rgba(0, 0, 0, 0)');
  assert.ok(await page.locator('.notes-tools').evaluate(el=>el.getBoundingClientRect().height)<=60);
  assert.equal(await page.evaluate(()=>statusColors.at(-1).color),'#e9ddf7');
  await page.locator('#note-add').tap();await page.locator('#note-title').fill('Проверка поиска');await page.locator('#note-rich').fill('Запись на чёрной панели');await page.locator('#note-save').tap();await page.locator('#notes-back').tap();
  await page.locator('#notes-search').fill('Нет такой записи');assert.equal(await page.locator('[data-note-open]').count(),0);
  await page.locator('#notes-search').fill('Проверка поиска');assert.equal(await page.locator('[data-note-open]').count(),1);
  await page.locator('#notes-search').fill('');assert.equal(await page.locator('[data-media-new-folder]').isVisible(),true);
  await page.screenshot({path:'/workspace/.planner-startup-review/notes-black-panel.png'});
  assert.deepEqual(errors,[]);
 }finally{await context.close()}
});

test('scrolling content extends behind rounded bottom navigation in every theme',async()=>{
 const {page,context,errors}=await open();
 try{
  for(const variant of Object.keys(palettes)){
   for(const section of ['day','calendar','notes']){
    await page.evaluate(({variant,section})=>{state.prefs.variant=variant;applyTheme();navigate(selected,section)},{variant,section});
    await page.waitForTimeout(50);
    const bounds=await page.evaluate(()=>{
     const nav=document.querySelector('main>.tabs'),view=document.getElementById('view'),n=nav.getBoundingClientRect(),v=view.getBoundingClientRect();
     return {navBottom:n.bottom,viewBottom:v.bottom,navPosition:getComputedStyle(nav).position,cornerTag:document.elementFromPoint(n.left+1,n.top+1)?.closest('.tabs')!==null};
    });
    assert.equal(bounds.navPosition,'absolute');assert.ok(bounds.viewBottom>=bounds.navBottom-1,JSON.stringify(bounds));assert.equal(bounds.cornerTag,false);
   }
  }
  await page.evaluate(()=>{state.prefs.variant='violet';applyTheme();navigate('2026-11-08','day')});
  await page.locator('.violet-event-paper').evaluate(el=>el.scrollTop=el.scrollHeight);
  const end=await page.evaluate(()=>{
   const nav=document.querySelector('.tabs').getBoundingClientRect(),paper=document.querySelector('.violet-event-paper'),track=document.querySelector('.violet-event-timeline').getBoundingClientRect();
   return {paperBottom:paper.getBoundingClientRect().bottom,navBottom:nav.bottom,trackBottom:track.bottom};
  });
  assert.ok(end.paperBottom>=end.navBottom-1);assert.ok(end.trackBottom>=end.navBottom-1);
  await page.screenshot({path:'/workspace/.planner-startup-review/bottom-corners-fixed.png'});
  assert.deepEqual(errors,[]);
 }finally{await context.close()}
});

test('workout uses the same compact header as calendar and notes; history and exercises work',async()=>{
 const {page,context,errors}=await open();
 try{
  for(const variant of Object.keys(palettes)){
   const heights=[];
   for(const section of ['calendar','notes','workout']){
    await page.evaluate(({variant,section})=>{state.prefs.variant=variant;applyTheme();navigate(selected,section)},{variant,section});
    heights.push(await page.locator(section==='calendar'?'#calendar-top-tools':section==='notes'?'.notes-tools':'.workout-heading').evaluate(el=>el.getBoundingClientRect().height));
   }
   assert.deepEqual(heights,[50,50,50]);
   assert.equal(await page.locator('.app-heading').isVisible(),false);
   assert.equal(await page.locator('.workout-heading').evaluate(el=>getComputedStyle(el).backgroundColor),'rgba(0, 0, 0, 0)');
   assert.match(await page.evaluate(()=>statusColors.at(-1).color),/^#[0-9a-f]{6}$/);
   assert.equal(await page.locator('.workout-heading h1').evaluate(el=>getComputedStyle(el).fontSize),'18px');
  }
  await page.locator('[data-workout-history]').tap();assert.equal(await page.locator('.workout-heading h1').innerText(),'История');await page.locator('[data-workout-back]').tap();
  await page.screenshot({path:'/workspace/.planner-startup-review/workout-compact.png'});
  await page.locator('[data-workout-open]').first().tap();assert.equal(await page.locator('.workout-detail').isVisible(),true);
  assert.deepEqual(errors,[]);
 }finally{await context.close()}
});

test('lavender status bar in all secondary sections; home and bottom navigation preserved',async()=>{
 const {page,context,errors}=await open();
 try{
  await page.evaluate(()=>{state.prefs.variant='violet';applyTheme();navigate(selected,'day')});
  const homeBefore=await page.screenshot();
  for(const section of ['calendar','notes','workout','events','savings','wishes','week','month']){
   await page.evaluate(section=>navigate(selected,section),section);
   await page.waitForTimeout(50);
   assert.deepEqual(await page.evaluate(()=>statusColors.at(-1)),{color:'#e9ddf7',dark:false},section);
   assert.equal(await page.locator('.tabs').evaluate(el=>getComputedStyle(el).backgroundColor),'rgb(0, 0, 0)');
  }
  await page.evaluate(()=>navigate(selected,'day'));
  assert.equal(await page.evaluate(()=>statusColors.at(-1).color),'#000000');
  await page.evaluate(()=>navigate(selected,'workout'));await page.screenshot({path:'/workspace/.planner-startup-review/workout-flat.png'});
  await page.evaluate(()=>navigate(selected,'notes'));await page.screenshot({path:'/workspace/.planner-startup-review/notes-flat.png'});
  await page.evaluate(()=>navigate(selected,'calendar'));await page.screenshot({path:'/workspace/.planner-startup-review/calendar-flat.png'});
  assert.deepEqual(errors,[]);
 }finally{await context.close()}
});

test('lavender-on-black header buttons and a smaller violet hero leave more timeline visible',async()=>{
 const {page,context,errors}=await open();
 try{
  await page.evaluate(()=>{state.prefs.variant='violet';applyTheme();navigate(selected,'calendar')});
  for(const section of ['calendar','notes','workout']){
   await page.evaluate(section=>navigate(selected,section),section);
   const buttons=page.locator(section==='calendar'?'#calendar-top-tools button':section==='notes'?'.notes-tools .btn':'.workout-heading button');
   for(const button of await buttons.all())assert.deepEqual(await button.evaluate(el=>({bg:getComputedStyle(el).backgroundColor,ink:getComputedStyle(el).color})),{bg:'rgb(0, 0, 0)',ink:'rgb(197, 154, 242)'});
  }
  await page.evaluate(()=>navigate(selected,'day'));
  assert.equal(await page.locator('.ice-hero').evaluate(el=>el.getBoundingClientRect().height),124);
  assert.ok(await page.locator('.violet-event-paper').evaluate(el=>el.getBoundingClientRect().height)>500);
  await page.screenshot({path:'/workspace/.planner-startup-review/compact-home-111.png'});assert.deepEqual(errors,[]);
 }finally{await context.close()}
});
test('double touch on empty timeline opens the same editor as plus; a single tap and drag do not',async()=>{
 const {page,context,errors}=await open();
 try{
  await page.evaluate(()=>{state.prefs.variant='violet';applyTheme();navigate('2026-11-08','day')});
  const paper=page.locator('.violet-event-paper'),box=await paper.boundingBox(),x=box.x+box.width*.65,y=box.y+90;
  await page.touchscreen.tap(x,y);assert.equal(await page.locator('#violet-task-editor').isVisible(),false);await page.waitForTimeout(400);
  await page.touchscreen.tap(x,y);await page.waitForTimeout(70);await page.touchscreen.tap(x,y);
  assert.equal(await page.locator('#violet-task-editor').isVisible(),true);
  const fromDouble=await page.locator('#violet-task-editor').innerText();await page.locator('#violet-editor-close').tap();await page.locator('#violet-add-task').tap();assert.equal(await page.locator('#violet-task-editor').innerText(),fromDouble);
  await page.locator('#violet-editor-close').tap();
  await page.evaluate(({x,y})=>{const el=document.elementFromPoint(x,y);el.dispatchEvent(new PointerEvent('pointerdown',{bubbles:true,pointerId:7,isPrimary:true,clientX:x,clientY:y}));el.dispatchEvent(new PointerEvent('pointermove',{bubbles:true,pointerId:7,isPrimary:true,clientX:x,clientY:y-60}));el.dispatchEvent(new PointerEvent('pointerup',{bubbles:true,pointerId:7,isPrimary:true,clientX:x,clientY:y-60}))},{x,y});
  assert.equal(await page.locator('#violet-task-editor').isVisible(),false);
  await page.touchscreen.tap(x,y);await page.waitForTimeout(70);await page.touchscreen.tap(x,y);await page.locator('#violet-editor-text').fill('Создано двойным тапом');await page.locator('#violet-editor-start').fill('10:00');await page.locator('#violet-editor-end').fill('10:30');await page.locator('#violet-editor-done').tap();
  assert.equal(await page.locator('.violet-event-card').filter({hasText:'Создано двойным тапом'}).count(),1);assert.deepEqual(errors,[]);
 }finally{await context.close()}
});

test('brand silhouette and glow are present; leaving home has no intermediate animated layout',async()=>{
 const {page,context,errors}=await open();
 try{
  await page.evaluate(()=>{state.prefs.variant='violet';applyTheme();navigate(selected,'day')});
  const brand=await page.evaluate(()=>({logo:getComputedStyle(document.querySelector('.ice-tribute'),'::before').backgroundImage,plus:getComputedStyle(document.getElementById('violet-add-task')).boxShadow,menu:getComputedStyle(document.querySelector('.ice-compose')).boxShadow}));
  assert.ok(brand.logo.includes('swoosh.svg'));assert.equal(await page.evaluate(async()=>{const img=new Image();img.src='violet/swoosh.svg';await img.decode();return img.naturalWidth>0}),true);assert.notEqual(brand.plus,'none');assert.notEqual(brand.menu,'none');
  await page.screenshot({path:'/workspace/.planner-startup-review/home-brand-112.png'});
  for(const section of ['notes','calendar','workout','events','savings']){
   await page.evaluate(()=>navigate(selected,'day'));
   const frames=await page.evaluate(async section=>{
    navigate(selected,section);const frames=[];
    for(let i=0;i<8;i++){await new Promise(requestAnimationFrame);const v=document.getElementById('view'),n=document.querySelector('.tabs'),b=v.getBoundingClientRect();frames.push({top:b.top,bottom:b.bottom,opacity:getComputedStyle(v).opacity,nav:n.getBoundingClientRect().top,animation:document.getAnimations().filter(a=>a.playState==='running').length,status:statusColors.at(-1).color})}
    return frames;
   },section);
   assert.ok(frames.every(f=>f.opacity==='1'&&f.animation===0&&f.status==='#e9ddf7'),JSON.stringify(frames));assert.ok(frames.every(f=>f.top===frames[0].top&&f.bottom===frames[0].bottom&&f.nav===frames[0].nav),JSON.stringify(frames));
  }
  assert.deepEqual(errors,[]);
 }finally{await context.close()}
});

test('touch navigation leaves no focus rectangles; keyboard focus remains visible',async()=>{
 const {page,context,errors}=await open();
 try{
  await page.evaluate(()=>{state.prefs.variant='violet';applyTheme();state.prefs.mainMenu=['day','calendar','notes'];render()});
  for(const section of ['calendar','notes','day','calendar','day']){
   await page.locator(section==='day'?'.tabs [data-ice-action=today]':`.tabs [data-tab=${section}]`).tap();
   const focus=await page.evaluate(()=>{const el=document.activeElement,css=getComputedStyle(el);return {outline:css.outlineStyle,tap:css.webkitTapHighlightColor,mode:document.body.dataset.inputMode}});
   assert.equal(focus.outline,'none');assert.equal(focus.tap,'rgba(0, 0, 0, 0)');assert.equal(focus.mode,'pointer');
  }
  await page.keyboard.press('Tab');assert.equal(await page.evaluate(()=>document.body.dataset.inputMode),'keyboard');
  await page.locator('#violet-add-task').focus();assert.notEqual(await page.locator('#violet-add-task').evaluate(el=>getComputedStyle(el).outlineStyle),'none');
  await page.locator('.tabs [data-tab=calendar]').tap();assert.equal(await page.locator('.tabs [data-tab=calendar]').evaluate(el=>getComputedStyle(el).outlineStyle),'none');
  assert.deepEqual(errors,[]);
 }finally{await context.close()}
});
test('favorites have no purple media edge and savings has no duplicate date',async()=>{
 const {page,context,errors}=await open();
 try{
  await page.evaluate(()=>{state.prefs.variant='violet';applyTheme();navigate(selected,'wishes')});
  const png=await page.evaluate(()=>{const c=document.createElement('canvas');c.width=60;c.height=80;c.getContext('2d').fillRect(0,0,60,80);return c.toDataURL().split(',')[1]});
  await page.locator('[data-wish-add]').first().tap();await page.locator('#wish-file').setInputFiles({name:'original.png',mimeType:'image/png',buffer:Buffer.from(png,'base64')});
  await page.waitForFunction(()=>!!wishDraftPhoto);await page.locator('#wish-save').tap();await page.waitForFunction(()=>document.querySelector('.wish-image img')?.naturalWidth>0);
  const card=await page.locator('.wish-card').first().evaluate(el=>({border:getComputedStyle(el).borderTopWidth,shadow:getComputedStyle(el).boxShadow,bg:getComputedStyle(el.querySelector('.wish-image')).backgroundColor}));
  assert.deepEqual(card,{border:'0px',shadow:'none',bg:'rgba(0, 0, 0, 0)'});
  await page.evaluate(()=>navigate(selected,'savings'));assert.equal(await page.locator('.app-heading').isVisible(),false);assert.equal(await page.evaluate(()=>statusColors.at(-1).color),'#e9ddf7');
  assert.deepEqual(errors,[]);
 }finally{await context.close()}
});

test('button labels cannot be selected or copied; note editor remains selectable',async()=>{
 const {page,context,errors}=await open();
 try{
  const labels=await page.locator('.tabs button span').evaluateAll(els=>els.map(el=>({select:getComputedStyle(el).userSelect,animation:getComputedStyle(el).animationName,transition:getComputedStyle(el).transitionDuration})));
  assert.ok(labels.length>0);assert.ok(labels.every(x=>x.select==='none'&&x.animation==='none'&&x.transition==='0s'));
  assert.equal(await page.locator('.tabs button').first().evaluate(el=>{const e=new MouseEvent('contextmenu',{bubbles:true,cancelable:true});return el.dispatchEvent(e)}),false);
  await page.evaluate(()=>navigate(selected,'notes'));await page.locator('#note-add').tap();await page.locator('#note-rich').fill('Этот текст можно выделить');
  assert.notEqual(await page.locator('#note-rich').evaluate(el=>getComputedStyle(el).userSelect),'none');assert.deepEqual(errors,[]);
 }finally{await context.close()}
});
