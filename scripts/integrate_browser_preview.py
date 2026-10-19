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
new = ''' @Deprecated("Deprecated in Java")override fun onActivityResult(r:Int,c:Int,d:Intent?){super.onActivityResult(r,c,d);if(r!=901||c!=RESULT_OK)return;val u=d?.data?:return;pending=u;pendingName=name(u);browserPreview.stop();val dialog=AlertDialog.Builder(this).setTitle("BROWSER • $pendingName").setMessage("Preview or load to BANK ${'A'+bank} PAD ${pad+1}.\\n\\nPREVIEW streams the full WAV/MP3; pads still use low-latency one-shot playback.").setNegativeButton("CANCEL",null).setNeutralButton("PREVIEW",null).setPositiveButton("LOAD TO PAD",null).create();dialog.setOnShowListener{dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener{browserPreview.play(u,onReady={runOnUiThread{status.text="BROWSER • previewing $pendingName"}},onError={msg->runOnUiThread{Toast.makeText(this,msg,Toast.LENGTH_LONG).show()}})};dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener{browserPreview.stop();loadPending();dialog.dismiss()}};dialog.setOnDismissListener{browserPreview.stop()};dialog.show()}'''
if 'PREVIEW streams the full WAV/MP3' not in s:
    if old not in s:
        raise SystemExit('Browser result anchor not found')
    s = s.replace(old, new, 1)

p.write_text(s)
print('Streaming Browser preview integrated')
