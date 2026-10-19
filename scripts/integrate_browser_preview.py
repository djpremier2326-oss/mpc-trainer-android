from pathlib import Path

p = Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerActivity.kt')
s = p.read_text()

old_field = ' private var metronome=false;private var countIn=true;private var lastBeat=-1L;private var lastClockTick=-1L;private var countInToken=0\n'
new_field = old_field + ' private val browserPreview by lazy { BrowserPreview(this) }\n'
if 'private val browserPreview by lazy' not in s:
    if old_field not in s:
        raise SystemExit('Browser preview field anchor not found')
    s = s.replace(old_field, new_field, 1)

old = ''' @Deprecated("Deprecated in Java")override fun onActivityResult(r:Int,c:Int,d:Intent?){super.onActivityResult(r,c,d);if(r!=901||c!=RESULT_OK)return;val u=d?.data?:return;pending=u;pendingName=name(u);val fd=contentResolver.openFileDescriptor(u,"r");val preview=if(fd!=null){val id=pool.load(fd.fileDescriptor,0,fd.statSize,1);fd.close();id}else 0;AlertDialog.Builder(this).setTitle("BROWSER • $pendingName").setMessage("Preview or load to BANK ${'A'+bank} PAD ${pad+1}.").setNegativeButton("CANCEL",null).setNeutralButton("PREVIEW"){_,_->if(preview!=0)pool.play(preview,.8f,.8f,1,0,1f)}.setPositiveButton("LOAD TO PAD"){_,_->loadPending()}.show()}'''
new = ''' @Deprecated("Deprecated in Java")override fun onActivityResult(r:Int,c:Int,d:Intent?){super.onActivityResult(r,c,d);if(r!=901||c!=RESULT_OK)return;val u=d?.data?:return;pending=u;pendingName=name(u);browserPreview.stop();val dialog=AlertDialog.Builder(this).setTitle("BROWSER • $pendingName").setMessage("Прослушать или загрузить в BANK ${'A'+bank} PAD ${pad+1}.\\n\\nPREVIEW воспроизводит полный WAV/MP3. После LOAD звук назначается на выбранный пэд.").setNegativeButton("ОТМЕНА",null).setNeutralButton("PREVIEW",null).setPositiveButton("LOAD TO PAD",null).create();dialog.setOnShowListener{dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener{browserPreview.play(u,onReady={runOnUiThread{showDisplayMessage("BROWSER • PREVIEW • $pendingName")}},onError={msg->runOnUiThread{Toast.makeText(this,msg,Toast.LENGTH_LONG).show()}})};dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener{browserPreview.stop();loadPending();dialog.dismiss()}};dialog.setOnDismissListener{browserPreview.stop()};dialog.show()}'''
if 'PREVIEW воспроизводит полный WAV/MP3' not in s:
    if 'PREVIEW streams the full WAV/MP3' in s:
        start=s.index(' @Deprecated("Deprecated in Java")override fun onActivityResult')
        end=s.index('\n private fun loadPending()',start)
        s=s[:start]+new+s[end:]
    else:
        if old not in s:
            raise SystemExit('Browser result anchor not found')
        s = s.replace(old, new, 1)

# Sample Edit preview must not depend on asynchronous SoundPool loading plus a guessed delay.
old_preview='val id=pool.load(f.absolutePath,1);runOnUiThread{h.postDelayed({pool.play(id,1f,1f,1,0,1f)},80)}'
new_preview='runOnUiThread{browserPreview.play(Uri.fromFile(f),onReady={showDisplayMessage("SAMPLE EDIT • PREVIEW • $label")},onError={msg->Toast.makeText(this,msg,Toast.LENGTH_LONG).show()})}'
if old_preview in s:
    s=s.replace(old_preview,new_preview,1)
elif 'SAMPLE EDIT • PREVIEW • $label' not in s:
    raise SystemExit('Sample Edit preview anchor not found')

# Ensure preview cannot continue after editor closes or after CHOP.
if 'dialog.setOnDismissListener{browserPreview.stop()};dialog.show()' not in s:
    marker='};dialog.show()}\n private fun refreshPads()'
    if marker in s:
        s=s.replace(marker,'};dialog.setOnDismissListener{browserPreview.stop()};dialog.show()}\n private fun refreshPads()',1)

p.write_text(s)
print('Browser and Sample Edit preview unified on MediaPlayer; Browser text localized')
