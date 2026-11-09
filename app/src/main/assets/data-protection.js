/* Logical full backups: stream blobs so videos do not hit the legacy 50 MB JSON limit. */
(()=>{
'use strict';
const native=window.AndroidFiles;
if(!native?.vaultBegin)return;
let revision=0,timer=null,running=false,again=false,restoring=false,lastCompleted=-1,pendingFinish=null,retryDelay=30000;
window.vaultSnapshotDone=text=>{const resolve=pendingFinish;pendingFinish=null;if(resolve)resolve(JSON.parse(text))};
const utf8=new TextEncoder();
function notifyError(error){const message=error?.message||String(error);const label=document.getElementById('vault-status');if(label)label.textContent='Копия не завершена: '+message;return message}
function status(){
 const label=document.getElementById('vault-status');if(!label)return;
 try{const s=JSON.parse(native.vaultStatus()),date=n=>n?new Date(n).toLocaleString('ru-RU'):'ещё нет';
 label.textContent='Защищённая копия внутри приложения: '+date(s.localAt)+'. Зашифрованная облачная копия: '+date(s.externalAt)+'. '+(s.folder?'Облако подключено. ':'Подключи облако для защиты после удаления приложения. ')+(s.error||'');
 }catch(error){notifyError(error)}
}
function dirty(){revision++;if(restoring||window.__plannerLoadFailed)return;if(running){again=true;return}if(!timer)timer=setTimeout(()=>{timer=null;checkpoint().catch(notifyError)},15000)}
window.vaultDirty=dirty;
async function records(db,storeName){return new Promise((resolve,reject)=>{
 const tx=db.transaction(storeName,'readonly'),store=tx.objectStore(storeName),values=store.getAll(),keys=store.getAllKeys();
 tx.oncomplete=()=>resolve(values.result.map((value,i)=>({key:keys.result[i],value})));tx.onerror=tx.onabort=()=>reject(tx.error||Error('Не удалось прочитать медиа'));
})}
async function encode(bytes){let out='';for(let i=0;i<bytes.length;i+=8192)out+=String.fromCharCode(...bytes.subarray(i,i+8192));return btoa(out)}
async function writeEntry(name,blob){
 if(!native.vaultEntry(name))throw Error('Не удалось открыть файл копии');
 for(let at=0;at<blob.size;at+=196608){const bytes=new Uint8Array(await blob.slice(at,at+196608).arrayBuffer());if(!native.vaultChunk(await encode(bytes)))throw Error('Не хватает места для копии');await new Promise(r=>setTimeout(r,0))}
 if(!native.vaultEndEntry())throw Error('Не удалось записать файл копии');
}
async function checkpoint(force=false){
 if(!force&&lastCompleted===revision)return true;
 if(window.__plannerLoadFailed)throw Error('Исходные данные не прочитаны. Автокопирование остановлено для их защиты');
 if(running)throw Error('Предыдущая копия ещё сохраняется');
 if(!validState(state))throw Error('Данные не прошли проверку');
 running=true;again=false;clearTimeout(timer);timer=null;const started=revision;let retry=false;
 try{
  const saved=JSON.parse(JSON.stringify(state));
  const db=await mediaDB();const media=await records(db,'media');
  const wishDb=await wishPhotoDb();let wishes;try{wishes=await records(wishDb,'photos')}finally{wishDb.close()}
  if(started!==revision)throw Error('Записи изменились во время чтения. Повторю копирование');
  for(const wish of saved.wishes||[])if(!wishes.some(r=>r.key===wish.id))throw Error('Не найден файл избранного. Неполная копия не заменит прежнюю');
  if(!native.vaultBegin())throw Error('Не удалось начать копирование');
  const manifest={format:'black-deeds-vault-1',createdAt:new Date().toISOString(),state:saved,media:[],wishes:[]};let blobId=0;
  for(const row of media){const {blob,...metadata}=row.value;if(!(blob instanceof Blob))throw Error('Не прочитан файл альбома');const file='blobs/'+(blobId++)+'.bin';await writeEntry(file,blob);manifest.media.push({...metadata,file,mime:blob.type,bytes:blob.size})}
  for(const row of wishes){if(row.value instanceof Blob){const file='blobs/'+(blobId++)+'.bin';await writeEntry(file,row.value);manifest.wishes.push({id:row.key,file,mime:row.value.type,bytes:row.value.size})}else if(typeof row.value==='string')manifest.wishes.push({id:row.key,value:row.value});else throw Error('Не прочитан файл избранного')}
  if(started!==revision){again=true;throw Error('Записи изменились. Сохраняю новую копию')}
  await writeEntry('manifest.json',new Blob([JSON.stringify(manifest)],{type:'application/json'}));
  const result=await new Promise(resolve=>{pendingFinish=resolve;const legacy=native.vaultFinish();if(typeof legacy==='string')window.vaultSnapshotDone(legacy)});if(result.error)throw Error(result.error);lastCompleted=started;retryDelay=30000;status();return true;
 }catch(error){native.vaultAbort();notifyError(error);retry=true;throw error}
 finally{running=false;if(again&&!restoring){again=false;dirty()}else if(retry&&!restoring&&!window.__plannerLoadFailed){timer=setTimeout(()=>{timer=null;checkpoint().catch(notifyError)},retryDelay);retryDelay=Math.min(300000,retryDelay*2)}}
}
window.vaultCheckpoint=checkpoint;
// Watch committed changes, including media changes made without a state write.
for(const name of ['mediaPut','mediaRemove']){const original=window[name];if(typeof original==='function')window[name]=async function(...args){const result=await original(...args);dirty();return result}}
const originalWish=wishPhoto;wishPhoto=async function(action,...args){const value=await originalWish(action,...args);if(action!=='get')dirty();return value};
document.addEventListener('planner-media-changed',dirty);
async function readBlob(row){
 if(!/^blobs\/[0-9]+\.bin$/.test(row.file)||!Number.isSafeInteger(row.bytes)||row.bytes<0)throw Error('Неверный файл копии');
 const parts=[];for(let at=0;at<row.bytes;at+=196608){const raw=native.vaultRead(row.file,at,Math.min(196608,row.bytes-at));if(raw==='!')throw Error('Файл копии повреждён');const value=atob(raw),part=Uint8Array.from(value,c=>c.charCodeAt(0));if(part.length!==Math.min(196608,row.bytes-at))throw Error('Файл копии записан не полностью');parts.push(part);await new Promise(r=>setTimeout(r,0))}
 return new Blob(parts,{type:row.mime||'application/octet-stream'});
}
async function restoreDatabase(name,isMedia){return new Promise((resolve,reject)=>{const request=indexedDB.open(name,1);request.onupgradeneeded=()=>{const store=request.result.createObjectStore(isMedia?'media':'photos',isMedia?{keyPath:'id'}:undefined);if(isMedia)store.createIndex('folder','folder',{unique:false})};request.onsuccess=()=>resolve(request.result);request.onerror=()=>reject(request.error)})}
async function putRestored(db,storeName,value,key){return new Promise((resolve,reject)=>{const tx=db.transaction(storeName,'readwrite'),store=tx.objectStore(storeName);if(key===undefined)store.put(value);else store.put(value,key);tx.oncomplete=resolve;tx.onerror=tx.onabort=()=>reject(tx.error||Error('Не хватает места для восстановления'))})}
window.restoreVaultArchive=async text=>{
 try{
  const manifest=JSON.parse(text);if(manifest.format!=='black-deeds-vault-1'||!validState(manifest.state)||!Array.isArray(manifest.media)||!Array.isArray(manifest.wishes))throw Error('Нужна полная копия «Чёрных дел»');
  if(!confirm('Восстановить полную копию от '+new Date(manifest.createdAt).toLocaleString('ru-RU')+'? Текущие данные сначала будут сохранены отдельной копией.'))return;
  if(running)throw Error('Дождись завершения текущей копии');
  if(!window.__plannerLoadFailed)await checkpoint();
  restoring=true;window.__vaultRestoring=true;
  // Write into new databases and publish their names only with the restored state.
  // A failed import cannot modify current photos or reveal old hidden albums.
  const restored=JSON.parse(JSON.stringify(manifest.state)),token=crypto.randomUUID();
  restored.prefs={...restored.prefs,mediaDatabase:'dayplan-media-restored-'+token,wishDatabase:'dayplan-wishes-restored-'+token};
  const mediaTarget=await restoreDatabase(restored.prefs.mediaDatabase,true),wishTarget=await restoreDatabase(restored.prefs.wishDatabase,false);
  try{
   for(const row of manifest.media){const {file,mime,bytes,...metadata}=row;await putRestored(mediaTarget,'media',{...metadata,blob:await readBlob(row)})}
   for(const row of manifest.wishes){if(!row.file&&typeof row.value!=='string')throw Error('Не прочитан файл избранного');await putRestored(wishTarget,'photos',row.file?await readBlob(row):row.value,row.id)}
  }finally{mediaTarget.close();wishTarget.close()}
  const old=state,failed=window.__plannerLoadFailed;state=restored;window.__plannerLoadFailed=false;window.__vaultRestoring=false;
  if(!persist()){state=old;window.__plannerLoadFailed=failed;throw Error('Восстановление не завершено: исходные данные сохранены')}
  location.reload();
 }catch(error){toast('Восстановление не завершено: '+notifyError(error),8000)}finally{restoring=false;window.__vaultRestoring=false;native.vaultCloseRestore()}
};
const settings=document.querySelector('#settings .settings-buttons');
if(settings){const block=document.createElement('section');block.innerHTML='<h3>Защита скрытых данных</h3><p id="vault-status" role="status" class="dialog-note"></p><button class="btn" id="vault-password">Задать пароль облачных копий</button><button class="btn" id="vault-cloud-files">Подключить Google Диск / облачные файлы</button><button class="btn" id="vault-folder">Подключить папку другого облака</button><button class="btn" id="vault-now">Сохранить зашифрованную копию сейчас</button><button class="btn" id="vault-local">Восстановить защищённую копию приложения</button><button class="btn" id="vault-open">Восстановить зашифрованную копию из облака</button><p class="dialog-note">Содержимое скрытых папок, имена файлов и записи шифруются. В галерее и общих папках телефона копии не создаются. Для облачных автокопий задай пароль и выбери облачный диск в системном окне. Для Google Диска подключаются два файла A и B: приложение обновляет их по очереди, сохраняя предыдущую копию при сбое. После переустановки открой файл .bdvault из облака и введи пароль. Приложение не сможет восстановить забытый пароль. Копии создаются после изменений; проверяй дату последней облачной копии, свободное место и завершение синхронизации в выбранном облаке.</p>';settings.append(block);
 document.getElementById('vault-cloud-files').onclick=()=>native.vaultCloudFiles();document.getElementById('vault-password').onclick=()=>native.vaultPassword();document.getElementById('vault-folder').onclick=()=>native.vaultFolder();document.getElementById('vault-now').onclick=async()=>{try{await checkpoint(true);toast('Полная копия сохранена')}catch(error){toast(notifyError(error),8000)}};document.getElementById('vault-local').onclick=()=>native.vaultLocal();document.getElementById('vault-open').onclick=()=>native.vaultOpen();
 const settingsDialog=document.getElementById('settings');new MutationObserver(status).observe(settingsDialog,{attributes:true,attributeFilter:['open']});
}
window.vaultPasswordConfigured=()=>{status();dirty()};
window.vaultFolderSelected=async()=>{status();try{await checkpoint(true);if(JSON.parse(native.vaultStatus()).files)await checkpoint(true);toast('Облако подключено, зашифрованные копии проверены')}catch(error){toast(notifyError(error),8000)}};
const oldExport=document.getElementById('export');if(oldExport){oldExport.hidden=true;oldExport.onclick=async()=>{try{await checkpoint(true);toast('Зашифрованная копия сохранена')}catch(error){toast(notifyError(error),8000)}};}
status();dirty();
if(window.__plannerLoadFailed){const warning=document.createElement('div');warning.setAttribute('role','alert');warning.style.cssText='padding:16px;margin:12px;border:2px solid #c94747;border-radius:12px;background:var(--surface);color:var(--text)';warning.textContent='Данные не удалось прочитать. Исходные файлы сохранены, запись поверх них заблокирована. Открой Настройки → Защита скрытых данных, чтобы восстановить копию.';document.querySelector('main')?.prepend(warning);}
// Request a backup at background transition as well as through the debounce timer.
document.addEventListener('visibilitychange',()=>{if(document.hidden&&!running&&!window.__plannerLoadFailed)checkpoint().catch(notifyError)});
})();
