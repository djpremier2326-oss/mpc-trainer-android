/* Shared text-only section actions; the bottom navigation is unchanged. */
(()=>{
function alignControls(){
 $('prev').textContent='‹';$('next').textContent='›';
 const shuffle=document.querySelector('.wishes-heading-actions [data-wish-shuffle]');if(shuffle)shuffle.textContent='Перемешать';
}
const originalRender=render;render=function(){originalRender();alignControls()};
})();
