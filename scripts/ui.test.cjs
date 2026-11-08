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
  try{res.setHeader('Content-Type',({'.html':'text/html; charset=utf-8','.js':'text/javascript','.css':'text/css'})[path.extname(file)]||'application/octet-stream');res.end(fs.readFileSync(file))}catch{res.writeHead(404).end()}
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

test('compact calendar header matches system bar across all themes and resets on exit',async()=>{
 const {page,context,errors}=await open();
 try{
  for(const variant of Object.keys(palettes)){
   await page.evaluate(v=>{state.prefs.variant=v;applyTheme();navigate(selected,'calendar')},variant);
   const chrome=await page.evaluate(()=>({height:document.getElementById('calendar-top-tools').getBoundingClientRect().height,top:getComputedStyle(document.getElementById('calendar-top-tools')).background,bottom:getComputedStyle(document.querySelector('.tabs')).background,status:statusColors.at(-1)}));
   assert.ok(chrome.height<=52,JSON.stringify(chrome));assert.equal(chrome.top,chrome.bottom);assert.match(chrome.status.color,/^#[0-9a-f]{6}$/);if(variant==='violet')assert.equal(chrome.status.color,'#000000');
   await page.evaluate(()=>navigate(selected,'events'));assert.equal(await page.evaluate(()=>statusColors.at(-1).color),'');
  }
  assert.deepEqual(errors,[]);
 }finally{await context.close()}
});

test('home and notes have black system chrome; notes controls retain search and actions',async()=>{
 const {page,context,errors}=await open();
 try{
  assert.equal(await page.evaluate(()=>statusColors.at(-1).color),'#000000');
  await page.evaluate(()=>{state.prefs.variant='violet';applyTheme();navigate(selected,'notes')});
  assert.equal(await page.locator('.app-heading').isVisible(),false);
  assert.equal(await page.locator('.notes-tools').evaluate(el=>getComputedStyle(el).backgroundColor),'rgb(0, 0, 0)');
  assert.ok(await page.locator('.notes-tools').evaluate(el=>el.getBoundingClientRect().height)<=60);
  assert.equal(await page.evaluate(()=>statusColors.at(-1).color),'#000000');
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
