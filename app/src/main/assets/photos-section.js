/* Existing albums and original blobs now belong to the dedicated Photos section. */
function photosIcon(){return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="3" y="3" width="18" height="18" rx="3"/><circle cx="8" cy="8" r="1.5"/><path d="m3 17 5-5 4 4 4-6 5 7"/></svg>'}
function renderPhotos(){
 if(tab!=='photos')return;
 if(mediaFolderActive){mediaFolderRender();return}
 mediaDispose();
 $('view').innerHTML='<section class="photos-page"><div class="notes-tools photos-tools"><strong>Фото</strong><button type="button" class="btn" data-media-new-folder>＋ Папка</button></div>'+mediaFolderCards()+(mediaFolderList().length?'':'<p class="empty-events">Создай альбом, чтобы добавить фотографии и видео.</p>')+'</section>';
}
