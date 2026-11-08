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
   assert.deepEqual(heights,[44,44,44]);
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

test('light header buttons and a smaller violet hero leave more timeline visible',async()=>{
 const {page,context,errors}=await open();
 try{
  await page.evaluate(()=>{state.prefs.variant='violet';applyTheme();navigate(selected,'calendar')});
  for(const section of ['calendar','notes','workout']){
   await page.evaluate(section=>navigate(selected,section),section);
   const buttons=page.locator(section==='calendar'?'#calendar-top-tools button':section==='notes'?'.notes-tools .btn':'.workout-heading button');
   for(const button of await buttons.all())assert.deepEqual(await button.evaluate(el=>({bg:getComputedStyle(el).backgroundColor,ink:getComputedStyle(el).color})),{bg:'rgb(251, 248, 255)',ink:'rgb(23, 19, 34)'});
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

test('first configured menu button is the startup section, even when Today is elsewhere',async()=>{
 const {page,context,errors}=await open();
 try{
  for(const first of ['calendar','notes','workout','savings','events','wishes','week','day']){
   await page.evaluate(()=>document.getElementById('main-menu-open').click());
   const menu=first==='day'?['day','calendar','notes']:[first,'day',first==='notes'?'calendar':'notes'];
   for(const [i,name] of ['menuFirst','menuSecond','menuThird'].entries())await page.locator(`#main-menu-form [name=${name}]`).selectOption(menu[i]);
   await page.locator('#main-menu-save').tap();
   await page.reload();await page.waitForFunction(()=>document.documentElement.dataset.appReady==='true');
   assert.equal(await page.evaluate(()=>tab),first);
   if(first==='calendar'){assert.equal(await page.locator('#calendar-top-tools').isVisible(),true);assert.equal(await page.locator('.calendar-jump').isVisible(),false);assert.equal(await page.locator('.calendar-hint').count(),0)}
   assert.deepEqual(await page.evaluate(()=>state.prefs.mainMenu),menu);
   const firstButton=await page.locator('.tabs button').first().evaluate(el=>({active:el.classList.contains('active'),current:el.getAttribute('aria-current')}));
   assert.equal(firstButton.active,true);assert.equal(firstButton.current,'page');
   await page.evaluate(()=>{window.onPlannerResume?.();window.onPlannerResume?.()});assert.equal(await page.evaluate(()=>tab),first);
  }
  assert.deepEqual(errors,[]);
 }finally{await context.close()}
});

test('calendar startup has only compact right-aligned actions and restores shift colors immediately',async()=>{
 const {page,context,errors}=await open();
 try{
  await page.evaluate(()=>{state.prefs.variant='violet';state.prefs.mainMenu=['calendar','notes','day'];state.prefs.calendarSchedule={mode:'2/2',start:'2026-11-08',pattern:['work','work','off','off'],colors:{work:'#12ab34',day:'#63d8ef',night:'#b38be8',off:'#f3edf9'}};persist()});
  await page.reload();await page.waitForFunction(()=>document.documentElement.dataset.appReady==='true');
  assert.equal(await page.evaluate(()=>tab),'calendar');assert.equal(await page.locator('.calendar-jump').isVisible(),false);assert.equal(await page.locator('#calendar-top-tools').count(),1);assert.equal(await page.locator('#calendar-today').count(),1);
  assert.ok(await page.locator('.calendar-shift-label').count()>0);
  const actions=await page.locator('#calendar-top-tools').evaluate(el=>{const b=el.getBoundingClientRect(),buttons=[...el.querySelectorAll('button')];return {width:b.width,left:buttons[0].getBoundingClientRect().left-b.left,right:b.right-buttons[1].getBoundingClientRect().right,buttons:buttons.map(x=>({width:x.getBoundingClientRect().width,height:x.getBoundingClientRect().height,font:getComputedStyle(x).fontSize,bg:getComputedStyle(x).backgroundColor}))}});
  assert.ok(actions.left>actions.width*.4,JSON.stringify(actions));assert.ok(actions.right<=14);assert.ok(actions.buttons.every(b=>b.width<120&&b.height===36&&b.font==='12px'&&b.bg==='rgb(251, 248, 255)'));
  await page.locator('#calendar-schedule-open').tap();assert.equal(await page.locator('#calendar-schedule-dialog').isVisible(),true);await page.locator('#calendar-schedule-close').tap();await page.locator('#calendar-today').tap();
  await page.screenshot({path:'/workspace/.planner-startup-review/calendar-start-115.png'});assert.deepEqual(errors,[]);
 }finally{await context.close()}
});

test('mixed favorite photo and video previews have the same gap without stretching images',async()=>{
 const {page,context,errors}=await open();
 try{
  await page.evaluate(async()=>{
   state.prefs.variant='violet';applyTheme();state.wishes=[];
   const sizes=[[600,150],[100,600],[300,300],[500,200]];
   for(let i=0;i<sizes.length;i++){
    const c=document.createElement('canvas');[c.width,c.height]=sizes[i];c.getContext('2d').fillRect(0,0,c.width,c.height);const image=c.toDataURL(),id='spacing-'+i;
    await wishPhoto('put',id,image);state.wishes.push({id,title:'Файл '+i,why:'',done:false,...(i===2?{kind:'video',poster:image,duration:1}:{})});
   }
   persist();navigate(selected,'wishes');
  });
  await page.waitForFunction(()=>[...document.querySelectorAll('.wish-image img')].length===4&&[...document.querySelectorAll('.wish-image img')].every(i=>i.naturalWidth>0));
  for(const width of [393,720]){
   await page.setViewportSize({width,height:851});
   const boxes=await page.locator('.wish-card').evaluateAll(els=>els.map(el=>{const b=el.getBoundingClientRect(),img=el.querySelector('img'),i=img.getBoundingClientRect();return {top:b.top,bottom:b.bottom,height:b.height,imageHeight:i.height,ratio:i.width/i.height,naturalRatio:img.naturalWidth/img.naturalHeight}}));
   for(let i=1;i<boxes.length;i++)assert.ok(Math.abs(boxes[i].top-boxes[i-1].bottom-12)<.6,JSON.stringify(boxes));
   assert.ok(boxes.every(b=>Math.abs(b.height-b.imageHeight)<.6&&Math.abs(b.ratio-b.naturalRatio)<.02),JSON.stringify(boxes));
  }
  assert.equal(await page.locator('.wish-play').count(),1);
  await page.setViewportSize({width:393,height:851});await page.screenshot({path:'/workspace/.planner-startup-review/favorites-spacing-116.png'});assert.deepEqual(errors,[]);
 }finally{await context.close()}
});

async function holdCalendarDay(page,day){
 const cell=page.locator(`#cal-${day.slice(0,7)} .cal-cell[data-date="${day}"]`);await cell.scrollIntoViewIfNeeded();const b=await cell.boundingBox(),cdp=await page.context().newCDPSession(page);await cdp.send('Input.dispatchTouchEvent',{type:'touchStart',touchPoints:[{x:b.x+b.width/2,y:b.y+b.height/2}]});await page.waitForTimeout(650);await cdp.send('Input.dispatchTouchEvent',{type:'touchEnd',touchPoints:[]});await cdp.detach();
 await page.waitForFunction(()=>document.getElementById('calendar-day-settings').open);
}
test('long-press calendar labels and monochrome icons persist, edit and remove without changing schedule',async()=>{
 const {page,context,errors}=await open();
 try{
  await page.evaluate(()=>{state.prefs.variant='violet';state.prefs.mainMenu=['calendar','notes','day'];applyTheme();state.iceMarks=state.iceMarks||{};state.iceMarks.days={'2026-11-11':'black'};persist();navigate('2026-11-08','calendar')});
  const cell=page.locator('#cal-2026-11 [data-date="2026-11-11"]');
  assert.ok(await cell.evaluate(el=>el.getBoundingClientRect().width)>=48);
  await holdCalendarDay(page,'2026-11-11');assert.equal(await page.locator('[data-calendar-day-color=black]').getAttribute('aria-pressed'),'true');assert.equal(await page.locator('#calendar-preview').isVisible().catch(()=>false),false);
  await page.locator('#calendar-day-settings [name=text]').fill('Больница');await page.locator('[data-calendar-icon=hospital]').tap();await page.locator('#calendar-day-settings [type=submit]').tap();
  assert.equal(await cell.locator('.calendar-day-text').innerText(),'Больница');assert.equal(await cell.locator('.calendar-day-icon').evaluate(el=>getComputedStyle(el).color),'rgb(255, 255, 255)');
  assert.equal(await page.evaluate(()=>state.iceMarks.days['2026-11-11']),'black');assert.equal(await page.evaluate(()=>slots('2026-11-11').some(r=>r.text.trim())),false);
  await page.reload();await page.waitForFunction(()=>document.documentElement.dataset.appReady==='true');assert.equal(await page.evaluate(()=>tab),'calendar');assert.equal(await cell.locator('.calendar-day-text').innerText(),'Больница');
  await holdCalendarDay(page,'2026-11-11');await page.locator('#calendar-day-settings [name=text]').fill('');await page.locator('[data-calendar-icon=hospital]').tap();await page.locator('[data-calendar-icon=person]').tap();await page.locator('#calendar-day-settings [type=submit]').tap();assert.equal(await cell.locator('.calendar-day-text').count(),0);assert.equal(await cell.locator('.calendar-day-icon').count(),1);
  await holdCalendarDay(page,'2026-11-12');await page.locator('#calendar-day-settings [name=text]').fill('Прогулка');await page.locator('[data-calendar-icon=person]').tap();await page.locator('#calendar-day-settings [type=submit]').tap();assert.equal(await page.locator('#cal-2026-11 [data-date="2026-11-12"] .calendar-day-icon').evaluate(el=>getComputedStyle(el).color),'rgb(0, 0, 0)');
  await page.screenshot({path:'/workspace/.planner-startup-review/calendar-labels-117.png'});
  await holdCalendarDay(page,'2026-11-11');await page.locator('#calendar-day-settings-remove').tap();assert.equal(await cell.locator('.calendar-day-note').count(),0);
  await page.waitForTimeout(1500);
  const day=page.locator('#cal-2026-11 [data-date="2026-11-12"]');await day.scrollIntoViewIfNeeded();const b=await day.boundingBox();await page.mouse.click(b.x+b.width/2,b.y+10);await page.waitForTimeout(80);await page.mouse.click(b.x+b.width/2,b.y+10);assert.equal(await page.evaluate(()=>tab),'day');assert.equal(await page.evaluate(()=>selected),'2026-11-12');
  assert.deepEqual(errors,[]);
 }finally{await context.close()}
});
test('calendar long press cancels on drag and does not save cancelled edits',async()=>{
 const {page,context,errors}=await open();
 try{
  await page.evaluate(()=>navigate('2026-11-08','calendar'));
  const cell=page.locator('#cal-2026-11 [data-date="2026-11-12"]');await cell.scrollIntoViewIfNeeded();const b=await cell.boundingBox(),x=b.x+b.width/2,y=b.y+10;
  await page.mouse.move(x,y);await page.mouse.down();await page.mouse.move(x,y-30,{steps:4});await page.waitForTimeout(650);await page.mouse.up();assert.equal(await page.locator('#calendar-day-settings').isVisible(),false);
  await holdCalendarDay(page,'2026-11-12');await page.locator('#calendar-day-settings [name=text]').fill('Не сохранять');await page.locator('#calendar-day-settings-close').tap();assert.equal(await cell.locator('.calendar-day-note').count(),0);assert.deepEqual(errors,[]);
 }finally{await context.close()}
});

test('calendar day settings share day colors with schedule and keep draft changes cancellable',async()=>{
 const {page,context,errors}=await open();
 try{
  await page.evaluate(()=>{state.prefs.variant='violet';applyTheme();navigate('2026-11-08','calendar')});
  const cell=page.locator('#cal-2026-11 [data-date="2026-11-12"]');
  assert.ok(await cell.evaluate(el=>el.getBoundingClientRect().width)>52);assert.equal(await page.locator('#cal-2026-11 .cal-grid').evaluate(el=>getComputedStyle(el).columnGap),'1px');
  await holdCalendarDay(page,'2026-11-12');await page.locator('#calendar-day-color-toggle').tap();await page.locator('[data-calendar-day-color=black]').tap();await page.locator('[data-calendar-icon=dumbbell]').tap();await page.locator('#calendar-day-settings [name=text]').fill('Зал');await page.locator('#calendar-day-settings [type=submit]').tap();
  assert.equal(await page.evaluate(()=>dayColor('2026-11-12')),'black');assert.equal(await cell.locator('.calendar-day-icon').evaluate(el=>getComputedStyle(el).color),'rgb(255, 255, 255)');
  await page.evaluate(()=>navigate('2026-11-12','day'));assert.equal(await page.evaluate(()=>dayColor(selected)),'black');
  await page.evaluate(()=>navigate('2026-11-12','calendar'));await holdCalendarDay(page,'2026-11-12');assert.equal(await page.locator('[data-calendar-day-color=black]').getAttribute('aria-pressed'),'true');await page.locator('#calendar-day-color-toggle').tap();await page.locator('[data-calendar-day-color=green]').tap();await page.locator('#calendar-day-settings-close').tap();assert.equal(await page.evaluate(()=>dayColor('2026-11-12')),'black');
  await holdCalendarDay(page,'2026-11-12');await page.locator('#calendar-day-color-toggle').tap();await page.locator('[data-calendar-day-color=green]').tap();await page.locator('#calendar-day-settings [type=submit]').tap();assert.equal(await page.evaluate(()=>dayColor('2026-11-12')),'green');assert.equal(await cell.locator('.calendar-day-icon').evaluate(el=>getComputedStyle(el).color),'rgb(0, 0, 0)');
  await page.reload();await page.waitForFunction(()=>document.documentElement.dataset.appReady==='true');assert.equal(await page.evaluate(()=>dayColor('2026-11-12')),'green');
  await page.evaluate(()=>navigate('2026-11-12','calendar'));await holdCalendarDay(page,'2026-11-12');await page.locator('#calendar-day-color-toggle').tap();await page.locator('[data-calendar-day-color=none]').tap();await page.locator('#calendar-day-settings [type=submit]').tap();assert.equal(await page.evaluate(()=>dayColor('2026-11-12')),'none');assert.equal(await cell.locator('.calendar-day-text').innerText(),'Зал');
  await page.screenshot({path:'/workspace/.planner-startup-review/calendar-colors-118.png'});assert.deepEqual(errors,[]);
 }finally{await context.close()}
});

test('multiple calendar icons toggle independently, scale and survive relaunch alongside legacy icons',async()=>{
 const {page,context,errors}=await open();
 try{
  await page.evaluate(()=>{state.prefs.variant='violet';state.prefs.mainMenu=['calendar','notes','day'];state.prefs.calendarDayNotes={'2026-11-12':{text:'Зал и врач',icon:'dumbbell'}};applyTheme();persist();navigate('2026-11-08','calendar')});
  const icons=page.locator('#cal-2026-11 [data-date="2026-11-12"] .calendar-day-icon');
  assert.equal(await icons.count(),1);assert.equal(await icons.first().evaluate(el=>el.getBoundingClientRect().width),23);
  await holdCalendarDay(page,'2026-11-12');assert.equal(await page.locator('[data-calendar-icon=dumbbell]').getAttribute('aria-pressed'),'true');await page.locator('[data-calendar-icon=hospital]').tap();assert.equal(await page.locator('[data-calendar-icon=dumbbell]').getAttribute('aria-pressed'),'true');await page.locator('#calendar-day-settings [type=submit]').tap();
  assert.equal(await icons.count(),2);assert.equal(await icons.first().evaluate(el=>el.getBoundingClientRect().width),20);
  await page.reload();await page.waitForFunction(()=>document.documentElement.dataset.appReady==='true');assert.equal(await icons.count(),2);
  await holdCalendarDay(page,'2026-11-12');await page.locator('[data-calendar-icon=person]').tap();await page.locator('[data-calendar-icon=work]').tap();await page.locator('#calendar-day-settings [type=submit]').tap();assert.equal(await icons.count(),4);assert.equal(await icons.first().evaluate(el=>el.getBoundingClientRect().width),14);
  assert.ok(await icons.evaluateAll(els=>els.every(el=>{const b=el.getBoundingClientRect(),c=el.closest('.cal-cell').getBoundingClientRect();return b.left>=c.left&&b.right<=c.right})));await page.screenshot({path:'/workspace/.planner-startup-review/calendar-multi-icons-119.png'});
  await holdCalendarDay(page,'2026-11-12');await page.locator('[data-calendar-icon=hospital]').tap();await page.locator('#calendar-day-settings [type=submit]').tap();assert.equal(await icons.count(),3);
  await holdCalendarDay(page,'2026-11-12');await page.locator('[data-calendar-icon=""]').tap();await page.locator('#calendar-day-settings [type=submit]').tap();assert.equal(await icons.count(),0);assert.equal(await page.locator('#cal-2026-11 [data-date="2026-11-12"] .calendar-day-text').innerText(),'Зал и врач');assert.deepEqual(errors,[]);
 }finally{await context.close()}
});

async function dragMenuItem(page,id,target,last=false){
 const source=page.locator(`[data-more-menu="${id}"]`),destination=page.locator(`[data-more-menu="${target}"]`);await source.scrollIntoViewIfNeeded();const a=await source.boundingBox(),b=await destination.boundingBox(),cdp=await page.context().newCDPSession(page),x=a.x+a.width/2,y=a.y+a.height/2,end=last?b.y+b.height-3:b.y+3;
 await cdp.send('Input.dispatchTouchEvent',{type:'touchStart',touchPoints:[{x,y}]});await page.waitForTimeout(520);assert.equal(await page.locator('.more-menu-drag-ghost').isVisible(),true);
 for(let i=1;i<=6;i++)await cdp.send('Input.dispatchTouchEvent',{type:'touchMove',touchPoints:[{x,y:y+(end-y)*i/6}]});
 await cdp.send('Input.dispatchTouchEvent',{type:'touchEnd',touchPoints:[]});await cdp.detach();
}
test('more menu excludes bottom sections, exposes all remaining sections and opens reminders',async()=>{
 const {page,context,errors}=await open();
 try{
  for(const menu of [['day','calendar','notes'],['notes','workout','savings'],['events','wishes','week']]){
   await page.evaluate(menu=>{state.prefs.mainMenu=menu;render()},menu);await page.locator('#ice-add').tap();
   const sections=await page.locator('[data-more-menu]').evaluateAll(els=>els.filter(el=>el.parentElement.id==='ice-tools-list').map(el=>el.dataset.moreMenu));
   const all=['day','week','calendar','notes','savings','events','wishes','workout','calculator','settings'];assert.deepEqual([...sections].sort(),all.filter(id=>!menu.includes(id)).sort());assert.equal(new Set(sections).size,sections.length);
   await page.locator('[data-close="ice-menu-dialog"]').tap();
  }
  await page.evaluate(()=>{state.prefs.mainMenu=['day','calendar','notes'];render()});await page.locator('#ice-add').tap();await page.locator('[data-more-menu=events]').tap();assert.equal(await page.evaluate(()=>tab),'events');assert.equal(await page.locator('#ice-menu-dialog').isVisible(),false);assert.deepEqual(errors,[]);
 }finally{await context.close()}
});
test('long-hold touch dragging reorders menu and persists without opening a section',async()=>{
 const {page,context,errors}=await open();
 try{
  await page.evaluate(()=>{state.prefs.variant='violet';state.prefs.mainMenu=['day','calendar','notes'];applyTheme();render()});await page.locator('#ice-add').tap();
  await dragMenuItem(page,'workout','wishes');assert.equal(await page.locator('#ice-tools-list>button').first().getAttribute('data-more-menu'),'workout');assert.equal(await page.evaluate(()=>tab),'day');assert.equal(await page.locator('#ice-menu-dialog').isVisible(),true);
  await dragMenuItem(page,'savings','settings',true);assert.equal(await page.locator('#ice-tools-list>button').last().getAttribute('data-more-menu'),'savings');assert.equal(await page.locator('.more-menu-drag-ghost').count(),0);
  await page.locator('[data-close="ice-menu-dialog"]').tap();await page.reload();await page.waitForFunction(()=>document.documentElement.dataset.appReady==='true');await page.locator('#ice-add').tap();assert.equal(await page.locator('#ice-tools-list>button').first().getAttribute('data-more-menu'),'workout');assert.equal(await page.locator('#ice-tools-list>button').last().getAttribute('data-more-menu'),'savings');
  await page.screenshot({path:'/workspace/.planner-startup-review/reordered-menu-121.png'});await page.locator('[data-more-menu=workout]').tap();assert.equal(await page.evaluate(()=>tab),'workout');
  await page.evaluate(()=>{state.prefs.mainMenu=['workout','calendar','notes'];persist();render()});await page.locator('#ice-add').tap();assert.equal(await page.locator('#ice-tools-list [data-more-menu=workout]').count(),0);assert.equal(await page.locator('#ice-tools-list [data-more-menu=day]').count(),1);await page.locator('[data-close="ice-menu-dialog"]').tap();
  await page.evaluate(()=>{state.prefs.mainMenu=['day','calendar','notes'];persist();render()});await page.locator('#ice-add').tap();assert.equal(await page.locator('#ice-tools-list>button').first().getAttribute('data-more-menu'),'workout');assert.deepEqual(errors,[]);
 }finally{await context.close()}
});

async function swipeDateStrip(page,left=true){
 const b=await page.locator('.home-date-strip').boundingBox(),x=left?b.x+b.width*.85:b.x+b.width*.15,end=left?b.x+b.width*.15:b.x+b.width*.85,y=b.y+b.height/2,cdp=await page.context().newCDPSession(page);
 await cdp.send('Input.dispatchTouchEvent',{type:'touchStart',touchPoints:[{x,y}]});for(let i=1;i<=10;i++){await cdp.send('Input.dispatchTouchEvent',{type:'touchMove',touchPoints:[{x:x+(end-x)*i/10,y}]});await page.waitForTimeout(20)}await cdp.send('Input.dispatchTouchEvent',{type:'touchEnd',touchPoints:[]});await cdp.detach();await page.waitForTimeout(250);
}
test('home dates swipe both ways and open tomorrow directly to add a task',async()=>{
 const {page,context,errors}=await open();
 try{
  await page.evaluate(()=>{state.prefs.variant='violet';applyTheme();navigate('2026-11-08','day')});
  const nav=page.locator('.home-date-strip');const bounds=await nav.evaluate(el=>{const n=el.getBoundingClientRect(),b=el.querySelector('[aria-current=date]').getBoundingClientRect();return {width:b.width,expected:(el.clientWidth-26)/7-4,right:n.right-b.right,left:b.left-n.left}});assert.ok(Math.abs(bounds.width-bounds.expected)<1);assert.ok(bounds.right>=6&&bounds.left>=6,JSON.stringify(bounds));const start=await nav.evaluate(el=>el.scrollLeft);await swipeDateStrip(page);const forward=await nav.evaluate(el=>el.scrollLeft);assert.ok(forward>start+60);assert.equal(await page.evaluate(()=>selected),'2026-11-08');
  const tomorrow=page.locator('.home-date-strip [data-date="2026-11-09"]');await tomorrow.tap();assert.equal(await page.evaluate(()=>selected),'2026-11-09');assert.equal(await page.evaluate(()=>tab),'day');assert.equal(await page.locator('.home-date-strip [aria-current=date]').getAttribute('data-date'),'2026-11-09');
  await page.locator('#violet-add-task').tap();await page.locator('#violet-editor-text').fill('План на завтра');await page.locator('#violet-editor-start').fill('10:00');await page.locator('#violet-editor-end').fill('10:30');await page.locator('#violet-editor-done').tap();assert.equal(await page.locator('.violet-event-card').filter({hasText:'План на завтра'}).count(),1);
  const firstVisible=el=>{const left=el.getBoundingClientRect().left;return [...el.children].find(b=>b.getBoundingClientRect().right>left+8).dataset.date};const before=await nav.evaluate(firstVisible);await swipeDateStrip(page,false);assert.ok(await nav.evaluate(firstVisible)<before);
  await page.locator('.home-date-strip [data-date="2026-11-08"]').tap();assert.equal(await page.evaluate(()=>selected),'2026-11-08');assert.equal(await page.locator('.violet-event-card').filter({hasText:'План на завтра'}).count(),0);
  await page.screenshot({path:'/workspace/.planner-startup-review/home-scroll-dates-122.png'});assert.deepEqual(errors,[]);
 }finally{await context.close()}
});
test('date strip extends in both directions without duplicate dates or losing day selection',async()=>{
 const {page,context,errors}=await open();
 try{
  for(const variant of Object.keys(palettes)){
   await page.evaluate(v=>{state.prefs.variant=v;applyTheme();navigate('2026-12-31','day')},variant);
   const nav=page.locator('.home-date-strip');assert.equal(await page.locator('.home-date-strip [aria-current=date]').getAttribute('data-date'),'2026-12-31');
   const initial=await nav.locator('button').count();await nav.evaluate(el=>el.scrollLeft=0);await page.waitForTimeout(70);assert.ok(await nav.locator('button').count()>initial);
   const after=await nav.locator('button').count();await nav.evaluate(el=>el.scrollLeft=el.scrollWidth);await page.waitForTimeout(70);assert.ok(await nav.locator('button').count()>after);
   const dates=await nav.locator('button').evaluateAll(els=>els.map(el=>el.dataset.date));assert.equal(new Set(dates).size,dates.length);assert.ok(dates.includes('2027-01-01'));assert.equal(await page.evaluate(()=>selected),'2026-12-31');
  }
  assert.deepEqual(errors,[]);
 }finally{await context.close()}
});

test('calendar photos keep original bytes in Notes Calendar folder, show date markers and survive relaunch',async()=>{
 const {page,context,errors}=await open();
 try{
  await page.evaluate(()=>{state.prefs.variant='violet';state.prefs.mainMenu=['calendar','notes','day'];applyTheme();persist();navigate('2026-11-08','calendar')});
  await holdCalendarDay(page,'2026-11-12');
  const png=Buffer.from(await page.evaluate(()=>{const c=document.createElement('canvas');c.width=80;c.height=120;const x=c.getContext('2d');x.fillStyle='#c59af2';x.fillRect(0,0,80,120);return c.toDataURL().split(',')[1]}),'base64');
  await page.locator('#calendar-photo-picker').setInputFiles([{name:'first-original.png',mimeType:'image/png',buffer:png},{name:'second-original.png',mimeType:'image/png',buffer:png}]);
  await page.waitForFunction(()=>document.querySelectorAll('.calendar-photo-thumb').length===2);
  assert.equal(await page.locator('#cal-2026-11 [data-date="2026-11-12"] .calendar-photo-badge small').innerText(),'2');
  const stored=await page.evaluate(async()=>{const rows=(await mediaQuery()).filter(r=>r.calendarDate==='2026-11-12');return {count:rows.length,bytes:await Promise.all(rows.map(async r=>Array.from(new Uint8Array(await r.blob.arrayBuffer())))),folder:state.mediaFolders.find(f=>f.id===rows[0].folder)?.name}});
  assert.equal(stored.count,2);assert.equal(stored.folder,'Календарь');for(const bytes of stored.bytes)assert.deepEqual(Buffer.from(bytes),png);
  await page.locator('[data-calendar-photo-view]').first().tap();await page.locator('#calendar-photo-gallery').waitFor({state:'visible'});await page.locator('#calendar-gallery-close').tap();await page.locator('#calendar-day-settings-close').tap();
  await page.reload();await page.waitForFunction(()=>document.documentElement.dataset.appReady==='true');await page.waitForFunction(()=>document.querySelector('#cal-2026-11 [data-date="2026-11-12"] .calendar-photo-badge small')?.textContent==='2');
  await holdCalendarDay(page,'2026-11-12');assert.equal(await page.locator('.calendar-photo-thumb').count(),2);await page.locator('#calendar-photo-folder').tap();assert.equal(await page.evaluate(()=>tab),'notes');
  await page.waitForFunction(()=>document.querySelectorAll('.notes-media-item').length===2);assert.equal(await page.locator('.calendar-photo-date').first().innerText(),'12 ноября 2026 г.');
  await page.locator('[data-media-preview]').first().tap();await page.locator('#notes-media-viewer').waitFor({state:'visible'});await page.locator('[data-media-viewer-close]').tap();
  await page.evaluate(()=>navigate('2026-11-08','calendar'));await holdCalendarDay(page,'2026-11-12');await page.locator('[data-calendar-photo-detach]').first().tap();await page.waitForFunction(()=>document.querySelectorAll('.calendar-photo-thumb').length===1);assert.equal(await page.evaluate(async()=>(await mediaQuery()).length),2);
  await page.locator('#calendar-day-settings-close').tap();assert.equal(await page.locator('#cal-2026-11 [data-date="2026-11-12"] .calendar-photo-badge small').innerText(),'1');
  await page.screenshot({path:'/workspace/.planner-startup-review/calendar-photo-marker-123.png'});assert.deepEqual(errors,[]);
 }finally{await context.close()}
});

test('calendar photo button opens swipe gallery and closes back to the same preview and calendar',async()=>{
 const {page,context,errors}=await open();
 try{
  await page.evaluate(async()=>{
   state.prefs.variant='violet';applyTheme();
   for(let i=0;i<3;i++){const c=document.createElement('canvas');c.width=80;c.height=120;const x=c.getContext('2d');x.fillStyle=['red','blue','green'][i];x.fillRect(0,0,80,120);const blob=await new Promise(resolve=>c.toBlob(resolve));await mediaPut({id:'gallery-'+i,folder:'calendar-fixture',name:'Фото '+i+'.png',type:'image/png',blob,created:i,calendarDate:i<2?'2026-11-12':'2026-11-13'})}
   ensureHourlyDay('2026-11-12').forEach((r,i)=>{r.text='Запись '+i});navigate('2026-11-08','calendar');
  });
  await page.locator('#cal-2026-11 [data-date="2026-11-12"]').tap();assert.equal(await page.locator('#calendar-preview').isVisible(),true);
  const actionBoxes=await page.locator('#calendar-preview .dialog-actions button').evaluateAll(els=>els.map(el=>{const b=el.getBoundingClientRect();return {top:b.top,bottom:b.bottom}}));assert.equal(actionBoxes.length,2);assert.ok(Math.abs(actionBoxes[0].top-actionBoxes[1].top)<1);
  const initial=await page.evaluate(()=>({tab,selected,scroll:document.getElementById('view').scrollTop,preview:document.getElementById('calendar-preview').innerHTML}));
  await page.locator('[data-calendar-photos-open="2026-11-12"]').tap();await page.locator('#calendar-photo-gallery').waitFor({state:'visible'});assert.equal(await page.locator('.calendar-gallery-page').count(),2);assert.equal(await page.locator('#calendar-gallery-count').innerText(),'1 / 2');assert.equal(await page.locator('#calendar-day-settings').isVisible(),false);
  const b=await page.locator('#calendar-gallery-track').boundingBox(),cdp=await context.newCDPSession(page),y=b.y+b.height/2;
  async function swipe(left){const x=left?b.x+b.width*.8:b.x+b.width*.2,end=left?b.x+b.width*.2:b.x+b.width*.8;await cdp.send('Input.dispatchTouchEvent',{type:'touchStart',touchPoints:[{x,y}]});for(let i=1;i<=10;i++){await cdp.send('Input.dispatchTouchEvent',{type:'touchMove',touchPoints:[{x:x+(end-x)*i/10,y}]});await page.waitForTimeout(20)}await cdp.send('Input.dispatchTouchEvent',{type:'touchEnd',touchPoints:[]})}
  await swipe(true);await page.waitForFunction(()=>document.getElementById('calendar-gallery-count').textContent==='2 / 2');await page.waitForTimeout(250);await swipe(false);await page.waitForFunction(()=>document.getElementById('calendar-gallery-count').textContent==='1 / 2');await cdp.detach();
  await page.locator('#calendar-gallery-close').tap();assert.equal(await page.locator('#calendar-photo-gallery').isVisible(),false);assert.equal(await page.locator('#calendar-preview').isVisible(),true);
  assert.deepEqual(await page.evaluate(()=>({tab,selected,scroll:document.getElementById('view').scrollTop,preview:document.getElementById('calendar-preview').innerHTML})),initial);
  await page.locator('[data-calendar-photos-open="2026-11-12"]').tap();assert.equal(await page.locator('#calendar-gallery-count').innerText(),'1 / 2');await page.keyboard.press('Escape');assert.equal(await page.locator('#calendar-preview').isVisible(),true);assert.deepEqual(errors,[]);
 }finally{await context.close()}
});

test('week entries follow actual start times and show saved half-hour intervals without reordering stored slots',async()=>{
 const {page,context,errors}=await open();
 try{
  await page.evaluate(()=>{
   state.prefs.variant='violet';applyTheme();const rows=ensureHourlyDay('2026-11-09');
   const tasks=[['06:00','06:40','Подъём'],['07:30','08:00','Завтрак'],['06:40','07:30','Прогулка'],['15:00','16:00','Тренировка'],['08:00','10:00','Работа'],['12:30','13:00','Обед']];
   tasks.forEach(([time,endTime,text],i)=>Object.assign(rows[i],{time,endTime,text}));window.weekFixture=JSON.stringify(state.hourlyDays);navigate('2026-11-09','week');
  });
  const entries=await page.locator('.overview').first().locator('.preview-line').evaluateAll(els=>els.map(el=>({time:el.querySelector('.preview-time').textContent,title:el.querySelector('p').textContent})));
  assert.deepEqual(entries,[{time:'06:00–06:40',title:'Подъём'},{time:'06:40–07:30',title:'Прогулка'},{time:'07:30–08:00',title:'Завтрак'},{time:'08:00–10:00',title:'Работа'},{time:'12:30–13:00',title:'Обед'},{time:'15:00–16:00',title:'Тренировка'}]);
  assert.equal(await page.evaluate(()=>JSON.stringify(state.hourlyDays)===window.weekFixture),true);
  await page.screenshot({path:'/workspace/.planner-startup-review/week-chronological-127.png'});assert.deepEqual(errors,[]);
 }finally{await context.close()}
});

test('Notes albums show three square covers per row, fitting names and correct file counts',async()=>{
 const {page,context,errors}=await open();
 try{
  await page.evaluate(async()=>{
   state.prefs.variant='violet';applyTheme();state.mediaFolders=[{id:'album-calendar',name:'Календарь'},{id:'album-photo',name:'Фотографии'}];
   const c=document.createElement('canvas');c.width=100;c.height=80;const ctx=c.getContext('2d');ctx.fillStyle='#c59af2';ctx.fillRect(0,0,100,80);const blob=await new Promise(resolve=>c.toBlob(resolve));
   await mediaPut({id:'album-file-one',folder:'album-calendar',name:'Оригинал.png',type:'image/png',blob,created:1});
   await mediaPut({id:'album-file-two',folder:'album-photo',name:'Фото.png',type:'image/png',blob,created:2});
   await mediaPut({id:'album-file-three',folder:'album-photo',name:'Фото 2.png',type:'image/png',blob,created:3});
   navigate(selected,'notes');
  });
  await page.waitForFunction(()=>document.querySelector('[data-album-id="album-calendar"] .notes-album-count')?.textContent==='1');
  const initial=await page.locator('.notes-album').first().evaluate(el=>el.getBoundingClientRect().width);assert.equal(await page.locator('.notes-album').count(),2);
  assert.equal(await page.locator('[data-album-id=album-photo] .notes-album-count').innerText(),'2');
  assert.equal(await page.locator('[data-album-id=album-calendar] .notes-album-name').evaluate(el=>el.scrollWidth<=el.clientWidth),true);
  await page.evaluate(()=>{state.mediaFolders.push({id:'album-empty',name:'Пустая'},{id:'album-long',name:'Длинное название папки с фотографиями'});persist();renderNotes()});await page.waitForFunction(()=>document.querySelector('[data-album-id="album-empty"] .notes-album-count')?.textContent==='0');
  const layout=await page.locator('.notes-album').evaluateAll(els=>els.map(el=>{const b=el.getBoundingClientRect(),cover=el.querySelector('.notes-album-cover').getBoundingClientRect();return {top:b.top,left:b.left,width:b.width,coverWidth:cover.width,coverHeight:cover.height}}));
  assert.ok(Math.abs(layout[0].width-initial)<1);assert.equal(layout[0].top,layout[2].top);assert.ok(layout[3].top>layout[0].top);assert.ok(layout.every(b=>Math.abs(b.coverWidth-b.coverHeight)<1));
  assert.equal(await page.locator('.notes-album-cover img').count(),2);
  await page.screenshot({path:'/workspace/.planner-startup-review/note-albums-128.png'});
  await page.locator('[data-media-folder-open=album-calendar]').tap();await page.waitForFunction(()=>document.querySelectorAll('.notes-media-item').length===1);assert.equal(await page.locator('.notes-media-toolbar strong').innerText(),'Календарь');
  await page.locator('[data-media-back]').tap();await page.locator('[data-media-folder-menu=album-empty]').tap();assert.equal(await page.locator('#notes-media-sheet').isVisible(),true);assert.deepEqual(errors,[]);
 }finally{await context.close()}
});
test('album cover can display a frame from an original video file',async()=>{
 const {page,context,errors}=await open();
 try{
  await page.evaluate(async()=>{
   const c=document.createElement('canvas');c.width=80;c.height=60;const x=c.getContext('2d');x.fillStyle='orange';x.fillRect(0,0,80,60);const stream=c.captureStream(10),chunks=[],recorder=new MediaRecorder(stream,{mimeType:'video/webm'});
   const done=new Promise(resolve=>{recorder.ondataavailable=e=>chunks.push(e.data);recorder.onstop=resolve});recorder.start();await new Promise(resolve=>setTimeout(resolve,200));recorder.stop();await done;stream.getTracks().forEach(t=>t.stop());const blob=new Blob(chunks,{type:'video/webm'});
   state.mediaFolders=[{id:'video-album',name:'Видео'}];await mediaPut({id:'video-cover',folder:'video-album',name:'Original.webm',type:'video/webm',blob,created:1});navigate(selected,'notes');
  });
  await page.waitForFunction(()=>document.querySelector('.notes-album-cover video')?.readyState>=2);assert.equal(await page.locator('.notes-album-count').innerText(),'1');assert.equal(await page.locator('.notes-album-video').isVisible(),true);assert.deepEqual(errors,[]);
 }finally{await context.close()}
});

test('note list cards have consistent compact previews with two text lines and both dates',async()=>{
 const {page,context,errors}=await open();
 try{
  await page.evaluate(()=>{
   const now=Date.now();state.prefs.variant='violet';applyTheme();state.notes=[{id:'short-preview',title:'Короткая',text:'Одна строка',created:now,updated:now},{id:'long-preview',title:'Длинное название заметки '.repeat(8),text:'Первая строка\nВторая строка\n'+('Всё остальное остаётся внутри заметки\n'.repeat(100)),created:now-1000,updated:now},{id:'empty-preview',title:'',text:'',created:now,updated:now}];persist();navigate(selected,'notes');
  });
  const cards=await page.locator('#notes-list .note-card').evaluateAll(els=>els.map(el=>({height:el.getBoundingClientRect().height,text:el.querySelector('p').getBoundingClientRect().height,dates:[...el.querySelectorAll('small')].map(x=>x.textContent),clamp:getComputedStyle(el.querySelector('p')).webkitLineClamp})));
  assert.equal(cards.length,3);assert.ok(cards.every(c=>c.height===124&&c.text===36&&c.clamp==='2'&&c.dates.length===2));assert.ok(cards.every(c=>c.dates[0].startsWith('Создано:')&&c.dates[1].startsWith('Изменено:')));
  await page.screenshot({path:'/workspace/.planner-startup-review/compact-note-previews-129.png'});
  await page.locator('[data-note-open=long-preview]').tap();assert.ok((await page.locator('#note-rich').innerText()).includes('Всё остальное остаётся внутри заметки'));assert.equal(await page.evaluate(()=>state.notes.find(n=>n.id==='long-preview').text.split('\n').length),103);assert.deepEqual(errors,[]);
 }finally{await context.close()}
});

test('calendar day settings open compact with collapsed colors and readily accessible icons and photos',async()=>{
 const {page,context,errors}=await open();
 try{
  await page.evaluate(()=>{state.prefs.variant='violet';applyTheme();navigate('2026-11-08','calendar')});await holdCalendarDay(page,'2026-11-12');
  assert.equal(await page.locator('#calendar-day-color-choices').isVisible(),false);assert.equal(await page.locator('#calendar-day-color-toggle').getAttribute('aria-expanded'),'false');
  const positions=await page.evaluate(()=>{const d=document.getElementById('calendar-day-settings').getBoundingClientRect(),photo=document.getElementById('calendar-photo-add').getBoundingClientRect(),icons=document.querySelector('.calendar-icon-choices').getBoundingClientRect();return {bottom:d.bottom,photoBottom:photo.bottom,iconsBottom:icons.bottom,scroll:document.getElementById('calendar-day-settings').scrollTop}});assert.equal(positions.scroll,0);assert.ok(positions.photoBottom<positions.bottom&&positions.iconsBottom<positions.bottom,JSON.stringify(positions));
  await page.locator('#calendar-day-color-toggle').tap();assert.equal(await page.locator('#calendar-day-color-choices').isVisible(),true);await page.locator('[data-calendar-day-color=green]').tap();assert.equal(await page.locator('#calendar-day-color-choices').isVisible(),false);assert.ok((await page.locator('#calendar-day-color-name').innerText()).includes('Салатовый'));
  await page.locator('[data-calendar-icon=person]').tap();await page.locator('#calendar-day-settings [type=submit]').tap();assert.equal(await page.evaluate(()=>dayColor('2026-11-12')),'green');
  await holdCalendarDay(page,'2026-11-12');assert.equal(await page.locator('#calendar-day-color-choices').isVisible(),false);assert.equal(await page.locator('[data-calendar-day-color=green]').getAttribute('aria-pressed'),'true');
  await page.screenshot({path:'/workspace/.planner-startup-review/compact-day-settings-130.png'});assert.deepEqual(errors,[]);
 }finally{await context.close()}
});

test('section actions share light buttons, black text and top alignment; week retains date selection',async()=>{
 const {page,context,errors}=await open();
 try{
  const tops=[];
  for(const [section,selector] of [['calendar','#calendar-top-tools button'],['week','.toolbar .nav .btn'],['notes','.notes-tools .btn'],['workout','.workout-heading button'],['wishes','.wishes-heading-actions .btn'],['events','.events-tools .btn'],['savings','#savings-configure']]){
   await page.evaluate(section=>{state.prefs.variant='violet';applyTheme();navigate(selected,section)},section);
   const buttons=await page.locator(selector).evaluateAll(els=>els.map(el=>{const b=el.getBoundingClientRect(),s=getComputedStyle(el);return {top:b.top,height:b.height,bg:s.backgroundColor,ink:s.color,font:s.fontSize,family:s.fontFamily}}));
   assert.ok(buttons.length>0,section);assert.ok(buttons.every(b=>b.height===36&&b.bg==='rgb(251, 248, 255)'&&b.ink==='rgb(23, 19, 34)'&&b.font==='12px'),JSON.stringify({section,buttons}));tops.push({section,top:buttons[0].top});
   if(section==='calendar')assert.equal(await page.locator('#calendar-top-tools svg').count(),0);
   if(section==='week'){assert.equal(await page.locator('.app-heading').isVisible(),false);assert.equal(await page.locator('#date').isVisible(),true)}
  }
  assert.ok(tops.every(t=>Math.abs(t.top-tops[0].top)<1),JSON.stringify(tops));
  await page.evaluate(()=>navigate(selected,'calendar'));await page.screenshot({path:'/workspace/.planner-startup-review/light-controls-calendar-131.png'});
  await page.evaluate(()=>navigate(selected,'week'));await page.screenshot({path:'/workspace/.planner-startup-review/light-controls-week-131.png'});assert.deepEqual(errors,[]);
 }finally{await context.close()}
});

test('week keeps date picker and arrows for moving between weeks',async()=>{
 const {page,context,errors}=await open();
 try{
  await page.evaluate(()=>navigate('2026-11-09','week'));assert.equal(await page.locator('#date').isVisible(),true);
  await page.locator('#next').tap();assert.equal(await page.evaluate(()=>selected),'2026-11-16');assert.equal(await page.evaluate(()=>tab),'week');
  await page.locator('#prev').tap();assert.equal(await page.evaluate(()=>selected),'2026-11-09');
  await page.locator('#date').fill('2026-12-14');await page.locator('#date').dispatchEvent('change');assert.equal(await page.evaluate(()=>selected),'2026-12-14');assert.equal(await page.evaluate(()=>tab),'week');
  await page.locator('#today').tap();assert.equal(await page.evaluate(()=>selected),await page.evaluate(()=>iso()));assert.equal(await page.evaluate(()=>tab),'week');assert.deepEqual(errors,[]);
 }finally{await context.close()}
});

test('new everyday icons are searchable and categorized, and running plus basketball persist on a calendar day',async()=>{
 const {page,context,errors}=await open();
 try{
  await page.evaluate(()=>{state.prefs.variant='violet';state.prefs.mainMenu=['calendar','notes','day'];applyTheme();persist();navigate('2026-11-08','calendar')});await holdCalendarDay(page,'2026-11-12');
  assert.ok(await page.locator('[data-calendar-icon]').count()>=40);await page.locator('[data-icon-category=sport]').tap();assert.equal(await page.locator('[data-calendar-icon=run]').isVisible(),true);assert.equal(await page.locator('[data-calendar-icon=ball-basketball]').isVisible(),true);assert.equal(await page.locator('[data-calendar-icon=wallet]').isVisible(),false);
  await page.locator('[data-calendar-icon=run]').tap();await page.locator('#calendar-icon-search').fill('баскет');assert.equal(await page.locator('[data-calendar-icon=ball-basketball]').isVisible(),true);await page.locator('[data-calendar-icon=ball-basketball]').tap();
  await page.locator('[data-icon-category=selected]').tap();assert.equal(await page.locator('.calendar-icon-choices [aria-pressed=true]:visible').count(),2);
  await page.locator('#calendar-day-settings [type=submit]').tap();assert.equal(await page.locator('#cal-2026-11 [data-date="2026-11-12"] .calendar-day-icon').count(),2);
  await page.reload();await page.waitForFunction(()=>document.documentElement.dataset.appReady==='true');assert.equal(await page.locator('#cal-2026-11 [data-date="2026-11-12"] .calendar-day-icon').count(),2);
  await holdCalendarDay(page,'2026-11-12');await page.locator('[data-icon-category=all]').tap();await page.screenshot({path:'/workspace/.planner-startup-review/calendar-icons-132.png'});await page.locator('#calendar-icon-search').fill('зубы');assert.equal(await page.locator('[data-calendar-icon=dental]').isVisible(),true);assert.deepEqual(errors,[]);
 }finally{await context.close()}
});

test('calendar icon groups align at the bottom regardless of text, task count and photo badges',async()=>{
 const {page,context,errors}=await open();
 try{
  await page.evaluate(async()=>{
   state.prefs.variant='violet';applyTheme();state.prefs.calendarDayNotes={'2026-11-09':{text:'Зал и врач',icons:['dumbbell','hospital']},'2026-11-10':{text:'',icons:['run']},'2026-11-11':{text:'Важная встреча в больнице',icons:['hospital']}};
   ensureHourlyDay('2026-11-09')[0].text='Дело';const c=document.createElement('canvas');c.width=10;c.height=10;const blob=await new Promise(resolve=>c.toBlob(resolve));await mediaPut({id:'alignment-photo',folder:'calendar',name:'Фото.png',type:'image/png',blob,created:1,calendarDate:'2026-11-11'});navigate('2026-11-09','calendar');
  });
  const layout=await page.evaluate(()=>['2026-11-09','2026-11-10','2026-11-11'].map(date=>{const cell=document.querySelector('#cal-2026-11 [data-date="'+date+'"]'),c=cell.getBoundingClientRect(),icons=cell.querySelector('.calendar-day-icons').getBoundingClientRect(),text=cell.querySelector('.calendar-day-text')?.getBoundingClientRect(),photo=cell.querySelector('.calendar-photo-badge')?.getBoundingClientRect();return {date,bottom:icons.bottom,offset:c.bottom-icons.bottom,textBottom:text?.bottom,photoBottom:photo?.bottom,iconsTop:icons.top}}));
  assert.ok(layout.every(x=>Math.abs(x.bottom-layout[0].bottom)<1),JSON.stringify(layout));assert.ok(layout.every(x=>x.offset>=6&&x.offset<=10),JSON.stringify(layout));assert.ok(layout.every(x=>(!x.textBottom||x.textBottom<x.iconsTop)&&(!x.photoBottom||x.photoBottom<x.iconsTop)),JSON.stringify(layout));
  await page.screenshot({path:'/workspace/.planner-startup-review/calendar-bottom-icons-133.png'});assert.deepEqual(errors,[]);
 }finally{await context.close()}
});

test('header artwork swipes independently from dates, cycles to swoosh and persists after relaunch',async()=>{
 const {page,context,errors}=await open();
 try{
  const samples=['leaves','car','earth','sound','city','basketball','silk','wave'].map((id,i)=>({id,title:id,src:'data:image/svg+xml,'+encodeURIComponent(`<svg xmlns="http://www.w3.org/2000/svg" width="300" height="100"><rect width="300" height="100" fill="hsl(${i*40},50%,50%)"/></svg>`)}));
  await page.route('**/header-artworks.js',route=>route.fulfill({contentType:'text/javascript',body:'window.plannerHeaderImages='+JSON.stringify(samples)}));
  await page.evaluate(()=>{state.prefs.variant='violet';persist()});await page.reload();await page.waitForFunction(()=>document.documentElement.dataset.appReady==='true');
  const hit=page.locator('.header-artwork-swipe');
  async function swipe(left=true,vertical=false){const b=await hit.boundingBox(),x=left?b.x+b.width*.8:b.x+b.width*.2,y=b.y+b.height*.5,end=left?b.x+b.width*.2:b.x+b.width*.8,cdp=await context.newCDPSession(page);await cdp.send('Input.dispatchTouchEvent',{type:'touchStart',touchPoints:[{x,y}]});for(let i=1;i<=8;i++){await cdp.send('Input.dispatchTouchEvent',{type:'touchMove',touchPoints:[{x:vertical?x:x+(end-x)*i/8,y:vertical?y+45*i/8:y}]});await page.waitForTimeout(20)}await cdp.send('Input.dispatchTouchEvent',{type:'touchEnd',touchPoints:[]});await cdp.detach()}
  const date=await page.evaluate(()=>selected);await swipe();assert.equal(await page.evaluate(()=>state.prefs.headerArtwork),'leaves');assert.equal(await page.locator('.header-artwork-image').isVisible(),true);assert.equal(await page.evaluate(()=>selected),date);
  await swipe();assert.equal(await page.evaluate(()=>state.prefs.headerArtwork),'car');await swipe(false);assert.equal(await page.evaluate(()=>state.prefs.headerArtwork),'leaves');await swipe(true,true);assert.equal(await page.evaluate(()=>state.prefs.headerArtwork),'leaves');
  await page.reload();await page.waitForFunction(()=>document.documentElement.dataset.appReady==='true');assert.equal(await page.evaluate(()=>state.prefs.headerArtwork),'leaves');assert.equal(await page.locator('.ice-tribute').getAttribute('data-header-artwork'),'image');
  await swipeDateStrip(page);assert.equal(await page.evaluate(()=>state.prefs.headerArtwork),'leaves');await swipe(false);assert.equal(await page.evaluate(()=>state.prefs.headerArtwork),'swoosh');assert.equal(await page.locator('.header-artwork-image').isVisible(),false);assert.deepEqual(errors,[]);
 }finally{await context.close()}
});


test('all ten original header pictures decode and remain selected after relaunch',async()=>{
 const {page,context,errors}=await open();
 try{
  await page.evaluate(()=>{state.prefs.variant='violet';state.prefs.mainMenu=['day','calendar','notes'];applyTheme();persist();navigate(selected,'day')});
  const count=await page.evaluate(async()=>{for(const picture of window.plannerHeaderImages){const image=new Image();image.src=picture.src;await image.decode();if(!image.naturalWidth||!image.naturalHeight)throw new Error(picture.src)}return window.plannerHeaderImages.length});assert.equal(count,10);
  const hit=page.locator('.header-artwork-swipe');await hit.focus();
  for(let i=1;i<=10;i++){await hit.press('ArrowRight');assert.equal(await page.evaluate(()=>state.prefs.headerArtwork),'header-'+String(i).padStart(2,'0'));await page.locator('.header-artwork-image').evaluate(image=>image.decode())}
  await page.screenshot({path:'/workspace/.planner-startup-review/header-original-134.png'});
  await page.reload();await page.waitForFunction(()=>document.documentElement.dataset.appReady==='true');assert.equal(await page.evaluate(()=>state.prefs.headerArtwork),'header-10');await page.locator('.header-artwork-image').evaluate(image=>image.decode());
  await page.locator('.header-artwork-swipe').press('ArrowRight');assert.equal(await page.evaluate(()=>state.prefs.headerArtwork),'swoosh');assert.deepEqual(errors,[]);
 }finally{await context.close()}
});
