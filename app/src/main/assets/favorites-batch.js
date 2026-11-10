/* Each selected file is committed independently; failed files can be retried. */
(()=>{
'use strict';
let files=[],busy=false;
const originalEdit=editWish;
editWish=async function(id){if(busy)return;files=[];await originalEdit(id);$('wish-file').multiple=!wishDraftId};
async function prepare(file){
 if(file.type.startsWith('video/')){const result=await prepareWishVideo(file);return{media:result.blob,kind:'video',poster:result.poster,duration:result.duration}}
 if(!file.type.startsWith('image/')||file.size>30000000)throw Error('Выбери изображение размером до 30 МБ');
 const bitmap=await createImageBitmap(file,{resizeWidth:1200,resizeQuality:'high'});
 try{const canvas=document.createElement('canvas'),ratio=Math.min(1,1200/Math.max(bitmap.width,bitmap.height));canvas.width=Math.max(1,Math.round(bitmap.width*ratio));canvas.height=Math.max(1,Math.round(bitmap.height*ratio));canvas.getContext('2d').drawImage(bitmap,0,0,canvas.width,canvas.height);let media=canvas.toDataURL('image/jpeg',.78);if(media.length>400000)media=canvas.toDataURL('image/jpeg',.45);if(media.length>400000)throw Error('Выбери фото меньшего размера');return{media,kind:'photo'}}finally{bitmap.close()}
}
document.addEventListener('change',e=>{
 if(e.target.id!=='wish-file'||wishDraftId)return;
 files=[];$('wish-save').textContent='Сохранить';if(e.target.files.length<2)return;
 e.stopImmediatePropagation();++wishPhotoRequest;files=Array.from(e.target.files);wishDraftPhoto=null;
 releaseWishUrl('draft');$('wish-draft-image').hidden=true;const video=$('wish-draft-video');video.pause();video.removeAttribute('src');video.hidden=true;
 $('wish-error').textContent='Выбрано файлов: '+files.length+'. Каждый добавится отдельной карточкой.';
 $('wish-save').textContent='Добавить '+files.length;$('wish-save').disabled=false;
},true);
document.addEventListener('submit',async e=>{
 if(e.target.id!=='wish-form'||!files.length)return;
 e.preventDefault();e.stopImmediatePropagation();if(busy)return;
 if((state.wishes||[]).length+files.length>100){$('wish-error').textContent='В избранном может быть до 100 файлов. Выбрано: '+files.length;return}
 busy=true;const selected=files.slice(),failed=[],messages=[],version=wishDraftVersion,title=e.target.elements.title.value.trim(),why=e.target.elements.why.value.trim();
 const input=$('wish-file'),button=$('wish-save'),close=$('wish-editor').querySelector('[data-close]');input.disabled=true;button.disabled=true;close.disabled=true;let saved=0;
 try{for(let i=0;i<selected.length;i++){
  const file=selected[i];let id=null,committed=false;$('wish-error').textContent='Добавляю '+(i+1)+' из '+selected.length+'…';
  try{const prepared=await prepare(file);id=crypto.randomUUID();await wishPhoto('put',id,prepared.media);const rows=(state.wishes||[]).slice();if(rows.length>=100)throw Error('В избранном уже 100 файлов');const item={id,title,why,done:false,...(prepared.kind==='video'?{kind:'video',poster:prepared.poster,duration:prepared.duration}:{})};rows.push(item);if(!wishCommit(rows))throw Error('Не удалось сохранить запись');committed=true;saved++}
  catch(error){if(id&&!committed)try{await wishPhoto('delete',id)}catch{}failed.push(file);messages.push(file.name+': '+(error.message||'Не удалось добавить'))}
 }
 files=failed;if(tab==='wishes')renderWishes();
 if(!failed.length){$('wish-editor').close();toast('Добавлено файлов: '+saved)}else{$('wish-error').textContent='Добавлено: '+saved+'. Не добавлено: '+failed.length+'. '+messages.join('; ');button.textContent='Повторить для '+failed.length}
 }finally{busy=false;input.disabled=false;button.disabled=false;close.disabled=false;if(version!==wishDraftVersion)files=[]}
},true);
document.addEventListener('cancel',e=>{if(e.target.id==='wish-editor'&&busy)e.preventDefault()},true);
const dialogs=wishDialogs;wishDialogs=function(){dialogs();$('wish-file').multiple=!wishDraftId;if(!busy)$('wish-save').textContent='Сохранить'};
})();
