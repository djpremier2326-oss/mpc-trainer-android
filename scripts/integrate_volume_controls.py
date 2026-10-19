from pathlib import Path

p=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerActivity.kt')
s=p.read_text()

old='private var metronome=false;private var countIn=true;private var lastBeat=-1L;private var lastClockTick=-1L;private var countInToken=0'
new='private var metronome=false;private var countIn=true;private var lastBeat=-1L;private var lastClockTick=-1L;private var countInToken=0\n private var masterVolume=1f;private val padVolumes=Array(4){FloatArray(16){1f}}'
if old not in s: raise SystemExit('volume state anchor not found')
s=s.replace(old,new,1)

old='restorePads();restoreProject();engine=LearnEngine'
new='restorePads();restoreProject();restoreVolumes();engine=LearnEngine'
if old not in s: raise SystemExit('restore anchor not found')
s=s.replace(old,new,1)

old='pads[b][p]?.let{val g=padGain(v);pool.play(it.soundId,g,g,1,0,1f)}'
new='pads[b][p]?.let{val g=(padGain(v)*padVolumes[b][p]*masterVolume).coerceIn(0f,1f);pool.play(it.soundId,g,g,1,0,1f)}'
if old not in s: raise SystemExit('manual gain anchor not found')
s=s.replace(old,new,1)

old='pads[e.bank][e.pad]?.let{val g=if(e.velocity>=100)1f else e.velocity.coerceIn(1,99)/100f;pool.play(it.soundId,g,g,1,0,1f)}'
new='pads[e.bank][e.pad]?.let{val base=if(e.velocity>=100)1f else e.velocity.coerceIn(1,99)/100f;val g=(base*padVolumes[e.bank][e.pad]*masterVolume).coerceIn(0f,1f);pool.play(it.soundId,g,g,1,0,1f)}'
if old not in s: raise SystemExit('sequence gain anchor not found')
s=s.replace(old,new,1)

anchor='override fun onDestroy(){'
block='''fun showVolumeMixer(){val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(28,8,28,8)};val oldMaster=masterVolume;val oldPad=padVolumes[bank][pad];val masterLabel=TextView(this);val master=SeekBar(this).apply{max=100;progress=(masterVolume*100).toInt()};val padLabel=TextView(this);val pv=SeekBar(this).apply{max=100;progress=(padVolumes[bank][pad]*100).toInt()};fun update(){masterLabel.text="MASTER / PROJECT  ${master.progress}%";padLabel.text="BANK ${'A'+bank} PAD ${pad+1}  ${pv.progress}%"};val l=object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(s:SeekBar?,x:Int,f:Boolean){update()};override fun onStartTrackingTouch(s:SeekBar?){};override fun onStopTrackingTouch(s:SeekBar?){}};master.setOnSeekBarChangeListener(l);pv.setOnSeekBarChangeListener(l);box.addView(masterLabel);box.addView(master);box.addView(padLabel);box.addView(pv);update();AlertDialog.Builder(this).setTitle("MIXER / VOLUME").setView(box).setNegativeButton("CANCEL",null).setPositiveButton("APPLY"){_,_->val newMaster=master.progress/100f;val newPad=pv.progress/100f;masterVolume=newMaster;padVolumes[bank][pad]=newPad;saveVolumes();if(newMaster!=oldMaster||newPad!=oldPad)report("mixer_changed");refreshStatus()}.show()}
 private fun saveVolumes(){val o=JSONObject().put("master",masterVolume);val a=JSONArray();for(b in 0..3)for(p in 0..15)a.put(padVolumes[b][p]);o.put("pads",a);getSharedPreferences("trainer_v2",MODE_PRIVATE).edit().putString("volumes",o.toString()).apply()}
 private fun restoreVolumes(){val raw=getSharedPreferences("trainer_v2",MODE_PRIVATE).getString("volumes",null)?:return;try{val o=JSONObject(raw);masterVolume=o.optDouble("master",1.0).toFloat().coerceIn(0f,1f);val a=o.optJSONArray("pads")?:return;for(b in 0..3)for(p in 0..15){val i=b*16+p;if(i<a.length())padVolumes[b][p]=a.optDouble(i,1.0).toFloat().coerceIn(0f,1f)}}catch(_:Exception){}}
 '''
if anchor not in s: raise SystemExit('onDestroy anchor not found')
s=s.replace(anchor,block+anchor,1)

p.write_text(s)
print('Per-pad and MASTER project volume integrated, persisted, and Learn-validated')